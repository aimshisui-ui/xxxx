package com.soulenchants.style;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Centralised particle + sound flourishes for soul-themed moments —
 * book applications, soul-tier enchant procs, soul drains. Ported from
 * Nordic's Enchants module (CrankedPvP) so the in-world texture matches
 * what players are used to on comparable servers.
 *
 * All helpers accept a Location + Player so they can fire asynchronously
 * from listeners that may not have both. Minecraft 1.8.8's Effect enum
 * doesn't expose every ParticleType from newer versions, so we pick the
 * closest equivalents:
 *
 *   Nordic ParticleEffect.SPELL         → Effect.WITCH_MAGIC
 *   Nordic ParticleEffect.CRIT_MAGIC    → Effect.ENDER_SIGNAL  (1.8 Effect enum lacks CRIT_MAGIC)
 *   Nordic ParticleEffect.LAVA          → Effect.LAVA_POP
 *   Nordic ParticleEffect.FLAME         → Effect.MOBSPAWNER_FLAMES
 *   Nordic ParticleEffect.EXPLOSION_LG  → World.createExplosion(x,y,z, 0f, false) (visual only)
 *   Nordic ParticleEffect.VILLAGER_HAPPY→ Effect.HAPPY_VILLAGER
 */
public final class SoulVFX {

    private SoulVFX() {}

    // ─────────────────── Book application ───────────────────

    /**
     * Book successfully applied to gear — Nordic's celebratory burst:
     * 30 spell + 30 crit_magic particles around the player's torso,
     * LEVEL_UP at 0.75 pitch (more triumphant than vanilla-default).
     */
    public static void bookApplySuccess(Player p) {
        Location at = p.getLocation().add(0, 1, 0);
        // 1.8 Effect lacks CRIT_MAGIC — ENDER_SIGNAL gives the same shimmering
        // purple dust with a flicker, which reads just as celebratory.
        for (int i = 0; i < 15; i++) {
            p.getWorld().playEffect(offset(at, 0.6), Effect.ENDER_SIGNAL, 0);
        }
        for (int i = 0; i < 15; i++) {
            p.getWorld().playEffect(offset(at, 0.8), Effect.MOBSPAWNER_FLAMES, 0);
        }
        for (int i = 0; i < 10; i++) {
            p.getWorld().playEffect(offset(at, 0.5), Effect.WITCH_MAGIC, 0);
        }
        p.getWorld().playSound(p.getLocation(), Sound.LEVEL_UP, 1.0f, 0.75f);
    }

    /**
     * Book failed with destroy roll — LAVA_POP * 30 around the gear,
     * Sound.LAVA_POP at volume 5 pitch 0.1. Nordic plays this sound loud
     * on purpose: "your gear is gone" should be felt, not missed.
     */
    public static void bookApplyDestroy(Player p) {
        Location at = p.getLocation().add(0, 1, 0);
        for (int i = 0; i < 30; i++) {
            p.getWorld().playEffect(offset(at, 0.5), Effect.LAVA_POP, 0);
        }
        p.getWorld().playSound(p.getLocation(), Sound.LAVA_POP, 5.0f, 0.1f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_BREAK, 1.0f, 0.4f);
    }

    /** Book failed but gear intact — softer "whoosh". */
    public static void bookApplyFailSoft(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ORB_PICKUP, 0.6f, 0.8f);
    }

    // ─────────────────── Soul-tier enchant procs ───────────────────

    /**
     * Divine Immolation flourish — LAVA + FLAMES at the victim's torso,
     * with a muted FIREWORK_BLAST + ZOMBIE_PIG_ANGRY stereo pair.
     */
    public static void divineImmolationHit(Location victim, Player source) {
        for (int i = 0; i < 30; i++) {
            victim.getWorld().playEffect(offset(victim.clone().add(0, 1, 0), 0.6), Effect.LAVA_POP, 0);
        }
        for (int i = 0; i < 15; i++) {
            victim.getWorld().playEffect(offset(victim.clone().add(0, 1, 0), 0.5), Effect.MOBSPAWNER_FLAMES, 0);
        }
        source.getWorld().playSound(source.getLocation(), Sound.FIREWORK_BLAST, 1.0f, 0.3f);
        source.getWorld().playSound(source.getLocation(), Sound.ZOMBIE_PIG_ANGRY, 0.8f, 0.5f);
    }

    /** Nature's Wrath root explosion — SPELL ring + visual-only blast + LAVA. */
    public static void naturesWrathBurst(Location center) {
        for (int i = 0; i < 30; i++) {
            center.getWorld().playEffect(offset(center.clone().add(0, 1, 0), 0.8), Effect.WITCH_MAGIC, 0);
        }
        for (int i = 0; i < 15; i++) {
            center.getWorld().playEffect(offset(center.clone().add(0, 1, 0), 0.5), Effect.LAVA_POP, 0);
        }
        // Visual-only explosion (power 0, no block damage, no fire).
        center.getWorld().createExplosion(center.getX(), center.getY() + 1, center.getZ(), 0f, false, false);
        center.getWorld().playSound(center, Sound.EXPLODE, 0.8f, 1.2f);
    }

    /** Soul Burst knock — tight HAPPY_VILLAGER ring for the shield-up moment. */
    public static void soulBurstRing(Location center) {
        for (int i = 0; i < 20; i++) {
            double a = (Math.PI * 2 * i) / 20.0;
            Location at = center.clone().add(Math.cos(a) * 1.5, 1, Math.sin(a) * 1.5);
            center.getWorld().playEffect(at, Effect.HAPPY_VILLAGER, 0);
        }
        center.getWorld().playSound(center, Sound.ENDERDRAGON_GROWL, 0.5f, 1.8f);
    }

    /** Phoenix lethal save — dragon growl + flame pillar. */
    public static void phoenixSave(Player p) {
        Location at = p.getLocation();
        for (int y = 0; y < 3; y++) {
            for (int i = 0; i < 15; i++) {
                p.getWorld().playEffect(offset(at.clone().add(0, y, 0), 0.4), Effect.MOBSPAWNER_FLAMES, 0);
            }
        }
        p.getWorld().playSound(at, Sound.ENDERDRAGON_GROWL, 1.0f, 1.25f);
    }

    // ─────────────────── Helpers ───────────────────

    /** Random offset for a particle burst. */
    private static Location offset(Location base, double spread) {
        return base.clone().add(
                (Math.random() - 0.5) * 2 * spread,
                (Math.random() - 0.5) * spread,
                (Math.random() - 0.5) * 2 * spread);
    }
}
