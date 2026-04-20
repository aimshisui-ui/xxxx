package com.soulenchants.currency;

import org.bukkit.Location;
import org.bukkit.entity.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-mob soul drop table + anti-abuse rules.
 *
 * Soul awards run through {@link #compute} from EntityDeathListener so:
 *   - mob type → base value
 *   - baby mobs → flat 1
 *   - mobs spawned from spawners → halved (set via {@link #markSpawner})
 *   - mobs killed >100 blocks from any player → 0 (anti-AFK)
 *   - mobs at full HP one-shot → halved (anti-cheese)
 *   - Soul Reaper bonus is applied externally before adding via SoulManager
 */
public final class MobSoulRules {

    /** UUIDs of entities that originated from a vanilla spawner block. */
    public static final Set<UUID> FROM_SPAWNER = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private MobSoulRules() {}

    public static void markSpawner(UUID id) { FROM_SPAWNER.add(id); }
    public static void unmarkSpawner(UUID id) { FROM_SPAWNER.remove(id); }

    /** Base souls for the mob type. 0 means no drop (peaceful, owned, etc.) */
    public static int baseFor(LivingEntity entity) {
        if (entity instanceof Tameable && ((Tameable) entity).getOwner() != null) return 0;
        if (entity instanceof Zombie) {
            Zombie z = (Zombie) entity;
            if (z.isBaby()) return 1;
            if (z instanceof PigZombie) return 6;
            if (z instanceof Villager) return 0;
            return 3;
        }
        if (entity instanceof Skeleton) {
            Skeleton s = (Skeleton) entity;
            return s.getSkeletonType() == Skeleton.SkeletonType.WITHER ? 12 : 4;
        }
        if (entity instanceof CaveSpider) return 5;
        if (entity instanceof Spider)     return 3;
        if (entity instanceof Creeper)    return 8;
        if (entity instanceof Witch)      return 12;
        if (entity instanceof Enderman)   return 15;
        if (entity instanceof Blaze)      return 10;
        if (entity instanceof MagmaCube) {
            MagmaCube m = (MagmaCube) entity;
            return m.getSize() <= 1 ? 2 : 4;
        }
        if (entity instanceof Slime) {
            Slime s = (Slime) entity;
            return s.getSize() <= 1 ? 1 : 2;
        }
        if (entity instanceof Ghast)      return 9;
        if (entity instanceof Silverfish) return 2;
        if (entity instanceof EnderDragon) return 2500;
        if (entity instanceof Wither)     return 1750;
        return 0; // peaceful or unknown
    }

    public static class Result {
        public final int amount;
        public final boolean afk;
        public final boolean spawnerHalved;
        public final boolean fullHpOneShot;
        public Result(int amount, boolean afk, boolean spawnerHalved, boolean fullHpOneShot) {
            this.amount = amount;
            this.afk = afk;
            this.spawnerHalved = spawnerHalved;
            this.fullHpOneShot = fullHpOneShot;
        }
    }

    /**
     * Compute the souls to award. Caller is responsible for:
     *   - rejecting mob-vs-mob kills (no Player killer)
     *   - applying tier bonus (+1 at Silver+)
     *   - applying Soul Reaper bonus
     *   - actually adding via SoulManager.add()
     */
    public static Result compute(LivingEntity entity, Player killer, double dealtDamage) {
        int base = baseFor(entity);
        if (base <= 0) return new Result(0, false, false, false);

        // AFK check: any player within 100 blocks (besides killer) keeps it valid
        Location loc = entity.getLocation();
        boolean nearby = false;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 100 * 100) { nearby = true; break; }
        }
        if (!nearby) return new Result(0, true, false, false);

        boolean spawnerHalved = FROM_SPAWNER.remove(entity.getUniqueId());
        if (spawnerHalved) base = Math.max(1, base / 2);

        boolean oneShot = dealtDamage >= entity.getMaxHealth();
        if (oneShot) base = Math.max(1, base / 2);

        return new Result(base, false, spawnerHalved, oneShot);
    }
}
