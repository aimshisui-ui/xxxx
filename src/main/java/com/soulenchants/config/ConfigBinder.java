package com.soulenchants.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection-based YAML binder. Loads a bundled default file from the plugin
 * jar, overlays any on-disk copy, and writes back the merged file so new keys
 * added in a release propagate without clobbering operator tuning.
 *
 * Bind a singleton by:
 *   EnchantConfig cfg = new EnchantConfig();
 *   ConfigBinder.bind(plugin, "enchants.yml", cfg);
 *
 * Reload with ConfigBinder.reload(cfg) — re-reads the file and re-fills every
 * @ConfigValue field on the instance.
 */
public final class ConfigBinder {

    private ConfigBinder() {}

    /**
     * First-time bind. Copies the jar-embedded default to disk if missing,
     * merges any new keys from the jar default into the on-disk copy, then
     * loads the merged file into the target's annotated fields.
     */
    public static void bind(JavaPlugin plugin, String resourceName, Object target) {
        File onDisk = new File(plugin.getDataFolder(), resourceName);
        if (!onDisk.getParentFile().exists()) onDisk.getParentFile().mkdirs();

        FileConfiguration jarDefault = loadBundled(plugin, resourceName);

        if (!onDisk.exists()) {
            // First boot — drop the bundled default straight to disk.
            plugin.saveResource(resourceName, false);
        } else if (jarDefault != null) {
            // Merge new keys from the jar into the on-disk file.
            FileConfiguration live = YamlConfiguration.loadConfiguration(onDisk);
            boolean dirty = false;
            for (String key : jarDefault.getKeys(true)) {
                if (!live.contains(key)) {
                    live.set(key, jarDefault.get(key));
                    dirty = true;
                }
            }
            if (dirty) {
                try {
                    live.save(onDisk);
                    plugin.getLogger().info("[config] " + resourceName + " upgraded with new keys");
                } catch (IOException e) {
                    plugin.getLogger().warning("[config] failed to save merged " + resourceName + ": " + e);
                }
            }
        }

        FileConfiguration live = YamlConfiguration.loadConfiguration(onDisk);
        fill(plugin, target, live, resourceName);

        target.getClass();
        ReloadRegistry.register(target, plugin, resourceName);
    }

    /** Re-read the bound YAML file and refill every annotated field. */
    public static void reload(Object target) {
        ReloadRegistry.Binding b = ReloadRegistry.get(target);
        if (b == null) return;
        File onDisk = new File(b.plugin.getDataFolder(), b.resource);
        if (!onDisk.exists()) return;
        FileConfiguration live = YamlConfiguration.loadConfiguration(onDisk);
        fill(b.plugin, target, live, b.resource);
    }

    private static FileConfiguration loadBundled(JavaPlugin plugin, String name) {
        InputStream in = plugin.getResource(name);
        if (in == null) return null;
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(r);
        } catch (IOException e) {
            plugin.getLogger().warning("[config] failed to read bundled " + name + ": " + e);
            return null;
        }
    }

    private static void fill(JavaPlugin plugin, Object target, FileConfiguration yaml, String resource) {
        int filled = 0, missing = 0;
        for (Field f : target.getClass().getDeclaredFields()) {
            ConfigValue cv = f.getAnnotation(ConfigValue.class);
            if (cv == null) continue;
            f.setAccessible(true);
            String path = cv.value();
            if (!yaml.contains(path)) {
                missing++;
                continue;
            }
            Object raw = yaml.get(path);
            Object coerced = coerce(raw, f.getType(), f);
            if (coerced == null) {
                plugin.getLogger().warning("[config] " + resource + ": cannot coerce " + path
                        + " (value=" + raw + ") to " + f.getType().getSimpleName());
                continue;
            }
            try {
                f.set(target, coerced);
                filled++;
            } catch (IllegalAccessException e) {
                plugin.getLogger().warning("[config] failed to set " + path + ": " + e);
            }
        }
        plugin.getLogger().info("[config] " + resource + " bound: " + filled + " values, "
                + missing + " missing (defaults preserved)");
    }

    @SuppressWarnings("unchecked")
    private static Object coerce(Object raw, Class<?> type, Field f) {
        if (raw == null) return null;
        if (type == int.class || type == Integer.class) {
            if (raw instanceof Number) return ((Number) raw).intValue();
            try { return Integer.parseInt(raw.toString()); } catch (NumberFormatException e) { return null; }
        }
        if (type == long.class || type == Long.class) {
            if (raw instanceof Number) return ((Number) raw).longValue();
            try { return Long.parseLong(raw.toString()); } catch (NumberFormatException e) { return null; }
        }
        if (type == double.class || type == Double.class) {
            if (raw instanceof Number) return ((Number) raw).doubleValue();
            try { return Double.parseDouble(raw.toString()); } catch (NumberFormatException e) { return null; }
        }
        if (type == float.class || type == Float.class) {
            if (raw instanceof Number) return ((Number) raw).floatValue();
            try { return Float.parseFloat(raw.toString()); } catch (NumberFormatException e) { return null; }
        }
        if (type == boolean.class || type == Boolean.class) {
            if (raw instanceof Boolean) return raw;
            return Boolean.parseBoolean(raw.toString());
        }
        if (type == String.class) return raw.toString();
        if (List.class.isAssignableFrom(type)) {
            if (raw instanceof List<?>) {
                List<String> out = new ArrayList<>();
                for (Object o : (List<?>) raw) out.add(o == null ? "" : o.toString());
                return out;
            }
            return new ArrayList<>();
        }
        return null;
    }

    /** Tracks which plugin + resource each bound object came from, for reload. */
    private static final class ReloadRegistry {
        static final class Binding {
            final JavaPlugin plugin; final String resource;
            Binding(JavaPlugin p, String r) { this.plugin = p; this.resource = r; }
        }
        private static final java.util.Map<Object, Binding> map = new java.util.IdentityHashMap<>();
        static void register(Object target, JavaPlugin p, String r) { map.put(target, new Binding(p, r)); }
        static Binding get(Object t) { return map.get(t); }
    }
}
