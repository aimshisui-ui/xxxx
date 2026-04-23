package com.soulenchants.mythic.impl;

import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/**
 * Nordic Bard-style aura — buffs apply to everyone in radius, including the
 * wielder, while the mythic is in the hotbar. Works even if not in main hand,
 * so you can bring it along without sacrificing your weapon slot.
 */
public final class Tidecaller extends MythicWeapon {

    private final MythicConfig cfg;

    public Tidecaller(MythicConfig cfg) {
        super("tidecaller", "Tidecaller", ProximityMode.AURA);
        this.cfg = cfg;
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "Carried in the hotbar, answered by the tide.",
                "",
                MessageStyle.TIER_RARE + "▸ " + MessageStyle.MUTED + "Water Breathing aura to allies within "
                        + MessageStyle.VALUE + (int)cfg.tidecallerAuraRadius + "m"
        );
    }

    @Override
    public void onAuraTick(Player owner) {
        if (cfg.tidecallerWaterBreathing) {
            owner.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,
                    60, 0, true, false), true);
        }
        for (Entity e : owner.getNearbyEntities(cfg.tidecallerAuraRadius,
                cfg.tidecallerAuraRadius, cfg.tidecallerAuraRadius)) {
            if (!(e instanceof Player)) continue;
            Player other = (Player) e;
            if (cfg.tidecallerWaterBreathing) {
                other.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,
                        60, 0, true, false), true);
            }
        }
    }
}
