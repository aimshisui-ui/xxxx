package com.soulenchants.pets.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.pets.Pet;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ember Fox — endgame fire offensive. Permanent Fire Resistance +
 * Strength I, and attackers always catch fire. Active spawns a three-ring
 * inferno: massive damage, ignite, and leaves lava-flame particles for 6s.
 */
public final class EmberFoxPet extends Pet {

    private static final long ACTIVE_CD_MS = 35_000L;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public EmberFoxPet(SoulEnchants plugin) { super(plugin); }

    @Override public String getId()            { return "ember_fox"; }
    @Override public String getDisplayName()   { return "Ember Fox"; }
    @Override public String getArchetype()     { return "Offensive"; }
    @Override public String getRarityColor()   { return MessageStyle.TIER_EPIC; }

    @Override public ItemStack buildCompanionHelmet() { return new ItemStack(Material.NETHERRACK); }
    @Override public ItemStack buildEggIcon()         { return new ItemStack(Material.BLAZE_POWDER); }

    @Override public List<String> getFlavorLore() {
        return Arrays.asList(
                "Kept in a coat of embers.",
                "Whatever bites it, bites back lighter."
        );
    }

    @Override public String getPassiveDescription() {
        return "Permanent Fire Resistance + Strength I. Attackers ignite for 4s on every hit.";
    }

    @Override public String getActiveDescription() {
        return "Inferno Ring — 8-block 3-ring burst · 10 dmg + 10s ignite + 6s residual flame aura. §735s CD.";
    }

    @Override
    public void onTick(Player owner, ArmorStand companion, int level) {
        owner.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 80, 0, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 80, 0, true, false), true);
        if (owner.getFireTicks() > 0) owner.setFireTicks(0);
        if (Math.random() < 0.25)
            companion.getWorld().playEffect(companion.getEyeLocation(), Effect.MOBSPAWNER_FLAMES, 0);
    }

    @Override
    public void onOwnerHurt(Player owner, ArmorStand companion, double damage, int level) {
        // Hit-back ignite — every attacker in a tight 4-block sphere catches fire
        // for 4s. Deliberately tight so the owner's own ground flames don't hit
        // far-away passives.
        Location loc = owner.getLocation();
        for (LivingEntity e : owner.getWorld().getLivingEntities()) {
            if (e.equals(owner) || e instanceof ArmorStand) continue;
            if (e.getLocation().distanceSquared(loc) > 16.0) continue;
            if (e.getFireTicks() < 80) e.setFireTicks(80);
        }
    }

    @Override
    public boolean onActivate(Player owner, ArmorStand companion, int level) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(owner.getUniqueId());
        if (last != null && now - last < ACTIVE_CD_MS) {
            owner.sendMessage(MessageStyle.BAD + "Ember Fox is warming up — "
                    + ((ACTIVE_CD_MS - (now - last)) / 1000L) + "s");
            return false;
        }
        cooldown.put(owner.getUniqueId(), now);
        Location loc = owner.getLocation();
        int hit = 0;
        for (LivingEntity e : owner.getWorld().getLivingEntities()) {
            if (e.equals(owner) || e instanceof ArmorStand) continue;
            if (e.getLocation().distanceSquared(loc) > 64.0) continue;
            e.damage(10.0, owner);
            e.setFireTicks(200);
            hit++;
        }
        // Three concentric particle rings
        for (int r : new int[]{3, 5, 8}) {
            for (int i = 0; i < 48; i++) {
                double ang = (Math.PI * 2) * i / 48.0;
                Location pLoc = loc.clone().add(Math.cos(ang) * r, 0.3, Math.sin(ang) * r);
                owner.getWorld().playEffect(pLoc, Effect.MOBSPAWNER_FLAMES, 0);
            }
        }
        // Residual 6s flame aura — schedule particles for the next 120 ticks.
        final Location center = loc.clone();
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks += 10;
                for (int i = 0; i < 20; i++) {
                    double ang = Math.random() * Math.PI * 2;
                    double radius = Math.random() * 7.0;
                    Location spray = center.clone().add(Math.cos(ang) * radius, 0.2, Math.sin(ang) * radius);
                    spray.getWorld().playEffect(spray, Effect.MOBSPAWNER_FLAMES, 0);
                }
                // Re-ignite anyone standing in it
                for (LivingEntity le : center.getWorld().getLivingEntities()) {
                    if (le.equals(owner) || le instanceof ArmorStand) continue;
                    if (le.getLocation().distanceSquared(center) > 49.0) continue;
                    if (le.getFireTicks() < 60) le.setFireTicks(60);
                }
                if (ticks >= 120) cancel();
            }
        }.runTaskTimer(plugin, 10L, 10L);
        owner.playSound(loc, org.bukkit.Sound.FIRE_IGNITE, 1.0f, 0.7f);
        owner.sendMessage(MessageStyle.TIER_EPIC + "✦ Ember Fox " + MessageStyle.MUTED + "scorched "
                + MessageStyle.VALUE + hit + MessageStyle.MUTED + " enemies.");
        return true;
    }
}
