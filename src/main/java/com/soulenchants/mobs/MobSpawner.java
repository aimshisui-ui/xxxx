package com.soulenchants.mobs;

import com.soulenchants.SoulEnchants;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Replaces vanilla hostile spawns with our custom mob roster.
 *
 *   - Spawn reasons NATURAL or SPAWNER → roll a custom mob to replace it
 *   - Tier weights bias toward early-game mobs unless the player is high-lifetime
 *   - 80% replacement rate (so vanilla still happens for variety)
 */
public class MobSpawner implements Listener {

    private static final Random RNG = new Random();
    private static final double REPLACEMENT_CHANCE = 0.80;

    private final SoulEnchants plugin;
    private final MobListener mobListener;

    public MobSpawner(SoulEnchants plugin, MobListener mobListener) {
        this.plugin = plugin;
        this.mobListener = mobListener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        // Don't loop on our own spawns
        if (CustomMob.idOf(e.getEntity()) != null) return;
        // Don't replace bosses, wandering villagers, animals
        switch (e.getSpawnReason()) {
            case NATURAL: case SPAWNER: case CHUNK_GEN:
                break;
            default: return;
        }

        // Only replace hostile entities
        if (!isHostileType(e.getEntity().getType())) return;

        if (RNG.nextDouble() > REPLACEMENT_CHANCE) return;

        CustomMob pick = pickForLocation(e.getLocation());
        if (pick == null) return;
        // Cancel original, spawn ours
        e.setCancelled(true);
        org.bukkit.scheduler.BukkitRunnable r = new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() {
                org.bukkit.entity.LivingEntity spawned = pick.spawn(e.getLocation());
                if (spawned != null) mobListener.track(spawned);
            }
        };
        r.runTaskLater(plugin, 1L);
    }

    private CustomMob pickForLocation(Location loc) {
        // Compute a weighted pool: cheap mobs more often, elites very rarely
        // Weight: EARLY=10, MID=4, LATE=1, ELITE=0.2
        // Weight by avg lifetime souls of nearby players (more lifetime = higher avg tier)
        double avgLifetime = 0;
        int n = 0;
        for (org.bukkit.entity.Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 80 * 80) {
                avgLifetime += plugin.getSoulManager().getLifetime(p);
                n++;
            }
        }
        if (n > 0) avgLifetime /= n;

        // Weights shift toward MID/LATE as avgLifetime grows
        double earlyWeight = 10;
        double midWeight   = avgLifetime > 5_000 ? 6 : 3;
        double lateWeight  = avgLifetime > 25_000 ? 3 : 1;
        double eliteWeight = avgLifetime > 100_000 ? 0.5 : 0.1;

        List<CustomMob> early = MobRegistry.byTier(CustomMob.Tier.EARLY);
        List<CustomMob> mid   = MobRegistry.byTier(CustomMob.Tier.MID);
        List<CustomMob> late  = MobRegistry.byTier(CustomMob.Tier.LATE);
        List<CustomMob> elite = MobRegistry.byTier(CustomMob.Tier.ELITE);

        double total = earlyWeight + midWeight + lateWeight + eliteWeight;
        double roll = RNG.nextDouble() * total;
        if (roll < earlyWeight)                 return rand(early);
        if (roll < earlyWeight + midWeight)     return rand(mid);
        if (roll < earlyWeight + midWeight + lateWeight) return rand(late);
        return rand(elite);
    }

    private static <T> T rand(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(RNG.nextInt(list.size()));
    }

    private static boolean isHostileType(org.bukkit.entity.EntityType t) {
        switch (t) {
            case ZOMBIE: case SKELETON: case CREEPER: case SPIDER: case CAVE_SPIDER:
            case WITCH: case ENDERMAN: case BLAZE: case PIG_ZOMBIE: case MAGMA_CUBE:
            case SILVERFISH: case GHAST: case SLIME:
                return true;
            default: return false;
        }
    }
}
