package com.soulenchants.pets;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Lifecycle + follow logic for every active pet.
 *
 *   • A companion is an invisible ArmorStand wearing the pet's visual
 *     helmet. It has no collision, no gravity, a floating nametag with
 *     the pet's display name + level, and drifts behind the owner.
 *   • One pet per player. Summoning a new one despawns the old one.
 *   • The follow task runs every 5 ticks: smooth-step each companion
 *     toward its owner's shoulder; teleport if distance > 24 blocks or
 *     if the owner changed worlds; despawn silently if the owner went
 *     offline.
 */
public final class PetManager {

    private static final double FOLLOW_OFFSET_BEHIND = 1.4;
    private static final double FOLLOW_OFFSET_SIDE   = 1.1;
    private static final double FOLLOW_OFFSET_UP     = 0.9;
    private static final double LERP_SPEED           = 0.35;
    private static final double TELEPORT_DIST        = 24.0;

    private final SoulEnchants plugin;
    private BukkitRunnable task;

    /** ownerUuid -> companion state */
    private final Map<UUID, Companion> active = new HashMap<>();

    public PetManager(SoulEnchants plugin) { this.plugin = plugin; }

    // ──────────────── Lifecycle ────────────────

    public void start() {
        task = new BukkitRunnable() {
            @Override public void run() { tickAll(); }
        };
        task.runTaskTimer(plugin, 5L, 5L);
    }

    public void stop() {
        if (task != null) { try { task.cancel(); } catch (Exception ignored) {} task = null; }
        for (Companion c : active.values()) c.despawnSilent();
        active.clear();
    }

    // ──────────────── Public API ────────────────

    /** Is there an active pet for this player? */
    public boolean hasActive(Player p) {
        return active.containsKey(p.getUniqueId());
    }

    /** Get the active companion for a player, or null. */
    public Companion getActive(Player p) {
        return active.get(p.getUniqueId());
    }

    /** Summon or replace this player's pet. Returns the new companion. */
    public Companion summon(Player owner, Pet pet, ItemStack egg) {
        Companion existing = active.remove(owner.getUniqueId());
        if (existing != null) existing.despawnSilent();

        int level = PetItem.levelOf(egg);
        long xp   = PetItem.xpOf(egg);
        UUID eggUid = PetItem.uidOf(egg);

        Location spawnLoc = owner.getLocation().add(0, 0.5, 0);
        ArmorStand stand = (ArmorStand) owner.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setSmall(true);
        stand.setMarker(false); // keep the nametag at a nice height
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setRemoveWhenFarAway(false);
        stand.setCustomName(renderName(pet, level));
        stand.setCustomNameVisible(true);
        EntityEquipment eq = stand.getEquipment();
        if (eq != null) eq.setHelmet(pet.buildCompanionHelmet());

        Companion c = new Companion(owner.getUniqueId(), pet, stand, eggUid, level);
        active.put(owner.getUniqueId(), c);
        Chat.good(owner, "Summoned " + pet.getRarityColor() + pet.getDisplayName()
                + MessageStyle.GOOD + " §7(Lv." + level + ")");
        return c;
    }

    /** Despawn this player's active pet. Returns true if there was one. */
    public boolean despawn(Player owner, boolean announce) {
        Companion c = active.remove(owner.getUniqueId());
        if (c == null) return false;
        c.despawnSilent();
        if (announce) Chat.info(owner, "Dismissed " + c.pet.getRarityColor() + c.pet.getDisplayName()
                + MessageStyle.MUTED + ".");
        return true;
    }

    /** Apply an XP delta to the currently-held egg matching the active pet's uid. */
    public void grantXp(Player owner, long amount) {
        Companion c = active.get(owner.getUniqueId());
        if (c == null || amount <= 0) return;

        for (int slot = 0; slot < owner.getInventory().getSize(); slot++) {
            ItemStack stack = owner.getInventory().getItem(slot);
            if (!PetItem.isPetEgg(stack)) continue;
            UUID eggUid = PetItem.uidOf(stack);
            if (eggUid == null || !eggUid.equals(c.eggUid)) continue;

            Pet pet = PetRegistry.get(PetItem.idOf(stack));
            if (pet == null) continue;

            int  prevLevel = PetItem.levelOf(stack);
            long prevXp    = PetItem.xpOf(stack);
            long newXp     = prevXp + amount;
            int  newLevel  = prevLevel;
            while (newLevel < Pet.MAX_LEVEL && newXp >= Pet.xpForLevel(newLevel + 1)) {
                newLevel++;
            }
            ItemStack updated = PetItem.withState(pet, newLevel, newXp, eggUid);
            owner.getInventory().setItem(slot, updated);
            if (newLevel > prevLevel) {
                Chat.good(owner, pet.getRarityColor() + pet.getDisplayName()
                        + MessageStyle.GOOD + " §llevelled up! §r§7Now Lv." + newLevel);
                c.level = newLevel;
                if (c.stand != null && c.stand.isValid())
                    c.stand.setCustomName(renderName(pet, newLevel));
                owner.playSound(owner.getLocation(), org.bukkit.Sound.LEVEL_UP, 0.8f, 1.4f);
            }
            return;
        }
    }

    // ──────────────── Tick ────────────────

    private void tickAll() {
        Iterator<Map.Entry<UUID, Companion>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Companion> e = it.next();
            Companion c = e.getValue();
            Player owner = plugin.getServer().getPlayer(e.getKey());
            if (owner == null || !owner.isOnline() || c.stand == null || !c.stand.isValid()) {
                c.despawnSilent();
                it.remove();
                continue;
            }
            followStep(owner, c);
            try {
                c.pet.onTick(owner, c.stand, c.level);
            } catch (Throwable t) {
                plugin.getLogger().warning("[pets] " + c.pet.getId() + " onTick threw: " + t);
            }
        }
    }

    private void followStep(Player owner, Companion c) {
        Location target = desiredLoc(owner);
        if (!target.getWorld().equals(c.stand.getWorld())
                || c.stand.getLocation().distance(target) > TELEPORT_DIST) {
            c.stand.teleport(target);
            return;
        }
        Vector delta = target.toVector().subtract(c.stand.getLocation().toVector()).multiply(LERP_SPEED);
        Location next = c.stand.getLocation().add(delta);
        next.setYaw(owner.getLocation().getYaw());
        next.setPitch(0f);
        c.stand.teleport(next);
    }

    private static Location desiredLoc(Player owner) {
        Location lo = owner.getLocation();
        double yawRad = Math.toRadians(lo.getYaw());
        // Behind + to the right (same convention as Minecraft "look direction").
        double bx = Math.sin(yawRad) * FOLLOW_OFFSET_BEHIND;
        double bz = -Math.cos(yawRad) * FOLLOW_OFFSET_BEHIND;
        double sx = Math.cos(yawRad) * FOLLOW_OFFSET_SIDE;
        double sz = Math.sin(yawRad) * FOLLOW_OFFSET_SIDE;
        return lo.clone().add(-bx + sx, FOLLOW_OFFSET_UP, -bz + sz);
    }

    private static String renderName(Pet pet, int level) {
        return pet.getRarityColor() + "✦ " + pet.getDisplayName()
                + " " + MessageStyle.MUTED + "Lv." + MessageStyle.VALUE + level;
    }

    // ──────────────── Companion holder ────────────────

    public static final class Companion {
        public final UUID owner;
        public final Pet  pet;
        public final ArmorStand stand;
        public final UUID eggUid;
        public int        level;

        Companion(UUID owner, Pet pet, ArmorStand stand, UUID eggUid, int level) {
            this.owner = owner; this.pet = pet; this.stand = stand;
            this.eggUid = eggUid; this.level = level;
        }

        void despawnSilent() {
            try { if (stand != null && stand.isValid()) stand.remove(); }
            catch (Throwable ignored) {}
        }
    }
}
