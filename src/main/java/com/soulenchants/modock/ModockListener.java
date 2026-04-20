package com.soulenchants.modock;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * One-shot listener bound to a single ModockBoss instance — registered on
 * spawn, unregistered on death/abort. Tracks per-player damage so the boss's
 * damage map stays in sync regardless of source (melee, projectile, splash).
 */
public class ModockListener implements Listener {

    private final SoulEnchants plugin;
    private final ModockBoss boss;
    private boolean registered = false;

    public ModockListener(SoulEnchants plugin, ModockBoss boss) {
        this.plugin = plugin;
        this.boss = boss;
    }

    public void register() {
        if (registered) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registered = true;
    }

    public void unregister() {
        if (!registered) return;
        HandlerList.unregisterAll(this);
        registered = false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (boss.getEntity() == null) return;
        if (!boss.getEntity().getUniqueId().equals(e.getEntity().getUniqueId())) return;
        Player attacker = resolvePlayer(e);
        if (attacker == null) return;
        boss.recordDamage(attacker.getUniqueId(), e.getFinalDamage());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (boss.getEntity() == null) return;
        if (!boss.getEntity().getUniqueId().equals(e.getEntity().getUniqueId())) return;
        // Drop nothing vanilla — ModockManager.onDeath drops the actual loot
        e.getDrops().clear();
        e.setDroppedExp(0);
        Player killer = e.getEntity().getKiller();
        plugin.getModockManager().onDeath(killer);
        unregister();
    }

    private Player resolvePlayer(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) return (Player) e.getDamager();
        if (e.getDamager() instanceof Projectile) {
            Projectile pr = (Projectile) e.getDamager();
            if (pr.getShooter() instanceof Player) return (Player) pr.getShooter();
        }
        return null;
    }
}
