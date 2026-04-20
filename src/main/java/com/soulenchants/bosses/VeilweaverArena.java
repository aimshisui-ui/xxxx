package com.soulenchants.bosses;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VeilweaverArena {

    public static final double RADIUS = 17.0;
    public static final double RADIUS_SQ = RADIUS * RADIUS;
    public static final double ORB_RADIUS = 14.0;

    private final Location center;
    private final List<EnderCrystal> loomOrbs = new ArrayList<>();
    private int particleTick = 0;
    private final Map<UUID, Integer> pushCooldown = new HashMap<>();
    private final Map<UUID, Integer> messageCooldown = new HashMap<>();

    public VeilweaverArena(Location center) {
        this.center = center.clone();
    }

    public Location getCenter() { return center.clone(); }

    public void spawn() {
        // Loom orbs disabled — they read as "extra crystals" alongside the
        // proper shield crystals and confused the encounter. The arena border
        // particle effect (tickBorder) is enough to mark the fight zone.
    }

    public void tickBorder(LivingEntity boss) {
        particleTick++;
        // Soft border: render particles at edge every few ticks
        if (particleTick % 5 == 0) {
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2 * i) / 24.0;
                double x = center.getX() + Math.cos(angle) * RADIUS;
                double z = center.getZ() + Math.sin(angle) * RADIUS;
                for (int dy = 0; dy < 6; dy += 2) {
                    Location particleLoc = new Location(center.getWorld(), x, center.getY() + dy, z);
                    center.getWorld().playEffect(particleLoc, Effect.PORTAL, 0);
                }
            }
        }
        // Decrement cooldowns for everyone
        pushCooldown.replaceAll((k, v) -> Math.max(0, v - 1));
        messageCooldown.replaceAll((k, v) -> Math.max(0, v - 1));

        // Push players back if outside border (gated by per-player cooldown)
        for (Player p : center.getWorld().getPlayers()) {
            if (isInArena(p.getLocation())) continue;
            UUID id = p.getUniqueId();
            int pushCd = pushCooldown.getOrDefault(id, 0);
            if (pushCd > 0) continue;
            Location pushTo = center.clone();
            pushTo.setY(p.getLocation().getY());
            org.bukkit.util.Vector dir = pushTo.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.45);
            p.setVelocity(dir.setY(0.15));
            pushCooldown.put(id, 30); // 1.5s before next push
            int msgCd = messageCooldown.getOrDefault(id, 0);
            if (msgCd <= 0) {
                p.sendMessage("§5✦ §dThe Veil pulls you back toward the Loom...");
                messageCooldown.put(id, 100); // 5s between messages
            }
        }
    }

    public boolean isInArena(Location loc) {
        if (!loc.getWorld().equals(center.getWorld())) return false;
        double dx = loc.getX() - center.getX();
        double dz = loc.getZ() - center.getZ();
        return dx * dx + dz * dz <= RADIUS_SQ;
    }

    public boolean noPlayersInArena() {
        for (Player p : center.getWorld().getPlayers()) {
            if (isInArena(p.getLocation())) return false;
        }
        return true;
    }

    public List<Player> playersInArena() {
        List<Player> list = new ArrayList<>();
        for (Player p : center.getWorld().getPlayers()) {
            if (isInArena(p.getLocation())) list.add(p);
        }
        return list;
    }

    public int aliveLoomOrbs() {
        Iterator<EnderCrystal> it = loomOrbs.iterator();
        int count = 0;
        while (it.hasNext()) {
            EnderCrystal o = it.next();
            if (o == null || o.isDead()) it.remove();
            else count++;
        }
        return count;
    }

    public void destroyOrbs(int amount) {
        Iterator<EnderCrystal> it = loomOrbs.iterator();
        int destroyed = 0;
        while (it.hasNext() && destroyed < amount) {
            EnderCrystal o = it.next();
            if (o != null && !o.isDead()) {
                Location loc = o.getLocation();
                loc.getWorld().createExplosion(loc, 0f, false);
                o.remove();
                it.remove();
                destroyed++;
            }
        }
    }

    public void cleanup() {
        for (EnderCrystal o : loomOrbs) if (o != null && !o.isDead()) o.remove();
        loomOrbs.clear();
    }
}
