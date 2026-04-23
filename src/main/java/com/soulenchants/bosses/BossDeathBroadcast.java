package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Server-wide broadcast on boss kill — header, top-3 damagers with damage +
 * contribution %. Uses MessageStyle so every boss fight closes with the same
 * chromatic language as the rest of the plugin.
 */
public final class BossDeathBroadcast {

    private BossDeathBroadcast() {}

    public static void broadcast(SoulEnchants plugin, String bossName,
                                 ChatColor accent, Map<UUID, Double> damageMap, double bossMaxHp) {
        Bukkit.broadcastMessage(MessageStyle.RULE);
        Bukkit.broadcastMessage(MessageStyle.FRAME + "    "
                + accent + MessageStyle.BOLD + MessageStyle.SKULL + "  " + bossName
                + "  has fallen  " + MessageStyle.SKULL);
        Bukkit.broadcastMessage("");

        if (damageMap.isEmpty()) {
            Bukkit.broadcastMessage(MessageStyle.FRAME + "    " + MessageStyle.MUTED + "No damage was dealt.");
        } else {
            List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(damageMap.entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            Bukkit.broadcastMessage(MessageStyle.FRAME + "    " + MessageStyle.MUTED + "Top damagers:");
            // Gold / silver / bronze trophy chromatics
            String[] medals = {
                    MessageStyle.TIER_LEGENDARY + MessageStyle.BOLD + "① ",
                    MessageStyle.MUTED          + MessageStyle.BOLD + "② ",
                    MessageStyle.TIER_EPIC      + MessageStyle.BOLD + "③ "
            };
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                Map.Entry<UUID, Double> entry = sorted.get(i);
                Player p = Bukkit.getPlayer(entry.getKey());
                String name = p != null ? p.getName() : "Unknown";
                double dmg = entry.getValue();
                double pct = (dmg / bossMaxHp) * 100.0;
                Bukkit.broadcastMessage(String.format(
                        "%s    %s%s%-16s%s  %s%.1f%s dmg %s(%s%.1f%%%s)",
                        MessageStyle.FRAME,
                        medals[i], MessageStyle.VALUE, name, MessageStyle.FRAME,
                        MessageStyle.MUTED, dmg, MessageStyle.FRAME,
                        MessageStyle.FRAME, MessageStyle.VALUE, pct, MessageStyle.FRAME));
            }
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(MessageStyle.FRAME + "    " + MessageStyle.MUTED
                + "Loot has dropped at the corpse.");
        Bukkit.broadcastMessage(MessageStyle.RULE);
    }
}
