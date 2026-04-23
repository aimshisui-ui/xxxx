package com.soulenchants.mythic.impl;

import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Effect;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Emberlash — PvE mythic sword. Every landed swing scatters fire into
 * a 4-block splash around the victim: 3 flat damage + 4s ignite on every
 * nearby non-player. Players + ArmorStand-based pet companions inside
 * the splash are skipped so allies / pets aren't griefed.
 *
 * SPLASH_GUARD is a ThreadLocal sentinel flipped ON while we deal the
 * splash damage, so the EntityDamageByEntityEvent that splash triggers
 * re-enters onAttack and immediately short-circuits. Without this, the
 * splash recurses into itself and crashes the tick thread.
 */
public final class Emberlash extends MythicWeapon {

    private static final double SPLASH_RADIUS = 4.0;
    private static final double SPLASH_DMG    = 3.0;
    private static final int    IGNITE_TICKS  = 80;

    /** True while we're applying splash damage — prevents recursive re-entry. */
    private static final ThreadLocal<Boolean> SPLASH_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public Emberlash() {
        super("emberlash", "Emberlash", ProximityMode.HELD);
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "Each swing cracks the air into cinders.",
                "",
                MessageStyle.TIER_EPIC + "▸ " + MessageStyle.VALUE + SPLASH_RADIUS + "-block"
                        + MessageStyle.MUTED + " fire splash on hit",
                MessageStyle.TIER_EPIC + "▸ " + MessageStyle.VALUE + SPLASH_DMG + " dmg"
                        + MessageStyle.MUTED + " + " + (IGNITE_TICKS / 20) + "s ignite to nearby mobs"
        );
    }

    @Override
    public void onAttack(Player owner, EntityDamageByEntityEvent event) {
        if (SPLASH_GUARD.get()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity hub = (LivingEntity) event.getEntity();
        if (hub instanceof ArmorStand) return; // don't splash off pet/cosmetic stands

        SPLASH_GUARD.set(true);
        try {
            for (Entity near : hub.getNearbyEntities(SPLASH_RADIUS, 2, SPLASH_RADIUS)) {
                if (near == owner || near == hub) continue;
                if (!(near instanceof LivingEntity)) continue;
                // Skip players (friendly fire) and ArmorStands (pet companions).
                if (near instanceof Player || near instanceof ArmorStand) continue;
                LivingEntity le = (LivingEntity) near;
                le.damage(SPLASH_DMG, owner);
                le.setFireTicks(Math.max(le.getFireTicks(), IGNITE_TICKS));
                le.getWorld().playEffect(le.getLocation().add(0, 1, 0), Effect.MOBSPAWNER_FLAMES, 0);
            }
        } finally {
            SPLASH_GUARD.set(false);
        }
    }
}
