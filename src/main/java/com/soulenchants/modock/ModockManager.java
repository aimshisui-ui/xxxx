package com.soulenchants.modock;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Owns the active {@link ModockBoss} instance + handles player entry, death,
 * and post-fight rewards. One Modock alive at a time, server-wide.
 */
public class ModockManager {

    private final SoulEnchants plugin;
    private final ModockSpawnConfig spawnCfg;
    private ModockBoss active;

    public ModockManager(SoulEnchants plugin, ModockSpawnConfig spawnCfg) {
        this.plugin = plugin;
        this.spawnCfg = spawnCfg;
    }

    public ModockBoss getActive() {
        if (active != null && !active.isAlive()) return null;
        return active;
    }
    public boolean isModock(LivingEntity e) {
        return active != null && active.getEntity() != null
                && active.getEntity().getUniqueId().equals(e.getUniqueId());
    }
    public ModockSpawnConfig getSpawnCfg() { return spawnCfg; }

    /** Begin a Modock encounter — TP `summoner` to phase 1 player spawn,
     *  spawn Modock at phase 1 boss spawn. */
    public boolean begin(Player summoner) {
        if (active != null && active.isAlive()) {
            summoner.sendMessage(ChatColor.RED + "✦ Modock is already in battle. Wait or kill him.");
            return false;
        }
        ModockSpawnConfig.Pair p1 = spawnCfg.get("phase1");
        if (p1 == null || p1.boss == null || p1.player == null) {
            summoner.sendMessage(ChatColor.RED + "✦ Modock arena isn't set up. Admin must run /modock setspawn boss + /modock setspawn player in each phase world.");
            return false;
        }
        active = new ModockBoss(plugin, spawnCfg);
        boolean ok = active.spawn();
        if (!ok) { active = null; return false; }
        // TP summoner + add to participants (boss listener tracks subsequent
        // damagers automatically)
        try { summoner.teleport(p1.player); } catch (Throwable ignored) {}
        active.getParticipants().add(summoner.getUniqueId());
        summoner.sendMessage(ChatColor.AQUA + "✦ You step into Atlantis. Modock awaits.");
        summoner.playSound(summoner.getLocation(), org.bukkit.Sound.PORTAL_TRIGGER, 1.0f, 0.6f);
        return true;
    }

    /** Called from listener when Modock dies. Distributes rewards + cleans up. */
    public void onDeath(Player killer) {
        if (active == null) return;
        ModockBoss boss = active;
        // Souls reward — comparable to Veilweaver (top end)
        long souls = 3500;
        if (killer != null) {
            plugin.getSoulManager().add(killer, souls);
            killer.sendMessage("§b✦ §3Modock falls. §f+" + souls + " souls");
        }
        // Top damager bonus
        Player top = topDamager(boss);
        long topGuildPts = 0L;
        if (top != null && top != killer) {
            plugin.getSoulManager().add(top, souls / 2);
            top.sendMessage("§b✦ §3Top damage reward: §f+" + (souls / 2) + " souls");
        }
        // Guild points (proportional)
        if (plugin.getGuildManager() != null) {
            java.util.Map<com.soulenchants.guilds.Guild, Double> byGuild = new java.util.HashMap<>();
            for (java.util.Map.Entry<java.util.UUID, Double> e : boss.getDamageDealt().entrySet()) {
                com.soulenchants.guilds.Guild g = plugin.getGuildManager().getByMember(e.getKey());
                if (g == null) continue;
                byGuild.merge(g, e.getValue(), Double::sum);
            }
            com.soulenchants.guilds.Guild killerGuild = killer == null ? null
                    : plugin.getGuildManager().getByMember(killer.getUniqueId());
            for (java.util.Map.Entry<com.soulenchants.guilds.Guild, Double> e : byGuild.entrySet()) {
                double pct = e.getValue() / ModockBoss.MAX_HP;
                if (pct < 0.10) continue;
                long pts = Math.max(1L, (long)(600L * Math.min(1.0, pct)));
                plugin.getGuildManager().awardPoints(e.getKey(), pts, "modock kill");
                if (killerGuild != null && e.getKey().equals(killerGuild)) topGuildPts = pts;
            }
        }
        // Cinematic
        com.soulenchants.bosses.BossKillRewardFX.play(plugin, killer, top,
                "Modock, King of Atlantis", ChatColor.AQUA,
                killer != null ? souls : 0L,
                top != null && top != killer ? souls / 2 : 0L,
                topGuildPts,
                boss.getEntity().getLocation());
        // Boss death broadcast (top damagers list)
        com.soulenchants.bosses.BossDeathBroadcast.broadcast(plugin,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Modock, King of Atlantis",
                ChatColor.AQUA, boss.getDamageDealt(), ModockBoss.MAX_HP);

        // Drop loot at corpse
        org.bukkit.Location loc = boss.getEntity().getLocation();
        loc.getWorld().dropItemNaturally(loc, com.soulenchants.shop.LootBox.item(
                com.soulenchants.shop.LootBox.Kind.BOSS));
        loc.getWorld().dropItemNaturally(loc, com.soulenchants.shop.LootBox.item(
                com.soulenchants.shop.LootBox.Kind.BOSS));

        // Return participants to main world after a 5s grace
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (java.util.UUID u : new java.util.ArrayList<>(boss.getParticipants())) {
                Player p = Bukkit.getPlayer(u);
                if (p == null || !p.isOnline()) continue;
                try { p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()); } catch (Throwable ignored) {}
                p.sendMessage(ChatColor.AQUA + "✦ You emerge from the deep, victorious.");
            }
            boss.stop(true);
            active = null;
        }, 100L);
    }

    private Player topDamager(ModockBoss boss) {
        java.util.UUID best = null;
        double bestDmg = 0;
        for (java.util.Map.Entry<java.util.UUID, Double> e : boss.getDamageDealt().entrySet()) {
            if (e.getValue() > bestDmg) { bestDmg = e.getValue(); best = e.getKey(); }
        }
        return best == null ? null : Bukkit.getPlayer(best);
    }

    public void abort() {
        if (active == null) return;
        for (java.util.UUID u : new java.util.ArrayList<>(active.getParticipants())) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                try { p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()); } catch (Throwable ignored) {}
                p.sendMessage(ChatColor.RED + "✦ The encounter was aborted. The deep retreats.");
            }
        }
        active.stop(false);
        active = null;
    }
}
