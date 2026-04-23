package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemUtil;
import com.soulenchants.items.OrbBuilder;
import com.soulenchants.style.MessageStyle;
import com.soulenchants.style.SoulVFX;
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

import java.util.Random;

/**
 * Applies a Weapon/Armor Slot Orb to a compatible item on click.
 *
 * Flow:
 *   1. Player holds an orb on cursor, clicks a gear item in inventory.
 *   2. Orb type must match the item (weapon orb ↔ sword/axe/bow/rod,
 *      armor orb ↔ helmet/chestplate/legs/boots).
 *   3. Orb's target slot count must be strictly greater than the item's
 *      current max (never a downgrade).
 *   4. Success roll (orb's success %). On success we bump the item's
 *      se_item_slots NBT to the orb's target value. On failure the orb
 *      is still consumed — same risk model as Cosmic / Nordic.
 */
public final class OrbListener implements Listener {

    private final SoulEnchants plugin;
    private final Random rng = new Random();

    public OrbListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        final ItemStack cursor = e.getCursor();
        final ItemStack target = e.getCurrentItem();
        if (!OrbBuilder.isOrb(cursor)) return;

        InventoryAction act = e.getAction();
        boolean isPlace = act == InventoryAction.PLACE_ALL
                || act == InventoryAction.PLACE_ONE
                || act == InventoryAction.PLACE_SOME
                || act == InventoryAction.SWAP_WITH_CURSOR;
        if (!isPlace) return;
        if (target == null || target.getType() == Material.AIR) return;

        final Player p = (Player) e.getWhoClicked();
        OrbBuilder.OrbType type = OrbBuilder.typeOf(cursor);
        if (type == null) return;

        // Type match — orb gates to its category only.
        if (!type.matches(target.getType())) {
            p.sendMessage(MessageStyle.PREFIX + MessageStyle.BAD + "This orb only applies to "
                    + MessageStyle.VALUE + type.label.toLowerCase() + MessageStyle.BAD + " items.");
            e.setCancelled(true);
            return;
        }

        int orbTarget  = OrbBuilder.slotsOf(cursor);
        int orbSuccess = OrbBuilder.successOf(cursor);
        int currentMax = ItemUtil.getMaxSlots(target);

        if (orbTarget <= currentMax) {
            p.sendMessage(MessageStyle.PREFIX + MessageStyle.BAD + "This item already has "
                    + MessageStyle.VALUE + currentMax + MessageStyle.BAD + " slots — use a higher-tier orb.");
            e.setCancelled(true);
            return;
        }

        // Consume the orb + roll.
        e.setCancelled(true);
        final int finalOrbTarget = orbTarget;
        final boolean success = rng.nextInt(100) < orbSuccess;
        final int slot = e.getSlot();
        final org.bukkit.inventory.Inventory clickedInv = e.getClickedInventory();
        final ItemStack originalTarget = target.clone();
        final ItemStack cursorClone = cursor.clone();

        new BukkitRunnable() {
            @Override public void run() {
                int amt = cursorClone.getAmount();
                if (amt > 1) {
                    cursorClone.setAmount(amt - 1);
                    p.setItemOnCursor(cursorClone);
                } else {
                    p.setItemOnCursor(new ItemStack(Material.AIR));
                }
                if (success) {
                    ItemStack updated = ItemUtil.setMaxSlots(originalTarget, finalOrbTarget);
                    if (clickedInv != null) clickedInv.setItem(slot, updated);
                    p.sendMessage(MessageStyle.PREFIX + MessageStyle.GOOD + MessageStyle.BOLD
                            + "✦ ORB APPLIED " + MessageStyle.RESET + MessageStyle.MUTED
                            + "max slots now " + MessageStyle.VALUE + finalOrbTarget);
                    try { SoulVFX.bookApplySuccess(p); } catch (Throwable ignored) {}
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.9f, 1.4f);
                } else {
                    p.sendMessage(MessageStyle.PREFIX + MessageStyle.BAD + MessageStyle.BOLD
                            + "✗ ORB SHATTERED " + MessageStyle.RESET + MessageStyle.MUTED
                            + "the orb broke before it could forge new slots.");
                    try { SoulVFX.bookApplyFailSoft(p); } catch (Throwable ignored) {}
                    p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1.0f, 0.7f);
                }
                p.updateInventory();
            }
        }.runTask(plugin);
    }
}
