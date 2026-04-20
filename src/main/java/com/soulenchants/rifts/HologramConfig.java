package com.soulenchants.rifts;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistent hologram store. Each hologram has a UUID key, a world + xyz anchor,
 * and a list of lines. Backed by holograms.yml. Never touches Bukkit entities —
 * that's HologramManager's job.
 */
public final class HologramConfig {

    public static final class Entry {
        public final UUID id;
        public final String world;
        public final double x, y, z;
        public List<String> lines;
        public Entry(UUID id, String world, double x, double y, double z, List<String> lines) {
            this.id = id; this.world = world;
            this.x = x; this.y = y; this.z = z;
            this.lines = new ArrayList<>(lines);
        }
        public Location toLocation() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z);
        }
    }

    private final SoulEnchants plugin;
    private final File file;
    private YamlConfiguration cfg;

    public HologramConfig(SoulEnchants plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) { try { file.createNewFile(); } catch (IOException ignored) {} }
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        ConfigurationSection sec = cfg.getConfigurationSection("holograms");
        if (sec == null) return out;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection h = sec.getConfigurationSection(key);
            if (h == null) continue;
            try {
                UUID id = UUID.fromString(key);
                out.add(new Entry(id,
                        h.getString("world", "world"),
                        h.getDouble("x"), h.getDouble("y"), h.getDouble("z"),
                        h.getStringList("lines")));
            } catch (Throwable ignored) {}
        }
        return out;
    }

    public Entry get(UUID id) {
        for (Entry e : all()) if (e.id.equals(id)) return e;
        return null;
    }

    public Entry add(Location loc, List<String> lines) {
        UUID id = UUID.randomUUID();
        String base = "holograms." + id + ".";
        cfg.set(base + "world", loc.getWorld().getName());
        cfg.set(base + "x", loc.getX());
        cfg.set(base + "y", loc.getY());
        cfg.set(base + "z", loc.getZ());
        cfg.set(base + "lines", new ArrayList<>(lines));
        save();
        return new Entry(id, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), lines);
    }

    public void updateLines(UUID id, List<String> lines) {
        cfg.set("holograms." + id + ".lines", new ArrayList<>(lines));
        save();
    }

    public void remove(UUID id) {
        cfg.set("holograms." + id, null);
        save();
    }

    private void save() {
        try { cfg.save(file); }
        catch (IOException e) { plugin.getLogger().warning("[holograms] save: " + e.getMessage()); }
    }

    public void reload() { this.cfg = YamlConfiguration.loadConfiguration(file); }
}
