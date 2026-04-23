package com.soulenchants.pets.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.pets.Pet;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
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
 * Stone Guardian — endgame defensive companion. Passive is a strong
 * permanent Resistance + Health Boost floor; active drops a Bulwark
 * that grants 8 hearts of Absorption, full Resistance III, and knocks
 * back every nearby enemy on cast.
 */
public final class GuardianPet extends Pet {

    private static final long ACTIVE_CD_MS = 40_000L;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public GuardianPet(SoulEnchants plugin) { super(plugin); }

    @Override public String getId()            { return "guardian"; }
    @Override public String getDisplayName()   { return "Stone Guardian"; }
    @Override public String getArchetype()     { return "Defensive"; }
    @Override public String getRarityColor()   { return MessageStyle.TIER_EPIC; }

    @Override public ItemStack buildCompanionHelmet() { return new ItemStack(Material.IRON_BLOCK); }
    @Override public ItemStack buildEggIcon()         { return new ItemStack(Material.IRON_INGOT); }

    @Override public List<String> getFlavorLore() {
        return Arrays.asList(
                "Carved from the bones of a forge.",
                "It remembers every blow you've taken."
        );
    }

    @Override public String getPassiveDescription() {
        return "Permanent Resistance I + Health Boost II (+4 hearts max HP).";
    }

    @Override public String getActiveDescription() {
        return "Bulwark — 8 hearts of Absorption + Resistance III for 8s + knockback pulse. §740s CD.";
    }

    // Bulwark's Absorption amp 7 = 16 HP = 8 hearts, matching the lore.

    @Override
    public void onTick(Player owner, ArmorStand companion, int level) {
        owner.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 80, 0, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST,      80, 1, true, false), true);
        if (Math.random() < 0.15)
            companion.getWorld().playEffect(companion.getEyeLocation(), Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
    }

    @Override
    public boolean onActivate(Player owner, ArmorStand companion, int level) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(owner.getUniqueId());
        if (last != null && now - last < ACTIVE_CD_MS) {
            owner.sendMessage(MessageStyle.BAD + "Guardian is recovering — "
                    + ((ACTIVE_CD_MS - (now - last)) / 1000L) + "s");
            return false;
        }
        cooldown.put(owner.getUniqueId(), now);
        // 8 hearts of Absorption (amp 7 = 16 HP = 8 hearts) + Resistance III for 8s
        owner.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,         160, 7, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,  160, 2, true, false), true);
        // Knockback pulse — everything in 6 blocks gets shoved away.
        org.bukkit.Location loc = owner.getLocation();
        for (org.bukkit.entity.Entity near : owner.getWorld().getNearbyEntities(loc, 6, 4, 6)) {
            if (!(near instanceof org.bukkit.entity.LivingEntity)) continue;
            if (near == owner) continue;
            org.bukkit.util.Vector push = near.getLocation().toVector()
                    .subtract(loc.toVector()).setY(0).normalize().multiply(1.3);
            push.setY(0.55);
            near.setVelocity(push);
            near.getWorld().playEffect(near.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
        }
        owner.getWorld().createExplosion(loc.getX(), loc.getY() + 1, loc.getZ(), 0f, false);
        owner.playSound(loc, org.bukkit.Sound.ANVIL_LAND, 0.8f, 1.2f);
        owner.sendMessage(MessageStyle.TIER_EPIC + "✦ Guardian " + MessageStyle.MUTED + "raised a bulwark around you.");
        return true;
    }
}
