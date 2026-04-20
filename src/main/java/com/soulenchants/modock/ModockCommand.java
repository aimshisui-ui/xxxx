package com.soulenchants.modock;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /modock setspawn boss     (admin, in-world) — set Modock's spawn point in this phase
 * /modock setspawn player   (admin, in-world) — set the player arrival point in this phase
 * /modock give [player]     — admin: give a Modock spawner item
 * /modock summon            — admin: bypass the item, jump straight in
 * /modock abort             — admin: end the encounter, TP all back
 * /modock status            — current state
 * /modock tp <phase>        — admin: TP yourself to a phase world for setup
 */
public final class ModockCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public ModockCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "status";

        if (sub.equals("status")) {
            ModockBoss b = plugin.getModockManager().getActive();
            if (b == null) {
                sender.sendMessage(ChatColor.AQUA + "✦ Modock: idle.");
            } else {
                sender.sendMessage(ChatColor.AQUA + "✦ Modock: " + b.getPhase() + " · "
                        + ChatColor.WHITE + (int) b.getEntity().getHealth() + "/" + (int) b.getEntity().getMaxHealth() + " HP"
                        + ChatColor.GRAY + " · " + b.getParticipants().size() + " players");
            }
            return true;
        }

        if (sub.equals("give")) {
            if (!sender.hasPermission("soulenchants.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission."); return true;
            }
            Player target = args.length >= 2 ? Bukkit.getPlayer(args[1])
                    : (sender instanceof Player ? (Player) sender : null);
            if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
            target.getInventory().addItem(ModockSpawner.create()).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
            sender.sendMessage(ChatColor.GREEN + "✦ Gave " + target.getName() + " a Modock spawner.");
            return true;
        }

        if (sub.equals("abort")) {
            if (!sender.hasPermission("soulenchants.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission."); return true;
            }
            plugin.getModockManager().abort();
            sender.sendMessage(ChatColor.YELLOW + "✦ Modock aborted.");
            return true;
        }

        // Player-required from here
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Player only."); return true;
        }
        Player p = (Player) sender;

        if (sub.equals("summon")) {
            if (!p.hasPermission("soulenchants.admin")) {
                p.sendMessage(ChatColor.RED + "No permission."); return true;
            }
            plugin.getModockManager().begin(p);
            return true;
        }

        if (sub.equals("setspawn")) {
            if (!p.hasPermission("soulenchants.admin")) {
                p.sendMessage(ChatColor.RED + "No permission."); return true;
            }
            if (args.length < 2 || (!args[1].equalsIgnoreCase("boss") && !args[1].equalsIgnoreCase("player"))) {
                p.sendMessage(ChatColor.RED + "Usage: /modock setspawn <boss|player>"); return true;
            }
            String phase = ModockSpawnConfig.phaseFor(p.getWorld().getName());
            if (phase == null) {
                p.sendMessage(ChatColor.RED + "Stand in a Modock phase world (modock_phase1/2/3) first.");
                return true;
            }
            plugin.getModockManager().getSpawnCfg().setLocation(phase, args[1].toLowerCase(), p.getLocation());
            p.sendMessage(ChatColor.GREEN + "✦ Set " + phase + "." + args[1].toLowerCase()
                    + " spawn to your location.");
            return true;
        }

        if (sub.equals("tp")) {
            if (!p.hasPermission("soulenchants.admin")) {
                p.sendMessage(ChatColor.RED + "No permission."); return true;
            }
            if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /modock tp <phase1|phase2|phase3>"); return true; }
            String wn = ModockSpawnConfig.worldFor(args[1].toLowerCase());
            if (wn == null) { p.sendMessage(ChatColor.RED + "Unknown phase."); return true; }
            org.bukkit.World w = Bukkit.getWorld(wn);
            if (w == null) { p.sendMessage(ChatColor.RED + "World not loaded: " + wn); return true; }
            try { p.teleport(w.getSpawnLocation()); } catch (Throwable ignored) {}
            p.sendMessage(ChatColor.AQUA + "✦ Teleported to " + wn + ".");
            return true;
        }

        sender.sendMessage(ChatColor.AQUA + "/modock status | give [player] | summon | abort | setspawn <boss|player> | tp <phase>");
        return true;
    }
}
