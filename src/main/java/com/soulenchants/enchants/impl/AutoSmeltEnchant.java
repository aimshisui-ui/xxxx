package com.soulenchants.enchants.impl;

import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantSlot;
import com.soulenchants.enchants.EnchantTier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AutoSmeltEnchant extends CustomEnchant {

    private static final Map<Material, ItemStack> SMELT_MAP = new HashMap<>();

    static {
        SMELT_MAP.put(Material.IRON_ORE,    new ItemStack(Material.IRON_INGOT));
        SMELT_MAP.put(Material.GOLD_ORE,    new ItemStack(Material.GOLD_INGOT));
        SMELT_MAP.put(Material.SAND,        new ItemStack(Material.GLASS));
        SMELT_MAP.put(Material.COBBLESTONE, new ItemStack(Material.STONE));
        SMELT_MAP.put(Material.NETHERRACK,  new ItemStack(Material.NETHERRACK));
    }

    public AutoSmeltEnchant() {
        super("autosmelt", "AutoSmelt", EnchantTier.UNCOMMON, EnchantSlot.PICKAXE, 1,
              "Smelts ores instantly when mined");
    }

    public static ItemStack smelt(Material m) {
        ItemStack s = SMELT_MAP.get(m);
        return s == null ? null : s.clone();
    }

    public static boolean canSmelt(Material m) {
        return SMELT_MAP.containsKey(m);
    }
}
