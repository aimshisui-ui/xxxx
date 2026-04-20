package com.soulenchants.config;

import com.soulenchants.SoulEnchants;
import com.soulenchants.mobs.AbilitySpec;
import com.soulenchants.mobs.CustomMob;
import com.soulenchants.mobs.DropSpec;
import com.soulenchants.mobs.MobRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML persistence for live mob stat/ability/drop overrides + boss stat/damage/drop overrides.
 *
 *   mob-overrides.yml:
 *     rotling:
 *       hp: 14                          # overrides baseline
 *       bonus-damage: 3
 *       souls: 5
 *       abilities:                      # FULL replacement (preserves order)
 *         - type: bonus_damage
 *           params: { amount: 3 }
 *       drops:
 *         - { material: ROTTEN_FLESH, min: 1, max: 2, chance: 0.5 }
 *
 *   boss-overrides.yml:
 *     veilweaver:
 *       hp: 800
 *       damage:                         # keyed boss-attack damage overrides
 *         tether_tick: 14
 *         shatter_bolt: 12
 *       drops: [ ... same shape as mob drops ]
 */
public final class LootConfig {

    private final SoulEnchants plugin;
    private final File mobFile;
    private final File bossFile;
    private YamlConfiguration mobYaml;
    private YamlConfiguration bossYaml;

    public LootConfig(SoulEnchants plugin) {
        this.plugin = plugin;
        this.mobFile = new File(plugin.getDataFolder(), "mob-overrides.yml");
        this.bossFile = new File(plugin.getDataFolder(), "boss-overrides.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        try { if (!mobFile.exists())  mobFile.createNewFile(); } catch (IOException ignored) {}
        try { if (!bossFile.exists()) bossFile.createNewFile(); } catch (IOException ignored) {}
        this.mobYaml  = YamlConfiguration.loadConfiguration(mobFile);
        this.bossYaml = YamlConfiguration.loadConfiguration(bossFile);
    }

    // ── MOB OVERRIDES ────────────────────────────────────────────────────

    /** Apply saved overrides to every registered custom mob (call after MobRegistry load). */
    public void applyMobOverrides() {
        for (String id : mobYaml.getKeys(false)) {
            CustomMob cm = MobRegistry.get(id);
            if (cm == null) continue;
            ConfigurationSection sec = mobYaml.getConfigurationSection(id);
            if (sec == null) continue;
            if (sec.contains("hp"))           cm.maxHp = sec.getInt("hp", cm.maxHp);
            if (sec.contains("bonus-damage")) cm.bonusDamage = sec.getDouble("bonus-damage", cm.bonusDamage);
            if (sec.contains("souls"))        cm.souls = sec.getInt("souls", cm.souls);
            if (sec.contains("abilities")) {
                List<Map<?, ?>> list = sec.getMapList("abilities");
                List<AbilitySpec> specs = new ArrayList<>();
                for (Map<?, ?> m : list) {
                    try {
                        Map<String, Object> cast = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> e : m.entrySet()) cast.put(String.valueOf(e.getKey()), e.getValue());
                        specs.add(AbilitySpec.deserialize(cast));
                    } catch (Throwable ignored) {}
                }
                cm.abilitySpecs = specs;
            }
            if (sec.contains("drops")) {
                List<Map<?, ?>> list = sec.getMapList("drops");
                List<DropSpec> drops = new ArrayList<>();
                for (Map<?, ?> m : list) {
                    try {
                        Map<String, Object> cast = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> e : m.entrySet()) cast.put(String.valueOf(e.getKey()), e.getValue());
                        drops.add(DropSpec.deserialize(cast));
                    } catch (Throwable ignored) {}
                }
                cm.dropSpecs = drops;
            }
        }
    }

    /** Persist the current live values of one mob. */
    public void saveMob(CustomMob cm) {
        String id = cm.id;
        mobYaml.set(id + ".hp", cm.maxHp);
        mobYaml.set(id + ".bonus-damage", cm.bonusDamage);
        mobYaml.set(id + ".souls", cm.souls);
        List<Map<String, Object>> abil = new ArrayList<>();
        for (AbilitySpec s : cm.abilitySpecs) abil.add(s.serialize());
        mobYaml.set(id + ".abilities", abil);
        List<Map<String, Object>> drops = new ArrayList<>();
        for (DropSpec d : cm.dropSpecs) drops.add(d.serialize());
        mobYaml.set(id + ".drops", drops);
        try { mobYaml.save(mobFile); }
        catch (IOException e) { plugin.getLogger().warning("[loot] save mob " + id + ": " + e.getMessage()); }
    }

    /** Delete this mob's saved overrides (next reload will use baselines). */
    public void clearMob(String id) {
        mobYaml.set(id, null);
        try { mobYaml.save(mobFile); } catch (IOException ignored) {}
    }

    // ── BOSS OVERRIDES ───────────────────────────────────────────────────

    public double bossHp(String bossId, double def) {
        return bossYaml.getDouble(bossId + ".hp", def);
    }

    public double bossDamage(String bossId, String key, double def) {
        String path = bossId + ".damage." + key;
        return bossYaml.contains(path) ? bossYaml.getDouble(path, def) : def;
    }

    public List<DropSpec> bossDrops(String bossId) {
        List<DropSpec> out = new ArrayList<>();
        List<Map<?, ?>> list = bossYaml.getMapList(bossId + ".drops");
        for (Map<?, ?> m : list) {
            try {
                Map<String, Object> cast = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) cast.put(String.valueOf(e.getKey()), e.getValue());
                out.add(DropSpec.deserialize(cast));
            } catch (Throwable ignored) {}
        }
        return out;
    }

    public void setBossHp(String bossId, double hp) {
        bossYaml.set(bossId + ".hp", hp);
        persistBoss();
    }

    public void setBossDamage(String bossId, String key, double dmg) {
        bossYaml.set(bossId + ".damage." + key, dmg);
        persistBoss();
    }

    public void setBossDrops(String bossId, List<DropSpec> drops) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (DropSpec d : drops) out.add(d.serialize());
        bossYaml.set(bossId + ".drops", out);
        persistBoss();
    }

    public org.bukkit.configuration.ConfigurationSection bossSection(String bossId) {
        org.bukkit.configuration.ConfigurationSection s = bossYaml.getConfigurationSection(bossId);
        if (s != null) return s;
        return bossYaml.createSection(bossId);
    }

    private void persistBoss() {
        try { bossYaml.save(bossFile); }
        catch (IOException e) { plugin.getLogger().warning("[loot] save boss: " + e.getMessage()); }
    }

    // ── RELOAD ───────────────────────────────────────────────────────────

    /** Reload both YAMLs from disk and reset + re-apply overrides to every mob. */
    public void reload() {
        this.mobYaml  = YamlConfiguration.loadConfiguration(mobFile);
        this.bossYaml = YamlConfiguration.loadConfiguration(bossFile);
        for (CustomMob cm : MobRegistry.all()) cm.resetToDefaults();
        applyMobOverrides();
    }
}
