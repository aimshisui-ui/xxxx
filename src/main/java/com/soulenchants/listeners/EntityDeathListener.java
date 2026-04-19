package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class EntityDeathListener implements Listener {

    private final SoulEnchants plugin;
    public EntityDeathListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler
    public void onMobKill(EntityDeathEvent e) {
        if (e instanceof PlayerDeathEvent) return;
        LivingEntity entity = e.getEntity();
        Player killer = entity.getKiller();

        // Veilweaver kill
        if (plugin.getVeilweaverManager().isVeilweaver(entity)) {
            plugin.getVeilweaverManager().onVeilweaverDeath(killer);
            return;
        }
        // Iron Golem Boss kill
        if (plugin.getIronGolemManager().isIronGolemBoss(entity)) {
            plugin.getIronGolemManager().onIronGolemDeath(killer);
            return;
        }

        if (killer == null) return;

        // Quest mob hook (placeholder - actual quest tracking added in quest phase)
        // plugin.getQuestManager().onMobKill(killer, entity);

        // Regular mobs give nothing by default; bumpable in config
        long flat = plugin.getConfig().getLong("souls.per-mob-kill", 0);
        if (flat > 0) plugin.getSoulManager().add(killer, flat);
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        long souls = plugin.getConfig().getLong("souls.per-player-kill", 0);
        if (souls > 0) {
            plugin.getSoulManager().add(killer, souls);
            killer.sendMessage("§5✦ §d+" + souls + " Souls §7(player kill)");
        }
    }
}
