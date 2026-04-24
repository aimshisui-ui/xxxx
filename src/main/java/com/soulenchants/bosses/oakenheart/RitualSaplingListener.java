package com.soulenchants.bosses.oakenheart;

import com.soulenchants.SoulEnchants;
import com.soulenchants.loot.BossLootItems;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Consumes a Ritual Sapling ItemStack and summons Oakenheart where the
 * player right-clicked. Rules:
 *   - Right-click only
 *   - Must target a grass or dirt block (not air / water / etc.)
 *   - Must be in the main world (not rift / modock / pvp arena worlds)
 *   - No active Oakenheart already
 *   - Consumes one sapling on success
 */
public final class RitualSaplingListener implements Listener {

    private final SoulEnchants plugin;

    public RitualSaplingListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack inHand = e.getItem();
        if (inHand == null) return;
        String id = BossLootItems.getLootId(inHand);
        if (!"ritual_sapling".equals(id)) return;

        Player p = e.getPlayer();
        Block target = e.getClickedBlock();
        if (target == null) return;
        Material m = target.getType();
        if (m != Material.GRASS && m != Material.DIRT && m != Material.MYCEL && m != Material.SOUL_SAND) {
            p.sendMessage(ChatColor.DARK_GREEN + "» " + ChatColor.GRAY
                    + "The Ritual Sapling needs grass, dirt, or mycelium to take root.");
            e.setCancelled(true);
            return;
        }
        // Block in non-main worlds (rift, pvp, etc.) — tune the allow-list here.
        String w = target.getWorld().getName().toLowerCase();
        if (w.contains("rift") || w.contains("arena")) {
            p.sendMessage(ChatColor.DARK_GREEN + "» " + ChatColor.GRAY
                    + "Oakenheart will not answer in this world.");
            e.setCancelled(true);
            return;
        }

        if (plugin.getOakenheartManager() == null) return;
        if (plugin.getOakenheartManager().hasActive()) {
            p.sendMessage(ChatColor.DARK_GREEN + "» " + ChatColor.GRAY
                    + "Another Oakenheart already walks the world.");
            e.setCancelled(true);
            return;
        }

        // Consume one sapling + summon
        Location spawn = target.getLocation().clone().add(0, 1, 0);
        plugin.getOakenheartManager().summon(spawn);
        inHand.setAmount(inHand.getAmount() - 1);
        if (inHand.getAmount() <= 0) p.getInventory().setItemInHand(null);
        e.setCancelled(true);
    }
}
