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
        // Oakenheart kill — reagents + unique crown + chance boss-gear + souls
        if (plugin.getOakenheartManager() != null
                && plugin.getOakenheartManager().isOakenheart(entity)) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            com.soulenchants.bosses.oakenheart.OakenheartBoss b =
                    plugin.getOakenheartManager().getActive();
            java.util.Random rng = new java.util.Random();
            org.bukkit.Location dropLoc = entity.getLocation();
            // Guaranteed
            entity.getWorld().dropItemNaturally(dropLoc, com.soulenchants.loot.BossLootItems.heartwood(8));
            entity.getWorld().dropItemNaturally(dropLoc, com.soulenchants.loot.BossLootItems.oakenCrown());
            // Chance-based
            if (rng.nextDouble() < 0.60)  entity.getWorld().dropItemNaturally(dropLoc, com.soulenchants.loot.BossLootItems.verdantTear(2));
            if (rng.nextDouble() < 0.30)  entity.getWorld().dropItemNaturally(dropLoc, com.soulenchants.loot.BossLootItems.oakensapEssence(1));
            if (rng.nextDouble() < 0.08)  entity.getWorld().dropItemNaturally(dropLoc, com.soulenchants.loot.BossLootItems.briarMantle());
            if (rng.nextDouble() < 0.05)  entity.getWorld().dropItemNaturally(dropLoc, com.soulenchants.loot.BossLootItems.thornboundGauntlet());
            // Guild points + souls reward
            if (killer != null) {
                int reward = 2500 + rng.nextInt(1500);
                plugin.getSoulManager().add(killer, reward);
                killer.sendMessage("§2§l✦ +" + reward + " Souls §7(Oakenheart killing blow)");
            }
            plugin.getOakenheartManager().clearActive();
            if (b != null) b.stop(true);
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

        if (killer == null) {
            // Mob killed by mob, environment, etc — clean up spawner tag and bail
            com.soulenchants.currency.MobSoulRules.unmarkSpawner(entity.getUniqueId());
            return;
        }

        // Custom mobs: MobListener handles souls + drops. Skip vanilla logic here.
        if (com.soulenchants.mobs.CustomMob.idOf(entity) != null) {
            com.soulenchants.currency.MobSoulRules.unmarkSpawner(entity.getUniqueId());
            return;
        }

        // Compute base souls + apply anti-abuse rules
        com.soulenchants.currency.MobSoulRules.Result result =
                com.soulenchants.currency.MobSoulRules.compute(entity, killer, entity.getMaxHealth());
        if (result.amount <= 0) return;

        int total = result.amount;

        // Tier bonus: Silver+ adds +1 per kill
        com.soulenchants.currency.SoulTier tier = plugin.getSoulManager().getTier(killer);
        if (tier.grantsBonusPerKill()) total += 1;

        // Soul Reaper bonus on held weapon
        org.bukkit.inventory.ItemStack hand = killer.getItemInHand();
        int reaper = hand == null ? 0 : com.soulenchants.items.ItemUtil.getLevel(hand, "soulreaper");
        if (reaper > 0) total += (int) Math.ceil(result.amount * 0.25 * reaper);

        // Combat bonus: killer took damage in last 10s = +20%
        long lastHurt = killer.getLastDamage() > 0 ? (long)(System.currentTimeMillis() - killer.getNoDamageTicks() * 50L) : 0L;
        // Cheap proxy: if killer's noDamageTicks > 0, they took a hit recently
        if (killer.getNoDamageTicks() > 0 && killer.getNoDamageTicks() < 200) {
            total = (int) Math.ceil(total * 1.2);
        }

        plugin.getSoulManager().add(killer, total);
        com.soulenchants.lunar.LunarFx.floatingText(plugin, entity.getLocation(),
                org.bukkit.ChatColor.LIGHT_PURPLE + "+" + total + " §dSoul" + (total == 1 ? "" : "s"), 40);
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
