package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /stats [player]              — print the target's stat profile (defaults
 *                                to the sender)
 * /stats reset <player>        — admin-only; wipes the target's K/D record
 *                                (leaves soul balance + other stats intact)
 * /stats top [kills|kdr]       — top-10 leaderboard for the chosen metric
 *
 * Everything reads from in-memory caches (PvPStats + SoulManager) — no
 * disk I/O on the main thread.
 */
public final class StatsCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public StatsCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("soulenchants.admin")) {
                Chat.err(sender, "You need soulenchants.admin to reset stats.");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || target.getName() == null) {
                Chat.err(sender, "Unknown player: " + args[1]);
                return true;
            }
            boolean wiped = plugin.getPvPStats().resetKd(target.getUniqueId());
            if (wiped) {
                Chat.good(sender, "Reset K/D for " + MessageStyle.VALUE + target.getName()
                        + MessageStyle.GOOD + ". Soul balance and other stats kept.");
            } else {
                Chat.info(sender, target.getName() + " had no K/D record to reset.");
            }
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {
            String metric = args.length >= 2 ? args[1].toLowerCase() : "kills";
            printTop(sender, metric);
            return true;
        }

        OfflinePlayer target;
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || target.getName() == null) {
                Chat.err(sender, "Unknown player: " + args[0]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            Chat.info(sender, "Console usage: /stats <player>");
            return true;
        }
        printProfile(sender, target);
        return true;
    }

    private void printProfile(CommandSender to, OfflinePlayer target) {
        int kills  = plugin.getPvPStats().getKills(target);
        int deaths = plugin.getPvPStats().getDeaths(target);
        double kdr = plugin.getPvPStats().getKDR(target);
        long souls = target.isOnline()
                ? plugin.getSoulManager().get((Player) target)
                : 0L;
        com.soulenchants.currency.SoulTier tier = target.isOnline()
                ? plugin.getSoulManager().getTier((Player) target)
                : null;

        Chat.banner(to, "Stats · " + target.getName());
        to.sendMessage(MessageStyle.MUTED + "  Kills   " + MessageStyle.VALUE + kills);
        to.sendMessage(MessageStyle.MUTED + "  Deaths  " + MessageStyle.VALUE + deaths);
        to.sendMessage(MessageStyle.MUTED + "  K/D     " + kdrColor(kdr) + String.format("%.2f", kdr));
        if (tier != null) {
            to.sendMessage(MessageStyle.MUTED + "  Tier    " + tier.getColor() + tier.getLabel());
            to.sendMessage(MessageStyle.MUTED + "  Souls   " + MessageStyle.SOUL_GOLD + souls);
        } else {
            to.sendMessage(MessageStyle.MUTED + "  " + ChatColor.ITALIC + "(offline — soul balance + tier hidden)");
        }
        Chat.rule(to);
    }

    private void printTop(CommandSender to, String metric) {
        // Build top 10 list from PvPStats cache
        java.util.List<java.util.Map.Entry<java.util.UUID, Integer>> killsRanked = new java.util.ArrayList<>();
        java.util.List<java.util.Map.Entry<java.util.UUID, Double>>   kdrRanked   = new java.util.ArrayList<>();
        // We don't expose the raw maps; reuse the getKills/getKDR over every seen UUID.
        // Cheap enough for top-10 — servers at this scale have < 1000 players.
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
        // Walk every OfflinePlayer Bukkit knows about (cached profiles)
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getUniqueId() == null) continue;
            seen.add(p.getUniqueId());
            killsRanked.add(new java.util.AbstractMap.SimpleEntry<>(
                    p.getUniqueId(), plugin.getPvPStats().getKills(p)));
            kdrRanked.add(new java.util.AbstractMap.SimpleEntry<>(
                    p.getUniqueId(), plugin.getPvPStats().getKDR(p)));
        }
        Chat.banner(to, "Leaderboard · " + metric);
        if (metric.equals("kdr")) {
            kdrRanked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            int i = 1;
            for (java.util.Map.Entry<java.util.UUID, Double> e : kdrRanked) {
                if (i > 10) break;
                OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
                if (p.getName() == null || e.getValue() == 0) continue;
                to.sendMessage(MessageStyle.FRAME + "  " + i + ". "
                        + MessageStyle.VALUE + p.getName()
                        + MessageStyle.MUTED + " — " + MessageStyle.VALUE
                        + String.format("%.2f", e.getValue()) + " K/D");
                i++;
            }
        } else {
            killsRanked.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            int i = 1;
            for (java.util.Map.Entry<java.util.UUID, Integer> e : killsRanked) {
                if (i > 10) break;
                OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
                if (p.getName() == null || e.getValue() == 0) continue;
                to.sendMessage(MessageStyle.FRAME + "  " + i + ". "
                        + MessageStyle.VALUE + p.getName()
                        + MessageStyle.MUTED + " — " + MessageStyle.VALUE
                        + e.getValue() + " kills");
                i++;
            }
        }
        Chat.rule(to);
    }

    private static String kdrColor(double kdr) {
        if (kdr >= 3.0) return "§6";  // gold
        if (kdr >= 1.5) return "§a";  // green
        if (kdr >= 1.0) return "§e";  // yellow
        return "§c";                  // red
    }
}
