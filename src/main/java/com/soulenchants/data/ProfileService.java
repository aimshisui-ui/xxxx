package com.soulenchants.data;

import com.soulenchants.SoulEnchants;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Central async-load orchestrator for per-player profile data.
 *
 * Guardrail: no new YAML load may run on the main thread during a
 * PlayerJoinEvent. Every per-player reader either
 *   (a) lives in a boot-time-loaded cache (the pattern SoulManager,
 *       PvPStats, GuildManager, LootFilterManager already use), or
 *   (b) registers an {@link AsyncLoader} here — ProfileService then
 *       dispatches the loader on AsyncPlayerPreLoginEvent so the disk
 *       I/O finishes BEFORE the player's PlayerJoinEvent fires.
 *
 * The AsyncPlayerPreLoginEvent runs ~50-150ms before PlayerJoinEvent
 * under normal conditions — plenty of runway for a single YAML read.
 * If the future is still incomplete by the time PlayerJoinEvent fires,
 * {@link #awaitLoad(UUID, long)} lets the sync handler block briefly;
 * a 3-second ceiling prevents pathological stalls.
 *
 * This class ships as infrastructure in v1.5; no existing manager
 * currently uses it (they all eager-boot-load into maps and read from
 * cache — zero disk I/O on join today). Future systems that need a
 * per-player disk read on connect should implement AsyncLoader instead
 * of opening files inline on join.
 */
public final class ProfileService implements Listener {

    /** Plug-point for any subsystem that wants its per-player data
     *  pre-warmed async. Implementations MUST be thread-safe — they run
     *  off the main thread from AsyncPlayerPreLoginEvent. */
    public interface AsyncLoader {
        /** Short label for diagnostic logs. */
        String name();
        /** Load whatever data this subsystem needs for the given UUID.
         *  Runs off the main thread. Exceptions are swallowed with a log
         *  so a single failing subsystem doesn't kick the player. */
        void loadFor(UUID uuid);
    }

    private final SoulEnchants plugin;
    private final List<AsyncLoader> loaders = new CopyOnWriteArrayList<>();
    private final java.util.concurrent.ConcurrentMap<UUID, CompletableFuture<Void>> inflight
            = new ConcurrentHashMap<>();

    /** 3-second budget for a single pre-login's combined async work. */
    private static final long PRELOGIN_BUDGET_MS = 3000L;

    public ProfileService(SoulEnchants plugin) { this.plugin = plugin; }

    public void register(AsyncLoader loader) { loaders.add(loader); }

    /** Install the event listener. Call from onEnable. */
    public void install() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        final UUID id = e.getUniqueId();
        if (loaders.isEmpty()) return;
        CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
            for (AsyncLoader loader : loaders) {
                try { loader.loadFor(id); }
                catch (Throwable t) {
                    plugin.getLogger().warning("[profile-load] " + loader.name()
                            + " threw for " + id + ": " + t);
                }
            }
        });
        inflight.put(id, f);
        // AsyncPlayerPreLoginEvent runs on the Netty worker thread, so a
        // short block here is safe — it doesn't freeze the main thread.
        try { f.get(PRELOGIN_BUDGET_MS, TimeUnit.MILLISECONDS); }
        catch (Throwable ignored) {
            // Timed out or interrupted — leave the future in the map so
            // awaitLoad() can join on it when the join handler runs.
        }
    }

    /** If a sync join-time consumer absolutely needs the async load done,
     *  this blocks the main thread for up to {@code timeoutMs} waiting for
     *  the pre-login future. Use sparingly — the whole point of pre-login
     *  is that the main thread doesn't wait. */
    public void awaitLoad(UUID uuid, long timeoutMs) {
        CompletableFuture<Void> f = inflight.get(uuid);
        if (f == null) return;
        try { f.get(Math.max(0, timeoutMs), TimeUnit.MILLISECONDS); }
        catch (Throwable ignored) {}
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // At this point the pre-login future has either completed or is
        // close to completing. Clean up the tracking entry after a brief
        // grace window so any consumer that wants to join() on it still can.
        UUID id = e.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            inflight.remove(id);
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        inflight.remove(e.getPlayer().getUniqueId());
    }

    /** Diagnostic — number of loaders currently registered. Feeds /ce debug. */
    public int registeredLoaderCount() { return loaders.size(); }
    /** Diagnostic — number of pre-login futures currently tracked. */
    public int inflightCount() { return inflight.size(); }
}
