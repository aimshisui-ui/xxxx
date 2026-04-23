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
 * Frost Sprite — endgame crowd-control companion. Non-player entities
 * within 10 blocks get Slow II + Mining Fatigue I passively. Active
 * locks everything in a 12-block sphere with Slow V + MF III + 6 dmg
 * per target, plus a particle shockwave.
 */
public final class IceSpritePet extends Pet {

    private static final long ACTIVE_CD_MS = 45_000L;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public IceSpritePet(SoulEnchants plugin) { super(plugin); }

    @Override public String getId()            { return "frost_sprite"; }
    @Override public String getDisplayName()   { return "Frost Sprite"; }
    @Override public String getArchetype()     { return "Crowd Control"; }
    @Override public String getRarityColor()   { return MessageStyle.TIER_EPIC; }

    @Override public ItemStack buildCompanionHelmet() { return new ItemStack(Material.ICE); }
    @Override public ItemStack buildEggIcon()         { return new ItemStack(Material.SNOW_BALL); }

    @Override public List<String> getFlavorLore() {
        return Arrays.asList(
                "It never melts. You never catch up.",
                "Winter wears it like a coat."
        );
    }

    @Override public String getPassiveDescription() {
        return "Enemies within 10 blocks get Slow II + Mining Fatigue I.";
    }

    @Override public String getActiveDescription() {
        return "Glacier Burst — 12-block AoE · Slow V + MF III for 5s + 6 dmg per target. §745s CD.";
    }

    @Override
    public void onTick(Player owner, ArmorStand companion, int level) {
        Location loc = owner.getLocation();
        if (Math.random() < 0.25)
            companion.getWorld().playEffect(companion.getEyeLocation(), Effect.SNOW_SHOVEL, 0);
        for (LivingEntity e : owner.getWorld().getLivingEntities()) {
            if (e.equals(owner) || e.equals(companion)) continue;
            if (e instanceof ArmorStand) continue;
            if (e.getLocation().distanceSquared(loc) > 100.0) continue;
            e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,         40, 1, true, false), true);
            e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40, 0, true, false), true);
        }
    }

    @Override
    public boolean onActivate(Player owner, ArmorStand companion, int level) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(owner.getUniqueId());
        if (last != null && now - last < ACTIVE_CD_MS) {
            owner.sendMessage(MessageStyle.BAD + "Frost Sprite gathering — "
                    + ((ACTIVE_CD_MS - (now - last)) / 1000L) + "s");
            return false;
        }
        cooldown.put(owner.getUniqueId(), now);
        Location loc = owner.getLocation();
        int hit = 0;
        for (LivingEntity e : owner.getWorld().getLivingEntities()) {
            if (e.equals(owner) || e instanceof ArmorStand) continue;
            if (e.getLocation().distanceSquared(loc) > 144.0) continue;
            e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,         100, 4, true, false), true);
            e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 100, 2, true, false), true);
            e.damage(6.0, owner);
            hit++;
        }
        // Particle shockwave — expanding ring of snow.
        for (int r = 1; r <= 12; r++) {
            double radius = r;
            for (int i = 0; i < 36; i++) {
                double ang = (Math.PI * 2) * i / 36.0;
                Location pLoc = loc.clone().add(Math.cos(ang) * radius, 0.3, Math.sin(ang) * radius);
                owner.getWorld().playEffect(pLoc, Effect.SNOW_SHOVEL, 0);
            }
        }
        owner.playSound(loc, org.bukkit.Sound.GLASS, 1.0f, 0.7f);
        owner.sendMessage(MessageStyle.TIER_EPIC + "✦ Frost Sprite " + MessageStyle.MUTED + "froze "
                + MessageStyle.VALUE + hit + MessageStyle.MUTED + " enemies.");
        return true;
    }
}
