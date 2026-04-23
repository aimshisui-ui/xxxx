package com.soulenchants.masks;

import com.soulenchants.SoulEnchants;
import com.soulenchants.util.ActionBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the three time-based mask abilities: Stalker, Phantom Dash, and
 * Soul Harvest's cooldown display. Everything else (Ironwill, Frostguard,
 * stat multipliers, immunities) is pulled inline by CombatListener /
 * BerserkTickTask via MaskEffects.
 *
 * Runs at 5 Hz (every 4 ticks). State is per-player and cleared on logout.
 */
public final class MaskAbilityTask implements Listener {

    private final SoulEnchants plugin;
    private BukkitRunnable task;

    // Stalker
    private final Map<UUID, Long> stalkerSneakStart = new HashMap<>();
    private final Map<UUID, Location> stalkerLastPos = new HashMap<>();
    private final Map<UUID, Boolean> stalkerActive = new HashMap<>();

    // Phantom Dash — tracks last activation timestamp (for 10s CD)
    private final Map<UUID, Long> phantomLast = new HashMap<>();

    // Soul Harvest — per-player cooldown stamp
    private final Map<UUID, Long> soulHarvestCd = new HashMap<>();

    private static final long STALKER_ARM_MS  = 2000L;
    private static final long PHANTOM_CD_MS   = 10_000L;
    private static final long PHANTOM_DUR_MS  = 2_000L;
    private static final long SOULHARVEST_CD  = 30_000L;

    public MaskAbilityTask(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() {
        task = new BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                for (Player p : plugin.getServer().getOnlinePlayers()) tick(p, now);
            }
        };
        task.runTaskTimer(plugin, 20L, 4L);
    }

    public void stop() { if (task != null) try { task.cancel(); } catch (Throwable ignored) {} }

    private void tick(Player p, long now) {
        Mask.MaskPower pw = MaskEffects.powerOf(p);
        String ability = pw == null ? null : pw.customAbility;
        UUID id = p.getUniqueId();

        // Clear Stalker state when the ability isn't equipped
        if (!"stalker".equals(ability)) {
            clearStalker(id);
        }

        if ("stalker".equals(ability)) {
            tickStalker(p, now, id);
        } else if ("phantomdash".equals(ability)) {
            tickPhantom(p, now, id);
        }
    }

    // ──────────────── Stalker ────────────────
    // Conditions: sneak + not moving (within 0.05 blocks of last pos) for 2s.
    // While held: grant Invisibility; clear on movement or attack (see event).
    private void tickStalker(Player p, long now, UUID id) {
        Location cur = p.getLocation();
        Location last = stalkerLastPos.get(id);
        boolean still = last != null && last.getWorld() == cur.getWorld()
                && last.distanceSquared(cur) < 0.0025;
        stalkerLastPos.put(id, cur);

        if (!p.isSneaking() || !still) {
            if (Boolean.TRUE.equals(stalkerActive.get(id))) cancelStalkerInvis(p);
            stalkerSneakStart.remove(id);
            stalkerActive.put(id, Boolean.FALSE);
            return;
        }
        Long started = stalkerSneakStart.get(id);
        if (started == null) { stalkerSneakStart.put(id, now); return; }
        if (now - started < STALKER_ARM_MS) {
            long remaining = STALKER_ARM_MS - (now - started);
            ActionBar.send(p, "§8§l🞄 §7§oArming Stalker · §f" + ((remaining + 999) / 1000) + "s§7...");
            return;
        }
        // Armed — apply Invisibility + action bar flag. 60-tick refresh so
        // the effect never visibly blips.
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, true, false), true);
        stalkerActive.put(id, Boolean.TRUE);
        ActionBar.send(p, "§8§l🞄 §f§lSTALKING §8§l🞄");
    }

    private void cancelStalkerInvis(Player p) {
        if (p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            // Only strip if duration looks like ours (short-refresh, amp 0)
            for (PotionEffect pe : p.getActivePotionEffects()) {
                if (pe.getType().equals(PotionEffectType.INVISIBILITY)
                        && pe.getAmplifier() == 0 && pe.getDuration() <= 80) {
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);
                    break;
                }
            }
        }
    }

    private void clearStalker(UUID id) {
        stalkerSneakStart.remove(id);
        stalkerActive.remove(id);
        stalkerLastPos.remove(id);
    }

    /** Breaks stalker on attack output. */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        UUID id = p.getUniqueId();
        if (Boolean.TRUE.equals(stalkerActive.get(id))) {
            cancelStalkerInvis(p);
            stalkerActive.put(id, Boolean.FALSE);
            stalkerSneakStart.remove(id);
        }
    }

    // ──────────────── Phantom Dash ────────────────
    private void tickPhantom(Player p, long now, UUID id) {
        if (!p.isSprinting()) return;
        Long last = phantomLast.get(id);
        if (last != null && now - last < PHANTOM_CD_MS) return;
        phantomLast.put(id, now);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                (int)(PHANTOM_DUR_MS / 50L), 0, true, false), true);
        ActionBar.send(p, "§5§l✦ §f§lPHANTOM DASH §5§l✦");
    }

    // ──────────────── Soul Harvest ────────────────
    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        Mask.MaskPower pw = MaskEffects.powerOf(killer);
        if (pw == null || !"soulharvest".equals(pw.customAbility)) return;
        long now = System.currentTimeMillis();
        Long last = soulHarvestCd.get(killer.getUniqueId());
        if (last != null && now - last < SOULHARVEST_CD) return;
        soulHarvestCd.put(killer.getUniqueId(), now);
        double heal = killer.getMaxHealth() * 0.20;
        killer.setHealth(Math.min(killer.getMaxHealth(), killer.getHealth() + heal));
        ActionBar.send(killer, "§6§l✦ §f§lSOUL HARVEST §6§l✦ §7+" + (int) Math.ceil(heal) + " HP");
    }
}
