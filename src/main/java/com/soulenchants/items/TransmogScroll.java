package com.soulenchants.items;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Nordic-style transmog scroll. Click the scroll onto an enchanted item in
 * inventory to:
 *   • Sort the enchant lore by tier (Mythic → Common)
 *   • Prepend [N] to the item name where N is the enchant count
 *
 * Consumes one scroll per use. Item must already have at least one custom
 * enchant for the scroll to apply.
 */
public class TransmogScroll implements Listener {

    public static ItemStack item() { return item(1); }

    public static ItemStack item(int amount) {
        ItemStack it = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Transmog Scroll");
        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Organizes enchants by " + ChatColor.YELLOW + ChatColor.UNDERLINE + "rarity",
                ChatColor.GRAY + "and prepends the enchant " + ChatColor.AQUA + "count" + ChatColor.GRAY + " to the name.",
                "",
                ChatColor.YELLOW + "" + ChatColor.ITALIC + "Place scroll on item to apply."
        );
        meta.setLore(lore);
        it.setItemMeta(meta);
        NBTItem nbt = new NBTItem(it);
        nbt.setBoolean(ItemUtil.NBT_TRANSMOG_SCROLL, true);
        return nbt.getItem();
    }

    public static boolean isTransmogScroll(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        NBTItem nbt = new NBTItem(item);
        return nbt.hasKey(ItemUtil.NBT_TRANSMOG_SCROLL) && nbt.getBoolean(ItemUtil.NBT_TRANSMOG_SCROLL);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTransmogClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        ItemStack cursor = e.getCursor();
        ItemStack target = e.getCurrentItem();
        if (!isTransmogScroll(cursor)) return;
        if (target == null || target.getType() == Material.AIR) return;
        // Must have at least one of our custom enchants on it
        if (ItemUtil.getEnchants(target).isEmpty()) {
            ((Player) e.getWhoClicked()).sendMessage(ChatColor.RED + "✦ Transmog Scroll: target has no custom enchants.");
            return;
        }
        e.setCancelled(true);
        // Apply transmog
        ItemStack transmogged = ItemUtil.transmog(target);
        e.setCurrentItem(transmogged);
        // Decrement scroll
        if (cursor.getAmount() <= 1) {
            e.setCursor(null);
        } else {
            cursor.setAmount(cursor.getAmount() - 1);
            e.setCursor(cursor);
        }
        Player p = (Player) e.getWhoClicked();
        p.sendMessage(ChatColor.LIGHT_PURPLE + "✦ Transmog Scroll applied — enchants sorted by rarity.");
        try { p.playSound(p.getLocation(), org.bukkit.Sound.ORB_PICKUP, 1.0f, 1.5f); } catch (Throwable ignored) {}
    }
}
