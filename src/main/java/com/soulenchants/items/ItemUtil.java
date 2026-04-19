package com.soulenchants.items;

import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemUtil {

    public static final String NBT_ENCHANTS = "se_enchants";
    public static final String NBT_BOOK_ENCHANT = "se_book_enchant";
    public static final String NBT_BOOK_LEVEL = "se_book_level";
    public static final String NBT_BOOK_SUCCESS = "se_book_success";
    public static final String NBT_BOOK_DESTROY = "se_book_destroy";
    public static final String NBT_DUST_RATE = "se_dust_rate";
    public static final String NBT_BLACK_SCROLL = "se_black_scroll";
    public static final String NBT_WHITE_SCROLL = "se_white_scroll";

    private static final String LORE_HEADER = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ Enchants ✦";

    public static Map<String, Integer> getEnchants(ItemStack item) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (item == null || item.getType().name().equals("AIR")) return map;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(NBT_ENCHANTS)) return map;
        de.tr7zw.changeme.nbtapi.NBTCompound compound = nbt.getCompound(NBT_ENCHANTS);
        for (String key : compound.getKeys()) {
            map.put(key, compound.getInteger(key));
        }
        return map;
    }

    public static int getLevel(ItemStack item, String enchantId) {
        return getEnchants(item).getOrDefault(enchantId.toLowerCase(), 0);
    }

    public static ItemStack addEnchant(ItemStack item, String enchantId, int level) {
        if (item == null) return null;
        NBTItem nbt = new NBTItem(item);
        de.tr7zw.changeme.nbtapi.NBTCompound compound = nbt.hasKey(NBT_ENCHANTS)
                ? nbt.getCompound(NBT_ENCHANTS)
                : nbt.addCompound(NBT_ENCHANTS);
        compound.setInteger(enchantId.toLowerCase(), level);
        ItemStack updated = nbt.getItem();
        return renderLore(updated);
    }

    public static ItemStack removeEnchant(ItemStack item, String enchantId) {
        if (item == null) return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(NBT_ENCHANTS)) return item;
        de.tr7zw.changeme.nbtapi.NBTCompound compound = nbt.getCompound(NBT_ENCHANTS);
        compound.removeKey(enchantId.toLowerCase());
        ItemStack updated = nbt.getItem();
        return renderLore(updated);
    }

    private static final String DIVIDER = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH
            + "                                  ";

    public static ItemStack renderLore(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        // Strip our prior enchant block (header + entries until next divider)
        Iterator<String> it = lore.iterator();
        boolean inBlock = false;
        while (it.hasNext()) {
            String line = it.next();
            if (line.equals(LORE_HEADER)) { inBlock = true; it.remove(); continue; }
            if (inBlock) {
                if (line.equals(DIVIDER)) { it.remove(); inBlock = false; continue; }
                String stripped = ChatColor.stripColor(line);
                if (stripped.isEmpty()) { it.remove(); continue; }
                boolean matches = false;
                for (CustomEnchant e : EnchantRegistry.all()) {
                    String enchantName = e.getDisplayName();
                    if (stripped.contains(enchantName)) { matches = true; break; }
                }
                if (matches) it.remove();
                else { inBlock = false; }
            }
        }
        Map<String, Integer> enchants = getEnchants(item);
        if (!enchants.isEmpty()) {
            List<String> block = new ArrayList<>();
            block.add(LORE_HEADER);
            for (Map.Entry<String, Integer> e : enchants.entrySet()) {
                CustomEnchant ce = EnchantRegistry.get(e.getKey());
                if (ce == null) continue;
                ChatColor c = ce.getTier().getColor();
                block.add(c + "▸ " + ce.getDisplayName() + " " + CustomEnchant.roman(e.getValue()));
            }
            block.add(DIVIDER);
            lore.addAll(0, block);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
