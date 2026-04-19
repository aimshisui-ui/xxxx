package com.soulenchants.bosses.attacks;

import com.soulenchants.bosses.Veilweaver;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class VeilweaverAttacks {

    private static final Random RNG = new Random();

    // ── PHASE 1 ──────────────────────────────────────────────────────────────

    public static void threadLash(Veilweaver vw) {
        final LivingEntity boss = vw.getEntity();
        final Location loc = boss.getLocation();
        final Vector forward = loc.getDirection().setY(0).normalize();
        // Telegraph: 10-tick warning sweep with red particles before impact
        loc.getWorld().playSound(loc, Sound.ZOMBIE_WOOD, 1.6f, 1.8f);
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 10) {
                    // Impact: damage + heavier particle burst
                    for (Player p : vw.getArena().playersInArena()) {
                        Vector toPlayer = p.getLocation().toVector().subtract(loc.toVector()).setY(0);
                        if (toPlayer.lengthSquared() > 16) continue;
                        double dot = toPlayer.normalize().dot(forward);
                        if (dot < 0.5) continue;
                        com.soulenchants.bosses.BossDamage.apply(p, 22, boss);
                        p.setVelocity(toPlayer.normalize().multiply(1.2).setY(0.4));
                    }
                    for (int i = 0; i < 40; i++) {
                        double angle = -Math.PI / 3 + (Math.PI * 2 / 3) * (i / 39.0);
                        double dist = 1 + RNG.nextDouble() * 3;
                        Vector v = rotateY(forward, angle).multiply(dist);
                        Location p = loc.clone().add(v.getX(), 1 + RNG.nextDouble() * 1.5, v.getZ());
                        p.getWorld().playEffect(p, Effect.STEP_SOUND, org.bukkit.Material.REDSTONE_BLOCK.getId());
                    }
                    loc.getWorld().playSound(loc, Sound.ENDERDRAGON_HIT, 1.8f, 1.2f);
                    cancel();
                    return;
                }
                // Telegraph particles every 2 ticks
                if (t % 2 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double angle = -Math.PI / 3 + (Math.PI * 2 / 3) * (i / 9.0);
                        Vector v = rotateY(forward, angle).multiply(1 + RNG.nextDouble() * 2.5);
                        Location p = loc.clone().add(v.getX(), 0.8, v.getZ());
                        p.getWorld().playEffect(p, Effect.WITCH_MAGIC, 0);
                    }
                }
            }
        }.runTaskTimer(vw.getPlugin(), 0L, 1L);
    }

    public static void shatterBolt(Veilweaver vw) {
        LivingEntity boss = vw.getEntity();
        List<Player> players = vw.getArena().playersInArena();
        if (players.isEmpty()) return;
        for (int i = 0; i < 3; i++) {
            Player target = players.get(RNG.nextInt(players.size()));
            Snowball orb = boss.getWorld().spawn(boss.getEyeLocation(), Snowball.class);
            orb.setShooter(boss);
            Vector vel = target.getEyeLocation().toVector().subtract(boss.getEyeLocation().toVector()).normalize().multiply(1.6);
            orb.setVelocity(vel);
            // Trail + impact handled by ShatterBoltTracker
            new ShatterBoltTracker(vw, orb).start();
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.WITHER_SHOOT, 1.5f, 1.2f);
    }

    public static void minionWeave(Veilweaver vw) {
        LivingEntity boss = vw.getEntity();
        int count = 2 + RNG.nextInt(3);
        for (int i = 0; i < count; i++) {
            Location spawn = boss.getLocation().clone().add(RNG.nextDouble() * 4 - 2, 0, RNG.nextDouble() * 4 - 2);
            Silverfish s = (Silverfish) boss.getWorld().spawnEntity(spawn, EntityType.SILVERFISH);
            s.setMaxHealth(20);
            s.setHealth(20);
            s.setCustomName("§5Threadling");
            s.setCustomNameVisible(false);
            // 1.8.8: no Attribute API. Silverfish defaults are fine for MVP.
            vw.getMinions().add(s);
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.SILVERFISH_KILL, 1.5f, 0.7f);
    }

    // ── PHASE 2 ──────────────────────────────────────────────────────────────

    public static void dimensionalRift(Veilweaver vw) {
        LivingEntity boss = vw.getEntity();
        List<Player> players = vw.getArena().playersInArena();
        if (players.isEmpty()) return;
        Player target = players.get(RNG.nextInt(players.size()));
        // Telegraph at current boss location (fracturing portal) before teleport
        Location origin = boss.getLocation();
        boss.getWorld().playSound(origin, Sound.PORTAL_TRIGGER, 1.2f, 1.5f);
        for (int i = 0; i < 30; i++) origin.getWorld().playEffect(origin.clone().add(0, 0.5, 0), Effect.PORTAL, 0);
        // Teleport boss near target
        Location tp = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-3));
        boss.teleport(tp);
        boss.getWorld().playSound(tp, Sound.ENDERMAN_TELEPORT, 1.8f, 0.9f);
        // Burst at destination
        for (int i = 0; i < 20; i++) tp.getWorld().playEffect(tp.clone().add(0, 1, 0), Effect.PORTAL, 0);
        // Pull players toward rift
        Location rift = boss.getLocation();
        for (Player p : players) {
            if (p.getLocation().distanceSquared(rift) > 36) continue;
            Vector pull = rift.toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.5);
            p.setVelocity(pull);
        }
        // Spawn 1-2 endermen as adds
        int adds = 1 + RNG.nextInt(2);
        for (int i = 0; i < adds; i++) {
            Enderman e = (Enderman) boss.getWorld().spawnEntity(rift, EntityType.ENDERMAN);
            if (!players.isEmpty()) e.setTarget(players.get(RNG.nextInt(players.size())));
            vw.getMinions().add(e);
        }
        // Visual rift
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ > 30) { cancel(); return; }
                for (int i = 0; i < 16; i++) {
                    double a = (Math.PI * 2 * i) / 16.0;
                    Location p = rift.clone().add(Math.cos(a) * 2.5, 0.1, Math.sin(a) * 2.5);
                    rift.getWorld().playEffect(p, Effect.PORTAL, 0);
                }
            }
        }.runTaskTimer(vw.getPlugin(), 0L, 2L);
    }

    public static void loomLaser(Veilweaver vw) {
        LivingEntity boss = vw.getEntity();
        List<Player> players = vw.getArena().playersInArena();
        if (players.isEmpty()) return;
        final Player target = players.get(RNG.nextInt(players.size()));
        target.sendTitle("§5✦", "§dLoom Laser locked on you!");
        // Channel for 50 ticks (2.5s)
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 50) {
                    fireLaser(vw, target);
                    cancel();
                    return;
                }
                Location from = boss.getEyeLocation();
                Location to = target.getEyeLocation();
                Vector dir = to.toVector().subtract(from.toVector());
                double dist = dir.length();
                dir.normalize();
                for (double d = 0; d < dist; d += 0.5) {
                    Location p = from.clone().add(dir.clone().multiply(d));
                    p.getWorld().playEffect(p, Effect.PORTAL, 0);
                }
            }
        }.runTaskTimer(vw.getPlugin(), 0L, 1L);
    }

    private static void fireLaser(Veilweaver vw, Player target) {
        LivingEntity boss = vw.getEntity();
        Location from = boss.getEyeLocation();
        Location to = target.getEyeLocation();
        Vector dir = to.toVector().subtract(from.toVector()).normalize();
        double dist = from.distance(to);
        for (double d = 0; d < dist; d += 1.0) {
            Location p = from.clone().add(dir.clone().multiply(d));
            for (Entity e : p.getWorld().getNearbyEntities(p, 1.0, 1.0, 1.0)) {
                if (e instanceof Player) {
                    com.soulenchants.bosses.BossDamage.apply((Player) e, 30, boss);
                    ((Player) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 1));
                }
            }
            p.getWorld().playEffect(p, Effect.STEP_SOUND, org.bukkit.Material.OBSIDIAN.getId());
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.WITHER_HURT, 2.0f, 0.5f);
    }

    public static void echoClones(Veilweaver vw) {
        LivingEntity boss = vw.getEntity();
        for (int i = 0; i < 3; i++) {
            Location spawn = boss.getLocation().clone().add(RNG.nextDouble() * 6 - 3, 0, RNG.nextDouble() * 6 - 3);
            Skeleton clone = (Skeleton) boss.getWorld().spawnEntity(spawn, EntityType.SKELETON);
            clone.setSkeletonType(Skeleton.SkeletonType.WITHER);
            clone.setMaxHealth(80);
            clone.setHealth(80);
            clone.setCustomName("§8Echo of the Veilweaver");
            clone.setCustomNameVisible(true);
            clone.getEquipment().setItemInHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD));
            vw.getEchoClones().add(clone);
            final Skeleton finalClone = clone;
            // Spawn flair: portal burst + sound
            for (int j = 0; j < 15; j++)
                spawn.getWorld().playEffect(spawn.clone().add(0, 1, 0), Effect.PORTAL, 0);
            spawn.getWorld().playSound(spawn, Sound.ENDERMAN_TELEPORT, 1.2f, 0.7f);
            // Despawn after 15s
            new BukkitRunnable() {
                @Override public void run() { if (!finalClone.isDead()) finalClone.remove(); }
            }.runTaskLater(vw.getPlugin(), 300L);
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENDERDRAGON_GROWL, 1.5f, 0.6f);
    }

    // ── PHASE 3 ──────────────────────────────────────────────────────────────

    public static void realityFracture(Veilweaver vw) {
        LivingEntity boss = vw.getEntity();
        Location center = boss.getLocation();
        new BukkitRunnable() {
            int ring = 0;
            @Override public void run() {
                if (ring++ > 6) { cancel(); return; }
                final double radius = ring * 1.5;
                // Render expanding ring
                for (int i = 0; i < 32; i++) {
                    double a = (Math.PI * 2 * i) / 32.0;
                    Location p = center.clone().add(Math.cos(a) * radius, 0.5, Math.sin(a) * radius);
                    p.getWorld().playEffect(p, Effect.STEP_SOUND, org.bukkit.Material.NETHER_BRICK.getId());
                }
                // Damage players currently in this ring (within 0.8 blocks of radius)
                for (Player p : vw.getArena().playersInArena()) {
                    double dist = p.getLocation().distance(center);
                    if (Math.abs(dist - radius) < 0.8) {
                        com.soulenchants.bosses.BossDamage.apply(p, 25, boss);
                        // Levitation substitute: launch up + slowness on landing
                        p.setVelocity(p.getVelocity().setY(0.9));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2));
                    }
                }
            }
        }.runTaskTimer(vw.getPlugin(), 0L, 8L);
        boss.getWorld().playSound(center, Sound.WITHER_SPAWN, 1.5f, 1.2f);
    }

    public static void apocalypseWeave(Veilweaver vw) {
        LivingEntity boss = vw.getEntity();
        Location center = boss.getLocation();
        boss.setVelocity(new Vector(0, 1.5, 0));
        boss.getWorld().playSound(center, Sound.WITHER_DEATH, 2.0f, 0.7f);
        // Set invulnerable for 4 seconds via reflection-free flag handled in damage listener
        ApocalypseInvuln.setUntil(vw, System.currentTimeMillis() + 4000);
        // Spiral of bolts
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 12) { cancel(); return; }
                Player target = pickPlayer(vw);
                if (target == null) return;
                Snowball orb = boss.getWorld().spawn(boss.getEyeLocation(), Snowball.class);
                orb.setShooter(boss);
                Vector vel = target.getEyeLocation().toVector().subtract(boss.getEyeLocation().toVector()).normalize().multiply(1.4);
                Vector spiral = new Vector(Math.cos(t * 0.5), 0, Math.sin(t * 0.5)).multiply(0.3);
                orb.setVelocity(vel.add(spiral));
                new ShatterBoltTracker(vw, orb).start();
            }
        }.runTaskTimer(vw.getPlugin(), 0L, 8L);
        // Void storm: lightning rains for 8s
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 16) { cancel(); return; }
                for (Player p : vw.getArena().playersInArena()) {
                    if (RNG.nextDouble() < 0.4) {
                        Location strike = p.getLocation().clone().add(RNG.nextDouble() * 4 - 2, 0, RNG.nextDouble() * 4 - 2);
                        strike.getWorld().strikeLightningEffect(strike);
                        for (Entity e : strike.getWorld().getNearbyEntities(strike, 2.5, 3, 2.5)) {
                            if (e instanceof Player) com.soulenchants.bosses.BossDamage.apply((Player) e, 5, boss);
                        }
                    }
                }
            }
        }.runTaskTimer(vw.getPlugin(), 20L, 10L);
    }

    public static void finalThreadBind(Veilweaver vw) {
        LivingEntity boss = vw.getEntity();
        Player victim = vw.getTopDamager();
        if (victim == null) return;
        victim.sendTitle("§4✦ THREAD BIND ✦", "§cKill the boss before the bind completes!");
        org.bukkit.Bukkit.broadcastMessage("§5✦ §dThe Veilweaver binds " + victim.getName() + " in threads of fate!");
        // Damage every 0.5s for 6 seconds = 12 ticks * 10 = 120 ticks
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 12 || boss.isDead() || victim.isDead()) {
                    if (!victim.isDead() && t >= 12) victim.setHealth(0);
                    cancel();
                    return;
                }
                victim.setVelocity(new Vector(0, 0, 0));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 6));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20, -10));
                com.soulenchants.bosses.BossDamage.applyTrue(victim, 45.0 / 12.0, boss);
                // Visual chain
                Location from = boss.getEyeLocation();
                Location to = victim.getEyeLocation();
                Vector dir = to.toVector().subtract(from.toVector());
                double dist = dir.length();
                dir.normalize();
                for (double d = 0; d < dist; d += 0.4) {
                    Location p = from.clone().add(dir.clone().multiply(d));
                    p.getWorld().playEffect(p, Effect.PORTAL, 0);
                }
            }
        }.runTaskTimer(vw.getPlugin(), 0L, 10L);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Player pickPlayer(Veilweaver vw) {
        List<Player> players = vw.getArena().playersInArena();
        return players.isEmpty() ? null : players.get(RNG.nextInt(players.size()));
    }

    public static Vector rotateY(Vector v, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }
}
