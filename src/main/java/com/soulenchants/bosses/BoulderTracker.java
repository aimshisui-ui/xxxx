package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BoulderTracker extends BukkitRunnable {

    /** UUIDs of all live tracked boulders. Read by BlockPlaceBlockerListener
     *  to cancel block placement when a tracked falling block lands. */
    public static final Set<UUID> TRACKED = ConcurrentHashMap.newKeySet();

    private final SoulEnchants plugin;
    private final FallingBlock boulder;
    private final LivingEntity source;
    private int ticks = 0;

    public BoulderTracker(SoulEnchants plugin, FallingBlock boulder, LivingEntity source) {
        this.plugin = plugin;
        this.boulder = boulder;
        this.source = source;
        TRACKED.add(boulder.getUniqueId());
    }

    public void start() { this.runTaskTimer(plugin, 1L, 1L); }

    @Override
    public void run() {
        ticks++;
        if (boulder.isDead() || ticks > 100) {
            Location impact = boulder.getLocation();
            for (Entity e : impact.getWorld().getNearbyEntities(impact, 4, 4, 4)) {
                if (e instanceof Player) com.soulenchants.bosses.BossDamage.apply((Player) e, "irongolem", "boulder", 280, source);
            }
            impact.getWorld().createExplosion(impact.getX(), impact.getY(), impact.getZ(), 0f, false);
            if (!boulder.isDead()) boulder.remove();
            TRACKED.remove(boulder.getUniqueId());
            cancel();
        }
    }
}
