package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
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
    private final Map<UUID, Long> antiKbCd = new HashMap<>();
    private final Map<UUID, Long> lastCombatTick = new HashMap<>();

    // Bloodlust kill streak tracking
    private final Map<UUID, Integer> bloodlustStacks = new HashMap<>();
    private final Map<UUID, Long> bloodlustLastKill = new HashMap<>();

    public CombatListener(SoulEnchants plugin) { this.plugin = plugin; }

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

        // Featherweight (fall damage)
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            int fw = maxArmor(victim, "featherweight");
            if (fw > 0) e.setDamage(e.getDamage() * (1.0 - 0.33 * fw));
        }
        // Ironclad (explosion damage)
        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            int ic = maxArmor(victim, "ironclad");
            if (ic > 0) e.setDamage(e.getDamage() * (1.0 - 0.20 * ic));
        }

        // Lethal-hit enchants (Phoenix > Soul Shield priority)
        if (victim.getHealth() - e.getFinalDamage() > 0) return;
        UUID vid = victim.getUniqueId();

        // Phoenix — once per 10 min, survive lethal with full HP
        int phoenix = maxArmor(victim, "phoenix");
        if (phoenix > 0 && plugin.getCooldownManager().isReady("phoenix", vid)) {
            plugin.getCooldownManager().set("phoenix", vid, 2 * 60 * 1000L);
            e.setCancelled(true);
            victim.setHealth(victim.getMaxHealth());
            victim.getWorld().strikeLightningEffect(victim.getLocation());
            victim.sendMessage("§6✦ §e§lPhoenix rises! §7You survived death.");
            return;
        }

        int shield = maxArmor(victim, "soulshield");
        if (shield <= 0) return;
        if (!plugin.getCooldownManager().isReady("soulshield", vid)) return;
        int cost = 200;
        if (plugin.getSoulManager().get(victim) < cost) return;
        plugin.getSoulManager().take(victim, cost);
        plugin.getCooldownManager().set("soulshield", vid, 60_000L);
        e.setCancelled(true);
        victim.setHealth(Math.min(victim.getMaxHealth(), victim.getHealth() + 6));
        victim.getWorld().strikeLightningEffect(victim.getLocation());
        victim.sendMessage("§c✦ §4Soul Shield §4triggered! §7(-" + cost + " souls)");
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

    /** On-hit procs only fire against bosses, boss minions, or players (PvP). */
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
        if (ig != null && ig.getEntity().getUniqueId().equals(victim.getUniqueId())) return true;
        return false;
    }

    private void handleSwordEnchants(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        ItemStack hand = attacker.getItemInHand();
        if (hand == null) return;
        // Lock all proc-style enchants to boss / boss-minion / player targets.
        if (!isValidProcTarget(victim)) return;

        // Lifesteal (moved from DamageListener)
        int ls = ItemUtil.getLevel(hand, "lifesteal");
        if (ls > 0) new com.soulenchants.enchants.impl.LifestealEnchant().onHit(attacker, ls);

        // Bleed
        int bleed = ItemUtil.getLevel(hand, "bleed");
        if (bleed > 0 && rng.nextDouble() < 0.10 * bleed) scheduleBleed(victim, attacker, bleed);

        // Deepwounds
        int dw = ItemUtil.getLevel(hand, "deepwounds");
        if (dw > 0) victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60 * dw, 0));

        // Cripple (5s cooldown per attacker)
        int cr = ItemUtil.getLevel(hand, "cripple");
        if (cr > 0 && rng.nextDouble() < 0.08 * cr
                && plugin.getCooldownManager().isReady("cripple", attacker.getUniqueId())) {
            plugin.getCooldownManager().set("cripple", attacker.getUniqueId(), 5_000L);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, cr - 1));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, cr - 1));
        }

        // Venom
        int ven = ItemUtil.getLevel(hand, "venom");
        if (ven > 0 && rng.nextDouble() < 0.10 * ven)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40 * ven, 0));

        // Wither Bane
        int wb = ItemUtil.getLevel(hand, "witherbane");
        if (wb > 0 && isWitherFamily(victim)) e.setDamage(e.getDamage() * (1.0 + 0.35 * wb));

        // Demon Slayer
        int demon = ItemUtil.getLevel(hand, "demonslayer");
        if (demon > 0 && isNetherMob(victim)) e.setDamage(e.getDamage() * (1.0 + 0.30 * demon));

        // Beast Slayer
        int beast = ItemUtil.getLevel(hand, "beastslayer");
        if (beast > 0 && isArthropod(victim)) e.setDamage(e.getDamage() * (1.0 + 0.25 * beast));

        // Executioner
        int exec = ItemUtil.getLevel(hand, "executioner");
        if (exec > 0 && victim.getHealth() / victim.getMaxHealth() < 0.30)
            e.setDamage(e.getDamage() * (1.0 + 0.25 * exec));

        // Cleave
        int cleave = ItemUtil.getLevel(hand, "cleave");
        if (cleave > 0) {
            double splashDmg = e.getDamage() * (0.2 * cleave);
            for (Entity near : victim.getNearbyEntities(2.5, 2.5, 2.5)) {
                if (!(near instanceof LivingEntity) || near.equals(attacker) || near.equals(victim)) continue;
                if (near instanceof Player) continue;
                ((LivingEntity) near).damage(splashDmg, attacker);
            }
        }

        // Frost Aspect
        int frost = ItemUtil.getLevel(hand, "frostaspect");
        if (frost > 0 && rng.nextDouble() < 0.12 * frost) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40 * frost, frost - 1));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40 * frost, frost - 1));
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.PACKED_ICE.getId());
        }

        // Cursed Edge
        int curse = ItemUtil.getLevel(hand, "cursededge");
        if (curse > 0 && rng.nextDouble() < 0.07 * curse)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));

        // Soul Burn
        int sb = ItemUtil.getLevel(hand, "soulburn");
        if (sb > 0 && rng.nextDouble() < 0.12 * sb) {
            victim.setFireTicks(60 * sb);
            e.setDamage(e.getDamage() + (1.0 * sb));
        }

        // Phantom Strike
        int ps = ItemUtil.getLevel(hand, "phantomstrike");
        if (ps > 0 && rng.nextDouble() < 0.04 * ps && !(victim instanceof Player)) {
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
                ((LivingEntity) near).damage(aoeDmg, attacker);
            }
        }

        // Bonebreaker
        int bb = ItemUtil.getLevel(hand, "bonebreaker");
        if (bb > 0 && rng.nextDouble() < 0.10 * bb)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60 * bb, 0));

        // Critical Strike
        int crit = ItemUtil.getLevel(hand, "criticalstrike");
        if (crit > 0 && rng.nextDouble() < 0.05 * crit) {
            e.setDamage(e.getDamage() * 1.5);
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
                    ((LivingEntity) near).damage(1.5 * rw, attacker);
                }
            }
        }

        // Greedy — handled in onKill below
        // Headhunter — handled in onKill below

        // Drunk's strength buff is applied passively in tick task.

        // Bloodlust stacks
        int blvl = ItemUtil.getLevel(hand, "bloodlust");
        if (blvl > 0) {
            UUID id = attacker.getUniqueId();
            long now = System.currentTimeMillis();
            long last = bloodlustLastKill.getOrDefault(id, 0L);
            int stacks = (now - last < 10_000L) ? bloodlustStacks.getOrDefault(id, 0) : 0;
            if (stacks > 0) e.setDamage(e.getDamage() * (1.0 + 0.05 * blvl * stacks));
        }

        // Soul Strike
        int ss = ItemUtil.getLevel(hand, "soulstrike");
        if (ss > 0 && rng.nextDouble() < 0.15) {
            int cost = 30;
            if (plugin.getSoulManager().take(attacker, cost)) {
                e.setDamage(e.getDamage() * (1.5 + 0.5 * ss));
                attacker.getWorld().strikeLightningEffect(victim.getLocation());
                attacker.sendMessage("§4✦ Soul Strike! §7(-" + cost + ")");
            }
        }

        // Soul Drain
        int sd = ItemUtil.getLevel(hand, "souldrain");
        if (sd > 0) {
            int cost = 15;
            if (plugin.getSoulManager().take(attacker, cost)) {
                double heal = e.getDamage() * (0.25 * sd);
                attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
            }
        }
    }

    private void handleArmorOnHit(Player victim, EntityDamageByEntityEvent e) {
        ItemStack[] armor = victim.getInventory().getArmorContents();
        int hardened=0, antikb=0, molten=0, lastStand=0, stormcall=0, guardians=0,
            reflect=0, endurance=0, vengeance=0, soulburst=0;
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
            armoredSum     += ItemUtil.getLevel(a, "armored");
            enlightenedSum += ItemUtil.getLevel(a, "enlightened");
        }
        final UUID id = victim.getUniqueId();

        // Armored: stacks across chestplate + leggings.
        // proc = 8% × sum_level (max 64% at 4+4)   reduction = 4% × sum_level (max 32%)
        if (armoredSum > 0 && e.getDamager() instanceof Player) {
            ItemStack hand = ((Player) e.getDamager()).getItemInHand();
            if (hand != null && hand.getType().name().endsWith("_SWORD")) {
                double proc = Math.min(0.64, 0.08 * armoredSum);
                if (rng.nextDouble() < proc) {
                    double reduction = Math.min(0.32, 0.04 * armoredSum);
                    e.setDamage(e.getDamage() * (1.0 - reduction));
                }
            }
        }

        // Enlightened: stacks across all 4 armor pieces.
        // proc = 3% × sum_level (max 36% at full set lvl 3)   heals 100% of damage
        if (enlightenedSum > 0) {
            double proc = Math.min(0.36, 0.03 * enlightenedSum);
            if (rng.nextDouble() < proc) {
                double dmg = e.getDamage();
                e.setCancelled(true);
                double newHp = Math.min(victim.getMaxHealth(), victim.getHealth() + dmg);
                victim.setHealth(newHp);
                victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                        org.bukkit.Effect.STEP_SOUND, Material.EMERALD_BLOCK.getId());
                victim.sendMessage("§6✦ §eEnlightened — §a+" + (int) dmg + " HP §7(damage absorbed)");
                return;
            }
        }

        if (hardened > 0 && e.getDamager() instanceof LivingEntity)
            e.setDamage(e.getDamage() * (1.0 - 0.07 * hardened));

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
        if (molten > 0 && e.getDamager() instanceof LivingEntity && rng.nextDouble() < 0.12 * molten)
            ((LivingEntity) e.getDamager()).setFireTicks(60 * molten);
        if (lastStand > 0 && victim.getHealth() <= 6.0)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, lastStand - 1));
        if (stormcall > 0 && e.getDamage() >= 6 && e.getDamager() instanceof LivingEntity
                && plugin.getCooldownManager().isReady("stormcaller", id)) {
            long cd = 8000L - (1500L * (stormcall - 1));
            plugin.getCooldownManager().set("stormcaller", id, cd);
            e.getDamager().getWorld().strikeLightning(e.getDamager().getLocation());
        }
        if (guardians > 0 && plugin.getCooldownManager().isReady("guardians", id)) {
            plugin.getCooldownManager().set("guardians", id, 12_000L);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, guardians - 1));
        }
        if (reflect > 0 && e.getDamager() instanceof LivingEntity
                && plugin.getCooldownManager().isReady("reflect", id)) {
            plugin.getCooldownManager().set("reflect", id, 3_000L);
            ((LivingEntity) e.getDamager()).damage(e.getDamage() * (0.10 * reflect), victim);
        }
        if (endurance > 0) {
            long now = System.currentTimeMillis();
            long last = lastCombatTick.getOrDefault(id, 0L);
            if (now - last < 10_000L)
                e.setDamage(e.getDamage() * (1.0 - Math.min(0.20, 0.01 * (endurance * 2))));
            lastCombatTick.put(id, now);
        }
        if (vengeance > 0 && e.getDamager() instanceof LivingEntity)
            ((LivingEntity) e.getDamager()).damage(1.0 + 0.5 * vengeance, victim);
        if (soulburst > 0) {
            int cost = 50;
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

        // Bloodlust stacks
        int bl = ItemUtil.getLevel(hand, "bloodlust");
        if (bl > 0) {
            UUID id = killer.getUniqueId();
            long now = System.currentTimeMillis();
            long last = bloodlustLastKill.getOrDefault(id, 0L);
            int stacks = (now - last < 10_000L) ? bloodlustStacks.getOrDefault(id, 0) : 0;
            stacks = Math.min(stacks + 1, 5);
            bloodlustStacks.put(id, stacks);
            bloodlustLastKill.put(id, now);
        }

        // Headhunter — drop a player skull (generic) for the killed mob
        int hh = ItemUtil.getLevel(hand, "headhunter");
        if (hh > 0 && rng.nextDouble() < 0.04 * hh) {
            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 0);
            e.getDrops().add(skull);
        }

        // Greedy — bonus copies of drops
        int greedy = ItemUtil.getLevel(hand, "greedy");
        if (greedy > 0) {
            java.util.List<ItemStack> bonus = new java.util.ArrayList<>();
            for (ItemStack drop : e.getDrops()) {
                if (drop == null) continue;
                if (rng.nextDouble() < 0.10 * greedy) {
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

    private void scheduleBleed(LivingEntity target, Player source, int level) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 3 || target.isDead()) { cancel(); return; }
                target.damage(level * 1.5, source);
                target.getWorld().playEffect(target.getLocation().add(0, 1, 0),
                        Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
