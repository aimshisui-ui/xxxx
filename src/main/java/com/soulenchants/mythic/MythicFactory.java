package com.soulenchants.mythic;

import com.soulenchants.style.MessageStyle;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Build the actual ItemStack for a mythic. Each mythic ships with a curated
 * vanilla enchant set so they feel decisive in the hand before their custom
 * proc even fires:
 *
 *   Sword:        Sharpness 5, Unbreaking 3, Fire Aspect 2, Looting 3
 *   Axe:          Sharpness 6, Unbreaking 3, Fire Aspect 2, Looting 3
 *   Bow:          Power 5,     Unbreaking 3, Punch 2,       Flame 1, Infinity 1
 *   Fishing Rod:  Unbreaking 3, Luck of the Sea 3, Lure 3    (no combat enchants)
 *
 * Phoenix Feather is the fire-themed mythic — gets Fire Aspect 2 just like
 * everything else; its on-kill ignite radius stacks with vanilla ignite so
 * it becomes the de-facto "burn everything" weapon.
 *
 * No HIDE_ENCHANTS flag: players see the vanilla enchants under the mythic
 * badge, so gear literacy reads at a glance.
 */
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
            item.setItemMeta(meta);
        }

        applyVanillaEnchants(item, mat);

        NBTItem nbt = new NBTItem(item);
        nbt.setString(MythicRegistry.NBT_KEY, mythicId);
        return nbt.getItem();
    }

    /** Paint vanilla enchants onto a raw stack based on its material class. */
    private static void applyVanillaEnchants(ItemStack item, Material mat) {
        String n = mat.name();
        if (n.endsWith("_SWORD")) {
            safeEnchant(item, Enchantment.DAMAGE_ALL,  5);   // Sharpness V
            safeEnchant(item, Enchantment.DURABILITY,  3);   // Unbreaking III
            safeEnchant(item, Enchantment.FIRE_ASPECT, 2);   // Fire Aspect II
            safeEnchant(item, Enchantment.LOOT_BONUS_MOBS, 3); // Looting III
            return;
        }
        if (n.endsWith("_AXE")) {
            safeEnchant(item, Enchantment.DAMAGE_ALL,  6);   // Sharpness VI
            safeEnchant(item, Enchantment.DURABILITY,  3);
            safeEnchant(item, Enchantment.FIRE_ASPECT, 2);
            safeEnchant(item, Enchantment.LOOT_BONUS_MOBS, 3);
            return;
        }
        if (mat == Material.BOW) {
            safeEnchant(item, Enchantment.ARROW_DAMAGE,   5); // Power V
            safeEnchant(item, Enchantment.ARROW_KNOCKBACK,2); // Punch II
            safeEnchant(item, Enchantment.ARROW_FIRE,     1); // Flame
            safeEnchant(item, Enchantment.ARROW_INFINITE, 1); // Infinity
            safeEnchant(item, Enchantment.DURABILITY,     3);
            return;
        }
        if (mat == Material.FISHING_ROD) {
            safeEnchant(item, Enchantment.DURABILITY,    3);
            safeEnchant(item, Enchantment.LUCK,          3); // Luck of the Sea
            safeEnchant(item, Enchantment.LURE,          3);
            return;
        }
        // Unknown material — still glow via Unbreaking
        safeEnchant(item, Enchantment.DURABILITY, 1);
    }

    /** Quietly skip invalid enchant/level combinations on exotic materials. */
    private static void safeEnchant(ItemStack item, Enchantment e, int level) {
        try { item.addUnsafeEnchantment(e, level); }
        catch (Throwable ignored) {}
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
