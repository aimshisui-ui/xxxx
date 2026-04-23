package com.soulenchants.util;

import com.soulenchants.lunar.LunarBridge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
        PRETTY_NAMES.put("natureswrath", "Nature's Wrath");
        PRETTY_NAMES.put("aegis",        "Aegis");
        PRETTY_NAMES.put("rush",         "Rush");
        PRETTY_NAMES.put("overshield",   "Overshield");
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

    /**
     * Per-ability icon for the Lunar cooldown ring. Apollo's cooldown API
     * only renders the icon + countdown timer — no text label is supported,
     * per their docs. So every icon has to be visually unambiguous at a
     * glance. Picks below aim for maximum distinctiveness:
     *
     *   Stormcaller     BEACON          tall glowing beam
     *   Stormbringer    FIREWORK_CHARGE star-burst shape
     *   Guardians       GOLDEN_APPLE    gold glow
     *   Overshield      NETHER_STAR     unique sparkle
     *   Soul Shield     EMERALD         green gem
     *   Reflect         SLIME_BALL      green orb
     *   Phoenix         BLAZE_POWDER    orange dust
     *   Nature's Wrath  SAPLING         leafy green
     *   Dawnbringer     GLOWSTONE_DUST  yellow-white dust
     *   Aegis           DIAMOND_HELMET  blue gem helm
     *   Rush            FEATHER         speed-feel icon
     */
    private static final Map<String, Material> LUNAR_ICONS = new HashMap<>();
    static {
        LUNAR_ICONS.put("stormcaller",  Material.BEACON);
        LUNAR_ICONS.put("guardians",    Material.GOLDEN_APPLE);
        LUNAR_ICONS.put("soulshield",   Material.EMERALD);
        LUNAR_ICONS.put("phoenix",      Material.BLAZE_POWDER);
        LUNAR_ICONS.put("reflect",      Material.SLIME_BALL);
        LUNAR_ICONS.put("stormbringer", Material.FIREWORK_CHARGE);
        LUNAR_ICONS.put("dawnbringer",  Material.GLOWSTONE_DUST);
        LUNAR_ICONS.put("natureswrath", Material.SAPLING);
        LUNAR_ICONS.put("aegis",        Material.DIAMOND_HELMET);
        LUNAR_ICONS.put("rush",         Material.FEATHER);
        LUNAR_ICONS.put("overshield",   Material.NETHER_STAR);
    }

    public void set(String type, UUID player, long durationMs) {
        cds.computeIfAbsent(type.toLowerCase(), k -> new HashMap<>())
                .put(player, System.currentTimeMillis() + durationMs);
        // Lunar Client overlay — fires client-side cooldown ring above hotbar.
        String key = type.toLowerCase();
        if (LUNAR_PUSHED.contains(key)) {
            Player p = Bukkit.getPlayer(player);
            if (p != null && p.isOnline()) {
                Material icon = LUNAR_ICONS.getOrDefault(key, Material.DIAMOND_SWORD);
                LunarBridge.sendCooldown(p, pretty(key), durationMs, icon);
            }
        }
    }

    /** Map internal cooldown id to its pretty display label for Lunar's ring. */
    private static String pretty(String key) {
        String p = PRETTY_NAMES.get(key);
        return p != null ? p : key;
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
