package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SoulsCommand implements CommandExecutor {

    private final SoulEnchants plugin;
    public SoulsCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must specify a player.");
                return true;
            }
            Player p = (Player) sender;
            sender.sendMessage("§5✦ §dSouls: §f" + plugin.getSoulManager().get(p));
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
            if (!sender.hasPermission("soulenchants.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§c/souls " + sub + " <player> <amount>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            long amt;
            try { amt = Long.parseLong(args[2]); }
            catch (NumberFormatException e) { sender.sendMessage("§cInvalid amount."); return true; }
            switch (sub) {
                case "give": plugin.getSoulManager().add(target, amt); break;
                case "take": plugin.getSoulManager().take(target, amt); break;
                case "set":  plugin.getSoulManager().set(target, amt); break;
            }
            sender.sendMessage("§a✦ Updated " + target.getName() + "'s Souls. New balance: §f"
                    + plugin.getSoulManager().get(target));
            return true;
        }
        sender.sendMessage("§7Usage: /souls [give|take|set] [player] [amount]");
        return true;
    }
}
