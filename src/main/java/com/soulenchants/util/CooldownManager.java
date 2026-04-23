package com.soulenchants.util;

import com.soulenchants.lunar.LunarBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    // type -> player UUID -> expires-at (System.currentTimeMillis)
    private final Map<String, Map<UUID, Long>> cds = new HashMap<>();

    private static final Map<String, String> PRETTY_NAMES = new LinkedHashMap<>();
    static {
        PRETTY_NAMES.put("stormcaller", "Stormcaller");
        PRETTY_NAMES.put("guardians", "Guardians");
        PRETTY_NAMES.put("soulshield", "Soul Shield");
        PRETTY_NAMES.put("phoenix", "Phoenix");
        PRETTY_NAMES.put("reflect", "Reflect");
        PRETTY_NAMES.put("cripple", "Cripple");
        // v1.1 mythic abilities
        PRETTY_NAMES.put("stormbringer", "Stormbringer");
        PRETTY_NAMES.put("dawnbringer",  "Dawnbringer");
    }

    /**
     * Cooldown types we push to Lunar Client's cooldown overlay. Limited to
     * "big moment" abilities — spammy internal CDs (cripple msg, AoE guards)
     * don't need a client-side ring. If Lunar API isn't present on the
     * player, the push is a silent no-op.
     */
    private static final java.util.Set<String> LUNAR_PUSHED = new java.util.HashSet<>(java.util.Arrays.asList(
            "stormcaller", "guardians", "soulshield", "phoenix", "reflect",
            "stormbringer", "dawnbringer", "natureswrath", "aegis", "rush", "overshield"
    ));

    public void set(String type, UUID player, long durationMs) {
        cds.computeIfAbsent(type.toLowerCase(), k -> new HashMap<>())
                .put(player, System.currentTimeMillis() + durationMs);
        // Lunar Client overlay — fires client-side cooldown ring above hotbar.
        String key = type.toLowerCase();
        if (LUNAR_PUSHED.contains(key)) {
            Player p = Bukkit.getPlayer(player);
            if (p != null && p.isOnline()) {
                LunarBridge.sendCooldown(p, key, durationMs);
            }
        }
    }

    public long remaining(String type, UUID player) {
        Map<UUID, Long> map = cds.get(type.toLowerCase());
        if (map == null) return 0;
        Long until = map.get(player);
        if (until == null) return 0;
        long rem = until - System.currentTimeMillis();
        return Math.max(0, rem);
    }

    public boolean isReady(String type, UUID player) {
        return remaining(type, player) <= 0;
    }

    public Map<String, Long> getActiveCooldowns(UUID player) {
        Map<String, Long> active = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : PRETTY_NAMES.entrySet()) {
            long rem = remaining(e.getKey(), player);
            if (rem > 0) active.put(e.getValue(), rem);
        }
        return active;
    }
}
