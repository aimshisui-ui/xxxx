package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;


public class GrindingListener implements Listener {

    private final SoulEnchants plugin;
    public GrindingListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getItemInHand();
        if (tool == null) return;

        // Avoid recursion guard
        if (p.hasMetadata("se_grinding")) return;

        Block source = e.getBlock();

        int explosive = ItemUtil.getLevel(tool, "explosive");
        int treefell  = ItemUtil.getLevel(tool, "treefeller");

        if (explosive > 0) explode3x3(p, source);
        else if (treefell > 0 && isLog(source.getType())) treefell(p, source);
    }

    private void explode3x3(Player p, Block center) {
        p.setMetadata("se_grinding", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        try {
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block b = center.getRelative(dx, dy, dz);
                        if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK) continue;
                        breakAsPlayer(p, b);
                    }
        } finally {
            p.removeMetadata("se_grinding", plugin);
        }
    }

    private void treefell(Player p, Block start) {
        p.setMetadata("se_grinding", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        try {
            for (int y = 1; y < 30; y++) {
                Block above = start.getRelative(0, y, 0);
                if (!isLog(above.getType())) break;
                breakAsPlayer(p, above);
            }
        } finally {
            p.removeMetadata("se_grinding", plugin);
        }
    }

    private boolean isLog(Material m) {
        return m == Material.LOG || m == Material.LOG_2;
    }

    /** 1.8 manual block break with telepathy/autosmelt hooks via standard BlockBreakEvent flow. */
    private void breakAsPlayer(Player p, Block b) {
        if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK) return;
        ItemStack tool = p.getItemInHand();
        // Trigger BlockBreakEvent so other listeners (autosmelt/telepathy) run normally
        BlockBreakEvent event = new BlockBreakEvent(b, p);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        Collection<ItemStack> drops = b.getDrops(tool);
        boolean autosmelt = ItemUtil.getLevel(tool, "autosmelt") > 0;
        boolean telepathy = ItemUtil.getLevel(tool, "telepathy") > 0;
        if (telepathy) {
            for (ItemStack drop : drops) {
                p.getInventory().addItem(drop).values()
                        .forEach(over -> p.getWorld().dropItemNaturally(p.getLocation(), over));
            }
        } else {
            for (ItemStack drop : drops) {
                p.getWorld().dropItemNaturally(b.getLocation(), drop);
            }
        }
        b.setType(Material.AIR);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDeathXp(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        // XP boost / vampire on held weapon
        ItemStack tool = killer.getItemInHand();
        if (tool != null) {
            int xp = ItemUtil.getLevel(tool, "xpboost");
            if (xp > 0) e.setDroppedExp((int) (e.getDroppedExp() * (1.0 + 0.5 * xp)));

            int vampire = ItemUtil.getLevel(tool, "vampire");
            if (vampire > 0) e.setDroppedExp((int) (e.getDroppedExp() * (1.0 + 0.6 * vampire)));
        }
    }
}
