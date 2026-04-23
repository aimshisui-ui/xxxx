package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.SoulGem;
import com.soulenchants.items.SoulGemUtil;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Only job (post-deposit-removal): merge gems when one is dropped on top of
 * another. Right-click no longer deposits — soul withdrawals are one-way,
 * enforced at the SoulGemUtil level (no deposit() method exists).
 *
 * Merge flow: two gems collide via PLACE_* or SWAP_WITH_CURSOR in any
 * inventory slot → sum amounts, play Sound.ANVIL_USE, replace the slot's
 * gem with the combined item and clear the cursor.
 */
public final class SoulGemListener implements Listener {

    private final SoulEnchants plugin;

    public SoulGemListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        ItemStack cursor = e.getCursor();
        ItemStack target = e.getCurrentItem();
        if (!SoulGem.isGem(cursor) || !SoulGem.isGem(target)) return;

        InventoryAction act = e.getAction();
        boolean isPlace = act == InventoryAction.PLACE_ALL
                || act == InventoryAction.PLACE_ONE
                || act == InventoryAction.PLACE_SOME
                || act == InventoryAction.SWAP_WITH_CURSOR;
        if (!isPlace) return;

        e.setCancelled(true);
        final ItemStack merged = SoulGemUtil.merge(cursor, target);
        if (merged == null) return;
        final int slot = e.getSlot();
        final Player p = (Player) e.getWhoClicked();
        // Defer so the cancel doesn't revert the slot write.
        new BukkitRunnable() {
            @Override public void run() {
                e.getClickedInventory().setItem(slot, merged);
                p.setItemOnCursor(new ItemStack(Material.AIR));
                p.updateInventory();
                p.playSound(p.getLocation(), Sound.ANVIL_USE, 0.8f, 1.4f);
                Chat.info(p, "Merged soul gems — total "
                        + MessageStyle.VALUE + SoulGem.formatNum(SoulGem.amount(merged))
                        + MessageStyle.MUTED + " souls.");
            }
        }.runTask(plugin);
    }
}
