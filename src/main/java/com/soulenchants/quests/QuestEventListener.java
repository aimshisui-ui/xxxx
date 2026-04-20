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
        // Boss kill
        if (plugin.getVeilweaverManager().isVeilweaver(e.getEntity())
            || plugin.getIronGolemManager().isIronGolemBoss(e.getEntity())) {
            plugin.getQuestManager().onEvent(killer, QuestEvent.bossKilled(e.getEntity()));
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
