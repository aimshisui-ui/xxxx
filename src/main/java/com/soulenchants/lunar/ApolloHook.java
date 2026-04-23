package com.soulenchants.lunar;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.common.location.ApolloBlockLocation;
import com.lunarclient.apollo.common.icon.ItemStackIcon;
import com.lunarclient.apollo.module.cooldown.Cooldown;
import com.lunarclient.apollo.module.cooldown.CooldownModule;
import com.lunarclient.apollo.module.waypoint.Waypoint;
import com.lunarclient.apollo.module.waypoint.WaypointModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.Duration;
import java.util.Optional;

/**
 * Direct-API hook to Apollo (com.lunarclient:apollo-api:1.2.5).
 *
 * Every method is load-guarded by LunarBridge via Class.forName probe —
 * when Apollo isn't on the classpath, the JVM never loads this class, so
 * the missing com.lunarclient.apollo.* imports don't throw
 * NoClassDefFoundError.
 *
 * Keep this class narrow: its only job is to shield the rest of the
 * codebase from Apollo's types. Callers in LunarBridge use primitives +
 * Bukkit types; translation happens here.
 */
final class ApolloHook {

    private static CooldownModule cooldownModule;
    private static WaypointModule waypointModule;

    private ApolloHook() {}

    /** Cache module handles at startup. Returns true if both resolved. */
    static boolean init() {
        try {
            cooldownModule = Apollo.getModuleManager().getModule(CooldownModule.class);
            waypointModule = Apollo.getModuleManager().getModule(WaypointModule.class);
            return cooldownModule != null;
        } catch (Throwable t) {
            cooldownModule = null;
            waypointModule = null;
            return false;
        }
    }

    /**
     * Push a client-side cooldown ring above the player's hotbar. The icon
     * is the Material name ("DIAMOND_SWORD"). Cooldown name also serves as
     * the id — re-sending with the same name resets the ring.
     */
    static boolean sendCooldown(Player viewer, String name, long durationMs, Material icon) {
        if (cooldownModule == null) return false;
        Optional<ApolloPlayer> apolloPlayer = Apollo.getPlayerManager().getPlayer(viewer.getUniqueId());
        if (!apolloPlayer.isPresent()) return false;   // player not on Lunar Client
        try {
            cooldownModule.displayCooldown(apolloPlayer.get(),
                    Cooldown.builder()
                            .name(name)
                            .duration(Duration.ofMillis(durationMs))
                            .icon(ItemStackIcon.builder()
                                    .itemName(icon.name())
                                    .build())
                            .build());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Push a client-side waypoint ping to a Lunar Client player. */
    static boolean sendWaypoint(Player viewer, String name, org.bukkit.Location loc, int rgb) {
        if (waypointModule == null) return false;
        Optional<ApolloPlayer> apolloPlayer = Apollo.getPlayerManager().getPlayer(viewer.getUniqueId());
        if (!apolloPlayer.isPresent()) return false;
        try {
            waypointModule.displayWaypoint(apolloPlayer.get(),
                    Waypoint.builder()
                            .name(name)
                            .location(ApolloBlockLocation.builder()
                                    .world(loc.getWorld().getName())
                                    .x(loc.getBlockX())
                                    .y(loc.getBlockY())
                                    .z(loc.getBlockZ())
                                    .build())
                            .color(new Color(rgb))
                            .preventRemoval(false)
                            .hidden(false)
                            .build());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Clear a pushed cooldown early (e.g. ability re-triggered, refund, etc.). */
    static boolean clearCooldown(Player viewer, String name) {
        if (cooldownModule == null) return false;
        Optional<ApolloPlayer> apolloPlayer = Apollo.getPlayerManager().getPlayer(viewer.getUniqueId());
        if (!apolloPlayer.isPresent()) return false;
        try {
            cooldownModule.removeCooldown(apolloPlayer.get(), name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
