package com.soulenchants.quests;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Per-player quest state stored in quests.yml.
 *
 * Layout:
 *   {uuid}:
 *     tutorialStep: 0..5         # 0 = not started, 5 = complete
 *     dailies:
 *       rolledFor: 2026-04-19    # YYYY-MM-DD; rerolls when this != today
 *       active:
 *         d_zombie: 14           # progress on each
 *         d_skeleton: 0
 *       claimed: [d_creeper]
 *     tutorialProgress:
 *       t1_awakening: 1
 *       ...
 */
public class QuestProfile {

    private final File file;
    private final YamlConfiguration cfg;

    public QuestProfile(File dataFolder) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.file = new File(dataFolder, "quests.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public int getTutorialStep(UUID id) { return cfg.getInt(id + ".tutorialStep", 0); }
    public void setTutorialStep(UUID id, int step) { cfg.set(id + ".tutorialStep", step); save(); }

    public int getProgress(UUID id, String questId) {
        return cfg.getInt(id + ".progress." + questId, 0);
    }

    public void setProgress(UUID id, String questId, int value) {
        cfg.set(id + ".progress." + questId, value);
        save();
    }

    public void addProgress(UUID id, String questId, int delta) {
        setProgress(id, questId, getProgress(id, questId) + delta);
    }

    public boolean isClaimed(UUID id, String questId) {
        return cfg.getStringList(id + ".claimed").contains(questId);
    }

    public void markClaimed(UUID id, String questId) {
        List<String> list = cfg.getStringList(id + ".claimed");
        if (!list.contains(questId)) list.add(questId);
        cfg.set(id + ".claimed", list);
        save();
    }

    public List<String> getActiveDailies(UUID id) {
        // Roll new dailies if today != rolledFor
        String today = todayKey();
        String last = cfg.getString(id + ".dailies.rolledFor");
        if (last == null || !last.equals(today)) return Collections.emptyList();
        return cfg.getStringList(id + ".dailies.active");
    }

    public void rollDailies(UUID id, List<String> questIds) {
        cfg.set(id + ".dailies.rolledFor", todayKey());
        cfg.set(id + ".dailies.active", questIds);
        // Reset progress on each
        for (String qid : questIds) cfg.set(id + ".progress." + qid, 0);
        // Clear claimed list
        cfg.set(id + ".dailies.claimed", new ArrayList<>());
        save();
    }

    private static String todayKey() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private void save() {
        try { cfg.save(file); } catch (IOException ignored) {}
    }
}
