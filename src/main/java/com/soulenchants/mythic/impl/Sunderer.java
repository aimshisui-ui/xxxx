package com.soulenchants.mythic.impl;

import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/** Greataxe — adds true damage + applies Mining Fatigue (armor-strip flavour). */
public final class Sunderer extends MythicWeapon {

    private final MythicConfig cfg;

    public Sunderer(MythicConfig cfg) {
        super("sunderer", "Sunderer", ProximityMode.HELD);
        this.cfg = cfg;
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "Armor is just another kind of stone.",
                "",
                MessageStyle.TIER_LEGENDARY + "▸ " + MessageStyle.VALUE + "+" + (int)(cfg.sundererTruePct*100) +
                        "% true damage" + MessageStyle.MUTED + " (ignores armor)",
                MessageStyle.TIER_LEGENDARY + "▸ " + MessageStyle.MUTED + "Strips target for " +
                        MessageStyle.VALUE + (cfg.sundererStripTicks / 20) + "s"
        );
    }

    @Override
    public void onAttack(Player owner, EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();
        // True damage — tacked on top without going through armor
        double trueDmg = event.getDamage() * cfg.sundererTruePct;
        victim.damage(trueDmg);  // sourceless so it doesn't re-enter onAttack logic
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                cfg.sundererStripTicks, 2), true);
    }
}
