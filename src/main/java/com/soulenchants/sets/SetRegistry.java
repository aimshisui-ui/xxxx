package com.soulenchants.sets;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static registry of every armor set the plugin knows about. Lookup is by
 * stable id (`se_set_id` NBT key). Sets register themselves via {@link #add}
 * during plugin enable.
 */
public final class SetRegistry {

    /** NBT key written on each gear piece to identify which set it belongs to. */
    public static final String NBT_SET_ID = "se_set_id";

    private static final Map<String, SetBonus> BY_ID = new LinkedHashMap<>();

    private SetRegistry() {}

    public static void add(SetBonus set) { BY_ID.put(set.id().toLowerCase(), set); }

    public static SetBonus get(String id) {
        return id == null ? null : BY_ID.get(id.toLowerCase());
    }

    public static java.util.Collection<SetBonus> all() { return BY_ID.values(); }

    /** Read the set id from an item's NBT, or null if it isn't a set piece. */
    public static String idOf(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(NBT_SET_ID)) return null;
        String s = nbt.getString(NBT_SET_ID);
        return (s == null || s.isEmpty()) ? null : s;
    }

    /** Tag a piece with a set id and return the new ItemStack. */
    public static ItemStack tag(ItemStack item, String setId) {
        if (item == null) return null;
        NBTItem nbt = new NBTItem(item);
        nbt.setString(NBT_SET_ID, setId);
        return nbt.getItem();
    }
}
