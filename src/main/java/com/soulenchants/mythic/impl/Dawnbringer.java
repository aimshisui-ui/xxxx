package com.soulenchants.mythic.impl;

import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.mythic.state.MythicStateHandle;
import com.soulenchants.mythic.state.MythicStateRegistry;
import com.soulenchants.style.MessageStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Aura purges negative effects from wielder + guildmates in radius.
 *  v1.5 — per-wielder purge cooldown via MythicStateRegistry. Previously
 *  lastPurge was a shared field on the singleton MythicWeapon instance,
 *  so two players wielding Dawnbringer at once stepped on each other's
 *  cadence. MythicStateHandle isolates state per wielder UUID. */
public final class Dawnbringer extends MythicWeapon {

    private final MythicConfig cfg;

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
        DawnbringerState state = MythicStateRegistry.getOrCreate(
                owner, "dawnbringer", DawnbringerState::new);
        long now = System.currentTimeMillis();
        long intervalMs = cfg.dawnbringerPurgeIntervalTicks * 50L;
        if (now - state.lastPurge < intervalMs) return;
        state.lastPurge = now;
        stripNegatives(owner);
        com.soulenchants.util.AuraStacker.bump(owner, PotionEffectType.REGENERATION, 60);
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

    /** Per-wielder purge-cooldown state. Replaces the ex-shared
     *  `private long lastPurge` field on the MythicWeapon singleton. */
    public static final class DawnbringerState extends MythicStateHandle {
        public long lastPurge = 0L;
        public DawnbringerState(UUID wielder, String mythicId) { super(wielder, mythicId); }
    }
}
