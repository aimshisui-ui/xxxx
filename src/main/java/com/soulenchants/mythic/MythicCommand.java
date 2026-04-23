package com.soulenchants.mythic;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MythicCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public MythicCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("soulenchants.admin")) {
            Chat.err(sender, "You need soulenchants.admin.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Chat.banner(sender, "Mythic Weapons");
            for (MythicWeapon m : MythicRegistry.all()) {
                sender.sendMessage(MessageStyle.FRAME + "  " + MessageStyle.TIER_SOUL + MessageStyle.BOLD +
                        "❖ " + m.getDisplayName() + MessageStyle.SEP + MessageStyle.MUTED +
                        m.getId() + MessageStyle.SEP + MessageStyle.MUTED + MessageStyle.ITALIC +
                        m.getMode().name().toLowerCase());
            }
            Chat.rule(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("give") && args.length >= 2) {
            String target = args.length >= 3 ? args[2] : (sender instanceof Player ? sender.getName() : null);
            if (target == null) { Chat.err(sender, "Specify a player."); return true; }
            Player p = Bukkit.getPlayerExact(target);
            if (p == null) { Chat.err(sender, "Player not online: " + target); return true; }
            ItemStack item = MythicFactory.create(args[1].toLowerCase());
            if (item == null) { Chat.err(sender, "Unknown mythic id: " + args[1]); return true; }
            p.getInventory().addItem(item);
            Chat.good(sender, "Gave " + MessageStyle.TIER_SOUL + args[1] + MessageStyle.GOOD + " to " +
                    MessageStyle.VALUE + p.getName());
            return true;
        }
        Chat.info(sender, "Usage: /mythic list  |  /mythic give <id> [player]");
        return true;
    }
}
