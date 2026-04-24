package com.soulenchants.util;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Single plugin-wide registry for every UUID-keyed cache map.
 *
 *   MapManager.registerMap(myMap)   → every PlayerQuit evicts the quitting
 *                                     player's entry from myMap automatically.
 *   MapManager.registerSet(mySet)   → same for Set<UUID>.
 *
 * Replaces the scatter-gun pattern of every listener manually doing
 * `.remove(id)` inside its own quit handler. Fields that need quit-time
 * cleanup just register once at construction time and forget about it;
 * the PlayerQuitEvent listener below handles the rest.
 *
 * Also provides an onDisable() hook so a clean shutdown drains caches —
 * important for avoiding stale state on /reload.
 */
public final class MapManager implements Listener {

    /** All registered UUID→? maps. Synchronised wrapping keeps the
     *  iteration safe even if plugin init-order makes new registrations
     *  happen while cleanup is running. */
    private static final List<Map<UUID, ?>> MAPS = Collections.synchronizedList(new ArrayList<Map<UUID, ?>>());
    /** All registered Set<UUID> pools. */
    private static final List<Set<UUID>>    SETS = Collections.synchronizedList(new ArrayList<Set<UUID>>());

    /** Diagnostic label for each registered structure — helps /ce debug show
     *  which maps are contributing how many entries. */
    private static final List<String> MAP_LABELS = Collections.synchronizedList(new ArrayList<String>());
    private static final List<String> SET_LABELS = Collections.synchronizedList(new ArrayList<String>());

    private MapManager() {}

    /** Register a map whose entries should be evicted when their UUID quits. */
    public static <V> Map<UUID, V> registerMap(Map<UUID, V> map, String label) {
        MAPS.add(map);
        MAP_LABELS.add(label);
        return map;
    }

    /** Register a set whose entries should be evicted when their UUID quits. */
    public static Set<UUID> registerSet(Set<UUID> set, String label) {
        SETS.add(set);
        SET_LABELS.add(label);
        return set;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        // Snapshot to avoid holding the sync lock while we iterate.
        List<Map<UUID, ?>> mapsSnap;
        List<Set<UUID>>    setsSnap;
        synchronized (MAPS) { mapsSnap = new ArrayList<Map<UUID, ?>>(MAPS); }
        synchronized (SETS) { setsSnap = new ArrayList<Set<UUID>>(SETS); }
        for (Map<UUID, ?> m : mapsSnap) { try { m.remove(id); } catch (Throwable ignored) {} }
        for (Set<UUID> s : setsSnap)    { try { s.remove(id); } catch (Throwable ignored) {} }
    }

    /** Diagnostic snapshot — total entries across every registered cache. */
    public static int totalEntries() {
        int n = 0;
        synchronized (MAPS) { for (Map<UUID, ?> m : MAPS) n += m.size(); }
        synchronized (SETS) { for (Set<UUID> s : SETS) n += s.size(); }
        return n;
    }

    /** Wipe everything — called from onDisable() so /reload doesn't leave
     *  dangling references behind. */
    public static void clearAll() {
        synchronized (MAPS) { for (Map<UUID, ?> m : MAPS) try { m.clear(); } catch (Throwable ignored) {} }
        synchronized (SETS) { for (Set<UUID> s : SETS) try { s.clear(); } catch (Throwable ignored) {} }
    }

    /** Per-structure size breakdown — feed to a debug command. */
    public static List<String> describeSizes() {
        List<String> out = new ArrayList<String>();
        synchronized (MAPS) {
            for (int i = 0; i < MAPS.size(); i++)
                out.add(MAP_LABELS.get(i) + " (map) = " + MAPS.get(i).size());
        }
        synchronized (SETS) {
            for (int i = 0; i < SETS.size(); i++)
                out.add(SET_LABELS.get(i) + " (set) = " + SETS.get(i).size());
        }
        return out;
    }

    /** Install the shared listener. Call once from SoulEnchants.onEnable. */
    public static void install(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new MapManager(), plugin);
    }
}
