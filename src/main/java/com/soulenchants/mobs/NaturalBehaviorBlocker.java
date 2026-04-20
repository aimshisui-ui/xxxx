package com.soulenchants.mobs;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTeleportEvent;

/**
 * Suppresses unwanted vanilla behaviors on custom mobs:
 *   - Creepers don't explode (no terrain damage, no AoE damage)
 *   - Endermen don't auto-teleport (rain/water/projectile dodge cancelled)
 *
 * Custom mobs use our scripted ability system instead — these vanilla
 * behaviors would short-circuit our intended encounter design.
 */
public class NaturalBehaviorBlocker implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onExplosion(EntityExplodeEvent e) {
        // ALL creepers (custom or vanilla) explode visually + deal damage to players,
        // but never break blocks. Avoids terrain damage to player builds.
        if (e.getEntityType() == org.bukkit.entity.EntityType.CREEPER) {
            e.blockList().clear();
            e.setYield(0f);
            // Don't cancel — let the damage portion fire
        }
    }

    /** Set to true while a scripted teleport is in progress so this listener allows it. */
    public static final ThreadLocal<Boolean> SCRIPTED_TELEPORT = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(EntityTeleportEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        if (CustomMob.idOf((LivingEntity) e.getEntity()) == null) return;
        if (SCRIPTED_TELEPORT.get()) return;  // Our ability is doing it — allow
        e.setCancelled(true);
    }
}
