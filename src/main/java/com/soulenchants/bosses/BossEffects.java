package com.soulenchants.bosses;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Cinematic visual + audio effects for boss encounters. Designed to make
 * abilities READ — players should always know what's about to hit them.
 *
 * Methods are PURELY decorative — they don't deal damage. Combine with
 * BossDamage.apply() for the actual hit logic.
 */
public final class BossEffects {

    private static final Random RNG = new Random();

    private BossEffects() {}

    /**
     * Particle "cannon" beam — dense helical particle stream from `from` to `to`.
     * Heavy visibility: alternates the requested particle with bright FLAMES
     * + LARGE_SMOKE so it reads as a real beam, not a faint dust trail.
     * Renders over `durationTicks` ticks. Use for laser/beam-style abilities.
     */
    public static void particleCannon(Plugin plugin, final Location from, final Location to,
                                       final Effect particle, final int durationTicks) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= durationTicks) { cancel(); return; }
                Vector dir = to.toVector().subtract(from.toVector());
                double dist = dir.length();
                if (dist < 0.1) return;
                dir.normalize();
                // Helical sweep along the beam — TWO offset spirals for thicker visual,
                // bright flames as the core, requested particle as the trim.
                for (double d = 0; d < dist; d += 0.25) {           // tighter spacing
                    double phase = d * 2.0 + t * 0.6;
                    // Inner spiral (bright flames, radius 0.25)
                    double cx = Math.cos(phase) * 0.25;
                    double cy = Math.sin(phase) * 0.25;
                    // Outer spiral (requested particle, radius 0.6)
                    double ox = Math.cos(phase + Math.PI) * 0.6;
                    double oy = Math.sin(phase + Math.PI) * 0.6;
                    Vector perp1 = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                    Vector perp2 = new Vector(0, 1, 0);
                    Location core = from.clone().add(dir.clone().multiply(d))
                            .add(perp1.clone().multiply(cx)).add(perp2.clone().multiply(cy));
                    Location outer = from.clone().add(dir.clone().multiply(d))
                            .add(perp1.clone().multiply(ox)).add(perp2.clone().multiply(oy));
                    core.getWorld().playEffect(core, Effect.MOBSPAWNER_FLAMES, 0);
                    outer.getWorld().playEffect(outer, particle, 0);
                    // Center beam pulse every 4 ticks
                    if (t % 4 == 0) {
                        Location center = from.clone().add(dir.clone().multiply(d));
                        center.getWorld().playEffect(center, Effect.LARGE_SMOKE, 0);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Expanding shockwave ring at ground level. Use for slam / aoe attacks.
     * Plays a step-sound particle around a growing radius.
     */
    public static void shockwaveRing(Plugin plugin, final Location center, final double maxRadius,
                                      final Material visualBlock) {
        final int steps = 10;
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= steps) { cancel(); return; }
                double r = (maxRadius * (t + 1)) / steps;
                int rays = (int) (12 * r);
                for (int i = 0; i < rays; i++) {
                    double a = i * (Math.PI * 2 / rays);
                    Location p = center.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                    p.getWorld().playEffect(p, Effect.STEP_SOUND, visualBlock.getId());
                    if (t % 2 == 0) p.getWorld().playEffect(p.clone().add(0, 0.5, 0), Effect.SMOKE, 4);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Radial particle burst — fires N particles outward from center for impact. */
    public static void particleBurst(Location center, Effect particle, int count) {
        for (int i = 0; i < count; i++) {
            double yaw = RNG.nextDouble() * Math.PI * 2;
            double pitch = RNG.nextDouble() * Math.PI;
            double r = 0.3 + RNG.nextDouble() * 1.5;
            Location p = center.clone().add(
                    Math.cos(yaw) * Math.sin(pitch) * r,
                    Math.cos(pitch) * r,
                    Math.sin(yaw) * Math.sin(pitch) * r);
            p.getWorld().playEffect(p, particle, 0);
        }
    }

    /** Vertical vortex — swirling tornado of particles at a location for `durationTicks`. */
    public static void vortex(Plugin plugin, final Location center, final double radius,
                               final double height, final Effect particle, final int durationTicks) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= durationTicks) { cancel(); return; }
                for (double y = 0; y < height; y += 0.4) {
                    double phase = y * 2 + t * 0.4;
                    double r = radius * (1 - y / (height * 1.5));
                    Location p = center.clone().add(Math.cos(phase) * r, y, Math.sin(phase) * r);
                    p.getWorld().playEffect(p, particle, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Cone of particles fanning out from origin in `direction` over `length` blocks. */
    public static void cone(Location origin, Vector direction, double length, double spread,
                             Effect particle, int density) {
        Vector dir = direction.clone().normalize();
        Vector perp = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        for (int i = 0; i < density; i++) {
            double d = (length * i) / density;
            double half = spread * (d / length);
            int rays = 1 + (int) (half * 4);
            for (int r = -rays; r <= rays; r++) {
                Vector off = perp.clone().multiply((r * half) / Math.max(1, rays));
                Location p = origin.clone().add(dir.clone().multiply(d)).add(off);
                p.getWorld().playEffect(p, particle, 0);
            }
        }
    }

    /** Telegraphed warning ring — flashes a colored ring at the impact location
     *  for `windupTicks` ticks before an attack lands. Players see where to dodge. */
    public static void telegraph(Plugin plugin, final Location center, final double radius,
                                  final int windupTicks) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= windupTicks) { cancel(); return; }
                int n = (int) (16 * radius);
                for (int i = 0; i < n; i++) {
                    double a = i * (Math.PI * 2 / n);
                    Location p = center.clone().add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
                    p.getWorld().playEffect(p, Effect.MOBSPAWNER_FLAMES, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Boss roar — wither sound + dragon growl + particle burst at boss head. */
    public static void roar(Location loc) {
        loc.getWorld().playSound(loc, Sound.WITHER_SPAWN, 1.5f, 0.5f);
        loc.getWorld().playSound(loc, Sound.ENDERDRAGON_GROWL, 1.0f, 0.8f);
        particleBurst(loc.clone().add(0, 1.5, 0), Effect.SMOKE, 30);
        particleBurst(loc.clone().add(0, 1.5, 0), Effect.WITCH_MAGIC, 20);
    }

    /** Send a title to every player within range of center. */
    public static void titleNearby(Location center, double range, String title, String subtitle) {
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(center) > range * range) continue;
            try { p.sendTitle(title, subtitle); } catch (Throwable ignored) {}
        }
    }
}
