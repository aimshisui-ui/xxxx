package com.soulenchants.enchants.impl;

import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantSlot;
import com.soulenchants.enchants.EnchantTier;
import org.bukkit.entity.Player;

import java.util.Random;

public class LifestealEnchant extends CustomEnchant {

    private static final Random RNG = new Random();

    public LifestealEnchant() {
        super("lifesteal", "Lifesteal", EnchantTier.RARE, EnchantSlot.SWORD, 3,
              "Chance to heal on hit");
    }

    public void onHit(Player attacker, int level) {
        double chance = 0.05 * level; // 5/10/15%
        if (RNG.nextDouble() > chance) return;
        double heal = 1.0 * level; // 1/2/3 HP (half a heart per level pair, hearts =2 HP)
        double newHp = Math.min(attacker.getHealth() + heal, attacker.getMaxHealth());
        attacker.setHealth(newHp);
    }
}
