package com.soulenchants.modock;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-phase admin-set spawn points for Modock + arriving players.
 *
 * YAML layout (modock-spawns.yml):
 *   phase1:
 *     boss_x/y/z, player_x/y/z, yaw, pitch     (yaw/pitch optional, on player)
 *   phase2: ...
 *   phase3: ...
 *
 * Worlds are inferred from {@link ModockWorlds#PHASE1/2/3}, never stored,
 * so renaming the world folders won't desync the config.
 */
public class ModockSpawnConfig {

    public static final class Pair {
        public final Location boss;
        public final Location player;
        public Pair(Location boss, Location player) { this.boss = boss; this.player = player; }
    }

    private final SoulEnchants plugin;
    private final File file;
    private final FileConfiguration cfg;
    private final Map<String, Pair> cached = new HashMap<>();

    public ModockSpawnConfig(SoulEnchants plugin) {
        this.plugin = plugin;
        File dir = plugin.getDataFolder();
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "modock-spawns.yml");
        if (!file.exists()) try { file.createNewFile(); } catch (IOException ignored) {}
        this.cfg = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        for (String phase : new String[]{"phase1", "phase2", "phase3"}) {
            String wn = worldFor(phase);
            World w = Bukkit.getWorld(wn);
            if (w == null) continue;
            if (!cfg.isConfigurationSection(phase)) continue;
            Location boss = readLoc(w, phase + ".boss");
            Location pl   = readLoc(w, phase + ".player");
            if (boss == null) boss = w.getSpawnLocation();
            if (pl == null)   pl   = w.getSpawnLocation();
            cached.put(phase, new Pair(boss, pl));
        }
    }

    private Location readLoc(World w, String base) {
        if (!cfg.isSet(base + ".x")) return null;
        Location l = new Location(w,
                cfg.getDouble(base + ".x"),
                cfg.getDouble(base + ".y"),
                cfg.getDouble(base + ".z"),
                (float) cfg.getDouble(base + ".yaw", 0.0),
                (float) cfg.getDouble(base + ".pitch", 0.0));
        return l;
    }

    private void writeLoc(String base, Location l) {
        cfg.set(base + ".x", l.getX());
        cfg.set(base + ".y", l.getY());
        cfg.set(base + ".z", l.getZ());
        cfg.set(base + ".yaw", l.getYaw());
        cfg.set(base + ".pitch", l.getPitch());
    }

    public static String worldFor(String phase) {
        if ("phase1".equals(phase)) return ModockWorlds.PHASE1;
        if ("phase2".equals(phase)) return ModockWorlds.PHASE2;
        if ("phase3".equals(phase)) return ModockWorlds.PHASE3;
        return null;
    }

    /** Phase id (phase1/phase2/phase3) for a given world name. Returns null if
     *  the world isn't one of the Modock arenas. */
    public static String phaseFor(String worldName) {
        if (ModockWorlds.PHASE1.equals(worldName)) return "phase1";
        if (ModockWorlds.PHASE2.equals(worldName)) return "phase2";
        if (ModockWorlds.PHASE3.equals(worldName)) return "phase3";
        return null;
    }

    public Pair get(String phase) {
        Pair p = cached.get(phase);
        if (p != null) return p;
        // Fallback: world spawn for both
        World w = Bukkit.getWorld(worldFor(phase));
        if (w == null) return null;
        return new Pair(w.getSpawnLocation(), w.getSpawnLocation());
    }

    /** kind = "boss" or "player" */
    public void setLocation(String phase, String kind, Location loc) {
        if (!"boss".equals(kind) && !"player".equals(kind)) return;
        writeLoc(phase + "." + kind, loc);
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("[modock] save spawn-config: " + e);
        }
        // Update cache: read back the combined pair from cfg
        World w = Bukkit.getWorld(worldFor(phase));
        if (w == null) return;
        Location boss = readLoc(w, phase + ".boss");
        Location pl   = readLoc(w, phase + ".player");
        if (boss == null) boss = w.getSpawnLocation();
        if (pl == null)   pl   = w.getSpawnLocation();
        cached.put(phase, new Pair(boss, pl));
    }
}
