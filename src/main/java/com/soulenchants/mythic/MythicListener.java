package com.soulenchants.mythic;

import com.soulenchants.SoulEnchants;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/** Routes combat events to the player's currently-held/hotbar mythic. */
public final class MythicListener implements Listener {

    private final SoulEnchants plugin;

    public MythicListener(SoulEnchants plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        Player attacker = resolveAttacker(e);
        if (attacker == null) return;
        MythicWeapon m = MythicRegistry.of(attacker.getItemInHand());
        if (m != null) m.onAttack(attacker, e);
        // Soulbinder fires on projectile too — its onAttack checks cause internally.
        if (attacker.getItemInHand() == null && e.getCause() ==
                org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE) {
            // Ranged without a mythic — nothing to do.
        }
        if (e.getEntity() instanceof Player) {
            Player victim = (Player) e.getEntity();
            MythicWeapon held = MythicRegistry.of(victim.getItemInHand());
            if (held != null) held.onDefend(victim, e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;
        MythicWeapon m = MythicRegistry.of(killer.getItemInHand());
        if (m != null) m.onKill(killer, e);
    }

    /** Resolve the damaging player — direct hit or via projectile. */
    private Player resolveAttacker(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) return (Player) e.getDamager();
        if (e.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.projectiles.ProjectileSource src =
                    ((org.bukkit.entity.Projectile) e.getDamager()).getShooter();
            if (src instanceof Player) return (Player) src;
        }
        return null;
    }
}
