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
        // Tag every armor piece with the Duelist set id so it triggers the
        // Duelist set bonus (DuelistSet) when all 4 are worn.
        p.getInventory().setHelmet(    com.soulenchants.sets.SetRegistry.tag(godHelmet(), "duelist"));
        p.getInventory().setChestplate(com.soulenchants.sets.SetRegistry.tag(godChest(),  "duelist"));
        p.getInventory().setLeggings(  com.soulenchants.sets.SetRegistry.tag(godLegs(),   "duelist"));
        p.getInventory().setBoots(     com.soulenchants.sets.SetRegistry.tag(godBoots(),  "duelist"));
        p.getInventory().addItem(godSword());
        p.getInventory().addItem(godAxe());
        // Same "perfect" mythic stacks the PvE set uses — a fully enchanted
        // mythic is useful in PvP too (lifesteal, criticalstrike, blessed all
        // fire against players; PvE-only bonuses are inert in PvP, costless).
        p.getInventory().addItem(perfectCrimsonTongue());
        p.getInventory().addItem(perfectWraithcleaver());
    }

    private static ItemStack godHelmet() {
        // PvP helmet: every slot earns its place. No utility filler.
        ItemStack it = baseArmor(Material.DIAMOND_HELMET, "Veilwalker's Crown");
        it = ItemUtil.addEnchant(it, "drunk",       4);
        it = ItemUtil.addEnchant(it, "clarity",     3);
        it = ItemUtil.addEnchant(it, "enlightened", 3);
        it = ItemUtil.addEnchant(it, "stormcaller", 2);
        it = ItemUtil.addEnchant(it, "counter",     3);
        it = ItemUtil.addEnchant(it, "reflect",     2);
        it = ItemUtil.addEnchant(it, "oathbound",   3);
        it = ItemUtil.addEnchant(it, "wardenseye",  3);
        it = ItemUtil.addEnchant(it, "thornback",   3);
        return it;
    }

    private static ItemStack godChest() {
        // Exactly 9 enchants — PvP tank + reflect package.
        ItemStack it = baseArmor(Material.DIAMOND_CHESTPLATE, "Duelist's Bulwark");
        it = ItemUtil.addEnchant(it, "berserk",       3);
        it = ItemUtil.addEnchant(it, "overshield",    4);
        it = ItemUtil.addEnchant(it, "implants",      3);
        it = ItemUtil.addEnchant(it, "vital",         6);
        it = ItemUtil.addEnchant(it, "laststand",     3);
        it = ItemUtil.addEnchant(it, "armored",       4);
        it = ItemUtil.addEnchant(it, "spite",         3);
        it = ItemUtil.addEnchant(it, "aegis",         3);
        it = ItemUtil.addEnchant(it, "vampiricplate", 3);
        return it;
    }

    private static ItemStack godLegs() {
        // Exactly 9 enchants — PvP dodge + reflect pants.
        ItemStack it = baseArmor(Material.DIAMOND_LEGGINGS, "Hunter's Greaves");
        it = ItemUtil.addEnchant(it, "hardened",     3);
        it = ItemUtil.addEnchant(it, "armored",      4);
        it = ItemUtil.addEnchant(it, "enlightened",  3);
        it = ItemUtil.addEnchant(it, "ironskin",     3);
        it = ItemUtil.addEnchant(it, "callous",      3);
        it = ItemUtil.addEnchant(it, "molten",       2);
        it = ItemUtil.addEnchant(it, "entombed",     3);
        it = ItemUtil.addEnchant(it, "thornback",    3);
        it = ItemUtil.addEnchant(it, "radiantshell", 4);
        return it;
    }

    private static ItemStack godBoots() {
        // Exactly 9 enchants — mobility + dodge + reflect.
        ItemStack it = baseArmor(Material.DIAMOND_BOOTS, "Stormborn Treads");
        it = ItemUtil.addEnchant(it, "speed",         3);
        it = ItemUtil.addEnchant(it, "adrenaline",    3);
        it = ItemUtil.addEnchant(it, "phoenix",       3);
        it = ItemUtil.addEnchant(it, "rush",          3);
        it = ItemUtil.addEnchant(it, "enlightened",   3);
        it = ItemUtil.addEnchant(it, "voidwalker",    3);
        it = ItemUtil.addEnchant(it, "thornback",     3);
        it = ItemUtil.addEnchant(it, "radiantshell",  4);
        it = ItemUtil.addEnchant(it, "mobslayersward",3);
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

    /** PvP axe — heavy CC + bleed pressure + anti-heal finisher. Exactly 9 enchants. */
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
        it = ItemUtil.addEnchant(it, "reapingslash", 3);
        return it;
    }

    // ──────────────────────────────────────────────────────────────────
    //  BOSS SET — PvE focus. Sustain + anti-boss damage + group survival.
    //  v1.2 — every mythic in the loadout is fully enchanted on top of its
    //  base MythicFactory kit (Sharpness V/VI, Unbreaking III, Fire Aspect
    //  II, Looting III, Unbreakable). Custom enchants are layered per
    //  weapon archetype so swords read sword-flavour and axes read
    //  axe-flavour without overlap.
    // ──────────────────────────────────────────────────────────────────
    public static void giveBossSet(Player p) {
        // Tag every armor piece with the Boss-Killer set id so the BossKillerSet
        // bonuses fire while all 4 are worn.
        p.getInventory().setHelmet(    com.soulenchants.sets.SetRegistry.tag(bossHelmet(), "bosskiller"));
        p.getInventory().setChestplate(com.soulenchants.sets.SetRegistry.tag(bossChest(),  "bosskiller"));
        p.getInventory().setLeggings(  com.soulenchants.sets.SetRegistry.tag(bossLegs(),   "bosskiller"));
        p.getInventory().setBoots(     com.soulenchants.sets.SetRegistry.tag(bossBoots(),  "bosskiller"));
        p.getInventory().addItem(bossSword());
        p.getInventory().addItem(bossAxe());
        p.getInventory().addItem(perfectCrimsonTongue());
        p.getInventory().addItem(perfectWraithcleaver());
        p.getInventory().addItem(perfectGraverend());
        p.getInventory().addItem(perfectEmberlash());
        p.getInventory().addItem(perfectRuinhammer());
    }

    // ──────────────────────────────────────────────────────────────────
    //  "Perfectly enchanted" mythic builders. Each starts from a fresh
    //  MythicFactory/BossLootItems stack (already carries vanilla
    //  Sharpness+Unbreaking kit, custom NBT id + mythic_held aura tag)
    //  and layers thematic custom enchants on top.
    // ──────────────────────────────────────────────────────────────────

    private static ItemStack perfectCrimsonTongue() {
        // Base ships with: Sharpness VI, Fire Aspect II, Looting III,
        // Unbreaking III, bleed 6, deepwounds 3, mythic_held 1, unbreakable.
        // +6 custom enchants = 9 slots consumed.
        ItemStack it = com.soulenchants.loot.BossLootItems.crimsonTongue();
        it = ItemUtil.addEnchant(it, "lifesteal",        5);
        it = ItemUtil.addEnchant(it, "executioner",      3);
        it = ItemUtil.addEnchant(it, "huntersmark",      3);
        it = ItemUtil.addEnchant(it, "executionersmark", 3);
        it = ItemUtil.addEnchant(it, "blessed",          4);
        it = ItemUtil.addEnchant(it, "criticalstrike",   5);
        return it;
    }

    private static ItemStack perfectWraithcleaver() {
        // Base ships with: Sharpness VI, Knockback II, Looting III,
        // Unbreaking III, cleave 7, earthshaker 3, soulburn 3,
        // mythic_held 2, unbreakable.
        ItemStack it = com.soulenchants.loot.BossLootItems.wraithcleaver();
        it = ItemUtil.addEnchant(it, "holysmite",   3);
        it = ItemUtil.addEnchant(it, "witherbane",  3);
        it = ItemUtil.addEnchant(it, "overwhelm",   3);
        it = ItemUtil.addEnchant(it, "blessed",     4);
        it = ItemUtil.addEnchant(it, "criticalstrike", 5);
        return it;
    }

    /** Graverend — PvE axe, heal-on-kill + anti-boss bonus. Debuff-flavour
     *  axe stack on top so every swing pressures the target. 9 enchants. */
    private static ItemStack perfectGraverend() {
        ItemStack it = com.soulenchants.mythic.MythicFactory.create("graverend");
        it = ItemUtil.addEnchant(it, "bleed",            6);
        it = ItemUtil.addEnchant(it, "deepwounds",       3);
        it = ItemUtil.addEnchant(it, "reaver",           4);
        it = ItemUtil.addEnchant(it, "bloodfury",        3);
        it = ItemUtil.addEnchant(it, "berserkersedge",   3);
        it = ItemUtil.addEnchant(it, "exsanguinate",     3);
        it = ItemUtil.addEnchant(it, "executionersmark", 3);
        it = ItemUtil.addEnchant(it, "slayer",           3);
        it = ItemUtil.addEnchant(it, "reapingslash",     3);
        return it;
    }

    /** Emberlash — PvE sword, per-swing fire splash. Fire + sustain
     *  stack that turns trash packs into ash while you heal off each hit. 9 enchants. */
    private static ItemStack perfectEmberlash() {
        ItemStack it = com.soulenchants.mythic.MythicFactory.create("emberlash");
        it = ItemUtil.addEnchant(it, "lifesteal",        5);
        it = ItemUtil.addEnchant(it, "slayer",           3);
        it = ItemUtil.addEnchant(it, "holysmite",        3);
        it = ItemUtil.addEnchant(it, "criticalstrike",   5);
        it = ItemUtil.addEnchant(it, "soulburn",         5);
        it = ItemUtil.addEnchant(it, "divineimmolation", 4);
        it = ItemUtil.addEnchant(it, "witherbane",       3);
        it = ItemUtil.addEnchant(it, "blessed",          4);
        it = ItemUtil.addEnchant(it, "executioner",      3);
        return it;
    }

    /** Ruinhammer — PvE axe, stacking kill bonus. Loaded with AXE debuff
     *  enchants so each hit primes the stack for the 10-stack burst. 9 enchants. */
    private static ItemStack perfectRuinhammer() {
        ItemStack it = com.soulenchants.mythic.MythicFactory.create("ruinhammer");
        it = ItemUtil.addEnchant(it, "cleave",           7);
        it = ItemUtil.addEnchant(it, "skullcrush",       3);
        it = ItemUtil.addEnchant(it, "hamstring",        3);
        it = ItemUtil.addEnchant(it, "shieldbreaker",    3);
        it = ItemUtil.addEnchant(it, "rendingblow",      3);
        it = ItemUtil.addEnchant(it, "pulverize",        3);
        it = ItemUtil.addEnchant(it, "crushingblow",     3);
        it = ItemUtil.addEnchant(it, "reaver",           4);
        it = ItemUtil.addEnchant(it, "executionersmark", 3);
        return it;
    }

    private static ItemStack bossHelmet() {
        // Every slot earns its place — combat-relevant only. Dropped saturation /
        // aquatic / nightvision / magnetism (utility enchants with no bearing on
        // a boss fight) in favour of clarity + oathbound + wardenseye + dreadmantle.
        ItemStack it = baseArmor(Material.DIAMOND_HELMET, "Veilseeker's Crown");
        it = ItemUtil.addEnchant(it, "drunk",        4);
        it = ItemUtil.addEnchant(it, "clarity",      3);
        it = ItemUtil.addEnchant(it, "enlightened",  3);
        it = ItemUtil.addEnchant(it, "reflect",      2);
        it = ItemUtil.addEnchant(it, "stormcaller",  2);
        it = ItemUtil.addEnchant(it, "oathbound",    3);
        it = ItemUtil.addEnchant(it, "wardenseye",   3);
        it = ItemUtil.addEnchant(it, "dreadmantle",  3);
        it = ItemUtil.addEnchant(it, "thornback",    3);
        return it;
    }

    private static ItemStack bossChest() {
        // Exactly 9 enchants — best-in-slot PvE chest.
        ItemStack it = baseArmor(Material.DIAMOND_CHESTPLATE, "Ironheart Bulwark");
        it = ItemUtil.addEnchant(it, "berserk",     3);
        it = ItemUtil.addEnchant(it, "overshield",  4);
        it = ItemUtil.addEnchant(it, "implants",    3);
        it = ItemUtil.addEnchant(it, "vital",       6);
        it = ItemUtil.addEnchant(it, "laststand",   3);
        it = ItemUtil.addEnchant(it, "bloodlust",   6);
        it = ItemUtil.addEnchant(it, "armored",     4);
        it = ItemUtil.addEnchant(it, "bulwark",     3);
        it = ItemUtil.addEnchant(it, "soulwarden",  3);
        return it;
    }

    private static ItemStack bossLegs() {
        // Exactly 9 enchants — stacking survivability + AOE flip on death.
        ItemStack it = baseArmor(Material.DIAMOND_LEGGINGS, "Colossus Greaves");
        it = ItemUtil.addEnchant(it, "hardened",       3);
        it = ItemUtil.addEnchant(it, "armored",        4);
        it = ItemUtil.addEnchant(it, "enlightened",    3);
        it = ItemUtil.addEnchant(it, "lifebloom",      5);
        it = ItemUtil.addEnchant(it, "natureswrath",   4);
        it = ItemUtil.addEnchant(it, "entombed",       3);
        it = ItemUtil.addEnchant(it, "thornback",      3);
        it = ItemUtil.addEnchant(it, "mobslayersward", 3);
        it = ItemUtil.addEnchant(it, "radiantshell",   4);
        return it;
    }

    private static ItemStack bossBoots() {
        // Dropped depthstrider/jumpboost/firewalker — swim speed + jump + lava
        // safety don't move the needle in a boss fight. Voidwalker replaces
        // them (dodge-chance + permanent Speed I).
        ItemStack it = baseArmor(Material.DIAMOND_BOOTS, "Stormborn Treads");
        it = ItemUtil.addEnchant(it, "speed",         3);
        it = ItemUtil.addEnchant(it, "adrenaline",    3);
        it = ItemUtil.addEnchant(it, "phoenix",       3);
        it = ItemUtil.addEnchant(it, "stormcaller",   2);
        it = ItemUtil.addEnchant(it, "enlightened",   3);
        it = ItemUtil.addEnchant(it, "voidwalker",    3);
        it = ItemUtil.addEnchant(it, "thornback",     3);
        it = ItemUtil.addEnchant(it, "radiantshell",  4);
        it = ItemUtil.addEnchant(it, "mobslayersward",3);
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
