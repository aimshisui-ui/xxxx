package com.soulenchants.mobs;

import com.soulenchants.loot.CustomLootRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A loot drop: vanilla material + amount range + chance, OR a reference to
 * a registered custom item (via lootId — see {@link CustomLootRegistry}).
 *
 * If lootId is set, roll() returns a fresh ItemStack from the factory (carries
 * its custom name + lore + NBT). Otherwise it rolls a vanilla material item.
 *
 * lootId persists in YAML; the actual factory rebuild happens on every roll
 * so name/lore/NBT can never get stale.
 */
public final class DropSpec {

    public Material material;
    public int min;
    public int max;
    public double chance;
    /** Optional — if set, roll() pulls a fresh stack from the custom registry. */
    public String lootId;

    public DropSpec(Material material, int min, int max, double chance) {
        this.material = material;
        this.min = Math.max(1, min);
        this.max = Math.max(this.min, max);
        this.chance = Math.max(0.0, Math.min(1.0, chance));
    }

    /** Build a DropSpec backed by a registered custom-loot id. */
    public static DropSpec custom(String lootId, int amount, double chance) {
        CustomLootRegistry.Entry entry = CustomLootRegistry.get(lootId);
        Material mat = entry != null ? entry.create().getType() : Material.STONE;
        DropSpec d = new DropSpec(mat, amount, amount, chance);
        d.lootId = lootId;
        return d;
    }

    public static DropSpec of(ItemStack item, double chance) {
        return new DropSpec(item.getType(), item.getAmount(), item.getAmount(), chance);
    }

    public ItemStack roll(java.util.Random rng) {
        if (rng.nextDouble() > chance) return null;
        int n = (max <= min) ? min : (min + rng.nextInt(max - min + 1));
        if (lootId != null && !lootId.isEmpty()) {
            CustomLootRegistry.Entry entry = CustomLootRegistry.get(lootId);
            if (entry != null) {
                ItemStack it = entry.create();
                it.setAmount(Math.max(1, n));
                return it;
            }
            // Fall through — registry missing, drop vanilla material as a last resort
        }
        return new ItemStack(material, n);
    }

    /** Returns the display name to show in the editor list. */
    public String displayName() {
        if (lootId != null && !lootId.isEmpty()) {
            CustomLootRegistry.Entry entry = CustomLootRegistry.get(lootId);
            if (entry != null) return entry.displayName;
        }
        return material == null ? "AIR" : material.name();
    }

    public boolean isCustom() { return lootId != null && !lootId.isEmpty(); }

    public Map<String, Object> serialize() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("material", material.name());
        m.put("min", min);
        m.put("max", max);
        m.put("chance", chance);
        if (lootId != null) m.put("loot_id", lootId);
        return m;
    }

    public static DropSpec deserialize(Map<String, Object> m) {
        Material mat;
        try { mat = Material.valueOf(String.valueOf(m.get("material"))); }
        catch (Throwable t) { mat = Material.AIR; }
        int mn = asInt(m.get("min"), 1);
        int mx = asInt(m.get("max"), mn);
        double ch = asDouble(m.get("chance"), 1.0);
        DropSpec d = new DropSpec(mat, mn, mx, ch);
        Object lid = m.get("loot_id");
        if (lid != null) d.lootId = String.valueOf(lid);
        return d;
    }

    private static int asInt(Object o, int def) { return (o instanceof Number) ? ((Number) o).intValue() : def; }
    private static double asDouble(Object o, double def) { return (o instanceof Number) ? ((Number) o).doubleValue() : def; }
}
