package com.soulenchants.enchants;

import com.soulenchants.enchants.impl.AutoSmeltEnchant;
import com.soulenchants.enchants.impl.BerserkEnchant;
import com.soulenchants.enchants.impl.LifestealEnchant;
import com.soulenchants.enchants.impl.TelepathyEnchant;

import java.util.*;

public class EnchantRegistry {

    private static final Map<String, CustomEnchant> ENCHANTS = new LinkedHashMap<>();

    public static void registerDefaults() {
        // ── SWORD ─────────────────────────────────────────────────────────
        register(new LifestealEnchant());
        register(new CustomEnchant("bleed", "Bleed", EnchantTier.UNCOMMON, EnchantSlot.SWORD, 3,
                "Damage over time on hit"));
        register(new CustomEnchant("deepwounds", "Deepwounds", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 3,
                "Apply Wither (anti-regen) on hit"));
        register(new CustomEnchant("cripple", "Cripple", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Slowness + Weakness on hit (5s cooldown)"));
        register(new CustomEnchant("venom", "Venom", EnchantTier.UNCOMMON, EnchantSlot.SWORD, 3,
                "Poison on hit"));
        register(new CustomEnchant("vampire", "Vampire", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 3,
                "Killed mobs drop bonus XP"));
        register(new CustomEnchant("slayer", "Slayer", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Bonus damage to bosses and their minions"));
        register(new CustomEnchant("holysmite", "Holy Smite", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Bonus damage to undead mobs"));
        register(new CustomEnchant("witherbane", "Wither Bane", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Extra damage to wither variants"));
        register(new CustomEnchant("cleave", "Cleave", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Hits splash nearby mobs for a % of the damage"));
        register(new CustomEnchant("executioner", "Executioner", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 3,
                "Bonus damage to enemies below 30% HP"));
        register(new CustomEnchant("reaper", "Reaper", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Heal a chunk of HP on kill"));
        register(new CustomEnchant("soulreaper", "Soul Reaper", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 3,
                "Bonus Souls from boss kills"));
        register(new CustomEnchant("demonslayer", "Demon Slayer", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Bonus damage to nether mobs"));
        register(new CustomEnchant("beastslayer", "Beast Slayer", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Bonus damage to arthropods"));
        register(new CustomEnchant("frostaspect", "Frost Aspect", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Slowness + Mining Fatigue on hit"));
        register(new CustomEnchant("bloodlust", "Bloodlust", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 3,
                "Stacking damage bonus per kill (5 stacks max, 10s decay)"));
        register(new CustomEnchant("cursededge", "Cursed Edge", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Chance to apply Wither II on hit"));
        register(new CustomEnchant("soulburn", "Soul Burn", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Set targets on fire on hit"));
        register(new CustomEnchant("phantomstrike", "Phantom Strike", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 3,
                "Chance to teleport behind your target on hit"));
        register(new CustomEnchant("earthshaker", "Earthshaker", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Small AoE damage shockwave on hit"));
        register(new CustomEnchant("bonebreaker", "Bonebreaker", EnchantTier.UNCOMMON, EnchantSlot.SWORD, 3,
                "Apply Weakness on hit"));
        register(new CustomEnchant("criticalstrike", "Critical Strike", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Chance for a guaranteed critical hit (+50%)"));
        register(new CustomEnchant("headhunter", "Headhunter", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Chance for mob heads to drop on kill"));
        register(new CustomEnchant("razorwind", "Razor Wind", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Cone slash damages enemies in front on hit"));
        register(new CustomEnchant("greedy", "Greedy", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Bonus mob loot drops"));

        // ── HELMET ────────────────────────────────────────────────────────
        register(new CustomEnchant("drunk", "Drunk", EnchantTier.EPIC, EnchantSlot.HELMET, 4,
                "Strength + Slowness + Mining Fatigue while worn (Strength capped at III)"));
        register(new CustomEnchant("nightvision", "Nightvision", EnchantTier.COMMON, EnchantSlot.HELMET, 1,
                "Constant Night Vision"));
        register(new CustomEnchant("saturation", "Saturation", EnchantTier.UNCOMMON, EnchantSlot.HELMET, 3,
                "Slows hunger drain significantly"));
        register(new CustomEnchant("aquatic", "Aquatic", EnchantTier.COMMON, EnchantSlot.HELMET, 1,
                "Water breathing"));

        // ── CHESTPLATE ────────────────────────────────────────────────────
        register(new BerserkEnchant()); // chestplate now (updated below via slot override not possible — rely on default ARMOR for now)
        register(new CustomEnchant("phoenix", "Phoenix", EnchantTier.LEGENDARY, EnchantSlot.CHESTPLATE, 1,
                "Once every 2 minutes, survive a lethal hit with full HP"));
        register(new CustomEnchant("overshield", "Overshield", EnchantTier.EPIC, EnchantSlot.CHESTPLATE, 3,
                "Constant Absorption (+2/4/6 shield HP)"));
        register(new CustomEnchant("implants", "Implants", EnchantTier.RARE, EnchantSlot.CHESTPLATE, 3,
                "Constant Regeneration"));
        register(new CustomEnchant("vital", "Vital", EnchantTier.EPIC, EnchantSlot.CHESTPLATE, 3,
                "+2/4/6 max HP"));
        register(new CustomEnchant("laststand", "Last Stand", EnchantTier.LEGENDARY, EnchantSlot.CHESTPLATE, 3,
                "Resistance buff when below 6 HP"));
        register(new CustomEnchant("blessed", "Blessed", EnchantTier.EPIC, EnchantSlot.CHESTPLATE, 3,
                "Reduce damage taken from bosses + Wither"));

        // ── CHESTPLATE OR LEGGINGS ────────────────────────────────────────
        register(new CustomEnchant("armored", "Armored", EnchantTier.RARE, EnchantSlot.CHEST_OR_LEGGINGS, 4,
                "25% chance to reduce damage from sword attackers (10/15/20/25%)"));

        // ── LEGGINGS ──────────────────────────────────────────────────────
        register(new CustomEnchant("hardened", "Hardened", EnchantTier.UNCOMMON, EnchantSlot.LEGGINGS, 3,
                "Reduce melee damage taken"));
        register(new CustomEnchant("antiknockback", "Antiknockback", EnchantTier.EPIC, EnchantSlot.LEGGINGS, 3,
                "Reduce knockback received from attacks"));
        register(new CustomEnchant("endurance", "Endurance", EnchantTier.RARE, EnchantSlot.LEGGINGS, 3,
                "Damage reduction stacks the longer you fight"));
        register(new CustomEnchant("ironclad", "Ironclad", EnchantTier.RARE, EnchantSlot.LEGGINGS, 3,
                "Reduce explosion damage"));

        // ── BOOTS ─────────────────────────────────────────────────────────
        register(new CustomEnchant("speed", "Speed", EnchantTier.RARE, EnchantSlot.BOOTS, 3,
                "Constant Speed I/II/III"));
        register(new CustomEnchant("adrenaline", "Adrenaline", EnchantTier.RARE, EnchantSlot.BOOTS, 3,
                "Speed when below 7 HP"));
        register(new CustomEnchant("depthstrider", "Depth Strider", EnchantTier.COMMON, EnchantSlot.BOOTS, 3,
                "Swim faster in water"));
        register(new CustomEnchant("haste", "Haste", EnchantTier.RARE, EnchantSlot.BOOTS, 3,
                "Mine and attack faster"));
        register(new CustomEnchant("jumpboost", "Jump Boost", EnchantTier.COMMON, EnchantSlot.BOOTS, 3,
                "Jump higher"));
        register(new CustomEnchant("firewalker", "Firewalker", EnchantTier.RARE, EnchantSlot.BOOTS, 1,
                "Fire resistance + walk safely on lava"));
        register(new CustomEnchant("featherweight", "Featherweight", EnchantTier.UNCOMMON, EnchantSlot.BOOTS, 3,
                "Reduce fall damage by 33/66/100%"));

        // ── ANY ARMOR ─────────────────────────────────────────────────────
        register(new CustomEnchant("molten", "Molten", EnchantTier.EPIC, EnchantSlot.ARMOR, 3,
                "Set attackers on fire when hit"));
        register(new CustomEnchant("stormcaller", "Stormcaller", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 3,
                "Lightning strikes attacker on big hits"));
        register(new CustomEnchant("guardians", "Guardians", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 3,
                "Gain Absorption hearts when hit (cooldown)"));
        register(new CustomEnchant("reflect", "Reflect", EnchantTier.EPIC, EnchantSlot.ARMOR, 3,
                "Reflect a percentage of incoming damage"));
        register(new CustomEnchant("vengeance", "Vengeance", EnchantTier.RARE, EnchantSlot.ARMOR, 3,
                "Deal small damage back to attackers"));
        register(new CustomEnchant("lifebloom", "Lifebloom", EnchantTier.RARE, EnchantSlot.ARMOR, 3,
                "Regen when standing still"));
        register(new CustomEnchant("magnetism", "Magnetism", EnchantTier.COMMON, EnchantSlot.ARMOR, 1,
                "Pull nearby dropped items toward you"));
        register(new CustomEnchant("enlightened", "Enlightened", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 3,
                "Chance to convert incoming damage into healing"));

        // ── TOOLS / GRINDING ──────────────────────────────────────────────
        register(new AutoSmeltEnchant());
        register(new TelepathyEnchant());
        register(new CustomEnchant("xpboost", "XP Boost", EnchantTier.COMMON, EnchantSlot.TOOL, 3,
                "Multiply XP from mining and mob kills"));
        register(new CustomEnchant("treefeller", "Treefeller", EnchantTier.COMMON, EnchantSlot.AXE, 1,
                "Chop the entire tree at once"));
        register(new CustomEnchant("explosive", "Explosive", EnchantTier.EPIC, EnchantSlot.PICKAXE, 1,
                "Mine a 3×3 area"));

        // ── SOUL ENCHANTS ─────────────────────────────────────────────────
        register(new CustomEnchant("soulstrike", "Soul Strike", EnchantTier.SOUL_ENCHANT, EnchantSlot.SWORD, 3,
                "15% chance for massive bonus damage. Consumes Souls.", 30));
        register(new CustomEnchant("souldrain", "Soul Drain", EnchantTier.SOUL_ENCHANT, EnchantSlot.SWORD, 3,
                "Heal for damage dealt. Consumes Souls per hit.", 15));
        register(new CustomEnchant("soulshield", "Soul Shield", EnchantTier.SOUL_ENCHANT, EnchantSlot.CHESTPLATE, 1,
                "Prevent a lethal hit (60s cd). Consumes Souls on trigger.", 200));
        register(new CustomEnchant("soulburst", "Soul Burst", EnchantTier.SOUL_ENCHANT, EnchantSlot.ARMOR, 3,
                "AOE knockback when hit. Consumes Souls per trigger.", 50));
    }

    public static void register(CustomEnchant e) {
        ENCHANTS.put(e.getId().toLowerCase(), e);
    }

    public static CustomEnchant get(String id) {
        return id == null ? null : ENCHANTS.get(id.toLowerCase());
    }

    public static Collection<CustomEnchant> all() { return ENCHANTS.values(); }

    public static List<CustomEnchant> sortedForMenu() {
        List<CustomEnchant> list = new ArrayList<>(ENCHANTS.values());
        list.sort((a, b) -> {
            int t = Integer.compare(a.getTier().ordinal(), b.getTier().ordinal());
            if (t != 0) return t;
            return a.getDisplayName().compareTo(b.getDisplayName());
        });
        return list;
    }

    public static CustomEnchant randomWeighted(Random rng) {
        double total = 0;
        for (CustomEnchant e : ENCHANTS.values()) total += e.getTier().getDropWeight();
        double roll = rng.nextDouble() * total;
        double acc = 0;
        for (CustomEnchant e : ENCHANTS.values()) {
            acc += e.getTier().getDropWeight();
            if (roll <= acc) return e;
        }
        return ENCHANTS.values().iterator().next();
    }
}
