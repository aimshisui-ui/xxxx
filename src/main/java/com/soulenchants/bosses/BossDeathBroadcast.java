package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Server-wide broadcast on boss kill: header, top 3 damagers with %.
 */
public final class BossDeathBroadcast {

    private BossDeathBroadcast() {}

    public static void broadcast(SoulEnchants plugin, String bossName,
                                 ChatColor accent, Map<UUID, Double> damageMap, double bossMaxHp) {
        Bukkit.broadcastMessage(accent + "═══════════════════════════════════");
        Bukkit.broadcastMessage(accent + "" + ChatColor.BOLD + "  ✦ " + bossName + " has fallen! ✦");
        Bukkit.broadcastMessage("");

        if (damageMap.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "  No damage was dealt.");
        } else {
            // Sort descending by damage
            List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(damageMap.entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            Bukkit.broadcastMessage(ChatColor.GRAY + "  Top damagers:");
            String[] medals = { ChatColor.GOLD + "①", ChatColor.GRAY + "②", ChatColor.YELLOW + "③" };
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                Map.Entry<UUID, Double> entry = sorted.get(i);
                Player p = Bukkit.getPlayer(entry.getKey());
                String name = p != null ? p.getName() : "Unknown";
                double dmg = entry.getValue();
                double pct = (dmg / bossMaxHp) * 100.0;
                Bukkit.broadcastMessage(String.format("  %s %s§f%-16s §7%.1f dmg §8(§f%.1f%%§8)",
                        medals[i], ChatColor.WHITE, name, dmg, pct));
            }
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GRAY + "  Loot has dropped at the corpse.");
        Bukkit.broadcastMessage(accent + "═══════════════════════════════════");
    }
}
