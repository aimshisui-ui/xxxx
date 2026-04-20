package com.soulenchants.quests;

import com.soulenchants.SoulEnchants;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Maps in-game events to {@link QuestEvent}s and routes them through the
 * {@link QuestManager}. Tutorial / daily progress flows from here.
 */
public class QuestEventListener implements Listener {

    private final SoulEnchants plugin;
    public QuestEventListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler
    public void onMobKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        // Boss kill (Veilweaver / Iron Golem managers handle their own guild points
        // in their death paths; Hollow King is a CustomMob so we award here.)
        boolean vw = plugin.getVeilweaverManager().isVeilweaver(e.getEntity());
        boolean ig = plugin.getIronGolemManager().isIronGolemBoss(e.getEntity());
        String customId = com.soulenchants.mobs.CustomMob.idOf(e.getEntity());
        boolean hk = "hollow_king".equals(customId);
        if (vw || ig || hk) {
            plugin.getQuestManager().onEvent(killer, QuestEvent.bossKilled(e.getEntity()));
            if (hk) {
                long pts = 0L;
                if (plugin.getGuildManager() != null) {
                    com.soulenchants.guilds.Guild g = plugin.getGuildManager().getByMember(killer.getUniqueId());
                    if (g != null) {
                        plugin.getGuildManager().awardPoints(g, 250L, "hollow king kill");
                        pts = 250L;
                    }
                }
                // Hollow King doesn't track per-player damage like Veilweaver/IronGolem,
                // so the FX shows killer-only credit. Souls reward is hard-coded here
                // to match the loot drop balance — bump if we tune the boss config.
                long hkSouls = 1500L;
                com.soulenchants.bosses.BossKillRewardFX.play(plugin, killer, null,
                        "The Hollow King", org.bukkit.ChatColor.RED,
                        hkSouls, 0L, pts, e.getEntity().getLocation());
            }
            return;
        }
        plugin.getQuestManager().onEvent(killer, QuestEvent.mobKill(e.getEntity()));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        plugin.getQuestManager().onEvent(e.getPlayer(),
                QuestEvent.blockBreak(e.getBlock().getType().name()));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String full = e.getMessage();
        if (full == null || !full.startsWith("/")) return;
        String first = full.substring(1).split(" ")[0].toLowerCase();
        plugin.getQuestManager().onEvent(e.getPlayer(), QuestEvent.commandRan(first));
    }
}
