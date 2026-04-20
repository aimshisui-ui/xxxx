package com.soulenchants.mobs;

import com.soulenchants.SoulEnchants;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Routes Bukkit events to per-entity resolved {@link MobAbility} hooks.
 * Resolved instances live in {@link CustomMob#lookup(UUID)} and are built
 * once at spawn time — edits via /ce loot only affect NEXT-spawned mobs.
 */
public class MobListener implements Listener {

    private static final Random RNG = new Random();

    private final SoulEnchants plugin;
    private final Set<UUID> trackedMobs = new HashSet<>();

    public MobListener(SoulEnchants plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    public void track(LivingEntity le) { trackedMobs.add(le.getUniqueId()); }

    private List<MobAbility> resolved(LivingEntity le) {
        List<MobAbility> r = CustomMob.lookup(le.getUniqueId());
        return r != null ? r : Collections.<MobAbility>emptyList();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobHitsPlayer(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof LivingEntity)) return;
        if (!(e.getEntity() instanceof Player)) return;
        LivingEntity attacker = (LivingEntity) e.getDamager();
        if (CustomMob.idOf(attacker) == null) return;
        for (MobAbility ab : resolved(attacker)) {
            try { ab.onHitPlayer(attacker, (Player) e.getEntity(), e); }
            catch (Throwable t) { plugin.getLogger().warning("[mob ability] " + t.getMessage()); }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobHurt(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity v = (LivingEntity) e.getEntity();
        String mid = CustomMob.idOf(v);
        if (mid == null) return;
        for (MobAbility ab : resolved(v)) {
            try { ab.onHurt(v, e); }
            catch (Throwable t) { plugin.getLogger().warning("[mob ability] " + t.getMessage()); }
        }
        // Live HP-bar refresh for ELITE-tier mobs (Hollow King + future bosses).
        // Vanilla nametag only updates on chunk-network-resync, so without an
        // explicit setCustomName call after damage the bar visibly lags.
        // Defer one tick so the damage event has fully applied before we read HP.
        CustomMob def = MobRegistry.get(mid);
        if (def != null && def.tier == CustomMob.Tier.ELITE) {
            new BukkitRunnable() {
                @Override public void run() { CustomMob.refreshHpBar(v, def); }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity v = e.getEntity();
        String id = CustomMob.idOf(v);
        if (id == null) return;
        CustomMob def = MobRegistry.get(id);
        if (def == null) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        Player killer = v.getKiller();
        if (killer != null) {
            int total = def.souls;
            if (plugin.getSoulManager().getTier(killer).grantsBonusPerKill()) total += 1;
            org.bukkit.inventory.ItemStack hand = killer.getItemInHand();
            int reaper = hand == null ? 0 : com.soulenchants.items.ItemUtil.getLevel(hand, "soulreaper");
            if (reaper > 0) total += (int) Math.ceil(def.souls * 0.25 * reaper);
            if (killer.getNoDamageTicks() > 0 && killer.getNoDamageTicks() < 200) {
                total = (int) Math.ceil(total * 1.2);
            }
            plugin.getSoulManager().add(killer, total);
            com.soulenchants.util.FloatingText.show(plugin, v.getLocation(),
                    org.bukkit.ChatColor.LIGHT_PURPLE + "+" + total + " §dSouls");
        }
        // Roll editable drop table
        for (com.soulenchants.mobs.DropSpec ds : def.dropSpecs) {
            org.bukkit.inventory.ItemStack rolled = ds.roll(RNG);
            if (rolled != null) v.getWorld().dropItemNaturally(v.getLocation(), rolled);
        }
        for (MobAbility ab : resolved(v)) {
            try { ab.onDeath(v, killer); }
            catch (Throwable t) { plugin.getLogger().warning("[mob ability onDeath] " + t.getMessage()); }
        }
        // If this mob is tracked by an active rift, decrement its kill counter.
        try { plugin.getVoidRiftManager().notifyMobDied(v.getUniqueId()); }
        catch (Throwable ignored) {}
        trackedMobs.remove(v.getUniqueId());
        CustomMob.forget(v.getUniqueId());
    }

    private void startTickTask() {
        new BukkitRunnable() {
            @Override public void run() {
                for (org.bukkit.World w : plugin.getServer().getWorlds()) {
                    for (org.bukkit.entity.Entity e : new ArrayList<>(w.getEntities())) {
                        if (!(e instanceof LivingEntity)) continue;
                        LivingEntity le = (LivingEntity) e;
                        String id = CustomMob.idOf(le);
                        CustomMob def = id == null ? null : MobRegistry.get(id);
                        if (def == null) {
                            // Orphan recovery: tag was lost (NBT write race, plugin
                            // reload, etc.) but the custom name is still on the
                            // entity. Match by display name and re-tag + rebuild.
                            String name = le.getCustomName();
                            if (name == null || name.isEmpty()) continue;
                            String stripped = org.bukkit.ChatColor.stripColor(name);
                            for (CustomMob cm : MobRegistry.all()) {
                                if (cm.displayName.equals(stripped)) {
                                    def = cm;
                                    id = cm.id;
                                    cm.rebuildResolved(le);
                                    break;
                                }
                            }
                            if (def == null) continue;
                        }
                        // If the resolved cache is missing (server restart, chunk reload edge
                        // cases) the mob has lost its abilities + custom name. Re-attach
                        // everything so it stays a true custom mob across its lifetime.
                        List<MobAbility> rs = CustomMob.lookup(le.getUniqueId());
                        boolean stateDrifted = rs == null
                                || le.getCustomName() == null
                                || !le.isCustomNameVisible()
                                || le.getMaxHealth() != def.maxHp;
                        if (stateDrifted) {
                            // Either cache wiped (restart/chunk reload) or vanilla
                            // mechanics drifted us back toward defaults. Re-configure.
                            try { def.rebuildResolved(le); }
                            catch (Throwable t) { plugin.getLogger().warning("[mob rebuildResolved] " + t.getMessage()); }
                            rs = CustomMob.lookup(le.getUniqueId());
                            if (rs == null) continue;
                        }
                        for (MobAbility ab : rs) {
                            try { ab.onTick(le); }
                            catch (Throwable t) { plugin.getLogger().warning("[mob tick] " + t.getMessage()); }
                        }
                        // Passive HP-bar refresh for ELITE mobs — covers regen/heal
                        // ticks where no damage event fires. Damage-driven refresh
                        // in onMobHurt above handles the responsive case.
                        if (def.tier == CustomMob.Tier.ELITE) {
                            CustomMob.refreshHpBar(le, def);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
