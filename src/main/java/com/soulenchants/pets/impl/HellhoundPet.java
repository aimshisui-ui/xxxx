package com.soulenchants.pets.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.pets.Pet;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hellhound — endgame offensive companion. Permanent Strength + Speed
 * with a stacking on-kill Haste; active enters Bloodfrenzy: Strength III
 * + Speed II + lifesteal on every hit for 8s.
 */
public final class HellhoundPet extends Pet {

    private static final long ACTIVE_CD_MS = 30_000L;
    private static final long FRENZY_LEN_MS = 8_000L;
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Map<UUID, Long> frenzyUntil = new HashMap<>();

    public HellhoundPet(SoulEnchants plugin) { super(plugin); }

    @Override public String getId()            { return "hellhound"; }
    @Override public String getDisplayName()   { return "Hellhound"; }
    @Override public String getArchetype()     { return "Offensive"; }
    @Override public String getRarityColor()   { return MessageStyle.TIER_LEGENDARY; }

    @Override public ItemStack buildCompanionHelmet() {
        return new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.ZOMBIE.ordinal());
    }
    @Override public ItemStack buildEggIcon() {
        return new ItemStack(Material.BONE);
    }

    @Override public List<String> getFlavorLore() {
        return Arrays.asList(
                "A beast with two shadows.",
                "It heels only for those who feed it in blood."
        );
    }

    @Override public String getPassiveDescription() {
        return "Permanent Strength I + Speed I. Mob kills grant Haste II for 4s (stacking refresh).";
    }

    @Override public String getActiveDescription() {
        return "Bloodfrenzy — 8s Strength III + Speed II + 25% lifesteal on every hit. §730s CD.";
    }

    @Override
    public void onTick(Player owner, ArmorStand companion, int level) {
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 80, 0, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           80, 0, true, false), true);
        if (Math.random() < 0.15)
            companion.getWorld().playEffect(companion.getEyeLocation(), Effect.MOBSPAWNER_FLAMES, 0);
    }

    @Override
    public void onOwnerKill(Player owner, ArmorStand companion, LivingEntity victim, int level) {
        // Stacking Haste II on kill — each kill refreshes 4s.
        owner.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 80, 1, true, false), true);
    }

    @Override
    public void onOwnerHurt(Player owner, ArmorStand companion, double damage, int level) {
        // While frenzying, owner hits back and lifesteal amplifies. We can't
        // see the hit target from here — the frenzy state is consumed in
        // onActivate's Strength window; this hook is kept for flavor hooks.
    }

    @Override
    public boolean onActivate(Player owner, ArmorStand companion, int level) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(owner.getUniqueId());
        if (last != null && now - last < ACTIVE_CD_MS) {
            owner.sendMessage(MessageStyle.BAD + "Hellhound is panting — "
                    + ((ACTIVE_CD_MS - (now - last)) / 1000L) + "s");
            return false;
        }
        cooldown.put(owner.getUniqueId(), now);
        frenzyUntil.put(owner.getUniqueId(), now + FRENZY_LEN_MS);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 160, 2, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           160, 1, true, false), true);
        owner.playSound(owner.getLocation(), org.bukkit.Sound.WOLF_HOWL, 1.0f, 0.9f);
        owner.getWorld().playEffect(owner.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
        owner.sendMessage(MessageStyle.TIER_LEGENDARY + "✦ Hellhound " + MessageStyle.MUTED + "bayed — frenzy lit.");
        return true;
    }

    /** PetListener hook-in — called from Bukkit EntityDamageByEntityEvent via the
     *  owner's damage pipeline. While the owner is frenzying, heal for 25% of
     *  dealt damage. Fires as a no-op outside the frenzy window. */
    public void onOwnerDealsMelee(Player owner, EntityDamageByEntityEvent event) {
        Long until = frenzyUntil.get(owner.getUniqueId());
        if (until == null || System.currentTimeMillis() > until) return;
        double heal = event.getDamage() * 0.25;
        owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + heal));
    }
}
