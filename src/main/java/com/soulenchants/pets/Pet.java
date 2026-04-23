package com.soulenchants.pets;

import com.soulenchants.SoulEnchants;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Base class for every pet. A pet is a hybrid entity:
 *
 *   • INVENTORY ITEM — the "egg" players carry. Right-click summons the
 *     companion; right-click again despawns it. Identity lives on the
 *     item via se_pet_id NBT + per-instance se_pet_uid so XP/level stay
 *     pinned to that egg rather than the pet type.
 *
 *   • SPAWNED COMPANION — an invisible ArmorStand wearing the pet's
 *     visual helmet (skull or block). PetManager's follow task keeps it
 *     glued to the owner. onTick() fires every 5 ticks while summoned —
 *     use this for passive buffs. onActivate() fires on sneak+right-click
 *     of the egg — use it for the active ability (add your own cooldown).
 *
 * Implementations go in com.soulenchants.pets.impl. Each implementation
 * declares its visual, flavor lore, and the proc hooks it wants to run.
 */
public abstract class Pet {

    protected final SoulEnchants plugin;

    public Pet(SoulEnchants plugin) { this.plugin = plugin; }

    /** Stable id for registry + NBT. */
    public abstract String getId();

    /** Coloured display name shown on the egg + above the spawned companion. */
    public abstract String getDisplayName();

    /** Short theme tag for the picker UI ("OFFENSIVE", "DEFENSE", "UTILITY", "HYBRID"). */
    public abstract String getArchetype();

    /** One-line rarity badge colour (§ code prefix) for the egg's name. */
    public abstract String getRarityColor();

    /**
     * Visual helmet stack the spawned ArmorStand wears. Most pets use a
     * custom-textured skull via SkullUtil.skull(base64); a few use a
     * vanilla block (ice, magma, pumpkin) for material vibe.
     */
    public abstract ItemStack buildCompanionHelmet();

    /**
     * Icon for the egg item in inventory + menus. Can reuse
     * buildCompanionHelmet() for skull-based pets; block-based pets
     * usually return a dedicated skull instead.
     */
    public abstract ItemStack buildEggIcon();

    /** Flavor lore shown on the egg under the stat block. */
    public abstract List<String> getFlavorLore();

    /** Human-readable description of the always-on passive buff. */
    public abstract String getPassiveDescription();

    /** Human-readable description of the sneak+right-click active. */
    public abstract String getActiveDescription();

    // ──────────────── Hooks ────────────────

    /**
     * Called every 5 ticks while this pet is summoned for {@code owner}.
     * Apply passive buffs here. Keep the work cheap — this runs for every
     * summoned companion on every tick interval.
     */
    public void onTick(Player owner, ArmorStand companion, int level) { }

    /**
     * Fires when the owner does sneak + right-click on the egg. Return
     * true if the ability fired (triggers the default proc vfx + message),
     * false if it was blocked (cooldown, missing resource, etc.). Handle
     * your own cooldown tracking — the manager doesn't do it for you.
     */
    public boolean onActivate(Player owner, ArmorStand companion, int level) { return false; }

    /** Optional hook: the owner landed a killing blow on a mob. */
    public void onOwnerKill(Player owner, ArmorStand companion, org.bukkit.entity.LivingEntity victim, int level) { }

    /** Optional hook: the owner just took damage from any source. */
    public void onOwnerHurt(Player owner, ArmorStand companion, double damage, int level) { }

    // ──────────────── Leveling ────────────────

    /** Total XP required to reach {@code level} from level 1. */
    public static long xpForLevel(int level) {
        if (level <= 1) return 0;
        return 100L * (level - 1) * (level - 1);
    }

    /** Level cap. Each pet gets 50 levels of progression. */
    public static final int MAX_LEVEL = 50;
}
