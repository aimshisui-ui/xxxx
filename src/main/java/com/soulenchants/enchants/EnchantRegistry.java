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
        register(new LifestealEnchant());                                                                     // L5 — set in impl
        register(new CustomEnchant("bleed", "Bleed", EnchantTier.EPIC, EnchantSlot.AXE, 6,
                "Stacking Slow on hit. Stacks reset after 30s of no proc."));
        register(new CustomEnchant("deepwounds", "Deep Wounds", EnchantTier.LEGENDARY, EnchantSlot.WEAPON, 3,
                "Boosts the proc chance of your Bleed enchant"));
        register(new CustomEnchant("cripple", "Cripple", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Slowness + Weakness on hit (5s cooldown)"));
        register(new CustomEnchant("venom", "Venom", EnchantTier.UNCOMMON, EnchantSlot.SWORD, 5,
                "Poison on hit"));
        register(new CustomEnchant("vampire", "Vampire", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 2,
                "Killed mobs drop bonus XP"));
        register(new CustomEnchant("slayer", "Slayer", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Bonus damage to bosses and their minions"));
        register(new CustomEnchant("holysmite", "Holy Smite", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Bonus damage to undead mobs"));
        register(new CustomEnchant("witherbane", "Wither Bane", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Extra damage to wither variants"));
        register(new CustomEnchant("cleave", "Cleave", EnchantTier.EPIC, EnchantSlot.AXE, 7,
                "5% chance to deal 3 dmg to all enemies in (level)-block radius"));
        register(new CustomEnchant("executioner", "Executioner", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 3,
                "Bonus damage to enemies below 30% HP"));
        register(new CustomEnchant("reaper", "Reaper", EnchantTier.RARE, EnchantSlot.SWORD, 2,
                "Heal a chunk of HP on kill"));
        register(new CustomEnchant("soulreaper", "Soul Reaper", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 5,
                "Bonus Souls from boss + mob kills"));
        register(new CustomEnchant("demonslayer", "Demon Slayer", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Bonus damage to nether mobs"));
        register(new CustomEnchant("beastslayer", "Beast Slayer", EnchantTier.RARE, EnchantSlot.SWORD, 3,
                "Bonus damage to arthropods"));
        register(new CustomEnchant("frostaspect", "Frost Aspect", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Slowness + Mining Fatigue on hit"));
        register(new CustomEnchant("bloodlust", "Blood Lust", EnchantTier.LEGENDARY, EnchantSlot.CHESTPLATE, 6,
                "Heal 2 HP whenever an enemy near you takes Bleed damage"));
        register(new CustomEnchant("cursededge", "Cursed Edge", EnchantTier.EPIC, EnchantSlot.SWORD, 2,
                "Chance to apply Wither II on hit"));
        register(new CustomEnchant("soulburn", "Soul Burn", EnchantTier.RARE, EnchantSlot.SWORD, 5,
                "Set targets on fire on hit"));
        register(new CustomEnchant("phantomstrike", "Phantom Strike", EnchantTier.LEGENDARY, EnchantSlot.SWORD, 2,
                "Chance to teleport behind your target on hit"));
        register(new CustomEnchant("earthshaker", "Earthshaker", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Small AoE damage shockwave on hit"));
        register(new CustomEnchant("bonebreaker", "Bonebreaker", EnchantTier.UNCOMMON, EnchantSlot.SWORD, 5,
                "Apply Weakness on hit"));
        register(new CustomEnchant("criticalstrike", "Critical Strike", EnchantTier.RARE, EnchantSlot.SWORD, 5,
                "Chance for a guaranteed critical hit (+50%)"));
        register(new CustomEnchant("headhunter", "Headhunter", EnchantTier.EPIC, EnchantSlot.SWORD, 2,
                "Chance for mob heads to drop on kill"));
        register(new CustomEnchant("razorwind", "Razor Wind", EnchantTier.EPIC, EnchantSlot.SWORD, 3,
                "Cone slash damages enemies in front on hit"));
        register(new CustomEnchant("greedy", "Greedy", EnchantTier.RARE, EnchantSlot.SWORD, 2,
                "Bonus mob loot drops"));

        // ── AXE (10 new) ─────────────────────────────────────────────────
        register(new CustomEnchant("reaver", "Reaver", EnchantTier.EPIC, EnchantSlot.AXE, 4,
                "Bonus damage based on attacker's missing HP"));
        register(new CustomEnchant("skullcrush", "Skullcrush", EnchantTier.RARE, EnchantSlot.AXE, 3,
                "Chance to apply Nausea + Weakness II for a few seconds"));
        register(new CustomEnchant("hamstring", "Hamstring", EnchantTier.LEGENDARY, EnchantSlot.AXE, 3,
                "Chance to root victim with Slow IV for 2s"));
        register(new CustomEnchant("bloodfury", "Blood Fury", EnchantTier.LEGENDARY, EnchantSlot.AXE, 3,
                "Heal 25% of damage dealt while below 30% HP"));
        register(new CustomEnchant("shieldbreaker", "Shieldbreaker", EnchantTier.EPIC, EnchantSlot.AXE, 3,
                "Chance to deal +25% TRUE damage (ignores armor)"));
        register(new CustomEnchant("berserkersedge", "Berserker's Edge", EnchantTier.EPIC, EnchantSlot.AXE, 3,
                "Gain Strength I for 5s on kill (5s refreshable)"));
        register(new CustomEnchant("frostshatter", "Frostshatter", EnchantTier.EPIC, EnchantSlot.AXE, 3,
                "Chance to apply Slow III + Mining Fatigue III for 4s"));
        register(new CustomEnchant("wraithcleave", "Wraithcleave", EnchantTier.LEGENDARY, EnchantSlot.AXE, 3,
                "Bonus damage if target has no allies in 8 blocks"));
        register(new CustomEnchant("rendingblow", "Rending Blow", EnchantTier.EPIC, EnchantSlot.AXE, 3,
                "Chance to apply Wither III for 5s"));
        register(new CustomEnchant("executionersmark", "Executioner's Mark", EnchantTier.LEGENDARY, EnchantSlot.AXE, 3,
                "Bonus damage to enemies with active negative effects"));

        // ── AXE (v1.2 — debuff-heavy PvE focus) ──────────────────────────
        register(new CustomEnchant("marrowbreak", "Marrowbreak", EnchantTier.RARE, EnchantSlot.AXE, 3,
                "25%/lvl chance to apply Weakness II for 5s"));
        register(new CustomEnchant("crushingblow", "Crushing Blow", EnchantTier.EPIC, EnchantSlot.AXE, 3,
                "20%/lvl chance to apply Slow III for 3s"));
        register(new CustomEnchant("pulverize", "Pulverize", EnchantTier.EPIC, EnchantSlot.AXE, 3,
                "15%/lvl chance to apply Nausea III + Slow II for 4s"));
        register(new CustomEnchant("exsanguinate", "Exsanguinate", EnchantTier.LEGENDARY, EnchantSlot.AXE, 3,
                "10%/lvl — 5s true-damage DoT (1 HP/s, ignores armor)"));
        register(new CustomEnchant("huntersmark", "Hunter's Mark", EnchantTier.LEGENDARY, EnchantSlot.AXE, 3,
                "Mark target for 10s — deal +12%/lvl damage vs the mark"));
        register(new CustomEnchant("overwhelm", "Overwhelm", EnchantTier.EPIC, EnchantSlot.AXE, 3,
                "Consecutive hits on same target stack +6%/lvl dmg (max 5 stacks, resets after 3s)"));

        // ── Anti-Healing suite ──
        register(new CustomEnchant("severance",    "Severance",     EnchantTier.LEGENDARY, EnchantSlot.SWORD, 3,
                "20%/lvl chance · apply 25% Anti-Heal for 5s (reduces target's healing)"));
        register(new CustomEnchant("reapingslash", "Reaping Slash", EnchantTier.LEGENDARY, EnchantSlot.AXE,   3,
                "15%/lvl chance · apply 40% Anti-Heal for 6s (reduces target's healing)"));

        // ── ARMOR (7 new — PvP focused) ───────────────────────────────────
        register(new CustomEnchant("counter", "Counter", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 3,
                "Chance to disarm attacker (forces them to drop their weapon)"));
        register(new CustomEnchant("aegis", "Aegis", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 3,
                "Resistance II for 4s when below 6 HP — 30s CD"));
        register(new CustomEnchant("rush", "Rush", EnchantTier.EPIC, EnchantSlot.ARMOR, 3,
                "Speed II burst when hit — 10s CD"));
        register(new CustomEnchant("spite", "Spite", EnchantTier.EPIC, EnchantSlot.ARMOR, 3,
                "Reflect 8%/lvl damage as TRUE damage (ignores armor)"));
        register(new CustomEnchant("ironskin", "Iron Skin", EnchantTier.RARE, EnchantSlot.ARMOR, 3,
                "Chance to negate critical hits"));
        register(new CustomEnchant("vampiricplate", "Vampiric Plate", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 3,
                "Chance to steal 2 HP from attacker on hit"));
        register(new CustomEnchant("callous", "Callous", EnchantTier.RARE, EnchantSlot.ARMOR, 3,
                "Small chance to take 0 damage from melee hits"));

        // ── ARMOR (v1.2 — PvE focus) ──────────────────────────────────────
        register(new CustomEnchant("soulwarden", "Soul Warden", EnchantTier.EPIC, EnchantSlot.CHESTPLATE, 3,
                "Regeneration I/II/III for 5s after a mob damages you (60s CD). PvE only."));
        register(new CustomEnchant("mobslayersward", "Mobslayer's Ward", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 3,
                "-10%/lvl damage from custom mobs (bosses, minions, elites)"));
        register(new CustomEnchant("radiantshell", "Radiant Shell", EnchantTier.RARE, EnchantSlot.ARMOR, 4,
                "-1 flat damage per equipped piece with this enchant (max -4)"));
        register(new CustomEnchant("dreadmantle", "Dreadmantle", EnchantTier.EPIC, EnchantSlot.HELMET, 3,
                "When hit, mobs within 8 blocks get Weakness I-III for 3s"));

        // ── v1.3 god-tier armor fillers — pushes aquatic/nightvision/depthstrider
        //    out of bossset slots. Every one of these is slot-competitive.
        register(new CustomEnchant("thornback",  "Thornback",   EnchantTier.EPIC,      EnchantSlot.ARMOR,      3,
                "Reflect 5%/lvl of incoming damage as TRUE damage (stacks per piece)"));
        register(new CustomEnchant("wardenseye", "Warden's Eye",EnchantTier.EPIC,      EnchantSlot.HELMET,     3,
                "Mark attackers with a particle ring — reveals position to you"));
        register(new CustomEnchant("bulwark",    "Bulwark",     EnchantTier.LEGENDARY, EnchantSlot.CHESTPLATE, 3,
                "-6%/lvl damage from custom mobs · Resistance II below 40% HP"));
        register(new CustomEnchant("voidwalker", "Voidwalker",  EnchantTier.LEGENDARY, EnchantSlot.BOOTS,      3,
                "8%/lvl chance to dodge any hit · grants permanent Speed I"));
        register(new CustomEnchant("oathbound",  "Oathbound",   EnchantTier.EPIC,      EnchantSlot.HELMET,     3,
                "On hit · cleanse Slow / Weakness / Wither from self (30s CD)"));
        register(new CustomEnchant("entombed",   "Entombed",    EnchantTier.EPIC,      EnchantSlot.LEGGINGS,   3,
                "Below 30% HP on hit · Slow IV + Mining Fatigue III to nearby attackers · 60s CD"));

        // ── HELMET ────────────────────────────────────────────────────────
        register(new CustomEnchant("drunk", "Drunk", EnchantTier.EPIC, EnchantSlot.HELMET, 4,
                "Strength + Slowness + Mining Fatigue while worn (Strength capped at III)"));
        register(new CustomEnchant("nightvision", "Nightvision", EnchantTier.COMMON, EnchantSlot.HELMET, 1,
                "Constant Night Vision"));
        register(new CustomEnchant("saturation", "Saturation", EnchantTier.UNCOMMON, EnchantSlot.HELMET, 3,
                "Slows hunger drain significantly"));
        register(new CustomEnchant("aquatic", "Aquatic", EnchantTier.COMMON, EnchantSlot.HELMET, 1,
                "Water breathing"));
        register(new CustomEnchant("clarity", "Clarity", EnchantTier.EPIC, EnchantSlot.HELMET, 3,
                "Resist poison + blindness — full immunity at III"));

        // ── CHESTPLATE ────────────────────────────────────────────────────
        register(new BerserkEnchant()); // chestplate now (updated below via slot override not possible — rely on default ARMOR for now)
        register(new CustomEnchant("phoenix", "Phoenix", EnchantTier.SOUL_ENCHANT, EnchantSlot.BOOTS, 3,
                "Survive a lethal hit + heal to full. Burns 500-8000 random souls. 160s CD."));
        register(new CustomEnchant("overshield", "Overshield", EnchantTier.EPIC, EnchantSlot.CHESTPLATE, 4,
                "Rare chance (per level) to gain 1 absorption heart on hit — 120s CD"));
        register(new CustomEnchant("implants", "Implants", EnchantTier.RARE, EnchantSlot.CHESTPLATE, 3,
                "Regeneration when below 50% HP"));
        register(new CustomEnchant("vital", "Vital", EnchantTier.EPIC, EnchantSlot.CHESTPLATE, 6,
                "+1 heart per level (+6 hearts = 12 HP at VI; your max becomes 32 HP)"));
        register(new CustomEnchant("laststand", "Last Stand", EnchantTier.LEGENDARY, EnchantSlot.CHESTPLATE, 3,
                "Resistance buff when below 4 HP"));
        register(new CustomEnchant("blessed", "Blessed", EnchantTier.EPIC, EnchantSlot.WEAPON, 4,
                "20%/lvl chance on hit to strip your own debuffs"));

        // ── CHESTPLATE OR LEGGINGS ────────────────────────────────────────
        register(new CustomEnchant("armored", "Armored", EnchantTier.RARE, EnchantSlot.CHEST_OR_LEGGINGS, 4,
                "Reduce damage from sword attackers. Stacks: more pieces = stronger."));

        // ── LEGGINGS ──────────────────────────────────────────────────────
        register(new CustomEnchant("hardened", "Hardened", EnchantTier.EPIC, EnchantSlot.ARMOR, 3,
                "20%/lvl chance on hit to RESTORE durability across all your armor"));
        register(new CustomEnchant("antiknockback", "Antiknockback", EnchantTier.EPIC, EnchantSlot.LEGGINGS, 2,
                "Reduce knockback received from attacks"));
        register(new CustomEnchant("endurance", "Endurance", EnchantTier.RARE, EnchantSlot.LEGGINGS, 2,
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
        register(new CustomEnchant("haste", "Haste", EnchantTier.RARE, EnchantSlot.PICKAXE, 3,
                "Mining a block grants Haste(level) for 5s"));
        register(new CustomEnchant("jumpboost", "Jump Boost", EnchantTier.COMMON, EnchantSlot.BOOTS, 3,
                "Jump higher"));
        register(new CustomEnchant("firewalker", "Firewalker", EnchantTier.RARE, EnchantSlot.BOOTS, 1,
                "Fire resistance + walk safely on lava"));
        register(new CustomEnchant("featherweight", "Feather Weight", EnchantTier.UNCOMMON, EnchantSlot.SWORD, 3,
                "20%/lvl chance on hit to gain a Haste burst"));

        // ── ANY ARMOR ─────────────────────────────────────────────────────
        register(new CustomEnchant("molten", "Molten", EnchantTier.EPIC, EnchantSlot.ARMOR, 2,
                "Set attackers on fire when hit"));
        register(new CustomEnchant("stormcaller", "Stormcaller", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 2,
                "Lightning strikes attacker on big hits"));
        register(new CustomEnchant("guardians", "Guardians", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 3,
                "Small chance to gain Absorption when hit (120s CD)"));
        register(new CustomEnchant("reflect", "Reflect", EnchantTier.EPIC, EnchantSlot.ARMOR, 2,
                "Reflect a percentage of incoming damage"));
        register(new CustomEnchant("vengeance", "Vengeance", EnchantTier.RARE, EnchantSlot.ARMOR, 2,
                "Deal small damage back to attackers"));
        register(new CustomEnchant("lifebloom", "Lifebloom", EnchantTier.LEGENDARY, EnchantSlot.LEGGINGS, 5,
                "On YOUR death, fully heal all nearby allies in 20 blocks. 100s CD."));
        register(new CustomEnchant("magnetism", "Magnetism", EnchantTier.COMMON, EnchantSlot.ARMOR, 1,
                "Pull nearby dropped items toward you"));
        register(new CustomEnchant("enlightened", "Enlightened", EnchantTier.LEGENDARY, EnchantSlot.ARMOR, 3,
                "Tiny chance to convert incoming damage into a small heal. Stacks."));

        // ── TOOLS / GRINDING ──────────────────────────────────────────────
        register(new AutoSmeltEnchant());
        register(new TelepathyEnchant());
        register(new CustomEnchant("xpboost", "XP Boost", EnchantTier.COMMON, EnchantSlot.TOOL, 5,
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
        register(new CustomEnchant("divineimmolation", "Divine Immolation", EnchantTier.SOUL_ENCHANT, EnchantSlot.SWORD, 4,
                "Hits become AOE — divine fire damages all enemies near your target.", 5));
        register(new CustomEnchant("natureswrath", "Nature's Wrath", EnchantTier.SOUL_ENCHANT, EnchantSlot.ARMOR, 4,
                "On-hit proc — root all enemies in a wide radius and call lightning.", 75));
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
