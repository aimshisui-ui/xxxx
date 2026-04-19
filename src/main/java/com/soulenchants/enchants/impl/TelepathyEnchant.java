package com.soulenchants.enchants.impl;

import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantSlot;
import com.soulenchants.enchants.EnchantTier;

public class TelepathyEnchant extends CustomEnchant {
    public TelepathyEnchant() {
        super("telepathy", "Telepathy", EnchantTier.COMMON, EnchantSlot.TOOL, 1,
              "Drops go straight to your inventory");
    }
}
