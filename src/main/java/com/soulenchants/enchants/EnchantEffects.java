package com.soulenchants.enchants;

/**
 * Concrete effect-at-level descriptions for each enchant. Used in item lore
 * so players can see exactly what an enchant does at the level it's applied
 * — not just the generic flavor text.
 *
 * Numbers must match the actual proc logic in the listeners/items packages.
 * If you tune a value in the gameplay code, update the matching case here.
 */
public final class EnchantEffects {

    private EnchantEffects() {}

    /** Returns the concrete effect line at the given level, or null if no description. */
    public static String describe(String id, int level) {
        if (id == null) return null;
        switch (id.toLowerCase()) {
            // ── SWORD ──
            case "vampire":         return "+" + (level * 50) + "% XP from kills";
            case "deepwounds":      return "Boosts your Bleed proc chance by " + level + " level(s)";
            case "featherweight":   return (level * 20) + "% chance · Haste " + roman(level) + " for " + level + "s";
            case "blessed":         return (level * 20) + "% chance on hit · strip your own debuffs";
            case "slayer":          return "+" + (level * 25) + "% damage + " + (level * 5) + " TRUE damage vs bosses + minions (bypasses armor)";
            case "holysmite":       return "+" + (level * 20) + "% damage to undead";
            case "witherbane":      return "+" + (level * 25) + "% damage to wither variants";
            case "cleave":          return "5% chance · 3 dmg to all enemies in " + level + "-block radius";
            case "executioner":     return "+" + (level * 30) + "% damage to enemies <30% HP";
            case "reaper":          return "Heal " + (level * 15) + "% max HP on kill";
            case "soulreaper":      return "+" + (level * 25) + "% souls from kills";
            case "demonslayer":     return "+" + (level * 20) + "% damage to nether mobs";
            case "beastslayer":     return "+" + (level * 20) + "% damage to arthropods";
            case "frostaspect":     return (level * 5) + "% chance · Slow " + roman(level) + " + Mining Fatigue " + (level * 2) + "s";
            case "bloodlust":       return "Heal 2 HP when an enemy in 7-block radius takes a Bleed tick";
            case "cursededge":      return (level * 3) + "% chance · Wither II for " + (level * 4) + "s";
            case "soulburn":        return (level * 5) + "% chance · ignite for " + (level * 3) + "s + " + level + " bonus dmg";
            case "phantomstrike":   return (level * 2) + "% chance · teleport behind target";
            case "earthshaker":     return "AoE " + (level * 2) + " dmg in " + (level + 1) + "-block radius";
            case "bonebreaker":     return (level * 4) + "% chance · Weakness " + roman(Math.min(2, level)) + " for " + (level * 3) + "s";
            case "criticalstrike":  return (level * 2) + "% chance · +50% crit damage";
            case "headhunter":      return (level * 2) + "% chance · drops a mob head";
            case "bleed":
                if (level >= 4) {
                    int ahPct = (level - 3) * 10;
                    return String.format("%.1f", level * 0.6) + "% chance · stacking Slow + "
                            + ahPct + "% Anti-Heal for 6s (Deep Wounds boosts proc)";
                }
                return String.format("%.1f", level * 0.6) + "% chance · stacking Slow on hit (boosted by Deep Wounds)";
            case "razorwind":       return "Cone slash · " + (level * 3) + " dmg to enemies in front";
            case "greedy":          return (level * 4) + "% chance per drop to duplicate it";

            // ── AXE ──
            case "reaver":          return "+" + (level * 8) + "% damage scaled to your missing HP";
            case "skullcrush":      return (level * 4) + "% chance · Nausea " + (level * 3) + "s + Weakness II " + (level * 3) + "s";
            case "hamstring":       return (level * 3) + "% chance · root victim with Slow IV for 2s";
            case "bloodfury":       return "Below 30% HP · heal " + (level * 25) + "% of damage dealt";
            case "shieldbreaker":   return (level * 5) + "% chance · +25% TRUE damage (ignores armor)";
            case "berserkersedge":  return "Kill grants Strength I for " + (level * 5) + "s (refreshable)";
            case "frostshatter":    return (level * 5) + "% chance · Slow III + Mining Fatigue III for 4s";
            case "wraithcleave":    return "+" + (level * 25) + "% damage if target is alone in 8 blocks";
            case "rendingblow":     return (level * 4) + "% chance · Wither III for 5s";
            case "executionersmark":return "+" + (level * 15) + "% damage to enemies with active debuffs";

            // ── ARMOR (PvP focused) ──
            case "counter":         return (level * 4) + "% chance · disarm attacker (drops weapon · 30s CD)";
            case "aegis":           return "Below 6 HP · Resistance " + roman(level) + " for 4s · 30s CD";
            case "rush":            return (level * 6) + "% chance on hit · Speed II for 3s · 10s CD";
            case "spite":           return "Reflect " + (level * 8) + "% as TRUE damage (ignores armor)";
            case "ironskin":        return (level * 12) + "% chance to negate a critical hit";
            case "vampiricplate":   return (level * 4) + "% chance · steal 2 HP from attacker";
            case "callous":         return (level * 2) + "% chance · take 0 melee damage";

            // ── HELMET ──
            case "drunk":           return "Strength " + roman(Math.min(3, level)) + " + Slow " + roman(Math.min(3, level)) + " + MF " + roman(Math.min(3, level));
            case "nightvision":     return "Permanent Night Vision";
            case "saturation":      return "Hunger drain reduced by " + (level * 30) + "%";
            case "aquatic":         return "Permanent Water Breathing";
            case "clarity":         return level >= 3 ? "Full immunity to Poison + Blindness"
                                                       : (level * 33) + "% chance/tick to shed Poison + Blindness";

            // ── CHESTPLATE ──
            case "berserk":         return "Stacking dmg buff per kill (max +" + (level * 10) + "%)";
            case "phoenix":         return "Lethal save → full heal · 500-8000 random souls · 160s CD";
            case "overshield":      return (level * 0.5) + "% chance per hit · +1 absorb heart · 120s CD";
            case "implants":        return "Regen " + roman(level) + " when below 50% HP";
            case "vital":           return "+" + level + " hearts (" + (level * 2) + " max HP)";
            case "laststand":       return "Resistance " + roman(level) + " when below 4 HP";

            // ── CHEST OR LEGGINGS ──
            case "armored":         return "-" + (level * 5) + "% damage from sword attackers (stacks per piece)";

            // ── LEGGINGS ──
            case "antiknockback":   return "-" + (level * 50) + "% knockback received";
            case "endurance":       return "+" + (level * 1) + "% damage reduction per 5s in combat";
            case "ironclad":        return "-" + (level * 25) + "% explosion damage";

            // ── BOOTS ──
            case "speed":           return "Permanent Speed " + roman(level);
            case "adrenaline":      return "Speed " + roman(level) + " when below 7 HP";
            case "depthstrider":    return "Swim speed +" + (level * 33) + "%";
            case "jumpboost":       return "Permanent Jump Boost " + roman(level);
            case "firewalker":      return "Permanent Fire Resistance · safe on lava";

            // ── PICKAXE ──
            case "haste":           return "Mining gives Haste " + roman(level) + " for 5s";

            // ── ARMOR (any) ──
            case "hardened":        return (level * 20) + "% chance on hit · restore "
                                            + (level + 2) + " durability across all armor";
            case "molten":          return (level * 5) + "% chance · ignite attackers for " + (level * 3) + "s";
            case "stormcaller":     return (level * 5) + "% chance · lightning on hits >6 dmg (CD per hit)";
            case "guardians":       return (level * 1) + "% chance per hit · Absorption · 120s CD";
            case "reflect":         return "Reflect " + (level * 6) + "% incoming damage (5s CD)";
            case "vengeance":       return "Deal " + String.format("%.1f", 0.5 + 0.3 * level) + " damage back to attackers";
            case "lifebloom":       return "On YOUR death · fully heal allies in 20 blocks · 100s CD";
            case "magnetism":       return "Pull dropped items within 4 blocks";
            case "enlightened":     return String.format("%.1f", level * 0.2) + "% chance per piece · absorb hit + small heal";

            // ── TOOLS ──
            case "autosmelt":       return "Mined ores smelt automatically";
            case "telepathy":       return "Drops go straight to your inventory";
            case "xpboost":         return "+" + (level * 25) + "% XP from mining + kills";
            case "treefeller":      return "Chop the entire tree in one swing";
            case "explosive":       return "Mine a 3×3 area per swing";

            // ── SOUL ENCHANTS (high cost — for finishers, not steady DPS) ──
            case "soulstrike":      return "5% chance · +" + ((int)(50 + 50 * level)) + "% dmg · costs 100 souls per proc";
            case "souldrain":       return "Heal +" + (level * 25) + "% of dmg dealt · costs 50 souls per hit";
            case "soulburst":       return "On hit (any) · AoE " + (level * 4) + " dmg + knockback · costs 150 souls per proc";
            case "divineimmolation": return "AoE " + level + "-block radius · " + String.format("%.1f", level * 1.1) + " divine dmg · 5 souls per swing";
            case "natureswrath":    return "2% on-hit · root " + (level * 3) + "-block radius for " + (5 + level) + "s · 75 souls per proc";

            // ── Anti-Healing ──
            case "severance":       return (level * 20) + "% chance · 25% Anti-Heal for 5s (target's healing cut)";
            case "reapingslash":    return (level * 15) + "% chance · 40% Anti-Heal for 6s (target's healing cut)";

            // ── v1.2 AXE debuff suite ──
            case "marrowbreak":     return (level * 25) + "% chance · Weakness II for 5s";
            case "crushingblow":    return (level * 20) + "% chance · Slow III for 3s";
            case "pulverize":       return (level * 15) + "% chance · Nausea III + Slow II for 4s";
            case "exsanguinate":    return (level * 10) + "% chance · 5s true-damage DoT (1 HP/s ignores armor)";
            case "huntersmark":     return "Mark target for 10s · +" + (level * 12) + "% damage vs your mark";
            case "overwhelm":       return "Consecutive hits same target · +" + (level * 6) + "% per stack (max 5)";

            // ── v1.2 PvE armor ──
            case "soulwarden":      return "After mob damage · Regen " + roman(level) + " for 5s · 60s CD";
            case "mobslayersward":  return "-" + (level * 10) + "% damage from custom mobs (bosses, minions, elites)";
            case "radiantshell":    return "-1 flat damage per equipped piece (max -4 with full set)";
            case "dreadmantle":     return "When hit · mobs in 8 blocks get Weakness " + roman(level) + " for 3s";

            // ── v1.3 god-tier armor fillers ──
            case "thornback":       return "Attackers take " + (level * 5) + "% of their damage as TRUE damage (stacks per piece)";
            case "wardenseye":      return "Reveal invisible entities in 12 blocks · attackers briefly Glow";
            case "bulwark":         return "-" + (level * 6) + "% damage from mobs · Resistance II below 40% HP";
            case "voidwalker":      return (level * 4) + "% chance to dodge any hit · permanent Speed I";
            case "oathbound":       return "On hit · cleanse Slow/Weakness/Wither on self · 30s CD";
            case "entombed":        return "Below 30% HP on hit · Slow IV + Mining Fatigue III to attackers in 8 blocks for 4s · 60s CD";

            case "rage":            return "+(" + level + " × stacks × 2) bonus damage per hit · max 10 stacks · 30s decay · resets on damage taken";
            case "obsidianshield":  return "Permanent Fire Resistance while worn";
        }
        return null;
    }

    private static String roman(int n) { return CustomEnchant.roman(n); }
}
