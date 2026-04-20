package com.soulenchants.sets;

import com.soulenchants.SoulEnchants;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Single dispatcher: routes combat events to the wearer's currently-active
 * set (cached by SetManager) so we don't re-scan armor on every hit.
 *
 * Re-evaluates the set on join / respawn / armor-change. ArmorChangeListener
 * already fires on equipment slot changes — we hook into that via a quick
 * delayed re-eval after damage events too (covers death-drop scenarios).
 */
public class SetBonusListener implements Listener {

    private final SoulEnchants plugin;

    public SetBonusListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            Player attacker = (Player) e.getDamager();
            SetBonus s = plugin.getSetManager().getActive(attacker);
            if (s != null) {
                try { s.onAttack(attacker, e); } catch (Throwable t) { plugin.getLogger().warning("[sets] onAttack: " + t); }
            }
        }
        if (e.getEntity() instanceof Player) {
            Player victim = (Player) e.getEntity();
            SetBonus s = plugin.getSetManager().getActive(victim);
            if (s != null) {
                try { s.onDamaged(victim, e); } catch (Throwable t) { plugin.getLogger().warning("[sets] onDamaged: " + t); }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageGeneric(EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent) return;     // already covered above
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();
        SetBonus s = plugin.getSetManager().getActive(victim);
        if (s != null) {
            try { s.onDamaged(victim, e); } catch (Throwable t) { plugin.getLogger().warning("[sets] onDamaged: " + t); }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Defer one tick so inventory contents are fully loaded
        new BukkitRunnable() {
            @Override public void run() { plugin.getSetManager().reevaluate(e.getPlayer()); }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        new BukkitRunnable() {
            @Override public void run() { plugin.getSetManager().reevaluate(e.getPlayer()); }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getSetManager().clearPlayer(e.getPlayer().getUniqueId());
    }
}
