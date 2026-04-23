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
            // Which backend resolved? apollo > legacy > none
            String backend = LunarBridge.backend();
            String backendLabel;
            if ("apollo".equals(backend))      backendLabel = MessageStyle.GOOD + "Apollo " + MessageStyle.FRAME + "(modern)";
            else if ("legacy".equals(backend)) backendLabel = MessageStyle.TIER_LEGENDARY + "legacy LunarClient-API";
            else                                backendLabel = MessageStyle.BAD + "none";
            sender.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.DIAMOND + "  "
                    + MessageStyle.MUTED + "Backend:    " + backendLabel);
            sender.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.DIAMOND + "  "
                    + MessageStyle.MUTED + "API handle: " + MessageStyle.VALUE
                    + (LunarBridge.isAvailable() ? "resolved" : "not resolved"));
            Chat.rule(sender);
            if (!LunarBridge.isAvailable()) {
                Chat.info(sender, "Install " + MessageStyle.VALUE + "Apollo"
                        + MessageStyle.MUTED + " (Lunar's current server API) and restart.");
                Chat.info(sender, "Download: " + MessageStyle.FRAME + "lunarclient.dev/apollo/downloads");
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
        if (args[0].equalsIgnoreCase("rpc")) {
            if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
            Player p = (Player) sender;
            boolean ok = LunarBridge.setRichPresence(p,
                    "FabledMC", "SoulEnchants", "Test Presence",
                    "Manual /lunar rpc test @ " + System.currentTimeMillis() % 100000,
                    "Test Map", null);
            if (ok) {
                Chat.good(p, "Rich Presence packet sent. " + MessageStyle.MUTED
                        + "If Discord still shows \"playing a local server\", read below:");
                Chat.rule(p);
                Chat.info(p, MessageStyle.BAD + "IMPORTANT: §7Lunar's RichPresence module requires your");
                Chat.info(p, "server to be listed in §fLunar's ServerMappings §7— a curated");
                Chat.info(p, "allow-list of public servers. The client IGNORES rich-presence");
                Chat.info(p, "packets from unlisted/local servers.");
                Chat.info(p, "");
                Chat.info(p, "Verbatim from Lunar docs:");
                Chat.info(p, MessageStyle.MUTED + MessageStyle.ITALIC
                        + "  \"Your server must be a part of Lunar Client's ServerMappings");
                Chat.info(p, MessageStyle.MUTED + MessageStyle.ITALIC
                        + "   to use this module.\"");
                Chat.info(p, "");
                Chat.info(p, "To register: open a PR at §fgithub.com/LunarClient/ServerMappings");
                Chat.info(p, "Until then, RPC will not work — rest of Apollo (titles, toasts,");
                Chat.info(p, "waypoints, cooldowns, holograms) works fine without ServerMappings.");
            } else {
                Chat.err(p, "Rich Presence push failed — Apollo not resolved or player not on Lunar.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
            boolean ok = LunarBridge.resetRichPresence((Player) sender);
            Chat.info(sender, ok ? "Rich Presence reset." : "Reset failed (no Apollo or not on Lunar).");
            return true;
        }
        Chat.info(sender, "Usage: /lunar status | /lunar test | /lunar rpc | /lunar reset");
        return true;
    }

    private static void printDetectedPlugin(CommandSender to) {
        String[] names = { "Apollo", "LunarClient-API", "LunarBukkitAPI", "LunarClientAPI", "LunarAPI" };
        for (String n : names) {
            org.bukkit.plugin.Plugin pl = Bukkit.getPluginManager().getPlugin(n);
            if (pl != null) {
                to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.GOOD + "✓ "
                        + MessageStyle.MUTED + "Plugin:     " + MessageStyle.VALUE + n
                        + MessageStyle.FRAME + " v" + MessageStyle.VALUE + pl.getDescription().getVersion());
                return;
            }
        }
        to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.BAD + "✗ "
                + MessageStyle.MUTED + "No Lunar plugin found in plugins/");
    }
}
