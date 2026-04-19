package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.items.ItemFactories;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CECommand implements CommandExecutor {

    private final SoulEnchants plugin;
    public CECommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            sender.sendMessage("§5✦ §dRegistered Enchants:");
            for (CustomEnchant e : EnchantRegistry.all()) {
                sender.sendMessage(" §7- " + e.getTier().coloredName() + " §8| §f"
                        + e.getDisplayName() + " §7(max " + e.getMaxLevel() + ") §8- " + e.getDescription());
            }
            return true;
        }
        if (!sender.hasPermission("soulenchants.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (sub.equals("book")) {
            if (args.length < 4) { sender.sendMessage("§c/ce book <player> <enchant> <level>"); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
            CustomEnchant e = EnchantRegistry.get(args[2]);
            if (e == null) { sender.sendMessage("§cUnknown enchant. Try /ce list"); return true; }
            int lvl;
            try { lvl = Math.min(Integer.parseInt(args[3]), e.getMaxLevel()); }
            catch (NumberFormatException ex) { sender.sendMessage("§cInvalid level."); return true; }
            target.getInventory().addItem(ItemFactories.book(e, lvl));
            sender.sendMessage("§a✦ Gave " + target.getName() + " a " + e.getDisplayName() + " " + lvl + " book.");
            return true;
        }
        if (sub.equals("dust")) {
            if (args.length < 3) { sender.sendMessage("§c/ce dust <player> <rate 25|50|75|100>"); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
            int rate;
            try { rate = Integer.parseInt(args[2]); }
            catch (NumberFormatException ex) { sender.sendMessage("§cInvalid rate."); return true; }
            target.getInventory().addItem(ItemFactories.dust(rate));
            sender.sendMessage("§a✦ Gave " + target.getName() + " Magic Dust " + rate + "%.");
            return true;
        }
        if (sub.equals("scroll")) {
            if (args.length < 3) { sender.sendMessage("§c/ce scroll <player> <black|white>"); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
            ItemStack scroll = args[2].equalsIgnoreCase("white") ? ItemFactories.whiteScroll() : ItemFactories.blackScroll();
            target.getInventory().addItem(scroll);
            sender.sendMessage("§a✦ Gave " + target.getName() + " a " + args[2] + " scroll.");
            return true;
        }
        if (sub.equals("menu")) {
            if (!(sender instanceof Player)) { sender.sendMessage("§cMust be a player."); return true; }
            plugin.getEnchantMenu().open((Player) sender);
            return true;
        }
        if (sub.equals("summon") && args.length >= 2) {
            if (!(sender instanceof Player)) { sender.sendMessage("§cMust be a player."); return true; }
            Player p = (Player) sender;
            String boss = args[1].toLowerCase();
            if (boss.equals("veilweaver")) {
                boolean ok = plugin.getVeilweaverManager().summon(p.getLocation());
                if (!ok) { sender.sendMessage("§cThe Veilweaver is already manifested."); return true; }
                sender.sendMessage("§a✦ The Veilweaver has been summoned.");
                return true;
            }
            if (boss.equals("irongolem") || boss.equals("colossus")) {
                boolean ok = plugin.getIronGolemManager().summon(p.getLocation());
                if (!ok) { sender.sendMessage("§cThe Ironheart Colossus is already alive."); return true; }
                sender.sendMessage("§a✦ The Ironheart Colossus has been summoned.");
                return true;
            }
            sender.sendMessage("§c/ce summon <veilweaver|irongolem>");
            return true;
        }
        if (sub.equals("despawn") && args.length >= 2) {
            String boss = args[1].toLowerCase();
            if (boss.equals("veilweaver")) {
                if (plugin.getVeilweaverManager().getActive() == null) { sender.sendMessage("§cNo active Veilweaver."); return true; }
                plugin.getVeilweaverManager().getActive().getEntity().remove();
                plugin.getVeilweaverManager().getActive().stop(false);
                sender.sendMessage("§a✦ Veilweaver despawned.");
                return true;
            }
            if (boss.equals("irongolem") || boss.equals("colossus")) {
                if (plugin.getIronGolemManager().getActive() == null) { sender.sendMessage("§cNo active Colossus."); return true; }
                plugin.getIronGolemManager().getActive().getEntity().remove();
                plugin.getIronGolemManager().getActive().stop(false);
                sender.sendMessage("§a✦ Colossus despawned.");
                return true;
            }
            sender.sendMessage("§c/ce despawn <veilweaver|irongolem>");
            return true;
        }
        help(sender);
        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage("§5✦ §dSoulEnchants commands:");
        s.sendMessage("§7  /ce list");
        s.sendMessage("§7  /ce menu §8(admin GUI to grab any enchant at max)");
        s.sendMessage("§7  /ce book <player> <enchant> <level>");
        s.sendMessage("§7  /ce dust <player> <25|50|75|100>");
        s.sendMessage("§7  /ce scroll <player> <black|white>");
        s.sendMessage("§7  /ce summon <veilweaver|irongolem>");
        s.sendMessage("§7  /ce despawn <veilweaver|irongolem>");
    }
}
