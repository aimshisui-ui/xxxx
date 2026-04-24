package com.soulenchants.bosses.oakenheart;

import com.soulenchants.SoulEnchants;
import com.soulenchants.loot.BossLootItems;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /oakenheart subcommands:
 *   status      — prints active/inactive + HP + phase
 *   summon      — force-spawn at the sender's location (admin)
 *   abort       — kill the active boss silently (admin)
 *   give [p]    — hand out a Ritual Sapling (admin)
 */
public final class OakenheartCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public OakenheartCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            OakenheartManager mgr = plugin.getOakenheartManager();
            if (mgr == null || !mgr.hasActive()) {
                Chat.info(sender, "Oakenheart is dormant. Use a Ritual Sapling to summon.");
                return true;
            }
            OakenheartBoss b = mgr.getActive();
            int hp = (int) Math.ceil(b.getEntity().getHealth());
            int max = (int) Math.ceil(b.getEntity().getMaxHealth());
            Chat.banner(sender, "Oakenheart — Forest Sovereign");
            sender.sendMessage(MessageStyle.MUTED + "  Phase: " + MessageStyle.VALUE + b.getPhase());
            sender.sendMessage(MessageStyle.MUTED + "  HP:    " + MessageStyle.VALUE + hp + "/" + max);
            sender.sendMessage(MessageStyle.MUTED + "  World: " + MessageStyle.VALUE + b.getEntity().getWorld().getName());
            return true;
        }

        if (!sender.hasPermission("soulenchants.admin")) {
            Chat.err(sender, "You need soulenchants.admin.");
            return true;
        }

        if (args[0].equalsIgnoreCase("summon")) {
            if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
            Player p = (Player) sender;
            if (plugin.getOakenheartManager().hasActive()) {
                Chat.err(sender, "Oakenheart is already active.");
                return true;
            }
            plugin.getOakenheartManager().summon(p.getLocation());
            Chat.good(sender, "Summoned Oakenheart.");
            return true;
        }
        if (args[0].equalsIgnoreCase("abort")) {
            OakenheartManager mgr = plugin.getOakenheartManager();
            if (mgr == null || !mgr.hasActive()) { Chat.err(sender, "No active Oakenheart."); return true; }
            mgr.getActive().getEntity().remove();
            mgr.getActive().stop(false);
            mgr.clearActive();
            Chat.good(sender, "Aborted Oakenheart.");
            return true;
        }
        if (args[0].equalsIgnoreCase("give")) {
            String targetName = args.length >= 2 ? args[1]
                    : (sender instanceof Player ? sender.getName() : null);
            if (targetName == null) { Chat.err(sender, "Specify a player."); return true; }
            Player p = Bukkit.getPlayerExact(targetName);
            if (p == null) { Chat.err(sender, "Player not online: " + targetName); return true; }
            p.getInventory().addItem(BossLootItems.ritualSapling());
            Chat.good(sender, "Gave " + MessageStyle.VALUE + "Ritual Sapling"
                    + MessageStyle.GOOD + " to " + MessageStyle.VALUE + p.getName() + MessageStyle.GOOD + ".");
            return true;
        }
        Chat.info(sender, "Usage: /oakenheart status|summon|abort|give [player]");
        return true;
    }
}
