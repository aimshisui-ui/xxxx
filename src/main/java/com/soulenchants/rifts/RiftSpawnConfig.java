package com.soulenchants.rifts;

import com.soulenchants.SoulEnchants;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistable list of mob/boss spawn points inside the rift world.
 *
 *   rift-spawns.yml:
 *     spawns:
 *       - { mob: rotling,   x: 100, y: 5, z: 100 }
 *       - { mob: veilweaver,x: 200, y: 8, z: 200 }   # special: spawns the boss
 *
 * mob names:
 *   - any CustomMob registry id (see /mob list)
 *   - "veilweaver" → summons the Veilweaver boss
 *   - "irongolem"  → summons the Ironheart Colossus
 */
public final class RiftSpawnConfig {

    public static final class Entry {
        public final String mob;
        public final double x, y, z;
        public Entry(String mob, double x, double y, double z) {
            this.mob = mob; this.x = x; this.y = y; this.z = z;
        }
        public Location toLocation(World w) { return new Location(w, x, y, z); }
    }

    private final SoulEnchants plugin;
    private final File file;
    private YamlConfiguration cfg;

    public RiftSpawnConfig(SoulEnchants plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rift-spawns.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public List<Entry> list() {
        List<Entry> out = new ArrayList<>();
        List<?> raw = cfg.getList("spawns");
        if (raw == null) return out;
        for (Object o : raw) {
            if (!(o instanceof java.util.Map)) continue;
            java.util.Map<?, ?> m = (java.util.Map<?, ?>) o;
            Object mob = m.get("mob");
            Object x = m.get("x"), y = m.get("y"), z = m.get("z");
            if (mob == null || x == null || y == null || z == null) continue;
            try {
                out.add(new Entry(String.valueOf(mob),
                        ((Number) x).doubleValue(),
                        ((Number) y).doubleValue(),
                        ((Number) z).doubleValue()));
            } catch (Throwable ignored) {}
        }
        return out;
    }

    public void add(String mobId, Location loc) {
        List<Entry> entries = list();
        entries.add(new Entry(mobId, loc.getX(), loc.getY(), loc.getZ()));
        persist(entries);
    }

    /** Remove the entry nearest to loc (within 8 blocks). Returns true if one was removed. */
    public boolean removeNearest(Location loc) {
        List<Entry> entries = list();
        int best = -1;
        double bestSq = 8 * 8;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            double dx = e.x - loc.getX(), dy = e.y - loc.getY(), dz = e.z - loc.getZ();
            double d = dx*dx + dy*dy + dz*dz;
            if (d < bestSq) { bestSq = d; best = i; }
        }
        if (best < 0) return false;
        entries.remove(best);
        persist(entries);
        return true;
    }

    public void clearAll() { persist(new ArrayList<Entry>()); }

    private void persist(List<Entry> entries) {
        List<java.util.Map<String, Object>> raw = new ArrayList<>();
        for (Entry e : entries) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("mob", e.mob);
            m.put("x", e.x);
            m.put("y", e.y);
            m.put("z", e.z);
            raw.add(m);
        }
        cfg.set("spawns", raw);
        try { cfg.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("[rift] save spawns: " + ex.getMessage()); }
    }

    public void reload() { this.cfg = YamlConfiguration.loadConfiguration(file); }
}
