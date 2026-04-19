package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.enchants.impl.AutoSmeltEnchant;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.items.ItemUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BlockBreakListener implements Listener {

    private final SoulEnchants plugin;
    private final Random rng = new Random();

    public BlockBreakListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getItemInHand();
        if (tool == null) return;

        Block block = e.getBlock();

        boolean autosmelt = ItemUtil.getLevel(tool, "autosmelt") > 0;
        boolean telepathy = ItemUtil.getLevel(tool, "telepathy") > 0;

        // Book drop chance from ores
        String matKey = block.getType().name().toLowerCase();
        double dropChance = plugin.getConfig().getDouble("drops." + matKey, 0.0);
        if (dropChance > 0 && rng.nextDouble() < dropChance) {
            ItemStack book = ItemFactories.book(EnchantRegistry.randomWeighted(rng), 1);
            p.getInventory().addItem(book).values()
                    .forEach(over -> p.getWorld().dropItemNaturally(p.getLocation(), over));
            p.sendMessage("§5✦ §dYou found an Enchant Book!");
        }

        if (!autosmelt && !telepathy) return;

        // Compute effective drops
        Collection<ItemStack> drops = block.getDrops(tool);
        List<ItemStack> finalDrops = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (autosmelt && AutoSmeltEnchant.canSmelt(drop.getType())) {
                ItemStack smelted = AutoSmeltEnchant.smelt(drop.getType());
                if (smelted != null) {
                    smelted.setAmount(drop.getAmount());
                    finalDrops.add(smelted);
                    continue;
                }
            }
            // For autosmelt of mined block as ore directly:
            if (autosmelt && AutoSmeltEnchant.canSmelt(block.getType()) && drop.getType() == block.getType()) {
                ItemStack smelted = AutoSmeltEnchant.smelt(block.getType());
                if (smelted != null) {
                    smelted.setAmount(drop.getAmount());
                    finalDrops.add(smelted);
                    continue;
                }
            }
            finalDrops.add(drop);
        }

        if (telepathy) {
            // Cancel default drops, send to inventory
            block.setType(Material.AIR);
            e.setCancelled(true);
            // Manually award exp
            int exp = e.getExpToDrop();
            if (exp > 0) p.giveExp(exp);
            // Damage tool by 1 (rough approximation, vanilla durability handling)
            for (ItemStack drop : finalDrops) {
                Map<Integer, ItemStack> over = p.getInventory().addItem(drop);
                for (ItemStack leftover : over.values())
                    p.getWorld().dropItemNaturally(p.getLocation(), leftover);
            }
        } else if (autosmelt) {
            // Default drops are vanilla, we replace them by canceling and dropping smelted
            e.setCancelled(true);
            block.setType(Material.AIR);
            int exp = e.getExpToDrop();
            if (exp > 0) {
                org.bukkit.entity.ExperienceOrb orb = p.getWorld().spawn(
                        block.getLocation().add(0.5, 0.5, 0.5),
                        org.bukkit.entity.ExperienceOrb.class);
                orb.setExperience(exp);
            }
            for (ItemStack drop : finalDrops)
                p.getWorld().dropItemNaturally(block.getLocation(), drop);
        }
    }
}
