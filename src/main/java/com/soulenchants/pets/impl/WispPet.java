package com.soulenchants.pets.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.pets.Pet;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Effect;
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
 * Ethereal Wisp — endgame utility companion. Heavy passive uptime +
 * a defining Phase active that repositions the owner and heals.
 */
public final class WispPet extends Pet {

    private static final long ACTIVE_CD_MS = 30_000L;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public WispPet(SoulEnchants plugin) { super(plugin); }

    @Override public String getId()            { return "wisp"; }
    @Override public String getDisplayName()   { return "Ethereal Wisp"; }
    @Override public String getArchetype()     { return "Utility"; }
    @Override public String getRarityColor()   { return MessageStyle.TIER_RARE; }

    @Override public ItemStack buildCompanionHelmet() { return new ItemStack(Material.GLOWSTONE); }
    @Override public ItemStack buildEggIcon()         { return new ItemStack(Material.GLOWSTONE_DUST); }

    @Override public List<String> getFlavorLore() {
        return Arrays.asList(
                "A curl of light that learned to follow.",
                "It watches the dark places for you."
        );
    }

    @Override public String getPassiveDescription() {
        return "Permanent Night Vision + Speed I + Regeneration I. +25% souls from kills.";
    }

    @Override public String getActiveDescription() {
        return "Phase — 8s Invisibility + Speed III + heal 8 HP. §730s CD.";
    }

    @Override
    public void onTick(Player owner, ArmorStand companion, int level) {
        owner.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,  140, 0, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,         140, 0, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,  140, 0, true, false), true);
        if (Math.random() < 0.3)
            companion.getWorld().playEffect(companion.getEyeLocation(), Effect.ENDER_SIGNAL, 0);
    }

    @Override
    public void onOwnerKill(Player owner, ArmorStand companion, LivingEntity victim, int level) {
        // +25% soul bonus — scales with Seer/Voidreaver-style flavor
        long base = 1 + (victim.getMaxHealth() > 40 ? 5 : 1);
        plugin.getSoulManager().add(owner, Math.max(1, (long)(base * 0.25)));
    }

    @Override
    public boolean onActivate(Player owner, ArmorStand companion, int level) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(owner.getUniqueId());
        if (last != null && now - last < ACTIVE_CD_MS) {
            owner.sendMessage(MessageStyle.BAD + "Wisp is recovering — "
                    + ((ACTIVE_CD_MS - (now - last)) / 1000L) + "s");
            return false;
        }
        cooldown.put(owner.getUniqueId(), now);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 0, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        160, 2, true, false), true);
        owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + 8.0));
        owner.getWorld().playEffect(owner.getLocation(), Effect.PORTAL, 0);
        owner.sendMessage(MessageStyle.TIER_RARE + "✦ Wisp " + MessageStyle.MUTED + "draped you in fog.");
        return true;
    }
}
