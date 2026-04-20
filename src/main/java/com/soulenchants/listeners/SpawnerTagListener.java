package com.soulenchants.listeners;

import com.soulenchants.currency.MobSoulRules;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Tags every entity that spawns from a vanilla mob spawner so MobSoulRules
 * can halve its soul drop.
 */
public class SpawnerTagListener implements Listener {

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            MobSoulRules.markSpawner(e.getEntity().getUniqueId());
        }
    }
}
