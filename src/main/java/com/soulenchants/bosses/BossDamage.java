package com.soulenchants.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Boss damage helper. Routes everything through the normal damage pipeline so
 * armor, absorption hearts, defensive enchants, and lethal-save procs all
 * apply naturally. The CombatListener's Phoenix / Soul Shield hook catches
 * any lethal hit for free now.
 */
public class BossDamage {

    public static void apply(Player p, double amount, LivingEntity source) {
        if (p == null || p.isDead()) return;
        p.damage(amount, source);
    }

    /** Kept for callers that want to label a hit as bypassing armor — but it
     *  currently behaves identically to apply(). True damage is no longer used. */
    public static void applyTrue(Player p, double amount, LivingEntity source) {
        apply(p, amount, source);
    }
}
