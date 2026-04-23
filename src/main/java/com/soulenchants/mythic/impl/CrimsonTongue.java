package com.soulenchants.mythic.impl;

import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;

import java.util.Arrays;
import java.util.List;

/** Wielder heals N HP per Bleed tick on nearby victims. Wraps the legacy
 *  mythic_held=1 flag and keeps the same semantics — only now it's a real
 *  named weapon with identity. */
public final class CrimsonTongue extends MythicWeapon {

    private final MythicConfig cfg;

    public CrimsonTongue(MythicConfig cfg) {
        super("crimson_tongue", "Crimson Tongue", ProximityMode.HELD);
        this.cfg = cfg;
    }

    public double healPerBleedTick() { return cfg.crimsonTongueHealPerTick; }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "Drinks deep from severed veins.",
                "",
                MessageStyle.GOOD + "▸ " + MessageStyle.VALUE + "+" + cfg.crimsonTongueHealPerTick +
                        " HP" + MessageStyle.MUTED + " per nearby Bleed tick"
        );
    }
}
