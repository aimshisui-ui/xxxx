package com.soulenchants.rifts;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /rift                  — toggle: main world ↔ rift_world (admin shortcut)
 * /rift tp               — same as /rift
 * /rift setspawn         — admin: set rift_world spawn at your feet
 * /rift give [player]    — admin: give a Void Rift Spawner item
 * /rift addspawn <mob>   — admin (in rift_world): add a mob/boss spawn at feet
 * /rift removespawn      — admin (in rift_world): remove nearest spawn (8-block radius)
 * /rift listspawns       — list configured rift spawn entries
 * /rift clearspawns      — admin: wipe all spawn entries
 * /rift status           — current rift state + counts
 * /rift abort            — admin: force-end the active rift (no rewards)
 * /rift hologram <text>  — admin: spawn a text hologram at your feet
 *
 * Mob ids: any CustomMob id (see /mob list), plus 'veilweaver' or 'irongolem'
 * for boss spawns.
 */
public final class RiftCommand implements CommandExecutor {

    private final SoulEnchants plugin;
    private final RiftSpawnConfig spawns;
    private final VoidRiftManager rifts;
    private final HologramManager holograms;
    private final HologramGUI hologramGUI;
    private final Map<UUID, Location> returnPoint = new HashMap<>();

    public RiftCommand(SoulEnchants plugin, RiftSpawnConfig spawns, VoidRiftManager rifts,
                       HologramManager holograms, HologramGUI hologramGUI) {
        this.plugin = plugin;
        this.spawns = spawns;
        this.rifts = rifts;
        this.holograms = holograms;
        this.hologramGUI = hologramGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "tp";

        // Subcommands that don't require a player
        if (sub.equals("status")) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "✦ Rift status: " + ChatColor.WHITE + rifts.status());
            sender.sendMessage(ChatColor.GRAY + "  Configured spawns: " + ChatColor.WHITE + spawns.list().size());
            return true;
        }
        if (sub.equals("listspawns")) {
            List<RiftSpawnConfig.Entry> all = spawns.list();
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "✦ Rift spawn entries: " + ChatColor.WHITE + all.size());
            int i = 0;
            for (RiftSpawnConfig.Entry e : all) {
                sender.sendMessage(ChatColor.GRAY + "  " + (i++) + ": " + ChatColor.WHITE + e.mob
                        + ChatColor.GRAY + " @ (" + (int) e.x + ", " + (int) e.y + ", " + (int) e.z + ")");
            }
            return true;
        }
        if (sub.equals("give")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
            Player target = args.length >= 2 ? Bukkit.getPlayer(args[1])
                    : (sender instanceof Player ? (Player) sender : null);
            if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
            target.getInventory().addItem(VoidRiftItem.create()).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
            sender.sendMessage(ChatColor.GREEN + "✦ Gave " + target.getName() + " a Void Rift Spawner.");
            return true;
        }
        if (sub.equals("clearspawns")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
            spawns.clearAll();
            sender.sendMessage(ChatColor.YELLOW + "✦ All rift spawn entries cleared.");
            return true;
        }
        if (sub.equals("abort")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
            rifts.cleanup();
            sender.sendMessage(ChatColor.YELLOW + "✦ Active rift aborted.");
            return true;
        }

        // From here on, must be a player
        if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Player only."); return true; }
        Player p = (Player) sender;

        if (sub.equals("setspawn")) {
            if (!p.hasPermission("soulenchants.admin")) { p.sendMessage(ChatColor.RED + "No permission."); return true; }
            if (!p.getWorld().getName().equals(RiftWorld.NAME)) {
                p.sendMessage(ChatColor.RED + "Stand in rift_world first (/rift tp).");
                return true;
            }
            p.getWorld().setSpawnLocation(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
            p.sendMessage(ChatColor.GREEN + "✦ Rift world spawn set to " + fmt(p.getLocation()) + ".");
            return true;
        }

        if (sub.equals("addspawn")) {
            if (!p.hasPermission("soulenchants.admin")) { p.sendMessage(ChatColor.RED + "No permission."); return true; }
            if (!p.getWorld().getName().equals(RiftWorld.NAME)) {
                p.sendMessage(ChatColor.RED + "Stand in rift_world first.");
                return true;
            }
            if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /rift addspawn <mob_id>"); return true; }
            String mob = args[1].toLowerCase();
            spawns.add(mob, p.getLocation());
            p.sendMessage(ChatColor.GREEN + "✦ Added spawn: " + ChatColor.WHITE + mob
                    + ChatColor.GRAY + " @ " + fmt(p.getLocation()));
            return true;
        }

        if (sub.equals("removespawn")) {
            if (!p.hasPermission("soulenchants.admin")) { p.sendMessage(ChatColor.RED + "No permission."); return true; }
            if (!p.getWorld().getName().equals(RiftWorld.NAME)) {
                p.sendMessage(ChatColor.RED + "Stand in rift_world first.");
                return true;
            }
            boolean removed = spawns.removeNearest(p.getLocation());
            p.sendMessage(removed
                    ? ChatColor.YELLOW + "✦ Removed nearest spawn entry."
                    : ChatColor.RED + "No spawn entry within 8 blocks.");
            return true;
        }

        if (sub.equals("hologram") || sub.equals("holo") || sub.equals("holos")) {
            if (!p.hasPermission("soulenchants.admin")) { p.sendMessage(ChatColor.RED + "No permission."); return true; }
            // /rift hologram                → open the GUI
            // /rift hologram new            → create empty at feet, then open detail
            // /rift hologram delete         → delete nearest (within 8 blocks)
            // /rift hologram <text...>      → quick-create single-line at feet (legacy)
            if (args.length == 1) {
                hologramGUI.openRoot(p);
                return true;
            }
            String op = args[1].toLowerCase();
            if (op.equals("new") || op.equals("create")) {
                HologramConfig.Entry e = holograms.create(p.getLocation().add(0, 1.8, 0),
                        java.util.Collections.singletonList(ChatColor.GRAY + "(new hologram — click Edit lines)"));
                p.sendMessage(ChatColor.GREEN + "✦ Hologram created. Opening editor...");
                hologramGUI.openDetail(p, e.id);
                return true;
            }
            if (op.equals("delete") || op.equals("remove")) {
                HologramConfig.Entry near = holograms.nearest(p.getLocation(), 8);
                if (near == null) { p.sendMessage(ChatColor.RED + "No hologram within 8 blocks."); return true; }
                holograms.delete(near.id);
                p.sendMessage(ChatColor.YELLOW + "✦ Deleted nearest hologram.");
                return true;
            }
            // Quick-create legacy
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) { if (i > 1) sb.append(' '); sb.append(args[i]); }
            String txt = ChatColor.translateAlternateColorCodes('&', sb.toString());
            holograms.create(p.getLocation().add(0, 1.8, 0), java.util.Collections.singletonList(txt));
            p.sendMessage(ChatColor.GREEN + "✦ Hologram placed (persisted).");
            return true;
        }

        // Default: toggle teleport
        World rift = Bukkit.getWorld(RiftWorld.NAME);
        if (rift == null) {
            p.sendMessage(ChatColor.YELLOW + "✦ Rift world not loaded — creating it now...");
            rift = RiftWorld.ensure(plugin);
            if (rift == null) {
                p.sendMessage(ChatColor.RED + "Failed to create rift_world — check console.");
                return true;
            }
        }
        if (p.getWorld().equals(rift)) {
            Location back = returnPoint.remove(p.getUniqueId());
            if (back == null) back = Bukkit.getWorlds().get(0).getSpawnLocation();
            p.teleport(back);
            p.sendMessage(ChatColor.AQUA + "✦ Returned from the rift.");
        } else {
            returnPoint.put(p.getUniqueId(), p.getLocation());
            p.teleport(rift.getSpawnLocation());
            p.sendMessage(ChatColor.LIGHT_PURPLE + "✦ Through the veil...");
            p.sendMessage(ChatColor.GRAY + "  /rift again to return.");
        }
        return true;
    }

    private static String fmt(Location l) {
        return "(" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ")";
    }
}
