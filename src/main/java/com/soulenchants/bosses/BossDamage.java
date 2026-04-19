package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Boss damage helper. Bypasses most armor reduction by mixing a small
 * "vanilla" hit (for hurt animation + sound) with raw setHealth subtraction.
 * Lethal-save enchants (Phoenix, Soul Shield) are checked before killing.
 */
public class BossDamage {

    public static void apply(Player p, double amount, LivingEntity source) {
        if (p == null || p.isDead()) return;
        double vanillaPart = amount * 0.20;
        double truePart    = amount * 0.80;
        p.damage(vanillaPart, source);
        if (p.isDead() || p.getHealth() <= 0) return;
        double newHp = p.getHealth() - truePart;
        if (newHp <= 0) {
            if (trySaveEnchants(p)) return;
            p.setHealth(0);
        } else {
            p.setHealth(newHp);
        }
    }

    public static void applyTrue(Player p, double amount, LivingEntity source) {
        if (p == null || p.isDead()) return;
        p.damage(0.001, source);
        if (p.isDead()) return;
        double newHp = p.getHealth() - amount;
        if (newHp <= 0) {
            if (trySaveEnchants(p)) return;
            p.setHealth(0);
        } else {
            p.setHealth(newHp);
        }
    }

    private static boolean trySaveEnchants(Player p) {
        SoulEnchants plugin = (SoulEnchants) Bukkit.getPluginManager().getPlugin("SoulEnchants");
        if (plugin == null) return false;
        return LethalSave.trySave(p, plugin);
    }
}
