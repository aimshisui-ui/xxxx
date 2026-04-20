package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Cinematic on-killing-blow effect for any boss.
 *
 * Fires a 3-phase animation centered on the corpse:
 *   t=0     killer gets a giant title + Strength I + Regen II + Resistance I (5s)
 *           lightning effect at corpse, low rumble for everyone in 80b
 *   t=2..32 expanding particle dome (WITCH_MAGIC + LARGE_SMOKE) so the moment is
 *           visible from across the arena — three nested rings, one per 10t
 *   t=10    +souls / +guild-points popup as a chat-line + SUBTITLE swap
 *   t=40    final firework-blast sound + END_ROD sparkle column
 *
 * All scheduling uses Bukkit task queue; safe to call from a death-event
 * handler. accent is the boss-color theming so each boss feels distinct.
 */
public final class BossKillRewardFX {

    private BossKillRewardFX() {}

    public static void play(SoulEnchants plugin,
                            Player killer,
                            Player topDamager,
                            String bossDisplayName,
                            ChatColor accent,
                            long soulsKiller,
                            long soulsTop,
                            long guildPoints,
                            Location corpseAt) {
        if (corpseAt == null || corpseAt.getWorld() == null) return;
        final org.bukkit.World w = corpseAt.getWorld();

        // Frame 0 — killer-only big moment
        if (killer != null && killer.isOnline()) {
            try {
                killer.sendTitle(accent + "" + ChatColor.BOLD + "✦ KILLING BLOW ✦",
                                 ChatColor.WHITE + bossDisplayName);
            } catch (Throwable ignored) {}
            killer.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 0, true, false), true);
            killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    100, 1, true, false), true);
            killer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 0, true, false), true);
            killer.playSound(killer.getLocation(), Sound.ENDERDRAGON_DEATH, 0.7f, 1.2f);
        }

        // Lightning effect (no damage) at corpse for the whole arena
        try { w.strikeLightningEffect(corpseAt); } catch (Throwable ignored) {}

        // Low rumble for everyone within 80b
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(corpseAt) <= 80 * 80) {
                p.playSound(p.getLocation(), Sound.WITHER_DEATH, 0.4f, 0.6f);
            }
        }

        // Frames 2..32 — three nested expanding rings, one ring spawned per 10t
        new BukkitRunnable() {
            int wave = 0;
            @Override public void run() {
                if (wave++ >= 3) { cancel(); return; }
                double radius = 1.5 + wave * 1.8;
                int points = 32 + wave * 8;
                for (int i = 0; i < points; i++) {
                    double a = (Math.PI * 2 * i) / points;
                    Location ring = corpseAt.clone().add(Math.cos(a) * radius, 0.4, Math.sin(a) * radius);
                    w.playEffect(ring, Effect.WITCH_MAGIC, 0);
                    if (wave == 2) w.playEffect(ring.clone().add(0, 1.2, 0), Effect.LARGE_SMOKE, 0);
                }
            }
        }.runTaskTimer(plugin, 2L, 10L);

        // Frame 10 — reward popup (chat + subtitle swap)
        new BukkitRunnable() {
            @Override public void run() {
                StringBuilder sub = new StringBuilder();
                if (soulsKiller > 0) sub.append(ChatColor.LIGHT_PURPLE).append("+").append(soulsKiller).append(" souls");
                if (guildPoints > 0) {
                    if (sub.length() > 0) sub.append(ChatColor.GRAY).append(" · ");
                    sub.append(ChatColor.AQUA).append("+").append(guildPoints).append(" guild");
                }
                if (killer != null && killer.isOnline() && sub.length() > 0) {
                    try { killer.sendTitle("", sub.toString()); } catch (Throwable ignored) {}
                }
                if (topDamager != null && topDamager != killer && topDamager.isOnline() && soulsTop > 0) {
                    try {
                        topDamager.sendTitle(accent + "" + ChatColor.BOLD + "✦ TOP DAMAGE ✦",
                                ChatColor.LIGHT_PURPLE + "+" + soulsTop + " souls");
                    } catch (Throwable ignored) {}
                }
            }
        }.runTaskLater(plugin, 10L);

        // Frame 40 — closing firework blast + sparkle column
        new BukkitRunnable() {
            @Override public void run() {
                w.playSound(corpseAt, Sound.FIREWORK_BLAST, 1.4f, 0.8f);
                w.playSound(corpseAt, Sound.FIREWORK_LARGE_BLAST, 1.0f, 1.2f);
                for (int dy = 0; dy < 8; dy++) {
                    Location s = corpseAt.clone().add(0, dy * 0.5, 0);
                    w.playEffect(s, Effect.WITCH_MAGIC, 0);
                    w.playEffect(s.clone().add(0.3, 0, 0), Effect.WITCH_MAGIC, 0);
                    w.playEffect(s.clone().add(-0.3, 0, 0), Effect.WITCH_MAGIC, 0);
                }
            }
        }.runTaskLater(plugin, 40L);
    }
}
