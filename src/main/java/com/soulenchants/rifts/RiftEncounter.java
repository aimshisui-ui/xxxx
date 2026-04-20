package com.soulenchants.rifts;

import com.soulenchants.SoulEnchants;
import com.soulenchants.mobs.CustomMob;
import com.soulenchants.mobs.MobRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hardcoded auto-encounter for the Cave Rift. Fires every time a rift
 * becomes ACTIVE, regardless of whether the admin has configured manual
 * spawn points. Manual /rift addspawn entries spawn IN ADDITION.
 *
 * Layout:
 *   - Hollow King boss spawns at the rift world's spawn location
 *   - 25 cave-themed mobs scattered in a 40-block radius around spawn,
 *     each placed on the highest solid block at a random (dx, dz)
 *   - Mob ids and counts: see ROSTER below
 */
public final class RiftEncounter {

    private static final Random RNG = new Random();

    /** mob_id -> count */
    private static final String[][] ROSTER = {
            { "shardling",         "10" },
            { "echo_stalker",      "5"  },
            { "dripstone_crawler", "5"  },
            { "gloommaw",          "3"  },
            { "ruinwraith",        "2"  },
    };

    private static final String BOSS_ID = "hollow_king";
    private static final int SPAWN_RADIUS = 35;
    private static final int MIN_DIST_FROM_SPAWN = 6;

    private RiftEncounter() {}

    /** Spawn the encounter; returns the list of LivingEntities spawned (UUIDs to track). */
    public static List<LivingEntity> spawn(SoulEnchants plugin, World rift) {
        List<LivingEntity> spawned = new ArrayList<>();
        Location spawnAt = rift.getSpawnLocation();

        // Boss at spawn (slightly raised so it doesn't suffocate)
        CustomMob boss = MobRegistry.get(BOSS_ID);
        if (boss != null) {
            Location bossLoc = topSolid(rift, spawnAt.getBlockX(), spawnAt.getBlockZ());
            if (bossLoc == null) bossLoc = spawnAt.clone();
            LivingEntity le = boss.spawn(bossLoc.add(0.5, 1, 0.5));
            if (le != null) spawned.add(le);
            plugin.getLogger().info("[rift] boss " + BOSS_ID + " spawned at " + bossLoc.getBlockX() + "," + bossLoc.getBlockY() + "," + bossLoc.getBlockZ());
        } else {
            plugin.getLogger().warning("[rift] boss '" + BOSS_ID + "' not registered!");
        }

        // Mobs scattered around
        for (String[] entry : ROSTER) {
            String id = entry[0];
            int count = Integer.parseInt(entry[1]);
            CustomMob cm = MobRegistry.get(id);
            if (cm == null) {
                plugin.getLogger().warning("[rift] mob '" + id + "' not registered, skipping");
                continue;
            }
            int placed = 0;
            for (int attempt = 0; attempt < count * 10 && placed < count; attempt++) {
                double angle = RNG.nextDouble() * Math.PI * 2;
                double r = MIN_DIST_FROM_SPAWN + RNG.nextDouble() * (SPAWN_RADIUS - MIN_DIST_FROM_SPAWN);
                int dx = (int) (Math.cos(angle) * r);
                int dz = (int) (Math.sin(angle) * r);
                Location at = topSolid(rift, spawnAt.getBlockX() + dx, spawnAt.getBlockZ() + dz);
                if (at == null) continue;
                LivingEntity le = cm.spawn(at.add(0.5, 1, 0.5));
                if (le != null) { spawned.add(le); placed++; }
            }
        }
        return spawned;
    }

    /** Find the highest solid block at (x, z), return location of the air just above. */
    private static Location topSolid(World w, int x, int z) {
        for (int y = 95; y > 1; y--) {
            Block b = w.getBlockAt(x, y, z);
            if (b.getType() != Material.AIR && b.getType().isSolid()) {
                return new Location(w, x, y, z);
            }
        }
        return null;
    }
}
