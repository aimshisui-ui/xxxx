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
    public static final String NBT_TRANSMOG_SCROLL = "se_transmog_scroll";

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

    /**
     * Sort the item's enchant NBT by tier descending (Mythic → Common) then by
     * display name. Re-renders lore + prefixes "[N]" to the display name where
     * N is the enchant count. One-shot transmog (does not persist a sort flag).
     */
    public static ItemStack transmog(ItemStack item) {
        if (item == null) return null;
        Map<String, Integer> enchants = getEnchants(item);
        if (enchants.isEmpty()) return item;
        // Strip + rebuild compound in tier-sorted order
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(enchants.entrySet());
        entries.sort((a, b) -> {
            CustomEnchant ea = EnchantRegistry.get(a.getKey());
            CustomEnchant eb = EnchantRegistry.get(b.getKey());
            int ta = (ea == null) ? -1 : ea.getTier().ordinal();
            int tb = (eb == null) ? -1 : eb.getTier().ordinal();
            if (ta != tb) return Integer.compare(tb, ta);   // higher tier first
            String na = ea == null ? a.getKey() : ea.getDisplayName();
            String nb = eb == null ? b.getKey() : eb.getDisplayName();
            return na.compareTo(nb);
        });
        // Wipe + write back in sorted order
        NBTItem nbt = new NBTItem(item);
        if (nbt.hasKey(NBT_ENCHANTS)) nbt.removeKey(NBT_ENCHANTS);
        de.tr7zw.changeme.nbtapi.NBTCompound c = nbt.addCompound(NBT_ENCHANTS);
        for (Map.Entry<String, Integer> e : entries) c.setInteger(e.getKey(), e.getValue());
        ItemStack updated = nbt.getItem();
        // Rebuild lore from the sorted NBT
        updated = renderLore(updated);
        // Prefix [N] to display name (idempotent — strip prior [N] first)
        ItemMeta meta = updated.getItemMeta();
        if (meta != null) {
            String name = meta.hasDisplayName() ? meta.getDisplayName() : updated.getType().name();
            String stripped = name.replaceAll("^§[a-f0-9]\\[\\d+\\] ", "");
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "[" + entries.size() + "] " + stripped);
            updated.setItemMeta(meta);
        }
        return updated;
    }

    public static ItemStack renderLore(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        // Strip our prior enchant block: everything from LORE_HEADER through
        // the next DIVIDER (inclusive). Block contains enchant rows + their
        // gray effect-description sub-lines.
        Iterator<String> it = lore.iterator();
        boolean inBlock = false;
        while (it.hasNext()) {
            String line = it.next();
            if (line.equals(LORE_HEADER)) { inBlock = true; it.remove(); continue; }
            if (inBlock) {
                if (line.equals(DIVIDER)) { it.remove(); inBlock = false; continue; }
                it.remove();
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
                String effect = com.soulenchants.enchants.EnchantEffects.describe(e.getKey(), e.getValue());
                if (effect != null) {
                    block.add(ChatColor.GRAY + "    " + effect);
                }
            }
            block.add(DIVIDER);
            lore.addAll(0, block);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
