package com.soulenchants.masks;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles attach/detach via inventory clicks. Nordic-style:
 *
 *   ATTACH   drag a mask-item onto a helmet slot (PLACE_* or SWAP_WITH_CURSOR
 *            where cursor is a mask-item and target is a helmet that
 *            isn't already wearing a mask). Consumes 1 mask-item; adds
 *            se_mask_attached NBT to the helmet; plays ARMOR_EQUIP sound.
 *
 *   DETACH   right-click a helmet in your inventory that has an attached
 *            mask, empty cursor. Strips the NBT, gives the mask item back
 *            to the cursor.
 *
 * Visual refresh is instant because we call setHelmet(setHelmet(helmet))
 * which triggers a ENTITY_EQUIPMENT packet that the packet injector
 * rewrites according to the new NBT state.
 */
public final class MaskAttachListener implements Listener {

    private final SoulEnchants plugin;

    public MaskAttachListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        ItemStack cursor = e.getCursor();
        ItemStack target = e.getCurrentItem();

        // ── ATTACH ─────────────────────────────────────────────────────
        if (MaskRegistry.isMaskItem(cursor) && isHelmet(target)
                && !MaskRegistry.hasAttachedMask(target)) {
            InventoryAction act = e.getAction();
            boolean isPlace = act == InventoryAction.PLACE_ALL
                    || act == InventoryAction.PLACE_ONE
                    || act == InventoryAction.PLACE_SOME
                    || act == InventoryAction.SWAP_WITH_CURSOR;
            if (!isPlace) return;

            e.setCancelled(true);
            String maskId = MaskRegistry.maskItemId(cursor);
            Mask mask = MaskRegistry.get(maskId);
            if (mask == null) return;

            final ItemStack attachedHelmet = mask.applyTo(target);
            final int slot = e.getSlot();
            final Player p = (Player) e.getWhoClicked();
            final ItemStack cursorClone = cursor.clone();
            new BukkitRunnable() {
                @Override public void run() {
                    e.getClickedInventory().setItem(slot, attachedHelmet);
                    int amt = cursorClone.getAmount();
                    if (amt > 1) {
                        cursorClone.setAmount(amt - 1);
                        p.setItemOnCursor(cursorClone);
                    } else {
                        p.setItemOnCursor(new ItemStack(Material.AIR));
                    }
                    p.updateInventory();
                    p.playSound(p.getLocation(), Sound.ITEM_PICKUP, 0.8f, 1.6f);
                    Chat.good(p, "Attached " + MessageStyle.TIER_EPIC + mask.getDisplayName()
                            + MessageStyle.GOOD + " to your helmet.");
                    // If the wearer has this helmet equipped, force a re-render so nearby
                    // players see the mask swap immediately.
                    ItemStack wornHelm = p.getInventory().getHelmet();
                    if (wornHelm != null && wornHelm.isSimilar(attachedHelmet)) {
                        p.getInventory().setHelmet(attachedHelmet);
                    }
                }
            }.runTask(plugin);
            return;
        }

        // ── DETACH ─────────────────────────────────────────────────────
        // Right-click a helmet in inventory with an empty cursor, helmet has
        // a mask attached → detach.
        if (cursor == null || cursor.getType() == Material.AIR) {
            if (e.getClick() != ClickType.RIGHT) return;
            if (!isHelmet(target)) return;
            if (!MaskRegistry.hasAttachedMask(target)) return;
            MaskRegistry.DetachResult result = MaskRegistry.detach(target);
            if (result == null) return;

            e.setCancelled(true);
            final int slot = e.getSlot();
            final Player p = (Player) e.getWhoClicked();
            new BukkitRunnable() {
                @Override public void run() {
                    e.getClickedInventory().setItem(slot, result.cleanedHelmet);
                    p.setItemOnCursor(result.detachedMask);
                    p.updateInventory();
                    p.playSound(p.getLocation(), Sound.ITEM_PICKUP, 0.8f, 0.8f);
                    Chat.info(p, "Detached the mask from your helmet.");
                    ItemStack wornHelm = p.getInventory().getHelmet();
                    if (wornHelm != null && wornHelm.isSimilar(result.cleanedHelmet)) {
                        p.getInventory().setHelmet(result.cleanedHelmet);
                    }
                }
            }.runTask(plugin);
        }
    }

    private static boolean isHelmet(ItemStack it) {
        if (it == null) return false;
        String n = it.getType().name();
        return n.endsWith("_HELMET") || n.equals("SKULL_ITEM");
    }
}
