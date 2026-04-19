package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.Veilweaver;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class VeilweaverDamageListener implements Listener {

    private final SoulEnchants plugin;
    public VeilweaverDamageListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) e.getEntity();
        if (!plugin.getVeilweaverManager().isVeilweaver(entity)) return;
        Veilweaver vw = plugin.getVeilweaverManager().getActive();
        if (vw == null) return;

        // Fire/lava/drowning/fall immunity
        switch (e.getCause()) {
            case FIRE: case FIRE_TICK: case LAVA: case DROWNING: case FALL: case SUFFOCATION:
                e.setCancelled(true);
                return;
            default: break;
        }

        // Apocalypse invuln window
        if (plugin.getVeilweaverManager().isInvulnerable()) {
            e.setCancelled(true);
            return;
        }

        // Phase 2 arrow reduction
        if (vw.getPhase() == Veilweaver.Phase.TWO && e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            e.setDamage(e.getDamage() * 0.5);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) e.getEntity();
        if (!plugin.getVeilweaverManager().isVeilweaver(entity)) return;
        Veilweaver vw = plugin.getVeilweaverManager().getActive();
        if (vw == null) return;

        Player attacker = null;
        if (e.getDamager() instanceof Player) attacker = (Player) e.getDamager();
        else if (e.getDamager() instanceof Projectile) {
            Projectile pj = (Projectile) e.getDamager();
            if (pj.getShooter() instanceof Player) attacker = (Player) pj.getShooter();
        }
        if (attacker != null) vw.onDamageDealt(attacker, e.getFinalDamage());
    }
}
