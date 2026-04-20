package com.soulenchants.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Boss damage helper. Routes everything through the normal damage pipeline
 * (armor, absorption, defensive enchants all apply).
 *
 * Keyed variants look up a per-boss, per-attack override from
 * {@link com.soulenchants.config.LootConfig}. Call {@link #init} on plugin
 * start to wire the config.
 */
public class BossDamage {

    private static com.soulenchants.config.LootConfig LOOT;

    public static void init(com.soulenchants.config.LootConfig loot) { LOOT = loot; }

    public static void apply(Player p, double amount, LivingEntity source) {
        if (p == null || p.isDead()) return;
        p.damage(amount, source);
    }

    /** Keyed: reads override from boss-overrides.yml under {bossId}.damage.{key}, else defaultDmg. */
    public static void apply(Player p, String bossId, String key, double defaultDmg, LivingEntity source) {
        double amt = (LOOT != null) ? LOOT.bossDamage(bossId, key, defaultDmg) : defaultDmg;
        apply(p, amt, source);
    }

    /** Keyed variant with runtime multiplier on top of the looked-up value. */
    public static void applyScaled(Player p, String bossId, String key, double defaultDmg, double scale, LivingEntity source) {
        double amt = (LOOT != null) ? LOOT.bossDamage(bossId, key, defaultDmg) : defaultDmg;
        apply(p, amt * scale, source);
    }

    public static void applyTrue(Player p, double amount, LivingEntity source) { apply(p, amount, source); }

    public static void applyTrue(Player p, String bossId, String key, double defaultDmg, LivingEntity source) {
        apply(p, bossId, key, defaultDmg, source);
    }

    /** HP-override accessor — bosses call this at spawn to respect live config. */
    public static double bossHpOverride(String bossId, double def) {
        return (LOOT != null) ? LOOT.bossHp(bossId, def) : def;
    }
}
