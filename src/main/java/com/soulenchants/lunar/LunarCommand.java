package com.soulenchants.lunar;

import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /lunar status — diagnostic for the Moonsworth API bridge. Prints whether
 * the Lunar plugin is installed, which plugin name was matched, and whether
 * the cooldown/waypoint handles were resolved.
 *
 * /lunar test — sends a 10s test cooldown to the executing player so you
 * can verify the ring is showing up on Lunar Client.
 */
public final class LunarCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("soulenchants.admin")) {
            Chat.err(sender, "You need soulenchants.admin.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            Chat.banner(sender, "Lunar Bridge Status");
            printDetectedPlugin(sender);
            sender.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.DIAMOND + "  "
                    + MessageStyle.MUTED + "API handle: " + MessageStyle.VALUE
                    + (LunarBridge.isAvailable() ? "resolved" : "not resolved"));
            Chat.rule(sender);
            if (!LunarBridge.isAvailable()) {
                Chat.info(sender, "Install "
                        + MessageStyle.VALUE + "LunarClient-API"
                        + MessageStyle.MUTED + " (Moonsworth) and restart. Download: "
                        + MessageStyle.FRAME + "github.com/LunarClient/BukkitAPI/releases");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("test")) {
            if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
            Player p = (Player) sender;
            if (!LunarBridge.isAvailable()) {
                Chat.err(p, "Lunar bridge not resolved — see /lunar status.");
                return true;
            }
            LunarBridge.sendCooldown(p, "SE Test", 10_000L, Material.DIAMOND_SWORD);
            Chat.good(p, "Sent a 10-second test cooldown. "
                    + MessageStyle.MUTED + "If no ring appears, you may not be on Lunar Client.");
            return true;
        }
        Chat.info(sender, "Usage: /lunar status | /lunar test");
        return true;
    }

    private static void printDetectedPlugin(CommandSender to) {
        String[] names = { "LunarClient-API", "LunarBukkitAPI", "LunarClientAPI", "LunarAPI" };
        for (String n : names) {
            org.bukkit.plugin.Plugin pl = Bukkit.getPluginManager().getPlugin(n);
            if (pl != null) {
                to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.GOOD + "✓ "
                        + MessageStyle.MUTED + "Plugin found: " + MessageStyle.VALUE + n
                        + MessageStyle.FRAME + " v" + MessageStyle.VALUE + pl.getDescription().getVersion());
                return;
            }
        }
        to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.BAD + "✗ "
                + MessageStyle.MUTED + "No Lunar plugin found in plugins/");
    }
}
