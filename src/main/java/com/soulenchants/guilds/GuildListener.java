package com.soulenchants.guilds;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

/**
 * Friendly-fire blocker. Fires at LOWEST priority so the cancel happens
 * BEFORE any other listener (CombatListener, PvEDamageListener, NaturesWrath
 * proc gate, etc.) can apply procs. This means nothing on-hit triggers
 * between guildmates — no Bleed stacks, no Cleave splash, no Counter disarm,
 * no Lifesteal heal.
 *
 * Also handles arrows/snowballs/etc — projectile shooter is checked.
 */
public class GuildListener implements Listener {

    private final SoulEnchants plugin;

    public GuildListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();
        UUID attackerId = resolveAttackerUUID(e.getDamager());
        if (attackerId == null) return;
        if (attackerId.equals(victim.getUniqueId())) return;
        if (plugin.getGuildManager().isAlly(attackerId, victim.getUniqueId())) {
            e.setCancelled(true);
            // Hint the attacker once so they realize friendly-fire is blocked
            Player atk = (e.getDamager() instanceof Player)
                    ? (Player) e.getDamager()
                    : (e.getDamager() instanceof Projectile
                            && ((Projectile) e.getDamager()).getShooter() instanceof Player
                                ? (Player) ((Projectile) e.getDamager()).getShooter() : null);
            if (atk != null && plugin.getCooldownManager().isReady("guild_ff_msg", atk.getUniqueId())) {
                plugin.getCooldownManager().set("guild_ff_msg", atk.getUniqueId(), 5_000L);
                atk.sendMessage(ChatColor.AQUA + "✦ Guild — friendly fire blocked (" + victim.getName() + ")");
            }
        }
    }

    /** Pull the player UUID out of any damage source: direct player, or a
     *  projectile whose shooter is a player. Returns null otherwise. */
    private UUID resolveAttackerUUID(Entity damager) {
        if (damager instanceof Player) return damager.getUniqueId();
        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Player) return ((Player) proj.getShooter()).getUniqueId();
        }
        return null;
    }
}
