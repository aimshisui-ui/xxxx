package com.soulenchants.listeners;

import com.soulenchants.bosses.BoulderTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * Cancels block placement when a tracked Iron Golem boulder lands on the
 * ground — keeps the iron-block boulders from littering the arena.
 */
public class BoulderBlockBlocker implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLand(EntityChangeBlockEvent e) {
        if (BoulderTracker.TRACKED.contains(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
            e.getEntity().remove();
            BoulderTracker.TRACKED.remove(e.getEntity().getUniqueId());
        }
    }
}
