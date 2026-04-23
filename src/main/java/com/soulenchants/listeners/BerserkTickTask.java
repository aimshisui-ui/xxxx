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

public class BerserkTickTask extends BukkitRunnable {

    private static final java.util.Random RNG = new java.util.Random();
    private final SoulEnchants plugin;
    private final BerserkEnchant berserk = new BerserkEnchant();
    private final Map<UUID, Location> lastLoc = new HashMap<>();
    private final Map<UUID, Integer> stillTicks = new HashMap<>();
    private final Map<UUID, Set<PotionEffectType>> applied = new HashMap<>();
    // When set, skip applying Drunk's Slow/MF while this helmet is the one worn.
    // Cleared automatically when helmet is unequipped or replaced with a different one.
    private final Map<UUID, ItemStack> blessedHelmetSnapshot = new HashMap<>();
    /** Per-player tracking of last amp we wrote for each mythic-aura potion type.
     *  Lets us detect if the currently-active effect is "ours" (so we strip our
     *  bonus to find the underlying base) vs an external source we should stack on. */
    private final Map<UUID, Map<String, Integer>> auraApplied = new HashMap<>();

    public BerserkTickTask(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() { this.runTaskTimer(plugin, 20L, 20L); }

    public void clearPlayer(UUID id) {
        applied.remove(id);
        lastLoc.remove(id);
        stillTicks.remove(id);
        blessedHelmetSnapshot.remove(id);
        auraApplied.remove(id);
    }

    /** Called by /bless. Records the currently worn helmet so we can suppress its negatives. */
    public void bless(Player p) {
        ItemStack helmet = p.getInventory().getHelmet();
        if (helmet != null) blessedHelmetSnapshot.put(p.getUniqueId(), helmet.clone());
        else blessedHelmetSnapshot.remove(p.getUniqueId());
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) tickPlayer(p);
    }

    public void tickPlayer(Player p) {
        UUID id = p.getUniqueId();
        ItemStack helmet = p.getInventory().getHelmet();
        ItemStack chest  = p.getInventory().getChestplate();
        ItemStack legs   = p.getInventory().getLeggings();
        ItemStack boots  = p.getInventory().getBoots();
        ItemStack[] allArmor = new ItemStack[]{helmet, chest, legs, boots};

        // Maintain blessed-helmet invariant: clear when helmet changes or is removed
        ItemStack snapshot = blessedHelmetSnapshot.get(id);
        if (snapshot != null && (helmet == null || !snapshot.isSimilar(helmet))) {
            blessedHelmetSnapshot.remove(id);
            snapshot = null;
        }
        boolean drunkBlessed = snapshot != null && helmet != null && snapshot.isSimilar(helmet);

        int berserkLvl=0, implants=0, speed=0, adren=0, vital=0,
            aquatic=0, overshield=0;
        for (ItemStack a : allArmor) {
            if (a == null) continue;
            berserkLvl = Math.max(berserkLvl, ItemUtil.getLevel(a, "berserk"));
            implants   = Math.max(implants,   ItemUtil.getLevel(a, "implants"));
            speed      = Math.max(speed,      ItemUtil.getLevel(a, "speed"));
            adren      = Math.max(adren,      ItemUtil.getLevel(a, "adrenaline"));
            vital      = Math.max(vital,      ItemUtil.getLevel(a, "vital"));
            aquatic    = Math.max(aquatic,    ItemUtil.getLevel(a, "aquatic"));
            overshield = Math.max(overshield, ItemUtil.getLevel(a, "overshield"));
        }
        int drunk        = helmet == null ? 0 : ItemUtil.getLevel(helmet, "drunk");
        int nightvision  = helmet == null ? 0 : ItemUtil.getLevel(helmet, "nightvision");
        int saturation   = helmet == null ? 0 : ItemUtil.getLevel(helmet, "saturation");
        int clarity      = helmet == null ? 0 : ItemUtil.getLevel(helmet, "clarity");
        int depthstrider = boots  == null ? 0 : ItemUtil.getLevel(boots, "depthstrider");
        int jumpboost    = boots  == null ? 0 : ItemUtil.getLevel(boots, "jumpboost");
        int firewalker   = boots  == null ? 0 : ItemUtil.getLevel(boots, "firewalker");
        int voidwalker   = boots  == null ? 0 : ItemUtil.getLevel(boots, "voidwalker");

        Set<PotionEffectType> applyThisTick = new HashSet<>();

        if (berserkLvl > 0) berserk.onTick(p, berserkLvl);

        // Implants only regens when HP is below 50%. Lifebloom is now a death-trigger
        // enchant (heals allies on YOUR death) and no longer ticks here.
        boolean lowHp = p.getHealth() < (p.getMaxHealth() * 0.5);
        if (implants > 0 && lowHp) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false), true);
            applyThisTick.add(PotionEffectType.REGENERATION);
        }

        int finalSpeed = speed;
        if (adren > 0 && p.getHealth() <= 7.0 && adren > finalSpeed) finalSpeed = adren;
        if (depthstrider > 0 && p.getLocation().getBlock().isLiquid() && depthstrider > finalSpeed)
            finalSpeed = depthstrider;
        if (voidwalker > 0 && finalSpeed < 1) finalSpeed = 1; // Voidwalker grants permanent Speed I
        if (finalSpeed > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, finalSpeed - 1, true, false), true);
            applyThisTick.add(PotionEffectType.SPEED);
        }
        // Haste is now a pickaxe enchant fired on block break (BlockBreakListener) —
        // no longer a permanent boots-tick effect.
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
        // Mask passive auras — every attached mask grants a small set of
        // permanent potion effects (Speed, Strength, Resistance, etc.) while
        // the helmet is worn. Applied BEFORE clarity strip so none of the
        // mask auras get caught by the Poison/Blindness cleanup below.
        if (helmet != null) {
            String maskId = com.soulenchants.masks.MaskRegistry.attachedMaskId(helmet);
            if (maskId != null) {
                for (com.soulenchants.masks.Mask.Aura aura : com.soulenchants.masks.Mask.aurasFor(maskId)) {
                    p.addPotionEffect(new PotionEffect(aura.type, 60, aura.amp, true, false), true);
                    applyThisTick.add(aura.type);
                }
            }
        }

        // Clarity — strip POISON / BLINDNESS proportional to level.
        // L1 ≈ 33% strip-per-tick, L2 ≈ 66%, L3 = full immunity.
        if (clarity > 0) {
            boolean strip = clarity >= 3 || RNG.nextDouble() < 0.33 * clarity;
            if (strip) {
                if (p.hasPotionEffect(PotionEffectType.POISON))    p.removePotionEffect(PotionEffectType.POISON);
                if (p.hasPotionEffect(PotionEffectType.BLINDNESS)) p.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        }
        // Overshield is no longer a passive Absorption tick — it's been moved to a
        // chance-on-hit proc in CombatListener with a long cooldown.

        // Mythic held-sword aura (Crimson Tongue / Wraithcleaver). Stacks +1 tier
        // ON TOP of the player's current effect amp (drunk, external potions, etc.).
        ItemStack held = p.getItemInHand();
        int mythicLvl = held == null ? 0 : ItemUtil.getLevel(held, "mythic_held");
        if (mythicLvl == 1) {
            stackAura(p, PotionEffectType.INCREASE_DAMAGE, applyThisTick);
            stackAura(p, PotionEffectType.SPEED, applyThisTick);
        } else if (mythicLvl == 2) {
            stackAura(p, PotionEffectType.FAST_DIGGING, applyThisTick);
            stackAura(p, PotionEffectType.INCREASE_DAMAGE, applyThisTick);
        } else {
            // Not holding a mythic anymore — drop our memory so the strip logic
            // (below) actually clears the lingering aura effect.
            auraApplied.remove(id);
        }

        // Drunk: Strength is always applied; Slow + Mining Fatigue are suppressed if blessed.
        if (drunk > 0) {
            int strAmp = Math.min(drunk, 3) - 1;
            int slowAmp = drunk - 1;
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, strAmp, true, false), true);
            applyThisTick.add(PotionEffectType.INCREASE_DAMAGE);
            if (!drunkBlessed) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, slowAmp, true, false), true);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40, slowAmp, true, false), true);
                applyThisTick.add(PotionEffectType.SLOW);
                applyThisTick.add(PotionEffectType.SLOW_DIGGING);
            }
        }

        // Strip effects we previously applied but no longer should
        Set<PotionEffectType> prev = applied.getOrDefault(id, Collections.emptySet());
        for (PotionEffectType t : prev) {
            if (!applyThisTick.contains(t) && p.hasPotionEffect(t)) p.removePotionEffect(t);
        }
        applied.put(id, applyThisTick);

        // Vital + Heart-of-the-Forge stacks — all combined, self-correcting.
        // (Tier HP bonus was removed — only the Vital enchant grants +max HP now.)
        int heartHp = plugin.getLootProfile().bonusHpFor(p);
        double expectedMax = 20.0 + (vital * 2.0) + heartHp;
        if (Math.abs(p.getMaxHealth() - expectedMax) > 0.01) {
            p.setMaxHealth(expectedMax);
            if (p.getHealth() > expectedMax) p.setHealth(expectedMax);
        }

        lastLoc.put(id, p.getLocation());

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

    /**
     * Apply +1 amp on top of the player's current effect amp for `type`. If the
     * existing effect matches our last-applied amp we treat it as ours (so we
     * don't infinitely re-stack), otherwise we treat it as external and bump on
     * top. Adds the type to `applyThisTick` so the cleanup pass keeps it alive.
     */
    private void stackAura(Player p, PotionEffectType type, java.util.Set<PotionEffectType> applyThisTick) {
        UUID id = p.getUniqueId();
        PotionEffect existing = null;
        for (PotionEffect pe : p.getActivePotionEffects()) {
            if (pe.getType().equals(type)) { existing = pe; break; }
        }
        Map<String, Integer> myMap = auraApplied.get(id);
        Integer ourLast = (myMap == null) ? null : myMap.get(type.getName());
        int base;
        if (existing == null) {
            base = -1;
        } else if (ourLast != null && existing.getAmplifier() == ourLast) {
            // Our prior bonus is still showing — strip our +1 to find the underlying base
            base = ourLast - 1;
        } else {
            // External source (or higher than our last) — stack on top
            base = existing.getAmplifier();
        }
        int newAmp = Math.max(0, base + 1);
        p.addPotionEffect(new PotionEffect(type, 40, newAmp, true, false), true);
        if (myMap == null) {
            myMap = new HashMap<>();
            auraApplied.put(id, myMap);
        }
        myMap.put(type.getName(), newAmp);
        applyThisTick.add(type);
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
