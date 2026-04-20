package com.soulenchants.currency;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks current spendable souls + immutable lifetime-earned counter.
 * The lifetime counter only goes up; spending and dying never reduce it.
 *
 * Storage layout (souls.yml):
 *   {uuid}:
 *     current: 1234
 *     lifetime: 47000
 *
 * Legacy single-long entries are migrated on load.
 */
public class SoulManager {

    private final SoulEnchants plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, Long> currentCache  = new HashMap<>();
    private final Map<UUID, Long> lifetimeCache = new HashMap<>();

    public SoulManager(SoulEnchants plugin, File dataFolder) {
        this.plugin = plugin;
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.file = new File(dataFolder, "souls.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                if (config.isLong(key) || config.isInt(key)) {
                    // Legacy: single value, migrate as both current + lifetime baseline
                    long val = config.getLong(key);
                    currentCache.put(id, val);
                    lifetimeCache.put(id, val);
                } else {
                    currentCache.put(id, config.getLong(key + ".current", 0L));
                    lifetimeCache.put(id, config.getLong(key + ".lifetime", 0L));
                }
            } catch (Exception ignored) {}
        }
    }

    /** Backwards-compat constructor for code paths still using the old signature. */
    public SoulManager(File dataFolder) {
        this(null, dataFolder);
    }

    public long get(OfflinePlayer p) {
        return currentCache.getOrDefault(p.getUniqueId(), 0L);
    }

    public long getLifetime(OfflinePlayer p) {
        return lifetimeCache.getOrDefault(p.getUniqueId(), 0L);
    }

    public SoulTier getTier(OfflinePlayer p) {
        return SoulTier.forLifetime(getLifetime(p));
    }

    public void set(OfflinePlayer p, long amount) {
        currentCache.put(p.getUniqueId(), Math.max(0, amount));
    }

    /** Add souls. Bumps lifetime and triggers tier promotion if a threshold is crossed. */
    public void add(OfflinePlayer p, long amount) {
        if (amount <= 0) return;
        UUID id = p.getUniqueId();
        long oldLifetime = lifetimeCache.getOrDefault(id, 0L);
        long newLifetime = oldLifetime + amount;
        currentCache.put(id, currentCache.getOrDefault(id, 0L) + amount);
        lifetimeCache.put(id, newLifetime);
        SoulTier oldT = SoulTier.forLifetime(oldLifetime);
        SoulTier newT = SoulTier.forLifetime(newLifetime);
        if (newT != oldT && p instanceof Player) {
            promote((Player) p, oldT, newT);
        }
    }

    /** Admin-only: forcibly overwrite a player's lifetime counter. Bypasses the
     *  promotion broadcast — used for /souls settier when going down. */
    public void forceSetLifetime(OfflinePlayer p, long amount) {
        lifetimeCache.put(p.getUniqueId(), Math.max(0, amount));
    }

    public boolean take(OfflinePlayer p, long amount) {
        long bal = get(p);
        if (bal < amount) return false;
        currentCache.put(p.getUniqueId(), bal - amount);
        return true;
    }

    private void promote(Player p, SoulTier oldT, SoulTier newT) {
        Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "✦✦✦ "
                + ChatColor.LIGHT_PURPLE + p.getName() + ChatColor.DARK_PURPLE
                + " has reached " + newT.getColor() + ChatColor.BOLD + newT.getLabel()
                + ChatColor.DARK_PURPLE + " tier ✦✦✦");
        try {
            p.sendTitle(newT.getColor() + ChatColor.BOLD.toString() + newT.getLabel().toUpperCase(),
                    ChatColor.GRAY + "tier reached");
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.5f, 1.2f);
            p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 0.6f, 1.5f);
            // Fireworks-style explosion sound
            for (int i = 0; i < 3; i++)
                p.playSound(p.getLocation(), Sound.FIREWORK_BLAST, 1.0f, 1.0f);
        } catch (Throwable ignored) {}
    }

    public void save() {
        // Wipe + rewrite everything in the new layout
        for (String key : config.getKeys(false)) config.set(key, null);
        for (UUID id : currentCache.keySet()) {
            config.set(id.toString() + ".current",  currentCache.get(id));
            config.set(id.toString() + ".lifetime", lifetimeCache.getOrDefault(id, currentCache.get(id)));
        }
        try { config.save(file); } catch (IOException ignored) {}
    }
}
