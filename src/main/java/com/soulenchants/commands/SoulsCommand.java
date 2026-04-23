package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import com.soulenchants.currency.SoulTier;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            showProfile(sender, (OfflinePlayer) sender);
            return true;
        }
        String sub = args[0].toLowerCase();

        if (sub.equals("show") || sub.equals("info") || sub.equals("profile")) {
            OfflinePlayer target = args.length >= 2 ? Bukkit.getOfflinePlayer(args[1])
                    : (sender instanceof Player ? (Player) sender : null);
            if (target == null) { sender.sendMessage("§cSpecify a player."); return true; }
            showProfile(sender, target);
            return true;
        }

        // v1.1 — mint a Soul Gem from ledger souls. One-way: no deposit path.
        if (sub.equals("withdraw") || sub.equals("mint")) {
            if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
            Player p = (Player) sender;
            if (args.length < 2) {
                Chat.info(p, "Usage: /souls withdraw <amount>  " + MessageStyle.FRAME
                        + "(mints a Soul Gem; one-way — no deposit)");
                return true;
            }
            long amt = parseAmount(args[1]);
            if (amt <= 0) { Chat.err(p, "Enter a positive amount."); return true; }
            long ledger = plugin.getSoulManager().get(p);
            if (ledger < amt) {
                Chat.err(p, "Soul Bank short — you have " + MessageStyle.VALUE
                        + com.soulenchants.items.SoulGem.formatNum(ledger)
                        + MessageStyle.BAD + ", need " + MessageStyle.VALUE
                        + com.soulenchants.items.SoulGem.formatNum(amt));
                return true;
            }
            plugin.getSoulManager().take(p, amt);
            org.bukkit.inventory.ItemStack gem = com.soulenchants.items.SoulGem.create(amt);
            p.getInventory().addItem(gem).values().forEach(
                    o -> p.getWorld().dropItemNaturally(p.getLocation(), o));
            Chat.good(p, "Minted a " + MessageStyle.TIER_SOUL
                    + com.soulenchants.items.SoulGem.formatNum(amt) + "-soul gem"
                    + MessageStyle.GOOD + ". " + MessageStyle.BAD + MessageStyle.ITALIC
                    + "(One-way — cannot be deposited back.)");
            p.playSound(p.getLocation(), org.bukkit.Sound.ORB_PICKUP, 0.9f, 1.3f);
            return true;
        }

        if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (args.length < 3) { sender.sendMessage("§c/souls " + sub + " <player> <amount>"); return true; }
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

        // ── ADMIN DEBUG TOOLS ─────────────────────────────────────────────
        if (sub.equals("setlifetime") || sub.equals("setlt")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (args.length < 3) { sender.sendMessage("§c/souls setlifetime <player> <amount>"); return true; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            long amt;
            try { amt = Long.parseLong(args[2]); }
            catch (NumberFormatException e) { sender.sendMessage("§cInvalid amount."); return true; }
            // Force lifetime by calling add with the delta
            long current = plugin.getSoulManager().getLifetime(target);
            if (amt > current) plugin.getSoulManager().add(target, amt - current);
            else sender.sendMessage("§eNote: lifetime can't go down. Current: " + current);
            sender.sendMessage("§a✦ Lifetime now: §f" + plugin.getSoulManager().getLifetime(target)
                    + " §7(" + plugin.getSoulManager().getTier(target).getLabel() + ")");
            return true;
        }

        if (sub.equals("debug")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage("§cNo permission."); return true; }
            sender.sendMessage("§5✦ §dSoulManager debug:");
            sender.sendMessage("§7  Tier thresholds:");
            for (SoulTier t : SoulTier.values()) {
                sender.sendMessage("§7    " + t.prefix() + " §8≥ §f" + t.getThreshold()
                        + (t.grantsBonusPerKill() ? " §7| +1 souls/kill" : ""));
            }
            sender.sendMessage("§7  Online players:");
            for (Player p : Bukkit.getOnlinePlayers()) {
                long c = plugin.getSoulManager().get(p);
                long lt = plugin.getSoulManager().getLifetime(p);
                SoulTier t = plugin.getSoulManager().getTier(p);
                sender.sendMessage("§7    §f" + p.getName() + " §8| §dnow: " + c
                        + " §8| §5lt: " + lt + " §8| " + t.prefix());
            }
            return true;
        }

        if (sub.equals("simkill") || sub.equals("testkill")) {
            // Simulates a mob kill — useful for verifying soul drops, tier promotion
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (!(sender instanceof Player)) { sender.sendMessage("§cMust be a player."); return true; }
            int n = args.length >= 2 ? safeInt(args[1], 1) : 1;
            int per = args.length >= 3 ? safeInt(args[2], 5) : 5;
            Player p = (Player) sender;
            for (int i = 0; i < n; i++) plugin.getSoulManager().add(p, per);
            sender.sendMessage("§a✦ Awarded §f" + (n*per) + " §asouls (sim).");
            return true;
        }

        if (sub.equals("tier")) {
            // List or query a tier
            OfflinePlayer target = args.length >= 2 ? Bukkit.getOfflinePlayer(args[1])
                    : (sender instanceof Player ? (Player) sender : null);
            if (target == null) { sender.sendMessage("§cSpecify a player."); return true; }
            SoulTier t = plugin.getSoulManager().getTier(target);
            SoulTier next = t.next();
            sender.sendMessage("§5✦ §d" + target.getName() + " §7→ " + t.prefix());
            sender.sendMessage("§7  Lifetime souls: §f" + plugin.getSoulManager().getLifetime(target));
            if (next != null) {
                long need = next.getThreshold() - plugin.getSoulManager().getLifetime(target);
                sender.sendMessage("§7  Next tier: " + next.prefix() + " §7(need §f" + need + " §7more)");
            } else {
                sender.sendMessage("§7  §6Maximum tier reached.");
            }
            return true;
        }

        if (sub.equals("settier")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (args.length < 3) {
                sender.sendMessage("§c/souls settier <player> <tier>");
                StringBuilder tiers = new StringBuilder("§7Valid tiers: ");
                for (SoulTier t : SoulTier.values()) tiers.append(t.getColor()).append(t.getLabel()).append(" §7");
                sender.sendMessage(tiers.toString());
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            SoulTier wanted;
            try { wanted = SoulTier.valueOf(args[2].toUpperCase()); }
            catch (IllegalArgumentException ex) { sender.sendMessage("§cUnknown tier. Try: initiate/bronze/silver/gold/veiled/soulbound"); return true; }
            long current = plugin.getSoulManager().getLifetime(target);
            long target2 = wanted.getThreshold();
            if (target2 > current) {
                plugin.getSoulManager().add(target, target2 - current);
                sender.sendMessage("§a✦ Bumped " + target.getName() + " up to " + wanted.prefix());
            } else if (target2 < current) {
                // Lifetime is permanent; we have to surgically lower it without going through add()
                // Use the manager's set semantics via reflection-free helper
                plugin.getSoulManager().forceSetLifetime(target, target2);
                sender.sendMessage("§e✦ Forced " + target.getName() + " down to " + wanted.prefix() + " §7(non-canonical, debug only)");
            } else {
                sender.sendMessage("§7Already at " + wanted.prefix());
            }
            return true;
        }

        if (sub.equals("rules") || sub.equals("table")) {
            // Show the mob soul drop table
            sender.sendMessage("§5✦ §dMob soul drops:");
            sender.sendMessage("§7  Zombie §8| §f3         Skeleton §8| §f4");
            sender.sendMessage("§7  Spider §8| §f3         Cave Spider §8| §f5");
            sender.sendMessage("§7  Creeper §8| §f8        Witch §8| §f12");
            sender.sendMessage("§7  Enderman §8| §f15      Pig Zombie §8| §f6");
            sender.sendMessage("§7  Blaze §8| §f10         Wither Skel §8| §f12");
            sender.sendMessage("§7  Magma Cube §8| §f2-4   Slime §8| §f1-2");
            sender.sendMessage("§7  Ghast §8| §f9          Silverfish §8| §f2");
            sender.sendMessage("§7  Iron Sentinel §8| §f30  Echo Clone §8| §f50");
            sender.sendMessage("§7  Bonus rules:");
            sender.sendMessage("§7    • baby mobs: §f1");
            sender.sendMessage("§7    • spawner mobs: §c-50%");
            sender.sendMessage("§7    • one-shot full HP: §c-50%");
            sender.sendMessage("§7    • combat (recently hit): §a+20%");
            sender.sendMessage("§7    • Soul Reaper L1/2/3: §a+25/50/75%");
            sender.sendMessage("§7    • Silver+ tier: §a+1");
            sender.sendMessage("§7    • no players within 100 blocks: §c0 (anti-AFK)");
            return true;
        }

        Chat.banner(sender, "Souls " + MessageStyle.FRAME + "commands");
        row(sender, "/souls",                                "view your profile");
        row(sender, "/souls show <player>",                  "view any player's profile");
        row(sender, "/souls tier [player]",                  "show tier + progress to next");
        row(sender, "/souls withdraw <amount>",              MessageStyle.TIER_SOUL + "mint a Soul Gem "
                + MessageStyle.FRAME + "(one-way)");
        row(sender, "/souls rules",                          "mob drop table");
        sender.sendMessage(MessageStyle.FRAME + "  " + MessageStyle.BAR + MessageStyle.BAR + " "
                + MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Admin " + MessageStyle.FRAME + MessageStyle.BAR + MessageStyle.BAR);
        row(sender, "/souls give|take|set <player> <amt>",   "modify balance");
        row(sender, "/souls setlifetime <player> <amt>",     "only goes up");
        row(sender, "/souls settier <player> <tier>",        "jump to a tier");
        row(sender, "/souls debug",                          "verbose state dump");
        row(sender, "/souls simkill [count] [per]",          "simulate mob kills");
        Chat.rule(sender);
        return true;
    }

    private static void row(CommandSender s, String cmd, String desc) {
        s.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.ARROW + " "
                + MessageStyle.VALUE + cmd + MessageStyle.FRAME + "  " + MessageStyle.BAR + "  "
                + MessageStyle.MUTED + desc);
    }

    private void showProfile(CommandSender to, OfflinePlayer p) {
        long now = plugin.getSoulManager().get(p);
        long lt  = plugin.getSoulManager().getLifetime(p);
        SoulTier t = plugin.getSoulManager().getTier(p);
        SoulTier next = t.next();
        Chat.banner(to, p.getName() + MessageStyle.FRAME + " — Soul Profile");
        to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.SOUL_ORB + "  "
                + MessageStyle.MUTED + "Tier:     " + t.prefix()
                + (t.grantsBonusPerKill() ? MessageStyle.FRAME + "   " + MessageStyle.BAR + "   "
                        + MessageStyle.VALUE + "+1 souls/kill" : ""));
        to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.DIAMOND + "  "
                + MessageStyle.MUTED + "Souls:    " + MessageStyle.VALUE + now);
        to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.DIAMOND + "  "
                + MessageStyle.MUTED + "Lifetime: " + MessageStyle.VALUE + lt);
        if (next != null) {
            long need = next.getThreshold() - lt;
            to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.ARROW + "  "
                    + MessageStyle.MUTED + "Next:     " + next.prefix()
                    + MessageStyle.FRAME + "   " + MessageStyle.BAR + "   "
                    + MessageStyle.MUTED + "need " + MessageStyle.VALUE + need);
        } else {
            to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.STAR + "  "
                    + MessageStyle.SOUL_GOLD + "Maximum tier reached.");
        }
        Chat.rule(to);
    }

    private int safeInt(String s, int dflt) {
        try { return Integer.parseInt(s); } catch (Exception e) { return dflt; }
    }

    /** Supports k/m/b suffixes: "10k" → 10000, "2.5m" → 2_500_000. */
    private static long parseAmount(String s) {
        try {
            s = s.toLowerCase().trim().replace(",", "");
            double mult = 1;
            if      (s.endsWith("k")) { mult = 1_000;          s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("m")) { mult = 1_000_000;      s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("b")) { mult = 1_000_000_000L; s = s.substring(0, s.length() - 1); }
            return (long) (Double.parseDouble(s) * mult);
        } catch (Throwable t) { return -1; }
    }
}
