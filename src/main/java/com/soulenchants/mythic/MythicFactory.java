package com.soulenchants.mythic;

import com.soulenchants.style.MessageStyle;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Build the actual ItemStack for a mythic. Glowing, tagged, pretty lore. */
public final class MythicFactory {

    private MythicFactory() {}

    public static ItemStack create(String mythicId) {
        MythicWeapon m = MythicRegistry.get(mythicId);
        if (m == null) return null;
        Material mat = materialFor(mythicId);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageStyle.TIER_SOUL + MessageStyle.BOLD + m.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.addAll(m.prefixLore());
            lore.addAll(m.getLoreLines());
            meta.setLore(lore);
            try { meta.addEnchant(Enchantment.DURABILITY, 1, true); } catch (Throwable ignored) {}
            try { meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); } catch (Throwable ignored) {}
            item.setItemMeta(meta);
        }
        NBTItem nbt = new NBTItem(item);
        nbt.setString(MythicRegistry.NBT_KEY, mythicId);
        return nbt.getItem();
    }

    /** Map mythic id → material. Defaults to diamond sword for unknowns. */
    private static Material materialFor(String id) {
        if (id == null) return Material.DIAMOND_SWORD;
        switch (id) {
            case "crimson_tongue":  return Material.IRON_SWORD;
            case "wraithcleaver":   return Material.DIAMOND_AXE;
            case "stormbringer":    return Material.DIAMOND_SWORD;
            case "voidreaver":      return Material.DIAMOND_SWORD;
            case "dawnbringer":     return Material.GOLD_SWORD;
            case "sunderer":        return Material.DIAMOND_AXE;
            case "phoenix_feather": return Material.GOLD_SWORD;
            case "soulbinder":      return Material.BOW;
            case "tidecaller":      return Material.FISHING_ROD;
            default:                return Material.DIAMOND_SWORD;
        }
    }
}
