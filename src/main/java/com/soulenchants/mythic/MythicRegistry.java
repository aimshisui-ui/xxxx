package com.soulenchants.mythic;

import com.soulenchants.SoulEnchants;
import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.impl.*;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Central registry mirrored on EnchantRegistry. */
public final class MythicRegistry {

    public static final String NBT_KEY = "se_mythic";

    private static final Map<String, MythicWeapon> MYTHICS = new LinkedHashMap<>();

    private MythicRegistry() {}

    public static void registerDefaults(SoulEnchants plugin, MythicConfig cfg) {
        MYTHICS.clear();
        register(new CrimsonTongue(cfg));
        register(new Wraithcleaver(cfg));
        register(new Stormbringer(plugin, cfg));
        register(new Voidreaver(plugin, cfg));
        register(new Dawnbringer(cfg));
        register(new Sunderer(cfg));
        register(new PhoenixFeather(cfg));
        register(new Soulbinder(plugin, cfg));
        register(new Tidecaller(cfg));
    }

    public static void register(MythicWeapon m) { MYTHICS.put(m.getId(), m); }
    public static MythicWeapon get(String id) { return id == null ? null : MYTHICS.get(id); }
    public static Collection<MythicWeapon> all() { return MYTHICS.values(); }

    /** Return the mythic id from an item's NBT, or null. */
    public static String idOf(ItemStack item) {
        if (item == null || item.getType().name().equals("AIR")) return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(NBT_KEY)) return null;
        String s = nbt.getString(NBT_KEY);
        return s == null || s.isEmpty() ? null : s;
    }

    /** Return the resolved mythic for an item, or null. */
    public static MythicWeapon of(ItemStack item) { return get(idOf(item)); }
}
