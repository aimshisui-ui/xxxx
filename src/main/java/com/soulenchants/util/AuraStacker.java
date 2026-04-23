package com.soulenchants.util;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared "+1 tier above whatever's already active" aura helper. Mirrors the
 * same pattern BerserkTickTask uses for Crimson Tongue / Wraithcleaver so
 * every mythic aura (Voidreaver, Dawnbringer, Tidecaller, etc.) stacks
 * correctly with boots/helmet-enchant baselines instead of overwriting them.
 *
 * Expected call cadence: once per aura tick (20 ticks / 1s is fine). The
 * applied duration should be longer than the interval so the effect stays
 * lit — 40 ticks when polled every 20 is the safe default.
 */
public final class AuraStacker {

    /** Per-player, per-effect amp we last applied. Lets us distinguish our
     *  own +1 from an externally-applied buff so we don't runaway-stack. */
    private static final Map<UUID, Map<PotionEffectType, Integer>> OURS = new HashMap<>();

    private AuraStacker() {}

    /**
     * Apply an amplifier that is always +1 above whatever's currently on
     * the player for {@code type}. Safe to call every tick.
     *
     * @param p           the wielder
     * @param type        the effect to stack on top of
     * @param durationTicks how long to apply — pick ≥ 2× your call interval
     */
    public static void bump(Player p, PotionEffectType type, int durationTicks) {
        if (p == null || type == null) return;
        UUID id = p.getUniqueId();
        PotionEffect existing = null;
        for (PotionEffect pe : p.getActivePotionEffects()) {
            if (pe.getType().equals(type)) { existing = pe; break; }
        }
        Map<PotionEffectType, Integer> myMap = OURS.get(id);
        Integer ourLast = (myMap == null) ? null : myMap.get(type);

        int base;
        if (existing == null) {
            base = -1;
        } else if (ourLast != null && existing.getAmplifier() == ourLast) {
            // The current effect is still our last +1 — strip our bump to recover the real base.
            base = ourLast - 1;
        } else {
            // External source (boots/helmet upgrade / external potion) — stack on top.
            base = existing.getAmplifier();
        }
        int newAmp = Math.max(0, base + 1);
        p.addPotionEffect(new PotionEffect(type, durationTicks, newAmp, true, false), true);
        if (myMap == null) {
            myMap = new HashMap<>();
            OURS.put(id, myMap);
        }
        myMap.put(type, newAmp);
    }

    /** Forget all tracking for a player (call from quit/disconnect if you want
     *  to bound the memory footprint). */
    public static void forget(UUID id) {
        OURS.remove(id);
    }
}
