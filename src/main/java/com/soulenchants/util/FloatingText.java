package com.soulenchants.util;

import com.soulenchants.SoulEnchants;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Brief floating text near a location. Uses a marker armor stand so it has
 * no hitbox, no shadow, no gravity. Auto-removes after 2 seconds.
 */
public final class FloatingText {

    private FloatingText() {}

    public static void show(SoulEnchants plugin, Location loc, String text) {
        show(plugin, loc, text, 40);
    }

    public static void show(SoulEnchants plugin, Location loc, String text, int durationTicks) {
        Location spawn = loc.clone().add(0, 1.5, 0);
        final ArmorStand stand;
        try {
            stand = (ArmorStand) loc.getWorld().spawnEntity(spawn, org.bukkit.entity.EntityType.ARMOR_STAND);
        } catch (Throwable t) { return; }
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(text);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= durationTicks || stand.isDead()) {
                    if (!stand.isDead()) stand.remove();
                    cancel();
                    return;
                }
                stand.teleport(stand.getLocation().add(0, 0.02, 0));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
