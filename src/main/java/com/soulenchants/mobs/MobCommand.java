package com.soulenchants.mobs;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /mob list           — list every custom mob with tier
 * /mob spawn <id>     — spawn one at your location
 * /mob spawn <id> <n> — spawn N copies
 * /mob killall        — wipe every active custom mob in range
 */
public class MobCommand implements CommandExecutor {

    private final SoulEnchants plugin;
    private final MobListener listener;

    public MobCommand(SoulEnchants plugin, MobListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.WHITE + MobRegistry.all().size()
                    + ChatColor.LIGHT_PURPLE + " custom mobs registered:");
            for (CustomMob.Tier t : CustomMob.Tier.values()) {
                sender.sendMessage(t.color + "  ── " + t.label + " " + ChatColor.GRAY + "──");
                for (CustomMob m : MobRegistry.byTier(t)) {
                    sender.sendMessage("§7    §f" + m.id + " §8| §7" + m.displayName
                            + " §8| §fHP " + m.maxHp + " §8| §f" + m.souls + " souls");
                }
            }
            return true;
        }

        if (sub.equals("spawn")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (!(sender instanceof Player)) { sender.sendMessage("§cPlayer only."); return true; }
            if (args.length < 2) { sender.sendMessage("§c/mob spawn <id> [count]"); return true; }
            CustomMob def = MobRegistry.get(args[1].toLowerCase());
            if (def == null) { sender.sendMessage("§cUnknown mob id. Try /mob list"); return true; }
            int count = args.length >= 3 ? safeInt(args[2], 1) : 1;
            Player p = (Player) sender;
            for (int i = 0; i < count; i++) {
                org.bukkit.Location loc = p.getLocation().add(
                        (Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3);
                org.bukkit.entity.LivingEntity le = def.spawn(loc);
                if (le != null) listener.track(le);
            }
            sender.sendMessage("§a✦ Spawned " + count + "× " + def.displayName);
            return true;
        }

        if (sub.equals("killall") || sub.equals("clear")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage("§cNo permission."); return true; }
            int n = 0;
            for (org.bukkit.World w : plugin.getServer().getWorlds()) {
                for (org.bukkit.entity.Entity e : new java.util.ArrayList<>(w.getEntities())) {
                    if (!(e instanceof org.bukkit.entity.LivingEntity)) continue;
                    if (CustomMob.idOf((org.bukkit.entity.LivingEntity) e) == null) continue;
                    e.remove();
                    n++;
                }
            }
            sender.sendMessage("§a✦ Cleared " + n + " custom mobs.");
            return true;
        }

        help(sender);
        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage("§5✦ §dMob commands:");
        s.sendMessage("§7  /mob list");
        s.sendMessage("§7  /mob spawn <id> [count] §8(admin)");
        s.sendMessage("§7  /mob killall §8(admin — wipe all custom mobs)");
    }

    private int safeInt(String s, int dflt) { try { return Integer.parseInt(s); } catch (Exception e) { return dflt; } }
}
