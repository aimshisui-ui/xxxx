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
        org.bukkit.inventory.ItemStack held = attacker.getItemInHand();
        // Dual dispatch: core first, then bound ability (if any and distinct).
        MythicWeapon core    = MythicRegistry.of(held);
        MythicWeapon ability = MythicRegistry.abilityOf(held);
        if (core != null)    core.onAttack(attacker, e);
        if (ability != null) ability.onAttack(attacker, e);
        if (e.getEntity() instanceof Player) {
            Player victim = (Player) e.getEntity();
            org.bukkit.inventory.ItemStack vHeld = victim.getItemInHand();
            MythicWeapon vCore    = MythicRegistry.of(vHeld);
            MythicWeapon vAbility = MythicRegistry.abilityOf(vHeld);
            if (vCore != null)    vCore.onDefend(victim, e);
            if (vAbility != null) vAbility.onDefend(victim, e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;
        org.bukkit.inventory.ItemStack held = killer.getItemInHand();
        MythicWeapon core    = MythicRegistry.of(held);
        MythicWeapon ability = MythicRegistry.abilityOf(held);
        if (core != null)    core.onKill(killer, e);
        if (ability != null) ability.onKill(killer, e);
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
