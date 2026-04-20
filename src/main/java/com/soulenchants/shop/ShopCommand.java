package com.soulenchants.shop;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final SoulEnchants plugin;
    public ShopCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("refresh")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
            plugin.getShopFeatured().forceRoll();
            sender.sendMessage(ChatColor.GREEN + "✦ Featured rotation rolled. New picks:");
            for (String id : plugin.getShopFeatured().getFeaturedIds()) {
                ShopItem it = ShopCatalog.byId(id);
                sender.sendMessage("§7  ▸ §f" + (it != null ? (it.display.getItemMeta() != null ? it.display.getItemMeta().getDisplayName() : it.id) : id));
            }
            return true;
        }
        if (!(sender instanceof Player)) { sender.sendMessage("§cMust be a player."); return true; }
        plugin.getShopGUI().openHub((Player) sender);
        return true;
    }
}
