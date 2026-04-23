package com.soulenchants.config;

/**
 * Mythic weapon balance knobs. Each mythic is a weapon whose effect is active
 * while held — or while anywhere in the hotbar, for some aura-style mythics.
 * This file ships defaults; operators tune via mythics.yml.
 *
 * Mythic IDs are stable strings; the implementing class reads these fields
 * by name-matching, so renaming a field here is a breaking change.
 */
public final class MythicConfig {

    // ──────────────── Crimson Tongue — Bleed bloodletter ────────────────
    @ConfigValue("crimson-tongue.heal-per-bleed-tick")
    public double crimsonTongueHealPerTick = 1.0;

    // ──────────────── Wraithcleaver — Cleave cycler ────────────────
    @ConfigValue("wraithcleaver.heal-per-cleave-proc")
    public double wraithcleaverHealPerCleave = 1.0;

    // ──────────────── Stormbringer — lightning on crit ────────────────
    @ConfigValue("stormbringer.proc-chance")
    public double stormbringerProc = 0.08;
    @ConfigValue("stormbringer.cd-ms")
    public long stormbringerCdMs = 4_000L;
    @ConfigValue("stormbringer.chain-count")
    public int stormbringerChainCount = 3;
    @ConfigValue("stormbringer.chain-radius")
    public double stormbringerChainRadius = 6.0;
    @ConfigValue("stormbringer.chain-falloff")
    public double stormbringerFalloff = 0.75;
    @ConfigValue("stormbringer.soul-cost")
    public int stormbringerCost = 20;

    // ──────────────── Voidreaver — soul steal on kill ────────────────
    @ConfigValue("voidreaver.souls-on-kill")
    public int voidreaverSoulsOnKill = 50;
    @ConfigValue("voidreaver.crit-bonus-pct")
    public double voidreaverCritBonus = 0.30;
    @ConfigValue("voidreaver.aura-speed-amplifier")
    public int voidreaverAuraSpeed = 0;

    // ──────────────── Dawnbringer — debuff purge aura ────────────────
    @ConfigValue("dawnbringer.aura-radius")
    public double dawnbringerAuraRadius = 6.0;
    @ConfigValue("dawnbringer.purge-interval-ticks")
    public int dawnbringerPurgeIntervalTicks = 40;
    @ConfigValue("dawnbringer.regen-amplifier")
    public int dawnbringerRegenAmplifier = 0;

    // ──────────────── Sunderer — armor-ignoring greataxe ────────────────
    @ConfigValue("sunderer.true-dmg-pct")
    public double sundererTruePct = 0.20;
    @ConfigValue("sunderer.armor-strip-ticks")
    public int sundererStripTicks = 60;

    // ──────────────── Phoenix Feather — on-kill burst heal ────────────────
    @ConfigValue("phoenix-feather.heal-on-kill")
    public double phoenixFeatherHealOnKill = 4.0;
    @ConfigValue("phoenix-feather.ignite-radius")
    public double phoenixFeatherIgniteRadius = 3.0;

    // ──────────────── Soulbinder — bow, steals souls over range ────────────────
    @ConfigValue("soulbinder.souls-per-hit")
    public int soulbinderSoulsPerHit = 5;
    @ConfigValue("soulbinder.true-dmg-pct")
    public double soulbinderTruePct = 0.15;

    // ──────────────── Tidecaller — offhand-aura (Nordic-Bard-style) ────────────────
    @ConfigValue("tidecaller.aura-radius")
    public double tidecallerAuraRadius = 8.0;
    @ConfigValue("tidecaller.water-breathing")
    public boolean tidecallerWaterBreathing = true;
    @ConfigValue("tidecaller.depth-strider-amplifier")
    public int tidecallerDepthAmp = 2;

    // ──────────────── Aura system globals ────────────────
    @ConfigValue("aura.tick-interval-ticks")
    public int auraTickInterval = 20;
    @ConfigValue("aura.hotbar-counts-as-held")
    public boolean auraHotbarCountsAsHeld = true;
}
