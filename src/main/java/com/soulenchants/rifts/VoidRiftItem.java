package com.soulenchants.rifts;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** Void Rift Spawner — consumable that opens a rift portal. */
public final class VoidRiftItem {

    public static final String NBT_TAG = "se_void_rift_spawner";

    private VoidRiftItem() {}

    public static ItemStack create() {
        ItemStack it = new ItemStack(Material.EYE_OF_ENDER, 1);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ Void Rift Spawner ✦");
        m.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + "Artifact of broken spaces",
                "",
                ChatColor.GRAY + "A splinter of the veil between",
                ChatColor.GRAY + "worlds. Press it to the earth",
                ChatColor.GRAY + "and the fabric will tear open.",
                "",
                ChatColor.LIGHT_PURPLE + "» A portal appears for " + ChatColor.WHITE + "15s",
                ChatColor.LIGHT_PURPLE + "» Your presence is announced",
                ChatColor.LIGHT_PURPLE + "  to every soul in the realm",
                "",
                ChatColor.RED + "⚠ " + ChatColor.GRAY + "Late-game content",
                ChatColor.RED + "⚠ " + ChatColor.GRAY + "PvP is " + ChatColor.WHITE + "enabled " + ChatColor.GRAY + "inside",
                ChatColor.RED + "⚠ " + ChatColor.GRAY + "You have " + ChatColor.WHITE + "10 minutes " + ChatColor.GRAY + "to clear it",
                ChatColor.RED + "⚠ " + ChatColor.GRAY + "Failure = expelled to spawn",
                "",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Right-click on the ground to open"
        ));
        it.setItemMeta(m);
        try { NBTItem nbt = new NBTItem(it); nbt.setBoolean(NBT_TAG, true); it = nbt.getItem(); }
        catch (Throwable ignored) {}
        return it;
    }

    public static boolean isSpawner(ItemStack it) {
        if (it == null || it.getType() == Material.AIR || it.getAmount() <= 0) return false;
        try { return new NBTItem(it).getBoolean(NBT_TAG); }
        catch (Throwable t) { return false; }
    }
}
