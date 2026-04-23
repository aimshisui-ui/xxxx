package com.soulenchants.masks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Thin query layer over a player's currently-worn mask power. Every combat
 * / tick site that needs to ask "does this player have X immunity or Y
 * multiplier" goes through here so the lookup path is uniform and cheap.
 *
 * Reads the helmet slot once and caches nothing — mask identity is carried
 * by item NBT, so a /give or armor-swap is reflected immediately.
 */
public final class MaskEffects {

    private MaskEffects() {}

    public static Mask.MaskPower powerOf(Player p) {
        if (p == null) return null;
        ItemStack helmet = p.getInventory().getHelmet();
        if (helmet == null) return null;
        String id = MaskRegistry.attachedMaskId(helmet);
        if (id == null) return null;
        return Mask.powerOf(id);
    }

    public static String activeMaskId(Player p) {
        if (p == null) return null;
        ItemStack helmet = p.getInventory().getHelmet();
        return helmet == null ? null : MaskRegistry.attachedMaskId(helmet);
    }

    /** True if the player's mask grants immunity to the given potion effect. */
    public static boolean isEffectImmune(Player p, PotionEffectType type) {
        Mask.MaskPower pw = powerOf(p);
        return pw != null && pw.effectImmune.contains(type);
    }

    /** True if the player's mask grants immunity to the named custom
     *  enchant effect — e.g. "bleed", "wither". */
    public static boolean isEnchantImmune(Player p, String effectId) {
        Mask.MaskPower pw = powerOf(p);
        return pw != null && pw.enchantImmune.contains(effectId);
    }

    /** True if the player's mask makes them immune to fire damage. */
    public static boolean isFireImmune(Player p) {
        Mask.MaskPower pw = powerOf(p);
        return pw != null && pw.fireImmune;
    }

    /** Outgoing damage scalar from the mask — 1.0 if no effect. vs-players
     *  multiplier stacks additively with the generic outgoing multiplier. */
    public static double outgoingMultiplier(Player attacker, boolean targetIsPlayer) {
        Mask.MaskPower pw = powerOf(attacker);
        if (pw == null) return 1.0;
        double mult = 1.0 + pw.outgoingDmgMult;
        if (targetIsPlayer) mult += pw.outgoingDmgMultVsPlayers;
        return mult;
    }

    /** Incoming damage scalar — below 1.0 reduces damage. Caller should
     *  multiply the raw damage by this. */
    public static double incomingMultiplier(Player victim, boolean isExplosion) {
        Mask.MaskPower pw = powerOf(victim);
        if (pw == null) return 1.0;
        double mult = 1.0 - pw.incomingDmgReduce;
        if (victim.getHealth() < victim.getMaxHealth() * 0.5)
            mult -= pw.lowHpIncomingReduce;
        if (isExplosion) mult -= pw.explosionReduce;
        return Math.max(0, mult);
    }

    /** Apply aura boosts + strip immune potion effects. Called from the
     *  tick task. Boost means: if the player ALREADY has potion X, override
     *  with amp = current + 1. */
    public static void tick(Player p) {
        Mask.MaskPower pw = powerOf(p);
        if (pw == null) return;
        // Immunities: strip matching effects each tick.
        for (PotionEffectType t : pw.effectImmune) {
            if (p.hasPotionEffect(t)) p.removePotionEffect(t);
        }
        // Aura boosts: find the active effect's current amp, apply amp+1.
        for (PotionEffectType t : pw.auraBoosts) {
            if (!p.hasPotionEffect(t)) continue;
            int cur = currentAmp(p, t);
            int bumped = Math.min(4, cur + 1);   // hard cap at V
            p.addPotionEffect(new PotionEffect(t, 60, bumped, true, false), true);
        }
        if (pw.fireImmune && p.getFireTicks() > 0) p.setFireTicks(0);
    }

    private static int currentAmp(Player p, PotionEffectType t) {
        for (PotionEffect pe : p.getActivePotionEffects())
            if (pe.getType().equals(t)) return pe.getAmplifier();
        return -1;
    }
}
