package com.soulenchants.modock;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /modock setspawn boss [phase]    set Modock's spawn (defaults to phase = current world)
 * /modock setspawn player [phase]  set the player arrival point (same default)
 * /modock spawns                   list every configured boss/player spawn
 * /modock give [player]            give a Modock spawner item
 * /modock summon                   admin: bypass the item, jump straight in
 * /modock abort                    admin: end the encounter, TP all back
 * /modock status                   current state
 * /modock tp <phase>               admin: TP to a phase world for setup
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
                p.sendMessage(ChatColor.RED + "Usage: /modock setspawn <boss|player> [phase1|phase2|phase3]");
                return true;
            }
            // Optional 3rd arg lets you set any phase's spawn from any world.
            // No 3rd arg = use your current world.
            String phase;
            if (args.length >= 3) {
                phase = args[2].toLowerCase();
                if (!phase.equals("phase1") && !phase.equals("phase2") && !phase.equals("phase3")) {
                    p.sendMessage(ChatColor.RED + "Phase must be phase1, phase2, or phase3."); return true;
                }
            } else {
                phase = ModockSpawnConfig.phaseFor(p.getWorld().getName());
                if (phase == null) {
                    p.sendMessage(ChatColor.RED + "Either pass a phase explicitly, or stand in a Modock phase world.");
                    p.sendMessage(ChatColor.GRAY + "  /modock setspawn " + args[1].toLowerCase() + " phase1");
                    return true;
                }
            }
            plugin.getModockManager().getSpawnCfg().setLocation(phase, args[1].toLowerCase(), p.getLocation());
            p.sendMessage(ChatColor.GREEN + "✦ Set " + ChatColor.AQUA + phase + "." + args[1].toLowerCase()
                    + ChatColor.GREEN + " spawn to "
                    + ChatColor.WHITE + "(" + p.getLocation().getBlockX() + ", "
                    + p.getLocation().getBlockY() + ", " + p.getLocation().getBlockZ() + ")"
                    + ChatColor.GRAY + " in " + p.getWorld().getName());
            return true;
        }

        if (sub.equals("spawns")) {
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "✦ Modock spawn configuration:");
            for (String phase : new String[]{"phase1", "phase2", "phase3"}) {
                ModockSpawnConfig.Pair pair = plugin.getModockManager().getSpawnCfg().get(phase);
                String wn = ModockSpawnConfig.worldFor(phase);
                org.bukkit.World w = Bukkit.getWorld(wn);
                if (w == null) {
                    sender.sendMessage(ChatColor.GRAY + "  " + phase + " " + ChatColor.RED + "(world '"
                            + wn + "' NOT loaded)");
                    continue;
                }
                if (pair == null) {
                    sender.sendMessage(ChatColor.GRAY + "  " + phase + " " + ChatColor.YELLOW
                            + "(no config — will fall back to world spawn)");
                    continue;
                }
                sender.sendMessage(ChatColor.GRAY + "  " + ChatColor.AQUA + phase + ChatColor.GRAY + " in " + wn);
                sender.sendMessage(ChatColor.GRAY + "    boss   " + ChatColor.WHITE
                        + "(" + (int)pair.boss.getX() + ", " + (int)pair.boss.getY() + ", " + (int)pair.boss.getZ() + ")");
                sender.sendMessage(ChatColor.GRAY + "    player " + ChatColor.WHITE
                        + "(" + (int)pair.player.getX() + ", " + (int)pair.player.getY() + ", " + (int)pair.player.getZ() + ")");
            }
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

        sender.sendMessage(ChatColor.AQUA + "/modock status | spawns | give [player] | summon | abort");
        sender.sendMessage(ChatColor.AQUA + "          setspawn <boss|player> [phase1|phase2|phase3]");
        sender.sendMessage(ChatColor.AQUA + "          tp <phase1|phase2|phase3>");
        return true;
    }
}
