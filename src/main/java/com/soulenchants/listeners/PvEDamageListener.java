package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.items.ItemUtil;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class PvEDamageListener implements Listener {

    private final SoulEnchants plugin;
    public PvEDamageListener(SoulEnchants plugin) { this.plugin = plugin; }

    /** Projectile-only path for Slayer + Holy Smite. Melee Player→PvE is owned by
     *  CombatListener, which folds Slayer/HolySmite into the same additive pool
     *  as Witherbane/Demonslayer/Crit so they no longer multiply serially. We
     *  only fire here when a Player isn't the direct damager (i.e. arrow hits). */
    private static final double OFFENSIVE_BONUS_CAP = 2.00;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) e.getEntity();
        if (victim instanceof Player) return; // PvE only
        // Melee path is handled in CombatListener (additive cap). We only fire on
        // projectile hits, where CombatListener.onAttack short-circuits.
        if (e.getDamager() instanceof Player) return;

        Player attacker = null;
        if (e.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) e.getDamager();
            if (proj.getShooter() instanceof Player) attacker = (Player) proj.getShooter();
        }
        if (attacker == null) return;

        ItemStack hand = attacker.getItemInHand();
        if (hand == null) return;

        double offBonus = 0.0;
        int slayer = ItemUtil.getLevel(hand, "slayer");
        if (slayer > 0 && isBossOrMinion(victim)) offBonus += 0.25 * slayer;
        int holy = ItemUtil.getLevel(hand, "holysmite");
        if (holy > 0 && isUndead(victim)) offBonus += 0.30 * holy;
        if (offBonus > 0) {
            offBonus = Math.min(offBonus, OFFENSIVE_BONUS_CAP);
            e.setDamage(e.getDamage() * (1.0 + offBonus));
        }
    }

    // Blessed is now a SWORD enchant that strips your own debuffs on hit (Nordic-style).
    // The old chestplate damage-reduction is removed.

    private boolean isBossOrMinion(LivingEntity entity) {
        Veilweaver vw = plugin.getVeilweaverManager().getActive();
        if (vw == null) return false;
        if (entity.getUniqueId().equals(vw.getEntity().getUniqueId())) return true;
        for (LivingEntity m : vw.getMinions()) if (m != null && m.getUniqueId().equals(entity.getUniqueId())) return true;
        for (LivingEntity c : vw.getEchoClones()) if (c != null && c.getUniqueId().equals(entity.getUniqueId())) return true;
        return false;
    }

    private boolean isUndead(LivingEntity e) {
        EntityType t = e.getType();
        return t == EntityType.ZOMBIE || t == EntityType.SKELETON
                || t == EntityType.PIG_ZOMBIE || t == EntityType.WITHER;
    }
}
