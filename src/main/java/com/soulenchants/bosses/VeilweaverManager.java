package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.attacks.ApocalypseInvuln;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.enchants.EnchantTier;
import com.soulenchants.items.ItemFactories;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VeilweaverManager {

    private final SoulEnchants plugin;
    private Veilweaver active;
    private final Random rng = new Random();

    public VeilweaverManager(SoulEnchants plugin) { this.plugin = plugin; }

    public boolean summon(Location loc) {
        if (active != null && !active.getEntity().isDead()) return false;
        active = new Veilweaver(plugin, loc);
        active.start();
        announceSpawn(loc);
        return true;
    }

    /** Cinematic spawn announce — global broadcast + sound + title for nearby players. */
    private void announceSpawn(Location loc) {
        String div = "§5" + org.bukkit.ChatColor.STRIKETHROUGH + "                                          ";
        org.bukkit.Bukkit.broadcastMessage("");
        org.bukkit.Bukkit.broadcastMessage(div);
        org.bukkit.Bukkit.broadcastMessage("§5§l        ✦ THE VEILWEAVER MANIFESTS ✦");
        org.bukkit.Bukkit.broadcastMessage("");
        org.bukkit.Bukkit.broadcastMessage("§8§o  \"She has not woken in an age. She is waking now.\"");
        org.bukkit.Bukkit.broadcastMessage("");
        org.bukkit.Bukkit.broadcastMessage("§7  Coordinates  §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                + "§8  (" + loc.getWorld().getName() + ")");
        org.bukkit.Bukkit.broadcastMessage("§c  ⚔ Bring company. Bring blades. Bring everything.");
        org.bukkit.Bukkit.broadcastMessage(div);
        org.bukkit.Bukkit.broadcastMessage("");
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), org.bukkit.Sound.WITHER_SPAWN, 0.7f, 0.3f);
            p.playSound(p.getLocation(), org.bukkit.Sound.ENDERDRAGON_GROWL, 0.5f, 0.6f);
            // Title only for players in the same world, within 80 blocks
            if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distanceSquared(loc) <= 80 * 80) {
                try { p.sendTitle("§5§l✦ The Veilweaver ✦", "§d§oShe has woken."); } catch (Throwable ignored) {}
            }
        }
    }

    public Veilweaver getActive() {
        return active;
    }

    /** Manual nuller — called by death handler. Don't auto-null in getActive(),
     *  because EntityDeathEvent fires while entity.isDead() is already true and
     *  the death handler still needs access to the boss object. */
    public void clearActive() { this.active = null; }

    public boolean isVeilweaver(LivingEntity entity) {
        return active != null && active.getEntity().getUniqueId().equals(entity.getUniqueId());
    }

    public boolean isInvulnerable() {
        Veilweaver vw = getActive();
        if (vw == null) return false;
        return vw.isInvulnerable() || ApocalypseInvuln.isInvuln(vw);
    }

    public void onVeilweaverDeath(Player killer) {
        Veilweaver vw = getActive();
        if (vw == null) return;
        long souls = plugin.getConfig().getLong("veilweaver.souls-reward", 2500);
        if (killer != null) {
            // Soul Reaper on killer's held weapon = bonus souls
            org.bukkit.inventory.ItemStack hand = killer.getItemInHand();
            int reaper = hand == null ? 0 : com.soulenchants.items.ItemUtil.getLevel(hand, "soulreaper");
            if (reaper > 0) {
                long bonus = (long) (souls * 0.5 * reaper);
                souls += bonus;
                killer.sendMessage("§5✦ §dSoul Reaper bonus: §f+" + bonus);
            }
            plugin.getSoulManager().add(killer, souls);
        }
        // Drop top damager's reward + a guaranteed legendary book
        Player top = vw.getTopDamager();
        if (top != null) {
            plugin.getSoulManager().add(top, souls / 2);
            top.sendMessage("§5✦ §dTop damage reward: §f+" + (souls / 2) + " Souls");
        }
        // Drop a legendary+ book at boss location
        List<CustomEnchant> pool = new ArrayList<>();
        for (CustomEnchant e : EnchantRegistry.all()) {
            if (e.getTier().ordinal() >= EnchantTier.EPIC.ordinal()) pool.add(e);
        }
        if (pool.isEmpty()) pool.addAll(EnchantRegistry.all());
        CustomEnchant chosen = pool.get(rng.nextInt(pool.size()));
        vw.getEntity().getWorld().dropItemNaturally(vw.getEntity().getLocation(),
                // Random level between 1 and maxLevel inclusive (uniform)
                ItemFactories.book(chosen, 1 + rng.nextInt(chosen.getMaxLevel())));
        // Full boss loot table
        com.soulenchants.loot.BossLootTable.dropVeilweaver(vw.getEntity().getLocation());
        // Config-driven extra drops from /ce loot editor
        if (plugin.getLootConfig() != null) {
            java.util.Random r = new java.util.Random();
            for (com.soulenchants.mobs.DropSpec ds : plugin.getLootConfig().bossDrops("veilweaver")) {
                org.bukkit.inventory.ItemStack it = ds.roll(r);
                if (it != null) vw.getEntity().getWorld().dropItemNaturally(vw.getEntity().getLocation(), it);
            }
        }

        com.soulenchants.bosses.BossDeathBroadcast.broadcast(plugin,
                org.bukkit.ChatColor.DARK_PURPLE + "" + org.bukkit.ChatColor.BOLD + "The Veilweaver",
                org.bukkit.ChatColor.DARK_PURPLE, vw.getDamageDealt(),
                Veilweaver.MAX_HP);

        awardGuildPointsByDamage(vw.getDamageDealt(), Veilweaver.MAX_HP, 500L, "veilweaver kill");

        vw.stop(true);
        active = null;
    }

    /** Award guild points proportional to each guild's contribution. Each guild
     *  receives points scaled by (their members' summed damage / boss maxHp).
     *  Top contributors share `basePoints`; only guilds with ≥10% contribution
     *  get rewarded so trivial scratch-hits don't farm points.
     */
    private void awardGuildPointsByDamage(java.util.Map<java.util.UUID, Double> dmgMap,
                                          double bossMaxHp, long basePoints, String reason) {
        if (plugin.getGuildManager() == null || dmgMap == null || dmgMap.isEmpty()) return;
        java.util.Map<com.soulenchants.guilds.Guild, Double> byGuild = new java.util.HashMap<>();
        for (java.util.Map.Entry<java.util.UUID, Double> e : dmgMap.entrySet()) {
            com.soulenchants.guilds.Guild g = plugin.getGuildManager().getByMember(e.getKey());
            if (g == null) continue;
            byGuild.merge(g, e.getValue(), Double::sum);
        }
        for (java.util.Map.Entry<com.soulenchants.guilds.Guild, Double> e : byGuild.entrySet()) {
            double pct = e.getValue() / bossMaxHp;
            if (pct < 0.10) continue;
            long pts = Math.max(1L, (long) (basePoints * Math.min(1.0, pct)));
            plugin.getGuildManager().awardPoints(e.getKey(), pts, reason);
        }
    }
}
