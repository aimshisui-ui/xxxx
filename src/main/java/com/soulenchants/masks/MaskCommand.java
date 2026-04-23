package com.soulenchants.masks;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /mask — list every registered mask, and (admin-only) give them.
 * The equip/clear subcommands are gone in v1.1 — mask attach/detach is
 * now a real in-world interaction (drag the item onto a helmet).
 */
public final class MaskCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public MaskCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Chat.banner(sender, "Cosmetic Masks " + MessageStyle.FRAME + "(" + MaskRegistry.all().size() + ")");
            for (Mask m : MaskRegistry.all()) {
                sender.sendMessage(MessageStyle.FRAME + "  " + MessageStyle.TIER_EPIC + MessageStyle.BOLD
                        + "✦ " + m.getDisplayName() + MessageStyle.SEP
                        + MessageStyle.MUTED + m.getId());
            }
            Chat.rule(sender);
            Chat.info(sender, "Drag a mask onto any helmet in your inventory to attach. "
                    + MessageStyle.FRAME + "Right-click the helmet to detach.");
            if (sender.hasPermission("soulenchants.admin")) {
                Chat.info(sender, "Admin: " + MessageStyle.VALUE + "/mask give <id> [player]"
                        + MessageStyle.MUTED + " to hand out mask items.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("give") && args.length >= 2) {
            if (!sender.hasPermission("soulenchants.admin")) {
                Chat.err(sender, "You need soulenchants.admin.");
                return true;
            }
            Mask m = MaskRegistry.get(args[1].toLowerCase());
            if (m == null) { Chat.err(sender, "Unknown mask id: " + args[1]); return true; }
            String targetName = args.length >= 3 ? args[2]
                    : (sender instanceof Player ? sender.getName() : null);
            if (targetName == null) { Chat.err(sender, "Specify a player."); return true; }
            Player p = Bukkit.getPlayerExact(targetName);
            if (p == null) { Chat.err(sender, "Player not online: " + targetName); return true; }
            p.getInventory().addItem(m.buildInventoryItem());
            Chat.good(sender, "Gave " + MessageStyle.TIER_EPIC + m.getDisplayName()
                    + MessageStyle.GOOD + " to " + MessageStyle.VALUE + p.getName() + MessageStyle.GOOD + ".");
            return true;
        }
        Chat.info(sender, "Usage: /mask list  |  /mask give <id> [player]  " + MessageStyle.FRAME
                + "(drag onto helmet to attach)");
        return true;
    }
}
