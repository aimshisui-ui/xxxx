package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.enchants.EnchantTier;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.items.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class IronGolemManager {

    private final SoulEnchants plugin;
    private IronGolemBoss active;
    private final Random rng = new Random();

    public IronGolemManager(SoulEnchants plugin) { this.plugin = plugin; }

    public boolean summon(Location loc) {
        if (active != null && !active.getEntity().isDead()) return false;
        active = new IronGolemBoss(plugin, loc);
        active.start();
        return true;
    }

    public IronGolemBoss getActive() {
        if (active != null && active.getEntity().isDead()) active = null;
        return active;
    }

    public boolean isIronGolemBoss(LivingEntity entity) {
        IronGolemBoss b = getActive();
        return b != null && b.getEntity().getUniqueId().equals(entity.getUniqueId());
    }

    public void onIronGolemDeath(Player killer) {
        IronGolemBoss b = getActive();
        if (b == null) return;
        long souls = plugin.getConfig().getLong("irongolem.souls-reward", 1500);
        if (killer != null) {
            ItemStack hand = killer.getItemInHand();
            int reaper = hand == null ? 0 : ItemUtil.getLevel(hand, "soulreaper");
            if (reaper > 0) {
                long bonus = (long) (souls * 0.5 * reaper);
                souls += bonus;
                killer.sendMessage("§5✦ §dSoul Reaper bonus: §f+" + bonus);
            }
            plugin.getSoulManager().add(killer, souls);
        }
        Player top = b.getTopDamager();
        if (top != null && (killer == null || !top.equals(killer))) {
            plugin.getSoulManager().add(top, souls / 2);
            top.sendMessage("§6✦ §eTop damage reward: §f+" + (souls / 2) + " Souls");
        }
        // Drop guaranteed Epic+ enchant book
        List<CustomEnchant> pool = new ArrayList<>();
        for (CustomEnchant e : EnchantRegistry.all()) {
            int t = e.getTier().ordinal();
            if (t >= EnchantTier.EPIC.ordinal() && t < EnchantTier.SOUL_ENCHANT.ordinal()) pool.add(e);
        }
        if (pool.isEmpty()) pool.addAll(EnchantRegistry.all());
        CustomEnchant chosen = pool.get(rng.nextInt(pool.size()));
        b.getEntity().getWorld().dropItemNaturally(b.getEntity().getLocation(),
                ItemFactories.book(chosen, Math.max(1, chosen.getMaxLevel() / 2)));
        // Drop the unique "Iron Heart" item
        b.getEntity().getWorld().dropItemNaturally(b.getEntity().getLocation(), ironHeart());
        b.stop(true);
        active = null;
    }

    private ItemStack ironHeart() {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Iron Heart");
        meta.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  ",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "The still-warm core of",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "the Ironheart Colossus.",
                "",
                ChatColor.YELLOW + "» Trophy item from a boss kill",
                ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  "
        ));
        item.setItemMeta(meta);
        return item;
    }
}
