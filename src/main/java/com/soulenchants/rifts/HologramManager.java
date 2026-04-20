package com.soulenchants.rifts;

import com.soulenchants.SoulEnchants;
import de.tr7zw.changeme.nbtapi.NBTEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.*;

/**
 * Manages the physical ArmorStand entities behind each persistent hologram.
 *
 * Lifecycle:
 *   On plugin enable:
 *     1. Scan all loaded worlds for "orphan" invisible-marker ArmorStands
 *        with CustomNameVisible — these are legacy holograms (e.g. from
 *        /rift hologram before persistence existed). Import each as a new
 *        single-line HologramConfig entry, tag the ArmorStand with the UUID.
 *     2. For every persisted entry that lacks a live ArmorStand, spawn one.
 *
 *   When player adds/edits/removes a hologram:
 *     - Mutate HologramConfig, then respawn the affected ArmorStand(s).
 */
public final class HologramManager {

    private static final String NBT_TAG = "se_hologram";
    private static final double LINE_SPACING = 0.26;

    private final SoulEnchants plugin;
    private final HologramConfig config;
    /** id -> list of ArmorStands (one per line, top-to-bottom). */
    private final Map<UUID, List<ArmorStand>> live = new HashMap<>();

    public HologramManager(SoulEnchants plugin, HologramConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** Run once after worlds are loaded — imports legacy stands + spawns all saved holograms. */
    public void bootstrap() {
        int imported = importOrphans();
        if (imported > 0) {
            plugin.getLogger().info("[holograms] imported " + imported + " legacy hologram line(s).");
        }
        // Spawn stands for any config entry that doesn't already have a live stand
        for (HologramConfig.Entry e : config.all()) {
            if (!live.containsKey(e.id)) spawnFor(e);
        }
    }

    public HologramConfig config() { return config; }
    public Collection<UUID> liveIds() { return live.keySet(); }

    /** Create a new hologram and spawn it. */
    public HologramConfig.Entry create(Location anchor, List<String> lines) {
        HologramConfig.Entry e = config.add(anchor, lines);
        spawnFor(e);
        return e;
    }

    public void updateLines(UUID id, List<String> newLines) {
        config.updateLines(id, newLines);
        // Respawn from scratch — simpler than in-place when line count changes
        despawn(id);
        HologramConfig.Entry e = config.get(id);
        if (e != null) spawnFor(e);
    }

    public void delete(UUID id) {
        despawn(id);
        config.remove(id);
    }

    public HologramConfig.Entry nearest(Location loc, double maxDistance) {
        HologramConfig.Entry best = null;
        double bestSq = maxDistance * maxDistance;
        for (HologramConfig.Entry e : config.all()) {
            if (!e.world.equals(loc.getWorld().getName())) continue;
            double dx = e.x - loc.getX(), dy = e.y - loc.getY(), dz = e.z - loc.getZ();
            double d = dx*dx + dy*dy + dz*dz;
            if (d < bestSq) { bestSq = d; best = e; }
        }
        return best;
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private void spawnFor(HologramConfig.Entry e) {
        Location anchor = e.toLocation();
        if (anchor == null) return;
        List<ArmorStand> line = new ArrayList<>();
        Location cursor = anchor.clone();
        for (String text : e.lines) {
            ArmorStand as = spawnLine(cursor, text);
            tag(as, e.id);
            line.add(as);
            cursor.subtract(0, LINE_SPACING, 0);
        }
        live.put(e.id, line);
    }

    private ArmorStand spawnLine(Location at, String text) {
        ArmorStand as = (ArmorStand) at.getWorld().spawnEntity(at, org.bukkit.entity.EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setGravity(false);
        as.setMarker(true);
        as.setBasePlate(false);
        as.setArms(false);
        as.setRemoveWhenFarAway(false);
        as.setCustomName(text);
        as.setCustomNameVisible(true);
        return as;
    }

    private void tag(ArmorStand as, UUID id) {
        try { new NBTEntity(as).setString(NBT_TAG, id.toString()); }
        catch (Throwable ignored) {}
    }

    private void despawn(UUID id) {
        List<ArmorStand> stands = live.remove(id);
        if (stands == null) return;
        for (ArmorStand as : stands) {
            try { as.remove(); } catch (Throwable ignored) {}
        }
    }

    /**
     * Walk every loaded world looking for invisible marker ArmorStands with a
     * visible name that AREN'T already tagged — these are pre-persistence
     * holograms. Group vertically-stacked stands at the same x/z into one
     * hologram entry (multi-line), then import.
     */
    private int importOrphans() {
        // Already-tagged UUIDs — skip these
        Set<String> tagged = new HashSet<>();
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (!(e instanceof ArmorStand)) continue;
                try {
                    NBTEntity nbt = new NBTEntity(e);
                    if (nbt.hasKey(NBT_TAG)) tagged.add(nbt.getString(NBT_TAG));
                } catch (Throwable ignored) {}
            }
        }

        // Collect candidate orphans
        List<ArmorStand> candidates = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (!(e instanceof ArmorStand)) continue;
                ArmorStand as = (ArmorStand) e;
                try {
                    NBTEntity nbt = new NBTEntity(as);
                    if (nbt.hasKey(NBT_TAG)) continue;   // already ours
                } catch (Throwable ignored) {}
                if (as.isVisible()) continue;
                if (!as.isCustomNameVisible()) continue;
                if (!as.isMarker()) continue;
                if (as.getCustomName() == null || as.getCustomName().isEmpty()) continue;
                candidates.add(as);
            }
        }
        if (candidates.isEmpty()) return 0;

        // Group vertically-stacked stands at the same (world, x, z)
        // Two stands belong to the same hologram if their x,z match (±0.01)
        // and they are within 0.4 blocks of each other vertically.
        List<List<ArmorStand>> groups = new ArrayList<>();
        Set<ArmorStand> consumed = new HashSet<>();
        for (ArmorStand a : candidates) {
            if (consumed.contains(a)) continue;
            List<ArmorStand> grp = new ArrayList<>();
            grp.add(a);
            consumed.add(a);
            // Find partners
            for (ArmorStand b : candidates) {
                if (consumed.contains(b)) continue;
                if (!b.getWorld().equals(a.getWorld())) continue;
                if (Math.abs(b.getLocation().getX() - a.getLocation().getX()) > 0.01) continue;
                if (Math.abs(b.getLocation().getZ() - a.getLocation().getZ()) > 0.01) continue;
                // Check if b is close vertically to ANY stand already in the group
                for (ArmorStand m : grp) {
                    if (Math.abs(b.getLocation().getY() - m.getLocation().getY()) < 0.4) {
                        grp.add(b);
                        consumed.add(b);
                        break;
                    }
                }
            }
            groups.add(grp);
        }

        // Sort each group top-to-bottom, import as a single entry
        int count = 0;
        for (List<ArmorStand> grp : groups) {
            grp.sort((x, y) -> Double.compare(y.getLocation().getY(), x.getLocation().getY()));
            ArmorStand top = grp.get(0);
            List<String> lines = new ArrayList<>();
            for (ArmorStand s : grp) lines.add(s.getCustomName() != null ? s.getCustomName() : "");
            HologramConfig.Entry entry = config.add(top.getLocation(), lines);
            // Tag existing stands to our new UUID (reuse entities, don't respawn)
            List<ArmorStand> tracked = new ArrayList<>(grp);
            for (ArmorStand s : tracked) tag(s, entry.id);
            live.put(entry.id, tracked);
            count += lines.size();
        }
        return count;
    }

    public void shutdown() {
        // Don't despawn — we want them to persist across reloads.
        live.clear();
    }
}
