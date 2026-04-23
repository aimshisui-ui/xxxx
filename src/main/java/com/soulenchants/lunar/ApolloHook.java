package com.soulenchants.lunar;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.common.icon.ItemStackIcon;
import com.lunarclient.apollo.common.location.ApolloBlockLocation;
import com.lunarclient.apollo.common.location.ApolloLocation;
import com.lunarclient.apollo.module.cooldown.Cooldown;
import com.lunarclient.apollo.module.cooldown.CooldownModule;
import com.lunarclient.apollo.module.hologram.Hologram;
import com.lunarclient.apollo.module.hologram.HologramModule;
import com.lunarclient.apollo.module.notification.Notification;
import com.lunarclient.apollo.module.notification.NotificationModule;
import com.lunarclient.apollo.module.title.Title;
import com.lunarclient.apollo.module.title.TitleModule;
import com.lunarclient.apollo.module.title.TitleType;
import com.lunarclient.apollo.module.waypoint.Waypoint;
import com.lunarclient.apollo.module.waypoint.WaypointModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

    private static CooldownModule      cooldownModule;
    private static WaypointModule      waypointModule;
    private static HologramModule      hologramModule;
    private static NotificationModule  notificationModule;
    private static TitleModule         titleModule;

    /** Legacy-codes → Adventure Components so our existing §-colored strings
     *  render correctly on Lunar clients. '§' and '&' both parse. */
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('§').hexColors().build();

    private ApolloHook() {}

    /** Cache module handles at startup. Returns true if the minimum viable
     *  module set (cooldown) resolved — optional modules are tolerated as null. */
    static boolean init() {
        try {
            cooldownModule     = safeGet(CooldownModule.class);
            waypointModule     = safeGet(WaypointModule.class);
            hologramModule     = safeGet(HologramModule.class);
            notificationModule = safeGet(NotificationModule.class);
            titleModule        = safeGet(TitleModule.class);
            return cooldownModule != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static <T extends com.lunarclient.apollo.module.ApolloModule> T safeGet(Class<T> cls) {
        try { return Apollo.getModuleManager().getModule(cls); }
        catch (Throwable t) { return null; }
    }

    private static Optional<ApolloPlayer> ap(Player viewer) {
        if (viewer == null) return Optional.empty();
        try { return Apollo.getPlayerManager().getPlayer(viewer.getUniqueId()); }
        catch (Throwable t) { return Optional.empty(); }
    }

    // ──────────────────────── Cooldown ────────────────────────

    static boolean sendCooldown(Player viewer, String name, long durationMs, Material icon) {
        if (cooldownModule == null) return false;
        Optional<ApolloPlayer> a = ap(viewer);
        if (!a.isPresent()) return false;
        try {
            cooldownModule.displayCooldown(a.get(),
                    Cooldown.builder()
                            .name(name)
                            .duration(Duration.ofMillis(durationMs))
                            .icon(ItemStackIcon.builder()
                                    .itemName(icon.name())
                                    .build())
                            .build());
            return true;
        } catch (Throwable t) { return false; }
    }

    static boolean clearCooldown(Player viewer, String name) {
        if (cooldownModule == null) return false;
        Optional<ApolloPlayer> a = ap(viewer);
        if (!a.isPresent()) return false;
        try { cooldownModule.removeCooldown(a.get(), name); return true; }
        catch (Throwable t) { return false; }
    }

    // ──────────────────────── Waypoint ────────────────────────

    static boolean sendWaypoint(Player viewer, String name, org.bukkit.Location loc, int rgb) {
        if (waypointModule == null) return false;
        Optional<ApolloPlayer> a = ap(viewer);
        if (!a.isPresent()) return false;
        try {
            waypointModule.displayWaypoint(a.get(),
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
        } catch (Throwable t) { return false; }
    }

    static boolean clearWaypoint(Player viewer, String name) {
        if (waypointModule == null) return false;
        Optional<ApolloPlayer> a = ap(viewer);
        if (!a.isPresent()) return false;
        try { waypointModule.removeWaypoint(a.get(), name); return true; }
        catch (Throwable t) { return false; }
    }

    // ──────────────────────── Title ────────────────────────

    /** Send an enhanced Lunar title (also renders as a vanilla title for
     *  non-Lunar players via the caller's fallback). `subTitle` may be null. */
    static boolean sendTitleBroadcast(String title, String subTitle,
                                      long fadeInMs, long stayMs, long fadeOutMs,
                                      float scale) {
        if (titleModule == null) return false;
        try {
            com.lunarclient.apollo.recipients.Recipients all =
                    com.lunarclient.apollo.recipients.Recipients.ofEveryone();
            Title main = Title.builder()
                    .type(TitleType.TITLE)
                    .message(LEGACY.deserialize(title == null ? "" : title))
                    .scale(scale)
                    .fadeInTime(Duration.ofMillis(fadeInMs))
                    .displayTime(Duration.ofMillis(stayMs))
                    .fadeOutTime(Duration.ofMillis(fadeOutMs))
                    .interpolationScale(scale)
                    .interpolationRate(0f)
                    .build();
            titleModule.displayTitle(all, main);
            if (subTitle != null && !subTitle.isEmpty()) {
                Title sub = Title.builder()
                        .type(TitleType.SUBTITLE)
                        .message(LEGACY.deserialize(subTitle))
                        .scale(Math.max(0.75f, scale * 0.75f))
                        .fadeInTime(Duration.ofMillis(fadeInMs))
                        .displayTime(Duration.ofMillis(stayMs))
                        .fadeOutTime(Duration.ofMillis(fadeOutMs))
                        .interpolationScale(Math.max(0.75f, scale * 0.75f))
                        .interpolationRate(0f)
                        .build();
                titleModule.displayTitle(all, sub);
            }
            return true;
        } catch (Throwable t) { return false; }
    }

    /** Per-player enhanced title — used when only one player should see it. */
    static boolean sendTitlePlayer(Player viewer, String title, String subTitle,
                                   long fadeInMs, long stayMs, long fadeOutMs,
                                   float scale) {
        if (titleModule == null) return false;
        Optional<ApolloPlayer> a = ap(viewer);
        if (!a.isPresent()) return false;
        try {
            Title main = Title.builder()
                    .type(TitleType.TITLE)
                    .message(LEGACY.deserialize(title == null ? "" : title))
                    .scale(scale)
                    .fadeInTime(Duration.ofMillis(fadeInMs))
                    .displayTime(Duration.ofMillis(stayMs))
                    .fadeOutTime(Duration.ofMillis(fadeOutMs))
                    .interpolationScale(scale)
                    .interpolationRate(0f)
                    .build();
            titleModule.displayTitle(a.get(), main);
            if (subTitle != null && !subTitle.isEmpty()) {
                Title sub = Title.builder()
                        .type(TitleType.SUBTITLE)
                        .message(LEGACY.deserialize(subTitle))
                        .scale(Math.max(0.75f, scale * 0.75f))
                        .fadeInTime(Duration.ofMillis(fadeInMs))
                        .displayTime(Duration.ofMillis(stayMs))
                        .fadeOutTime(Duration.ofMillis(fadeOutMs))
                        .interpolationScale(Math.max(0.75f, scale * 0.75f))
                        .interpolationRate(0f)
                        .build();
                titleModule.displayTitle(a.get(), sub);
            }
            return true;
        } catch (Throwable t) { return false; }
    }

    // ──────────────────────── Notification (toast popups) ────────────────────────

    static boolean sendNotification(Player viewer, String title, String description,
                                    long durationMs, String resourceLocation) {
        if (notificationModule == null) return false;
        Optional<ApolloPlayer> a = ap(viewer);
        if (!a.isPresent()) return false;
        try {
            Notification.NotificationBuilder b = Notification.builder()
                    .title(title == null ? "" : title)
                    .description(description == null ? "" : description)
                    .displayTime(Duration.ofMillis(durationMs));
            if (resourceLocation != null && !resourceLocation.isEmpty()) {
                b = b.resourceLocation(resourceLocation);
            }
            notificationModule.displayNotification(a.get(), b.build());
            return true;
        } catch (Throwable t) { return false; }
    }

    // ──────────────────────── Hologram ────────────────────────

    static boolean displayHologram(String id, org.bukkit.Location loc, List<String> lines,
                                   boolean showThroughWalls) {
        if (hologramModule == null || loc == null || loc.getWorld() == null) return false;
        try {
            List<Component> components = new ArrayList<>(lines.size());
            for (String s : lines) components.add(LEGACY.deserialize(s == null ? "" : s));
            Hologram h = Hologram.builder()
                    .id(id)
                    .location(ApolloLocation.builder()
                            .world(loc.getWorld().getName())
                            .x(loc.getX())
                            .y(loc.getY())
                            .z(loc.getZ())
                            .build())
                    .lines(components)
                    .showThroughWalls(showThroughWalls)
                    .showShadow(true)
                    .showBackground(true)
                    .build();
            hologramModule.displayHologram(com.lunarclient.apollo.recipients.Recipients.ofEveryone(), h);
            return true;
        } catch (Throwable t) { return false; }
    }

    static boolean removeHologram(String id) {
        if (hologramModule == null) return false;
        try {
            hologramModule.removeHologram(
                    com.lunarclient.apollo.recipients.Recipients.ofEveryone(), id);
            return true;
        } catch (Throwable t) { return false; }
    }

}
