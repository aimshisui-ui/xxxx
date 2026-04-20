package com.soulenchants.mobs;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mutable runtime definition of a custom mob. Fields are non-final so
 * /ce loot can edit stats + ability specs + drops live, and the next
 * spawn picks up the edits without restarting the server.
 *
 *   id           — registry key, also the NBT tag
 *   baseHp/.../.   — baseline from MobRegistry (never mutated, used for reset)
 *   maxHp/.../.    — live values (what the next spawn uses)
 *   drops          — legacy fixed drops (kept for back-compat)
 *   dropSpecs      — editable drop table with min/max/chance
 *   abilitySpecs   — editable ability list (data-form; resolved at spawn)
 */
public class CustomMob {

    public enum Tier {
        EARLY (ChatColor.WHITE,        "T1"),
        MID   (ChatColor.YELLOW,       "T2"),
        LATE  (ChatColor.LIGHT_PURPLE, "T3"),
        ELITE (ChatColor.GOLD,         "T4");

        public final ChatColor color;
        public final String label;
        Tier(ChatColor color, String label) { this.color = color; this.label = label; }
    }

    public final String id;
    public final String displayName;
    public final EntityType base;
    public final Tier tier;
    public final String biomeFilter;
    public final Skeleton.SkeletonType skeletonType;
    public final boolean isBaby;

    // Baselines — never mutated, used as "reset to defaults"
    public final int baseHp;
    public final double baseBonusDamage;
    public final int baseSouls;
    public final List<AbilitySpec> baseAbilities;
    public final List<DropSpec> baseDrops;

    // Live (editable)
    public int maxHp;
    public double bonusDamage;
    public int souls;
    public List<ItemStack> drops;          // legacy — fixed ItemStacks from registry
    public List<DropSpec> dropSpecs;       // editable drop table
    public List<AbilitySpec> abilitySpecs; // editable abilities

    // Per-entity resolved abilities (per-entity state for cooldowns)
    private static final Map<UUID, List<MobAbility>> RESOLVED = new HashMap<>();

    public CustomMob(String id, String displayName, EntityType base, Tier tier, int maxHp,
                     double bonusDamage, int souls, List<ItemStack> drops,
                     List<AbilitySpec> abilities, String biomeFilter,
                     Skeleton.SkeletonType skeletonType, boolean isBaby) {
        this.id = id;
        this.displayName = displayName;
        this.base = base;
        this.tier = tier;
        this.biomeFilter = biomeFilter;
        this.skeletonType = skeletonType;
        this.isBaby = isBaby;

        this.baseHp = maxHp;
        this.baseBonusDamage = bonusDamage;
        this.baseSouls = souls;
        this.baseAbilities = abilities != null ? Collections.unmodifiableList(new ArrayList<>(abilities)) : Collections.<AbilitySpec>emptyList();
        // Legacy ItemStack drops -> DropSpec at chance 1.0
        List<DropSpec> baseD = new ArrayList<>();
        if (drops != null) for (ItemStack is : drops) baseD.add(DropSpec.of(is, 1.0));
        this.baseDrops = Collections.unmodifiableList(baseD);

        this.maxHp = maxHp;
        this.bonusDamage = bonusDamage;
        this.souls = souls;
        this.drops = drops != null ? new ArrayList<>(drops) : new ArrayList<ItemStack>();
        this.dropSpecs = new ArrayList<>(baseD);
        this.abilitySpecs = new ArrayList<>(baseAbilities);
    }

    /** Reset all live values to the registry baselines. */
    public void resetToDefaults() {
        this.maxHp = baseHp;
        this.bonusDamage = baseBonusDamage;
        this.souls = baseSouls;
        this.abilitySpecs = new ArrayList<>(baseAbilities);
        this.dropSpecs = new ArrayList<>(baseDrops);
    }

    /** Resolve abilitySpecs → per-entity MobAbility list; cached by UUID. */
    public List<MobAbility> resolvedFor(UUID uuid) {
        return RESOLVED.get(uuid);
    }

    public static List<MobAbility> lookup(UUID uuid) { return RESOLVED.get(uuid); }

    public static void forget(UUID uuid) { RESOLVED.remove(uuid); }

    public LivingEntity spawn(Location loc) {
        // Force-load the chunk so the spawn doesn't silently fail in unloaded territory.
        try { loc.getChunk().load(true); } catch (Throwable ignored) {}
        LivingEntity le;
        try {
            org.bukkit.entity.Entity raw = loc.getWorld().spawnEntity(loc, base);
            if (raw == null) {
                org.bukkit.Bukkit.getLogger().warning("[SoulEnchants] [mob] spawnEntity returned null for "
                        + id + " @ " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                return null;
            }
            if (!(raw instanceof LivingEntity)) {
                org.bukkit.Bukkit.getLogger().warning("[SoulEnchants] [mob] spawnEntity gave non-living for "
                        + id + ": " + raw.getType());
                return null;
            }
            le = (LivingEntity) raw;
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().warning("[SoulEnchants] [mob] spawnEntity threw for " + id + ": " + t);
            return null;
        }
        try {
            configure(le);
            org.bukkit.Bukkit.getLogger().info("[SoulEnchants] [mob] spawned " + id
                    + " hp=" + le.getMaxHealth()
                    + " name=" + le.getCustomName()
                    + " uuid=" + le.getUniqueId());
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().warning("[SoulEnchants] [mob] configure threw for " + id + ": " + t);
            t.printStackTrace();
        }
        return le;
    }

    public void configure(LivingEntity le) {
        try {
            le.setMaxHealth(maxHp);
            le.setHealth(maxHp);
        } catch (Throwable ignored) {}
        le.setCustomName(tier.color + displayName);
        le.setCustomNameVisible(true);
        le.setRemoveWhenFarAway(false);
        if (le instanceof Skeleton && skeletonType != null) {
            ((Skeleton) le).setSkeletonType(skeletonType);
        }
        if (le instanceof Zombie) {
            ((Zombie) le).setBaby(isBaby);
        }
        // Tag the mob via BOTH metadata (in-memory, instant) and NBT (persistent
        // across chunk unloads). Metadata is the fast-path primary; NBT is
        // backup for after restarts. Re-attempt NBT on the next tick because in
        // 1.8.8 NBTAPI sometimes can't write to a freshly-spawned entity until
        // it's fully attached to the world.
        try {
            le.setMetadata("se_custom_mob",
                    new org.bukkit.metadata.FixedMetadataValue(
                            org.bukkit.Bukkit.getPluginManager().getPlugin("SoulEnchants"), id));
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().warning("[SoulEnchants] [mob] meta set failed for " + id + ": " + t);
        }
        applyNbtTag(le, id);
        // Retry the NBT write next tick — covers freshly-spawned entities that
        // weren't fully world-attached when configure first ran.
        final LivingEntity feLe = le;
        final String fid = id;
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("SoulEnchants"),
                new Runnable() { @Override public void run() {
                    if (feLe.isDead() || !feLe.isValid()) return;
                    applyNbtTag(feLe, fid);
                } }, 1L);
        // Auto-equip tier-appropriate gear (LATE = iron Prot III; ELITE = diamond Prot IV + Thorns).
        // Provides real defense (vanilla armor + protection enchants) and offense (sharpness on
        // weapons), without changing HP. Drop chance is 0 so the loot stays on the mob.
        applyTierGear(le);

        // Build per-entity ability instances (so leap/fireball/aoe cooldowns are per-mob)
        List<MobAbility> runtime = new ArrayList<>(abilitySpecs.size() + 1);
        // Stat-based bonus damage applied as an implicit ability (not in spec list)
        if (bonusDamage > 0) {
            final double bd = bonusDamage;
            runtime.add(new MobAbility() {
                @Override public void onHitPlayer(LivingEntity a, org.bukkit.entity.Player v,
                                                  org.bukkit.event.entity.EntityDamageByEntityEvent e) {
                    e.setDamage(e.getDamage() + bd);
                }
            });
        }
        for (AbilitySpec spec : abilitySpecs) {
            MobAbility a = spec.build();
            runtime.add(a);
            try { a.onSpawn(le); } catch (Throwable ignored) {}
        }
        RESOLVED.put(le.getUniqueId(), runtime);
    }

    public static String idOf(LivingEntity le) {
        if (le == null) return null;
        // Fast path: metadata (set on every spawn, in-memory only)
        try {
            java.util.List<org.bukkit.metadata.MetadataValue> meta = le.getMetadata("se_custom_mob");
            if (meta != null && !meta.isEmpty()) {
                String s = meta.get(0).asString();
                if (s != null && !s.isEmpty()) return s;
            }
        } catch (Throwable ignored) {}
        // Slow path: NBT (persisted to disk, survives chunk unload + restart)
        try {
            de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(le);
            if (!nbt.hasKey("se_custom_mob")) return null;
            return nbt.getString("se_custom_mob");
        } catch (Throwable t) { return null; }
    }

    private static void applyNbtTag(LivingEntity le, String id) {
        try {
            new de.tr7zw.changeme.nbtapi.NBTEntity(le).setString("se_custom_mob", id);
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().warning("[SoulEnchants] [mob] NBT set failed for " + id + ": " + t);
        }
    }

    /**
     * Idempotent re-configure for an EXISTING live entity. Re-applies every
     * piece of CustomMob state (HP, name, NBT tag, skeleton/zombie variant,
     * abilities cache, on-spawn effects) without resetting current HP. Safe
     * to call every tick.
     *
     * Used by MobListener when it spots a custom mob whose state has drifted
     * (RESOLVED wiped, custom name cleared, max HP back to vanilla, potion
     * effects worn off, etc.). Single source of truth that "this mob is still
     * a true custom mob, not the vanilla fallback."
     */
    public void rebuildResolved(LivingEntity le) {
        if (le == null) return;

        // Max HP — keep current HP relative to old max if we have to upgrade
        try {
            double oldMax = le.getMaxHealth();
            if (oldMax != maxHp) {
                double curHp = le.getHealth();
                le.setMaxHealth(maxHp);
                // If we just RAISED max, leave current HP alone (heal proportionally)
                // If we just LOWERED max, clamp current to new max
                if (curHp > maxHp) le.setHealth(maxHp);
            }
        } catch (Throwable ignored) {}

        // Custom name + visibility
        if (le.getCustomName() == null || !le.isCustomNameVisible()
                || !le.getCustomName().contains(displayName)) {
            le.setCustomName(tier.color + displayName);
            le.setCustomNameVisible(true);
        }
        le.setRemoveWhenFarAway(false);

        // Skeleton variant
        if (le instanceof Skeleton && skeletonType != null) {
            try {
                if (((Skeleton) le).getSkeletonType() != skeletonType) {
                    ((Skeleton) le).setSkeletonType(skeletonType);
                }
            } catch (Throwable ignored) {}
        }
        // Zombie baby flag
        if (le instanceof Zombie) {
            try {
                if (((Zombie) le).isBaby() != isBaby) {
                    ((Zombie) le).setBaby(isBaby);
                }
            } catch (Throwable ignored) {}
        }

        // Metadata tag (in-memory, fastpath for idOf)
        try {
            le.setMetadata("se_custom_mob",
                    new org.bukkit.metadata.FixedMetadataValue(
                            org.bukkit.Bukkit.getPluginManager().getPlugin("SoulEnchants"), id));
        } catch (Throwable ignored) {}
        // NBT tag — stamp it again if missing or wrong
        try {
            de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(le);
            if (!nbt.hasKey("se_custom_mob") || !id.equals(nbt.getString("se_custom_mob"))) {
                nbt.setString("se_custom_mob", id);
            }
        } catch (Throwable ignored) {}

        // Build abilities cache + re-fire onSpawn so permanent potion effects
        // (strength, resistance, etc.) come back if Spigot dropped them
        List<MobAbility> runtime = new ArrayList<>(abilitySpecs.size() + 1);
        if (bonusDamage > 0) {
            final double bd = bonusDamage;
            runtime.add(new MobAbility() {
                @Override public void onHitPlayer(LivingEntity a, org.bukkit.entity.Player v,
                                                  org.bukkit.event.entity.EntityDamageByEntityEvent e) {
                    e.setDamage(e.getDamage() + bd);
                }
            });
        }
        for (AbilitySpec spec : abilitySpecs) {
            MobAbility a = spec.build();
            runtime.add(a);
            try { a.onSpawn(le); } catch (Throwable ignored) {}
        }
        RESOLVED.put(le.getUniqueId(), runtime);
    }

    /** LATE gets iron Prot III + custom-enchant flavor; ELITE gets diamond Prot IV + Thorns II + heavier flavor. */
    private void applyTierGear(LivingEntity le) {
        org.bukkit.inventory.EntityEquipment eq = le.getEquipment();
        if (eq == null) return;
        if (tier != Tier.LATE && tier != Tier.ELITE) return;

        boolean elite = tier == Tier.ELITE;

        eq.setHelmet(armorPiece(elite ? Material.DIAMOND_HELMET : Material.IRON_HELMET,
                (elite ? ChatColor.LIGHT_PURPLE : ChatColor.GRAY) + "Veiled Helm",
                elite ? 4 : 3, false,
                "aquatic", 1, "nightvision", 1));
        eq.setChestplate(armorPiece(elite ? Material.DIAMOND_CHESTPLATE : Material.IRON_CHESTPLATE,
                (elite ? ChatColor.LIGHT_PURPLE : ChatColor.GRAY) + "Veiled Cuirass",
                elite ? 4 : 3, elite,
                "overshield", elite ? 4 : 2, "hardened", elite ? 5 : 3));
        eq.setLeggings(armorPiece(elite ? Material.DIAMOND_LEGGINGS : Material.IRON_LEGGINGS,
                (elite ? ChatColor.LIGHT_PURPLE : ChatColor.GRAY) + "Veiled Greaves",
                elite ? 4 : 3, false,
                "antiknockback", elite ? 2 : 1, "endurance", elite ? 2 : 1));
        eq.setBoots(armorPiece(elite ? Material.DIAMOND_BOOTS : Material.IRON_BOOTS,
                (elite ? ChatColor.LIGHT_PURPLE : ChatColor.GRAY) + "Veiled Treads",
                elite ? 4 : 3, false,
                "featherweight", 3, "speed", elite ? 3 : 2));

        // Hand weapon — zombies, pig zombies, AND ELITE skeletons (e.g. Hollow King).
        // Regular skeletons keep their bow for the AI to function; ELITE skeletons
        // get a sword so they engage as melee bosses with custom-enchant flair.
        boolean armWithSword = le.getType() == EntityType.ZOMBIE
                || le.getType() == EntityType.PIG_ZOMBIE
                || (elite && le.getType() == EntityType.SKELETON);
        if (armWithSword) {
            org.bukkit.inventory.ItemStack current = eq.getItemInHand();
            boolean replace = current == null || current.getType() == Material.AIR
                    || current.getType() == Material.GOLD_SWORD
                    || current.getType() == Material.BOW
                    || current.getType() == Material.STONE_SWORD;
            if (replace) {
                eq.setItemInHand(weaponPiece(
                        elite ? Material.DIAMOND_SWORD : Material.IRON_SWORD,
                        (elite ? ChatColor.LIGHT_PURPLE : ChatColor.GRAY) + "Veiled Edge",
                        elite ? 5 : 3, elite,
                        "bleed", elite ? 5 : 3, "soulburn", elite ? 5 : 3));
            }
        }

        // Don't drop the buffed gear — keeps the mob looking the part for its lifetime
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInHandDropChance(0f);
    }

    private static org.bukkit.inventory.ItemStack armorPiece(Material mat, String name,
                                                              int protLevel, boolean withThorns,
                                                              Object... customEnchantPairs) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        try { meta.spigot().setUnbreakable(true); } catch (Throwable ignored) {}
        try { meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, protLevel, true); } catch (Throwable ignored) {}
        if (withThorns) {
            try { meta.addEnchant(org.bukkit.enchantments.Enchantment.THORNS, 2, true); } catch (Throwable ignored) {}
        }
        try { meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 3, true); } catch (Throwable ignored) {}
        item.setItemMeta(meta);
        return applyCustomEnchants(item, customEnchantPairs);
    }

    private static org.bukkit.inventory.ItemStack weaponPiece(Material mat, String name,
                                                                int sharpnessLevel, boolean elite,
                                                                Object... customEnchantPairs) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        try { meta.spigot().setUnbreakable(true); } catch (Throwable ignored) {}
        try { meta.addEnchant(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, sharpnessLevel, true); } catch (Throwable ignored) {}
        try { meta.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, elite ? 2 : 1, true); } catch (Throwable ignored) {}
        if (elite) {
            try { meta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 2, true); } catch (Throwable ignored) {}
        }
        try { meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 3, true); } catch (Throwable ignored) {}
        item.setItemMeta(meta);
        return applyCustomEnchants(item, customEnchantPairs);
    }

    private static org.bukkit.inventory.ItemStack applyCustomEnchants(
            org.bukkit.inventory.ItemStack item, Object... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            try {
                String id = String.valueOf(pairs[i]);
                int lvl = ((Number) pairs[i + 1]).intValue();
                item = com.soulenchants.items.ItemUtil.addEnchant(item, id, lvl);
            } catch (Throwable ignored) {}
        }
        return item;
    }
}
