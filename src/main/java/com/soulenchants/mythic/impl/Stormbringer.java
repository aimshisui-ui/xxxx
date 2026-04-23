package com.soulenchants.mythic.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.config.MythicConfig;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Lightning chain on crit. Hits up to N enemies, damage falloff each jump. */
public final class Stormbringer extends MythicWeapon {

    private final SoulEnchants plugin;
    private final MythicConfig cfg;
    private final Random rng = new Random();

    public Stormbringer(SoulEnchants plugin, MythicConfig cfg) {
        super("stormbringer", "Stormbringer", ProximityMode.HELD);
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @Override
    public List<String> getLoreLines() {
        return Arrays.asList(
                MessageStyle.MUTED + "The sky answers to steel.",
                "",
                MessageStyle.TIER_RARE + "▸ " + MessageStyle.VALUE + (int)(cfg.stormbringerProc * 100) +
                        "%" + MessageStyle.MUTED + " chance on hit",
                MessageStyle.TIER_RARE + "▸ " + MessageStyle.MUTED + "Chain to " + MessageStyle.VALUE +
                        cfg.stormbringerChainCount + MessageStyle.MUTED + " enemies, " +
                        MessageStyle.VALUE + (int)(cfg.stormbringerFalloff * 100) + "%" +
                        MessageStyle.MUTED + " falloff",
                MessageStyle.TIER_SOUL + "▸ " + MessageStyle.MUTED + "Costs " + MessageStyle.VALUE +
                        cfg.stormbringerCost + " souls" + MessageStyle.MUTED + " per chain"
        );
    }

    @Override
    public void onAttack(Player owner, EntityDamageByEntityEvent event) {
        if (rng.nextDouble() >= cfg.stormbringerProc) return;
        if (!plugin.getCooldownManager().isReady("stormbringer", owner.getUniqueId())) return;
        // v1.1 — gem-gated. Mythic cost path goes through SoulGemUtil so a
        // Stormbringer without a Soul Gem in inventory can't chain.
        if (!com.soulenchants.items.SoulGemUtil.chargeSoulCost(plugin, owner, cfg.stormbringerCost)) return;
        plugin.getCooldownManager().set("stormbringer", owner.getUniqueId(), cfg.stormbringerCdMs);
        LivingEntity target = (event.getEntity() instanceof LivingEntity) ? (LivingEntity) event.getEntity() : null;
        if (target == null) return;
        double dmg = event.getDamage();
        List<LivingEntity> chained = new ArrayList<>();
        chained.add(target);
        LivingEntity current = target;
        for (int i = 0; i < cfg.stormbringerChainCount; i++) {
            LivingEntity next = nearestUnchained(current, chained);
            if (next == null) break;
            chained.add(next);
            dmg *= cfg.stormbringerFalloff;
            next.damage(dmg, owner);
            Location loc = next.getLocation();
            loc.getWorld().strikeLightningEffect(loc);
            loc.getWorld().playSound(loc, Sound.AMBIENCE_THUNDER, 0.7f, 1.4f);
            current = next;
        }
        owner.sendMessage(MessageStyle.PREFIX + MessageStyle.TIER_RARE + MessageStyle.BOLD +
                "⚡ STORMBRINGER " + MessageStyle.RESET + MessageStyle.MUTED + "chained " +
                MessageStyle.VALUE + chained.size() + MessageStyle.MUTED + " targets");
    }

    private LivingEntity nearestUnchained(LivingEntity from, List<LivingEntity> excluded) {
        LivingEntity best = null;
        double bestDist = cfg.stormbringerChainRadius * cfg.stormbringerChainRadius;
        for (Entity e : from.getNearbyEntities(cfg.stormbringerChainRadius,
                cfg.stormbringerChainRadius, cfg.stormbringerChainRadius)) {
            if (!(e instanceof LivingEntity) || e instanceof Player) continue;
            LivingEntity le = (LivingEntity) e;
            if (excluded.contains(le)) continue;
            double d = le.getLocation().distanceSquared(from.getLocation());
            if (d < bestDist) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }
}
