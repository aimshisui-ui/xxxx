package com.soulenchants.loot;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Per-player permanent buffs from consumables.
 *  - heart_of_the_forge: +2 max HP each, capped at +20 total
 *  - veil_sigil: +1 souls per kill each, capped at +10
 */
public final class LootProfile {

    private final File file;
    private final YamlConfiguration cfg;

    public LootProfile(File dataFolder) {
        this.file = new File(dataFolder, "loot-profile.yml");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public int getHeartStacks(UUID id) {
        return cfg.getInt("hearts." + id, 0);
    }

    public int getSigilStacks(UUID id) {
        return cfg.getInt("sigils." + id, 0);
    }

    /** Returns true if applied; false if at cap. */
    public boolean addHeart(UUID id) {
        int n = getHeartStacks(id);
        if (n >= 10) return false;
        cfg.set("hearts." + id, n + 1);
        save();
        return true;
    }

    public boolean addSigil(UUID id) {
        int n = getSigilStacks(id);
        if (n >= 10) return false;
        cfg.set("sigils." + id, n + 1);
        save();
        return true;
    }

    public int bonusHpFor(OfflinePlayer p) {
        return getHeartStacks(p.getUniqueId()) * 2;
    }

    /** Admin reset — wipes heart-of-the-forge stacks (removes the permanent +HP buff). */
    public void clearHearts(UUID id) {
        cfg.set("hearts." + id, 0);
        save();
    }

    public int bonusSoulsFor(OfflinePlayer p) {
        return getSigilStacks(p.getUniqueId());
    }

    public void save() {
        try { cfg.save(file); } catch (IOException ignored) {}
    }
}
