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

    /** Player attacking PvE target — Slayer + Holy Smite */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) e.getEntity();
        if (victim instanceof Player) return; // PvE only

        Player attacker = null;
        if (e.getDamager() instanceof Player) attacker = (Player) e.getDamager();
        else if (e.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) e.getDamager();
            if (proj.getShooter() instanceof Player) attacker = (Player) proj.getShooter();
        }
        if (attacker == null) return;

        ItemStack hand = attacker.getItemInHand();
        if (hand == null) return;

        int slayer = ItemUtil.getLevel(hand, "slayer");
        if (slayer > 0 && isBossOrMinion(victim)) {
            e.setDamage(e.getDamage() * (1.0 + 0.25 * slayer)); // +25/50/75%
        }

        int holy = ItemUtil.getLevel(hand, "holysmite");
        if (holy > 0 && isUndead(victim)) {
            e.setDamage(e.getDamage() * (1.0 + 0.30 * holy)); // +30/60/90%
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
