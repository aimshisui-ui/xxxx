package com.soulenchants.masks;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MaskCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public MaskCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
        Player p = (Player) sender;
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Chat.banner(p, "Masks");
            for (Mask m : MaskRegistry.all()) {
                String line = MessageStyle.FRAME + "  " + MessageStyle.TIER_EPIC + MessageStyle.BOLD + "✦ " +
                        m.getDisplayName() + MessageStyle.SEP + MessageStyle.MUTED + m.getId();
                p.sendMessage(line);
            }
            Chat.rule(p);
            Chat.info(p, "Use /mask equip <id> to equip. /mask clear to remove.");
            return true;
        }
        if (args[0].equalsIgnoreCase("equip") && args.length >= 2) {
            Mask m = MaskRegistry.get(args[1].toLowerCase());
            if (m == null) { Chat.err(p, "Unknown mask id: " + args[1]); return true; }
            plugin.getMaskManager().equip(p, m.getId());
            Chat.good(p, "Equipped " + MessageStyle.TIER_EPIC + m.getDisplayName() + MessageStyle.GOOD + ".");
            refreshNearby(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("clear")) {
            plugin.getMaskManager().clear(p);
            Chat.good(p, "Cleared mask.");
            refreshNearby(p);
            return true;
        }
        Chat.info(p, "Usage: /mask list | /mask equip <id> | /mask clear");
        return true;
    }

    /** Force nearby players to re-render the wearer's helmet. */
    private void refreshNearby(Player p) {
        p.getInventory().setHelmet(p.getInventory().getHelmet());
    }
}
