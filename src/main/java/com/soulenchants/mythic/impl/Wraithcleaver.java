package com.soulenchants.mythic.impl;

import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;

import java.util.Arrays;
import java.util.List;

/** Heal on every Cleave AoE proc. Wraps legacy mythic_held=2. */
public final class Wraithcleaver extends MythicWeapon {

    private final MythicConfig cfg;

    public Wraithcleaver(MythicConfig cfg) {
        super("wraithcleaver", "Wraithcleaver", ProximityMode.HELD);
        this.cfg = cfg;
    }

    public double healPerCleaveProc() { return cfg.wraithcleaverHealPerCleave; }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "A single stroke, many shadows.",
                "",
                MessageStyle.GOOD + "▸ " + MessageStyle.VALUE + "+" + cfg.wraithcleaverHealPerCleave +
                        " HP" + MessageStyle.MUTED + " per Cleave proc"
        );
    }
}
