package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.enchants.impl.LifestealEnchant;
import com.soulenchants.items.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DamageListener implements Listener {

    private final SoulEnchants plugin;

    public DamageListener(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player attacker = (Player) e.getDamager();
        ItemStack hand = attacker.getItemInHand();
        if (hand == null) return;
        int level = ItemUtil.getLevel(hand, "lifesteal");
        if (level > 0) {
            new LifestealEnchant().onHit(attacker, level);
        }
    }
}
