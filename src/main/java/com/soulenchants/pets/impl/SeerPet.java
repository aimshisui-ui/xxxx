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
 * The Seer — endgame progression companion. Permanent Haste II,
 * Resistance I, +50% souls on every mob kill, and a flat +25% XP
 * bonus paid via giveExp. Active Mark reveals every nearby entity
 * with witch-magic particles and buffs the owner with Strength II +
 * Resistance II for 12s.
 */
public final class SeerPet extends Pet {

    private static final long ACTIVE_CD_MS = 40_000L;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public SeerPet(SoulEnchants plugin) { super(plugin); }

    @Override public String getId()            { return "seer"; }
    @Override public String getDisplayName()   { return "The Seer"; }
    @Override public String getArchetype()     { return "Progression"; }
    @Override public String getRarityColor()   { return MessageStyle.TIER_RARE; }

    @Override public ItemStack buildCompanionHelmet() { return new ItemStack(Material.ENCHANTMENT_TABLE); }
    @Override public ItemStack buildEggIcon()         { return new ItemStack(Material.EXP_BOTTLE); }

    @Override public List<String> getFlavorLore() {
        return Arrays.asList(
                "Reads the tally of the dead.",
                "Not a fortune-teller. A bookkeeper."
        );
    }

    @Override public String getPassiveDescription() {
        return "Permanent Haste II + Resistance I. Kills grant +50% souls and a flat XP bonus.";
    }

    @Override public String getActiveDescription() {
        return "Mark — reveal everything in 20 blocks + 12s Strength II + Resistance II for yourself. §740s CD.";
    }

    @Override
    public void onTick(Player owner, ArmorStand companion, int level) {
        owner.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING,       80, 1, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,  80, 0, true, false), true);
        if (Math.random() < 0.2)
            companion.getWorld().playEffect(companion.getEyeLocation(), Effect.WITCH_MAGIC, 0);
    }

    @Override
    public void onOwnerKill(Player owner, ArmorStand companion, LivingEntity victim, int level) {
        // Flat XP bonus — scales with pet level.
        owner.giveExp(3 + level / 6);
        // +50% souls on kill — small but compounds over endgame farm.
        long base = 2L + (victim.getMaxHealth() > 40 ? 6L : 0L);
        plugin.getSoulManager().add(owner, Math.max(1L, base / 2));
    }

    @Override
    public boolean onActivate(Player owner, ArmorStand companion, int level) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(owner.getUniqueId());
        if (last != null && now - last < ACTIVE_CD_MS) {
            owner.sendMessage(MessageStyle.BAD + "Seer is reading — "
                    + ((ACTIVE_CD_MS - (now - last)) / 1000L) + "s");
            return false;
        }
        cooldown.put(owner.getUniqueId(), now);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,   240, 1, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 240, 1, true, false), true);
        Location loc = owner.getLocation();
        int revealed = 0;
        for (LivingEntity e : owner.getWorld().getLivingEntities()) {
            if (e.equals(owner) || e instanceof ArmorStand) continue;
            if (e.getLocation().distanceSquared(loc) > 400.0) continue;
            Location target = e.getEyeLocation();
            for (int i = 0; i < 12; i++) {
                target.getWorld().playEffect(target, Effect.WITCH_MAGIC, 0);
            }
            revealed++;
        }
        owner.playSound(loc, org.bukkit.Sound.ORB_PICKUP, 1.0f, 1.8f);
        owner.sendMessage(MessageStyle.TIER_RARE + "✦ Seer " + MessageStyle.MUTED + "marked "
                + MessageStyle.VALUE + revealed + MessageStyle.MUTED + " nearby presences.");
        return true;
    }
}
