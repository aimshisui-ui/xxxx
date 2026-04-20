package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.bosses.VeilweaverCrystals;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Veilweaver crystal handler.
 *
 *   - Crystals require 3 player hits to break (HP tracker).
 *   - Crystals are immune to each other's explosions (no chain wipe).
 *   - On break: trigger a visual-only explosion + signal owning boss.
 *   - All explosions clear blockList so the world isn't damaged.
 */
public class CrystalListener implements Listener {

    private static final int HITS_TO_BREAK = 3;
    private final Map<UUID, Integer> hits = new HashMap<>();

    private final SoulEnchants plugin;
    public CrystalListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCrystalDamage(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.ENDER_CRYSTAL) return;
        UUID id = e.getEntity().getUniqueId();
        Veilweaver vw = VeilweaverCrystals.REGISTRY.get(id);
        if (vw == null) return; // not one of ours

        // Cancel ALL non-player damage so other crystals' blasts can't chain-kill.
        // Only player melee/projectile counts toward the hit counter.
        Player attacker = null;
        if (e instanceof EntityDamageByEntityEvent) {
            org.bukkit.entity.Entity dmgr = ((EntityDamageByEntityEvent) e).getDamager();
            if (dmgr instanceof Player) attacker = (Player) dmgr;
            else if (dmgr instanceof Projectile && ((Projectile) dmgr).getShooter() instanceof Player) {
                attacker = (Player) ((Projectile) dmgr).getShooter();
            }
        }

        if (attacker == null) {
            // Block-explosion / fire / suffocation / chain-explosion — fully cancel.
            e.setCancelled(true);
            return;
        }

        // Player hit — count it, don't let vanilla blow it up yet
        e.setCancelled(true);
        int n = hits.getOrDefault(id, 0) + 1;
        hits.put(id, n);
        Location loc = e.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.GLASS, 1.5f, 0.7f);
        for (int i = 0; i < 8; i++)
            loc.getWorld().playEffect(loc.clone().add(0, 1, 0), Effect.WITCH_MAGIC, 0);
        attacker.sendMessage(org.bukkit.ChatColor.LIGHT_PURPLE
                + "✦ Crystal: " + n + "/" + HITS_TO_BREAK);

        if (n >= HITS_TO_BREAK) {
            hits.remove(id);
            org.bukkit.entity.Entity entity = e.getEntity();
            // Visual-only explosion (no block damage handled by EntityExplodeEvent)
            loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0f, false);
            // Crystal-shatter debuff — Wither II + Blindness for ~6s on every player
            // standing within 6 blocks. Punishes melee-stacking on the shield.
            for (org.bukkit.entity.Entity near : loc.getWorld().getNearbyEntities(loc, 6, 6, 6)) {
                if (!(near instanceof Player)) continue;
                Player nearby = (Player) near;
                nearby.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.WITHER, 120, 1, false, true), true);
                nearby.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.BLINDNESS, 80, 0, false, true), true);
                nearby.sendMessage(org.bukkit.ChatColor.DARK_PURPLE
                        + "✦ The shattered crystal exhales — " + org.bukkit.ChatColor.GRAY
                        + "Wither II for 6s, Blindness 4s.");
            }
            entity.remove();
            vw.getCrystals().onCrystalDestroyed(id);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrystalExplode(EntityExplodeEvent e) {
        if (e.getEntityType() != EntityType.ENDER_CRYSTAL) return;
        UUID id = e.getEntity().getUniqueId();
        if (!VeilweaverCrystals.REGISTRY.containsKey(id)) return;
        e.blockList().clear();
        e.setYield(0f);
    }
}
