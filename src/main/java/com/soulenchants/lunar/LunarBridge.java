package com.soulenchants.lunar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Reflection-based bridge to Moonsworth's LunarClient-API. We compile against
 * no Lunar jar so the plugin builds clean in any environment; at runtime we
 * try to load the Lunar plugin's concrete classes and cache their handles.
 *
 * Real API surface (1.8.8 Moonsworth "LunarClient-API"):
 *   com.lunarclient.bukkitapi.LunarClientAPI          (main plugin class + singleton)
 *     .getInstance()                                  → singleton
 *     .sendWaypoint(Player, LCWaypoint)
 *     .sendCooldown(Player, LCCooldown)
 *     .sendTitle   (Player, LCTitle)
 *   com.lunarclient.bukkitapi.object.LCWaypoint       (String name, x, y, z, int color, boolean forced, boolean visible)
 *   com.lunarclient.bukkitapi.object.LCCooldown       (String id, long durationMs, Material icon)
 *   com.lunarclient.bukkitapi.object.LCTitle          (varies by version)
 *
 * If the plugin isn't installed, every bridge method no-ops silently.
 */
public final class LunarBridge {

    private static Logger log;
    private static boolean probed;

    // Cached handles — all null if Lunar isn't present.
    private static Object      lunarApiInstance;
    private static Method      sendCooldownMethod;
    private static Constructor<?> cooldownCtor;
    private static Method      sendWaypointMethod;
    private static Constructor<?> waypointCtor;
    private static Method      sendTitleMethod;

    private LunarBridge() {}

    public static void init(org.bukkit.plugin.java.JavaPlugin plugin) {
        log = plugin.getLogger();
        probe();
        if (isAvailable()) {
            log.info("[lunar] LunarClient-API detected @ " + lunarApiInstance.getClass().getName()
                    + " — cooldowns=" + (sendCooldownMethod != null)
                    + " waypoints=" + (sendWaypointMethod != null)
                    + " titles=" + (sendTitleMethod != null));
        } else {
            log.info("[lunar] LunarClient-API not present — pings/cosmetics will no-op");
        }
    }

    public static boolean isAvailable() { return lunarApiInstance != null; }

    // ──────────────────────── Cooldown ────────────────────────

    /**
     * Push a client-side cooldown ring to the given player. The icon is
     * rendered in the ring on Lunar Client. No-op if Lunar absent, call
     * failed, or the player isn't on Lunar (Lunar itself handles that
     * check server-side).
     *
     * @param id        Unique cooldown id — also the label on the ring.
     *                  Same id re-sent with new duration resets the ring.
     * @param durationMs Cooldown length in milliseconds.
     */
    public static void sendCooldown(Player p, String id, long durationMs) {
        sendCooldown(p, id, durationMs, Material.DIAMOND_SWORD);
    }

    public static void sendCooldown(Player p, String id, long durationMs, Material icon) {
        if (!isAvailable() || sendCooldownMethod == null || cooldownCtor == null) return;
        try {
            Object cooldown = cooldownCtor.newInstance(id, durationMs, icon);
            sendCooldownMethod.invoke(lunarApiInstance, p, cooldown);
        } catch (Throwable t) {
            if (log != null) log.fine("[lunar] sendCooldown failed: " + t);
        }
    }

    // ──────────────────────── Waypoint ────────────────────────

    public static void sendWaypoint(Player p, String name, Location loc, int rgb) {
        if (!isAvailable() || sendWaypointMethod == null || waypointCtor == null) return;
        try {
            // Moonsworth LCWaypoint(String name, int x, int y, int z, int color, boolean forced, boolean visible)
            Object waypoint = waypointCtor.newInstance(
                    name, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), rgb, false, true);
            sendWaypointMethod.invoke(lunarApiInstance, p, waypoint);
        } catch (Throwable t) {
            if (log != null) log.fine("[lunar] sendWaypoint failed: " + t);
        }
    }

    // ──────────────────────── Title ────────────────────────

    public static void sendTitle(Player p, String title, String subtitle, long durationMs) {
        if (!isAvailable() || sendTitleMethod == null) return;
        try { sendTitleMethod.invoke(lunarApiInstance, p, title, subtitle, durationMs); }
        catch (Throwable ignored) {}
    }

    // ──────────────────────── Probe ────────────────────────

    /**
     * Discover the Moonsworth API classes + methods. Tries multiple
     * candidate class names since Nordic's recon noted forks exist
     * (LunarClient-API, LunarBukkitAPI, LunarAPI, etc.).
     */
    private static void probe() {
        if (probed) return;
        probed = true;

        // 1. Find the Lunar plugin instance.
        String[] pluginNames = { "LunarClient-API", "LunarBukkitAPI", "LunarClientAPI", "LunarAPI" };
        org.bukkit.plugin.Plugin found = null;
        for (String n : pluginNames) {
            found = Bukkit.getPluginManager().getPlugin(n);
            if (found != null) break;
        }
        if (found == null) return;

        // 2. Resolve the API singleton. Moonsworth uses LunarClientAPI.getInstance().
        String[] apiClasses = {
                "com.lunarclient.bukkitapi.LunarClientAPI",
                "com.lunarclient.bukkitapi.LunarBukkitAPI",
                "com.moonsworth.lunar.bukkit.LunarClientAPI"
        };
        Class<?> apiClass = null;
        for (String cn : apiClasses) {
            try { apiClass = Class.forName(cn); break; }
            catch (ClassNotFoundException ignored) {}
        }
        if (apiClass == null) {
            if (log != null) log.warning("[lunar] plugin " + found.getName()
                    + " is loaded but its API class wasn't found on any known path");
            return;
        }
        try {
            Method getInstance = apiClass.getMethod("getInstance");
            lunarApiInstance = getInstance.invoke(null);
        } catch (Throwable t) {
            // Fall back to the plugin instance itself.
            lunarApiInstance = found;
        }
        if (lunarApiInstance == null) return;

        // 3. Resolve DTOs + their send methods.
        String[] cooldownClasses = {
                "com.lunarclient.bukkitapi.object.LCCooldown",
                "com.lunarclient.bukkitapi.cooldown.LCCooldown",
                "com.lunarclient.bukkitapi.LCCooldown"
        };
        Class<?> cooldownClass = firstClass(cooldownClasses);
        if (cooldownClass != null) {
            try {
                cooldownCtor = cooldownClass.getConstructor(String.class, long.class, Material.class);
            } catch (NoSuchMethodException e) {
                // Older variant: (String, long, int material-id)
                try { cooldownCtor = cooldownClass.getConstructor(String.class, long.class, int.class); }
                catch (NoSuchMethodException ignored) {}
            }
            sendCooldownMethod = findMethod(lunarApiInstance.getClass(), "sendCooldown", Player.class, cooldownClass);
            if (sendCooldownMethod == null) {
                // Some forks rename it — scan by Player+(matching DTO class) signature.
                sendCooldownMethod = findMethodBySig(lunarApiInstance.getClass(), "cooldown", Player.class, cooldownClass);
            }
        }

        String[] waypointClasses = {
                "com.lunarclient.bukkitapi.object.LCWaypoint",
                "com.lunarclient.bukkitapi.waypoint.LCWaypoint",
                "com.lunarclient.bukkitapi.LCWaypoint"
        };
        Class<?> waypointClass = firstClass(waypointClasses);
        if (waypointClass != null) {
            try {
                waypointCtor = waypointClass.getConstructor(
                        String.class, int.class, int.class, int.class, int.class, boolean.class, boolean.class);
            } catch (NoSuchMethodException ignored) {}
            sendWaypointMethod = findMethod(lunarApiInstance.getClass(), "sendWaypoint", Player.class, waypointClass);
            if (sendWaypointMethod == null) {
                sendWaypointMethod = findMethodBySig(lunarApiInstance.getClass(), "waypoint", Player.class, waypointClass);
            }
        }

        // Title — not critical; skip if missing. We don't currently call it.
        for (Method m : lunarApiInstance.getClass().getMethods()) {
            if (m.getName().toLowerCase().contains("title")
                    && m.getParameterTypes().length >= 2
                    && m.getParameterTypes()[0] == Player.class) {
                sendTitleMethod = m; break;
            }
        }
    }

    private static Class<?> firstClass(String[] candidates) {
        for (String cn : candidates) {
            try { return Class.forName(cn); }
            catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... args) {
        try { return cls.getMethod(name, args); }
        catch (NoSuchMethodException e) { return null; }
    }

    /** Flexible match — name contains substring, param types match exactly. */
    private static Method findMethodBySig(Class<?> cls, String nameContains, Class<?>... args) {
        for (Method m : cls.getMethods()) {
            if (!m.getName().toLowerCase().contains(nameContains.toLowerCase())) continue;
            Class<?>[] pt = m.getParameterTypes();
            if (pt.length != args.length) continue;
            boolean ok = true;
            for (int i = 0; i < pt.length; i++) {
                if (!pt[i].isAssignableFrom(args[i])) { ok = false; break; }
            }
            if (ok) return m;
        }
        return null;
    }
}
