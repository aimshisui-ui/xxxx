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
 * Clarity at III grants full proc-time immunity to POISON + BLINDNESS.
 * Clarity I and II use the chance-based slow-tick strip over in
 * BerserkTickTask — the effect visibly ticks for a moment before it
 * gets cleared, so lower tiers feel materially weaker than III.
 *
 * Three layers of defense for Clarity III:
 *   1. DebuffImmunity.clarityBlocks() at enchant proc time (checked
 *      before the proc block fires — blocks Venom et al up-front).
 *   2. PotionSplashEvent hook (this file) — witch potions, splash pots,
 *      etc. get intensity set to 0 on Clarity III wearers before they
 *      hit so the bar never flashes.
 *   3. Fast 2-tick scrubber — catches anything from third-party plugins
 *      or NMS that bypassed (1) and (2). Belt-and-suspenders.
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
