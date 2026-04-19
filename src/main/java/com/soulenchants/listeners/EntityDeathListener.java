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
            // Strip vanilla wither-skeleton drops (bones, coal, skull) and xp
            e.getDrops().clear();
            e.setDroppedExp(0);
            plugin.getVeilweaverManager().onVeilweaverDeath(killer);
            return;
        }
        // Iron Golem Boss kill
        if (plugin.getIronGolemManager().isIronGolemBoss(entity)) {
            // Strip vanilla iron golem drops (iron, poppy) and xp
            e.getDrops().clear();
            e.setDroppedExp(0);
            plugin.getIronGolemManager().onIronGolemDeath(killer);
            return;
        }
        // Veilweaver minions / echo clones — no vanilla drops
        de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(entity);
        if (nbt.hasKey("se_vw_minion") || nbt.hasKey("se_vw_clone")) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            return;
        }
        // Iron Sentinel — strip vanilla drops, award flat 30 souls to killer
        if (com.soulenchants.bosses.IronGolemMinions.ACTIVE_UUIDS.contains(entity.getUniqueId())) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            com.soulenchants.bosses.IronGolemMinions.ACTIVE_UUIDS.remove(entity.getUniqueId());
            if (killer != null) {
                plugin.getSoulManager().add(killer, 30);
                killer.sendMessage("§6✦ §e+30 Souls §7(Iron Sentinel)");
            }
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
