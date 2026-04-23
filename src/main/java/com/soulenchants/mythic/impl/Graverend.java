package com.soulenchants.mythic.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Graverend — PvE mythic axe. Heals 10% of killed mob's max HP on kill;
 * bonus damage against tracked bosses + their minions. Tuned so sustained
 * boss fights (Veilweaver, Hollow King) become self-regenerating.
 */
public final class Graverend extends MythicWeapon {

    private static final double HEAL_PCT_OF_MOB_MAX = 0.10;
    private static final double BOSS_BONUS_PCT      = 0.20;

    private final SoulEnchants plugin;

    public Graverend(SoulEnchants plugin) {
        super("graverend", "Graverend", ProximityMode.HELD);
        this.plugin = plugin;
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "Cut the thread, reel in the life.",
                "",
                MessageStyle.GOOD + "▸ " + MessageStyle.VALUE + "Heal " + (int)(HEAL_PCT_OF_MOB_MAX * 100) +
                        "%" + MessageStyle.MUTED + " of the slain mob's max HP on kill",
                MessageStyle.TIER_SOUL + "▸ " + MessageStyle.VALUE + "+" + (int)(BOSS_BONUS_PCT * 100) +
                        "%" + MessageStyle.MUTED + " damage vs bosses and their minions"
        );
    }

    @Override
    public void onAttack(Player owner, EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();
        if (isBossOrMinion(victim)) {
            event.setDamage(event.getDamage() * (1.0 + BOSS_BONUS_PCT));
        }
    }

    @Override
    public void onKill(Player owner, EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player) return;
        double heal = victim.getMaxHealth() * HEAL_PCT_OF_MOB_MAX;
        owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + heal));
    }

    private boolean isBossOrMinion(LivingEntity victim) {
        com.soulenchants.bosses.Veilweaver vw = plugin.getVeilweaverManager().getActive();
        if (vw != null) {
            if (victim.getUniqueId().equals(vw.getEntity().getUniqueId())) return true;
            for (LivingEntity m : vw.getMinions())
                if (m.getUniqueId().equals(victim.getUniqueId())) return true;
        }
        com.soulenchants.bosses.IronGolemBoss ig = plugin.getIronGolemManager().getActive();
        if (ig != null && victim.getUniqueId().equals(ig.getEntity().getUniqueId())) return true;
        return com.soulenchants.mobs.CustomMob.idOf(victim) != null;
    }
}
