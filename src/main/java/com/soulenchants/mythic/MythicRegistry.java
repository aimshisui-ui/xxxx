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

    /** Primary mythic id — the weapon's identity (Crimson Tongue, Stormbringer…). */
    public static final String NBT_KEY = "se_mythic";
    /**
     * Optional secondary effect id — "ability slot". A weapon with a
     * Stormbringer core and a Crimson Tongue ability procs both: lightning
     * chain on crit AND Bloodlust heal per Bleed tick.
     *
     * Abilities reuse the same 9 mythic implementations as cores, so the
     * pool is automatically combinatorial (9 × 9 = 81 unique weapons,
     * minus matching core==ability pairs which we reject at bind time).
     */
    public static final String NBT_ABILITY_KEY = "se_mythic_ability";

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
        // v1.2 — PvE-focused mythics
        register(new Graverend(plugin));
        register(new Emberlash());
        register(new Ruinhammer(plugin));
    }

    public static void register(MythicWeapon m) { MYTHICS.put(m.getId(), m); }
    public static MythicWeapon get(String id) { return id == null ? null : MYTHICS.get(id); }
    public static Collection<MythicWeapon> all() { return MYTHICS.values(); }

    /** Return the primary mythic id from an item's NBT, or null. */
    public static String idOf(ItemStack item) {
        if (item == null || item.getType().name().equals("AIR") || item.getAmount() <= 0) return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(NBT_KEY)) return null;
        String s = nbt.getString(NBT_KEY);
        return s == null || s.isEmpty() ? null : s;
    }

    /** Return the secondary ability id from an item's NBT, or null if none bound. */
    public static String abilityIdOf(ItemStack item) {
        if (item == null || item.getType().name().equals("AIR") || item.getAmount() <= 0) return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(NBT_ABILITY_KEY)) return null;
        String s = nbt.getString(NBT_ABILITY_KEY);
        return s == null || s.isEmpty() ? null : s;
    }

    /** Return the resolved primary mythic for an item, or null. */
    public static MythicWeapon of(ItemStack item) { return get(idOf(item)); }

    /** Return the resolved secondary ability for an item, or null. */
    public static MythicWeapon abilityOf(ItemStack item) { return get(abilityIdOf(item)); }

    /**
     * Bind an ability id to a mythic weapon ItemStack. Returns the updated
     * stack (NBT-API always returns a new instance). Fails safely if:
     *   - item has no core mythic NBT (not a mythic)
     *   - abilityId == coreId (would double-proc the same effect)
     *   - abilityId not in registry
     */
    public static ItemStack bindAbility(ItemStack item, String abilityId) {
        if (item == null) return null;
        String coreId = idOf(item);
        if (coreId == null) return item;
        if (abilityId == null || !MYTHICS.containsKey(abilityId)) return item;
        if (abilityId.equals(coreId)) return item;
        NBTItem nbt = new NBTItem(item);
        nbt.setString(NBT_ABILITY_KEY, abilityId);
        return nbt.getItem();
    }

    /** Remove any bound ability from a mythic. Returns the updated stack. */
    public static ItemStack clearAbility(ItemStack item) {
        if (item == null) return null;
        NBTItem nbt = new NBTItem(item);
        if (nbt.hasKey(NBT_ABILITY_KEY)) nbt.removeKey(NBT_ABILITY_KEY);
        return nbt.getItem();
    }
}
