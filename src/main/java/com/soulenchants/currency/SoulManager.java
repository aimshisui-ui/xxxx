package com.soulenchants.currency;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulManager {

    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, Long> cache = new HashMap<>();

    public SoulManager(File dataFolder) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.file = new File(dataFolder, "souls.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try { cache.put(UUID.fromString(key), config.getLong(key)); } catch (Exception ignored) {}
        }
    }

    public long get(OfflinePlayer p) {
        return cache.getOrDefault(p.getUniqueId(), 0L);
    }

    public void set(OfflinePlayer p, long amount) {
        cache.put(p.getUniqueId(), Math.max(0, amount));
    }

    public void add(OfflinePlayer p, long amount) {
        set(p, get(p) + amount);
    }

    public boolean take(OfflinePlayer p, long amount) {
        long bal = get(p);
        if (bal < amount) return false;
        set(p, bal - amount);
        return true;
    }

    public void save() {
        for (Map.Entry<UUID, Long> e : cache.entrySet()) {
            config.set(e.getKey().toString(), e.getValue());
        }
        try { config.save(file); } catch (IOException ignored) {}
    }
}
