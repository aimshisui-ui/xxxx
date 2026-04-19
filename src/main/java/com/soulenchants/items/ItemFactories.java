package com.soulenchants.items;

import com.soulenchants.enchants.CustomEnchant;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ItemFactories {

    private static final Random RNG = new Random();

    private static final String DIVIDER = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH
            + "                                  ";

    public static ItemStack book(CustomEnchant e, int level) {
        return book(e, level, 1 + RNG.nextInt(100), 1 + RNG.nextInt(100));
    }

    public static ItemStack book(CustomEnchant e, int level, int successRate, int destroyRate) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        ChatColor tier = e.getTier().getColor();
        meta.setDisplayName(tier + "" + ChatColor.BOLD + e.getDisplayName()
                + " " + CustomEnchant.roman(level));

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + e.getDescription());
        lore.add("");
        lore.add(ChatColor.GRAY + "Success Rate: " + ChatColor.WHITE + successRate + "%");
        lore.add(ChatColor.GRAY + "Destroy Rate: " + ChatColor.WHITE + destroyRate + "%");
        lore.add(ChatColor.GRAY + "Goes On: "      + ChatColor.WHITE + e.getSlot().getDisplay());
        lore.add(ChatColor.GRAY + "Tier: "          + e.getTier().coloredName());
        if (e.getSoulCost() > 0)
            lore.add(ChatColor.GRAY + "Soul Cost: " + ChatColor.RED + e.getSoulCost());
        lore.add("");
        lore.add(ChatColor.YELLOW + "" + ChatColor.ITALIC + "Drag onto compatible gear to apply");
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
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ Magic Dust "
                + ChatColor.GRAY + "(" + rate + "%)");

        meta.setLore(Arrays.asList(
                DIVIDER,
                ChatColor.GRAY + "" + ChatColor.ITALIC + "A shimmering powder that",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "guarantees a fixed enchant success rate.",
                "",
                ChatColor.GREEN + "▸ " + ChatColor.GRAY + "Sets next enchant to "
                        + ChatColor.WHITE + ChatColor.BOLD + rate + "%",
                "",
                ChatColor.YELLOW + "" + ChatColor.ITALIC + "Shift-click in inventory to arm",
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
        meta.setDisplayName(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "✦ Black Scroll");
        meta.setLore(Arrays.asList(
                DIVIDER,
                ChatColor.GRAY + "" + ChatColor.ITALIC + "A scrap of dark parchment that",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "tears an enchant from your gear",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "back into a fresh book.",
                "",
                ChatColor.YELLOW + "" + ChatColor.ITALIC + "Drag onto enchanted gear",
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
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "✦ White Scroll");
        meta.setLore(Arrays.asList(
                DIVIDER,
                ChatColor.GRAY + "" + ChatColor.ITALIC + "A blessed parchment that binds",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "to your gear and absorbs",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "one failed enchant destruction.",
                "",
                ChatColor.YELLOW + "" + ChatColor.ITALIC + "Drag onto gear to bind",
                DIVIDER
        ));
        item.setItemMeta(meta);
        NBTItem nbt = new NBTItem(item);
        nbt.setBoolean(ItemUtil.NBT_WHITE_SCROLL, true);
        return nbt.getItem();
    }

    public static List<Integer> dustRates() { return Arrays.asList(25, 50, 75, 100); }
}
