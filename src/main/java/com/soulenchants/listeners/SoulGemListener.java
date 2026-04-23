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
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Two jobs:
 *   • Right-click a held Soul Gem to deposit its balance into the ledger and
 *     consume the item. Feedback + sound mirrors the /soulgem deposit path.
 *   • Drop one Soul Gem on top of another in the inventory to merge — sums
 *     balances, plays Sound.ANVIL_USE (Nordic-accurate). Cursor is replaced
 *     with the combined gem; the slot becomes empty.
 */
public final class SoulGemListener implements Listener {

    private final SoulEnchants plugin;

    public SoulGemListener(SoulEnchants plugin) { this.plugin = plugin; }

    // ── Right-click deposit ─────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack held = e.getItem();
        if (!SoulGem.isGem(held)) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        long amt = SoulGemUtil.deposit(plugin, p, held);
        p.setItemInHand(new ItemStack(Material.AIR));
        Chat.good(p, "Deposited " + MessageStyle.VALUE + SoulGem.formatNum(amt)
                + MessageStyle.GOOD + " souls to the ledger.");
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.8f, 1.8f);
    }

    // ── Stack-merge on inventory click ──────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        // Only care when a gem on the cursor lands on another gem in a slot.
        // Any "place cursor onto slot" action — left-click, right-click,
        // shift-moves — funnels through here; we catch PLACE_* variants.
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
        // Defer state changes by a tick so the cancel doesn't revert them.
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
