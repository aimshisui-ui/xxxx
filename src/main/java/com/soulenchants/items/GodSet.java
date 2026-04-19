package com.soulenchants.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a fully-enchanted boss-killer loadout for admin testing.
 * Respects slot rules + 9-enchant cap. Stacks Armored + Enlightened across pieces.
 */
public class GodSet {

    public static void giveTo(Player p) {
        p.getInventory().setHelmet(buildHelmet());
        p.getInventory().setChestplate(buildChestplate());
        p.getInventory().setLeggings(buildLeggings());
        p.getInventory().setBoots(buildBoots());
        p.getInventory().addItem(buildSword());
    }

    // ── HELMET ─ helmet-only (4) + any-armor (5) = 9 ─
    private static ItemStack buildHelmet() {
        ItemStack it = baseArmor(Material.DIAMOND_HELMET, "Veilwalker's Crown");
        it = ItemUtil.addEnchant(it, "drunk",        4);
        it = ItemUtil.addEnchant(it, "nightvision",  1);
        it = ItemUtil.addEnchant(it, "saturation",   3);
        it = ItemUtil.addEnchant(it, "aquatic",      1);
        it = ItemUtil.addEnchant(it, "enlightened",  3);
        it = ItemUtil.addEnchant(it, "stormcaller",  3);
        it = ItemUtil.addEnchant(it, "reflect",      3);
        it = ItemUtil.addEnchant(it, "vengeance",    3);
        it = ItemUtil.addEnchant(it, "magnetism",    1);
        return it;
    }

    // ── CHESTPLATE ─ 7 chest-only + 2 stacking = 9 ─
    private static ItemStack buildChestplate() {
        ItemStack it = baseArmor(Material.DIAMOND_CHESTPLATE, "Ironheart Bulwark");
        it = ItemUtil.addEnchant(it, "berserk",      3);
        it = ItemUtil.addEnchant(it, "phoenix",      1);
        it = ItemUtil.addEnchant(it, "overshield",   3);
        it = ItemUtil.addEnchant(it, "implants",     3);
        it = ItemUtil.addEnchant(it, "vital",        3);
        it = ItemUtil.addEnchant(it, "laststand",    3);
        it = ItemUtil.addEnchant(it, "blessed",      3);
        it = ItemUtil.addEnchant(it, "armored",      4);
        it = ItemUtil.addEnchant(it, "enlightened",  3);
        return it;
    }

    // ── LEGGINGS ─ 4 leggings-only + 2 stacking + 3 any-armor = 9 ─
    private static ItemStack buildLeggings() {
        ItemStack it = baseArmor(Material.DIAMOND_LEGGINGS, "Colossus Greaves");
        it = ItemUtil.addEnchant(it, "hardened",     3);
        it = ItemUtil.addEnchant(it, "antiknockback",3);
        it = ItemUtil.addEnchant(it, "endurance",    3);
        it = ItemUtil.addEnchant(it, "ironclad",     3);
        it = ItemUtil.addEnchant(it, "armored",      4);
        it = ItemUtil.addEnchant(it, "enlightened",  3);
        it = ItemUtil.addEnchant(it, "guardians",    3);
        it = ItemUtil.addEnchant(it, "molten",       3);
        it = ItemUtil.addEnchant(it, "lifebloom",    3);
        return it;
    }

    // ── BOOTS ─ 7 boots-only + 1 stacking = 8 ─
    private static ItemStack buildBoots() {
        ItemStack it = baseArmor(Material.DIAMOND_BOOTS, "Stormborn Treads");
        it = ItemUtil.addEnchant(it, "speed",         3);
        it = ItemUtil.addEnchant(it, "adrenaline",    3);
        it = ItemUtil.addEnchant(it, "depthstrider",  3);
        it = ItemUtil.addEnchant(it, "haste",         3);
        it = ItemUtil.addEnchant(it, "jumpboost",     3);
        it = ItemUtil.addEnchant(it, "firewalker",    1);
        it = ItemUtil.addEnchant(it, "featherweight", 3);
        it = ItemUtil.addEnchant(it, "enlightened",   3);
        return it;
    }

    // ── SWORD ─ boss-killer (9 enchants cap) ─
    private static ItemStack buildSword() {
        ItemStack it = baseTool(Material.DIAMOND_SWORD, "Veilslayer");
        try {
            it.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
            it.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            it.addUnsafeEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
            it.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        } catch (Throwable ignored) {}
        it = ItemUtil.addEnchant(it, "lifesteal",       3);
        it = ItemUtil.addEnchant(it, "slayer",          3);
        it = ItemUtil.addEnchant(it, "holysmite",       3);
        it = ItemUtil.addEnchant(it, "witherbane",      3);
        it = ItemUtil.addEnchant(it, "cleave",          3);
        it = ItemUtil.addEnchant(it, "executioner",     3);
        it = ItemUtil.addEnchant(it, "reaper",          3);
        it = ItemUtil.addEnchant(it, "soulreaper",      3);
        it = ItemUtil.addEnchant(it, "criticalstrike",  3);
        return it;
    }

    private static ItemStack baseArmor(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + name);
        meta.spigot().setUnbreakable(true);
        try {
            meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            meta.addEnchant(Enchantment.DURABILITY, 3, true);
        } catch (Throwable ignored) {}
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack baseTool(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + name);
        meta.spigot().setUnbreakable(true);
        it.setItemMeta(meta);
        return it;
    }
}
