package com.soulenchants.util;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single source of truth for every temporary-block placement in the plugin.
 *
 *   TempBlockTracker.place(loc, Material.COBWEB, 80L, null)
 *         → block is placed now, previous block is stored, restored in 80 ticks
 *   TempBlockTracker.place(loc, Material.COBBLESTONE, 200L, "irongolem_wall")
 *         → same, but tagged so IronGolemBoss can force-restore its group early
 *
 * Under the hood: a ConcurrentHashMap<UUID, Entry> + a single 1-Hz sweeper
 * task that restores expired entries. Saves all live entries to a YAML file
 * on every add/remove so a hard crash still gets a clean restore on next
 * boot — IronGolemBoss's cobblestone walls no longer become permanent
 * world damage if the server dies mid-fight.
 */
public final class TempBlockTracker {

    private static SoulEnchants plugin;
    private static File saveFile;

    /** Live entries keyed by a generated UUID so we can cancel individually. */
    private static final java.util.Map<UUID, Entry> LIVE = new ConcurrentHashMap<UUID, Entry>();

    private static BukkitRunnable sweeper;

    private TempBlockTracker() {}

    public static final class Entry {
        public final UUID   id;
        public final Location loc;
        public final Material placed;
        @SuppressWarnings("deprecation")
        public final byte     placedData;
        public final Material prior;
        @SuppressWarnings("deprecation")
        public final byte     priorData;
        public final long     expiresAt;
        public final String   tag;

        Entry(UUID id, Location loc, Material placed, byte placedData,
              Material prior, byte priorData, long expiresAt, String tag) {
            this.id = id; this.loc = loc;
            this.placed = placed; this.placedData = placedData;
            this.prior = prior;   this.priorData = priorData;
            this.expiresAt = expiresAt; this.tag = tag;
        }
    }

    public static void install(SoulEnchants se) {
        plugin = se;
        saveFile = new File(plugin.getDataFolder(), "tempblocks.yml");
        plugin.getDataFolder().mkdirs();
        // Crash recovery — if saved entries exist, restore them right now,
        // BEFORE any boss tries to place new ones. Stale entries from a
        // prior crash are either restored immediately (past-expiry) or
        // respawn into LIVE for normal tick-driven cleanup.
        recoverFromDisk();
        startSweeper();
    }

    /** Place a temporary block. Returns the tracked entry's UUID so callers
     *  that want to force-restore early (e.g. boss despawn, phase change)
     *  can do so via {@link #restore(UUID)}. */
    @SuppressWarnings("deprecation")
    public static UUID place(Location loc, Material placed, byte placedData,
                              long durationTicks, String tag) {
        if (loc == null || loc.getWorld() == null) return null;
        Block b = loc.getBlock();
        Material prior = b.getType();
        byte priorData = b.getData();
        b.setType(placed);
        if (placedData != 0) b.setData(placedData);
        UUID id = UUID.randomUUID();
        long expires = System.currentTimeMillis() + durationTicks * 50L;
        Entry e = new Entry(id, loc.clone(), placed, placedData, prior, priorData, expires, tag);
        LIVE.put(id, e);
        saveToDisk();
        return id;
    }

    /** Convenience overload for the common "data = 0" case. */
    public static UUID place(Location loc, Material placed, long durationTicks, String tag) {
        return place(loc, placed, (byte) 0, durationTicks, tag);
    }

    /** Force-restore a specific entry early — returns true if something was
     *  actually restored. */
    public static boolean restore(UUID id) {
        Entry e = LIVE.remove(id);
        if (e == null) return false;
        restoreEntry(e);
        saveToDisk();
        return true;
    }

    /** Force-restore every entry with the given tag. Used by boss-fight
     *  cleanup: IronGolemBoss.stop() calls restoreByTag("irongolem_wall"). */
    public static int restoreByTag(String tag) {
        if (tag == null) return 0;
        int restored = 0;
        Iterator<java.util.Map.Entry<UUID, Entry>> it = LIVE.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next().getValue();
            if (tag.equals(e.tag)) {
                restoreEntry(e);
                it.remove();
                restored++;
            }
        }
        if (restored > 0) saveToDisk();
        return restored;
    }

    /** Restore EVERY live entry immediately. Called from SoulEnchants.onDisable. */
    public static int restoreAll() {
        int restored = 0;
        for (Entry e : new ArrayList<Entry>(LIVE.values())) {
            restoreEntry(e);
            restored++;
        }
        LIVE.clear();
        saveToDisk();
        return restored;
    }

    public static int liveCount() { return LIVE.size(); }

    // ───────────────────────────── internal ─────────────────────────────

    @SuppressWarnings("deprecation")
    private static void restoreEntry(Entry e) {
        try {
            Block b = e.loc.getBlock();
            // Only restore if the block is still what we placed — if a player
            // mined/replaced it, don't overwrite their work.
            if (b.getType() == e.placed) {
                b.setType(e.prior);
                if (e.priorData != 0) b.setData(e.priorData);
            }
        } catch (Throwable ignored) {}
    }

    private static void startSweeper() {
        sweeper = new BukkitRunnable() {
            @Override public void run() {
                if (LIVE.isEmpty()) return;
                long now = System.currentTimeMillis();
                boolean dirty = false;
                Iterator<java.util.Map.Entry<UUID, Entry>> it = LIVE.entrySet().iterator();
                while (it.hasNext()) {
                    Entry e = it.next().getValue();
                    if (e.expiresAt <= now) {
                        restoreEntry(e);
                        it.remove();
                        dirty = true;
                    }
                }
                if (dirty) saveToDisk();
            }
        };
        sweeper.runTaskTimer(plugin, 20L, 20L);   // 1 Hz
    }

    public static void shutdown() {
        if (sweeper != null) {
            try { sweeper.cancel(); } catch (Throwable ignored) {}
            sweeper = null;
        }
    }

    // ────────────────────────── persistence ──────────────────────────

    private static void saveToDisk() {
        if (saveFile == null) return;
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            int idx = 0;
            for (Entry e : LIVE.values()) {
                String k = "e" + (idx++);
                yaml.set(k + ".world", e.loc.getWorld().getName());
                yaml.set(k + ".x", e.loc.getBlockX());
                yaml.set(k + ".y", e.loc.getBlockY());
                yaml.set(k + ".z", e.loc.getBlockZ());
                yaml.set(k + ".placed", e.placed.name());
                yaml.set(k + ".placedData", (int) e.placedData);
                yaml.set(k + ".prior", e.prior.name());
                yaml.set(k + ".priorData", (int) e.priorData);
                yaml.set(k + ".expiresAt", e.expiresAt);
                if (e.tag != null) yaml.set(k + ".tag", e.tag);
            }
            yaml.save(saveFile);
        } catch (IOException ignored) {}
    }

    @SuppressWarnings("deprecation")
    private static void recoverFromDisk() {
        if (saveFile == null || !saveFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(saveFile);
        long now = System.currentTimeMillis();
        int restoredExpired = 0;
        int resumedLive = 0;
        for (String key : yaml.getKeys(false)) {
            String worldName = yaml.getString(key + ".world");
            World w = worldName == null ? null : Bukkit.getWorld(worldName);
            if (w == null) continue;
            int x = yaml.getInt(key + ".x"), y = yaml.getInt(key + ".y"), z = yaml.getInt(key + ".z");
            Material placed   = safeMaterial(yaml.getString(key + ".placed"));
            byte placedData   = (byte) yaml.getInt(key + ".placedData", 0);
            Material prior    = safeMaterial(yaml.getString(key + ".prior"));
            byte priorData    = (byte) yaml.getInt(key + ".priorData", 0);
            long expiresAt    = yaml.getLong(key + ".expiresAt", 0L);
            String tag        = yaml.getString(key + ".tag", null);
            if (placed == null || prior == null) continue;
            Location loc = new Location(w, x, y, z);
            // Past expiry — restore immediately and move on.
            if (expiresAt <= now) {
                Block b = loc.getBlock();
                if (b.getType() == placed) {
                    b.setType(prior);
                    if (priorData != 0) b.setData(priorData);
                }
                restoredExpired++;
                continue;
            }
            // Still live — resume into LIVE with the remaining duration.
            UUID id = UUID.randomUUID();
            Entry e = new Entry(id, loc, placed, placedData, prior, priorData, expiresAt, tag);
            LIVE.put(id, e);
            resumedLive++;
        }
        if (restoredExpired + resumedLive > 0) {
            Bukkit.getLogger().info("[SoulEnchants] Crash-recovery: restored " + restoredExpired
                    + " expired temp-blocks + resumed " + resumedLive + " live tracked blocks.");
        }
        saveToDisk();  // Overwrite file with reconciled state.
    }

    private static Material safeMaterial(String n) {
        if (n == null) return null;
        try { return Material.valueOf(n); } catch (IllegalArgumentException e) { return null; }
    }
}
