package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LootFilterCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public LootFilterCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length >= 1 && args[0].equalsIgnoreCase("togglemessage")) {
            plugin.getLootFilterManager().toggleMessages(p.getUniqueId());
            boolean now = plugin.getLootFilterManager().messagesEnabled(p.getUniqueId());
            p.sendMessage(ChatColor.GOLD + "✦ Loot Filter messages " + (now ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
            return true;
        }
        plugin.getLootFilterGUI().openMain(p);
        return true;
    }
}
