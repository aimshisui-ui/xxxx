package com.soulenchants.scoreboard;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight PvP kill/death tracker, persistent to pvpstats.yml. Used by the
 * sidebar scoreboard. Cheap in-memory map; saved on plugin disable.
 */
public class PvPStats implements Listener {

    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, Integer> kills  = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    public PvPStats(File dataFolder) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.file = new File(dataFolder, "pvpstats.yml");
        if (!file.exists()) try { file.createNewFile(); } catch (IOException ignored) {}
        this.config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                kills.put(id,  config.getInt(key + ".kills",  0));
                deaths.put(id, config.getInt(key + ".deaths", 0));
            } catch (Exception ignored) {}
        }
    }

    public int getKills(OfflinePlayer p)  { return kills.getOrDefault(p.getUniqueId(),  0); }
    public int getDeaths(OfflinePlayer p) { return deaths.getOrDefault(p.getUniqueId(), 0); }

    public double getKDR(OfflinePlayer p) {
        int k = getKills(p), d = getDeaths(p);
        if (d == 0) return k;
        return (double) k / d;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        UUID dead = e.getEntity().getUniqueId();
        deaths.merge(dead, 1, Integer::sum);
        org.bukkit.entity.Player killer = e.getEntity().getKiller();
        if (killer != null && !killer.getUniqueId().equals(dead)) {
            kills.merge(killer.getUniqueId(), 1, Integer::sum);
        }
    }

    public void save() {
        for (String key : config.getKeys(false)) config.set(key, null);
        for (UUID id : kills.keySet()) {
            config.set(id + ".kills",  kills.get(id));
            config.set(id + ".deaths", deaths.getOrDefault(id, 0));
        }
        // catch death-only entries
        for (UUID id : deaths.keySet()) {
            if (kills.containsKey(id)) continue;
            config.set(id + ".kills",  0);
            config.set(id + ".deaths", deaths.get(id));
        }
        try { config.save(file); } catch (IOException ignored) {}
    }
}
