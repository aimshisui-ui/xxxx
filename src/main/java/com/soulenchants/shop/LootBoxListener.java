package com.soulenchants.shop;

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
 * item(s) per {@link LootBox#roll}, and either inserts into inventory or
 * drops at feet if full.
 */
public class LootBoxListener implements Listener {

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

        // Roll and deliver
        List<ItemStack> rolled = LootBox.roll(kind);
        p.sendMessage(kind.color + "✦ " + ChatColor.BOLD + kind.label + " Loot Box" + ChatColor.RESET
                + ChatColor.GRAY + " opened. Contents:");
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1f, 1.2f);
        for (ItemStack s : rolled) {
            String name = (s.getItemMeta() != null && s.getItemMeta().hasDisplayName())
                    ? s.getItemMeta().getDisplayName() : s.getType().name();
            p.sendMessage(ChatColor.GRAY + "  ▸ " + ChatColor.WHITE + s.getAmount() + "× " + name);
            java.util.Map<Integer, ItemStack> leftover = p.getInventory().addItem(s);
            for (ItemStack lo : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), lo);
        }
    }
}
