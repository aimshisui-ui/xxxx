package com.soulenchants.sets.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.sets.SetBonus;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * DUELIST set — full-PvP 4-piece bonus.
 *   • +12% damage TO other Players
 *   • -8% damage FROM other Players
 *   • Speed II for 2s when hit (3s CD per hit)
 *   • Phantom Step on equip — Speed I burst for 5s
 */
public class DuelistSet implements SetBonus {

    public static final String ID = "duelist";
    private final SoulEnchants plugin;

    public DuelistSet(SoulEnchants plugin) { this.plugin = plugin; }

    @Override public String id()             { return ID; }
    @Override public String displayName()    { return ChatColor.LIGHT_PURPLE + "Duelist"; }
    @Override public int    requiredPieces() { return 4; }

    @Override public void onEquip(Player p) {
        p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ DUELIST " + ChatColor.WHITE + "set bonus active.");
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, true, false), true);
    }

    @Override public void onUnequip(Player p) {
        p.sendMessage(ChatColor.GRAY + "✦ Duelist set bonus removed.");
    }

    @Override public boolean onAttack(Player attacker, EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) {
            // Don't bonus-damage guildmates — friendly fire is already cancelled
            // upstream by GuildListener at LOWEST priority, but be defensive.
            Player victim = (Player) e.getEntity();
            if (plugin.getGuildManager() != null
                    && plugin.getGuildManager().isAlly(attacker.getUniqueId(), victim.getUniqueId())) return false;
            e.setDamage(e.getDamage() * 1.12);
            return true;
        }
        return false;
    }

    @Override public boolean onDamaged(Player victim, EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent)) return false;
        EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent) e;
        Entity src = ee.getDamager();
        Player attackerPlayer = null;
        if (src instanceof Player) attackerPlayer = (Player) src;
        else if (src instanceof Projectile && ((Projectile) src).getShooter() instanceof Player)
            attackerPlayer = (Player) ((Projectile) src).getShooter();
        if (attackerPlayer == null) return false;
        // Mitigate PvP damage
        e.setDamage(e.getDamage() * 0.92);
        // Speed-burst escape — 3s CD per hit
        if (plugin.getCooldownManager().isReady("duelist_burst", victim.getUniqueId())) {
            plugin.getCooldownManager().set("duelist_burst", victim.getUniqueId(), 3_000L);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false), true);
        }
        return true;
    }
}
