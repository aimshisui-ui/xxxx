package com.soulenchants.enchants.impl;

import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantSlot;
import com.soulenchants.enchants.EnchantTier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BerserkEnchant extends CustomEnchant {

    public BerserkEnchant() {
        super("berserk", "Berserk", EnchantTier.EPIC, EnchantSlot.CHESTPLATE, 3,
              "Strength when low HP (<5HP)");
    }

    public void onTick(Player player, int level) {
        if (player.getHealth() > 5.0) return;
        // Strength I -> level III, refresh every couple seconds
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INCREASE_DAMAGE, 60, level - 1, true, false), true);
    }
}
