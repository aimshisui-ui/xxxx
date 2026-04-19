package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.attacks.ApocalypseInvuln;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.enchants.EnchantTier;
import com.soulenchants.items.ItemFactories;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VeilweaverManager {

    private final SoulEnchants plugin;
    private Veilweaver active;
    private final Random rng = new Random();

    public VeilweaverManager(SoulEnchants plugin) { this.plugin = plugin; }

    public boolean summon(Location loc) {
        if (active != null && !active.getEntity().isDead()) return false;
        active = new Veilweaver(plugin, loc);
        active.start();
        return true;
    }

    public Veilweaver getActive() {
        return active;
    }

    /** Manual nuller — called by death handler. Don't auto-null in getActive(),
     *  because EntityDeathEvent fires while entity.isDead() is already true and
     *  the death handler still needs access to the boss object. */
    public void clearActive() { this.active = null; }

    public boolean isVeilweaver(LivingEntity entity) {
        return active != null && active.getEntity().getUniqueId().equals(entity.getUniqueId());
    }

    public boolean isInvulnerable() {
        Veilweaver vw = getActive();
        if (vw == null) return false;
        return vw.isInvulnerable() || ApocalypseInvuln.isInvuln(vw);
    }

    public void onVeilweaverDeath(Player killer) {
        Veilweaver vw = getActive();
        if (vw == null) return;
        long souls = plugin.getConfig().getLong("veilweaver.souls-reward", 2500);
        if (killer != null) {
            // Soul Reaper on killer's held weapon = bonus souls
            org.bukkit.inventory.ItemStack hand = killer.getItemInHand();
            int reaper = hand == null ? 0 : com.soulenchants.items.ItemUtil.getLevel(hand, "soulreaper");
            if (reaper > 0) {
                long bonus = (long) (souls * 0.5 * reaper);
                souls += bonus;
                killer.sendMessage("§5✦ §dSoul Reaper bonus: §f+" + bonus);
            }
            plugin.getSoulManager().add(killer, souls);
        }
        // Drop top damager's reward + a guaranteed legendary book
        Player top = vw.getTopDamager();
        if (top != null) {
            plugin.getSoulManager().add(top, souls / 2);
            top.sendMessage("§5✦ §dTop damage reward: §f+" + (souls / 2) + " Souls");
        }
        // Drop a legendary+ book at boss location
        List<CustomEnchant> pool = new ArrayList<>();
        for (CustomEnchant e : EnchantRegistry.all()) {
            if (e.getTier().ordinal() >= EnchantTier.EPIC.ordinal()) pool.add(e);
        }
        if (pool.isEmpty()) pool.addAll(EnchantRegistry.all());
        CustomEnchant chosen = pool.get(rng.nextInt(pool.size()));
        vw.getEntity().getWorld().dropItemNaturally(vw.getEntity().getLocation(),
                ItemFactories.book(chosen, Math.max(1, chosen.getMaxLevel() / 2)));
        // Full boss loot table
        com.soulenchants.loot.BossLootTable.dropVeilweaver(vw.getEntity().getLocation());

        com.soulenchants.bosses.BossDeathBroadcast.broadcast(plugin,
                org.bukkit.ChatColor.DARK_PURPLE + "" + org.bukkit.ChatColor.BOLD + "The Veilweaver",
                org.bukkit.ChatColor.DARK_PURPLE, vw.getDamageDealt(),
                Veilweaver.MAX_HP);

        vw.stop(true);
        active = null;
    }
}
