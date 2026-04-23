package com.soulenchants.config;

/**
 * All enchant balance knobs live here. Field defaults mirror the hardcoded
 * values that used to live inline in CombatListener — so a server with no
 * enchants.yml on disk runs identically to v1.0.
 *
 * Naming convention:
 *   &lt;enchant-id&gt;.&lt;parameter&gt;
 *   Proc chances are in [0..1] per level unless a fixed total (e.g. cleave).
 *   Cooldowns are milliseconds.
 *   Durations using ticks are explicit suffix _ticks; seconds use _seconds.
 *   Radii are blocks.
 */
public final class EnchantConfig {

    // ──────────────── Global ────────────────
    @ConfigValue("global.offensive-bonus-cap")
    public double offensiveBonusCap = 2.00;

    // ──────────────── Lifesteal ────────────────
    @ConfigValue("lifesteal.heal-per-level-pct")
    public double lifestealHealPerLevelPct = 0.05;
    @ConfigValue("lifesteal.heal-cap-hp")
    public double lifestealHealCapHp = 5.0;

    // ──────────────── Bleed ────────────────
    @ConfigValue("bleed.proc-chance-per-level")
    public double bleedProcPerLevel = 0.015;
    @ConfigValue("bleed.deep-wounds-bonus-per-level")
    public double bleedDeepWoundsBonus = 0.008;
    @ConfigValue("bleed.lockout-grace-ms")
    public long bleedLockoutGraceMs = 2_000L;

    // ──────────────── Cripple ────────────────
    @ConfigValue("cripple.proc-per-level")
    public double crippleProc = 0.03;
    @ConfigValue("cripple.cd-ms")
    public long crippleCdMs = 5_000L;
    @ConfigValue("cripple.slow-ticks")
    public int crippleSlowTicks = 60;

    // ──────────────── Venom ────────────────
    @ConfigValue("venom.proc-per-level")
    public double venomProc = 0.04;

    // ──────────────── Cleave ────────────────
    @ConfigValue("cleave.proc-flat")
    public double cleaveProc = 0.05;
    @ConfigValue("cleave.splash-damage")
    public double cleaveDmg = 3.0;

    // ──────────────── Frost Aspect ────────────────
    @ConfigValue("frost-aspect.proc-per-level")
    public double frostProc = 0.05;

    // ──────────────── Cursed Edge ────────────────
    @ConfigValue("cursed-edge.proc-per-level")
    public double cursedEdgeProc = 0.03;

    // ──────────────── Soul Burn ────────────────
    @ConfigValue("soul-burn.proc-per-level")
    public double soulBurnProc = 0.05;
    @ConfigValue("soul-burn.fire-ticks-per-level")
    public int soulBurnFireTicks = 60;
    @ConfigValue("soul-burn.flat-add-per-level")
    public double soulBurnFlatAdd = 1.0;

    // ──────────────── Phantom Strike ────────────────
    @ConfigValue("phantom-strike.proc-per-level")
    public double phantomStrikeProc = 0.02;

    // ──────────────── Earthshaker ────────────────
    @ConfigValue("earthshaker.flat-base")
    public double earthshakerFlatBase = 1.0;
    @ConfigValue("earthshaker.flat-per-level")
    public double earthshakerFlatPerLevel = 1.0;
    @ConfigValue("earthshaker.radius")
    public double earthshakerRadius = 2.0;

    // ──────────────── Bonebreaker ────────────────
    @ConfigValue("bonebreaker.proc-per-level")
    public double bonebreakerProc = 0.04;

    // ──────────────── Critical Strike ────────────────
    @ConfigValue("critical-strike.proc-per-level")
    public double critProc = 0.02;
    @ConfigValue("critical-strike.bonus")
    public double critBonus = 0.50;

    // ──────────────── Razor Wind ────────────────
    @ConfigValue("razor-wind.flat-per-level")
    public double razorWindPerLevel = 1.5;

    // ──────────────── Feather Weight ────────────────
    @ConfigValue("featherweight.proc-per-level")
    public double featherProc = 0.20;

    // ──────────────── Blessed ────────────────
    @ConfigValue("blessed.proc-per-level")
    public double blessedProc = 0.20;
    @ConfigValue("blessed.msg-cd-ms")
    public long blessedMsgCdMs = 6_000L;

    // ──────────────── Type slayers (additive) ────────────────
    @ConfigValue("witherbane.bonus-per-level")
    public double witherbaneBonus = 0.35;
    @ConfigValue("demonslayer.bonus-per-level")
    public double demonSlayerBonus = 0.30;
    @ConfigValue("beastslayer.bonus-per-level")
    public double beastSlayerBonus = 0.25;
    @ConfigValue("slayer.bonus-per-level")
    public double slayerBonus = 0.25;
    @ConfigValue("holysmite.bonus-per-level")
    public double holySmiteBonus = 0.30;

    // ──────────────── Executioner / Reaver / Executioner's Mark ────────────────
    @ConfigValue("executioner.bonus-per-level")
    public double executionerBonus = 0.25;
    @ConfigValue("executioner.hp-threshold-pct")
    public double executionerHpThreshold = 0.30;
    @ConfigValue("reaver.bonus-per-level")
    public double reaverBonus = 0.08;
    @ConfigValue("executioners-mark.bonus-per-level")
    public double executionersMarkBonus = 0.15;

    // ──────────────── Skullcrush / Hamstring / Blood Fury ────────────────
    @ConfigValue("skullcrush.proc-per-level")
    public double skullcrushProc = 0.04;
    @ConfigValue("hamstring.proc-per-level")
    public double hamstringProc = 0.03;
    @ConfigValue("blood-fury.heal-per-level-pct")
    public double bloodFuryHealPct = 0.25;
    @ConfigValue("blood-fury.heal-cap-hp")
    public double bloodFuryHealCapHp = 5.0;
    @ConfigValue("blood-fury.hp-threshold-pct")
    public double bloodFuryHpThreshold = 0.30;

    // ──────────────── Shieldbreaker / Frostshatter / Rending Blow ────────────────
    @ConfigValue("shieldbreaker.proc-per-level")
    public double shieldbreakerProc = 0.05;
    @ConfigValue("shieldbreaker.true-dmg-pct")
    public double shieldbreakerTrueDmgPct = 0.25;
    @ConfigValue("frostshatter.proc-per-level")
    public double frostshatterProc = 0.05;
    @ConfigValue("rending-blow.proc-per-level")
    public double rendingBlowProc = 0.04;
    @ConfigValue("wraithcleave.bonus-per-level")
    public double wraithcleaveBonus = 0.25;
    @ConfigValue("wraithcleave.alone-check-radius")
    public double wraithcleaveRadius = 8.0;

    // ──────────────── Soul-cost enchants ────────────────
    @ConfigValue("soul-strike.proc-chance")
    public double soulStrikeProc = 0.05;
    @ConfigValue("soul-strike.soul-cost")
    public int soulStrikeCost = 100;
    @ConfigValue("soul-strike.mult-base")
    public double soulStrikeMultBase = 1.5;
    @ConfigValue("soul-strike.mult-per-level")
    public double soulStrikeMultPerLevel = 0.5;

    @ConfigValue("soul-drain.heal-per-level-pct")
    public double soulDrainHealPct = 0.25;
    @ConfigValue("soul-drain.soul-cost")
    public int soulDrainCost = 50;

    @ConfigValue("divine-immolation.soul-cost")
    public int divineImmolationCost = 5;
    @ConfigValue("divine-immolation.splash-per-level")
    public double divineImmolationSplashPerLevel = 1.1;

    @ConfigValue("soul-burst.soul-cost")
    public int soulBurstCost = 150;
    @ConfigValue("soul-burst.knockback-radius")
    public double soulBurstRadius = 4.0;
    @ConfigValue("soul-burst.damage-per-level")
    public double soulBurstDmgPerLevel = 4.0;

    @ConfigValue("natures-wrath.proc-flat")
    public double naturesWrathProc = 0.02;
    @ConfigValue("natures-wrath.cd-ms")
    public long naturesWrathCdMs = 10_000L;
    @ConfigValue("natures-wrath.soul-cost")
    public int naturesWrathCost = 75;
    @ConfigValue("natures-wrath.radius-per-level")
    public double naturesWrathRadiusPerLevel = 3.0;
    @ConfigValue("natures-wrath.duration-seconds-base")
    public int naturesWrathDurationSecondsBase = 5;

    // ──────────────── Armor — stackable ────────────────
    @ConfigValue("armored.proc-per-stack-level")
    public double armoredProcPerLevel = 0.02;
    @ConfigValue("armored.proc-cap")
    public double armoredProcCap = 0.16;
    @ConfigValue("armored.reduction-per-stack-level")
    public double armoredReductionPerLevel = 0.01;
    @ConfigValue("armored.reduction-cap")
    public double armoredReductionCap = 0.08;
    @ConfigValue("enlightened.proc-per-stack-level")
    public double enlightenedProcPerLevel = 0.002;
    @ConfigValue("enlightened.proc-cap")
    public double enlightenedProcCap = 0.02;

    // ──────────────── Armor — single ────────────────
    @ConfigValue("hardened.proc-per-level")
    public double hardenedProc = 0.20;
    @ConfigValue("antiknockback.reduction-per-level")
    public double antiknockbackReduction = 0.25;
    @ConfigValue("molten.proc-per-level")
    public double moltenProc = 0.05;
    @ConfigValue("molten.fire-ticks-per-level")
    public int moltenFireTicks = 60;
    @ConfigValue("stormcaller.proc-per-level")
    public double stormcallerProc = 0.05;
    @ConfigValue("stormcaller.min-damage-trigger")
    public double stormcallerMinDmg = 6.0;
    @ConfigValue("stormcaller.cd-base-ms")
    public long stormcallerCdBaseMs = 14_000L;
    @ConfigValue("stormcaller.cd-reduce-per-level-ms")
    public long stormcallerCdReduceMs = 2_000L;
    @ConfigValue("guardians.proc-per-level")
    public double guardiansProc = 0.01;
    @ConfigValue("guardians.cd-ms")
    public long guardiansCdMs = 120_000L;
    @ConfigValue("overshield.proc-per-level")
    public double overshieldProc = 0.005;
    @ConfigValue("overshield.cd-ms")
    public long overshieldCdMs = 120_000L;
    @ConfigValue("reflect.pct-per-level")
    public double reflectPctPerLevel = 0.06;
    @ConfigValue("reflect.cd-ms")
    public long reflectCdMs = 5_000L;
    @ConfigValue("endurance.reduction-per-level")
    public double enduranceReductionPerLevel = 0.01;
    @ConfigValue("endurance.reduction-cap")
    public double enduranceReductionCap = 0.06;
    @ConfigValue("endurance.combat-window-ms")
    public long enduranceCombatWindowMs = 10_000L;
    @ConfigValue("vengeance.base")
    public double vengeanceBase = 0.5;
    @ConfigValue("vengeance.per-level")
    public double vengeancePerLevel = 0.3;
    @ConfigValue("last-stand.hp-threshold")
    public double lastStandHpThreshold = 4.0;

    // ──────────────── PvP armor ────────────────
    @ConfigValue("callous.proc-per-level")
    public double callousProc = 0.02;
    @ConfigValue("ironskin.proc-per-level")
    public double ironskinProc = 0.12;
    @ConfigValue("aegis.hp-threshold")
    public double aegisHpThreshold = 6.0;
    @ConfigValue("aegis.cd-ms")
    public long aegisCdMs = 30_000L;
    @ConfigValue("rush.proc-per-level")
    public double rushProc = 0.06;
    @ConfigValue("rush.cd-ms")
    public long rushCdMs = 10_000L;
    @ConfigValue("spite.pct-per-level")
    public double spitePctPerLevel = 0.08;
    @ConfigValue("vampiric-plate.proc-per-level")
    public double vampiricPlateProc = 0.04;
    @ConfigValue("vampiric-plate.heal-hp")
    public double vampiricPlateHeal = 2.0;
    @ConfigValue("counter.proc-per-level")
    public double counterProc = 0.04;
    @ConfigValue("counter.cd-ms")
    public long counterCdMs = 30_000L;

    // ──────────────── Ironclad (explosion armor) ────────────────
    @ConfigValue("ironclad.reduction-per-level")
    public double ironcladReductionPerLevel = 0.05;
    @ConfigValue("ironclad.reduction-cap")
    public double ironcladReductionCap = 0.15;
}
