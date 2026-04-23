package com.soulenchants.masks;

import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Minimal helper for building player-head skulls with a base64 skin
 * texture. 1.8's API doesn't expose GameProfile properties cleanly so we
 * use reflection. If the reflection path fails (shaded NMS, Paper
 * variants), we fall back to a plain skull — still functional, just not
 * the custom texture.
 */
public final class SkullUtil {

    private SkullUtil() {}

    public static ItemStack skull(String base64Texture) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        if (base64Texture == null) return skull;
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;
        try {
            Class<?> gameProfileCls = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyCls    = Class.forName("com.mojang.authlib.properties.Property");
            Object profile = gameProfileCls.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);
            Object propertyMap = gameProfileCls.getMethod("getProperties").invoke(profile);
            Object property    = propertyCls.getConstructor(String.class, String.class)
                    .newInstance("textures", base64Texture);
            propertyMap.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(propertyMap, "textures", property);
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Throwable t) {
            // Fall back to a plain skull; the texture just won't render.
        }
        skull.setItemMeta(meta);
        return skull;
    }
}
