package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.IronGolemBoss;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class IronGolemDamageListener implements Listener {

    private final SoulEnchants plugin;
    public IronGolemDamageListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) e.getEntity();
        if (!plugin.getIronGolemManager().isIronGolemBoss(entity)) return;
        IronGolemBoss b = plugin.getIronGolemManager().getActive();
        if (b == null) return;

        switch (e.getCause()) {
            case FIRE: case FIRE_TICK: case LAVA: case DROWNING: case FALL: case SUFFOCATION:
                e.setCancelled(true); return;
            default: break;
        }
        if (b.isInvulnerable()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) e.getEntity();
        if (!plugin.getIronGolemManager().isIronGolemBoss(entity)) return;
        IronGolemBoss b = plugin.getIronGolemManager().getActive();
        if (b == null) return;

        Player attacker = null;
        if (e.getDamager() instanceof Player) attacker = (Player) e.getDamager();
        else if (e.getDamager() instanceof Projectile) {
            Projectile pj = (Projectile) e.getDamager();
            if (pj.getShooter() instanceof Player) attacker = (Player) pj.getShooter();
        }
        if (attacker != null) b.onDamageDealt(attacker, e.getFinalDamage());
    }
}
