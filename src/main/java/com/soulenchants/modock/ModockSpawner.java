package com.soulenchants.modock;

import com.soulenchants.SoulEnchants;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Modock Spawner — consumable item that triggers the 3-phase Modock encounter.
 * Right-click anywhere → TPs the holder to phase 1 + spawns Modock + begins fight.
 *
 * Item is the same archetype as Void Rift Spawner (one-shot consumable, NBT
 * tagged) so the existing inventory pipeline handles drops/trades cleanly.
 */
public final class ModockSpawner implements Listener {

    public static final String NBT_TAG = "se_modock_spawner";

    private final SoulEnchants plugin;

    public ModockSpawner(SoulEnchants plugin) { this.plugin = plugin; }

    public static ItemStack create() {
        ItemStack it = new ItemStack(Material.PRISMARINE_SHARD, 1);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "✦ Modock's Tide ✦");
        m.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + "Sigil of Atlantis' lost king",
                "",
                ChatColor.GRAY + "A sliver of the deep that still",
                ChatColor.GRAY + "remembers his name. Hold it",
                ChatColor.GRAY + "and the sea opens beneath you.",
                "",
                ChatColor.AQUA + "» Three phases. Three worlds.",
                ChatColor.AQUA + "» Lightning. Lifesteal. Drowning.",
                ChatColor.AQUA + "» 1v1 finale.",
                "",
                ChatColor.RED + "⚠ " + ChatColor.GRAY + "End-game content",
                ChatColor.RED + "⚠ " + ChatColor.GRAY + "Bring " + ChatColor.WHITE + "everything",
                "",
                ChatColor.AQUA + "" + ChatColor.ITALIC + "Right-click to summon"
        ));
        it.setItemMeta(m);
        try { NBTItem nbt = new NBTItem(it); nbt.setBoolean(NBT_TAG, true); it = nbt.getItem(); }
        catch (Throwable ignored) {}
        return it;
    }

    public static boolean isSpawner(ItemStack it) {
        if (it == null || it.getType() == Material.AIR || it.getAmount() <= 0) return false;
        try { return new NBTItem(it).getBoolean(NBT_TAG); }
        catch (Throwable t) { return false; }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getItemInHand();
        if (!isSpawner(hand)) return;
        e.setCancelled(true);
        boolean ok = plugin.getModockManager().begin(p);
        if (ok) {
            // Consume one
            if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
            else p.setItemInHand(null);
        }
    }
}
