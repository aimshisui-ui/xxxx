package com.soulenchants.masks;

import com.soulenchants.SoulEnchants;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player equipped-mask state. Persists across restarts in
 * plugins/SoulEnchants/masks.yml. On equip, the MaskPacketInjector is told to
 * intercept the player's helmet packets.
 */
public final class MaskManager {

    private final SoulEnchants plugin;
    private final File file;
    private final Map<UUID, String> equipped = new HashMap<>();

    public MaskManager(SoulEnchants plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "masks.yml");
        load();
    }

    public String getEquipped(Player p) {
        return equipped.get(p.getUniqueId());
    }

    public void equip(Player p, String maskId) {
        if (MaskRegistry.get(maskId) == null) return;
        equipped.put(p.getUniqueId(), maskId);
        save();
    }

    public void clear(Player p) {
        equipped.remove(p.getUniqueId());
        save();
    }

    public Map<UUID, String> snapshot() { return new HashMap<>(equipped); }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : equipped.entrySet()) {
            cfg.set(e.getKey().toString(), e.getValue());
        }
        try { cfg.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("[masks] save failed: " + ex); }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID u = UUID.fromString(key);
                String v = cfg.getString(key);
                if (v != null) equipped.put(u, v);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
