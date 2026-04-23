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
 *         2. no gem?           → block + throttled msg + return false
 *         3. gem.amount &lt; C?   → block + throttled msg + return false
 *         4. debit C from gem (updates stack NBT in place)
 *         5. return true
 * </pre>
 *
 * The gem IS the source — no ledger fallback. Ledger souls exist to be
 * moved into gems via /souls withdraw; once minted, only gem souls power
 * enchants. A player whose gem runs dry mid-fight has to mint a fresh one
 * between engagements.
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
                            + MessageStyle.BAD + " in inventory — mint one with "
                            + MessageStyle.VALUE + "/souls withdraw <amount>");
            return false;
        }
        ItemStack gem = p.getInventory().getItem(gemSlot);
        long gemAmt = SoulGem.amount(gem);
        if (gemAmt < cost) {
            sendGatedMessage(plugin, p, "no_souls_msg",
                    MessageStyle.BAD + "Gem drained — need "
                            + MessageStyle.VALUE + SoulGem.formatNum(cost)
                            + MessageStyle.BAD + ", gem holds "
                            + MessageStyle.VALUE + SoulGem.formatNum(gemAmt)
                            + MessageStyle.BAD + ". Re-mint with "
                            + MessageStyle.VALUE + "/souls withdraw");
            return false;
        }
        // Debit from gem — no ledger fallback. Gem is the source of truth.
        ItemStack updated = SoulGem.withAmount(gem, gemAmt - cost);
        p.getInventory().setItem(gemSlot, updated);
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
