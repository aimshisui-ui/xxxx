package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.enchants.impl.BerserkEnchant;
import com.soulenchants.items.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Applies passive armor + helmet effects every tick cycle.
 * Tracks which potion effect types WE applied per player so we can strip them
 * immediately when the armor is removed (or after death).
 */
public class BerserkTickTask extends BukkitRunnable {

    private final SoulEnchants plugin;
    private final BerserkEnchant berserk = new BerserkEnchant();
    private final Map<UUID, Location> lastLoc = new HashMap<>();
    private final Map<UUID, Integer> stillTicks = new HashMap<>();
    private final Map<UUID, Double> vitalBoost = new HashMap<>();
    // Effects we applied last cycle, per player
    private final Map<UUID, Set<PotionEffectType>> applied = new HashMap<>();

    public BerserkTickTask(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() { this.runTaskTimer(plugin, 20L, 20L); }

    /** Clear tracked state for a player (call on death / respawn). */
    public void clearPlayer(UUID id) {
        applied.remove(id);
        vitalBoost.remove(id);
        lastLoc.remove(id);
        stillTicks.remove(id);
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            tickPlayer(p);
        }
    }

    public void tickPlayer(Player p) {
        UUID id = p.getUniqueId();
        ItemStack helmet = p.getInventory().getHelmet();
        ItemStack chest  = p.getInventory().getChestplate();
        ItemStack legs   = p.getInventory().getLeggings();
        ItemStack boots  = p.getInventory().getBoots();
        ItemStack[] allArmor = new ItemStack[]{helmet, chest, legs, boots};

        int berserkLvl=0, implants=0, speed=0, adren=0, vital=0,
            aquatic=0, lifebloom=0, overshield=0;
        for (ItemStack a : allArmor) {
            if (a == null) continue;
            berserkLvl = Math.max(berserkLvl, ItemUtil.getLevel(a, "berserk"));
            implants   = Math.max(implants,   ItemUtil.getLevel(a, "implants"));
            speed      = Math.max(speed,      ItemUtil.getLevel(a, "speed"));
            adren      = Math.max(adren,      ItemUtil.getLevel(a, "adrenaline"));
            vital      = Math.max(vital,      ItemUtil.getLevel(a, "vital"));
            aquatic    = Math.max(aquatic,    ItemUtil.getLevel(a, "aquatic"));
            lifebloom  = Math.max(lifebloom,  ItemUtil.getLevel(a, "lifebloom"));
            overshield = Math.max(overshield, ItemUtil.getLevel(a, "overshield"));
        }
        int drunk       = helmet == null ? 0 : ItemUtil.getLevel(helmet, "drunk");
        int nightvision = helmet == null ? 0 : ItemUtil.getLevel(helmet, "nightvision");
        int saturation  = helmet == null ? 0 : ItemUtil.getLevel(helmet, "saturation");
        int depthstrider= boots  == null ? 0 : ItemUtil.getLevel(boots, "depthstrider");
        int haste       = boots  == null ? 0 : ItemUtil.getLevel(boots, "haste");
        int jumpboost   = boots  == null ? 0 : ItemUtil.getLevel(boots, "jumpboost");
        int firewalker  = boots  == null ? 0 : ItemUtil.getLevel(boots, "firewalker");

        Set<PotionEffectType> applyThisTick = new HashSet<>();

        if (berserkLvl > 0) berserk.onTick(p, berserkLvl);

        if (implants > 0 || (lifebloom > 0 && isStanding(p, id))) {
            int lvl = Math.max(implants, (lifebloom > 0 && isStanding(p, id)) ? lifebloom : 0);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, lvl - 1, true, false), true);
            applyThisTick.add(PotionEffectType.REGENERATION);
        }

        int finalSpeed = speed;
        if (adren > 0 && p.getHealth() <= 7.0 && adren > finalSpeed) finalSpeed = adren;
        if (depthstrider > 0 && p.getLocation().getBlock().isLiquid() && depthstrider > finalSpeed)
            finalSpeed = depthstrider;
        if (finalSpeed > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, finalSpeed - 1, true, false), true);
            applyThisTick.add(PotionEffectType.SPEED);
        }
        if (haste > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 40, haste - 1, true, false), true);
            applyThisTick.add(PotionEffectType.FAST_DIGGING);
        }
        if (jumpboost > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 40, jumpboost - 1, true, false), true);
            applyThisTick.add(PotionEffectType.JUMP);
        }
        if (firewalker > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false), true);
            if (p.getFireTicks() > 0) p.setFireTicks(0);
            applyThisTick.add(PotionEffectType.FIRE_RESISTANCE);
        }
        if (nightvision > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 280, 0, true, false), true);
            applyThisTick.add(PotionEffectType.NIGHT_VISION);
        }
        if (saturation > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, saturation - 1, true, false), true);
            applyThisTick.add(PotionEffectType.SATURATION);
        }
        if (aquatic > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0, true, false), true);
            applyThisTick.add(PotionEffectType.WATER_BREATHING);
        }
        if (overshield > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60, overshield - 1, true, false), true);
            applyThisTick.add(PotionEffectType.ABSORPTION);
        }
        if (drunk > 0) {
            int strAmp = Math.min(drunk, 3) - 1;
            int slowAmp = drunk - 1;
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, strAmp, true, false), true);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, slowAmp, true, false), true);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40, slowAmp, true, false), true);
            applyThisTick.add(PotionEffectType.INCREASE_DAMAGE);
            applyThisTick.add(PotionEffectType.SLOW);
            applyThisTick.add(PotionEffectType.SLOW_DIGGING);
        }

        // Strip effects we previously applied but no longer should
        Set<PotionEffectType> prev = applied.getOrDefault(id, Collections.emptySet());
        for (PotionEffectType t : prev) {
            if (!applyThisTick.contains(t) && p.hasPotionEffect(t)) p.removePotionEffect(t);
        }
        applied.put(id, applyThisTick);

        // Vital — always set max HP directly (self-correcting, no delta tracking)
        double expectedMax = 20.0 + (vital * 2.0);
        if (Math.abs(p.getMaxHealth() - expectedMax) > 0.01) {
            p.setMaxHealth(expectedMax);
            if (p.getHealth() > expectedMax) p.setHealth(expectedMax);
        }

        // Track position for Lifebloom stillness check next tick
        lastLoc.put(id, p.getLocation());

        // Magnetism
        if (hasMagnetism(p)) applyMagnetism(p);
    }

    private boolean isStanding(Player p, UUID id) {
        Location now = p.getLocation();
        Location last = lastLoc.get(id);
        if (last == null || !last.getWorld().equals(now.getWorld())) return false;
        boolean still = last.distanceSquared(now) < 0.5;
        int count = stillTicks.getOrDefault(id, 0);
        stillTicks.put(id, still ? count + 1 : 0);
        return still && count >= 2;
    }

    private boolean hasMagnetism(Player p) {
        for (ItemStack a : p.getInventory().getArmorContents()) {
            if (a != null && ItemUtil.getLevel(a, "magnetism") > 0) return true;
        }
        return false;
    }

    private void applyMagnetism(Player p) {
        for (Entity e : p.getNearbyEntities(5, 3, 5)) {
            if (!(e instanceof Item)) continue;
            if (e.getTicksLived() < 10) continue;
            Vector pull = p.getLocation().toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.4);
            e.setVelocity(pull);
        }
    }
}
