package com.soulenchants.mythic.impl;

import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Arrays;
import java.util.List;

/** On kill, heal and ignite enemies in radius. */
public final class PhoenixFeather extends MythicWeapon {

    private final MythicConfig cfg;

    public PhoenixFeather(MythicConfig cfg) {
        super("phoenix_feather", "Phoenix Feather", ProximityMode.HELD);
        this.cfg = cfg;
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "From embers and obituaries.",
                "",
                MessageStyle.GOOD + "▸ " + MessageStyle.VALUE + "+" + cfg.phoenixFeatherHealOnKill +
                        " HP" + MessageStyle.MUTED + " on kill",
                MessageStyle.BAD + "▸ " + MessageStyle.MUTED + "Ignites enemies within " +
                        MessageStyle.VALUE + (int)cfg.phoenixFeatherIgniteRadius + "m"
        );
    }

    @Override
    public void onKill(Player owner, EntityDeathEvent event) {
        owner.setHealth(Math.min(owner.getMaxHealth(),
                owner.getHealth() + cfg.phoenixFeatherHealOnKill));
        for (Entity e : owner.getNearbyEntities(cfg.phoenixFeatherIgniteRadius,
                cfg.phoenixFeatherIgniteRadius, cfg.phoenixFeatherIgniteRadius)) {
            if (!(e instanceof LivingEntity) || e instanceof Player) continue;
            e.setFireTicks(80);
        }
    }
}
