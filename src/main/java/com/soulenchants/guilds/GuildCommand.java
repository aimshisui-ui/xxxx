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
        to.sendMessage(ChatColor.LIGHT_PURPLE + "§l✦ Guild Commands");
        to.sendMessage(ChatColor.GRAY + "  /guild create <name>");
        to.sendMessage(ChatColor.GRAY + "  /guild invite <player>");
        to.sendMessage(ChatColor.GRAY + "  /guild join <name>");
        to.sendMessage(ChatColor.GRAY + "  /guild leave");
        to.sendMessage(ChatColor.GRAY + "  /guild disband §8(owner only)");
        to.sendMessage(ChatColor.GRAY + "  /guild vault §8(members only)");
        to.sendMessage(ChatColor.GRAY + "  /guild info");
        to.sendMessage(ChatColor.GRAY + "  /guild top");
    }
}
