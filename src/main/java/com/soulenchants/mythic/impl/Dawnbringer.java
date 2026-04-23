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

/** Aura purges negative effects from wielder + guildmates in radius. */
public final class Dawnbringer extends MythicWeapon {

    private final MythicConfig cfg;
    private long lastPurge;

    public Dawnbringer(MythicConfig cfg) {
        super("dawnbringer", "Dawnbringer", ProximityMode.AURA);
        this.cfg = cfg;
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "The first light drives out the dark.",
                "",
                MessageStyle.TIER_LEGENDARY + "▸ " + MessageStyle.MUTED + "Purges debuffs within " +
                        MessageStyle.VALUE + (int)cfg.dawnbringerAuraRadius + "m",
                MessageStyle.TIER_LEGENDARY + "▸ " + MessageStyle.MUTED + "Grants Regen " +
                        (cfg.dawnbringerRegenAmplifier == 0 ? "I" : "II") + " aura"
        );
    }

    @Override
    public void onAuraTick(Player owner) {
        long now = System.currentTimeMillis();
        // Purge interval is in ticks; convert to ms.
        long intervalMs = cfg.dawnbringerPurgeIntervalTicks * 50L;
        if (now - lastPurge < intervalMs) return;
        lastPurge = now;
        // Self — strip debuffs, then +1 Regen above current tier (stacks with
        // Implants / Soul Warden etc. instead of overwriting them).
        stripNegatives(owner);
        com.soulenchants.util.AuraStacker.bump(owner, PotionEffectType.REGENERATION, 60);
        // Nearby players (guild-filtering is optional — omit the hard dep and buff all allies)
        for (Entity e : owner.getNearbyEntities(cfg.dawnbringerAuraRadius,
                cfg.dawnbringerAuraRadius, cfg.dawnbringerAuraRadius)) {
            if (!(e instanceof Player)) continue;
            Player other = (Player) e;
            stripNegatives(other);
            com.soulenchants.util.AuraStacker.bump(other, PotionEffectType.REGENERATION, 60);
        }
    }

    private static void stripNegatives(Player p) {
        p.removePotionEffect(PotionEffectType.POISON);
        p.removePotionEffect(PotionEffectType.WITHER);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.removePotionEffect(PotionEffectType.SLOW);
        p.removePotionEffect(PotionEffectType.WEAKNESS);
        p.removePotionEffect(PotionEffectType.CONFUSION);
        p.removePotionEffect(PotionEffectType.SLOW_DIGGING);
    }
}
