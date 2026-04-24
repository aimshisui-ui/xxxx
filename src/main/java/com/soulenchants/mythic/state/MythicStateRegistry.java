package com.soulenchants.mythic.state;

import com.soulenchants.util.MapManager;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

/**
 * Global registry of per-wielder mythic state. Indexed by {@code (wielderUUID,
 * mythicId)} so two players can wield the same mythic without sharing a
 * cooldown. Entries evict automatically on PlayerQuitEvent via
 * MapManager.registerMap.
 *
 * Typical use inside a mythic's onAuraTick / onHit:
 *
 *   DawnbringerState s = MythicStateRegistry.getOrCreate(
 *           owner, "dawnbringer", DawnbringerState::new);
 *   if (now - s.lastPurge < intervalMs) return;
 *   s.lastPurge = now;
 *
 * The registry is installed lazily the first time any mythic asks for
 * state. No onEnable wiring required.
 */
public final class MythicStateRegistry {

    /** Outer: wielder UUID → inner map.  Inner: mythic id → state handle. */
    private static final ConcurrentMap<UUID, ConcurrentMap<String, MythicStateHandle>> STATES =
            new ConcurrentHashMap<>();

    private static volatile boolean registered = false;

    private MythicStateRegistry() {}

    /** Install the quit-cleanup hook. Idempotent — safe to call multiple
     *  times (first-call wins). Triggers automatically on first getOrCreate. */
    private static synchronized void registerOnce() {
        if (registered) return;
        @SuppressWarnings({"unchecked", "rawtypes"})
        java.util.Map<UUID, ?> asRaw = (java.util.Map) STATES;
        MapManager.registerMap((java.util.Map<UUID, Object>) (java.util.Map) asRaw,
                "mythicStateRegistry");
        registered = true;
    }

    /** Fetch or create state for a (wielder, mythicId). The factory takes
     *  (UUID, mythicId) and MUST return a MythicStateHandle — typically
     *  via a constructor reference: {@code DawnbringerState::new}. */
    public static <S extends MythicStateHandle> S getOrCreate(
            Player wielder, String mythicId, BiFunction<UUID, String, S> factory) {
        registerOnce();
        UUID id = wielder.getUniqueId();
        ConcurrentMap<String, MythicStateHandle> inner =
                STATES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        @SuppressWarnings("unchecked")
        S existing = (S) inner.get(mythicId);
        if (existing != null) return existing;
        S created = factory.apply(id, mythicId);
        @SuppressWarnings("unchecked")
        S prior = (S) inner.putIfAbsent(mythicId, created);
        return prior != null ? prior : created;
    }

    /** Explicit eviction — call if a mythic wants to reset state (e.g. on
     *  hotbar swap out). Normal player-quit eviction is handled by
     *  MapManager automatically. */
    public static void forget(UUID wielder, String mythicId) {
        ConcurrentMap<String, MythicStateHandle> inner = STATES.get(wielder);
        if (inner != null) inner.remove(mythicId);
    }

    /** Diagnostic — total number of state handles currently held. */
    public static int totalHandles() {
        int n = 0;
        for (ConcurrentMap<String, MythicStateHandle> inner : STATES.values()) n += inner.size();
        return n;
    }
}
