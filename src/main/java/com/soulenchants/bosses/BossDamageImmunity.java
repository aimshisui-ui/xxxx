package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Foolproof damage immunity for active bosses (Veilweaver + Iron Golem) and
 * for any custom mob with the boss tier flag. Prevents environmental hazards
 * (fall, void, suffocation, fire-tick) from killing the boss before players
 * can. Real player damage still applies.
 */
public class BossDamageImmunity implements Listener {

    private final SoulEnchants plugin;

    public BossDamageImmunity(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEnvironmental(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        if (!isBoss((LivingEntity) e.getEntity())) return;
        switch (e.getCause()) {
            case FALL:
            case VOID:
            case SUFFOCATION:
            case DROWNING:
            case STARVATION:
            case CONTACT:                 // cactus
            case LIGHTNING:               // safe — bosses already use lightning visually
            case FIRE_TICK:               // we don't want fire ticks chipping bosses
                e.setCancelled(true);
                e.setDamage(0.0);
                break;
            default: // EntityDamage and EntityDamageByEntity flow normally
        }
    }

    private boolean isBoss(LivingEntity le) {
        // Active named bosses
        try {
            if (plugin.getVeilweaverManager().getActive() != null
                    && plugin.getVeilweaverManager().getActive().getEntity().getUniqueId().equals(le.getUniqueId()))
                return true;
            if (plugin.getIronGolemManager().getActive() != null
                    && plugin.getIronGolemManager().getActive().getEntity().getUniqueId().equals(le.getUniqueId()))
                return true;
        } catch (Throwable ignored) {}
        // ELITE-tier custom mobs (Hollow King + the registry's elite roster)
        try {
            String id = com.soulenchants.mobs.CustomMob.idOf(le);
            if (id == null) return false;
            com.soulenchants.mobs.CustomMob cm = com.soulenchants.mobs.MobRegistry.get(id);
            return cm != null && cm.tier == com.soulenchants.mobs.CustomMob.Tier.ELITE;
        } catch (Throwable t) { return false; }
    }
}
