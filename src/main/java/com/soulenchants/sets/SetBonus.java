package com.soulenchants.sets;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Pluggable armor-set behavior. Implement once per set; the
 * {@link SetManager} routes events to the implementation only when the
 * wearer has the full set equipped (or the partial-piece thresholds the
 * set declares).
 *
 * Design note: caching of "what set is this player wearing" lives in
 * {@link SetManager}, not in implementations. Implementations are stateless
 * singletons — any per-player state (cooldowns, etc.) goes through the
 * existing CooldownManager so it persists across re-equips.
 */
public interface SetBonus {

    /** Stable id used in NBT tag `se_set_id` on each gear piece. */
    String id();

    /** Human-readable label shown in tooltips/messages. */
    String displayName();

    /** Minimum pieces required for the bonus to be considered "active".
     *  Most sets use 4 (full set); some can use 2 or 3 for partial bonuses. */
    int requiredPieces();

    /** Lifecycle: called once when the player FIRST achieves requiredPieces. */
    default void onEquip(Player wearer) {}

    /** Lifecycle: called once when the player drops below requiredPieces. */
    default void onUnequip(Player wearer) {}

    /** Wearer about to deal damage. Mutate the event to apply attack bonuses.
     *  Return true if you applied a noticeable effect (used for telemetry). */
    default boolean onAttack(Player attacker, EntityDamageByEntityEvent e) { return false; }

    /** Wearer about to take damage. Mutate the event to apply mitigation. */
    default boolean onDamaged(Player victim, EntityDamageEvent e) { return false; }
}
