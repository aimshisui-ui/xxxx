package com.soulenchants.loot;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

/**
 * Removes filtered items from vanilla mob drop tables. Fires LOWEST priority
 * so other plugins still see the original drops if they want them. Skips when
 * there's no killer (environmental death) — filter is a per-player preference.
 *
 * Custom-mob drops are filtered upstream in the death_drop ability factory.
 */
public class LootFilterListener implements Listener {

    private final SoulEnchants plugin;

    public LootFilterListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        LootFilterManager mgr = plugin.getLootFilterManager();
        if (mgr == null) return;

        Iterator<ItemStack> it = e.getDrops().iterator();
        boolean any = false;
        while (it.hasNext()) {
            ItemStack drop = it.next();
            String id = LootFilterManager.filterIdOf(drop);
            if (id != null && mgr.isFiltered(killer.getUniqueId(), id)) {
                it.remove();
                any = true;
            }
        }
        if (any && mgr.messagesEnabled(killer.getUniqueId())) {
            killer.sendMessage(ChatColor.RED + "§l(!)§c Some drops were filtered out — §7/lootfilter§c to edit.");
        }
    }
}
