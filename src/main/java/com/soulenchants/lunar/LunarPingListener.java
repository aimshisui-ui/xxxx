package com.soulenchants.lunar;

import com.soulenchants.bosses.IronGolemBoss;
import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.SoulEnchants;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Broadcasts live Lunar waypoints for active bosses so players on Lunar Client
 * get an in-world pin they can glance at / navigate to. Also broadcasts a
 * Rift-entry waypoint. No-op if Lunar API isn't available.
 *
 * Runs at 2-second cadence — Lunar waypoints persist client-side once sent,
 * so we just refresh locations as they move.
 */
public final class LunarPingListener extends BukkitRunnable implements Listener {

    private static final int VEILWEAVER_RGB = 0x8B4789; // violet
    private static final int IRONGOLEM_RGB  = 0x8A8A8A; // iron grey
    private static final int MODOCK_RGB     = 0x1F6FEB; // deep blue
    private static final int RIFT_RGB       = 0x3FE2B8; // teal

    private final SoulEnchants plugin;

    public LunarPingListener(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() { runTaskTimer(plugin, 100L, 40L); }

    @Override
    public void run() {
        if (!LunarBridge.isAvailable()) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            pushBossPings(p);
        }
    }

    private void pushBossPings(Player p) {
        Veilweaver vw = plugin.getVeilweaverManager() == null ? null : plugin.getVeilweaverManager().getActive();
        if (vw != null && vw.getEntity() != null && !vw.getEntity().isDead()) {
            Location loc = vw.getEntity().getLocation();
            if (loc.getWorld().equals(p.getWorld())) {
                LunarBridge.sendWaypoint(p, "§5Veilweaver", loc, VEILWEAVER_RGB);
            }
        }
        IronGolemBoss ig = plugin.getIronGolemManager() == null ? null : plugin.getIronGolemManager().getActive();
        if (ig != null && ig.getEntity() != null && !ig.getEntity().isDead()) {
            Location loc = ig.getEntity().getLocation();
            if (loc.getWorld().equals(p.getWorld())) {
                LunarBridge.sendWaypoint(p, "§7Colossus", loc, IRONGOLEM_RGB);
            }
        }
    }
}
