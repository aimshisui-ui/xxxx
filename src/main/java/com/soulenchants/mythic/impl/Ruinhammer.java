package com.soulenchants.mythic.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Effect;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ruinhammer — PvE mythic axe. Stack-based damage ramp on consecutive
 * mob kills: each kill adds +1 stack (max 10), each stack grants +3%
 * bonus damage on the NEXT attack. At 10 stacks the next kill releases
 * a 5-block shockwave (5 dmg) around the victim and resets stacks.
 * Stacks also reset if the wielder dies or no kill happens within 15s.
 */
public final class Ruinhammer extends MythicWeapon {

    private static final int    MAX_STACKS     = 10;
    private static final double BONUS_PER_STACK = 0.03;
    private static final long   STACK_EXPIRE_MS = 15_000L;
    private static final double BURST_RADIUS    = 5.0;
    private static final double BURST_DMG       = 5.0;

    private final Map<UUID, Integer> stacks   = new HashMap<>();
    private final Map<UUID, Long>    lastKill = new HashMap<>();

    public Ruinhammer(SoulEnchants plugin) {
        super("ruinhammer", "Ruinhammer", ProximityMode.HELD);
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "Swings weigh what you've killed with them.",
                "",
                MessageStyle.TIER_SOUL + "▸ " + MessageStyle.VALUE + "+" + (int)(BONUS_PER_STACK * 100) +
                        "%" + MessageStyle.MUTED + " dmg per stack (kills, max " + MAX_STACKS + ")",
                MessageStyle.TIER_SOUL + "▸ " + MessageStyle.VALUE + BURST_DMG + " dmg"
                        + MessageStyle.MUTED + " in " + (int) BURST_RADIUS + "-block shockwave at " + MAX_STACKS + " stacks",
                MessageStyle.MUTED + "Stacks reset after 15s with no kill."
        );
    }

    @Override
    public void onAttack(Player owner, EntityDamageByEntityEvent event) {
        UUID id = owner.getUniqueId();
        Integer s = stacks.get(id);
        Long last = lastKill.get(id);
        if (s == null || s <= 0) return;
        if (last != null && System.currentTimeMillis() - last > STACK_EXPIRE_MS) {
            stacks.remove(id);
            return;
        }
        event.setDamage(event.getDamage() * (1.0 + BONUS_PER_STACK * s));
    }

    @Override
    public void onKill(Player owner, EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        UUID id = owner.getUniqueId();
        int s = stacks.getOrDefault(id, 0);
        if (s >= MAX_STACKS) {
            // Release shockwave, reset stacks
            LivingEntity victim = event.getEntity();
            org.bukkit.Location center = victim.getLocation();
            center.getWorld().createExplosion(center.getX(), center.getY() + 1, center.getZ(), 0f, false);
            for (Entity near : victim.getNearbyEntities(BURST_RADIUS, 3, BURST_RADIUS)) {
                if (near == owner) continue;
                // Skip Players (friendly fire) + ArmorStands (pet companions / cosmetics).
                if (!(near instanceof LivingEntity) || near instanceof Player || near instanceof ArmorStand) continue;
                ((LivingEntity) near).damage(BURST_DMG, owner);
                near.getWorld().playEffect(near.getLocation().add(0, 1, 0), Effect.MOBSPAWNER_FLAMES, 0);
            }
            owner.sendMessage(MessageStyle.TIER_SOUL + "✦ RUINHAMMER " + MessageStyle.MUTED + "— shockwave released.");
            stacks.put(id, 0);
        } else {
            stacks.put(id, s + 1);
        }
        lastKill.put(id, System.currentTimeMillis());
    }
}
