package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.IronGolemBoss;
import com.soulenchants.bosses.IronGolemMinions;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Iron Sentinels:
 *   - Apply Slowness II for 3s on player hit
 *   - Don't friendly-fire with the Colossus boss (in either direction)
 */
public class IronSentinelListener implements Listener {

    private final SoulEnchants plugin;
    public IronSentinelListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof LivingEntity)) return;
        if (!(e.getEntity() instanceof Player)) return;
        if (!IronGolemMinions.ACTIVE_UUIDS.contains(e.getDamager().getUniqueId())) return;
        Player victim = (Player) e.getEntity();
        // Bonus 8 real damage on top of weapon damage so they hit hard through armor
        e.setDamage(e.getDamage() + 8);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,     60, 1, false, true), true);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0, false, true), true);
        if (Math.random() < 0.15)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, false, true), true);
    }

    /** Cancel boss <-> sentinel damage. Vanilla iron golems aggro on zombies. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent e) {
        if (isFriendlyPair(e.getDamager().getUniqueId(), e.getEntity().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    /** Cancel boss <-> sentinel targeting. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent e) {
        if (e.getTarget() == null) return;
        if (isFriendlyPair(e.getEntity().getUniqueId(), e.getTarget().getUniqueId())) {
            e.setCancelled(true);
            e.setTarget(null);
        }
    }

    private boolean isFriendlyPair(UUID a, UUID b) {
        IronGolemBoss boss = plugin.getIronGolemManager().getActive();
        UUID bossId = boss == null ? null : boss.getEntity().getUniqueId();
        boolean aIsBoss = bossId != null && bossId.equals(a);
        boolean bIsBoss = bossId != null && bossId.equals(b);
        boolean aIsMinion = IronGolemMinions.ACTIVE_UUIDS.contains(a);
        boolean bIsMinion = IronGolemMinions.ACTIVE_UUIDS.contains(b);
        return (aIsBoss && bIsMinion) || (bIsBoss && aIsMinion) || (aIsMinion && bIsMinion);
    }
}
