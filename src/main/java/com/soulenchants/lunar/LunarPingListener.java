package com.soulenchants.lunar;

import com.soulenchants.bosses.IronGolemBoss;
import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.SoulEnchants;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

/**
 * Live Apollo waypoint broadcaster for active bosses.
 *
 * Runs every 40 ticks (2s) — for each boss that's currently alive, refresh
 * its waypoint for every online player (handles movement). When a boss
 * transitions from alive → dead, broadcast removeWaypoint to every online
 * player so the pin vanishes cleanly. No-op if no Lunar backend is loaded.
 *
 * We track waypoint names in a per-tick snapshot (wasActive) and diff
 * against the previous snapshot — anything in prev but not now gets removed.
 */
public final class LunarPingListener extends BukkitRunnable implements Listener {

    private static final int VEILWEAVER_RGB = 0x8B4789;
    private static final int IRONGOLEM_RGB  = 0x8A8A8A;
    private static final int MODOCK_RGB     = 0x1F6FEB;

    private final SoulEnchants plugin;
    /** Waypoint ids seen alive on the last tick — used to detect deaths. */
    private final Set<String> lastAliveIds = new HashSet<>();

    public LunarPingListener(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() { runTaskTimer(plugin, 100L, 40L); }

    @Override
    public void run() {
        if (!LunarBridge.isAvailable()) return;

        Set<String> nowAlive = new HashSet<>();

        // Veilweaver
        Veilweaver vw = plugin.getVeilweaverManager() == null ? null : plugin.getVeilweaverManager().getActive();
        if (vw != null && vw.getEntity() != null && !vw.getEntity().isDead()) {
            pushToEveryone("Veilweaver", vw.getEntity().getLocation(), VEILWEAVER_RGB);
            nowAlive.add("Veilweaver");
        }

        // Iron Golem Colossus
        IronGolemBoss ig = plugin.getIronGolemManager() == null ? null : plugin.getIronGolemManager().getActive();
        if (ig != null && ig.getEntity() != null && !ig.getEntity().isDead()) {
            pushToEveryone("Colossus", ig.getEntity().getLocation(), IRONGOLEM_RGB);
            nowAlive.add("Colossus");
        }

        // Modock (3-phase Atlantis boss)
        if (plugin.getModockManager() != null && plugin.getModockManager().getActive() != null) {
            com.soulenchants.modock.ModockBoss mb = plugin.getModockManager().getActive();
            try {
                if (mb.getEntity() != null && !mb.getEntity().isDead()) {
                    pushToEveryone("Modock", mb.getEntity().getLocation(), MODOCK_RGB);
                    nowAlive.add("Modock");
                }
            } catch (Throwable ignored) {
                // getEntity() may not exist on older Modock impls — fail silent.
            }
        }

        // Clear any waypoints that were alive last tick but aren't now.
        for (String id : lastAliveIds) {
            if (!nowAlive.contains(id)) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    LunarBridge.clearWaypoint(p, id);
                }
            }
        }
        lastAliveIds.clear();
        lastAliveIds.addAll(nowAlive);
    }

    private void pushToEveryone(String name, Location loc, int rgb) {
        if (loc == null || loc.getWorld() == null) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!loc.getWorld().equals(p.getWorld())) {
                // Player not in boss's world — hide the pin for them.
                LunarBridge.clearWaypoint(p, name);
                continue;
            }
            LunarBridge.sendWaypoint(p, name, loc, rgb);
        }
    }

    /** External cleanup hook — called from managers on hard despawn. */
    public void forceClear(String name) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            LunarBridge.clearWaypoint(p, name);
        }
        lastAliveIds.remove(name);
    }
}
