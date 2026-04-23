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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Handles attach/detach via inventory clicks + drags.
 *
 *   ATTACH   Three accepted paths:
 *            (a) drop a mask-item onto an EXISTING helmet (SWAP_WITH_CURSOR,
 *                PLACE_*, or HOTBAR_SWAP). Consumes 1 mask, writes
 *                se_mask_attached to the helmet.
 *            (b) drop a mask-item into an EMPTY armor helmet slot —
 *                the mask becomes its own helmet, wearing the mask's
 *                base material with se_mask_attached set. This is the
 *                "mask-as-helmet" shortcut players expect.
 *            (c) drag (InventoryDragEvent) a mask-item into a single
 *                slot containing a helmet (or empty armor slot) — same
 *                rules as (a)/(b).
 *
 *   DETACH   right-click a helmet in your inventory that has an attached
 *            mask, empty cursor. Strips the NBT, gives the mask item back
 *            to the cursor.
 */
public final class MaskAttachListener implements Listener {

    /** Player inventory armor slot indices (raw-slot scoped to player inv). */
    private static final int HELMET_SLOT = 39;

    private final SoulEnchants plugin;

    public MaskAttachListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        ItemStack cursor = e.getCursor();
        ItemStack target = e.getCurrentItem();

        // ── DETACH ─────────────────────────────────────────────────────
        // Right-click a helmet in inventory with empty cursor → detach.
        if ((cursor == null || cursor.getType() == Material.AIR)
                && e.getClick() == ClickType.RIGHT
                && isHelmet(target)
                && MaskRegistry.hasAttachedMask(target)) {
            MaskRegistry.DetachResult result = MaskRegistry.detach(target);
            if (result == null) return;

            e.setCancelled(true);
            final int slot = e.getSlot();
            final Player p = (Player) e.getWhoClicked();
            new BukkitRunnable() {
                @Override public void run() {
                    if (e.getClickedInventory() != null) {
                        e.getClickedInventory().setItem(slot, result.cleanedHelmet);
                    }
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
            return;
        }

        // ── ATTACH — cursor MUST be a mask-item ───────────────────────
        if (!MaskRegistry.isMaskItem(cursor)) return;

        InventoryAction act = e.getAction();
        boolean isAttachClick = act == InventoryAction.PLACE_ALL
                || act == InventoryAction.PLACE_ONE
                || act == InventoryAction.PLACE_SOME
                || act == InventoryAction.SWAP_WITH_CURSOR
                || act == InventoryAction.HOTBAR_SWAP;
        if (!isAttachClick) return;

        // (a) drop onto existing helmet → attach to that helmet
        if (isHelmet(target) && !MaskRegistry.hasAttachedMask(target)) {
            applyAttach(e, cursor, target);
            return;
        }

        // (b) drop into EMPTY armor helmet slot → mask becomes the helmet
        if ((target == null || target.getType() == Material.AIR)
                && e.getSlotType() == InventoryType.SlotType.ARMOR
                && e.getSlot() == HELMET_SLOT) {
            applyAttachToSelfMaterial(e, cursor);
        }
    }

    /**
     * Drag-to-single-slot path. InventoryDragEvent fires when the player
     * holds left-click and releases on a single slot (common mask drop
     * gesture). For single-slot drags we mirror the click path.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        ItemStack cursor = e.getOldCursor();
        if (!MaskRegistry.isMaskItem(cursor)) return;
        if (e.getRawSlots().size() != 1) return; // only single-slot drags

        int rawSlot = e.getRawSlots().iterator().next();
        org.bukkit.inventory.Inventory top = e.getView().getTopInventory();
        // For our purposes only the player's own inventory matters —
        // when the top is also the player inv (survival crafting) raw slot
        // 5 is the helmet; when the top is a container, raw slots ≥ top.size()
        // land in the player inv with offset.
        int playerSlot;
        if (top.getType() == InventoryType.CRAFTING) {
            // Survival inv layout: raw 5 == helmet
            if (rawSlot == 5) playerSlot = HELMET_SLOT;
            else playerSlot = -1;
        } else {
            int topSize = top.getSize();
            if (rawSlot < topSize) return; // dragged into container — ignore
            int adjusted = rawSlot - topSize + 9; // bukkit inv-mapping for bottom half
            playerSlot = adjusted;
        }
        if (playerSlot < 0) return;

        Player p = (Player) e.getWhoClicked();
        ItemStack current = p.getInventory().getItem(playerSlot);

        // Attach to existing helmet at the drag target
        if (isHelmet(current) && !MaskRegistry.hasAttachedMask(current)) {
            e.setCancelled(true);
            final ItemStack cursorClone = cursor.clone();
            Mask mask = MaskRegistry.get(MaskRegistry.maskItemId(cursorClone));
            if (mask == null) return;
            final ItemStack attachedHelmet = mask.applyTo(current);
            final int slot = playerSlot;
            new BukkitRunnable() {
                @Override public void run() {
                    p.getInventory().setItem(slot, attachedHelmet);
                    decrementCursor(p, cursorClone);
                    p.updateInventory();
                    p.playSound(p.getLocation(), Sound.ITEM_PICKUP, 0.8f, 1.6f);
                    Chat.good(p, "Attached " + MessageStyle.TIER_EPIC + mask.getDisplayName()
                            + MessageStyle.GOOD + " to your helmet.");
                    ItemStack worn = p.getInventory().getHelmet();
                    if (worn != null && worn.isSimilar(attachedHelmet)) {
                        p.getInventory().setHelmet(attachedHelmet);
                    }
                }
            }.runTask(plugin);
            return;
        }

        // Drag into empty helmet armor slot → mask becomes its own helmet
        if ((current == null || current.getType() == Material.AIR) && playerSlot == HELMET_SLOT) {
            e.setCancelled(true);
            final ItemStack cursorClone = cursor.clone();
            Mask mask = MaskRegistry.get(MaskRegistry.maskItemId(cursorClone));
            if (mask == null) return;
            final ItemStack attachedHelmet = buildSelfMaskHelmet(mask);
            new BukkitRunnable() {
                @Override public void run() {
                    p.getInventory().setHelmet(attachedHelmet);
                    decrementCursor(p, cursorClone);
                    p.updateInventory();
                    p.playSound(p.getLocation(), Sound.ITEM_PICKUP, 0.8f, 1.6f);
                    Chat.good(p, "Equipped " + MessageStyle.TIER_EPIC + mask.getDisplayName()
                            + MessageStyle.GOOD + " as your helmet.");
                }
            }.runTask(plugin);
        }
    }

    // ──────────────── Helpers ────────────────

    private void applyAttach(InventoryClickEvent e, ItemStack cursor, ItemStack target) {
        String maskId = MaskRegistry.maskItemId(cursor);
        Mask mask = MaskRegistry.get(maskId);
        if (mask == null) return;

        e.setCancelled(true);
        final ItemStack attachedHelmet = mask.applyTo(target);
        final int slot = e.getSlot();
        final Player p = (Player) e.getWhoClicked();
        final ItemStack cursorClone = cursor.clone();
        new BukkitRunnable() {
            @Override public void run() {
                if (e.getClickedInventory() != null) {
                    e.getClickedInventory().setItem(slot, attachedHelmet);
                }
                decrementCursor(p, cursorClone);
                p.updateInventory();
                p.playSound(p.getLocation(), Sound.ITEM_PICKUP, 0.8f, 1.6f);
                Chat.good(p, "Attached " + MessageStyle.TIER_EPIC + mask.getDisplayName()
                        + MessageStyle.GOOD + " to your helmet.");
                ItemStack wornHelm = p.getInventory().getHelmet();
                if (wornHelm != null && wornHelm.isSimilar(attachedHelmet)) {
                    p.getInventory().setHelmet(attachedHelmet);
                }
            }
        }.runTask(plugin);
    }

    /** Mask placed into empty helmet slot → build a fresh helmet using
     *  the mask's own material, with se_mask_attached NBT. */
    private void applyAttachToSelfMaterial(InventoryClickEvent e, ItemStack cursor) {
        String maskId = MaskRegistry.maskItemId(cursor);
        Mask mask = MaskRegistry.get(maskId);
        if (mask == null) return;

        e.setCancelled(true);
        final ItemStack attachedHelmet = buildSelfMaskHelmet(mask);
        final Player p = (Player) e.getWhoClicked();
        final ItemStack cursorClone = cursor.clone();
        new BukkitRunnable() {
            @Override public void run() {
                p.getInventory().setHelmet(attachedHelmet);
                decrementCursor(p, cursorClone);
                p.updateInventory();
                p.playSound(p.getLocation(), Sound.ITEM_PICKUP, 0.8f, 1.6f);
                Chat.good(p, "Equipped " + MessageStyle.TIER_EPIC + mask.getDisplayName()
                        + MessageStyle.GOOD + " as your helmet.");
            }
        }.runTask(plugin);
    }

    /** Fresh helmet from the mask's visual material + se_mask_attached NBT. */
    private static ItemStack buildSelfMaskHelmet(Mask mask) {
        ItemStack base = mask.buildVisual();
        return mask.applyTo(base);
    }

    private static void decrementCursor(Player p, ItemStack cursorClone) {
        int amt = cursorClone.getAmount();
        if (amt > 1) {
            cursorClone.setAmount(amt - 1);
            p.setItemOnCursor(cursorClone);
        } else {
            p.setItemOnCursor(new ItemStack(Material.AIR));
        }
    }

    private static boolean isHelmet(ItemStack it) {
        if (it == null) return false;
        String n = it.getType().name();
        return n.endsWith("_HELMET") || n.equals("SKULL_ITEM") || n.equals("PUMPKIN");
    }
}
