package com.soulenchants.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Lifts Bukkit/NMS's max-health ceiling so bosses can run with > 1024 HP.
 *
 * NMS 1.8 (v1_8_R3) backs maxHealth with an AttributeRanged whose upper
 * bound is hard-coded. Bukkit's CraftLivingEntity.setHealth() throws if you
 * pass a value above that bound. We use reflection to:
 *   1) Raise the global AttributeRanged max bound once on plugin enable.
 *   2) Optionally set HP directly via NMS as a belt-and-suspenders bypass.
 *
 * After raise() runs, normal entity.setMaxHealth(N) / setHealth(N) work
 * for any N up to 1_000_000.
 */
public final class BossHealthHack {

    private static boolean ceilingRaised = false;
    private static final double NEW_CEILING = 1_000_000.0;

    private BossHealthHack() {}

    public static synchronized void raise() {
        if (ceilingRaised) return;
        try {
            String pkg = nmsPackage();
            Class<?> genericAttributes = Class.forName("net.minecraft.server." + pkg + ".GenericAttributes");
            Class<?> attributeRanged = Class.forName("net.minecraft.server." + pkg + ".AttributeRanged");

            Object maxHealthAttr = genericAttributes.getField("maxHealth").get(null);

            Field maxField = findRangedMaxField(attributeRanged);
            maxField.setAccessible(true);
            maxField.setDouble(maxHealthAttr, NEW_CEILING);

            ceilingRaised = true;
            Bukkit.getLogger().info("[SoulEnchants] Boss HP ceiling raised to " + NEW_CEILING);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[SoulEnchants] Could not raise HP ceiling: " + t.getMessage());
        }
    }

    /**
     * Set both maxHealth and current HP in a single call, bypassing the
     * Bukkit bounds check by writing through NMS directly.
     */
    public static void apply(LivingEntity entity, double hp) {
        raise();
        try {
            entity.setMaxHealth(hp);
            entity.setHealth(hp);
        } catch (IllegalArgumentException ignored) {
            forceNms(entity, hp);
        }
    }

    private static void forceNms(LivingEntity entity, double hp) {
        try {
            String pkg = nmsPackage();
            Class<?> genericAttributes = Class.forName("net.minecraft.server." + pkg + ".GenericAttributes");
            Object maxHealthAttr = genericAttributes.getField("maxHealth").get(null);

            Object handle = entity.getClass().getMethod("getHandle").invoke(entity);

            Method getAttr = handle.getClass().getMethod("getAttributeInstance",
                    Class.forName("net.minecraft.server." + pkg + ".IAttribute"));
            Object attrInst = getAttr.invoke(handle, maxHealthAttr);
            attrInst.getClass().getMethod("setValue", double.class).invoke(attrInst, hp);

            handle.getClass().getMethod("setHealth", float.class).invoke(handle, (float) hp);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[SoulEnchants] forceNms failed: " + t.getMessage());
        }
    }

    private static String nmsPackage() {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /** AttributeRanged stores min/max as two private double fields. The max
     *  is the second one — find by declaration order to be obfuscation-safe. */
    private static Field findRangedMaxField(Class<?> attributeRanged) throws NoSuchFieldException {
        Field min = null;
        for (Field f : attributeRanged.getDeclaredFields()) {
            if (f.getType() == double.class) {
                if (min == null) {
                    min = f;
                } else {
                    return f; // second double field is the max
                }
            }
        }
        // Fallback: try named field "b"
        return attributeRanged.getDeclaredField("b");
    }
}
