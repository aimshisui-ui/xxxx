package com.soulenchants.rifts;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the right-click-the-spawner-on-the-ground interaction, plus
 * rift-world death (keep-inventory) and disconnect cleanup.
 */
public class VoidRiftListener implements Listener {

    private final SoulEnchants plugin;
    private final VoidRiftManager rifts;

    public VoidRiftListener(SoulEnchants plugin, VoidRiftManager rifts) {
        this.plugin = plugin;
        this.rifts = rifts;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getItemInHand();
        if (!VoidRiftItem.isSpawner(hand)) return;
        // ALWAYS suppress the vanilla Eye-of-Ender throw behavior — covers both
        // RIGHT_CLICK_AIR and RIGHT_CLICK_BLOCK regardless of what we do next.
        e.setCancelled(true);
        try { e.setUseItemInHand(org.bukkit.event.Event.Result.DENY); } catch (Throwable ignored) {}

        // Only RIGHT_CLICK_BLOCK can open the rift
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            p.sendMessage(ChatColor.RED + "✦ The Void Rift Spawner must be pressed against the ground.");
            return;
        }
        if (e.getClickedBlock() == null) return;

        Block clicked = e.getClickedBlock();
        Block portalSlot = clicked.getRelative(0, 1, 0);
        if (portalSlot.getType() != Material.AIR) {
            p.sendMessage(ChatColor.RED + "✦ Need a clear block of air above the surface.");
            return;
        }
        if (!clicked.getType().isSolid()) {
            p.sendMessage(ChatColor.RED + "✦ Can only be opened on solid ground.");
            return;
        }

        boolean ok = rifts.open(portalSlot.getLocation(), p);
        if (!ok) return;

        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else p.setItemInHand(null);
    }

    /** Rift world: don't drop items on death — players keep their gear. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (p.getWorld().getName().equals(RiftWorld.NAME)) {
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }

    /**
     * Player died inside the rift — they're expelled to the main world spawn
     * (gear preserved). If they were the last participant alive, the rift fails.
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!rifts.getParticipants().contains(p.getUniqueId())) return;
        if (rifts.getState() != VoidRiftManager.State.ACTIVE) return;
        // Send them home, not back into the meat grinder
        e.setRespawnLocation(org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation());
        p.sendMessage(ChatColor.RED + "✦ You fell within the rift. You are returned to the surface.");
        p.sendMessage(ChatColor.GRAY + "  Your gear remains with you.");
        // notifyParticipantExpelled handles the "all down → fail" check
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("SoulEnchants"),
                new Runnable() { @Override public void run() {
                    rifts.notifyParticipantExpelled(p.getUniqueId());
                } }, 1L);
    }

    /** If a participant disconnects, clean their UUID from the participant set. */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        rifts.getParticipants().remove(e.getPlayer().getUniqueId());
    }

    /**
     * The rift uses ENDER_PORTAL as a visual block. Stepping on it would
     * normally teleport the player to The End — block that. Our proximity
     * tick handles the actual rift TP via {@link VoidRiftManager#sendToRift}.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerPortal(PlayerPortalEvent e) {
        if (isRiftPortalLoc(e.getFrom())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityPortal(EntityPortalEvent e) {
        if (isRiftPortalLoc(e.getFrom())) e.setCancelled(true);
    }

    private boolean isRiftPortalLoc(org.bukkit.Location at) {
        if (at == null) return false;
        org.bukkit.Location pl = rifts.getPortalLoc();
        if (pl == null) return false;
        return at.getWorld() == pl.getWorld()
                && at.getBlockX() == pl.getBlockX()
                && at.getBlockY() == pl.getBlockY()
                && at.getBlockZ() == pl.getBlockZ();
    }
}
