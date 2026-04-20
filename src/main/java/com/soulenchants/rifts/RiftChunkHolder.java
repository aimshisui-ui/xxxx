package com.soulenchants.rifts;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * One-shot chunk pinning for screenshot / event purposes. Loads every chunk
 * within `radiusBlocks` of `centerX, centerZ` in `world`, then cancels any
 * subsequent {@link ChunkUnloadEvent} for those chunks until release().
 *
 *   /rift loadchunks [radius]     hold(rift_world.spawn, radius)
 *   /rift releasechunks           release()
 *
 * Holding is per-process — wiped on plugin reload or server restart, which is
 * intentional: the cap on damage scope is "this admin is taking a picture and
 * forgot to release". Don't combine with `setKeepSpawnInMemory(false)` —
 * we flip that to true for the held world so spawn chunks also stay put.
 *
 * Cap on radius is 480 blocks (~30 chunks side, ~3600 chunks total) to keep
 * a 1.8.8 server from collapsing into a memory pit.
 */
public final class RiftChunkHolder implements Listener {

    public static final int MAX_RADIUS_BLOCKS = 480;

    /** Encoded chunk key: (chunkX << 32) | (chunkZ & 0xFFFFFFFFL). World scope is
     *  enforced separately so cross-world key collisions can't leak holds. */
    private static final Set<Long> heldKeys = new HashSet<>();
    private static volatile String heldWorldName = null;
    private static volatile boolean prevKeepSpawn = false;

    private static long key(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    /** Load every chunk within `radiusBlocks` of (centerX, centerZ) in `world`
     *  and pin them. Returns the count loaded. Releases any prior hold first
     *  so callers don't accidentally mix worlds. */
    public static int hold(World world, int centerX, int centerZ, int radiusBlocks) {
        if (world == null) return 0;
        if (radiusBlocks < 16) radiusBlocks = 16;
        if (radiusBlocks > MAX_RADIUS_BLOCKS) radiusBlocks = MAX_RADIUS_BLOCKS;
        release();
        heldWorldName = world.getName();
        prevKeepSpawn = world.getKeepSpawnInMemory();
        world.setKeepSpawnInMemory(true);

        int radiusChunks = (radiusBlocks + 15) / 16;
        int baseCx = centerX >> 4;
        int baseCz = centerZ >> 4;
        int loaded = 0;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                int cx = baseCx + dx, cz = baseCz + dz;
                try {
                    Chunk c = world.getChunkAt(cx, cz);
                    if (!c.isLoaded()) c.load(true);
                    heldKeys.add(key(cx, cz));
                    loaded++;
                } catch (Throwable ignored) {}
            }
        }
        return loaded;
    }

    public static int heldCount() { return heldKeys.size(); }
    public static String heldWorld() { return heldWorldName; }

    /** Release the hold — chunks may now unload normally. Restores the held
     *  world's keepSpawnInMemory flag to its pre-hold value. */
    public static void release() {
        if (heldWorldName != null) {
            World w = Bukkit.getWorld(heldWorldName);
            if (w != null) w.setKeepSpawnInMemory(prevKeepSpawn);
        }
        heldKeys.clear();
        heldWorldName = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUnload(ChunkUnloadEvent e) {
        if (heldKeys.isEmpty()) return;
        if (heldWorldName == null) return;
        if (!e.getWorld().getName().equals(heldWorldName)) return;
        long k = key(e.getChunk().getX(), e.getChunk().getZ());
        if (heldKeys.contains(k)) e.setCancelled(true);
    }
}
