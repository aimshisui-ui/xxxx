package com.soulenchants.items;

import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.style.MessageStyle;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Factories for the core consumables — enchant books, Magic Dust, white/black
 * scrolls. All items share the same visual language:
 *   • bold display name with tier-coloured glyph
 *   • strikethrough divider at top + bottom
 *   • italic flavour line, then concrete effect, then mechanics, then CTA
 */
public class ItemFactories {

    private static final Random RNG = new Random();
    private static final String DIVIDER =
            MessageStyle.FRAME + "" + ChatColor.STRIKETHROUGH + "                                  ";

    /** Enchants whose effect strength scales with multiple copies on the same
     *  gear class (armor pieces). Every other enchant in the game either
     *  Math.max's across pieces (only the highest copy counts) or is weapon-
     *  slot (single hand-held). Source-of-truth for the "Enchant Stacks"
     *  book-lore footer. */
    private static final java.util.Set<String> STACKING_IDS = new java.util.HashSet<>(java.util.Arrays.asList(
            "armored",      // explicit: sums levels across chest + leggings
            "enlightened",  // explicit: sums levels across all pieces
            "radiantshell", // -1 flat dmg per equipped piece, cap -4
            "thornback"     // reflect TRUE dmg per-piece, stacks
    ));

    /** Whether this enchant's effect stacks when applied to multiple pieces
     *  of gear. Returns false for every weapon enchant (single weapon in
     *  hand), and for armor enchants that Math.max across pieces. */
    public static boolean stacks(CustomEnchant e) {
        return e != null && STACKING_IDS.contains(e.getId().toLowerCase());
    }

    public static ItemStack book(CustomEnchant e, int level) {
        return book(e, level, 1 + RNG.nextInt(100), 1 + RNG.nextInt(100));
    }

    public static ItemStack book(CustomEnchant e, int level, int successRate, int destroyRate) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        ChatColor tier = e.getTier().getColor();
        meta.setDisplayName(tier + "" + ChatColor.BOLD + MessageStyle.DIAMOND + " " + e.getDisplayName()
                + " " + CustomEnchant.roman(level));

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add(MessageStyle.MUTED + "" + MessageStyle.ITALIC + e.getDescription());
        String effect = com.soulenchants.enchants.EnchantEffects.describe(e.getId(), level);
        if (effect != null) {
            lore.add("");
            lore.add(MessageStyle.TIER_EPIC + "At " + CustomEnchant.roman(level) + MessageStyle.MUTED + ": "
                    + MessageStyle.VALUE + effect);
        }
        lore.add("");
        lore.add(MessageStyle.labeledValue("Success",   successRate + "%"));
        lore.add(MessageStyle.labeledValue("Destroy",   destroyRate + "%"));
        lore.add(MessageStyle.labeledValue("Goes on",   e.getSlot().getDisplay()));
        lore.add(MessageStyle.MUTED + "Tier: "        + e.getTier().coloredName());
        if (e.getSoulCost() > 0)
            lore.add(MessageStyle.MUTED + "Soul Cost: " + MessageStyle.BAD + e.getSoulCost());
        boolean st = stacks(e);
        lore.add(MessageStyle.MUTED + "Enchant Stacks: "
                + (st ? MessageStyle.GOOD + "True" : MessageStyle.BAD + "False"));
        lore.add("");
        lore.add(MessageStyle.TIER_LEGENDARY + "" + MessageStyle.ITALIC + "▶ Drag onto compatible gear to apply");
        lore.add(DIVIDER);

        meta.setLore(lore);
        item.setItemMeta(meta);

        NBTItem nbt = new NBTItem(item);
        nbt.setString(ItemUtil.NBT_BOOK_ENCHANT, e.getId().toLowerCase());
        nbt.setInteger(ItemUtil.NBT_BOOK_LEVEL, level);
        nbt.setInteger(ItemUtil.NBT_BOOK_SUCCESS, successRate);
        nbt.setInteger(ItemUtil.NBT_BOOK_DESTROY, destroyRate);
        return nbt.getItem();
    }

    public static ItemStack dust(int rate) {
        ItemStack item = new ItemStack(Material.SUGAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageStyle.TIER_EPIC + "" + ChatColor.BOLD + MessageStyle.STAR
                + " Magic Dust " + MessageStyle.FRAME + "(" + MessageStyle.VALUE + rate + "%"
                + MessageStyle.FRAME + ")");
        meta.setLore(Arrays.asList(
                DIVIDER,
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "A shimmering powder that",
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "guarantees a fixed enchant",
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "success rate on the next try.",
                "",
                MessageStyle.GOOD + "▸ " + MessageStyle.MUTED + "Sets next enchant to "
                        + MessageStyle.VALUE + MessageStyle.BOLD + rate + "%",
                "",
                MessageStyle.TIER_LEGENDARY + "" + MessageStyle.ITALIC + "▶ Shift-click in inventory to arm",
                DIVIDER
        ));
        item.setItemMeta(meta);
        NBTItem nbt = new NBTItem(item);
        nbt.setInteger(ItemUtil.NBT_DUST_RATE, rate);
        return nbt.getItem();
    }

    public static ItemStack blackScroll() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageStyle.FRAME + "" + ChatColor.BOLD + MessageStyle.STAR + " Black Scroll");
        meta.setLore(Arrays.asList(
                DIVIDER,
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "A scrap of dark parchment that",
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "tears an enchant from your gear",
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "back into a fresh book.",
                "",
                MessageStyle.BAD + "▸ " + MessageStyle.MUTED + "Extracts one random enchant",
                "",
                MessageStyle.TIER_LEGENDARY + "" + MessageStyle.ITALIC + "▶ Drag onto enchanted gear",
                DIVIDER
        ));
        item.setItemMeta(meta);
        NBTItem nbt = new NBTItem(item);
        nbt.setBoolean(ItemUtil.NBT_BLACK_SCROLL, true);
        return nbt.getItem();
    }

    public static ItemStack whiteScroll() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageStyle.VALUE + "" + ChatColor.BOLD + MessageStyle.STAR + " White Scroll");
        meta.setLore(Arrays.asList(
                DIVIDER,
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "A blessed parchment that binds",
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "to your gear and absorbs",
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "one failed enchant destruction.",
                "",
                MessageStyle.GOOD + "▸ " + MessageStyle.MUTED + "Prevents gear-loss on next fail",
                "",
                MessageStyle.TIER_LEGENDARY + "" + MessageStyle.ITALIC + "▶ Drag onto gear to bind",
                DIVIDER
        ));
        item.setItemMeta(meta);
        NBTItem nbt = new NBTItem(item);
        nbt.setBoolean(ItemUtil.NBT_WHITE_SCROLL, true);
        return nbt.getItem();
    }

    public static List<Integer> dustRates() { return Arrays.asList(25, 50, 75, 100); }
}
