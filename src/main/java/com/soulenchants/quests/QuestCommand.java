package com.soulenchants.quests;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand implements CommandExecutor {

    private final SoulEnchants plugin;
    public QuestCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
            if (args.length < 2) { sender.sendMessage("§c/quests reset <player>"); return true; }
            org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
            plugin.getQuestManager().getProfile().setTutorialStep(t.getUniqueId(), 0);
            sender.sendMessage("§a✦ Reset " + t.getName() + "'s quest progress.");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player)) { sender.sendMessage("§cPlayer only."); return true; }
            Player p = (Player) sender;
            int step = plugin.getQuestManager().getProfile().getTutorialStep(p.getUniqueId());
            sender.sendMessage("§5✦ §dQuest status:");
            sender.sendMessage("§7  Tutorial step: §f" + step + "§7/" + QuestRegistry.tutorialChain().size());
            for (String qid : plugin.getQuestManager().getProfile().getActiveDailies(p.getUniqueId())) {
                Quest q = QuestRegistry.get(qid);
                if (q == null) continue;
                int prog = plugin.getQuestManager().getProfile().getProgress(p.getUniqueId(), qid);
                sender.sendMessage("§7  Daily: §f" + q.name + " §8(§f" + prog + "§7/§f" + q.goal + "§8)");
            }
            return true;
        }
        if (!(sender instanceof Player)) { sender.sendMessage("§cPlayer only."); return true; }
        plugin.getQuestManager().ensureDailies((Player) sender);
        plugin.getQuestManager().getProfile().getTutorialStep(((Player) sender).getUniqueId()); // touch profile
        // Open GUI
        Player p = (Player) sender;
        // Use the registered QuestGUI
        plugin.getQuestGUI().open(p);
        return true;
    }
}
