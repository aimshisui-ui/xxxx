package com.soulenchants.shop;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks the daily "featured" rotation — 3 random items at 30% off. Resets
 * at 00:00 server-local time. Persists across restart so the same 3 items
 * stay featured for the whole day.
 */
public class ShopFeatured {

    public static final double DISCOUNT = 0.30; // 30% off

    private final File file;
    private final YamlConfiguration cfg;
    private long rolledAt;
    private List<String> featuredIds = new ArrayList<>();

    public ShopFeatured(File dataFolder) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.file = new File(dataFolder, "shop-featured.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
        this.rolledAt = cfg.getLong("rolledAt", 0);
        this.featuredIds = cfg.getStringList("featured");
    }

    public void maybeRoll() {
        if (isSameLocalDay(rolledAt, System.currentTimeMillis())) return;
        roll();
    }

    public void forceRoll() { roll(); }

    private void roll() {
        List<ShopItem> pool = new ArrayList<>();
        for (ShopItem s : ShopCatalog.all()) {
            // Only feature items from main categories (not loot boxes — those are chase)
            if (s.category.equals(ShopCatalog.CAT_BOOKS)
             || s.category.equals(ShopCatalog.CAT_REAGENTS)
             || s.category.equals(ShopCatalog.CAT_CONSUMABLES)) {
                pool.add(s);
            }
        }
        Collections.shuffle(pool);
        List<String> picked = new ArrayList<>();
        for (int i = 0; i < Math.min(3, pool.size()); i++) picked.add(pool.get(i).id);
        featuredIds = picked;
        rolledAt = System.currentTimeMillis();
        save();
    }

    public boolean isFeatured(String id) {
        return featuredIds.contains(id);
    }

    public long priceFor(ShopItem s) {
        if (!isFeatured(s.id)) return s.price;
        return Math.max(1, (long) (s.price * (1 - DISCOUNT)));
    }

    public List<String> getFeaturedIds() { return featuredIds; }

    private static boolean isSameLocalDay(long a, long b) {
        java.util.Calendar ca = java.util.Calendar.getInstance();
        java.util.Calendar cb = java.util.Calendar.getInstance();
        ca.setTimeInMillis(a);
        cb.setTimeInMillis(b);
        return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR)
            && ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private void save() {
        cfg.set("rolledAt", rolledAt);
        cfg.set("featured", featuredIds);
        try { cfg.save(file); } catch (IOException ignored) {}
    }
}
