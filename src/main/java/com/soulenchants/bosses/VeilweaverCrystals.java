package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Per-phase end crystal shield around the Veilweaver. While ANY crystal is
 * alive, all damage to the boss is blocked and a hint is shown to attackers.
 * Players must destroy the crystals to expose the boss.
 *
 * Crystals are tagged with NBT so the explosion + death listeners can:
 *   - Suppress block damage from the explosion (visual only)
 *   - Recognize "our" crystals when one dies and signal back here
 */
public final class VeilweaverCrystals {

    public static final String NBT_VW_CRYSTAL = "se_vw_crystal";

    /** Static registry: crystal UUID -> owning Veilweaver. Listeners read this. */
    public static final java.util.Map<UUID, Veilweaver> REGISTRY = new java.util.concurrent.ConcurrentHashMap<>();

    private final SoulEnchants plugin;
    private final Veilweaver vw;
    private final Set<UUID> active = new HashSet<>();
    private final List<BukkitRunnable> beams = new ArrayList<>();

    public VeilweaverCrystals(SoulEnchants plugin, Veilweaver vw) {
        this.plugin = plugin;
        this.vw = vw;
    }

    public boolean anyAlive() { return !active.isEmpty(); }

    /** Spawns N crystals in a ring around the boss. Removes any prior set first. */
    public void spawnRing(int count) {
        clearAll();
        Location bossLoc = vw.getEntity().getLocation();
        for (int i = 0; i < count; i++) {
            double angle = i * (Math.PI * 2 / count);
            Location spawn = bossLoc.clone().add(Math.cos(angle) * 7, 1, Math.sin(angle) * 7);
            // Find ground level
            for (int dy = 0; dy < 6; dy++) {
                if (spawn.clone().add(0, -1, 0).getBlock().getType().isSolid()) break;
                spawn.add(0, -1, 0);
            }
            EnderCrystal crystal = (EnderCrystal) spawn.getWorld().spawnEntity(spawn, EntityType.ENDER_CRYSTAL);
            try { new de.tr7zw.changeme.nbtapi.NBTEntity(crystal).setBoolean(NBT_VW_CRYSTAL, true); }
            catch (Throwable ignored) {}
            active.add(crystal.getUniqueId());
            REGISTRY.put(crystal.getUniqueId(), vw);
            // Visual flair
            for (int j = 0; j < 25; j++) spawn.getWorld().playEffect(spawn.clone().add(0, 1, 0), Effect.PORTAL, 0);
            spawn.getWorld().playSound(spawn, Sound.ENDERDRAGON_HIT, 1.0f, 1.6f);
        }
        startBeams();
        announceShield();
    }

    /** Tether beam visuals between boss and each living crystal. */
    private void startBeams() {
        BukkitRunnable beam = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                t++;
                if (vw.getEntity().isDead()) { cancel(); return; }
                if (active.isEmpty()) { cancel(); return; }
                Location bossLoc = vw.getEntity().getLocation().add(0, 1, 0);
                for (UUID id : new ArrayList<>(active)) {
                    org.bukkit.entity.Entity e = plugin.getServer().getWorlds().get(0).getEntities().stream()
                            .filter(x -> x.getUniqueId().equals(id)).findFirst().orElse(null);
                    if (e == null || e.isDead()) { active.remove(id); REGISTRY.remove(id); continue; }
                    Location cl = e.getLocation().add(0, 1, 0);
                    org.bukkit.util.Vector dir = cl.toVector().subtract(bossLoc.toVector());
                    double dist = dir.length();
                    if (dist < 0.1) continue;
                    dir.normalize();
                    int steps = Math.max(1, (int) (dist * 2));
                    for (int s = 0; s < steps; s++) {
                        Location pt = bossLoc.clone().add(dir.clone().multiply(s * 0.5));
                        pt.getWorld().playEffect(pt, Effect.WITCH_MAGIC, 0);
                    }
                }
            }
        };
        beam.runTaskTimer(plugin, 5L, 5L);
        beams.add(beam);
    }

    public void onCrystalDestroyed(UUID id) {
        active.remove(id);
        REGISTRY.remove(id);
        if (active.isEmpty()) {
            announceShieldBroken();
            // Boss takes a chunk of damage as reward for breaking the shield
            double bonus = vw.getEntity().getMaxHealth() * 0.05;
            double newHp = Math.max(1, vw.getEntity().getHealth() - bonus);
            vw.getEntity().setHealth(newHp);
        }
    }

    public void clearAll() {
        for (BukkitRunnable b : beams) try { b.cancel(); } catch (Exception ignored) {}
        beams.clear();
        for (UUID id : active) {
            REGISTRY.remove(id);
            for (org.bukkit.World w : plugin.getServer().getWorlds()) {
                w.getEntities().stream()
                        .filter(e -> e.getUniqueId().equals(id))
                        .findFirst().ifPresent(e -> e.remove());
            }
        }
        active.clear();
        // Belt-and-suspenders: scan ALL loaded worlds for crystals tagged as ours.
        // Catches leaks where a crystal was in an unloaded chunk during the previous
        // clear and never got removed via UUID lookup.
        for (org.bukkit.World w : plugin.getServer().getWorlds()) {
            for (org.bukkit.entity.Entity e : new java.util.ArrayList<>(w.getEntities())) {
                if (!(e instanceof org.bukkit.entity.EnderCrystal)) continue;
                try {
                    de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(e);
                    if (nbt.hasKey(NBT_VW_CRYSTAL) && nbt.getBoolean(NBT_VW_CRYSTAL)) {
                        REGISTRY.remove(e.getUniqueId());
                        e.remove();
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private void announceShield() {
        for (Player p : vw.nearbyPlayers(40)) {
            p.sendMessage(ChatColor.DARK_PURPLE + "✦ " + ChatColor.LIGHT_PURPLE
                    + "Crystal shield active! " + ChatColor.GRAY + "Destroy the End Crystals to damage the boss.");
            p.playSound(p.getLocation(), Sound.PORTAL_TRIGGER, 1f, 1.2f);
        }
    }

    private void announceShieldBroken() {
        for (Player p : vw.nearbyPlayers(40)) {
            p.sendMessage(ChatColor.DARK_PURPLE + "✦ " + ChatColor.LIGHT_PURPLE
                    + "Shield shattered! " + ChatColor.GRAY + "The Veilweaver is exposed.");
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_DEATH, 0.7f, 1.4f);
        }
    }
}
