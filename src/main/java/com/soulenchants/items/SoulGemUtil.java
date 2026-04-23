package com.soulenchants.items;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.MessageStyle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * The "soul spend" pipeline for soul-tier enchants.
 *
 * <pre>
 *   player proc soul-enchant X with cost C
 *     → SoulGemUtil.chargeSoulCost(plugin, player, C)
 *         1. find largest soul gem in inventory
 *         2. no gem?                        → block + throttled msg + return false
 *         3. gem.amount + ledger &lt; C?       → block + throttled msg + return false
 *         4. debit min(gem.amount, C) from gem (updates stack NBT in place)
 *         5. debit remainder (if any) from ledger via SoulManager.take()
 *         6. return true
 * </pre>
 *
 * Rationale: the gem is a license AND a battery. Keeping ledger as fallback
 * means a player who forgot to refill their gem mid-fight still gets one
 * emergency proc, which feels less punishing than "you forgot one item, no
 * procs for you".
 */
public final class SoulGemUtil {

    private SoulGemUtil() {}

    /**
     * Scan the player's inventory for the highest-balance Soul Gem.
     * Returns the slot index, or -1 if none found.
     */
    public static int findGemSlot(Player p) {
        PlayerInventory inv = p.getInventory();
        int best = -1;
        long bestAmt = -1;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (!SoulGem.isGem(it)) continue;
            long amt = SoulGem.amount(it);
            if (amt > bestAmt) { bestAmt = amt; best = i; }
        }
        return best;
    }

    /** Does this player currently carry any Soul Gem (regardless of balance)? */
    public static boolean hasGem(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (SoulGem.isGem(it)) return true;
        }
        return false;
    }

    /** Total souls across every gem in the inventory. */
    public static long totalGemBalance(Player p) {
        long total = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (SoulGem.isGem(it)) total += SoulGem.amount(it);
        }
        return total;
    }

    /**
     * Main entry point — charge a soul-enchant cost against the player. See
     * class javadoc for the debit order.
     *
     * @return true if the full cost was paid; false means the proc should
     *         cancel. On false, a throttled message is sent to the player.
     */
    public static boolean chargeSoulCost(SoulEnchants plugin, Player p, long cost) {
        if (cost <= 0) return true;
        int gemSlot = findGemSlot(p);
        if (gemSlot < 0) {
            sendGatedMessage(plugin, p, "no_gem_msg",
                    MessageStyle.BAD + "No " + MessageStyle.TIER_SOUL + "Soul Gem"
                            + MessageStyle.BAD + " in inventory — soul enchants require one.");
            return false;
        }
        ItemStack gem = p.getInventory().getItem(gemSlot);
        long gemAmt = SoulGem.amount(gem);
        long ledger = plugin.getSoulManager().get(p);
        if (gemAmt + ledger < cost) {
            sendGatedMessage(plugin, p, "no_souls_msg",
                    MessageStyle.BAD + "Not enough souls — need "
                            + MessageStyle.VALUE + SoulGem.formatNum(cost)
                            + MessageStyle.BAD + ", have "
                            + MessageStyle.VALUE + SoulGem.formatNum(gemAmt + ledger)
                            + MessageStyle.MUTED + " (" + SoulGem.formatNum(gemAmt)
                            + " gem + " + SoulGem.formatNum(ledger) + " ledger).");
            return false;
        }
        // Pay gem first
        long fromGem = Math.min(gemAmt, cost);
        if (fromGem > 0) {
            ItemStack updated = SoulGem.withAmount(gem, gemAmt - fromGem);
            p.getInventory().setItem(gemSlot, updated);
        }
        // Fallback to ledger
        long remainder = cost - fromGem;
        if (remainder > 0) {
            plugin.getSoulManager().take(p, remainder);
        }
        return true;
    }

    /**
     * Move souls between a gem and the ledger.
     *
     * @param direction +1 to mint a gem from the ledger, -1 to redeem a gem
     *                 back into the ledger. Returns true on success.
     */
    public static boolean withdraw(SoulEnchants plugin, Player p, long amount) {
        if (amount <= 0) return false;
        if (plugin.getSoulManager().get(p) < amount) return false;
        plugin.getSoulManager().take(p, amount);
        ItemStack gem = SoulGem.create(amount);
        p.getInventory().addItem(gem).values().forEach(
                o -> p.getWorld().dropItemNaturally(p.getLocation(), o));
        return true;
    }

    /** Deposit a gem back into the ledger, emptying the item. */
    public static long deposit(SoulEnchants plugin, Player p, ItemStack gem) {
        long amt = SoulGem.amount(gem);
        if (amt > 0) plugin.getSoulManager().add(p, amt);
        return amt;
    }

    /** Merge two gems — sum amounts, return a fresh gem with combined total. */
    public static ItemStack merge(ItemStack a, ItemStack b) {
        if (!SoulGem.isGem(a) || !SoulGem.isGem(b)) return null;
        long total = SoulGem.amount(a) + SoulGem.amount(b);
        return SoulGem.create(total);
    }

    /** Send a throttled message so proc failures don't spam chat during combat. */
    private static void sendGatedMessage(SoulEnchants plugin, Player p, String key, String body) {
        if (plugin.getCooldownManager().isReady(key, p.getUniqueId())) {
            plugin.getCooldownManager().set(key, p.getUniqueId(), 5_000L);
            p.sendMessage(MessageStyle.PREFIX + body);
            try { p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_BREAK, 0.6f, 0.4f); }
            catch (Throwable ignored) {}
        }
    }
}
