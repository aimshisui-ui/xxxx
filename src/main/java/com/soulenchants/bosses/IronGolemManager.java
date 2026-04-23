package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.enchants.EnchantTier;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.items.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class IronGolemManager {

    private final SoulEnchants plugin;
    private IronGolemBoss active;
    private final Random rng = new Random();

    public IronGolemManager(SoulEnchants plugin) { this.plugin = plugin; }

    public boolean summon(Location loc) {
        if (active != null && !active.getEntity().isDead()) return false;
        active = new IronGolemBoss(plugin, loc);
        active.start();
        announceSpawn(loc);
        return true;
    }

    /** Cinematic spawn announce — global broadcast + sound + title for nearby players. */
    private void announceSpawn(Location loc) {
        String div = "§6" + org.bukkit.ChatColor.STRIKETHROUGH + "                                          ";
        org.bukkit.Bukkit.broadcastMessage("");
        org.bukkit.Bukkit.broadcastMessage(div);
        org.bukkit.Bukkit.broadcastMessage("§6§l        ✦ THE IRONHEART COLOSSUS RISES ✦");
        org.bukkit.Bukkit.broadcastMessage("");
        org.bukkit.Bukkit.broadcastMessage("§8§o  \"Iron remembers every blow it took to forge it.\"");
        org.bukkit.Bukkit.broadcastMessage("");
        org.bukkit.Bukkit.broadcastMessage("§7  Coordinates  §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                + "§8  (" + loc.getWorld().getName() + ")");
        org.bukkit.Bukkit.broadcastMessage("§c  ⚔ Bring company. Bring blades. Bring everything.");
        org.bukkit.Bukkit.broadcastMessage(div);
        org.bukkit.Bukkit.broadcastMessage("");
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), org.bukkit.Sound.IRONGOLEM_DEATH, 0.8f, 0.5f);
            p.playSound(p.getLocation(), org.bukkit.Sound.ANVIL_LAND, 0.6f, 0.4f);
            if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distanceSquared(loc) <= 80 * 80) {
                com.soulenchants.lunar.LunarFx.sendTitle(p,
                        "§6§l✦ Ironheart Colossus ✦", "§e§oThe forge wakes.",
                        250L, 2200L, 500L, 1.3f);
            }
        }
    }

    public IronGolemBoss getActive() {
        return active;
    }

    /** Manual nuller — see VeilweaverManager.clearActive() for rationale. */
    public void clearActive() { this.active = null; }

    public boolean isIronGolemBoss(LivingEntity entity) {
        return active != null && active.getEntity().getUniqueId().equals(entity.getUniqueId());
    }

    public void onIronGolemDeath(Player killer) {
        IronGolemBoss b = getActive();
        if (b == null) return;
        long souls = plugin.getConfig().getLong("irongolem.souls-reward", 1500);
        if (killer != null) {
            ItemStack hand = killer.getItemInHand();
            int reaper = hand == null ? 0 : ItemUtil.getLevel(hand, "soulreaper");
            if (reaper > 0) {
                long bonus = (long) (souls * 0.5 * reaper);
                souls += bonus;
                killer.sendMessage("§5✦ §dSoul Reaper bonus: §f+" + bonus);
            }
            plugin.getSoulManager().add(killer, souls);
        }
        Player top = b.getTopDamager();
        if (top != null && (killer == null || !top.equals(killer))) {
            plugin.getSoulManager().add(top, souls / 2);
            top.sendMessage("§6✦ §eTop damage reward: §f+" + (souls / 2) + " Souls");
        }
        // Drop guaranteed Epic+ enchant book
        List<CustomEnchant> pool = new ArrayList<>();
        for (CustomEnchant e : EnchantRegistry.all()) {
            int t = e.getTier().ordinal();
            if (t >= EnchantTier.EPIC.ordinal() && t < EnchantTier.SOUL_ENCHANT.ordinal()) pool.add(e);
        }
        if (pool.isEmpty()) pool.addAll(EnchantRegistry.all());
        CustomEnchant chosen = pool.get(rng.nextInt(pool.size()));
        b.getEntity().getWorld().dropItemNaturally(b.getEntity().getLocation(),
                // Random level between 1 and maxLevel inclusive (uniform)
                ItemFactories.book(chosen, 1 + rng.nextInt(chosen.getMaxLevel())));
        // Drop the unique "Iron Heart" item
        b.getEntity().getWorld().dropItemNaturally(b.getEntity().getLocation(), ironHeart());
        // Full boss loot table
        com.soulenchants.loot.BossLootTable.dropIronGolem(b.getEntity().getLocation());
        // Config-driven extra drops from /ce loot editor
        if (plugin.getLootConfig() != null) {
            java.util.Random r = new java.util.Random();
            for (com.soulenchants.mobs.DropSpec ds : plugin.getLootConfig().bossDrops("irongolem")) {
                org.bukkit.inventory.ItemStack it = ds.roll(r);
                if (it != null) b.getEntity().getWorld().dropItemNaturally(b.getEntity().getLocation(), it);
            }
        }

        com.soulenchants.bosses.BossDeathBroadcast.broadcast(plugin,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Ironheart Colossus",
                ChatColor.GOLD, b.getDamageDealt(),
                IronGolemBoss.MAX_HP);

        long killerGuildPts = awardGuildPointsByDamage(b.getDamageDealt(), IronGolemBoss.MAX_HP, 350L,
                "ironheart kill", killer);

        com.soulenchants.bosses.BossKillRewardFX.play(plugin, killer, top,
                "Ironheart Colossus", ChatColor.GOLD,
                killer != null ? souls : 0L,
                top != null && top != killer ? souls / 2 : 0L,
                killerGuildPts,
                b.getEntity().getLocation());

        b.stop(true);
        active = null;
    }

    /** See VeilweaverManager.awardGuildPointsByDamage — returns the killer's
     *  guild's award (0 if none) for the cinematic display. */
    private long awardGuildPointsByDamage(java.util.Map<java.util.UUID, Double> dmgMap,
                                          double bossMaxHp, long basePoints, String reason,
                                          Player killer) {
        if (plugin.getGuildManager() == null || dmgMap == null || dmgMap.isEmpty()) return 0L;
        java.util.Map<com.soulenchants.guilds.Guild, Double> byGuild = new java.util.HashMap<>();
        for (java.util.Map.Entry<java.util.UUID, Double> e : dmgMap.entrySet()) {
            com.soulenchants.guilds.Guild g = plugin.getGuildManager().getByMember(e.getKey());
            if (g == null) continue;
            byGuild.merge(g, e.getValue(), Double::sum);
        }
        com.soulenchants.guilds.Guild killerGuild = killer == null ? null
                : plugin.getGuildManager().getByMember(killer.getUniqueId());
        long killerGuildPts = 0L;
        for (java.util.Map.Entry<com.soulenchants.guilds.Guild, Double> e : byGuild.entrySet()) {
            double pct = e.getValue() / bossMaxHp;
            if (pct < 0.10) continue;
            long pts = Math.max(1L, (long) (basePoints * Math.min(1.0, pct)));
            plugin.getGuildManager().awardPoints(e.getKey(), pts, reason);
            if (killerGuild != null && e.getKey().equals(killerGuild)) killerGuildPts = pts;
        }
        return killerGuildPts;
    }

    private ItemStack ironHeart() {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Iron Heart");
        meta.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  ",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "The still-warm core of",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "the Ironheart Colossus.",
                "",
                ChatColor.YELLOW + "» Trophy item from a boss kill",
                ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  "
        ));
        item.setItemMeta(meta);
        return item;
    }
}
