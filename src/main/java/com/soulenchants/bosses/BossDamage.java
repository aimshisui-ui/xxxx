package com.soulenchants.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Boss damage helper. Bypasses most armor reduction by mixing a small
 * "vanilla" hit (for hurt animation + sound) with raw setHealth subtraction.
 */
public class BossDamage {

    /** ~80% bypasses armor; the small vanilla portion still respects Blessed/Hardened/etc. */
    public static void apply(Player p, double amount, LivingEntity source) {
        if (p == null || p.isDead()) return;
        double vanillaPart = amount * 0.20;
        double truePart    = amount * 0.80;
        p.damage(vanillaPart, source); // triggers hurt anim + listener procs (Blessed etc.)
        if (p.isDead() || p.getHealth() <= 0) return;
        double newHp = p.getHealth() - truePart;
        if (newHp <= 0) p.setHealth(0);
        else p.setHealth(newHp);
    }

    /** Pure true damage — used for ultimate hits like Final Thread Bind. */
    public static void applyTrue(Player p, double amount, LivingEntity source) {
        if (p == null || p.isDead()) return;
        p.damage(0.001, source); // tiny tick to trigger hurt anim
        if (p.isDead()) return;
        double newHp = p.getHealth() - amount;
        if (newHp <= 0) p.setHealth(0);
        else p.setHealth(newHp);
    }
}
