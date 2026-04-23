package com.soulenchants.masks;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mask registry. Identity lives on items (inventory-item mask, and the
 * helmet it's attached to) via two NBT keys:
 *
 *   se_mask_item      → string id, carried by the unattached inventory
 *                        item the player drags
 *   se_mask_attached  → string id, carried by the helmet the mask has
 *                        been attached to
 *
 * Both keys point to the same Mask.id — the registry resolves them.
 */
public final class MaskRegistry {

    public static final String NBT_MASK_ITEM     = "se_mask_item";
    public static final String NBT_MASK_ATTACHED = "se_mask_attached";

    private static final Map<String, Mask> MASKS = new LinkedHashMap<>();

    private MaskRegistry() {}

    public static void registerDefaults() {
        MASKS.clear();
        for (Mask m : Mask.defaults()) MASKS.put(m.getId(), m);
    }

    public static void register(Mask m) { MASKS.put(m.getId(), m); }
    public static Mask get(String id)   { return id == null ? null : MASKS.get(id); }
    public static Collection<Mask> all(){ return MASKS.values(); }

    /** Is this ItemStack a standalone mask-item (not yet attached)? */
    public static boolean isMaskItem(ItemStack it) {
        if (it == null) return false;
        NBTItem nbt = new NBTItem(it);
        return nbt.hasKey(NBT_MASK_ITEM);
    }

    public static String maskItemId(ItemStack it) {
        if (it == null) return null;
        NBTItem nbt = new NBTItem(it);
        return nbt.hasKey(NBT_MASK_ITEM) ? nbt.getString(NBT_MASK_ITEM) : null;
    }

    /** Does this helmet have an attached mask? */
    public static boolean hasAttachedMask(ItemStack helmet) {
        if (helmet == null) return false;
        NBTItem nbt = new NBTItem(helmet);
        return nbt.hasKey(NBT_MASK_ATTACHED);
    }

    public static String attachedMaskId(ItemStack helmet) {
        if (helmet == null) return null;
        NBTItem nbt = new NBTItem(helmet);
        return nbt.hasKey(NBT_MASK_ATTACHED) ? nbt.getString(NBT_MASK_ATTACHED) : null;
    }

    /** Strip the attached mask from a helmet, returning both the cleaned
     *  helmet and the detached mask item. */
    public static DetachResult detach(ItemStack helmet) {
        if (!hasAttachedMask(helmet)) return null;
        String id = attachedMaskId(helmet);
        Mask m = get(id);
        if (m == null) return null;

        NBTItem nbt = new NBTItem(helmet);
        nbt.removeKey(NBT_MASK_ATTACHED);
        ItemStack cleanHelmet = nbt.getItem();
        org.bukkit.inventory.meta.ItemMeta meta = cleanHelmet.getItemMeta();
        if (meta != null && meta.hasLore()) {
            java.util.List<String> lore = new java.util.ArrayList<>(meta.getLore());
            lore.removeIf(s -> s != null && s.contains("ATTACHED:"));
            meta.setLore(lore);
            cleanHelmet.setItemMeta(meta);
        }
        return new DetachResult(cleanHelmet, m.buildInventoryItem());
    }

    public static final class DetachResult {
        public final ItemStack cleanedHelmet;
        public final ItemStack detachedMask;
        DetachResult(ItemStack h, ItemStack m) { this.cleanedHelmet = h; this.detachedMask = m; }
    }
}
