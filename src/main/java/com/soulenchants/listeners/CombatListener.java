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
    /**
     * Live handle to the bound config. Every balance literal in this file now
     * reads from cfg.* — /ce reload re-fills these same fields in place, so
     * tuning flows through without a restart.
     */
    private final com.soulenchants.config.EnchantConfig cfg;
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
     *  Re-procs are blocked while bleedUntil is in the future + 2s grace, so
     *  the DOT always visibly ends before another can start. */
    private final Map<UUID, Integer> bleedStacks = new HashMap<>();
    private final Map<UUID, Long> bleedUntil = new HashMap<>();
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

    // ── v1.2 new-enchant state maps ──────────────────────────────────────
    /** Exsanguinate DoT — per-victim expiry + original attacker credit. */
    private final Map<UUID, Long> exsangUntil    = new HashMap<>();
    private final Map<UUID, UUID> exsangAttacker = new HashMap<>();
    /** Hunter's Mark — per-victim: which attacker marked, and when it expires. */
    private final Map<UUID, UUID> markedBy    = new HashMap<>();
    private final Map<UUID, Long> markedUntil = new HashMap<>();
    /** Overwhelm — per-attacker: current streak victim, stack count, last hit. */
    private final Map<UUID, UUID>    owVictim = new HashMap<>();
    private final Map<UUID, Integer> owStacks = new HashMap<>();
    private final Map<UUID, Long>    owLast   = new HashMap<>();
    /** Soul Warden — per-victim cooldown on the Regen proc. */
    private final Map<UUID, Long>    soulWardenCd = new HashMap<>();
    /** Rage (ported from Nordic) — per-attacker streak state: victim UUID, stack
     *  count, last-hit timestamp. 30 s decay. Resets when the attacker switches
     *  victims or when a victim is hit by a different player. PvP-only —
     *  onHitMob is a no-op for this enchant. */
    private final Map<UUID, UUID>    rageVictim = new HashMap<>();
    private final Map<UUID, Integer> rageStacks = new HashMap<>();
    private final Map<UUID, Long>    rageLast   = new HashMap<>();
    private static final int  RAGE_MAX_STACKS    = 10;
    private static final long RAGE_DECAY_MS      = 30_000L;

    /** Action-bar indicator for the attacker's current Rage stack count.
     *  Bar ramps GREEN → YELLOW → GOLD → RED as stacks climb to 10. */
    private static void sendRageActionbar(Player attacker, int stacks) {
        int cap = RAGE_MAX_STACKS;
        String color = stacks <= 3 ? "§a"
                : stacks <= 5 ? "§e"
                : stacks <= 7 ? "§6"
                : "§c";
        StringBuilder bar = new StringBuilder();
        bar.append(color);
        for (int i = 0; i < stacks; i++) bar.append("▉");
        bar.append("§8");
        for (int i = stacks; i < cap; i++) bar.append("▉");
        String text = "§c§l⚔ RAGE §7" + bar + " " + color + stacks + "§7/" + cap;
        com.soulenchants.util.ActionBar.send(attacker, text);
    }
    /** Anti-Healing debuff — keyed by (victim, source). Each source (bleed,
     *  severance, reapingslash, …) tracks its own {pct, until} independently;
     *  sources stack MULTIPLICATIVELY with diminishing returns, so 30% + 20%
     *  combine as 1 - (1 - .30)(1 - .20) = 44%, not 50%. Applied by Bleed L4+
     *  (10/20/30% at L4/5/6) and by the two dedicated AH enchants Severance
     *  (sword) / Reaping Slash (axe). Static so boss self-heal sites
     *  (Veilweaver / Ironheart / Oakenheart / CustomMob lifesteal abilities) can
     *  query via scaleHealForAntiHeal() — those sites call setHealth() directly
     *  and never fire EntityRegainHealthEvent. */
    private static final class AHEntry {
        final double pct; final long until;
        AHEntry(double pct, long until) { this.pct = pct; this.until = until; }
    }
    private static final Map<UUID, Map<String, AHEntry>> antiHealBySource = new HashMap<>();

    /** Combined active anti-heal % for this victim — expires stale entries
     *  in-place and returns the multiplicative reduction 1 - Π(1 - pct_i). */
    private static double effectiveAntiHealPct(UUID id) {
        Map<String, AHEntry> bySource = antiHealBySource.get(id);
        if (bySource == null || bySource.isEmpty()) return 0.0;
        long now = System.currentTimeMillis();
        double remaining = 1.0;
        java.util.Iterator<Map.Entry<String, AHEntry>> it = bySource.entrySet().iterator();
        while (it.hasNext()) {
            AHEntry en = it.next().getValue();
            if (en.until <= now) { it.remove(); continue; }
            remaining *= (1.0 - en.pct);
        }
        if (bySource.isEmpty()) antiHealBySource.remove(id);
        return 1.0 - remaining;
    }

    /** Scale a heal amount by the victim's active anti-heal %, if any. Boss
     *  self-heal sites that call setHealth() directly must wrap the heal
     *  amount with this helper — EntityRegainHealthEvent isn't fired for
     *  direct setHealth() writes, so the event-based listener below never
     *  catches boss regen. */
    public static double scaleHealForAntiHeal(org.bukkit.entity.LivingEntity victim, double amount) {
        if (victim == null || amount <= 0) return amount;
        double pct = effectiveAntiHealPct(victim.getUniqueId());
        if (pct <= 0) return amount;
        return Math.max(0, amount * (1.0 - pct));
    }

    public CombatListener(SoulEnchants plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getEnchantConfig();
        // Register every UUID-keyed cache with MapManager so quit cleanup is
        // automatic. Previously each map needed a manual .remove(id) inside
        // onQuitClearWrath; missing a call meant a leak. Labels feed /ce debug.
        com.soulenchants.util.MapManager.registerMap(bleedStacks,       "bleedStacks");
        com.soulenchants.util.MapManager.registerMap(bleedUntil,        "bleedUntil");
        com.soulenchants.util.MapManager.registerMap(bleedAttacker,     "bleedAttacker");
        com.soulenchants.util.MapManager.registerMap(bleedCrimsonTongue,"bleedCrimsonTongue");
        com.soulenchants.util.MapManager.registerMap(antiKbCd,          "antiKbCd");
        com.soulenchants.util.MapManager.registerMap(lastCombatTick,    "lastCombatTick");
        com.soulenchants.util.MapManager.registerMap(naturesWrathCd,    "naturesWrathCd");
        com.soulenchants.util.MapManager.registerSet(naturesWrathRooted,"naturesWrathRooted");
        com.soulenchants.util.MapManager.registerMap(exsangUntil,       "exsangUntil");
        com.soulenchants.util.MapManager.registerMap(exsangAttacker,    "exsangAttacker");
        com.soulenchants.util.MapManager.registerMap(markedBy,          "markedBy");
        com.soulenchants.util.MapManager.registerMap(markedUntil,       "markedUntil");
        com.soulenchants.util.MapManager.registerMap(owVictim,          "overwhelmVictim");
        com.soulenchants.util.MapManager.registerMap(owStacks,          "overwhelmStacks");
        com.soulenchants.util.MapManager.registerMap(owLast,            "overwhelmLast");
        com.soulenchants.util.MapManager.registerMap(soulWardenCd,      "soulWardenCd");
        com.soulenchants.util.MapManager.registerMap(rageVictim,        "rageVictim");
        com.soulenchants.util.MapManager.registerMap(rageStacks,        "rageStacks");
        com.soulenchants.util.MapManager.registerMap(rageLast,          "rageLast");
        com.soulenchants.util.MapManager.registerMap(antiHealBySource,  "antiHealBySource");
        startBleedTicker();
        startExsanguinateTicker();
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

        // Rage — the attacker's own rage stack resets when they take damage.
        // Rage only builds when you're the aggressor landing free hits; any
        // time something bites back, the streak is broken.
        UUID vid = victim.getUniqueId();
        if (rageStacks.containsKey(vid)) {
            rageVictim.remove(vid);
            rageStacks.remove(vid);
            rageLast.remove(vid);
        }

        // Mask — fire immunity cancels fire-source damage entirely.
        if (com.soulenchants.masks.MaskEffects.isFireImmune(victim)) {
            EntityDamageEvent.DamageCause c = e.getCause();
            if (c == EntityDamageEvent.DamageCause.FIRE
                    || c == EntityDamageEvent.DamageCause.FIRE_TICK
                    || c == EntityDamageEvent.DamageCause.LAVA) {
                e.setCancelled(true);
                if (victim.getFireTicks() > 0) victim.setFireTicks(0);
                return;
            }
        }

        // Mask — scale incoming damage by the mask's damage-reduction power.
        // Runs BEFORE phoenix/soul-shield so those decisions are made on
        // post-mitigation damage, not raw.
        boolean isExplosion = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                           || e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
        double maskIncoming = com.soulenchants.masks.MaskEffects.incomingMultiplier(victim, isExplosion);
        if (maskIncoming != 1.0) e.setDamage(e.getDamage() * maskIncoming);

        // Featherweight is now a SWORD enchant (Haste burst on hit, Nordic-style)
        // and no longer reduces fall damage.

        // Ironclad — explosion damage reduction, stackable-cap per cfg
        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            int ic = maxArmor(victim, "ironclad");
            if (ic > 0) e.setDamage(e.getDamage() * (1.0
                    - Math.min(cfg.ironcladReductionCap, cfg.ironcladReductionPerLevel * ic)));
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

    // Offensive bonus cap moved to EnchantConfig.offensiveBonusCap — read via cfg.

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
            double heal = Math.min(e.getDamage() * cfg.lifestealHealPerLevelPct * ls, cfg.lifestealHealCapHp);
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

        // Bleed — proc forbidden while the victim is ALREADY bleeding. This
        // breaks the chain that came from prior tunings: a 6s DOT with rapid
        // re-rolls almost always got refreshed before it lapsed, so the DOT
        // visibly never ended. Now Bleed is a discrete event: hit until the
        // victim bleeds, watch the DOT play out, then a re-proc is possible.
        // Proc rate is also way down so even fresh victims rarely roll it.
        int bleed = ItemUtil.getLevel(hand, "bleed");
        if (bleed > 0) {
            int dw = ItemUtil.getLevel(hand, "deepwounds");
            double procChance = cfg.bleedProcPerLevel * bleed + cfg.bleedDeepWoundsBonus * dw;
            long now = System.currentTimeMillis();
            long activeUntil = bleedUntil.getOrDefault(victim.getUniqueId(), 0L);
            boolean inLockout = now < activeUntil + cfg.bleedLockoutGraceMs;
            if (!inLockout && rng.nextDouble() < procChance) {
                applyBleedProc(victim, attacker, hand);
            }
        }

        // Cripple — proc + short CD per attacker.
        // Gated by Dawnbringer aura (blocks all non-soul debuffs).
        int cr = ItemUtil.getLevel(hand, "cripple");
        if (cr > 0 && rng.nextDouble() < cfg.crippleProc * cr
                && plugin.getCooldownManager().isReady("cripple", attacker.getUniqueId())
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.SLOW)) {
            plugin.getCooldownManager().set("cripple", attacker.getUniqueId(), cfg.crippleCdMs);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,     cfg.crippleSlowTicks, cr - 1));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, cfg.crippleSlowTicks, cr - 1));
        }

        // Venom — Poison proc. Clarity + Dawnbringer both block.
        int ven = ItemUtil.getLevel(hand, "venom");
        if (ven > 0 && rng.nextDouble() < cfg.venomProc * ven
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.POISON))
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40 * ven, 0));

        // Wither Bane → additive
        int wb = ItemUtil.getLevel(hand, "witherbane");
        if (wb > 0 && isWitherFamily(victim)) offBonus += cfg.witherbaneBonus * wb;

        // Demon Slayer → additive
        int demon = ItemUtil.getLevel(hand, "demonslayer");
        if (demon > 0 && isNetherMob(victim)) offBonus += cfg.demonSlayerBonus * demon;

        // Beast Slayer → additive
        int beast = ItemUtil.getLevel(hand, "beastslayer");
        if (beast > 0 && isArthropod(victim)) offBonus += cfg.beastSlayerBonus * beast;

        // Executioner → additive, fires when victim is below HP threshold
        int exec = ItemUtil.getLevel(hand, "executioner");
        if (exec > 0 && victim.getHealth() / victim.getMaxHealth() < cfg.executionerHpThreshold)
            offBonus += cfg.executionerBonus * exec;

        // Slayer — additive bonus vs active bosses + their minions, PLUS a
        // flat TRUE-damage add that bypasses armor. The additive bonus alone
        // multiplies post-armor damage, which gets crushed by diamond-armored
        // bosses — the TRUE-damage add is what actually makes Slayer feel
        // like an anti-boss enchant.
        int slayer = ItemUtil.getLevel(hand, "slayer");
        if (slayer > 0 && isBossOrMinionTarget(victim)) {
            offBonus += cfg.slayerBonus * slayer;
            flatAdd  += 5.0 * slayer;   // L3 = +15 TRUE dmg per hit vs bosses
        }

        // Holy Smite — additive bonus vs undead
        int holy = ItemUtil.getLevel(hand, "holysmite");
        if (holy > 0 && isUndead(victim)) offBonus += cfg.holySmiteBonus * holy;

        // Cleave (Nordic-style) — flat chance AoE hit. AOE_GUARD via aoeDamage()
        // prevents recursive cleave-of-cleave; CLEAVE_GUARD retained as belt-
        // and-suspenders on top.
        int cleave = ItemUtil.getLevel(hand, "cleave");
        if (cleave > 0 && !CLEAVE_GUARD.get() && rng.nextDouble() < cfg.cleaveProc) {
            CLEAVE_GUARD.set(true);
            try {
                for (Entity near : attacker.getNearbyEntities(cleave, 10.0, cleave)) {
                    if (!(near instanceof LivingEntity) || near.equals(attacker) || near.equals(victim)) continue;
                    if (!isValidProcTarget((LivingEntity) near)) continue;
                    aoeDamage((LivingEntity) near, cfg.cleaveDmg, attacker);
                }
            } finally {
                CLEAVE_GUARD.set(false);
            }
            // Wraithcleaver (mythic_held = 2) — heal per Cleave proc
            if (ItemUtil.getLevel(hand, "mythic_held") == 2) {
                attacker.setHealth(Math.min(attacker.getMaxHealth(),
                        attacker.getHealth() + plugin.getMythicConfig().wraithcleaverHealPerCleave));
            }
        }

        // Frost Aspect — proc Slow + Mining Fatigue. Gated by Dawnbringer.
        int frost = ItemUtil.getLevel(hand, "frostaspect");
        if (frost > 0 && rng.nextDouble() < cfg.frostProc * frost
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.SLOW)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40 * frost, frost - 1));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40 * frost, frost - 1));
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.PACKED_ICE.getId());
        }

        // Cursed Edge — proc Wither II. Gated by Dawnbringer.
        int curse = ItemUtil.getLevel(hand, "cursededge");
        if (curse > 0 && rng.nextDouble() < cfg.cursedEdgeProc * curse
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.WITHER))
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));

        // Soul Burn — flat add stays additive AFTER multiplier so the cap doesn't eat it
        int sb = ItemUtil.getLevel(hand, "soulburn");
        if (sb > 0 && rng.nextDouble() < cfg.soulBurnProc * sb) {
            victim.setFireTicks(cfg.soulBurnFireTicks * sb);
            flatAdd += cfg.soulBurnFlatAdd * sb;
        }

        // Phantom Strike — teleport-behind mob (player targets excluded)
        int ps = ItemUtil.getLevel(hand, "phantomstrike");
        if (ps > 0 && rng.nextDouble() < cfg.phantomStrikeProc * ps && !(victim instanceof Player)) {
            Location behind = victim.getLocation().add(victim.getLocation().getDirection().multiply(-1.2));
            behind.setYaw(victim.getLocation().getYaw());
            attacker.teleport(behind);
            attacker.getWorld().playEffect(behind, Effect.PORTAL, 0);
        }

        // Earthshaker — AoE around victim
        int es = ItemUtil.getLevel(hand, "earthshaker");
        if (es > 0) {
            double aoeDmg = cfg.earthshakerFlatBase + (cfg.earthshakerFlatPerLevel * es);
            for (Entity near : victim.getNearbyEntities(cfg.earthshakerRadius, 1.5, cfg.earthshakerRadius)) {
                if (!(near instanceof LivingEntity) || near.equals(attacker) || near.equals(victim)) continue;
                if (near instanceof Player) continue;
                aoeDamage((LivingEntity) near, aoeDmg, attacker);
            }
        }

        // Bonebreaker — proc Weakness. Gated by Dawnbringer.
        int bb = ItemUtil.getLevel(hand, "bonebreaker");
        if (bb > 0 && rng.nextDouble() < cfg.bonebreakerProc * bb
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.WEAKNESS))
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60 * bb, 0));

        // Critical Strike — proc-gated additive bonus
        int crit = ItemUtil.getLevel(hand, "criticalstrike");
        if (crit > 0 && rng.nextDouble() < cfg.critProc * crit) {
            offBonus += cfg.critBonus;
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
                    aoeDamage((LivingEntity) near, cfg.razorWindPerLevel * rw, attacker);
                }
            }
        }

        // Greedy — handled in onKill below
        // Headhunter — handled in onKill below

        // Drunk's strength buff is applied passively in tick task.
        // Bloodlust is a CHESTPLATE proc (heal-on-nearby-bleed-tick) — handled by the global Bleed ticker.

        // Feather Weight — proc Haste burst
        int fw = ItemUtil.getLevel(hand, "featherweight");
        if (fw > 0 && !attacker.hasPotionEffect(PotionEffectType.FAST_DIGGING)
                && rng.nextDouble() <= cfg.featherProc * fw) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, fw * 20, fw - 1, true, false), true);
        }

        // Blessed — proc strips own debuffs. Message throttled per cfg.blessedMsgCdMs.
        int blessed = ItemUtil.getLevel(hand, "blessed");
        if (blessed > 0 && rng.nextDouble() < cfg.blessedProc * blessed) {
            blessSelf(attacker);
            if (plugin.getCooldownManager().isReady("blessed_msg", attacker.getUniqueId())) {
                plugin.getCooldownManager().set("blessed_msg", attacker.getUniqueId(), cfg.blessedMsgCdMs);
                attacker.sendMessage("§e§l** BLESSED **");
                attacker.playSound(attacker.getLocation(), org.bukkit.Sound.SPLASH, 1.2f, 2.0f);
            }
        }

        // ─── AXE enchants (PvE/PvP, fire whether wielding sword OR axe) ───

        // Reaver → additive, scales with attacker's missing HP
        int reaver = ItemUtil.getLevel(hand, "reaver");
        if (reaver > 0) {
            double missingPct = 1.0 - (attacker.getHealth() / attacker.getMaxHealth());
            offBonus += cfg.reaverBonus * reaver * missingPct;
        }
        // Skullcrush — proc Nausea + Weakness II. Previously player-only; now
        // fires against bosses + minions too (victim is already gated by
        // isValidProcTarget at the top of this method).
        // Skullcrush — proc Nausea + Weakness II. Gated by Dawnbringer.
        int skull = ItemUtil.getLevel(hand, "skullcrush");
        if (skull > 0 && rng.nextDouble() < cfg.skullcrushProc * skull
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.WEAKNESS)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 60 * skull, 0));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,  60 * skull, 1));
        }
        // Hamstring — root victim (Slow IV) for 2s. Gated by Dawnbringer.
        int ham = ItemUtil.getLevel(hand, "hamstring");
        if (ham > 0 && rng.nextDouble() < cfg.hamstringProc * ham
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.SLOW)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 3));
        }
        // Blood Fury — heal scaled to damage dealt while below HP threshold
        int bf = ItemUtil.getLevel(hand, "bloodfury");
        if (bf > 0 && attacker.getHealth() < attacker.getMaxHealth() * cfg.bloodFuryHpThreshold) {
            double heal = Math.min(e.getDamage() * (cfg.bloodFuryHealPct * bf), cfg.bloodFuryHealCapHp);
            attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
        }
        // Shieldbreaker — proc TRUE bonus damage (ignores armor). Sourceless damage
        // call still fires EntityDamageEvent without a damager, so it doesn't
        // re-enter onAttack — but guard anyway since handleArmorOnHit/onAnyDamage
        // would still process it.
        int sb2 = ItemUtil.getLevel(hand, "shieldbreaker");
        if (sb2 > 0 && rng.nextDouble() < cfg.shieldbreakerProc * sb2) {
            double trueDmg = e.getDamage() * cfg.shieldbreakerTrueDmgPct;
            aoeDamage(victim, trueDmg, null);
        }
        // Frostshatter — Slow III + Mining Fatigue III for 4s. Gated by Dawnbringer.
        int fs = ItemUtil.getLevel(hand, "frostshatter");
        if (fs > 0 && rng.nextDouble() < cfg.frostshatterProc * fs
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.SLOW)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 2));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 80, 2));
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.PACKED_ICE.getId());
        }
        // Wraithcleave → additive, only if victim is alone within configured radius
        int wc = ItemUtil.getLevel(hand, "wraithcleave");
        if (wc > 0) {
            boolean alone = true;
            double r = cfg.wraithcleaveRadius;
            for (Entity near : victim.getNearbyEntities(r, r, r)) {
                if (near == attacker) continue;
                if (near instanceof LivingEntity && !(near instanceof Player)) {
                    alone = false; break;
                }
            }
            if (alone) offBonus += cfg.wraithcleaveBonus * wc;
        }
        // Rending Blow — Wither III for 5s. Gated by Dawnbringer.
        int rb = ItemUtil.getLevel(hand, "rendingblow");
        if (rb > 0 && rng.nextDouble() < cfg.rendingBlowProc * rb
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.WITHER)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2));
        }
        // Executioner's Mark → additive when victim has any debuff
        int em = ItemUtil.getLevel(hand, "executionersmark");
        if (em > 0) {
            boolean hasDebuff = victim.hasPotionEffect(PotionEffectType.SLOW)
                    || victim.hasPotionEffect(PotionEffectType.WEAKNESS)
                    || victim.hasPotionEffect(PotionEffectType.WITHER)
                    || victim.hasPotionEffect(PotionEffectType.POISON)
                    || victim.hasPotionEffect(PotionEffectType.SLOW_DIGGING)
                    || victim.hasPotionEffect(PotionEffectType.CONFUSION)
                    || victim.hasPotionEffect(PotionEffectType.BLINDNESS);
            if (hasDebuff) offBonus += cfg.executionersMarkBonus * em;
        }

        // ─── v1.2 AXE debuffs (PvE-heavy, Dawnbringer-gated like other debuffs) ───

        // Marrowbreak — Weakness II proc
        int mbrk = ItemUtil.getLevel(hand, "marrowbreak");
        if (mbrk > 0 && rng.nextDouble() < 0.25 * mbrk
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.WEAKNESS))
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));

        // Crushing Blow — Slow III proc
        int cbl = ItemUtil.getLevel(hand, "crushingblow");
        if (cbl > 0 && rng.nextDouble() < 0.20 * cbl
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.SLOW))
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2));

        // Pulverize — Nausea III + Slow II proc
        int pv = ItemUtil.getLevel(hand, "pulverize");
        if (pv > 0 && rng.nextDouble() < 0.15 * pv
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul(victim, PotionEffectType.SLOW)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 80, 2));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,      80, 1));
        }

        // Exsanguinate — start/refresh 5s true-damage DOT (1 HP/s, ticks in startExsanguinateTicker)
        int exs = ItemUtil.getLevel(hand, "exsanguinate");
        if (exs > 0 && rng.nextDouble() < 0.10 * exs) {
            long now = System.currentTimeMillis();
            exsangUntil.put(victim.getUniqueId(), now + 5_000L);
            exsangAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
        }

        // Hunter's Mark — on first hit this attacker marks victim for 10s; subsequent hits
        // on the marked victim by the SAME attacker get +12%/lvl bonus.
        int hm = ItemUtil.getLevel(hand, "huntersmark");
        if (hm > 0) {
            UUID atkId = attacker.getUniqueId();
            UUID vid   = victim.getUniqueId();
            long now   = System.currentTimeMillis();
            UUID marker = markedBy.get(vid);
            Long expiry = markedUntil.get(vid);
            boolean alreadyMine = marker != null && marker.equals(atkId) && expiry != null && expiry > now;
            if (alreadyMine) {
                offBonus += 0.12 * hm;
            } else {
                markedBy.put(vid, atkId);
                markedUntil.put(vid, now + 10_000L);
            }
        }

        // Severance (sword) — dedicated anti-heal proc. 20%/lvl chance, 25% AH for 5s.
        int sv = ItemUtil.getLevel(hand, "severance");
        if (sv > 0 && rng.nextDouble() < 0.20 * sv) {
            applyAntiHeal(victim, "severance", 0.25, 5_000L);
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
        }

        // Reaping Slash (axe) — rarer but stronger AH. 15%/lvl chance, 40% AH for 6s.
        int rs2 = ItemUtil.getLevel(hand, "reapingslash");
        if (rs2 > 0 && rng.nextDouble() < 0.15 * rs2) {
            applyAntiHeal(victim, "reapingslash", 0.40, 6_000L);
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
        }

        // Rage (Nordic port) — consecutive hits on the same target stack
        // +(level × stack × 2) bonus damage per hit. 10 stacks max, 30s decay.
        // Works in BOTH PvP and PvE. Stacks reset if we switch victims or if
        // our victim is hit by someone else (scan below).
        int rage = ItemUtil.getLevel(hand, "rage");
        if (rage > 0) {
            UUID atkId = attacker.getUniqueId();
            UUID vid   = victim.getUniqueId();
            long now   = System.currentTimeMillis();
            // Reset any OTHER attacker's streak against this victim — Nordic's
            // "victim was hit by someone else → previous attacker's rage resets"
            // rule. Small data; O(n) scan is fine.
            for (java.util.Iterator<Map.Entry<UUID, UUID>> it = rageVictim.entrySet().iterator();
                 it.hasNext();) {
                Map.Entry<UUID, UUID> en = it.next();
                if (!en.getKey().equals(atkId) && en.getValue().equals(vid)) {
                    rageStacks.remove(en.getKey());
                    rageLast.remove(en.getKey());
                    it.remove();
                }
            }
            UUID prev  = rageVictim.get(atkId);
            Long last  = rageLast.get(atkId);
            int stacks;
            if (prev == null || !prev.equals(vid) || last == null || now - last > RAGE_DECAY_MS) {
                // First hit against this victim — initialize streak at 1, no bonus yet.
                rageVictim.put(atkId, vid);
                rageStacks.put(atkId, 1);
                rageLast.put(atkId, now);
                stacks = 1;
            } else {
                stacks = Math.min(RAGE_MAX_STACKS, rageStacks.getOrDefault(atkId, 1));
                double bonus = (rage * stacks) * 2.0;
                flatAdd += bonus;
                rageStacks.put(atkId, Math.min(RAGE_MAX_STACKS, stacks + 1));
                rageLast.put(atkId, now);
                // Redstone-dust burst at victim — Nordic signature cue.
                if (stacks > 1) {
                    for (int i = 0; i < 6; i++) {
                        victim.getWorld().playEffect(victim.getLocation().add(
                                (rng.nextDouble() - 0.5) * 0.8,
                                0.6 + rng.nextDouble() * 0.6,
                                (rng.nextDouble() - 0.5) * 0.8),
                                Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
                    }
                }
            }
            // Actionbar stack indicator — colored bar that scales with stack count.
            sendRageActionbar(attacker, stacks);
        }

        // Overwhelm — consecutive hits on the same target stack 6%/lvl per stack, capped at 5.
        // Stacks reset on target switch or 3s no-hit gap.
        int ow = ItemUtil.getLevel(hand, "overwhelm");
        if (ow > 0) {
            UUID atkId = attacker.getUniqueId();
            UUID vid   = victim.getUniqueId();
            long now   = System.currentTimeMillis();
            UUID prev  = owVictim.get(atkId);
            Long last  = owLast.get(atkId);
            int stacks;
            if (prev == null || !prev.equals(vid) || last == null || now - last > 3_000L) {
                stacks = 1;
            } else {
                stacks = Math.min(5, owStacks.getOrDefault(atkId, 0) + 1);
            }
            owVictim.put(atkId, vid);
            owStacks.put(atkId, stacks);
            owLast.put(atkId, now);
            offBonus += 0.06 * stacks * ow;
        }

        // Soul Strike — bypass-cap special multiplier (rare proc + soul cost = self-throttled).
        // Gem-gated via SoulGemUtil.
        int ss = ItemUtil.getLevel(hand, "soulstrike");
        if (ss > 0 && rng.nextDouble() < cfg.soulStrikeProc) {
            int cost = cfg.soulStrikeCost;
            if (com.soulenchants.items.SoulGemUtil.chargeSoulCost(plugin, attacker, cost)) {
                specialMult *= (cfg.soulStrikeMultBase + cfg.soulStrikeMultPerLevel * ss);
                attacker.getWorld().strikeLightningEffect(victim.getLocation());
                attacker.sendMessage("§4✦ Soul Strike! §7(-" + cost + ")");
            }
        }

        // Soul Drain — gem-gated heal per hit
        int sd = ItemUtil.getLevel(hand, "souldrain");
        if (sd > 0) {
            int cost = cfg.soulDrainCost;
            if (com.soulenchants.items.SoulGemUtil.chargeSoulCost(plugin, attacker, cost)) {
                double heal = e.getDamage() * (cfg.soulDrainHealPct * sd);
                attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
            }
        }

        // Divine Immolation — gem-gated, soul-cost-throttled splash every swing
        int di = ItemUtil.getLevel(hand, "divineimmolation");
        if (di > 0 && com.soulenchants.items.SoulGemUtil.chargeSoulCost(plugin, attacker, cfg.divineImmolationCost)) {
            double splash = di * cfg.divineImmolationSplashPerLevel;
            double radius = di;
            for (Entity near : victim.getNearbyEntities(radius, radius, radius)) {
                if (near == attacker) continue;
                if (!(near instanceof LivingEntity)) continue;
                LivingEntity le = (LivingEntity) near;
                if (!isValidProcTarget(le)) continue;
                aoeDamage(le, splash, attacker);
                Location l = le.getLocation().add(0, 1, 0);
                l.getWorld().playEffect(l, Effect.MOBSPAWNER_FLAMES, 0);
                if (le instanceof Player) com.soulenchants.lunar.LunarFx.notify((Player) le,
                        "§c§l✦ DIVINE IMMOLATION", "§7Splash damage from divine fire", 2500L);
            }
            attacker.getWorld().playSound(attacker.getLocation(), org.bukkit.Sound.FIREWORK_BLAST, 1.0f, 0.4f);
        }

        // ──────────────────────────────────────────────────────────────
        // FINAL DAMAGE APPLICATION
        // base * (1 + min(offBonus, CAP)) * specialMult + flatAdd
        // final step: scale by any mask outgoing multiplier
        // ──────────────────────────────────────────────────────────────
        double cappedBonus = Math.min(offBonus, cfg.offensiveBonusCap);
        double finalDmg = baseDmg * (1.0 + cappedBonus) * specialMult + flatAdd;
        double maskMult = com.soulenchants.masks.MaskEffects.outgoingMultiplier(
                attacker, victim instanceof Player);
        if (maskMult != 1.0) finalDmg *= maskMult;
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

        // Armored — stackable across chestplate + leggings. Proc + reduction
        // scales with summed level; both values hard-capped per cfg.
        if (armoredSum > 0 && e.getDamager() instanceof Player) {
            ItemStack hand = ((Player) e.getDamager()).getItemInHand();
            if (hand != null && hand.getType().name().endsWith("_SWORD")) {
                double proc = Math.min(cfg.armoredProcCap, cfg.armoredProcPerLevel * armoredSum);
                if (rng.nextDouble() < proc) {
                    double reduction = Math.min(cfg.armoredReductionCap,
                            cfg.armoredReductionPerLevel * armoredSum);
                    e.setDamage(e.getDamage() * (1.0 - reduction));
                }
            }
        }

        // Enlightened — stackable proc, hard-capped per cfg.enlightenedProcCap.
        if (enlightenedSum > 0) {
            double proc = Math.min(cfg.enlightenedProcCap, cfg.enlightenedProcPerLevel * enlightenedSum);
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

        // Hardened — proc restores durability across all armor pieces.
        // Each piece's durability shifts down by (level+2). Lower durability
        // values = more uses left in 1.8.x's anvil-side numbering.
        if (hardened > 0 && rng.nextDouble() < cfg.hardenedProc * hardened) {
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
                final double reduceTo = 1.0 - (cfg.antiknockbackReduction * antikb);
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
        // Molten — proc ignite attacker. Gated by Dawnbringer aura on the attacker.
        if (molten > 0 && e.getDamager() instanceof LivingEntity
                && rng.nextDouble() < cfg.moltenProc * molten
                && !com.soulenchants.util.DebuffImmunity.isImmuneNonSoul((LivingEntity) e.getDamager(), null))
            ((LivingEntity) e.getDamager()).setFireTicks(cfg.moltenFireTicks * molten);
        if (lastStand > 0 && victim.getHealth() <= cfg.lastStandHpThreshold)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30, Math.max(0, lastStand - 2)));
        // Stormcaller — proc + CD. CD scales down with level (faster procs at higher level).
        if (stormcall > 0 && e.getDamage() >= cfg.stormcallerMinDmg && e.getDamager() instanceof LivingEntity
                && plugin.getCooldownManager().isReady("stormcaller", id)
                && rng.nextDouble() < cfg.stormcallerProc * stormcall) {
            long cd = cfg.stormcallerCdBaseMs - (cfg.stormcallerCdReduceMs * (stormcall - 1));
            plugin.getCooldownManager().set("stormcaller", id, cd);
            e.getDamager().getWorld().strikeLightning(e.getDamager().getLocation());
        }
        // Guardians — rare Absorption proc
        if (guardians > 0 && plugin.getCooldownManager().isReady("guardians", id)
                && rng.nextDouble() < cfg.guardiansProc * guardians) {
            plugin.getCooldownManager().set("guardians", id, cfg.guardiansCdMs);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 0));
            victim.sendMessage("§6✦ §eGuardians §7— §a+1 absorption heart §7("
                    + (cfg.guardiansCdMs / 1000) + "s CD)");
        }
        // Overshield — rare Absorption proc
        if (overshield > 0 && plugin.getCooldownManager().isReady("overshield", id)
                && rng.nextDouble() < cfg.overshieldProc * overshield) {
            plugin.getCooldownManager().set("overshield", id, cfg.overshieldCdMs);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 0));
            victim.sendMessage("§6✦ §eOvershield §7— §a+1 absorption heart §7("
                    + (cfg.overshieldCdMs / 1000) + "s CD)");
        }
        if (reflect > 0 && e.getDamager() instanceof LivingEntity
                && plugin.getCooldownManager().isReady("reflect", id)) {
            plugin.getCooldownManager().set("reflect", id, cfg.reflectCdMs);
            aoeDamage((LivingEntity) e.getDamager(), e.getDamage() * (cfg.reflectPctPerLevel * reflect), victim);
        }
        // Endurance — stackable reduction while recently in combat (capped per cfg)
        if (endurance > 0) {
            long now = System.currentTimeMillis();
            long last = lastCombatTick.getOrDefault(id, 0L);
            if (now - last < cfg.enduranceCombatWindowMs)
                e.setDamage(e.getDamage() * (1.0
                        - Math.min(cfg.enduranceReductionCap, cfg.enduranceReductionPerLevel * endurance)));
            lastCombatTick.put(id, now);
        }
        if (vengeance > 0 && e.getDamager() instanceof LivingEntity)
            aoeDamage((LivingEntity) e.getDamager(), cfg.vengeanceBase + cfg.vengeancePerLevel * vengeance, victim);
        // Nature's Wrath — 2% on-hit proc, 10s per-victim CD. Roots all enemy
        // LivingEntities in level*3 radius for (5+level)s + lightning every
        // second for `level` damage. 75 souls per cast. Soul-tier ARMOR.
        if (naturesWrath > 0 && e.getDamager() instanceof LivingEntity && rng.nextDouble() < cfg.naturesWrathProc) {
            long now = System.currentTimeMillis();
            long lastCast = naturesWrathCd.getOrDefault(id, 0L);
            if (now - lastCast >= cfg.naturesWrathCdMs
                    && com.soulenchants.items.SoulGemUtil.chargeSoulCost(plugin, victim, cfg.naturesWrathCost)) {
                naturesWrathCd.put(id, now);
                triggerNaturesWrath(victim, naturesWrath);
            }
        }

        // Soul Burst — gem-gated AoE knockback + damage
        if (soulburst > 0) {
            int cost = cfg.soulBurstCost;
            if (com.soulenchants.items.SoulGemUtil.chargeSoulCost(plugin, victim, cost)) {
                double r = cfg.soulBurstRadius;
                for (Entity near : victim.getNearbyEntities(r, 3, r)) {
                    if (!(near instanceof LivingEntity)) continue;
                    Vector push = near.getLocation().toVector().subtract(victim.getLocation().toVector())
                            .setY(0).normalize().multiply(1.2);
                    push.setY(0.5);
                    near.setVelocity(push);
                    ((LivingEntity) near).damage(cfg.soulBurstDmgPerLevel * soulburst, victim);
                }
                victim.getWorld().createExplosion(victim.getLocation().getX(), victim.getLocation().getY(),
                        victim.getLocation().getZ(), 0f, false);
            }
        }

        // ─── New PvP armor enchants ───────────────────────────────────────

        // Callous — proc dodges melee entirely
        if (callous > 0 && e.getDamager() instanceof LivingEntity && rng.nextDouble() < cfg.callousProc * callous) {
            e.setDamage(0);
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
            return;
        }
        // Iron Skin — chance to negate critical hits (attacker airborne heuristic, 1.8 lacks Bukkit crit flag)
        if (ironskin > 0 && e.getDamager() instanceof Player && rng.nextDouble() < cfg.ironskinProc * ironskin) {
            Player atk = (Player) e.getDamager();
            if (atk.getFallDistance() > 0 && !atk.isOnGround()) {
                e.setDamage(e.getDamage() / 1.5); // strip the vanilla 1.5x crit bonus
                victim.sendMessage("§7§l⛨ Iron Skin §8— §fcritical hit absorbed.");
            }
        }
        // Aegis — low-HP Resistance II proc
        if (aegis > 0 && victim.getHealth() <= cfg.aegisHpThreshold
                && plugin.getCooldownManager().isReady("aegis", id)) {
            plugin.getCooldownManager().set("aegis", id, cfg.aegisCdMs);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 80, 1));
            victim.sendMessage("§b§l⛨ AEGIS §7— §fResistance II for 4s");
        }
        // Rush — proc Speed II burst on hit
        if (rush > 0 && plugin.getCooldownManager().isReady("rush", id)
                && rng.nextDouble() < cfg.rushProc * rush) {
            plugin.getCooldownManager().set("rush", id, cfg.rushCdMs);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
            victim.sendMessage("§e§l✦ RUSH §7— §fSpeed II for 3s");
        }
        // Spite — reflect true damage (ignores armor)
        if (spite > 0 && e.getDamager() instanceof LivingEntity) {
            double trueDmg = e.getDamage() * cfg.spitePctPerLevel * spite;
            aoeDamage((LivingEntity) e.getDamager(), trueDmg, null);
        }
        // Vampiric Plate — proc steals HP from attacker
        if (vampiricplate > 0 && e.getDamager() instanceof LivingEntity
                && rng.nextDouble() < cfg.vampiricPlateProc * vampiricplate) {
            LivingEntity atk = (LivingEntity) e.getDamager();
            aoeDamage(atk, cfg.vampiricPlateHeal, null);
            victim.setHealth(Math.min(victim.getMaxHealth(), victim.getHealth() + cfg.vampiricPlateHeal));
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
        }
        // Counter — proc disarm (player-only; bosses + minions can't trigger).
        // The extra custom-mob + boss-minion checks below are belt-and-suspenders
        // against any plugin spawning fake-Players.
        if (counter > 0 && e.getDamager() instanceof Player
                && plugin.getCooldownManager().isReady("counter", id)
                && rng.nextDouble() < cfg.counterProc * counter) {
            Player atk = (Player) e.getDamager();
            // Skip if the "Player" is somehow tagged as a custom mob or registered
            // as an active boss / boss-minion in our trackers.
            if (com.soulenchants.mobs.CustomMob.idOf(atk) != null) return;
            if (isBossOrMinionAttacker(atk)) return;

            plugin.getCooldownManager().set("counter", id, cfg.counterCdMs);
            ItemStack atkHand = atk.getItemInHand();
            if (atkHand != null && atkHand.getType() != Material.AIR) {
                atk.setItemInHand(null);
                atk.getWorld().dropItemNaturally(atk.getLocation().add(0, 0.4, 0), atkHand);
                atk.sendMessage("§c§l⚔ COUNTER §7— §fdisarmed by §e" + victim.getName());
                victim.sendMessage("§b§l⚔ COUNTER §7— §fdisarmed §e" + atk.getName());
            }
        }

        // ─── v1.2 PvE armor enchants ──────────────────────────────────────

        // Radiant Shell — flat -1 per equipped piece with the enchant (caps at -4 for full set).
        int radiantPieces = 0;
        for (ItemStack a : armor) {
            if (a != null && ItemUtil.getLevel(a, "radiantshell") > 0) radiantPieces++;
        }
        if (radiantPieces > 0 && e.getDamage() > 1.0) {
            e.setDamage(Math.max(1.0, e.getDamage() - radiantPieces));
        }

        // Mobslayer's Ward — scale damage down when the hitter is a custom mob.
        int msw = 0;
        for (ItemStack a : armor) msw = Math.max(msw, ItemUtil.getLevel(a, "mobslayersward"));
        if (msw > 0 && e.getDamager() instanceof LivingEntity) {
            LivingEntity raw = (LivingEntity) e.getDamager();
            if (com.soulenchants.mobs.CustomMob.idOf(raw) != null) {
                e.setDamage(e.getDamage() * (1.0 - 0.10 * msw));
            }
        }

        // Soul Warden — Regen proc after mob damage (60s CD).
        ItemStack chest = victim.getInventory().getChestplate();
        int sw = chest == null ? 0 : ItemUtil.getLevel(chest, "soulwarden");
        if (sw > 0 && e.getDamager() instanceof LivingEntity && !(e.getDamager() instanceof Player)) {
            long now = System.currentTimeMillis();
            long last = soulWardenCd.getOrDefault(id, 0L);
            if (now - last > 60_000L) {
                soulWardenCd.put(id, now);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, sw - 1));
                victim.sendMessage("§d§l✦ SOUL WARDEN §7— §fRegeneration for 5s");
            }
        }

        // Dreadmantle — nearby mobs get Weakness when you take a hit.
        ItemStack helm = victim.getInventory().getHelmet();
        int dm = helm == null ? 0 : ItemUtil.getLevel(helm, "dreadmantle");
        if (dm > 0) {
            for (Entity near : victim.getNearbyEntities(8, 4, 8)) {
                if (!(near instanceof LivingEntity) || near instanceof Player) continue;
                ((LivingEntity) near).addPotionEffect(
                        new PotionEffect(PotionEffectType.WEAKNESS, 60, dm - 1, true, false), true);
            }
        }

        // ─── v1.3 god-tier armor enchants ────────────────────────────────

        // Voidwalker — outright dodge chance. Nerfed from 8%/lvl → 4%/lvl
        // (L3 = 12% instead of 24%) — full dodge is a very strong effect and
        // stacking it with Callous made armored players functionally immortal.
        // Fires BEFORE thornback / bulwark so a dodged hit doesn't consume
        // unrelated procs.
        ItemStack bootsVw = victim.getInventory().getBoots();
        int vw = bootsVw == null ? 0 : ItemUtil.getLevel(bootsVw, "voidwalker");
        if (vw > 0 && rng.nextDouble() < 0.04 * vw) {
            e.setDamage(0);
            e.setCancelled(true);
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0), Effect.PORTAL, 0);
            victim.sendMessage("§5§l✦ VOIDWALKER §7— §fphased through the strike");
            return;
        }

        // Bulwark — scale incoming damage down when hitter is a custom mob.
        // Also grants Resistance II while below 40% HP so the wearer has a
        // reliable "I won't get deleted" floor in boss fights.
        ItemStack chestBw = victim.getInventory().getChestplate();
        int bw = chestBw == null ? 0 : ItemUtil.getLevel(chestBw, "bulwark");
        if (bw > 0) {
            if (e.getDamager() instanceof LivingEntity
                    && com.soulenchants.mobs.CustomMob.idOf((LivingEntity) e.getDamager()) != null) {
                e.setDamage(e.getDamage() * (1.0 - 0.06 * bw));
            }
            if (victim.getHealth() / victim.getMaxHealth() < 0.40) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 1, true, false), true);
            }
        }

        // Thornback — reflect summed-across-pieces percent as TRUE damage.
        int thornSum = 0;
        for (ItemStack a : armor) {
            if (a != null) thornSum += ItemUtil.getLevel(a, "thornback");
        }
        if (thornSum > 0 && e.getDamager() instanceof LivingEntity) {
            aoeDamage((LivingEntity) e.getDamager(), e.getDamage() * 0.05 * thornSum, null);
        }

        // Warden's Eye — trace a particle halo on the attacker so they're
        // easy to find through walls / crowds.
        int we = helm == null ? 0 : ItemUtil.getLevel(helm, "wardenseye");
        if (we > 0 && e.getDamager() instanceof LivingEntity) {
            Location mark = e.getDamager().getLocation().add(0, 1, 0);
            for (int i = 0; i < 8; i++)
                mark.getWorld().playEffect(mark, Effect.WITCH_MAGIC, 0);
        }

        // Oathbound — cleanse self of the three most-common CC debuffs when hit.
        int ob = helm == null ? 0 : ItemUtil.getLevel(helm, "oathbound");
        if (ob > 0 && plugin.getCooldownManager().isReady("oathbound", id)
                && (victim.hasPotionEffect(PotionEffectType.SLOW)
                        || victim.hasPotionEffect(PotionEffectType.WEAKNESS)
                        || victim.hasPotionEffect(PotionEffectType.WITHER))) {
            plugin.getCooldownManager().set("oathbound", id, 30_000L);
            victim.removePotionEffect(PotionEffectType.SLOW);
            victim.removePotionEffect(PotionEffectType.WEAKNESS);
            victim.removePotionEffect(PotionEffectType.WITHER);
            victim.sendMessage("§f§l⚔ OATHBOUND §7— §fcleansed");
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0), Effect.MOBSPAWNER_FLAMES, 0);
        }

        // Entombed — low-HP CC burst on attackers (60s CD).
        ItemStack legsEn = victim.getInventory().getLeggings();
        int en = legsEn == null ? 0 : ItemUtil.getLevel(legsEn, "entombed");
        if (en > 0 && victim.getHealth() / victim.getMaxHealth() < 0.30
                && plugin.getCooldownManager().isReady("entombed", id)) {
            plugin.getCooldownManager().set("entombed", id, 60_000L);
            for (Entity near : victim.getNearbyEntities(8, 4, 8)) {
                if (!(near instanceof LivingEntity) || near == victim) continue;
                ((LivingEntity) near).addPotionEffect(
                        new PotionEffect(PotionEffectType.SLOW, 80, 3, true, false), true);
                ((LivingEntity) near).addPotionEffect(
                        new PotionEffect(PotionEffectType.SLOW_DIGGING, 80, 2, true, false), true);
            }
            victim.sendMessage("§5§l✦ ENTOMBED §7— §fattackers rooted");
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (e instanceof org.bukkit.event.entity.PlayerDeathEvent) return;
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        ItemStack hand = killer.getItemInHand();
        if (hand == null) return;

        // Reaper — flat heal on kill
        int reaper = ItemUtil.getLevel(hand, "reaper");
        if (reaper > 0)
            killer.setHealth(Math.min(killer.getMaxHealth(),
                    killer.getHealth() + cfg.reaperHealPerLevel * reaper));

        // Berserker's Edge — refreshable Strength I on kill, duration scales with level
        int be = ItemUtil.getLevel(hand, "berserkersedge");
        if (be > 0) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,
                    be * cfg.berserkersEdgeStrengthSecondsPerLevel * 20, 0), true);
        }

        // Headhunter — proc skull drop on kill
        int hh = ItemUtil.getLevel(hand, "headhunter");
        if (hh > 0 && rng.nextDouble() < cfg.headhunterProc * hh) {
            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 0);
            e.getDrops().add(skull);
        }

        // Greedy — proc duplicate each drop on kill
        int greedy = ItemUtil.getLevel(hand, "greedy");
        if (greedy > 0) {
            java.util.List<ItemStack> bonus = new java.util.ArrayList<>();
            for (ItemStack drop : e.getDrops()) {
                if (drop == null) continue;
                if (rng.nextDouble() < cfg.greedyProc * greedy) {
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
        int radius = (int) (level * cfg.naturesWrathRadiusPerLevel);
        int durTicks = (cfg.naturesWrathDurationSecondsBase + level) * 20;
        java.util.List<Player> rootedPlayers = new java.util.ArrayList<>();
        java.util.List<LivingEntity> rootedMobs = new java.util.ArrayList<>();
        for (Entity near : caster.getNearbyEntities(radius, radius, radius)) {
            if (!(near instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity) near;
            if (le == caster || le.isDead()) continue;
            if (le instanceof Player) {
                Player victim = (Player) le;
                try { victim.setWalkSpeed(0.0f); } catch (Throwable ignored) {}
                victim.removePotionEffect(PotionEffectType.JUMP);
                victim.removePotionEffect(PotionEffectType.SLOW);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,     durTicks, 129, false, false));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,     durTicks, 129, false, false));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durTicks, 3,   false, false));
                naturesWrathRooted.add(victim.getUniqueId());
                rootedPlayers.add(victim);
                victim.playSound(victim.getLocation(), org.bukkit.Sound.ENDERDRAGON_GROWL, 2.0f, 2.0f);
                victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0), Effect.LARGE_SMOKE, 0);
                continue;
            }
            // Custom mobs (bosses + minions) — Slow 129 achieves the functional
            // root since they don't have Player walkSpeed. Skip vanilla mobs so
            // we don't nuke random passive fauna; NW is a combat enchant.
            if (!isValidProcTarget(le)) continue;
            if (com.soulenchants.mobs.CustomMob.idOf(le) == null) continue;
            le.removePotionEffect(PotionEffectType.SLOW);
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,     durTicks, 129, false, false));
            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durTicks, 3,   false, false));
            rootedMobs.add(le);
            le.getWorld().playEffect(le.getLocation().add(0, 1, 0), Effect.LARGE_SMOKE, 0);
        }
        int totalRooted = rootedPlayers.size() + rootedMobs.size();
        com.soulenchants.lunar.LunarFx.notify(caster,
                "§a§l✦ NATURE'S WRATH",
                "§7" + totalRooted + " rooted · §c−75 Souls§7 · §f" + plugin.getSoulManager().get(caster) + "§7 left",
                4500L);
        caster.sendMessage("§a§l** NATURE'S WRATH **");
        caster.sendMessage("§a§l- 75 Soul Gems §7§n" + plugin.getSoulManager().get(caster) + "§7 souls left.");
        if (totalRooted == 0) return;
        // 5-tick lightning storm: every 1s for 5s, strike each rooted target
        // for `level` damage. Players keep their NW-rooted flag; custom mobs
        // just take damage and keep their Slow-129 until it decays naturally.
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 5) {
                    for (Player v : rootedPlayers) restoreFromNaturesWrath(v);
                    cancel();
                    return;
                }
                for (Player v : rootedPlayers) {
                    if (v == null || v.isDead() || !v.isOnline()) continue;
                    v.getWorld().strikeLightningEffect(v.getLocation());
                    v.damage(level);
                    try { v.playSound(v.getLocation(), org.bukkit.Sound.GHAST_SCREAM2, 2.0f, 2.0f); } catch (Throwable ignored) {}
                    v.sendMessage("§2§l** NATURES **");
                }
                for (LivingEntity m : rootedMobs) {
                    if (m == null || m.isDead()) continue;
                    m.getWorld().strikeLightningEffect(m.getLocation());
                    // Route through aoeDamage so our damage event flows cleanly
                    // through AOE_GUARD (won't re-enter handleSwordEnchants).
                    // Caster is the source → boss damage-map credits the caster.
                    aoeDamage(m, level, caster);
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
        bleedAttacker.remove(id);
        bleedCrimsonTongue.remove(id);
        // v1.2 maps
        exsangUntil.remove(id);
        exsangAttacker.remove(id);
        markedBy.remove(id);
        markedUntil.remove(id);
        owVictim.remove(id);
        owStacks.remove(id);
        owLast.remove(id);
        soulWardenCd.remove(id);
        antiHealBySource.remove(id);
        // Rage state — both as attacker and as tracked victim.
        rageVictim.remove(id);
        rageStacks.remove(id);
        rageLast.remove(id);
        rageVictim.values().removeIf(v -> v.equals(id));
    }

    /** 1 Hz DoT that drains Exsanguinate'd victims for 1 HP each tick until expiry. */
    private void startExsanguinateTicker() {
        new BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                java.util.Iterator<Map.Entry<UUID, Long>> it = exsangUntil.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Long> entry = it.next();
                    UUID vid = entry.getKey();
                    if (now > entry.getValue()) {
                        it.remove();
                        exsangAttacker.remove(vid);
                        continue;
                    }
                    LivingEntity victim = lookupLiving(vid);
                    if (victim == null || victim.isDead()) {
                        it.remove();
                        exsangAttacker.remove(vid);
                        continue;
                    }
                    UUID atkId = exsangAttacker.get(vid);
                    LivingEntity atk = atkId == null ? null : lookupLiving(atkId);
                    aoeDamage(victim, 1.0, atk);
                    victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                            Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
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
        plugin.getCooldownManager().set("lifebloom", victim.getUniqueId(), cfg.lifebloomCdMs);
        int range = cfg.lifebloomRadius;
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
        // Mask — Ironwill / Dragon Skull blocks bleed application outright.
        if (victim instanceof Player
                && com.soulenchants.masks.MaskEffects.isEnchantImmune((Player) victim, "bleed")) {
            return;
        }
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

        // Anti-Healing — bleed at level 4+ applies a healing-reduction debuff
        // that lasts for the bleed duration. 10/20/30% at L4/5/6.
        int bleedLvl = weapon == null ? 0 : ItemUtil.getLevel(weapon, "bleed");
        if (bleedLvl >= 4) {
            double pct = Math.min(0.30, (bleedLvl - 3) * 0.10);
            applyAntiHeal(victim, "bleed", pct, BLEED_DURATION_MS);
        }

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

    /**
     * Apply (or refresh) an Anti-Healing debuff SOURCE on a victim. Each source
     * (bleed / severance / reapingslash / …) is tracked independently and
     * stacks MULTIPLICATIVELY with every other active source — e.g. 30% bleed
     * + 20% severance combine as 1 - (1 - .30)(1 - .20) = 44% reduction, not
     * 50%. Re-applying the same source refreshes its pct to the stronger value
     * and extends the expiry to the later of (existing, new).
     */
    private void applyAntiHeal(LivingEntity victim, String source, double pct, long durationMs) {
        if (victim == null || pct <= 0 || source == null) return;
        UUID id = victim.getUniqueId();
        long now = System.currentTimeMillis();
        long newUntil = now + durationMs;
        Map<String, AHEntry> bySource = antiHealBySource.computeIfAbsent(id, k -> new HashMap<>());
        AHEntry prev = bySource.get(source);
        double finalPct = pct;
        long finalUntil = newUntil;
        if (prev != null && prev.until > now) {
            finalPct  = Math.max(prev.pct, pct);
            finalUntil = Math.max(prev.until, newUntil);
        }
        bySource.put(source, new AHEntry(finalPct, finalUntil));
        if (victim instanceof Player) {
            double combined = effectiveAntiHealPct(id);
            Player pv = (Player) victim;
            // Lunar toast if available; otherwise chat fallback.
            boolean sent = com.soulenchants.lunar.LunarBridge.sendNotification(pv,
                    "§c⚑ Anti-Heal",
                    "§7Healing reduced §f" + (int)(combined * 100) + "%§7 · " + (durationMs / 1000) + "s",
                    3500L);
            if (!sent) {
                pv.sendMessage("§c§l⚑ ANTI-HEAL §7— healing reduced §f"
                        + (int)(combined * 100) + "%§7 for " + (durationMs / 1000) + "s");
            }
        }
    }

    /**
     * Scale down any healing the victim receives while anti-heal is active.
     * Covers EntityRegainHealthEvent (natural regen, golden apples, regen
     * potions, /heal etc.). Runs at MONITOR so we never race our own
     * post-proc heals from other listeners; we just scale the final amount.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHealDebuff(org.bukkit.event.entity.EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        double pct = effectiveAntiHealPct(e.getEntity().getUniqueId());
        if (pct <= 0) return;
        double scaled = e.getAmount() * (1.0 - pct);
        e.setAmount(Math.max(0, scaled));
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
                        bleedAttacker.remove(id);
                        bleedCrimsonTongue.remove(id);
                        continue;
                    }
                    int stacks = bleedStacks.getOrDefault(id, 0);
                    if (stacks <= 0) continue;

                    double dmg = 0.5 + 0.15 * Math.max(0, stacks - 1);   // L1 stack = 0.5 HP, 20 stacks = 3.35 HP
                    UUID atkId = bleedAttacker.get(id);
                    Player attacker = atkId == null ? null : Bukkit.getPlayer(atkId);
                    // Bleed ticks must not knock the victim around — DoT is
                    // just HP drip, not a fresh hit. Capture pre-damage velocity
                    // and restore next tick so Bukkit's damage() knockback
                    // impulse gets cancelled out.
                    final LivingEntity pinned = victim;
                    final Vector preVel = victim.getVelocity().clone();
                    // aoeDamage routes through AOE_GUARD so the bleed tick doesn't
                    // re-enter handleSwordEnchants and roll another Bleed proc.
                    aoeDamage(victim, dmg, attacker);
                    new BukkitRunnable() {
                        @Override public void run() {
                            if (pinned.isDead()) return;
                            pinned.setVelocity(preVel);
                        }
                    }.runTask(plugin);
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
