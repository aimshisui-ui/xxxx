package com.soulenchants.sets.impl;

import com.soulenchants.SoulEnchants;
import com.soulenchants.sets.SetBonus;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * BOSS KILLER set — full-PvE 4-piece bonus.
 *   • +20% damage TO bosses + boss minions
 *   • -15% damage FROM bosses + boss minions
 *   • Permanent Regeneration I when below 50% HP
 *   • +1 absorption heart granted on equip (refreshes every 60s while in combat)
 */
public class BossKillerSet implements SetBonus {

    public static final String ID = "bosskiller";
    private final SoulEnchants plugin;

    public BossKillerSet(SoulEnchants plugin) { this.plugin = plugin; }

    @Override public String id()              { return ID; }
    @Override public String displayName()     { return ChatColor.GOLD + "Boss Killer"; }
    @Override public int    requiredPieces()  { return 4; }

    @Override public void onEquip(Player p) {
        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ BOSS KILLER " + ChatColor.YELLOW + "set bonus active.");
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1200, 0, true, false), true);
    }

    @Override public void onUnequip(Player p) {
        p.sendMessage(ChatColor.GRAY + "✦ Boss Killer set bonus removed.");
    }

    @Override public boolean onAttack(Player attacker, EntityDamageByEntityEvent e) {
        Entity target = e.getEntity();
        if (target instanceof LivingEntity && isBossOrMinion((LivingEntity) target)) {
            e.setDamage(e.getDamage() * 1.20);
            return true;
        }
        return false;
    }

    @Override public boolean onDamaged(Player victim, EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent)) return false;
        EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent) e;
        Entity src = ee.getDamager();
        LivingEntity attacker = null;
        if (src instanceof LivingEntity) attacker = (LivingEntity) src;
        else if (src instanceof Projectile && ((Projectile) src).getShooter() instanceof LivingEntity)
            attacker = (LivingEntity) ((Projectile) src).getShooter();
        if (attacker == null || !isBossOrMinion(attacker)) return false;
        e.setDamage(e.getDamage() * 0.85);
        return true;
    }

    private boolean isBossOrMinion(LivingEntity ent) {
        com.soulenchants.bosses.Veilweaver vw = plugin.getVeilweaverManager().getActive();
        if (vw != null) {
            if (vw.getEntity().getUniqueId().equals(ent.getUniqueId())) return true;
            for (LivingEntity m : vw.getMinions())
                if (m != null && m.getUniqueId().equals(ent.getUniqueId())) return true;
            for (LivingEntity c : vw.getEchoClones())
                if (c != null && c.getUniqueId().equals(ent.getUniqueId())) return true;
        }
        com.soulenchants.bosses.IronGolemBoss ig = plugin.getIronGolemManager().getActive();
        if (ig != null && ig.getEntity().getUniqueId().equals(ent.getUniqueId())) return true;
        // Custom mob (Hollow King + cave roster): tagged via CustomMob.idOf
        return com.soulenchants.mobs.CustomMob.idOf(ent) != null;
    }
}
