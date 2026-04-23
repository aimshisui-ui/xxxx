package com.soulenchants.lunar;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.EliteBossHooks;
import com.soulenchants.bosses.IronGolemBoss;
import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.mobs.CustomMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Renders a multi-line Apollo hologram over every active boss — name, phase
 * tag, and a 20-segment HP bar. Refreshes at 5 Hz so the bar feels live
 * without spamming packets. Clears cleanly on boss death / despawn.
 *
 * No-op when Apollo isn't available; non-Lunar players fall back to the
 * vanilla custom-name HP nametag that CustomMob.refreshHpBar() already
 * maintains.
 */
public final class BossHologramTask {

    private static final String ID_PREFIX = "se_boss_hp_";

    private final SoulEnchants plugin;
    private BukkitRunnable task;
    /** Boss UUIDs that currently have a hologram shown. */
    private final Set<UUID> shown = new HashSet<>();

    public BossHologramTask(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() {
        task = new BukkitRunnable() {
            @Override public void run() {
                if (!LunarBridge.isAvailable()) return;
                tick();
            }
        };
        task.runTaskTimer(plugin, 40L, 4L);  // 5 Hz
    }

    public void stop() {
        if (task != null) try { task.cancel(); } catch (Throwable ignored) {}
        for (UUID id : shown) LunarBridge.removeHologram(ID_PREFIX + id);
        shown.clear();
    }

    private void tick() {
        Set<UUID> aliveNow = new HashSet<>();

        // Veilweaver
        if (plugin.getVeilweaverManager() != null) {
            Veilweaver vw = plugin.getVeilweaverManager().getActive();
            if (vw != null && vw.getEntity() != null && !vw.getEntity().isDead()) {
                String phase = vw.getPhase() == Veilweaver.Phase.ONE ? "I"
                        : vw.getPhase() == Veilweaver.Phase.TWO ? "II" : "III";
                renderBar(vw.getEntity(),
                        "§5§l✦ The Veilweaver §d[" + phase + "]",
                        "§d", 0xD946EF);
                aliveNow.add(vw.getEntity().getUniqueId());
            }
        }
        // Ironheart Colossus
        if (plugin.getIronGolemManager() != null) {
            IronGolemBoss ig = plugin.getIronGolemManager().getActive();
            if (ig != null && ig.getEntity() != null && !ig.getEntity().isDead()) {
                String phase = ig.getEntity().getHealth() / ig.getEntity().getMaxHealth() > 0.5 ? "I" : "II";
                renderBar(ig.getEntity(),
                        "§6§l✦ Ironheart Colossus §e[" + phase + "]",
                        "§e", 0xFFC107);
                aliveNow.add(ig.getEntity().getUniqueId());
            }
        }
        // Modock
        if (plugin.getModockManager() != null && plugin.getModockManager().getActive() != null) {
            try {
                LivingEntity me = plugin.getModockManager().getActive().getEntity();
                if (me != null && !me.isDead()) {
                    renderBar(me, "§b§l✦ Modock §3[Atlantis]", "§b", 0x00BCD4);
                    aliveNow.add(me.getUniqueId());
                }
            } catch (Throwable ignored) {}
        }
        // Elite CustomMob bosses
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity ent : w.getEntities()) {
                if (!(ent instanceof LivingEntity)) continue;
                LivingEntity le = (LivingEntity) ent;
                if (le.isDead()) continue;
                String id = CustomMob.idOf(le);
                EliteBossHooks.Spec spec = EliteBossHooks.specOf(id);
                if (spec == null) continue;
                int phase = EliteBossHooks.phaseByEntity.getOrDefault(le.getUniqueId(), 1);
                String ph = phase == 1 ? "I" : phase == 2 ? "II" : "III";
                renderBar(le,
                        spec.color + "§l✦ " + spec.display + " §7[" + ph + "]",
                        String.valueOf(spec.color), spec.waypointRgb);
                aliveNow.add(le.getUniqueId());
            }
        }

        // Clear any holograms whose boss is no longer alive.
        for (UUID prev : new ArrayList<>(shown)) {
            if (!aliveNow.contains(prev)) {
                LunarBridge.removeHologram(ID_PREFIX + prev);
                shown.remove(prev);
            }
        }
    }

    /** Draw a 3-line hologram above `le` — label, HP bar, numeric HP. */
    private void renderBar(LivingEntity le, String label, String color, int rgbUnused) {
        double hp = Math.max(0, le.getHealth());
        double max = Math.max(1, le.getMaxHealth());
        double pct = hp / max;
        int filled = (int) Math.round(pct * 20);
        StringBuilder bar = new StringBuilder();
        bar.append(color);
        for (int i = 0; i < filled; i++) bar.append('▉');
        bar.append("§8");
        for (int i = filled; i < 20; i++) bar.append('▉');

        List<String> lines = new ArrayList<>(3);
        lines.add(label);
        lines.add(bar.toString());
        lines.add("§c❤ §f" + (int) Math.ceil(hp) + "§7/§f" + (int) Math.ceil(max)
                + "  §8(" + (int)(pct * 100) + "%)");

        Location loc = le.getLocation().clone().add(0, le.getEyeHeight() + 0.6, 0);
        if (LunarBridge.displayHologram(ID_PREFIX + le.getUniqueId(), loc, lines)) {
            shown.add(le.getUniqueId());
        }
    }
}
