package com.soulenchants.loot;

import com.soulenchants.SoulEnchants;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Per-player blacklist of CustomLootRegistry ids. When a custom mob would drop
 * an item the killer has filtered, the drop is skipped and the player is told.
 *
 * Storage layout (lootfilter.yml):
 *   {uuid}:
 *     filtered: [loot_id_1, loot_id_2, ...]
 *     messages: true
 */
public class LootFilterManager {

    private final SoulEnchants plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, Set<String>> filtered = new HashMap<>();
    private final Map<UUID, Boolean> messagesOn = new HashMap<>();

    public LootFilterManager(SoulEnchants plugin, File dataFolder) {
        this.plugin = plugin;
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.file = new File(dataFolder, "lootfilter.yml");
        if (!file.exists()) try { file.createNewFile(); } catch (IOException ignored) {}
        this.config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                List<String> ids = config.getStringList(key + ".filtered");
                filtered.put(id, new HashSet<>(ids));
                messagesOn.put(id, config.getBoolean(key + ".messages", true));
            } catch (Exception ignored) {}
        }
    }

    public boolean isFiltered(UUID uuid, String lootId) {
        if (lootId == null) return false;
        Set<String> set = filtered.get(uuid);
        return set != null && set.contains(lootId);
    }

    /**
     * Resolves the filter id for ANY ItemStack — custom NBT-tagged items use
     * their `se_loot_id`; everything else falls back to "vanilla:MATERIAL_NAME".
     * Returns null only for AIR / null input.
     */
    public static String filterIdOf(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return null;
        String custom = com.soulenchants.loot.BossLootItems.getLootId(item);
        if (custom != null) return custom;
        return "vanilla:" + item.getType().name();
    }

    /** Returns true if the item was just added (now filtered), false if removed. */
    public boolean toggle(UUID uuid, String lootId) {
        Set<String> set = filtered.computeIfAbsent(uuid, k -> new HashSet<>());
        if (set.remove(lootId)) return false;
        set.add(lootId);
        return true;
    }

    public int countFilteredInCategory(UUID uuid, CustomLootRegistry.Category cat) {
        Set<String> set = filtered.get(uuid);
        if (set == null || set.isEmpty()) return 0;
        int n = 0;
        for (CustomLootRegistry.Entry e : CustomLootRegistry.byCategory(cat))
            if (set.contains(e.id)) n++;
        return n;
    }

    public boolean messagesEnabled(UUID uuid) {
        return messagesOn.getOrDefault(uuid, true);
    }

    public void toggleMessages(UUID uuid) {
        messagesOn.put(uuid, !messagesEnabled(uuid));
    }

    public void save() {
        for (String key : config.getKeys(false)) config.set(key, null);
        for (Map.Entry<UUID, Set<String>> e : filtered.entrySet()) {
            config.set(e.getKey() + ".filtered", new ArrayList<>(e.getValue()));
            config.set(e.getKey() + ".messages", messagesEnabled(e.getKey()));
        }
        try { config.save(file); } catch (IOException ignored) {}
    }
}
