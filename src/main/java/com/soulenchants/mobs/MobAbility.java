package com.soulenchants.mobs;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Composable behaviour layer for custom mobs. Each ability hooks one or
 * more lifecycle points and modifies the entity / damage / drops.
 *
 * Abilities are pure data transforms — they don't hold per-entity state
 * unless they explicitly use a static map. Most are stateless functions
 * of the entity passed in.
 */
public abstract class MobAbility {

    /** Called once when the mob spawns. */
    public void onSpawn(LivingEntity entity) {}

    /** Periodic tick — called every 20 ticks (1s). */
    public void onTick(LivingEntity entity) {}

    /** Mob hit a player. */
    public void onHitPlayer(LivingEntity attacker, Player victim, EntityDamageByEntityEvent e) {}

    /** Mob took damage from anything. */
    public void onHurt(LivingEntity victim, EntityDamageEvent e) {}

    /** Mob died. May give bonus drops, trigger explosions, etc. */
    public void onDeath(LivingEntity entity, Player killer) {}
}
