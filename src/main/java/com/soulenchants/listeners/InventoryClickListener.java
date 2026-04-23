package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.items.ItemUtil;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final SoulEnchants plugin;
    private final Random rng = new Random();
    private final Map<UUID, Integer> dustOverride = new HashMap<>();

    public InventoryClickListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        final Player p = (Player) e.getWhoClicked();

        // Skip our own GUI title (handled separately by EnchantMenuGUI listener)
        if (e.getView() != null && e.getView().getTitle() != null
                && e.getView().getTitle().contains("Enchant Menu")) return;

        final ItemStack cursor = e.getCursor();
        final ItemStack target = e.getCurrentItem();
        if (cursor == null || cursor.getType() == Material.AIR) return;
        if (target == null || target.getType() == Material.AIR) return;

        NBTItem cursorNbt;
        try { cursorNbt = new NBTItem(cursor); }
        catch (Exception ex) { return; }

        // ── BOOK → apply ──────────────────────────────────────────────────
        if (cursorNbt.hasKey(ItemUtil.NBT_BOOK_ENCHANT)) {
            final String id = cursorNbt.getString(ItemUtil.NBT_BOOK_ENCHANT);
            final int level = cursorNbt.getInteger(ItemUtil.NBT_BOOK_LEVEL);
            final CustomEnchant enchant = EnchantRegistry.get(id);
            if (enchant == null) return;

            e.setCancelled(true);
            if (!enchant.getSlot().matches(target.getType())) {
                p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                        + com.soulenchants.style.MessageStyle.BAD
                        + "That enchant cannot be applied to this item.");
                return;
            }
            int existing = ItemUtil.getLevel(target, id);
            if (existing >= enchant.getMaxLevel()) {
                p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                        + com.soulenchants.style.MessageStyle.BAD
                        + "This item already has " + com.soulenchants.style.MessageStyle.tier(enchant.getTier())
                        + enchant.getDisplayName() + com.soulenchants.style.MessageStyle.BAD + " at max level.");
                return;
            }
            // Per-item slot cap — default 9, raised by Weapon/Armor Orbs up to 14.
            // Only blocks NEW enchants; upgrades to an existing enchant always pass.
            // Counts visible enchants only; hidden internal flags (mythic_held
            // etc.) don't consume slots.
            int visibleCount = ItemUtil.countVisibleEnchants(target);
            int slotCap = ItemUtil.getMaxSlots(target);
            if (existing == 0 && visibleCount >= slotCap) {
                p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                        + com.soulenchants.style.MessageStyle.BAD
                        + "Item at " + com.soulenchants.style.MessageStyle.VALUE
                        + slotCap + "-enchant limit"
                        + com.soulenchants.style.MessageStyle.BAD + ". Apply a "
                        + com.soulenchants.style.MessageStyle.TIER_SOUL + "Slot Orb"
                        + com.soulenchants.style.MessageStyle.BAD + " or remove one with a "
                        + com.soulenchants.style.MessageStyle.FRAME + "Black Scroll"
                        + com.soulenchants.style.MessageStyle.BAD + " first.");
                return;
            }

            // Read rates from book NBT — each book has its own random success/destroy rolls.
            int bookSuccess = cursorNbt.hasKey(ItemUtil.NBT_BOOK_SUCCESS)
                    ? cursorNbt.getInteger(ItemUtil.NBT_BOOK_SUCCESS) : 50;
            int bookDestroy = cursorNbt.hasKey(ItemUtil.NBT_BOOK_DESTROY)
                    ? cursorNbt.getInteger(ItemUtil.NBT_BOOK_DESTROY) : 25;

            // Magic Dust override applies to success rate only.
            int successRate = dustOverride.getOrDefault(p.getUniqueId(), bookSuccess);
            dustOverride.remove(p.getUniqueId());

            final boolean success = rng.nextInt(100) < successRate;
            // Independent roll: only happens if the apply failed.
            final boolean destroy = !success && rng.nextInt(100) < bookDestroy;

            final Inventory clickedInv = e.getClickedInventory();
            final int slot = e.getSlot();
            final int targetLevel = Math.max(level, existing + 1);
            final ItemStack originalTarget = target.clone();

            // Schedule 1 tick later so cancel doesn't revert our changes
            new BukkitRunnable() {
                @Override public void run() {
                    p.setItemOnCursor(new ItemStack(Material.AIR));
                    if (success) {
                        ItemStack updated = ItemUtil.addEnchant(originalTarget, id, targetLevel);
                        clickedInv.setItem(slot, updated);
                        p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                                + com.soulenchants.style.MessageStyle.GOOD
                                + com.soulenchants.style.MessageStyle.BOLD + "✦ APPLIED "
                                + com.soulenchants.style.MessageStyle.RESET + enchant.formatLore(targetLevel));
                        // Nordic-ported particle burst + celebratory sound
                        com.soulenchants.style.SoulVFX.bookApplySuccess(p);
                        if (plugin.getQuestManager() != null) {
                            plugin.getQuestManager().onEvent(p,
                                    com.soulenchants.quests.QuestEvent.bookApplied(updated));
                        }
                    } else if (destroy) {
                        NBTItem itemNbt = new NBTItem(originalTarget);
                        if (itemNbt.hasKey(ItemUtil.NBT_WHITE_SCROLL) && itemNbt.getBoolean(ItemUtil.NBT_WHITE_SCROLL)) {
                            itemNbt.removeKey(ItemUtil.NBT_WHITE_SCROLL);
                            clickedInv.setItem(slot, itemNbt.getItem());
                            p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                                    + com.soulenchants.style.MessageStyle.TIER_EPIC
                                    + com.soulenchants.style.MessageStyle.BOLD + "✦ SAVED "
                                    + com.soulenchants.style.MessageStyle.RESET
                                    + com.soulenchants.style.MessageStyle.MUTED
                                    + "White Scroll absorbed the destruction.");
                            p.playSound(p.getLocation(), org.bukkit.Sound.ORB_PICKUP, 0.9f, 1.4f);
                        } else {
                            clickedInv.setItem(slot, new ItemStack(Material.AIR));
                            p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                                    + com.soulenchants.style.MessageStyle.BAD
                                    + com.soulenchants.style.MessageStyle.BOLD + "✗ DESTROYED "
                                    + com.soulenchants.style.MessageStyle.RESET
                                    + com.soulenchants.style.MessageStyle.BAD + "Enchant failed catastrophically.");
                            // Loud, unmistakable — gear-lost should feel felt.
                            com.soulenchants.style.SoulVFX.bookApplyDestroy(p);
                        }
                    } else {
                        p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                                + com.soulenchants.style.MessageStyle.MUTED
                                + "Enchant failed, but your item is intact.");
                        com.soulenchants.style.SoulVFX.bookApplyFailSoft(p);
                    }
                    p.updateInventory();
                }
            }.runTask(plugin);
            return;
        }

        // ── DUST → activate (shift-click) ─────────────────────────────────
        if (cursorNbt.hasKey(ItemUtil.NBT_DUST_RATE)) {
            if (e.getClick() != ClickType.SHIFT_RIGHT && e.getClick() != ClickType.SHIFT_LEFT) return;
            final int rate = cursorNbt.getInteger(ItemUtil.NBT_DUST_RATE);
            e.setCancelled(true);
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
                    dustOverride.put(p.getUniqueId(), rate);
                    p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                            + com.soulenchants.style.MessageStyle.TIER_EPIC
                            + com.soulenchants.style.MessageStyle.BOLD + "✦ ARMED  "
                            + com.soulenchants.style.MessageStyle.RESET
                            + com.soulenchants.style.MessageStyle.MUTED + "next book will succeed at "
                            + com.soulenchants.style.MessageStyle.VALUE + rate + "%"
                            + com.soulenchants.style.MessageStyle.MUTED + ".");
                    p.updateInventory();
                }
            }.runTask(plugin);
            return;
        }

        // ── BLACK SCROLL → extract ────────────────────────────────────────
        if (cursorNbt.hasKey(ItemUtil.NBT_BLACK_SCROLL)) {
            Map<String, Integer> enchants = ItemUtil.getEnchants(target);
            if (enchants.isEmpty()) return;
            e.setCancelled(true);
            final String id = enchants.keySet().iterator().next();
            final int lvl = enchants.get(id);
            final CustomEnchant ce = EnchantRegistry.get(id);
            if (ce == null) return;
            final Inventory clickedInv = e.getClickedInventory();
            final int slot = e.getSlot();
            final ItemStack originalTarget = target.clone();
            final ItemStack cursorClone = cursor.clone();
            new BukkitRunnable() {
                @Override public void run() {
                    ItemStack cleared = ItemUtil.removeEnchant(originalTarget, id);
                    clickedInv.setItem(slot, cleared);
                    int amt = cursorClone.getAmount();
                    if (amt > 1) {
                        cursorClone.setAmount(amt - 1);
                        p.setItemOnCursor(cursorClone);
                    } else {
                        p.setItemOnCursor(new ItemStack(Material.AIR));
                    }
                    p.getInventory().addItem(com.soulenchants.items.ItemFactories.book(ce, lvl)).values()
                            .forEach(over -> p.getWorld().dropItemNaturally(p.getLocation(), over));
                    p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                            + com.soulenchants.style.MessageStyle.FRAME
                            + com.soulenchants.style.MessageStyle.BOLD + "✦ EXTRACTED "
                            + com.soulenchants.style.MessageStyle.RESET + ce.formatLore(lvl)
                            + com.soulenchants.style.MessageStyle.MUTED + " → fresh book.");
                    p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_PICKUP, 0.9f, 1.2f);
                    p.updateInventory();
                }
            }.runTask(plugin);
            return;
        }

        // ── WHITE SCROLL → bind ───────────────────────────────────────────
        if (cursorNbt.hasKey(ItemUtil.NBT_WHITE_SCROLL)) {
            e.setCancelled(true);
            final Inventory clickedInv = e.getClickedInventory();
            final int slot = e.getSlot();
            final ItemStack originalTarget = target.clone();
            final ItemStack cursorClone = cursor.clone();
            new BukkitRunnable() {
                @Override public void run() {
                    NBTItem targetNbt = new NBTItem(originalTarget);
                    if (targetNbt.hasKey(ItemUtil.NBT_WHITE_SCROLL) && targetNbt.getBoolean(ItemUtil.NBT_WHITE_SCROLL)) {
                        p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                                + com.soulenchants.style.MessageStyle.BAD
                                + "This item already has a White Scroll bound.");
                        p.updateInventory();
                        return;
                    }
                    targetNbt.setBoolean(ItemUtil.NBT_WHITE_SCROLL, true);
                    clickedInv.setItem(slot, targetNbt.getItem());
                    int amt = cursorClone.getAmount();
                    if (amt > 1) {
                        cursorClone.setAmount(amt - 1);
                        p.setItemOnCursor(cursorClone);
                    } else {
                        p.setItemOnCursor(new ItemStack(Material.AIR));
                    }
                    p.sendMessage(com.soulenchants.style.MessageStyle.PREFIX
                            + com.soulenchants.style.MessageStyle.VALUE
                            + com.soulenchants.style.MessageStyle.BOLD + "✦ BOUND  "
                            + com.soulenchants.style.MessageStyle.RESET
                            + com.soulenchants.style.MessageStyle.MUTED
                            + "White Scroll guards your next enchant attempt.");
                    p.playSound(p.getLocation(), org.bukkit.Sound.LEVEL_UP, 0.9f, 1.9f);
                    p.updateInventory();
                }
            }.runTask(plugin);
        }
    }
}
