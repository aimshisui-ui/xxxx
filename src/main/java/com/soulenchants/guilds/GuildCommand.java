package com.soulenchants.guilds;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /guild create <name>      — found a guild (you become owner)
 * /guild invite <player>    — owner/member invites a player
 * /guild join <name>        — accept a pending invite
 * /guild leave              — leave your guild (owner must disband)
 * /guild disband            — owner-only; vault contents drop at owner's feet
 * /guild vault              — open the shared guild vault (members-only)
 * /guild top                — cached top-10 leaderboard
 * /guild info               — show your guild's roster + points
 */
public class GuildCommand implements CommandExecutor {

    private final SoulEnchants plugin;
    private static final int MAX_NAME_LEN = 16;

    public GuildCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String sub = args[0].toLowerCase();

        // /guild top works for everyone, including console
        if (sub.equals("top") || sub.equals("leaderboard")) { showTop(sender); return true; }

        if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
        Player p = (Player) sender;
        GuildManager mgr = plugin.getGuildManager();

        switch (sub) {
            case "ping": {
                // /g ping — broadcast an Apollo waypoint to every online guildmate
                // carrying the caller's position + their distance in the label.
                // Auto-removes after 5s so the HUD stays clean.
                Guild g = mgr.getByMember(p.getUniqueId());
                if (g == null) { p.sendMessage(ChatColor.RED + "You're not in a guild."); return true; }
                if (!com.soulenchants.lunar.LunarBridge.isAvailable()) {
                    p.sendMessage(ChatColor.RED + "Lunar API isn't loaded — install Apollo to use /g ping.");
                    return true;
                }
                final org.bukkit.Location origin = p.getLocation();
                final String pingName = "Ping: " + p.getName();
                int sent = 0;
                for (UUID memberId : g.getMembers()) {
                    Player mate = Bukkit.getPlayer(memberId);
                    if (mate == null || !mate.isOnline()) continue;
                    if (mate.equals(p)) continue; // no self-ping
                    String wpLabel;
                    if (mate.getWorld().equals(origin.getWorld())) {
                        wpLabel = pingName + " (" + (int) mate.getLocation().distance(origin) + "m)";
                    } else {
                        wpLabel = pingName + " (other world)";
                    }
                    com.soulenchants.lunar.LunarBridge.sendWaypoint(mate, wpLabel, origin, 0xE8B86A);
                    mate.sendMessage(ChatColor.GOLD + "✦ " + ChatColor.YELLOW + p.getName()
                            + ChatColor.GRAY + " pinged their location "
                            + ChatColor.WHITE + "(" + origin.getBlockX() + ", "
                            + origin.getBlockY() + ", " + origin.getBlockZ() + ")");
                    sent++;
                }
                if (sent == 0) {
                    p.sendMessage(ChatColor.YELLOW + "✦ No guildmates online to ping.");
                } else {
                    p.sendMessage(ChatColor.GOLD + "✦ Pinged your location to "
                            + ChatColor.YELLOW + sent + ChatColor.GOLD + " guildmate"
                            + (sent == 1 ? "" : "s") + ".");
                }
                // Auto-remove after 5 seconds
                final java.util.Set<UUID> receivers = new java.util.HashSet<>(g.getMembers());
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override public void run() {
                        for (UUID u : receivers) {
                            Player mate = Bukkit.getPlayer(u);
                            if (mate != null && mate.isOnline()) {
                                com.soulenchants.lunar.LunarBridge.clearWaypoint(mate, pingName);
                            }
                        }
                    }
                }.runTaskLater(plugin, 100L);
                return true;
            }
            case "create": {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /guild create <name>"); return true; }
                String name = args[1];
                if (name.length() > MAX_NAME_LEN || !name.matches("[A-Za-z0-9_]+")) {
                    p.sendMessage(ChatColor.RED + "Name must be 1-" + MAX_NAME_LEN + " chars, alphanumeric/underscore.");
                    return true;
                }
                if (mgr.getByMember(p.getUniqueId()) != null) {
                    p.sendMessage(ChatColor.RED + "You're already in a guild — leave first.");
                    return true;
                }
                if (mgr.get(name) != null) {
                    p.sendMessage(ChatColor.RED + "A guild with that name already exists.");
                    return true;
                }
                Guild g = mgr.create(name, p);
                if (g == null) { p.sendMessage(ChatColor.RED + "Could not create guild."); return true; }
                Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "✦ " + p.getName()
                        + " founded the guild §d§l" + name + ChatColor.LIGHT_PURPLE + "!");
                return true;
            }
            case "invite": {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /guild invite <player>"); return true; }
                Guild g = mgr.getByMember(p.getUniqueId());
                if (g == null) { p.sendMessage(ChatColor.RED + "You're not in a guild."); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { p.sendMessage(ChatColor.RED + "Player not online."); return true; }
                if (g.isFull()) { p.sendMessage(ChatColor.RED + "Guild is full (max " + Guild.MAX_MEMBERS + ")."); return true; }
                if (mgr.getByMember(target.getUniqueId()) != null) {
                    p.sendMessage(ChatColor.RED + target.getName() + " is already in a guild."); return true;
                }
                mgr.invite(g, target.getUniqueId());
                target.sendMessage(ChatColor.LIGHT_PURPLE + "✦ §d" + p.getName() + " §rinvited you to guild §d§l"
                        + g.getName() + ChatColor.GRAY + " — type §f/guild join " + g.getName() + ChatColor.GRAY + " to accept.");
                p.sendMessage(ChatColor.GREEN + "✦ Invite sent to " + target.getName() + ".");
                return true;
            }
            case "join": {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /guild join <name>"); return true; }
                int code = mgr.join(p, args[1]);
                switch (code) {
                    case 0:
                        Guild g = mgr.get(args[1]);
                        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "✦ " + p.getName()
                                + " joined guild §d§l" + g.getName());
                        break;
                    case 1: p.sendMessage(ChatColor.RED + "That guild is full."); break;
                    case 2: p.sendMessage(ChatColor.RED + "You don't have an invite to that guild."); break;
                    case 3: p.sendMessage(ChatColor.RED + "You're already in a guild — leave first."); break;
                    case 4: p.sendMessage(ChatColor.RED + "No guild by that name."); break;
                }
                return true;
            }
            case "leave": {
                int code = mgr.leave(p);
                if (code == 0) p.sendMessage(ChatColor.YELLOW + "✦ Left your guild.");
                else if (code == 1) p.sendMessage(ChatColor.RED + "You're the owner — use §f/guild disband§c instead.");
                else p.sendMessage(ChatColor.RED + "You're not in a guild.");
                return true;
            }
            case "disband": {
                if (mgr.disband(p)) {
                    p.sendMessage(ChatColor.GOLD + "✦ Guild disbanded. Vault contents dropped at your feet.");
                } else {
                    p.sendMessage(ChatColor.RED + "You must be the OWNER of a guild to disband it.");
                }
                return true;
            }
            case "vault": {
                Guild g = mgr.getByMember(p.getUniqueId());
                if (g == null) {
                    p.sendMessage(ChatColor.RED + "You must be in a guild to access the vault.");
                    return true;
                }
                p.openInventory(g.getVault());
                return true;
            }
            case "info": {
                Guild g = mgr.getByMember(p.getUniqueId());
                if (g == null) { p.sendMessage(ChatColor.RED + "You're not in a guild."); return true; }
                p.sendMessage(ChatColor.LIGHT_PURPLE + "§l✦ Guild: §d§l" + g.getName());
                p.sendMessage(ChatColor.GRAY + "  Owner: §f" + Bukkit.getOfflinePlayer(g.getOwner()).getName());
                p.sendMessage(ChatColor.GRAY + "  Members (" + g.getMembers().size() + "/" + Guild.MAX_MEMBERS + "):");
                for (UUID m : g.getMembers()) {
                    String name = Bukkit.getOfflinePlayer(m).getName();
                    Player online = Bukkit.getPlayer(m);
                    p.sendMessage(ChatColor.DARK_GRAY + "   - " + (online != null ? ChatColor.GREEN : ChatColor.GRAY) + name);
                }
                p.sendMessage(ChatColor.GRAY + "  Points: §e" + g.getPoints());
                return true;
            }
            default:
                help(sender);
                return true;
        }
    }

    private void showTop(CommandSender to) {
        java.util.List<GuildManager.TopEntry> top = plugin.getGuildManager().getTopCached();
        to.sendMessage(ChatColor.LIGHT_PURPLE + "§l✦ Guild Top 10 ✦");
        if (top.isEmpty()) {
            to.sendMessage(ChatColor.GRAY + "  (no guilds yet)");
            return;
        }
        int rank = 1;
        for (GuildManager.TopEntry e : top) {
            ChatColor c = rank == 1 ? ChatColor.GOLD : rank == 2 ? ChatColor.GRAY
                    : rank == 3 ? ChatColor.DARK_RED : ChatColor.WHITE;
            to.sendMessage(c + "  #" + rank + ChatColor.DARK_GRAY + " — "
                    + ChatColor.LIGHT_PURPLE + e.name + ChatColor.GRAY + " (" + e.memberCount + " members) "
                    + ChatColor.YELLOW + e.points + " pts");
            rank++;
        }
    }

    private void help(CommandSender to) {
        com.soulenchants.style.Chat.banner(to, "Guild " + com.soulenchants.style.MessageStyle.FRAME + "commands");
        row(to, "/guild create <name>",        "found a new guild");
        row(to, "/guild invite <player>",      "invite a player");
        row(to, "/guild join <name>",          "accept an invite");
        row(to, "/guild leave",                "leave your guild");
        row(to, "/guild disband",              "owner only");
        row(to, "/guild vault",                "shared storage");
        row(to, "/guild info",                 "roster + stats");
        row(to, "/guild top",                  "leaderboard");
        row(to, "/guild ping  " + com.soulenchants.style.MessageStyle.FRAME + "| " +
                com.soulenchants.style.MessageStyle.VALUE + "/g ping",
                com.soulenchants.style.MessageStyle.TIER_RARE + "v1.1 " + com.soulenchants.style.MessageStyle.MUTED
                        + "ping your location to every online guildmate (5s, Lunar)");
        com.soulenchants.style.Chat.rule(to);
    }

    private static void row(CommandSender s, String cmd, String desc) {
        s.sendMessage(com.soulenchants.style.MessageStyle.FRAME + "   "
                + com.soulenchants.style.MessageStyle.ARROW + " "
                + com.soulenchants.style.MessageStyle.VALUE + cmd
                + com.soulenchants.style.MessageStyle.FRAME + "  "
                + com.soulenchants.style.MessageStyle.BAR + "  "
                + (desc.startsWith("§") ? desc : com.soulenchants.style.MessageStyle.MUTED + desc));
    }
}
