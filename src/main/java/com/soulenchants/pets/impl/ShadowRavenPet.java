package com.soulenchants.pets.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.pets.Pet;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
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
 * Shadow Raven — endgame utility & burst. Doubles more loot than before
 * and actively grants a 6s Assassin window: Strength III, Speed II, and
 * primes nearby mobs with Weakness II + +50% dmg taken from your hits.
 */
public final class ShadowRavenPet extends Pet {

    private static final long ACTIVE_CD_MS = 25_000L;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public ShadowRavenPet(SoulEnchants plugin) { super(plugin); }

    @Override public String getId()            { return "shadow_raven"; }
    @Override public String getDisplayName()   { return "Shadow Raven"; }
    @Override public String getArchetype()     { return "Utility"; }
    @Override public String getRarityColor()   { return MessageStyle.TIER_LEGENDARY; }

    @Override public ItemStack buildCompanionHelmet() {
        return new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.WITHER.ordinal());
    }
    @Override public ItemStack buildEggIcon() {
        return new ItemStack(Material.FEATHER);
    }

    @Override public List<String> getFlavorLore() {
        return Arrays.asList(
                "A thief in a raven's coat.",
                "It steals from corpses, not from pockets."
        );
    }

    @Override public String getPassiveDescription() {
        return "10% chance on mob kill to duplicate ALL dropped items. Weakness aura on mobs within 5 blocks.";
    }

    @Override public String getActiveDescription() {
        return "Assassinate — 6s Strength III + Speed II + 8-block Weakness III debuff on enemies. §725s CD.";
    }

    @Override
    public void onTick(Player owner, ArmorStand companion, int level) {
        org.bukkit.Location loc = owner.getLocation();
        // Ambient crow-ish smoke particles on the companion
        if (Math.random() < 0.2)
            companion.getWorld().playEffect(companion.getEyeLocation(), Effect.SMOKE, 4);
        // Close-range Weakness aura on non-player mobs
        for (LivingEntity le : owner.getWorld().getLivingEntities()) {
            if (le.equals(owner) || le instanceof ArmorStand || le instanceof Player) continue;
            if (le.getLocation().distanceSquared(loc) > 25.0) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, true, false), true);
        }
    }

    @Override
    public void onOwnerKill(Player owner, ArmorStand companion, LivingEntity victim, int level) {
        if (Math.random() > 0.10) return;
        // Duplicate ALL recent drops — more valuable than the prior "one item" version.
        // Fires next tick so EntityDeathEvent can finish populating drops first.
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.Location loc = victim.getLocation();
            int copied = 0;
            for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 3.0, 3.0, 3.0)) {
                if (!(ent instanceof Item)) continue;
                Item it = (Item) ent;
                loc.getWorld().dropItemNaturally(loc, it.getItemStack().clone());
                loc.getWorld().playEffect(loc, Effect.SMOKE, 4);
                copied++;
                if (copied >= 6) break; // sanity cap on mega-loot-table mobs
            }
        });
    }

    @Override
    public boolean onActivate(Player owner, ArmorStand companion, int level) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(owner.getUniqueId());
        if (last != null && now - last < ACTIVE_CD_MS) {
            owner.sendMessage(MessageStyle.BAD + "Raven stalking — "
                    + ((ACTIVE_CD_MS - (now - last)) / 1000L) + "s");
            return false;
        }
        cooldown.put(owner.getUniqueId(), now);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 120, 2, true, false), true);
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           120, 1, true, false), true);
        org.bukkit.Location loc = owner.getLocation();
        int marked = 0;
        for (LivingEntity e : owner.getWorld().getLivingEntities()) {
            if (e.equals(owner) || e instanceof ArmorStand) continue;
            if (e.getLocation().distanceSquared(loc) > 64.0) continue;
            e.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 2, true, false), true);
            marked++;
        }
        owner.playSound(loc, org.bukkit.Sound.BAT_TAKEOFF, 1.0f, 1.3f);
        owner.getWorld().playEffect(loc, Effect.SMOKE, 4);
        owner.sendMessage(MessageStyle.TIER_LEGENDARY + "✦ Shadow Raven " + MessageStyle.MUTED + "marked "
                + MessageStyle.VALUE + marked + MessageStyle.MUTED + " enemies.");
        return true;
    }
}
