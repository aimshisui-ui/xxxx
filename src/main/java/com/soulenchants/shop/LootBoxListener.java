package com.soulenchants.shop;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Right-click-to-open loot boxes. Consumes 1 from the stack, rolls the
 * item(s) per {@link LootBox#roll}, and hands them to {@link LootBoxRevealGUI}
 * for the spin-and-reveal animation. Falls back to instant-deliver if the
 * GUI rejects (e.g. another reveal is already in progress for this player).
 */
public class LootBoxListener implements Listener {

    private final SoulEnchants plugin;

    public LootBoxListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getItemInHand();
        // NBT-API rejects air/null/amount=0, so short-circuit BEFORE calling kindOf
        if (hand == null || hand.getType() == org.bukkit.Material.AIR || hand.getAmount() <= 0) return;
        LootBox.Kind kind = LootBox.kindOf(hand);
        if (kind == null) return;

        e.setCancelled(true);

        // Consume one
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else p.setItemInHand(null);

        List<ItemStack> rolled = LootBox.roll(kind);

        // Hand off to the reveal animation. If it can't open (another reveal
        // already running for this player), refund the box rather than
        // silently eating the open — fairer than instant-deliver because the
        // player explicitly wants the animation.
        boolean opened = plugin.getLootBoxRevealGUI() != null
                && plugin.getLootBoxRevealGUI().open(p, kind, rolled);
        if (!opened) {
            // Refund: give the box back, do not consume the roll
            ItemStack refund = LootBox.item(kind);
            java.util.Map<Integer, ItemStack> leftover = p.getInventory().addItem(refund);
            for (ItemStack lo : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), lo);
            p.sendMessage(ChatColor.GRAY + "✦ Finish opening your current loot box first.");
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 0.5f, 0.7f);
        }
    }
}
