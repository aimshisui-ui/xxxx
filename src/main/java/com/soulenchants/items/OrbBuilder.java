package com.soulenchants.items;

import com.soulenchants.style.MessageStyle;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot-raising orb items. Drag an orb onto a compatible piece of gear
 * (weapon or armor depending on orb type) and — on a success roll — the
 * item's max-enchant-slot NBT gets bumped to the orb's target value.
 *
 * Orb NBT contract (read by {@link com.soulenchants.listeners.OrbListener}):
 *   {@link #NBT_ORB}        boolean · marker flag
 *   {@link #NBT_ORB_TYPE}   string  · "WEAPON" or "ARMOR"
 *   {@link #NBT_ORB_SLOTS}  int     · target max slots (10-14)
 *   {@link #NBT_ORB_SUCCESS}int     · 0-100 success %
 *
 * The default slot cap is {@link ItemUtil#DEFAULT_MAX_SLOTS} (9), and
 * orbs can ladder a single item up to {@link ItemUtil#DEFAULT_CEILING}
 * (14). An orb whose target is ≤ the item's current max is rejected —
 * you never use an orb to downgrade.
 */
public final class OrbBuilder {

    public enum OrbType {
        WEAPON("Weapon",
                "_SWORD", "_AXE", "BOW", "FISHING_ROD"),
        ARMOR("Armor",
                "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS");

        public final String label;
        public final String[] suffixes;
        OrbType(String label, String... suffixes) { this.label = label; this.suffixes = suffixes; }

        public boolean matches(Material mat) {
            if (mat == null) return false;
            String n = mat.name();
            for (String s : suffixes) {
                if (s.startsWith("_") ? n.endsWith(s) : n.equals(s)) return true;
            }
            return false;
        }

        public static OrbType parse(String raw) {
            if (raw == null) return null;
            String u = raw.toUpperCase();
            if (u.equals("WEAPON") || u.equals("SWORD") || u.equals("AXE") || u.equals("BOW")) return WEAPON;
            if (u.equals("ARMOR")  || u.equals("ARMOUR") || u.equals("HELMET")
                    || u.equals("CHEST") || u.equals("LEGS") || u.equals("BOOTS")) return ARMOR;
            return null;
        }
    }

    public static final String NBT_ORB         = "se_orb";
    public static final String NBT_ORB_TYPE    = "se_orb_type";
    public static final String NBT_ORB_SLOTS   = "se_orb_slots";
    public static final String NBT_ORB_SUCCESS = "se_orb_success";

    private OrbBuilder() {}

    /** Build a fresh orb item. {@code targetSlots} is clamped into
     *  [DEFAULT_MAX_SLOTS+1 .. DEFAULT_CEILING]; {@code successRate} to 1..100. */
    public static ItemStack build(OrbType type, int targetSlots, int successRate) {
        int slots = Math.min(ItemUtil.DEFAULT_CEILING,
                Math.max(ItemUtil.DEFAULT_MAX_SLOTS + 1, targetSlots));
        int succ  = Math.min(100, Math.max(1, successRate));

        ItemStack it = new ItemStack(Material.EYE_OF_ENDER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageStyle.TIER_SOUL + ChatColor.BOLD + "✦ "
                    + slots + "-Slot " + type.label + " Orb");
            List<String> lore = new ArrayList<>();
            lore.add(MessageStyle.MUTED + "" + ChatColor.ITALIC + "Raises an item's enchant capacity.");
            lore.add("");
            lore.add(MessageStyle.TIER_EPIC + "▸ " + MessageStyle.VALUE + "+"
                    + (slots - ItemUtil.DEFAULT_MAX_SLOTS)
                    + MessageStyle.MUTED + " enchant slot" + (slots - ItemUtil.DEFAULT_MAX_SLOTS == 1 ? "" : "s"));
            lore.add(MessageStyle.TIER_EPIC + "▸ " + MessageStyle.VALUE + slots
                    + MessageStyle.MUTED + " max enchantment slots");
            lore.add(MessageStyle.TIER_EPIC + "▸ " + MessageStyle.VALUE + succ + "%"
                    + MessageStyle.MUTED + " success chance");
            lore.add("");
            lore.add(MessageStyle.TIER_LEGENDARY + "Applies to: " + MessageStyle.VALUE + type.label);
            lore.add("");
            lore.add(MessageStyle.SOUL_GOLD + "▸ " + MessageStyle.MUTED + "Drop this orb onto a "
                    + type.label.toLowerCase() + " to apply.");
            lore.add(MessageStyle.BAD + "✗ " + MessageStyle.MUTED + "Consumed on success OR failure.");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        NBTItem nbt = new NBTItem(it);
        nbt.setBoolean(NBT_ORB,        true);
        nbt.setString(NBT_ORB_TYPE,    type.name());
        nbt.setInteger(NBT_ORB_SLOTS,  slots);
        nbt.setInteger(NBT_ORB_SUCCESS, succ);
        return nbt.getItem();
    }

    // ──────────────── NBT accessors ────────────────

    private static boolean isValid(ItemStack it) {
        return it != null && it.getType() != Material.AIR && it.getAmount() > 0;
    }

    public static boolean isOrb(ItemStack it) {
        if (!isValid(it)) return false;
        NBTItem n = new NBTItem(it);
        return n.hasKey(NBT_ORB) && n.getBoolean(NBT_ORB);
    }

    public static OrbType typeOf(ItemStack it) {
        if (!isOrb(it)) return null;
        NBTItem n = new NBTItem(it);
        try { return OrbType.valueOf(n.getString(NBT_ORB_TYPE)); }
        catch (Exception e) { return null; }
    }

    public static int slotsOf(ItemStack it) {
        if (!isOrb(it)) return 0;
        return new NBTItem(it).getInteger(NBT_ORB_SLOTS);
    }

    public static int successOf(ItemStack it) {
        if (!isOrb(it)) return 0;
        return new NBTItem(it).getInteger(NBT_ORB_SUCCESS);
    }
}
