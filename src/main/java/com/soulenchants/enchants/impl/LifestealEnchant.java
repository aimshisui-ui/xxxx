package com.soulenchants.enchants.impl;

import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantSlot;
import com.soulenchants.enchants.EnchantTier;
import org.bukkit.entity.Player;

import java.util.Random;

public class LifestealEnchant extends CustomEnchant {

    private static final Random RNG = new Random();

    public LifestealEnchant() {
        super("lifesteal", "Lifesteal", EnchantTier.RARE, EnchantSlot.SWORD, 5,
              "Chance to heal on hit");
    }

    public void onHit(Player attacker, int level) {
        // Heavy nerf: 2% chance per level (max 10% at L5), heals 1 HP flat per proc.
        double chance = Math.min(0.10, 0.02 * level);
        if (RNG.nextDouble() > chance) return;
        double newHp = Math.min(attacker.getHealth() + 1.0, attacker.getMaxHealth());
        attacker.setHealth(newHp);
    }
}
