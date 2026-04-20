package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Clarity III safety net.
 *
 * The main BerserkTickTask runs every 20 ticks — leaves a full second of
 * visible Poison/Blindness before the strip. This task fires every 2 ticks
 * (0.1s) so the effect never visibly applies on a Clarity III wearer. We
 * also cancel Poison/Blindness splashes from PotionSplashEvent before they
 * even hit, which is cleaner for the common case (witch potions, splash pots).
 *
 * Clarity I/II still use the original chance-based slow-tick strip.
 */
public class ClarityTask implements Listener {

    private final SoulEnchants plugin;
    private BukkitRunnable task;

    public ClarityTask(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() {
        task = new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!hasClarity3(p)) continue;
                    if (p.hasPotionEffect(PotionEffectType.POISON))    p.removePotionEffect(PotionEffectType.POISON);
                    if (p.hasPotionEffect(PotionEffectType.BLINDNESS)) p.removePotionEffect(PotionEffectType.BLINDNESS);
                }
            }
        };
        task.runTaskTimer(plugin, 2L, 2L);
    }

    public void stop() { if (task != null) try { task.cancel(); } catch (Exception ignored) {} }

    /** Cancel POISON/BLINDNESS splashes BEFORE they apply to Clarity III wearers. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent e) {
        boolean carriesPoisonOrBlindness = false;
        for (org.bukkit.potion.PotionEffect pe : e.getPotion().getEffects()) {
            PotionEffectType t = pe.getType();
            if (t == PotionEffectType.POISON || t == PotionEffectType.BLINDNESS) {
                carriesPoisonOrBlindness = true;
                break;
            }
        }
        if (!carriesPoisonOrBlindness) return;
        for (LivingEntity victim : e.getAffectedEntities()) {
            if (!(victim instanceof Player)) continue;
            if (hasClarity3((Player) victim)) {
                // Set intensity to 0 — the effect simply doesn't apply to this player
                e.setIntensity(victim, 0.0);
            }
        }
    }

    private boolean hasClarity3(Player p) {
        ItemStack helmet = p.getInventory().getHelmet();
        if (helmet == null) return false;
        return ItemUtil.getLevel(helmet, "clarity") >= 3;
    }
}
