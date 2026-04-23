package com.soulenchants.lunar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Reflection-based bridge to LunarClient API. If the plugin isn't loaded, all
 * methods no-op silently. Supports two optional backends:
 *   • LunarClient-API (Moonsworth's "LunarBukkitAPI" / "LunarClientAPI")
 *   • LunarAPI — older community fork
 *
 * Reflection target methods are discovered at first call and cached. No
 * compile-time dep on Lunar jars means SoulEnchants still builds in a clean
 * environment.
 */
public final class LunarBridge {

    private static Logger log;
    private static Object lunarApiInstance;
    private static Method waypointMethod;     // void sendWaypoint(Player, String name, Location, int colorRGB)
    private static Method cooldownMethod;     // void sendCooldown(Player, String id, long durationMs)
    private static Method titleMethod;        // void sendTitle(Player, String title, String subtitle, long durationMs)
    private static boolean probed;

    private LunarBridge() {}

    /** Call once during plugin enable. Quietly detects availability. */
    public static void init(org.bukkit.plugin.java.JavaPlugin plugin) {
        log = plugin.getLogger();
        probe();
        if (isAvailable()) log.info("[lunar] API detected — pings/cosmetics enabled");
        else log.info("[lunar] API not present — pings/cosmetics will no-op");
    }

    public static boolean isAvailable() { return lunarApiInstance != null; }

    /** Send a pingable waypoint to a single player on Lunar. No-op otherwise. */
    public static void sendWaypoint(Player p, String name, Location loc, int rgb) {
        if (!isAvailable() || waypointMethod == null) return;
        try {
            waypointMethod.invoke(lunarApiInstance, p, name, loc, rgb);
        } catch (Throwable t) {
            // Fail-silent — Lunar API fluctuates across versions.
        }
    }

    /** Attempt a client-side cooldown overlay for an ability. */
    public static void sendCooldown(Player p, String id, long durationMs) {
        if (!isAvailable() || cooldownMethod == null) return;
        try { cooldownMethod.invoke(lunarApiInstance, p, id, durationMs); }
        catch (Throwable ignored) {}
    }

    /** Client-side title via LC — renders cleaner than vanilla on Lunar. */
    public static void sendTitle(Player p, String title, String subtitle, long durationMs) {
        if (!isAvailable() || titleMethod == null) return;
        try { titleMethod.invoke(lunarApiInstance, p, title, subtitle, durationMs); }
        catch (Throwable ignored) {}
    }

    // ──────────────── Reflection probing ────────────────
    private static void probe() {
        if (probed) return;
        probed = true;
        String[] pluginNames = { "LunarClient-API", "LunarBukkitAPI", "LunarClientAPI", "LunarAPI" };
        org.bukkit.plugin.Plugin found = null;
        for (String n : pluginNames) {
            found = Bukkit.getPluginManager().getPlugin(n);
            if (found != null) break;
        }
        if (found == null) return;
        try {
            Class<?> cls = found.getClass();
            // Common naming patterns in Lunar's API
            // (1) static getApi() / (2) getInstance() / (3) singleton field "INSTANCE"
            Object api = null;
            try { api = cls.getMethod("getApi").invoke(null); } catch (Throwable ignored) {}
            if (api == null) try { api = cls.getMethod("getInstance").invoke(null); } catch (Throwable ignored) {}
            if (api == null) api = found;  // fall back to plugin instance
            lunarApiInstance = api;
            // Cache method references; swallow each independently.
            for (Method m : api.getClass().getMethods()) {
                String n = m.getName().toLowerCase();
                if (waypointMethod == null && n.contains("waypoint")) waypointMethod = m;
                else if (cooldownMethod == null && n.contains("cooldown")) cooldownMethod = m;
                else if (titleMethod == null && n.contains("title")) titleMethod = m;
            }
        } catch (Throwable t) {
            if (log != null) log.warning("[lunar] probe failed: " + t);
        }
    }
}
