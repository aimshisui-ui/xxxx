package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.SoulGem;
import com.soulenchants.items.SoulGemUtil;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SoulGemCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public SoulGemCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
        Player p = (Player) sender;
        if (args.length == 0 || args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("bal")) {
            long ledger  = plugin.getSoulManager().get(p);
            long gemTotal = SoulGemUtil.totalGemBalance(p);
            Chat.banner(p, "Soul Balance");
            p.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.DIAMOND + "  "
                    + MessageStyle.MUTED + "Ledger:  " + MessageStyle.VALUE + SoulGem.formatNum(ledger));
            p.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.SOUL_ORB + "  "
                    + MessageStyle.MUTED + "Gems:    " + MessageStyle.VALUE + SoulGem.formatNum(gemTotal)
                    + MessageStyle.FRAME + "   " + MessageStyle.BAR + "   "
                    + MessageStyle.MUTED + (SoulGemUtil.hasGem(p)
                        ? MessageStyle.GOOD + "license active" : MessageStyle.BAD + "no gem — soul enchants blocked"));
            p.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.STAR + "  "
                    + MessageStyle.MUTED + "Total:   " + MessageStyle.VALUE + SoulGem.formatNum(ledger + gemTotal));
            Chat.rule(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("withdraw") && args.length >= 2) {
            long amt = parseAmount(args[1]);
            if (amt <= 0) { Chat.err(p, "Enter a positive amount."); return true; }
            if (!SoulGemUtil.withdraw(plugin, p, amt)) {
                Chat.err(p, "Not enough ledger souls — you have " + MessageStyle.VALUE
                        + SoulGem.formatNum(plugin.getSoulManager().get(p)) + MessageStyle.BAD + ".");
                return true;
            }
            Chat.good(p, "Minted a " + MessageStyle.TIER_SOUL + SoulGem.formatNum(amt) + "-soul gem"
                    + MessageStyle.GOOD + ".");
            p.playSound(p.getLocation(), org.bukkit.Sound.ORB_PICKUP, 0.9f, 1.3f);
            return true;
        }
        if (args[0].equalsIgnoreCase("deposit")) {
            org.bukkit.inventory.ItemStack held = p.getItemInHand();
            if (!SoulGem.isGem(held)) { Chat.err(p, "Hold a Soul Gem to deposit."); return true; }
            long amt = SoulGemUtil.deposit(plugin, p, held);
            p.setItemInHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
            Chat.good(p, "Deposited " + MessageStyle.VALUE + SoulGem.formatNum(amt)
                    + MessageStyle.GOOD + " souls to the ledger.");
            p.playSound(p.getLocation(), org.bukkit.Sound.LEVEL_UP, 0.8f, 1.8f);
            return true;
        }
        Chat.banner(p, "Soul Gem " + MessageStyle.FRAME + "commands");
        row(p, "/soulgem",                             "balance summary");
        row(p, "/soulgem withdraw <amount>",           "mint a gem from ledger souls");
        row(p, "/soulgem deposit",                     "deposit held gem into the ledger");
        row(p, "" + MessageStyle.MUTED + MessageStyle.ITALIC + "(or right-click a gem to deposit)", "");
        Chat.rule(p);
        return true;
    }

    /** Supports k / m suffixes — "10k" -> 10000, "2.5m" -> 2500000. */
    private static long parseAmount(String s) {
        try {
            s = s.toLowerCase().trim().replace(",", "");
            double mult = 1;
            if (s.endsWith("k")) { mult = 1_000; s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("m")) { mult = 1_000_000; s = s.substring(0, s.length() - 1); }
            else if (s.endsWith("b")) { mult = 1_000_000_000L; s = s.substring(0, s.length() - 1); }
            return (long) (Double.parseDouble(s) * mult);
        } catch (Throwable t) { return -1; }
    }

    private static void row(CommandSender s, String cmd, String desc) {
        s.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.ARROW + " "
                + MessageStyle.VALUE + cmd
                + (desc.isEmpty() ? "" : MessageStyle.FRAME + "  " + MessageStyle.BAR + "  "
                        + MessageStyle.MUTED + desc));
    }
}
