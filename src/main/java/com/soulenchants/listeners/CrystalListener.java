package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.bosses.VeilweaverCrystals;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.UUID;

/**
 * Handles Veilweaver-owned end crystals:
 *  - Their explosions don't damage blocks (clear blockList).
 *  - When one dies, signal back to its owning Veilweaver to update the
 *    crystal-shield counter.
 */
public class CrystalListener implements Listener {

    private final SoulEnchants plugin;

    public CrystalListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrystalExplode(EntityExplodeEvent e) {
        if (e.getEntityType() != EntityType.ENDER_CRYSTAL) return;
        UUID id = e.getEntity().getUniqueId();
        if (!VeilweaverCrystals.REGISTRY.containsKey(id)) return;
        // Don't tear up the world
        e.blockList().clear();
        e.setYield(0f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCrystalDamage(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.ENDER_CRYSTAL) return;
        UUID id = e.getEntity().getUniqueId();
        Veilweaver vw = VeilweaverCrystals.REGISTRY.get(id);
        if (vw == null) return;
        // Crystals die in 1 hit, so signal here regardless of cancellation state
        vw.getCrystals().onCrystalDestroyed(id);
    }
}
