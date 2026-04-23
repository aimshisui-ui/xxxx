package com.soulenchants.lunar;

import com.soulenchants.SoulEnchants;
import com.soulenchants.util.FloatingText;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lunar-first UX façade — every method transparently upgrades to the Apollo
 * implementation when Lunar is present, then falls back to vanilla 1.8
 * equivalents so non-Lunar players never get dropped.
 *
 * Using this from listeners/bosses keeps the vanilla-fallback pattern in
 * exactly one place — call sites become one-liners and don't need to
 * know whether Apollo is available.
 */
public final class LunarFx {

    private LunarFx() {}

    // ──────────────────────── Titles ────────────────────────

    /** Boss-spawn-style broadcast: big enhanced title for Lunar users + the
     *  vanilla sendTitle() for everyone (so non-Lunar still sees a banner).
     *  Fade values in milliseconds. */
    public static void broadcastTitle(String title, String sub,
                                      long fadeInMs, long stayMs, long fadeOutMs,
                                      float scale) {
        // Lunar path — renders at true size w/ fade interpolation.
        LunarBridge.broadcastTitle(title, sub, fadeInMs, stayMs, fadeOutMs, scale);
        // Vanilla fallback — same banner for non-Lunar clients.
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.sendTitle(title == null ? "" : title, sub == null ? "" : sub); }
            catch (Throwable ignored) {}
        }
    }

    public static void broadcastTitle(String title, String sub) {
        broadcastTitle(title, sub, 250L, 2000L, 500L, 1.25f);
    }

    /** Per-player enhanced title with vanilla fallback. */
    public static void sendTitle(Player p, String title, String sub,
                                 long fadeInMs, long stayMs, long fadeOutMs, float scale) {
        LunarBridge.sendTitle(p, title, sub, fadeInMs, stayMs, fadeOutMs, scale);
        try { p.sendTitle(title == null ? "" : title, sub == null ? "" : sub); }
        catch (Throwable ignored) {}
    }

    public static void sendTitle(Player p, String title, String sub) {
        sendTitle(p, title, sub, 200L, 1800L, 400L, 1.0f);
    }

    // ──────────────────────── Notifications (toast) ────────────────────────

    /** Lunar toast popup. Falls back to a chat line so non-Lunar players
     *  still get the signal. */
    public static void notify(Player p, String title, String body) {
        notify(p, title, body, 3500L);
    }

    public static void notify(Player p, String title, String body, long durationMs) {
        if (!LunarBridge.sendNotification(p, title, body, durationMs)) {
            try { p.sendMessage("§8[§5Soul§8] §f" + title + " §7— " + body); }
            catch (Throwable ignored) {}
        }
    }

    // ──────────────────────── Holograms ────────────────────────

    /** Floating text near a Location — uses Apollo Hologram when available,
     *  falls back to the ArmorStand-based FloatingText for vanilla users.
     *  Auto-clears after the given tick lifetime. */
    public static void floatingText(SoulEnchants plugin, Location loc, String text, int durationTicks) {
        floatingText(plugin, loc, Collections.singletonList(text), durationTicks);
    }

    public static void floatingText(SoulEnchants plugin, Location loc, List<String> lines, int durationTicks) {
        final String id = "se_fx_" + UUID.randomUUID();
        boolean apollo = LunarBridge.displayHologram(id, loc.clone().add(0, 1.5, 0), lines);
        if (apollo) {
            new BukkitRunnable() {
                @Override public void run() { LunarBridge.removeHologram(id); }
            }.runTaskLater(plugin, Math.max(1, durationTicks));
        } else {
            // Fallback — one ArmorStand per line stacked vertically.
            double yOff = 1.5;
            for (String ln : lines) {
                FloatingText.show(plugin, loc.clone().add(0, yOff, 0), ln, durationTicks);
                yOff -= 0.28;
            }
        }
    }

    // ──────────────────────── Persistent boss HP hologram ────────────────────────

    /** Long-lived hologram driven by a provider lambda — refreshes every
     *  `periodTicks` until stop is called. One per ID; displaying the same id
     *  twice overwrites the previous hologram. */
    public static final class BossBar {
        private final SoulEnchants plugin;
        private final String id;
        private BukkitRunnable task;
        private boolean apolloActive;

        public BossBar(SoulEnchants plugin, String id) {
            this.plugin = plugin;
            this.id = id;
        }

        /** Start a live-updating hologram above `locSupplier` with lines from
         *  `linesSupplier`. Location + lines are re-queried every period so
         *  a moving boss's bar follows it. Apollo is preferred; if Apollo
         *  isn't present, this no-ops (FloatingText has no long-lived API). */
        public void start(java.util.function.Supplier<Location> locSupplier,
                          java.util.function.Supplier<List<String>> linesSupplier,
                          long periodTicks) {
            stop();
            if (!LunarBridge.isAvailable()) return;  // nothing to update without Apollo
            task = new BukkitRunnable() {
                @Override public void run() {
                    Location loc = locSupplier.get();
                    if (loc == null) { stop(); return; }
                    List<String> lines = linesSupplier.get();
                    if (lines == null || lines.isEmpty()) { stop(); return; }
                    apolloActive = LunarBridge.displayHologram(id, loc.clone().add(0, 2.0, 0), lines);
                }
            };
            task.runTaskTimer(plugin, 1L, Math.max(1, periodTicks));
        }

        public void stop() {
            if (task != null) { try { task.cancel(); } catch (Throwable ignored) {} task = null; }
            if (apolloActive) { LunarBridge.removeHologram(id); apolloActive = false; }
        }
    }

    // ──────────────────────── Rich Presence ────────────────────────

    /** Per-player Discord RPC engage state. Tracked so we only push a new
     *  presence when it actually changes, and so LogoutListener can
     *  reset on quit. */
    private static final Set<UUID> rpcActive = new HashSet<>();

    public static void setBossEngage(Player p, String bossName, String phase) {
        if (p == null) return;
        boolean ok = LunarBridge.setRichPresence(p,
                "FabledMC",                                           // gameName
                "SoulEnchants",                                       // gameVariant
                "Boss Fight",                                         // gameState
                "Fighting " + bossName + (phase == null ? "" : " [" + phase + "]"),  // playerState
                bossName,                                             // mapName
                null);                                                // subServer
        if (ok) rpcActive.add(p.getUniqueId());
    }

    public static void setIdle(Player p) {
        if (p == null) return;
        LunarBridge.setRichPresence(p, "FabledMC", "SoulEnchants", "Exploring", "On FabledMC", null, null);
        rpcActive.add(p.getUniqueId());
    }

    public static void clearPresence(Player p) {
        if (p == null) return;
        if (rpcActive.remove(p.getUniqueId())) LunarBridge.resetRichPresence(p);
    }

    // ──────────────────────── Helpers ────────────────────────

    public static List<String> lines(String... s) { return Arrays.asList(s); }
}
