package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Forces immediate re-evaluation of armor effects on equip/unequip and death.
 */
public class ArmorChangeListener implements Listener {

    private final SoulEnchants plugin;
    private final BerserkTickTask tickTask;

    public ArmorChangeListener(SoulEnchants plugin, BerserkTickTask tickTask) {
        this.plugin = plugin;
        this.tickTask = tickTask;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        int slot = e.getSlot();
        // Armor slots in player inventory are 36-39 (boots, leggings, chest, helmet) or by slot type
        boolean isArmorSlot = (slot >= 36 && slot <= 39)
                || e.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR;
        boolean hotbarSwap = e.getClick() != null && e.getClick().toString().contains("NUMBER");
        if (!isArmorSlot && !hotbarSwap) return;
        final Player p = (Player) e.getWhoClicked();
        new BukkitRunnable() {
            @Override public void run() { tickTask.tickPlayer(p); }
        }.runTask(plugin);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        tickTask.clearPlayer(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        tickTask.clearPlayer(e.getPlayer().getUniqueId());
    }
}
