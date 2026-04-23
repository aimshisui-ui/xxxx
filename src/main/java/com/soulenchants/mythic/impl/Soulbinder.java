package com.soulenchants.mythic.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Arrays;
import java.util.List;

/** Bow mythic — steals souls per hit, adds true damage on ranged shots. */
public final class Soulbinder extends MythicWeapon {

    private final SoulEnchants plugin;
    private final MythicConfig cfg;

    public Soulbinder(SoulEnchants plugin, MythicConfig cfg) {
        super("soulbinder", "Soulbinder", ProximityMode.HELD);
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "Each arrow threads a soul home.",
                "",
                MessageStyle.TIER_SOUL + "▸ " + MessageStyle.VALUE + "+" + cfg.soulbinderSoulsPerHit +
                        " souls" + MessageStyle.MUTED + " per ranged hit",
                MessageStyle.TIER_LEGENDARY + "▸ " + MessageStyle.VALUE + "+" +
                        (int)(cfg.soulbinderTruePct*100) + "%" + MessageStyle.MUTED + " true damage"
        );
    }

    @Override
    public void onAttack(Player owner, EntityDamageByEntityEvent event) {
        // Bow attacks arrive as the arrow being the damager; Bukkit marks cause PROJECTILE.
        if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        plugin.getSoulManager().add(owner, cfg.soulbinderSoulsPerHit);
        LivingEntity victim = (LivingEntity) event.getEntity();
        double bonus = event.getDamage() * cfg.soulbinderTruePct;
        victim.damage(bonus);
    }
}
