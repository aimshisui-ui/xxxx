package com.soulenchants.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Two distinct loadouts:
 *   • giveGodSet(p)  — best-in-slot PvP gear (player-killing focus)
 *   • giveBossSet(p) — best-in-slot PvE/boss-killer gear (sustain + anti-boss procs)
 *
 * Slot rules are bypassed (admin loadout writes NBT directly via ItemUtil.addEnchant).
 */
public class GodSet {

    /** Backwards-compat — older callers default to the PvP set. */
    public static void giveTo(Player p) { giveGodSet(p); }

    // ──────────────────────────────────────────────────────────────────
    //  GOD SET — PvP focus. Player-killing weapons + defensive PvP procs.
    // ──────────────────────────────────────────────────────────────────
    public static void giveGodSet(Player p) {
        p.getInventory().setHelmet(godHelmet());
        p.getInventory().setChestplate(godChest());
        p.getInventory().setLeggings(godLegs());
        p.getInventory().setBoots(godBoots());
        p.getInventory().addItem(godSword());
        p.getInventory().addItem(godAxe());
        p.getInventory().addItem(com.soulenchants.loot.BossLootItems.crimsonTongue());
        p.getInventory().addItem(com.soulenchants.loot.BossLootItems.wraithcleaver());
    }

    private static ItemStack godHelmet() {
        ItemStack it = baseArmor(Material.DIAMOND_HELMET, "Veilwalker's Crown");
        it = ItemUtil.addEnchant(it, "drunk",       4);
        it = ItemUtil.addEnchant(it, "nightvision", 1);
        it = ItemUtil.addEnchant(it, "saturation",  3);
        it = ItemUtil.addEnchant(it, "aquatic",     1);
        it = ItemUtil.addEnchant(it, "clarity",     3);
        it = ItemUtil.addEnchant(it, "enlightened", 3);
        it = ItemUtil.addEnchant(it, "stormcaller", 2);
        it = ItemUtil.addEnchant(it, "counter",     3);
        it = ItemUtil.addEnchant(it, "reflect",     2);
        return it;
    }

    private static ItemStack godChest() {
        ItemStack it = baseArmor(Material.DIAMOND_CHESTPLATE, "Duelist's Bulwark");
        it = ItemUtil.addEnchant(it, "berserk",     3);
        it = ItemUtil.addEnchant(it, "overshield",  4);
        it = ItemUtil.addEnchant(it, "implants",    3);
        it = ItemUtil.addEnchant(it, "vital",       5);
        it = ItemUtil.addEnchant(it, "laststand",   3);
        it = ItemUtil.addEnchant(it, "armored",     4);
        it = ItemUtil.addEnchant(it, "spite",       3);
        it = ItemUtil.addEnchant(it, "aegis",       3);
        it = ItemUtil.addEnchant(it, "vampiricplate", 3);
        return it;
    }

    private static ItemStack godLegs() {
        ItemStack it = baseArmor(Material.DIAMOND_LEGGINGS, "Hunter's Greaves");
        it = ItemUtil.addEnchant(it, "hardened",     3);
        it = ItemUtil.addEnchant(it, "antiknockback",2);
        it = ItemUtil.addEnchant(it, "endurance",    2);
        it = ItemUtil.addEnchant(it, "ironclad",     3);
        it = ItemUtil.addEnchant(it, "armored",      4);
        it = ItemUtil.addEnchant(it, "enlightened",  3);
        it = ItemUtil.addEnchant(it, "ironskin",     3);
        it = ItemUtil.addEnchant(it, "callous",      3);
        it = ItemUtil.addEnchant(it, "molten",       2);
        return it;
    }

    private static ItemStack godBoots() {
        ItemStack it = baseArmor(Material.DIAMOND_BOOTS, "Stormborn Treads");
        it = ItemUtil.addEnchant(it, "speed",       3);
        it = ItemUtil.addEnchant(it, "adrenaline",  3);
        it = ItemUtil.addEnchant(it, "depthstrider",3);
        it = ItemUtil.addEnchant(it, "jumpboost",   3);
        it = ItemUtil.addEnchant(it, "firewalker",  1);
        it = ItemUtil.addEnchant(it, "phoenix",     3);
        it = ItemUtil.addEnchant(it, "rush",        3);
        it = ItemUtil.addEnchant(it, "magnetism",   1);
        it = ItemUtil.addEnchant(it, "enlightened", 3);
        return it;
    }

    /** PvP sword — control + crits + lifesteal. No boss-only enchants. */
    private static ItemStack godSword() {
        ItemStack it = baseTool(Material.DIAMOND_SWORD, "Duellist's Edge");
        try {
            it.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
            it.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            it.addUnsafeEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
            it.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        } catch (Throwable ignored) {}
        it = ItemUtil.addEnchant(it, "lifesteal",      5);
        it = ItemUtil.addEnchant(it, "executioner",    3);
        it = ItemUtil.addEnchant(it, "criticalstrike", 5);
        it = ItemUtil.addEnchant(it, "bonebreaker",    5);
        it = ItemUtil.addEnchant(it, "cripple",        3);
        it = ItemUtil.addEnchant(it, "frostaspect",    3);
        it = ItemUtil.addEnchant(it, "phantomstrike",  2);
        it = ItemUtil.addEnchant(it, "blessed",        4);
        it = ItemUtil.addEnchant(it, "soulreaper",     5);
        return it;
    }

    /** PvP axe — heavy CC + bleed pressure + disarm via cleave/skullcrush. */
    private static ItemStack godAxe() {
        ItemStack it = baseTool(Material.DIAMOND_AXE, "Duellist's Cleaver");
        try {
            it.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
            it.addUnsafeEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
            it.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        } catch (Throwable ignored) {}
        it = ItemUtil.addEnchant(it, "bleed",        6);
        it = ItemUtil.addEnchant(it, "deepwounds",   3);
        it = ItemUtil.addEnchant(it, "blessed",      4);
        it = ItemUtil.addEnchant(it, "skullcrush",   3);
        it = ItemUtil.addEnchant(it, "hamstring",    3);
        it = ItemUtil.addEnchant(it, "shieldbreaker",3);
        it = ItemUtil.addEnchant(it, "rendingblow",  3);
        it = ItemUtil.addEnchant(it, "frostshatter", 3);
        return it;
    }

    // ──────────────────────────────────────────────────────────────────
    //  BOSS SET — PvE focus. Sustain + anti-boss damage + group survival.
    // ──────────────────────────────────────────────────────────────────
    public static void giveBossSet(Player p) {
        p.getInventory().setHelmet(bossHelmet());
        p.getInventory().setChestplate(bossChest());
        p.getInventory().setLeggings(bossLegs());
        p.getInventory().setBoots(bossBoots());
        p.getInventory().addItem(bossSword());
        p.getInventory().addItem(bossAxe());
        p.getInventory().addItem(com.soulenchants.loot.BossLootItems.crimsonTongue());
        p.getInventory().addItem(com.soulenchants.loot.BossLootItems.wraithcleaver());
    }

    private static ItemStack bossHelmet() {
        ItemStack it = baseArmor(Material.DIAMOND_HELMET, "Veilseeker's Crown");
        it = ItemUtil.addEnchant(it, "drunk",        4);
        it = ItemUtil.addEnchant(it, "nightvision",  1);
        it = ItemUtil.addEnchant(it, "saturation",   3);
        it = ItemUtil.addEnchant(it, "aquatic",      1);
        it = ItemUtil.addEnchant(it, "clarity",      3);
        it = ItemUtil.addEnchant(it, "enlightened",  3);
        it = ItemUtil.addEnchant(it, "reflect",      2);
        it = ItemUtil.addEnchant(it, "stormcaller",  2);
        it = ItemUtil.addEnchant(it, "magnetism",    1);
        return it;
    }

    private static ItemStack bossChest() {
        ItemStack it = baseArmor(Material.DIAMOND_CHESTPLATE, "Ironheart Bulwark");
        it = ItemUtil.addEnchant(it, "berserk",     3);
        it = ItemUtil.addEnchant(it, "overshield",  4);
        it = ItemUtil.addEnchant(it, "implants",    3);
        it = ItemUtil.addEnchant(it, "vital",       5);
        it = ItemUtil.addEnchant(it, "laststand",   3);
        it = ItemUtil.addEnchant(it, "bloodlust",   6);
        it = ItemUtil.addEnchant(it, "armored",     4);
        it = ItemUtil.addEnchant(it, "enlightened", 3);
        it = ItemUtil.addEnchant(it, "guardians",   3);
        return it;
    }

    private static ItemStack bossLegs() {
        ItemStack it = baseArmor(Material.DIAMOND_LEGGINGS, "Colossus Greaves");
        it = ItemUtil.addEnchant(it, "hardened",     3);
        it = ItemUtil.addEnchant(it, "antiknockback",2);
        it = ItemUtil.addEnchant(it, "endurance",    2);
        it = ItemUtil.addEnchant(it, "ironclad",     3);
        it = ItemUtil.addEnchant(it, "armored",      4);
        it = ItemUtil.addEnchant(it, "enlightened",  3);
        it = ItemUtil.addEnchant(it, "molten",       2);
        it = ItemUtil.addEnchant(it, "lifebloom",    5);
        it = ItemUtil.addEnchant(it, "natureswrath", 4);
        return it;
    }

    private static ItemStack bossBoots() {
        ItemStack it = baseArmor(Material.DIAMOND_BOOTS, "Stormborn Treads");
        it = ItemUtil.addEnchant(it, "speed",        3);
        it = ItemUtil.addEnchant(it, "adrenaline",   3);
        it = ItemUtil.addEnchant(it, "depthstrider", 3);
        it = ItemUtil.addEnchant(it, "jumpboost",    3);
        it = ItemUtil.addEnchant(it, "firewalker",   1);
        it = ItemUtil.addEnchant(it, "phoenix",      3);
        it = ItemUtil.addEnchant(it, "stormcaller",  2);
        it = ItemUtil.addEnchant(it, "enlightened",  3);
        it = ItemUtil.addEnchant(it, "magnetism",    1);
        return it;
    }

    /** PvE sword — anti-boss damage modifiers + sustain + AoE. */
    private static ItemStack bossSword() {
        ItemStack it = baseTool(Material.DIAMOND_SWORD, "Veilslayer");
        try {
            it.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
            it.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            it.addUnsafeEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
            it.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        } catch (Throwable ignored) {}
        it = ItemUtil.addEnchant(it, "lifesteal",        5);
        it = ItemUtil.addEnchant(it, "slayer",           3);
        it = ItemUtil.addEnchant(it, "holysmite",        3);
        it = ItemUtil.addEnchant(it, "witherbane",       3);
        it = ItemUtil.addEnchant(it, "executioner",      3);
        it = ItemUtil.addEnchant(it, "soulreaper",       5);
        it = ItemUtil.addEnchant(it, "criticalstrike",   5);
        it = ItemUtil.addEnchant(it, "blessed",          4);
        it = ItemUtil.addEnchant(it, "divineimmolation", 4);
        return it;
    }

    /** PvE axe — wave-clear minions + low-HP scaling vs bosses + debuff finisher. */
    private static ItemStack bossAxe() {
        ItemStack it = baseTool(Material.DIAMOND_AXE, "Veilreaper");
        try {
            it.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
            it.addUnsafeEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
            it.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        } catch (Throwable ignored) {}
        it = ItemUtil.addEnchant(it, "bleed",            6);
        it = ItemUtil.addEnchant(it, "cleave",           7);
        it = ItemUtil.addEnchant(it, "deepwounds",       3);
        it = ItemUtil.addEnchant(it, "blessed",          4);
        it = ItemUtil.addEnchant(it, "reaver",           4);
        it = ItemUtil.addEnchant(it, "wraithcleave",     3);
        it = ItemUtil.addEnchant(it, "executionersmark", 3);
        it = ItemUtil.addEnchant(it, "bloodfury",        3);
        it = ItemUtil.addEnchant(it, "berserkersedge",   3);
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
