package com.soulenchants.mythic.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/** Harvester — souls on kill + permanent Speed aura while held. */
public final class Voidreaver extends MythicWeapon {

    private final SoulEnchants plugin;
    private final MythicConfig cfg;

    public Voidreaver(SoulEnchants plugin, MythicConfig cfg) {
        super("voidreaver", "Voidreaver", ProximityMode.HELD);
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "Drinks the void between heartbeats.",
                "",
                MessageStyle.TIER_SOUL + "▸ " + MessageStyle.VALUE + "+" + cfg.voidreaverSoulsOnKill +
                        " souls" + MessageStyle.MUTED + " per kill",
                MessageStyle.TIER_RARE + "▸ " + MessageStyle.VALUE + "+" + (int)(cfg.voidreaverCritBonus*100) +
                        "% crit" + MessageStyle.MUTED + " bonus while held",
                MessageStyle.GOOD + "▸ " + MessageStyle.MUTED + "Grants Speed " +
                        (cfg.voidreaverAuraSpeed == 0 ? "I" : "II") + " aura"
        );
    }

    @Override
    public void onAuraTick(Player owner) {
        // Stacks +1 above whatever the player already has (boots, potions, etc).
        com.soulenchants.util.AuraStacker.bump(owner, PotionEffectType.SPEED, 40);
    }

    @Override
    public void onKill(Player owner, EntityDeathEvent event) {
        plugin.getSoulManager().add(owner, cfg.voidreaverSoulsOnKill);
        owner.sendMessage(MessageStyle.PREFIX + MessageStyle.TIER_SOUL + MessageStyle.BOLD +
                "❖ VOIDREAVER " + MessageStyle.RESET + MessageStyle.GOOD + "+" +
                cfg.voidreaverSoulsOnKill + " souls");
    }
}
