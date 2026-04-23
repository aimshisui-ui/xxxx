package com.soulenchants.lunar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Dual-path bridge to Lunar Client server APIs. We try the modern Apollo
 * API first (com.lunarclient:apollo-api), and fall back to the legacy
 * LunarClient-API via reflection if Apollo isn't installed.
 *
 * Apollo is the current, supported API — see https://lunarclient.dev/apollo
 * Legacy is kept only for servers still running the old Moonsworth plugin.
 *
 *    backend    | plugin name        | notes
 *    ───────────┼────────────────────┼──────────────────────────────────────
 *    Apollo     | Apollo             | preferred; uses type-safe ApolloHook
 *    legacy     | LunarClient-API    | reflection; LCCooldown DTO shape
 *
 * When neither is installed, every bridge call no-ops silently. Admins
 * can verify status with /lunar status.
 */
public final class LunarBridge {

    private static Logger log;
    private static boolean probed;

    /** Which backend resolved — null, "apollo", or "legacy". */
    private static String backend;

    // Legacy backend reflection handles
    private static Object      lunarApiInstance;
    private static Method      legacySendCooldown;
    private static Constructor<?> legacyCooldownCtor;
    private static Method      legacySendWaypoint;
    private static Constructor<?> legacyWaypointCtor;

    private LunarBridge() {}

    public static void init(org.bukkit.plugin.java.JavaPlugin plugin) {
        log = plugin.getLogger();
        probe();
        if ("apollo".equals(backend)) {
            log.info("[lunar] Apollo API detected — cooldowns + waypoints enabled");
        } else if ("legacy".equals(backend)) {
            log.info("[lunar] legacy LunarClient-API detected — cooldowns=" + (legacySendCooldown != null)
                    + " waypoints=" + (legacySendWaypoint != null));
        } else {
            log.info("[lunar] no Lunar server API present — install Apollo to enable cooldowns/waypoints");
        }
    }

    public static boolean isAvailable() { return backend != null; }
    public static String  backend()     { return backend; }

    // ──────────────────────── Cooldown ────────────────────────

    public static void sendCooldown(Player p, String id, long durationMs) {
        sendCooldown(p, id, durationMs, Material.DIAMOND_SWORD);
    }

    public static void sendCooldown(Player p, String id, long durationMs, Material icon) {
        if ("apollo".equals(backend)) {
            ApolloHook.sendCooldown(p, id, durationMs, icon);
        } else if ("legacy".equals(backend) && legacySendCooldown != null && legacyCooldownCtor != null) {
            try {
                Object cooldown = legacyCooldownCtor.newInstance(id, durationMs, icon);
                legacySendCooldown.invoke(lunarApiInstance, p, cooldown);
            } catch (Throwable t) {
                if (log != null) log.fine("[lunar-legacy] sendCooldown failed: " + t);
            }
        }
    }

    // ──────────────────────── Waypoint ────────────────────────

    public static void sendWaypoint(Player p, String name, Location loc, int rgb) {
        if ("apollo".equals(backend)) {
            ApolloHook.sendWaypoint(p, name, loc, rgb);
        } else if ("legacy".equals(backend) && legacySendWaypoint != null && legacyWaypointCtor != null) {
            try {
                Object waypoint = legacyWaypointCtor.newInstance(
                        name, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), rgb, false, true);
                legacySendWaypoint.invoke(lunarApiInstance, p, waypoint);
            } catch (Throwable t) {
                if (log != null) log.fine("[lunar-legacy] sendWaypoint failed: " + t);
            }
        }
    }

    /** Clear a previously-pushed waypoint by name — use on boss death / ping expiry. */
    public static void clearWaypoint(Player p, String name) {
        if ("apollo".equals(backend)) ApolloHook.clearWaypoint(p, name);
        // Legacy API has no remove equivalent exposed.
    }

    /** Clear a previously-pushed cooldown by name. */
    public static void clearCooldown(Player p, String name) {
        if ("apollo".equals(backend)) ApolloHook.clearCooldown(p, name);
    }

    // ──────────────────────── Title ────────────────────────

    /** Enhanced-title broadcast to every Lunar-equipped player. Returns true if
     *  at least Apollo's TitleModule fired. Callers should still call the
     *  vanilla sendTitle() for non-Lunar users. */
    public static boolean broadcastTitle(String title, String subtitle,
                                         long fadeInMs, long stayMs, long fadeOutMs, float scale) {
        if ("apollo".equals(backend)) {
            return ApolloHook.sendTitleBroadcast(title, subtitle, fadeInMs, stayMs, fadeOutMs, scale);
        }
        return false;
    }

    public static boolean sendTitle(Player p, String title, String subtitle,
                                    long fadeInMs, long stayMs, long fadeOutMs, float scale) {
        if ("apollo".equals(backend)) {
            return ApolloHook.sendTitlePlayer(p, title, subtitle, fadeInMs, stayMs, fadeOutMs, scale);
        }
        return false;
    }

    // ──────────────────────── Notification (toast) ────────────────────────

    public static boolean sendNotification(Player p, String title, String description, long durationMs) {
        return sendNotification(p, title, description, durationMs, null);
    }

    public static boolean sendNotification(Player p, String title, String description,
                                           long durationMs, String resourceLocation) {
        if ("apollo".equals(backend)) {
            return ApolloHook.sendNotification(p, title, description, durationMs, resourceLocation);
        }
        return false;
    }

    // ──────────────────────── Hologram ────────────────────────

    public static boolean displayHologram(String id, Location loc, java.util.List<String> lines) {
        return displayHologram(id, loc, lines, true);
    }

    public static boolean displayHologram(String id, Location loc, java.util.List<String> lines,
                                          boolean showThroughWalls) {
        if ("apollo".equals(backend)) {
            return ApolloHook.displayHologram(id, loc, lines, showThroughWalls);
        }
        return false;
    }

    public static boolean removeHologram(String id) {
        if ("apollo".equals(backend)) return ApolloHook.removeHologram(id);
        return false;
    }

    // ──────────────────────── Rich Presence (Discord RPC) ────────────────────────

    public static boolean setRichPresence(Player p, String gameName, String gameVariant,
                                          String gameState, String playerState,
                                          String mapName, String subServerName) {
        if ("apollo".equals(backend)) {
            return ApolloHook.sendRichPresence(p, gameName, gameVariant, gameState, playerState,
                    mapName, subServerName);
        }
        return false;
    }

    public static boolean resetRichPresence(Player p) {
        if ("apollo".equals(backend)) return ApolloHook.resetRichPresence(p);
        return false;
    }

    // ──────────────────────── Probe ────────────────────────

    /**
     * Resolve the best available backend:
     *   1. Apollo — if com.lunarclient.apollo.Apollo is on the classpath
     *   2. Legacy LunarClient-API — if com.lunarclient.bukkitapi.LunarClientAPI is
     *   3. Nothing — no-op all calls
     *
     * Apollo is class-path-checked (not plugin-checked) because the Apollo
     * Bukkit jar exposes its API classes through Bukkit's plugin classloader
     * the moment we softdepend on it in plugin.yml.
     */
    private static void probe() {
        if (probed) return;
        probed = true;

        // 1) Apollo — preferred modern path.
        if (classPresent("com.lunarclient.apollo.Apollo")) {
            try {
                if (ApolloHook.init()) {
                    backend = "apollo";
                    return;
                }
                if (log != null) log.warning("[lunar] Apollo class present but module init failed");
            } catch (Throwable t) {
                if (log != null) log.warning("[lunar] Apollo init threw: " + t);
            }
        }

        // 2) Legacy LunarClient-API — reflection, DTO constructor (String, long, Material)
        if (probeLegacy()) {
            backend = "legacy";
        }
    }

    private static boolean classPresent(String name) {
        try { Class.forName(name); return true; }
        catch (ClassNotFoundException e) { return false; }
    }

    private static boolean probeLegacy() {
        String[] pluginNames = { "LunarClient-API", "LunarBukkitAPI", "LunarClientAPI", "LunarAPI" };
        org.bukkit.plugin.Plugin found = null;
        for (String n : pluginNames) {
            found = Bukkit.getPluginManager().getPlugin(n);
            if (found != null) break;
        }
        if (found == null) return false;

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
        if (apiClass == null) return false;
        try { lunarApiInstance = apiClass.getMethod("getInstance").invoke(null); }
        catch (Throwable t) { lunarApiInstance = found; }
        if (lunarApiInstance == null) return false;

        // DTOs
        String[] cooldownClasses = {
                "com.lunarclient.bukkitapi.object.LCCooldown",
                "com.lunarclient.bukkitapi.cooldown.LCCooldown",
                "com.lunarclient.bukkitapi.LCCooldown"
        };
        Class<?> cdCls = firstClass(cooldownClasses);
        if (cdCls != null) {
            try { legacyCooldownCtor = cdCls.getConstructor(String.class, long.class, Material.class); }
            catch (NoSuchMethodException e) {
                try { legacyCooldownCtor = cdCls.getConstructor(String.class, long.class, int.class); }
                catch (NoSuchMethodException ignored) {}
            }
            legacySendCooldown = findMethod(lunarApiInstance.getClass(), "sendCooldown", Player.class, cdCls);
        }
        String[] waypointClasses = {
                "com.lunarclient.bukkitapi.object.LCWaypoint",
                "com.lunarclient.bukkitapi.waypoint.LCWaypoint",
                "com.lunarclient.bukkitapi.LCWaypoint"
        };
        Class<?> wpCls = firstClass(waypointClasses);
        if (wpCls != null) {
            try {
                legacyWaypointCtor = wpCls.getConstructor(
                        String.class, int.class, int.class, int.class, int.class, boolean.class, boolean.class);
            } catch (NoSuchMethodException ignored) {}
            legacySendWaypoint = findMethod(lunarApiInstance.getClass(), "sendWaypoint", Player.class, wpCls);
        }
        return legacySendCooldown != null || legacySendWaypoint != null;
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
}
