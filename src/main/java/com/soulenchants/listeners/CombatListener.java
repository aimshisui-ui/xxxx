package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CombatListener implements Listener {

    private final SoulEnchants plugin;
    private final Random rng = new Random();
    /** Set during a cleave splash so the resulting damage events don't recursively re-cleave. */
    private static final ThreadLocal<Boolean> CLEAVE_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);
    /** Set during a Bleed slow-tick application so we don't recurse into bloodlust forever. */
    private static final ThreadLocal<Boolean> BLEED_TICK = ThreadLocal.withInitial(() -> Boolean.FALSE);
    /** Set during ANY enchant-driven secondary damage call (Earthshaker, Razor Wind,
     *  Divine Immolation, Reflect, Vengeance, etc). Short-circuits handleSwordEnchants
     *  on re-entry so an AOE hit doesn't recursively trigger more AOE — caused stack
     *  overflows when hitting silverfish (which spawn more silverfish on damage). */
    private static final ThreadLocal<Boolean> AOE_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Damage `target` from `source` while suppressing our own enchant procs on the
     *  secondary event. Use for every .damage() call originating in enchant logic. */
    private static void aoeDamage(LivingEntity target, double dmg, LivingEntity source) {
        if (target == null || target.isDead() || dmg <= 0) return;
        AOE_GUARD.set(true);
        try {
            if (source != null) target.damage(dmg, source);
            else target.damage(dmg);
        } catch (Throwable ignored) {
        } finally {
            AOE_GUARD.set(false);
        }
    }
    /** Per-victim Bleed stack tracking. Cosmic-style: stacks count up to 20,
     *  a DOT deals HP damage + HURT_FLESH sound every second until bleedUntil
     *  expires, then stacks decay. `bleedAttacker` credits DOT damage back to
     *  the original hitter so boss damage-maps still track contribution;
     *  `bleedCrimsonTongue` carries the mythic-weapon heal-on-tick flag.
     *  `bleedProcCd` is a per-victim throttle so rapid hits can't refresh
     *  Bleed every swing — without it, the 6s DOT visibly never lapses. */
    private final Map<UUID, Integer> bleedStacks = new HashMap<>();
    private final Map<UUID, Long> bleedUntil = new HashMap<>();
    private final Map<UUID, Long> bleedProcCd = new HashMap<>();
    private final Map<UUID, UUID> bleedAttacker = new HashMap<>();
    private final Map<UUID, Boolean> bleedCrimsonTongue = new HashMap<>();
    private final Map<UUID, Long> antiKbCd = new HashMap<>();
    private final Map<UUID, Long> lastCombatTick = new HashMap<>();
    /** Per-victim cooldown for Nature's Wrath proc (10s). */
    private final Map<UUID, Long> naturesWrathCd = new HashMap<>();
    /** Players currently rooted by Nature's Wrath — restore walk speed when expired. */
    private final java.util.Set<UUID> naturesWrathRooted = new java.util.HashSet<>();

    // (Bloodlust kill-streak tracking removed — bloodlust is now a chestplate
    // proc that heals on nearby Bleed ticks; no stack state needed.)

    public CombatListener(SoulEnchants plugin) {
        this.plugin = plugin;
        startBleedTicker();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && e.getEntity() instanceof LivingEntity) {
            handleSwordEnchants((Player) e.getDamager(), (LivingEntity) e.getEntity(), e);
        }
        if (e.getEntity() instanceof Player) {
            handleArmorOnHit((Player) e.getEntity(), e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();

        // Featherweight is now a SWORD enchant (Haste burst on hit, Nordic-style)
        // and no longer reduces fall damage.

        // Ironclad (explosion damage)
        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            int ic = maxArmor(victim, "ironclad");
            if (ic > 0) e.setDamage(e.getDamage() * (1.0 - Math.min(0.15, 0.05 * ic)));
        }

        // Lethal-hit save enchants (Phoenix, Soul Shield) via unified helper
        if (victim.getHealth() - e.getFinalDamage() > 0) return;
        if (com.soulenchants.bosses.LethalSave.trySave(victim, plugin)) {
            e.setCancelled(true);
        }
    }

    private int maxArmor(Player p, String id) {
        int max = 0;
        for (ItemStack a : p.getInventory().getArmorContents()) {
            if (a == null) continue;
            int lvl = ItemUtil.getLevel(a, id);
            if (lvl > max) max = lvl;
        }
        return max;
    }

    /** True if `attacker` is the active Veilweaver, Iron Golem, or one of their
     *  tracked minions. Used by Counter to refuse to disarm bosses. */
    private boolean isBossOrMinionAttacker(LivingEntity attacker) {
        java.util.UUID a = attacker.getUniqueId();
        com.soulenchants.bosses.Veilweaver vw = plugin.getVeilweaverManager().getActive();
        if (vw != null) {
            if (a.equals(vw.getEntity().getUniqueId())) return true;
            for (LivingEntity m : vw.getMinions()) if (m != null && a.equals(m.getUniqueId())) return true;
            for (LivingEntity c : vw.getEchoClones()) if (c != null && a.equals(c.getUniqueId())) return true;
        }
        com.soulenchants.bosses.IronGolemBoss ig = plugin.getIronGolemManager().getActive();
        if (ig != null && a.equals(ig.getEntity().getUniqueId())) return true;
        return false;
    }

    /** On-hit procs fire against players (PvP), active bosses, their minions,
     *  and any custom mob (Hollow King + cave roster, rift encounter adds).
     *  Vanilla mobs (zombie, skeleton etc in the overworld) are excluded so
     *  proc-style enchants don't farm mob grinders. */
    private boolean isValidProcTarget(LivingEntity victim) {
        if (victim instanceof Player) return true;
        com.soulenchants.bosses.Veilweaver vw = plugin.getVeilweaverManager().getActive();
        if (vw != null) {
            if (vw.getEntity().getUniqueId().equals(victim.getUniqueId())) return true;
            for (LivingEntity m : vw.getMinions())
                if (m != null && m.getUniqueId().equals(victim.getUniqueId())) return true;
            for (LivingEntity c : vw.getEchoClones())
                if (c != null && c.getUniqueId().equals(victim.getUniqueId())) return true;
        }
        com.soulenchants.bosses.IronGolemBoss ig = plugin.getIronGolemManager().getActive();
        if (ig != null) {
            if (ig.getEntity().getUniqueId().equals(victim.getUniqueId())) return true;
            if (ig.getMinions() != null && ig.getMinions().isOurMinion(victim)) return true;
        }
        // Any custom mob counts — covers Hollow King + the cave roster spawned
        // inside void rifts. CustomMob.idOf returns null for vanilla mobs so
        // regular mob grinders stay unaffected.
        if (com.soulenchants.mobs.CustomMob.idOf(victim) != null) return true;
        return false;
    }

    /** Hard cap on summed offensive bonus — final dmg can be at most 3x base
     *  from stacked enchants (Witherbane + Demonslayer + Slayer + Executioner +
     *  Reaver + Crit + ...). Soul Strike is a soul-cost ability and bypasses the
     *  cap by design. Soul Burn's flat add stays additive after the multiplier. */
    private static final double OFFENSIVE_BONUS_CAP = 2.00;

    private void handleSwordEnchants(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        // Re-entry from our own enchant-driven secondary damage (AOE splash, reflect,
        // etc) — skip processing so AOE doesn't recursively re-fire AOE.
        if (AOE_GUARD.get()) return;
        ItemStack hand = attacker.getItemInHand();
        if (hand == null) return;
        // Lock all proc-style enchants to boss / boss-minion / player targets.
        if (!isValidProcTarget(victim)) return;

        // Lifesteal (moved from DamageListener) — heal capped at 5 HP per hit
        // so it can't stack ridiculously with Blood Fury / Soul Drain.
        int ls = ItemUtil.getLevel(hand, "lifesteal");
        if (ls > 0) {
            double heal = Math.min(e.getDamage() * 0.05 * ls, 5.0);
            attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
        }

        // ──────────────────────────────────────────────────────────────
        // ADDITIVE OFFENSIVE BONUS POOL
        //
        // Every "type slayer" / "situational damage" enchant contributes to a
        // single accumulator that's capped at OFFENSIVE_BONUS_CAP. Previously
        // these multiplied serially, so a max stack hit ~8x base on bosses.
        // Now: final = base * (1 + min(sum, CAP)) * specialMult + flatAdd
        // ──────────────────────────────────────────────────────────────
        double baseDmg = e.getDamage();
        double offBonus = 0.0;
        double specialMult = 1.0;   // Soul Strike — bypasses the cap (soul-cost)
        double flatAdd = 0.0;        // Soul Burn — flat add after multiplier

        // Bleed — Cosmic-style mechanics (stacks + DOT + Slow scaling) with
        // tuned-down proc + per-victim throttle. Without the throttle, axe
        // attack speed (~1.6/sec) refreshes the 6s DOT before it lapses so it
        // visibly procs every swing. Now: 2.5%-15% per roll AND a 1.5s
        // per-victim cooldown — at most one Bleed event per 1.5s per target.
        int bleed = ItemUtil.getLevel(hand, "bleed");
        if (bleed > 0) {
            int dw = ItemUtil.getLevel(hand, "deepwounds");
            double procChance = 0.025 * bleed + 0.012 * dw;  // L1=2.5%, L6=15%, +3.6% per DW
            long now = System.currentTimeMillis();
            long lastProc = bleedProcCd.getOrDefault(victim.getUniqueId(), 0L);
            if (now - lastProc >= 1500L && rng.nextDouble() < procChance) {
                bleedProcCd.put(victim.getUniqueId(), now);
                applyBleedProc(victim, attacker, hand);
            }
        }

        // Cripple — heavy proc nerf (was 8%/lvl) + 5s CD per attacker
        int cr = ItemUtil.getLevel(hand, "cripple");
        if (cr > 0 && rng.nextDouble() < 0.03 * cr
                && plugin.getCooldownManager().isReady("cripple", attacker.getUniqueId())) {
            plugin.getCooldownManager().set("cripple", attacker.getUniqueId(), 5_000L);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, cr - 1));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, cr - 1));
        }

        // Venom — heavy proc nerf (was 10%/lvl)
        int ven = ItemUtil.getLevel(hand, "venom");
        if (ven > 0 && rng.nextDouble() < 0.04 * ven)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40 * ven, 0));

        // Wither Bane → additive
        int wb = ItemUtil.getLevel(hand, "witherbane");
        if (wb > 0 && isWitherFamily(victim)) offBonus += 0.35 * wb;

        // Demon Slayer → additive
        int demon = ItemUtil.getLevel(hand, "demonslayer");
        if (demon > 0 && isNetherMob(victim)) offBonus += 0.30 * demon;

        // Beast Slayer → additive
        int beast = ItemUtil.getLevel(hand, "beastslayer");
        if (beast > 0 && isArthropod(victim)) offBonus += 0.25 * beast;

        // Executioner → additive
        int exec = ItemUtil.getLevel(hand, "executioner");
        if (exec > 0 && victim.getHealth() / victim.getMaxHealth() < 0.30)
            offBonus += 0.25 * exec;

        // Slayer (was in PvEDamageListener — moved here for melee so it joins the cap).
        int slayer = ItemUtil.getLevel(hand, "slayer");
        if (slayer > 0 && isBossOrMinionTarget(victim)) offBonus += 0.25 * slayer;

        // Holy Smite (was in PvEDamageListener — moved here for melee).
        int holy = ItemUtil.getLevel(hand, "holysmite");
        if (holy > 0 && isUndead(victim)) offBonus += 0.30 * holy;

        // Cleave (Nordic-style) — flat 5% chance to deal a fixed 3 dmg to all valid
        // proc targets in (level)-block radius. AOE_GUARD via aoeDamage() prevents
        // recursive cleave-of-cleave; CLEAVE_GUARD retained as belt-and-suspenders.
        int cleave = ItemUtil.getLevel(hand, "cleave");
        if (cleave > 0 && !CLEAVE_GUARD.get() && rng.nextDouble() < 0.05) {
            CLEAVE_GUARD.set(true);
            try {
                for (Entity near : attacker.getNearbyEntities(cleave, 10.0, cleave)) {
                    if (!(near instanceof LivingEntity) || near.equals(attacker) || near.equals(victim)) continue;
                    if (!isValidProcTarget((LivingEntity) near)) continue;
                    aoeDamage((LivingEntity) near, 3.0, attacker);
                }
            } finally {
                CLEAVE_GUARD.set(false);
            }
            // Wraithcleaver (mythic_held = 2) — heal 1 HP per Cleave proc
            if (ItemUtil.getLevel(hand, "mythic_held") == 2) {
                attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + 1.0));
            }
        }

        // Frost Aspect — heavy proc nerf (was 12%/lvl)
        int frost = ItemUtil.getLevel(hand, "frostaspect");
        if (frost > 0 && rng.nextDouble() < 0.05 * frost) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40 * frost, frost - 1));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40 * frost, frost - 1));
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.PACKED_ICE.getId());
        }

        // Cursed Edge — heavy proc nerf (was 7%/lvl)
        int curse = ItemUtil.getLevel(hand, "cursededge");
        if (curse > 0 && rng.nextDouble() < 0.03 * curse)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));

        // Soul Burn — flat add (stays additive AFTER multiplier so the cap doesn't eat it)
        int sb = ItemUtil.getLevel(hand, "soulburn");
        if (sb > 0 && rng.nextDouble() < 0.05 * sb) {
            victim.setFireTicks(60 * sb);
            flatAdd += 1.0 * sb;
        }

        // Phantom Strike — heavy proc nerf (was 4%/lvl)
        int ps = ItemUtil.getLevel(hand, "phantomstrike");
        if (ps > 0 && rng.nextDouble() < 0.02 * ps && !(victim instanceof Player)) {
            Location behind = victim.getLocation().add(victim.getLocation().getDirection().multiply(-1.2));
            behind.setYaw(victim.getLocation().getYaw());
            attacker.teleport(behind);
            attacker.getWorld().playEffect(behind, Effect.PORTAL, 0);
        }

        // Earthshaker
        int es = ItemUtil.getLevel(hand, "earthshaker");
        if (es > 0) {
            double aoeDmg = 1.0 + (1.0 * es);
            for (Entity near : victim.getNearbyEntities(2.0, 1.5, 2.0)) {
                if (!(near instanceof LivingEntity) || near.equals(attacker) || near.equals(victim)) continue;
                if (near instanceof Player) continue;
                aoeDamage((LivingEntity) near, aoeDmg, attacker);
            }
        }

        // Bonebreaker — heavy proc nerf (was 10%/lvl)
        int bb = ItemUtil.getLevel(hand, "bonebreaker");
        if (bb > 0 && rng.nextDouble() < 0.04 * bb)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60 * bb, 0));

        // Critical Strike — proc-gated, contributes +50% to additive pool
        int crit = ItemUtil.getLevel(hand, "criticalstrike");
        if (crit > 0 && rng.nextDouble() < 0.02 * crit) {
            offBonus += 0.50;
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
        }

        // Razor Wind (small cone of bonus dmg)
        int rw = ItemUtil.getLevel(hand, "razorwind");
        if (rw > 0) {
            Vector forward = attacker.getLocation().getDirection().setY(0).normalize();
            for (Entity near : attacker.getNearbyEntities(4, 2, 4)) {
                if (!(near instanceof LivingEntity) || near.equals(attacker) || near.equals(victim)) continue;
                if (near instanceof Player) continue;
                Vector to = near.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0);
                if (to.lengthSquared() == 0) continue;
                if (to.normalize().dot(forward) > 0.4) {
                    aoeDamage((LivingEntity) near, 1.5 * rw, attacker);
                }
            }
        }

        // Greedy — handled in onKill below
        // Headhunter — handled in onKill below

        // Drunk's strength buff is applied passively in tick task.
        // Bloodlust is a CHESTPLATE proc (heal-on-nearby-bleed-tick) — handled by the global Bleed ticker.

        // Feather Weight (Nordic-style) — 20%/lvl chance to gain Haste burst
        int fw = ItemUtil.getLevel(hand, "featherweight");
        if (fw > 0 && !attacker.hasPotionEffect(PotionEffectType.FAST_DIGGING)
                && rng.nextDouble() <= 0.2 * fw) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, fw * 20, fw - 1, true, false), true);
        }

        // Blessed (Nordic-style) — 20%/lvl chance on hit to strip your own debuffs.
        // Effect still procs every roll; chat message is throttled to once per 6s
        // so it doesn't flood the chat during sustained combat.
        int blessed = ItemUtil.getLevel(hand, "blessed");
        if (blessed > 0 && rng.nextDouble() < 0.2 * blessed) {
            blessSelf(attacker);
            if (plugin.getCooldownManager().isReady("blessed_msg", attacker.getUniqueId())) {
                plugin.getCooldownManager().set("blessed_msg", attacker.getUniqueId(), 6_000L);
                attacker.sendMessage("§e§l** BLESSED **");
                attacker.playSound(attacker.getLocation(), org.bukkit.Sound.SPLASH, 1.2f, 2.0f);
            }
        }

        // ─── AXE enchants (PvE/PvP, fire whether wielding sword OR axe) ───

        // Reaver → additive (scales with attacker's missing HP)
        int reaver = ItemUtil.getLevel(hand, "reaver");
        if (reaver > 0) {
            double missingPct = 1.0 - (attacker.getHealth() / attacker.getMaxHealth());
            offBonus += 0.08 * reaver * missingPct;
        }
        // Skullcrush — proc Nausea + Weakness II. Previously player-only; now
        // fires against bosses + minions too (victim is already gated by
        // isValidProcTarget at the top of this method).
        int skull = ItemUtil.getLevel(hand, "skullcrush");
        if (skull > 0 && rng.nextDouble() < 0.04 * skull) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 60 * skull, 0));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,  60 * skull, 1));
        }
        // Hamstring — root victim (Slow IV) for 2s
        int ham = ItemUtil.getLevel(hand, "hamstring");
        if (ham > 0 && rng.nextDouble() < 0.03 * ham) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 3));
        }
        // Blood Fury — heal 25%/lvl of damage dealt while below 30% HP. Cap at
        // 5 HP per hit so it doesn't combo with Lifesteal/Soul Drain into
        // perma-sustain.
        int bf = ItemUtil.getLevel(hand, "bloodfury");
        if (bf > 0 && attacker.getHealth() < attacker.getMaxHealth() * 0.30) {
            double heal = Math.min(e.getDamage() * (0.25 * bf), 5.0);
            attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
        }
        // Shieldbreaker — proc TRUE bonus damage (ignores armor). Sourceless damage
        // call still fires EntityDamageEvent without a damager, so it doesn't
        // re-enter onAttack — but guard anyway since handleArmorOnHit/onAnyDamage
        // would still process it.
        int sb2 = ItemUtil.getLevel(hand, "shieldbreaker");
        if (sb2 > 0 && rng.nextDouble() < 0.05 * sb2) {
            double trueDmg = e.getDamage() * 0.25;
            aoeDamage(victim, trueDmg, null);
        }
        // Frostshatter — Slow III + Mining Fatigue III for 4s
        int fs = ItemUtil.getLevel(hand, "frostshatter");
        if (fs > 0 && rng.nextDouble() < 0.05 * fs) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 2));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 80, 2));
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.PACKED_ICE.getId());
        }
        // Wraithcleave → additive (only if victim is alone)
        int wc = ItemUtil.getLevel(hand, "wraithcleave");
        if (wc > 0) {
            boolean alone = true;
            for (Entity near : victim.getNearbyEntities(8, 8, 8)) {
                if (near == attacker) continue;
                if (near instanceof LivingEntity && !(near instanceof Player)) {
                    alone = false; break;
                }
            }
            if (alone) offBonus += 0.25 * wc;
        }
        // Rending Blow — Wither III for 5s
        int rb = ItemUtil.getLevel(hand, "rendingblow");
        if (rb > 0 && rng.nextDouble() < 0.04 * rb) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2));
        }
        // Executioner's Mark → additive (when victim has any debuff)
        int em = ItemUtil.getLevel(hand, "executionersmark");
        if (em > 0) {
            boolean hasDebuff = victim.hasPotionEffect(PotionEffectType.SLOW)
                    || victim.hasPotionEffect(PotionEffectType.WEAKNESS)
                    || victim.hasPotionEffect(PotionEffectType.WITHER)
                    || victim.hasPotionEffect(PotionEffectType.POISON)
                    || victim.hasPotionEffect(PotionEffectType.SLOW_DIGGING)
                    || victim.hasPotionEffect(PotionEffectType.CONFUSION)
                    || victim.hasPotionEffect(PotionEffectType.BLINDNESS);
            if (hasDebuff) offBonus += 0.15 * em;
        }

        // Soul Strike — bypass-cap special multiplier (rare proc + soul cost = self-throttled)
        int ss = ItemUtil.getLevel(hand, "soulstrike");
        if (ss > 0 && rng.nextDouble() < 0.05) {
            int cost = 100;
            if (plugin.getSoulManager().take(attacker, cost)) {
                specialMult *= (1.5 + 0.5 * ss);
                attacker.getWorld().strikeLightningEffect(victim.getLocation());
                attacker.sendMessage("§4✦ Soul Strike! §7(-" + cost + ")");
            }
        }

        // Soul Drain — cost bump (15 → 50 souls per hit)
        int sd = ItemUtil.getLevel(hand, "souldrain");
        if (sd > 0) {
            int cost = 50;
            if (plugin.getSoulManager().take(attacker, cost)) {
                double heal = e.getDamage() * (0.25 * sd);
                attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
            }
        }

        // Divine Immolation — every swing, splash divine fire to ALL valid proc
        // targets within `level` blocks (excludes attacker). 5 souls per swing.
        // Self-rate-limited by soul cost; no cooldown.
        int di = ItemUtil.getLevel(hand, "divineimmolation");
        if (di > 0 && plugin.getSoulManager().take(attacker, 5)) {
            double splash = di * 1.1;
            double radius = di;
            for (Entity near : victim.getNearbyEntities(radius, radius, radius)) {
                if (near == attacker) continue;
                if (!(near instanceof LivingEntity)) continue;
                LivingEntity le = (LivingEntity) near;
                if (!isValidProcTarget(le)) continue;
                aoeDamage(le, splash, attacker);
                Location l = le.getLocation().add(0, 1, 0);
                l.getWorld().playEffect(l, Effect.MOBSPAWNER_FLAMES, 0);
                if (le instanceof Player) ((Player) le).sendMessage("§c§l** DIVINE IMMOLATION **");
            }
            attacker.getWorld().playSound(attacker.getLocation(), org.bukkit.Sound.FIREWORK_BLAST, 1.0f, 0.4f);
        }

        // ──────────────────────────────────────────────────────────────
        // FINAL DAMAGE APPLICATION
        // base * (1 + min(offBonus, CAP)) * specialMult + flatAdd
        // ──────────────────────────────────────────────────────────────
        double cappedBonus = Math.min(offBonus, OFFENSIVE_BONUS_CAP);
        double finalDmg = baseDmg * (1.0 + cappedBonus) * specialMult + flatAdd;
        if (finalDmg != baseDmg) e.setDamage(finalDmg);
    }

    /** Is `target` an active boss or one of their tracked minions? Used for Slayer
     *  bonus eligibility. Mirrors PvEDamageListener.isBossOrMinion but lives here
     *  so the additive pool can read it without crossing listener boundaries.
     *  Includes Iron Golem sentinels + any custom mob (Hollow King, cave roster). */
    private boolean isBossOrMinionTarget(LivingEntity target) {
        com.soulenchants.bosses.Veilweaver vw = plugin.getVeilweaverManager().getActive();
        if (vw != null) {
            if (target.getUniqueId().equals(vw.getEntity().getUniqueId())) return true;
            for (LivingEntity m : vw.getMinions())
                if (m != null && m.getUniqueId().equals(target.getUniqueId())) return true;
            for (LivingEntity c : vw.getEchoClones())
                if (c != null && c.getUniqueId().equals(target.getUniqueId())) return true;
        }
        com.soulenchants.bosses.IronGolemBoss ig = plugin.getIronGolemManager().getActive();
        if (ig != null) {
            if (ig.getEntity().getUniqueId().equals(target.getUniqueId())) return true;
            if (ig.getMinions() != null && ig.getMinions().isOurMinion(target)) return true;
        }
        if (com.soulenchants.mobs.CustomMob.idOf(target) != null) return true;
        return false;
    }

    private boolean isUndead(LivingEntity ent) {
        EntityType t = ent.getType();
        return t == EntityType.ZOMBIE || t == EntityType.SKELETON
                || t == EntityType.PIG_ZOMBIE || t == EntityType.WITHER;
    }

    private void handleArmorOnHit(Player victim, EntityDamageByEntityEvent e) {
        // Same recursion break as handleSwordEnchants — Reflect/Vengeance/Spite/
        // Vampiric Plate damage the attacker, which can re-enter onAttack. Without
        // this guard a Reflect chain between two armored players could oscillate.
        if (AOE_GUARD.get()) return;
        ItemStack[] armor = victim.getInventory().getArmorContents();
        int hardened=0, antikb=0, molten=0, lastStand=0, stormcall=0, guardians=0,
            reflect=0, endurance=0, vengeance=0, soulburst=0, overshield=0, naturesWrath=0,
            counter=0, aegis=0, rush=0, spite=0, ironskin=0, vampiricplate=0, callous=0;
        // Stackable enchants: SUM levels across all pieces (more pieces = stronger).
        int armoredSum = 0, enlightenedSum = 0;
        for (ItemStack a : armor) {
            if (a == null) continue;
            hardened   = Math.max(hardened,   ItemUtil.getLevel(a, "hardened"));
            antikb     = Math.max(antikb,     ItemUtil.getLevel(a, "antiknockback"));
            molten     = Math.max(molten,     ItemUtil.getLevel(a, "molten"));
            lastStand  = Math.max(lastStand,  ItemUtil.getLevel(a, "laststand"));
            stormcall  = Math.max(stormcall,  ItemUtil.getLevel(a, "stormcaller"));
            guardians  = Math.max(guardians,  ItemUtil.getLevel(a, "guardians"));
            reflect    = Math.max(reflect,    ItemUtil.getLevel(a, "reflect"));
            endurance  = Math.max(endurance,  ItemUtil.getLevel(a, "endurance"));
            vengeance  = Math.max(vengeance,  ItemUtil.getLevel(a, "vengeance"));
            soulburst  = Math.max(soulburst,  ItemUtil.getLevel(a, "soulburst"));
            overshield = Math.max(overshield, ItemUtil.getLevel(a, "overshield"));
            naturesWrath = Math.max(naturesWrath, ItemUtil.getLevel(a, "natureswrath"));
            counter      = Math.max(counter,      ItemUtil.getLevel(a, "counter"));
            aegis        = Math.max(aegis,        ItemUtil.getLevel(a, "aegis"));
            rush         = Math.max(rush,         ItemUtil.getLevel(a, "rush"));
            spite        = Math.max(spite,        ItemUtil.getLevel(a, "spite"));
            ironskin     = Math.max(ironskin,     ItemUtil.getLevel(a, "ironskin"));
            vampiricplate= Math.max(vampiricplate,ItemUtil.getLevel(a, "vampiricplate"));
            callous      = Math.max(callous,      ItemUtil.getLevel(a, "callous"));
            armoredSum     += ItemUtil.getLevel(a, "armored");
            enlightenedSum += ItemUtil.getLevel(a, "enlightened");
        }
        final UUID id = victim.getUniqueId();

        // Armored: stacks across chestplate + leggings.
        // proc = 2% × sum_level (max 16% at 4+4)   reduction = 1% × sum_level (max 8%)
        if (armoredSum > 0 && e.getDamager() instanceof Player) {
            ItemStack hand = ((Player) e.getDamager()).getItemInHand();
            if (hand != null && hand.getType().name().endsWith("_SWORD")) {
                double proc = Math.min(0.16, 0.02 * armoredSum);
                if (rng.nextDouble() < proc) {
                    double reduction = Math.min(0.08, 0.01 * armoredSum);
                    e.setDamage(e.getDamage() * (1.0 - reduction));
                }
            }
        }

        // Enlightened — heavy nerf. 0.2% × sum_level, hard-capped at 2%.
        // (was 0.5% × sum_level, max 6%)
        if (enlightenedSum > 0) {
            double proc = Math.min(0.02, 0.002 * enlightenedSum);
            if (rng.nextDouble() < proc) {
                double dmg = e.getDamage();
                int healCap = (int) Math.min(6.0, Math.max(1.0, dmg));
                int heal = 1 + rng.nextInt(healCap); // uniform 1..healCap, so 6 is rare
                e.setCancelled(true);
                double newHp = Math.min(victim.getMaxHealth(), victim.getHealth() + heal);
                victim.setHealth(newHp);
                victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                        org.bukkit.Effect.STEP_SOUND, Material.EMERALD_BLOCK.getId());
                victim.sendMessage("§6✦ §eEnlightened — §a+" + heal + " HP §7(damage absorbed)");
                return;
            }
        }

        // Hardened (Nordic-style) — 20%/lvl chance to RESTORE durability across all
        // armor pieces. Each piece's durability shifts down by (level+2). Lower
        // durability values = more uses left in 1.8.x's anvil-side numbering.
        if (hardened > 0 && rng.nextDouble() < 0.2 * hardened) {
            for (ItemStack piece : armor) {
                if (piece == null) continue;
                if (!piece.getType().name().endsWith("_HELMET")
                        && !piece.getType().name().endsWith("_CHESTPLATE")
                        && !piece.getType().name().endsWith("_LEGGINGS")
                        && !piece.getType().name().endsWith("_BOOTS")) continue;
                short dur = piece.getDurability();
                int restored = hardened + 2;
                short newDur = (short) Math.max(0, dur - restored);
                piece.setDurability(newDur);
            }
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
        }

        if (antikb > 0) {
            long now = System.currentTimeMillis();
            if (now - antiKbCd.getOrDefault(id, 0L) > 200) {
                antiKbCd.put(id, now);
                final double reduceTo = 1.0 - (0.25 * antikb);
                final Vector before = victim.getVelocity().clone();
                new BukkitRunnable() {
                    @Override public void run() {
                        Vector after = victim.getVelocity();
                        Vector delta = after.clone().subtract(before);
                        victim.setVelocity(before.add(delta.multiply(Math.max(0, reduceTo))));
                    }
                }.runTask(plugin);
            }
        }
        // Molten — heavy proc nerf (was 12%/lvl)
        if (molten > 0 && e.getDamager() instanceof LivingEntity && rng.nextDouble() < 0.05 * molten)
            ((LivingEntity) e.getDamager()).setFireTicks(60 * molten);
        if (lastStand > 0 && victim.getHealth() <= 4.0)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30, Math.max(0, lastStand - 2)));
        // Stormcaller — added a proc gate. Was: always when dmg>=6 + CD only.
        // Now: 5% × lvl chance ON TOP of CD, so it's a rare lightning rather than reliable.
        if (stormcall > 0 && e.getDamage() >= 6 && e.getDamager() instanceof LivingEntity
                && plugin.getCooldownManager().isReady("stormcaller", id)
                && rng.nextDouble() < 0.05 * stormcall) {
            long cd = 14000L - (2000L * (stormcall - 1));
            plugin.getCooldownManager().set("stormcaller", id, cd);
            e.getDamager().getWorld().strikeLightning(e.getDamager().getLocation());
        }
        // Guardians — heavy nerf (was 3%/lvl, max 9% at L3 → now 1%/lvl, max 3%)
        if (guardians > 0 && plugin.getCooldownManager().isReady("guardians", id)
                && rng.nextDouble() < 0.01 * guardians) {
            plugin.getCooldownManager().set("guardians", id, 120_000L);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 0));
            victim.sendMessage("§6✦ §eGuardians §7— §a+1 absorption heart §7(120s CD)");
        }
        // Overshield — heavy nerf (was 2%/lvl, max 8% at L4 → now 0.5%/lvl, max 2%)
        if (overshield > 0 && plugin.getCooldownManager().isReady("overshield", id)
                && rng.nextDouble() < 0.005 * overshield) {
            plugin.getCooldownManager().set("overshield", id, 120_000L);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 0));
            victim.sendMessage("§6✦ §eOvershield §7— §a+1 absorption heart §7(120s CD)");
        }
        if (reflect > 0 && e.getDamager() instanceof LivingEntity
                && plugin.getCooldownManager().isReady("reflect", id)) {
            plugin.getCooldownManager().set("reflect", id, 5_000L);
            aoeDamage((LivingEntity) e.getDamager(), e.getDamage() * (0.06 * reflect), victim);
        }
        // Endurance: capped at 6% (was 12%)
        if (endurance > 0) {
            long now = System.currentTimeMillis();
            long last = lastCombatTick.getOrDefault(id, 0L);
            if (now - last < 10_000L)
                e.setDamage(e.getDamage() * (1.0 - Math.min(0.06, 0.01 * endurance)));
            lastCombatTick.put(id, now);
        }
        if (vengeance > 0 && e.getDamager() instanceof LivingEntity)
            aoeDamage((LivingEntity) e.getDamager(), 0.5 + 0.3 * vengeance, victim);
        // Nature's Wrath — 2% on-hit proc, 10s per-victim CD. Roots all enemy
        // LivingEntities in level*3 radius for (5+level)s + lightning every
        // second for `level` damage. 75 souls per cast. Soul-tier ARMOR.
        if (naturesWrath > 0 && e.getDamager() instanceof LivingEntity && rng.nextDouble() < 0.02) {
            long now = System.currentTimeMillis();
            long lastCast = naturesWrathCd.getOrDefault(id, 0L);
            if (now - lastCast >= 10_000L && plugin.getSoulManager().take(victim, 75)) {
                naturesWrathCd.put(id, now);
                triggerNaturesWrath(victim, naturesWrath);
            }
        }

        // Soul Burst — soul cost bumped (50 → 150)
        if (soulburst > 0) {
            int cost = 150;
            if (plugin.getSoulManager().take(victim, cost)) {
                for (Entity near : victim.getNearbyEntities(4, 3, 4)) {
                    if (!(near instanceof LivingEntity)) continue;
                    Vector push = near.getLocation().toVector().subtract(victim.getLocation().toVector())
                            .setY(0).normalize().multiply(1.2);
                    push.setY(0.5);
                    near.setVelocity(push);
                    ((LivingEntity) near).damage(4 * soulburst, victim);
                }
                victim.getWorld().createExplosion(victim.getLocation().getX(), victim.getLocation().getY(),
                        victim.getLocation().getZ(), 0f, false);
            }
        }

        // ─── New PvP armor enchants ───────────────────────────────────────

        // Callous — % chance to take 0 melee damage. Checked first so it can short-circuit.
        if (callous > 0 && e.getDamager() instanceof LivingEntity && rng.nextDouble() < 0.02 * callous) {
            e.setDamage(0);
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
            return;
        }
        // Iron Skin — chance to negate critical hits. We approximate "crit" as
        // attacker airborne + their dmg event being marked CRITICAL by Bukkit
        // isn't available pre-1.9; use airborne as the cheap heuristic.
        if (ironskin > 0 && e.getDamager() instanceof Player && rng.nextDouble() < 0.12 * ironskin) {
            Player atk = (Player) e.getDamager();
            if (atk.getFallDistance() > 0 && !atk.isOnGround()) {
                e.setDamage(e.getDamage() / 1.5); // strip the 1.5x crit bonus
                victim.sendMessage("§7§l⛨ Iron Skin §8— §fcritical hit absorbed.");
            }
        }
        // Aegis — Resistance II for 4s when below 6 HP, 30s CD
        if (aegis > 0 && victim.getHealth() <= 6.0
                && plugin.getCooldownManager().isReady("aegis", id)) {
            plugin.getCooldownManager().set("aegis", id, 30_000L);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 80, 1));
            victim.sendMessage("§b§l⛨ AEGIS §7— §fResistance II for 4s");
        }
        // Rush — chance for Speed II burst on hit, 10s CD
        if (rush > 0 && plugin.getCooldownManager().isReady("rush", id)
                && rng.nextDouble() < 0.06 * rush) {
            plugin.getCooldownManager().set("rush", id, 10_000L);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
            victim.sendMessage("§e§l✦ RUSH §7— §fSpeed II for 3s");
        }
        // Spite — reflect 8%/lvl as TRUE damage (ignores armor)
        if (spite > 0 && e.getDamager() instanceof LivingEntity) {
            double trueDmg = e.getDamage() * 0.08 * spite;
            aoeDamage((LivingEntity) e.getDamager(), trueDmg, null);
        }
        // Vampiric Plate — chance to steal 2 HP from attacker
        if (vampiricplate > 0 && e.getDamager() instanceof LivingEntity
                && rng.nextDouble() < 0.04 * vampiricplate) {
            LivingEntity atk = (LivingEntity) e.getDamager();
            aoeDamage(atk, 2.0, null);
            victim.setHealth(Math.min(victim.getMaxHealth(), victim.getHealth() + 2.0));
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
        }
        // Counter — chance to disarm attacker. 30s CD per victim.
        // Player-only by design; bosses + custom mobs are LivingEntity not Player
        // so they can't trigger this. The extra custom-mob NBT/metadata check
        // below is belt-and-suspenders against any plugin spawning fake-Players.
        if (counter > 0 && e.getDamager() instanceof Player
                && plugin.getCooldownManager().isReady("counter", id)
                && rng.nextDouble() < 0.04 * counter) {
            Player atk = (Player) e.getDamager();
            // Skip if the "Player" is somehow tagged as a custom mob or registered
            // as an active boss / boss-minion in our trackers.
            if (com.soulenchants.mobs.CustomMob.idOf(atk) != null) return;
            if (isBossOrMinionAttacker(atk)) return;

            plugin.getCooldownManager().set("counter", id, 30_000L);
            ItemStack atkHand = atk.getItemInHand();
            if (atkHand != null && atkHand.getType() != Material.AIR) {
                atk.setItemInHand(null);
                atk.getWorld().dropItemNaturally(atk.getLocation().add(0, 0.4, 0), atkHand);
                atk.sendMessage("§c§l⚔ COUNTER §7— §fdisarmed by §e" + victim.getName());
                victim.sendMessage("§b§l⚔ COUNTER §7— §fdisarmed §e" + atk.getName());
            }
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (e instanceof org.bukkit.event.entity.PlayerDeathEvent) return;
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        ItemStack hand = killer.getItemInHand();
        if (hand == null) return;

        // Reaper heal-on-kill
        int reaper = ItemUtil.getLevel(hand, "reaper");
        if (reaper > 0)
            killer.setHealth(Math.min(killer.getMaxHealth(), killer.getHealth() + 2.0 * reaper));

        // Berserker's Edge — Strength I for (level * 5)s on kill (refreshable)
        int be = ItemUtil.getLevel(hand, "berserkersedge");
        if (be > 0) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, be * 5 * 20, 0), true);
        }

        // (Bloodlust is now a CHESTPLATE enchant that heals on nearby Bleed ticks.)

        // Headhunter — heavy proc nerf (was 4%/lvl)
        int hh = ItemUtil.getLevel(hand, "headhunter");
        if (hh > 0 && rng.nextDouble() < 0.02 * hh) {
            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 0);
            e.getDrops().add(skull);
        }

        // Greedy — proc nerf (was 10%/lvl → 4%/lvl)
        int greedy = ItemUtil.getLevel(hand, "greedy");
        if (greedy > 0) {
            java.util.List<ItemStack> bonus = new java.util.ArrayList<>();
            for (ItemStack drop : e.getDrops()) {
                if (drop == null) continue;
                if (rng.nextDouble() < 0.04 * greedy) {
                    ItemStack extra = drop.clone();
                    extra.setAmount(1);
                    bonus.add(extra);
                }
            }
            e.getDrops().addAll(bonus);
        }
    }

    private boolean isWitherFamily(LivingEntity ent) {
        if (ent.getType() == EntityType.WITHER) return true;
        if (ent instanceof Skeleton && ((Skeleton) ent).getSkeletonType() == Skeleton.SkeletonType.WITHER) return true;
        return false;
    }

    private boolean isNetherMob(LivingEntity ent) {
        EntityType t = ent.getType();
        return t == EntityType.BLAZE || t == EntityType.PIG_ZOMBIE
                || t == EntityType.GHAST || t == EntityType.MAGMA_CUBE
                || t == EntityType.WITHER || isWitherFamily(ent);
    }

    private boolean isArthropod(LivingEntity ent) {
        EntityType t = ent.getType();
        return t == EntityType.SPIDER || t == EntityType.CAVE_SPIDER
                || t == EntityType.SILVERFISH || t == EntityType.ENDERMITE;
    }

    /**
     * Nature's Wrath cast — root every enemy in level*3 radius for (5+level)s,
     * then call lightning down on each every second for `level` damage.
     * Rooting uses Slow 129 + Jump 129 + Weakness + walkSpeed=0; restored on
     * task expiry, death, or quit (handled in PvEDamageListener / quit hooks).
     */
    private void triggerNaturesWrath(Player caster, int level) {
        int radius = level * 3;
        int durTicks = (5 + level) * 20;
        java.util.List<Player> rooted = new java.util.ArrayList<>();
        for (Entity near : caster.getNearbyEntities(radius, radius, radius)) {
            if (!(near instanceof Player)) continue;
            Player victim = (Player) near;
            if (victim == caster) continue;
            try { victim.setWalkSpeed(0.0f); } catch (Throwable ignored) {}
            victim.removePotionEffect(PotionEffectType.JUMP);
            victim.removePotionEffect(PotionEffectType.SLOW);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,     durTicks, 129, false, false));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,     durTicks, 129, false, false));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durTicks, 3,   false, false));
            naturesWrathRooted.add(victim.getUniqueId());
            rooted.add(victim);
            victim.playSound(victim.getLocation(), org.bukkit.Sound.ENDERDRAGON_GROWL, 2.0f, 2.0f);
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0), Effect.LARGE_SMOKE, 0);
        }
        caster.sendMessage("§a§l** NATURE'S WRATH **");
        caster.sendMessage("§a§l- 75 Soul Gems §7§n" + plugin.getSoulManager().get(caster) + "§7 souls left.");
        if (rooted.isEmpty()) return;
        // 5-tick lightning storm: every 1s for 5s, strike each rooted player + dmg `level`.
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 5) {
                    for (Player v : rooted) restoreFromNaturesWrath(v);
                    cancel();
                    return;
                }
                for (Player v : rooted) {
                    if (v == null || v.isDead() || !v.isOnline()) continue;
                    v.getWorld().strikeLightningEffect(v.getLocation());
                    v.damage(level);
                    try { v.playSound(v.getLocation(), org.bukkit.Sound.GHAST_SCREAM2, 2.0f, 2.0f); } catch (Throwable ignored) {}
                    v.sendMessage("§2§l** NATURES **");
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /** Restore walk speed + clear NW marker. Public-ish so we can call from
     *  death/quit listeners if we add them later. */
    public void restoreFromNaturesWrath(Player p) {
        if (p == null) return;
        try { p.setWalkSpeed(0.2f); } catch (Throwable ignored) {}
        p.removePotionEffect(PotionEffectType.JUMP);
        p.removePotionEffect(PotionEffectType.SLOW);
        p.removePotionEffect(PotionEffectType.WEAKNESS);
        naturesWrathRooted.remove(p.getUniqueId());
    }

    @EventHandler
    public void onQuitClearWrath(org.bukkit.event.player.PlayerQuitEvent e) {
        restoreFromNaturesWrath(e.getPlayer());
        // Per-player tracking maps shouldn't grow forever for offline players.
        UUID id = e.getPlayer().getUniqueId();
        antiKbCd.remove(id);
        lastCombatTick.remove(id);
        naturesWrathCd.remove(id);
        bleedStacks.remove(id);
        bleedUntil.remove(id);
        bleedProcCd.remove(id);
        bleedAttacker.remove(id);
        bleedCrimsonTongue.remove(id);
    }

    @EventHandler
    public void onDeathClearWrath(org.bukkit.event.entity.PlayerDeathEvent e) {
        restoreFromNaturesWrath(e.getEntity());
    }

    /**
     * Lifebloom (Nordic-style) — on the wearer's death, fully heal every
     * GUILDMATE within 20 blocks. Wearer must be in a guild for the proc to
     * fire at all. 100s cooldown to prevent farm-cycles.
     */
    @EventHandler
    public void onLifebloomDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        Player victim = e.getEntity();
        ItemStack legs = victim.getInventory().getLeggings();
        if (legs == null) return;
        int lvl = ItemUtil.getLevel(legs, "lifebloom");
        if (lvl <= 0) return;
        com.soulenchants.guilds.Guild g = plugin.getGuildManager() == null ? null
                : plugin.getGuildManager().getByMember(victim.getUniqueId());
        if (g == null) return;                          // not in a guild → no proc
        if (!plugin.getCooldownManager().isReady("lifebloom", victim.getUniqueId())) return;
        plugin.getCooldownManager().set("lifebloom", victim.getUniqueId(), 100_000L);
        int range = 20;
        for (Entity near : victim.getNearbyEntities(range, range, range)) {
            if (!(near instanceof Player) || near == victim) continue;
            Player ally = (Player) near;
            if (!g.isMember(ally.getUniqueId())) continue; // guildmates only
            ally.setHealth(ally.getMaxHealth());
            try {
                ally.sendTitle("§a§l** LIFEBLOOM **",
                        "§7" + victim.getName() + " §areturned your blood to bloom.");
            } catch (Throwable ignored) {}
            ally.playSound(ally.getLocation(), org.bukkit.Sound.LEVEL_UP, 1.0f, 1.5f);
        }
    }

    /** Bleed proc — on hit. Decays stacks if 30s has passed with no refresh,
     *  then adds one stack (capped 20), extends the DOT to now+6s, re-applies
     *  Slow with amp scaled by stacks, and caches the attacker/weapon flags
     *  the DOT ticker will need. */
    private static final int BLEED_MAX_STACKS   = 20;
    private static final long BLEED_DURATION_MS = 6_000L;
    private static final long BLEED_DECAY_MS    = 30_000L;

    private void applyBleedProc(LivingEntity victim, Player attacker, ItemStack weapon) {
        UUID id = victim.getUniqueId();
        long now = System.currentTimeMillis();
        long until = bleedUntil.getOrDefault(id, 0L);
        // Decay: if fully expired AND old enough, reset stacks
        if (now > until && now - until > BLEED_DECAY_MS) bleedStacks.put(id, 0);

        int stacks = Math.min(BLEED_MAX_STACKS, bleedStacks.getOrDefault(id, 0) + 1);
        bleedStacks.put(id, stacks);
        bleedUntil.put(id, now + BLEED_DURATION_MS);
        bleedAttacker.put(id, attacker != null ? attacker.getUniqueId() : null);
        bleedCrimsonTongue.put(id, weapon != null && ItemUtil.getLevel(weapon, "mythic_held") == 1);

        // Slow amp: 1 extra per 5 stacks, cap at 3 (Slow IV). Duration matches
        // remaining bleed so the slow visibly tracks the DOT.
        int amp = Math.min(3, stacks / 5);
        int dur = (int)(BLEED_DURATION_MS / 50L);  // ticks
        BLEED_TICK.set(true);
        try { victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, dur, amp), true); }
        finally { BLEED_TICK.set(false); }
        // On-proc burst — bigger than the tick burst to punctuate the initial hit
        spawnBloodBurst(victim, stacks + 2);
    }

    /** Redstone-block step-sound particle burst around a victim. Cosmic uses
     *  the same `sendWorldEffect(2001, REDSTONE_BLOCK)` trick — it's the
     *  bright red crumble particle, which reads as blood. Count scales with
     *  stacks so a max-stack victim looks like they're fountaining. */
    private static void spawnBloodBurst(LivingEntity victim, int intensity) {
        Location base = victim.getLocation().add(0, 1.0, 0);
        org.bukkit.World w = victim.getWorld();
        int particles = Math.min(12, 3 + intensity / 2);
        for (int i = 0; i < particles; i++) {
            double dx = (Math.random() - 0.5) * 0.9;
            double dy = (Math.random() - 0.2) * 0.9;
            double dz = (Math.random() - 0.5) * 0.9;
            w.playEffect(base.clone().add(dx, dy, dz),
                    Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
        }
    }

    /** One global 1-second ticker drives all active Bleeds: deals scaled HP
     *  damage, credits the attacker, heals nearby Bloodlust wearers, and heals
     *  the Crimson Tongue wielder. Cleans up expired / dead entries in-place. */
    private void startBleedTicker() {
        new BukkitRunnable() {
            @Override public void run() {
                if (bleedUntil.isEmpty()) return;
                long now = System.currentTimeMillis();
                java.util.Iterator<Map.Entry<UUID, Long>> it = bleedUntil.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Long> ent = it.next();
                    UUID id = ent.getKey();
                    // Expired — drop all state but keep the stack history around
                    // briefly so a re-proc within 30s stacks on top.
                    if (now > ent.getValue()) {
                        if (now - ent.getValue() > BLEED_DECAY_MS) {
                            bleedStacks.remove(id);
                            bleedProcCd.remove(id);
                        }
                        it.remove();
                        bleedAttacker.remove(id);
                        bleedCrimsonTongue.remove(id);
                        continue;
                    }
                    LivingEntity victim = lookupLiving(id);
                    if (victim == null || victim.isDead()) {
                        it.remove();
                        bleedStacks.remove(id);
                        bleedProcCd.remove(id);
                        bleedAttacker.remove(id);
                        bleedCrimsonTongue.remove(id);
                        continue;
                    }
                    int stacks = bleedStacks.getOrDefault(id, 0);
                    if (stacks <= 0) continue;

                    double dmg = 0.5 + 0.15 * Math.max(0, stacks - 1);   // L1 stack = 0.5 HP, 20 stacks = 3.35 HP
                    UUID atkId = bleedAttacker.get(id);
                    Player attacker = atkId == null ? null : Bukkit.getPlayer(atkId);
                    // aoeDamage routes through AOE_GUARD so the bleed tick doesn't
                    // re-enter handleSwordEnchants and roll another Bleed proc.
                    aoeDamage(victim, dmg, attacker);
                    victim.getWorld().playSound(victim.getLocation(), Sound.HURT_FLESH, 0.5f, 1.2f);
                    // Blood burst — cluster of redstone-block step-sound particles at the
                    // victim's hitbox. Count scales with stacks so a heavy-stack target
                    // visibly fountains. Little vertical spread + a small horizontal
                    // spread to read as a splatter, not a column.
                    spawnBloodBurst(victim, stacks);

                    // Crimson Tongue: attacker heals 1 HP per DOT tick (was per-proc,
                    // now per-tick — matches the mythic weapon's flavor better)
                    if (attacker != null && Boolean.TRUE.equals(bleedCrimsonTongue.get(id))) {
                        attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + 1.0));
                    }
                    // Bloodlust: nearby players wearing the chestplate heal per tick
                    for (Entity near : victim.getNearbyEntities(7, 7, 7)) {
                        if (!(near instanceof Player) || near == victim) continue;
                        Player healer = (Player) near;
                        int blvl = maxArmor(healer, "bloodlust");
                        if (blvl <= 0) continue;
                        healer.setHealth(Math.min(healer.getMaxHealth(), healer.getHealth() + 1.0 * blvl));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /** Find a LivingEntity by UUID across all loaded worlds. 1.8.8 has no
     *  Bukkit.getEntity(UUID). Cheap enough for the Bleed ticker since we only
     *  call it once per bleeding entity per second. */
    private static LivingEntity lookupLiving(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) return p;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e.getUniqueId().equals(id) && e instanceof LivingEntity) return (LivingEntity) e;
            }
        }
        return null;
    }

    /** Strip every negative potion effect from `p` — used by Blessed proc. */
    private void blessSelf(Player p) {
        for (PotionEffectType t : new PotionEffectType[]{
                PotionEffectType.WITHER, PotionEffectType.POISON, PotionEffectType.BLINDNESS,
                PotionEffectType.SLOW, PotionEffectType.SLOW_DIGGING, PotionEffectType.WEAKNESS,
                PotionEffectType.HUNGER, PotionEffectType.CONFUSION}) {
            if (p.hasPotionEffect(t)) p.removePotionEffect(t);
        }
    }
}
