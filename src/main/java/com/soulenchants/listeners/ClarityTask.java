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
 * Clarity safety net.
 *
 * v1.1 spec change: Clarity at ANY level grants hard immunity to POISON
 * and BLINDNESS. No more "chance to strip" slow-tick model — the effect
 * simply never applies. Three layers of defense:
 *
 *   1. DebuffImmunity.clarityBlocks() — enchant procs check this before
 *      applying their debuff (see CombatListener / BossDamage / etc).
 *   2. PotionSplashEvent hook — witch potions, splash pots, etc. get
 *      their intensity set to 0 on Clarity wearers before they land.
 *   3. Fast 2-tick scrubber — any effect that slipped past (1) and (2),
 *      from third-party plugins or NMS, gets stripped within 0.1s.
 *      Belt-and-suspenders.
 */
public class ClarityTask implements Listener {

    private final SoulEnchants plugin;
    private BukkitRunnable task;

    public ClarityTask(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() {
        task = new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!hasClarity(p)) continue;
                    if (p.hasPotionEffect(PotionEffectType.POISON))    p.removePotionEffect(PotionEffectType.POISON);
                    if (p.hasPotionEffect(PotionEffectType.BLINDNESS)) p.removePotionEffect(PotionEffectType.BLINDNESS);
                }
            }
        };
        task.runTaskTimer(plugin, 2L, 2L);
    }

    public void stop() { if (task != null) try { task.cancel(); } catch (Exception ignored) {} }

    /** Cancel POISON/BLINDNESS splashes BEFORE they apply to any Clarity wearer. */
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
            if (hasClarity((Player) victim)) {
                e.setIntensity(victim, 0.0);
            }
        }
    }

    private boolean hasClarity(Player p) {
        ItemStack helmet = p.getInventory().getHelmet();
        if (helmet == null) return false;
        return ItemUtil.getLevel(helmet, "clarity") >= 1;
    }
}
