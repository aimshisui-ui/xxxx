package com.soulenchants.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player damage-pipeline trace ring buffer. When an admin enables
 * tracing via {@code /ce dmg-trace <player>}, CombatListener pushes an
 * entry here on every outgoing swing. The player's chat then receives a
 * structured breakdown so you can see WHY a hit did the damage it did.
 *
 * Tracing auto-expires after 60 seconds to keep console clean. The
 * ring buffer caps at 50 entries per player so long fights don't OOM.
 */
public final class DmgTrace {

    private static final int  MAX_ENTRIES   = 50;
    private static final long AUTO_EXPIRE_MS = 60_000L;

    /** Per-player state: who's tracing, when they started, their ring buffer. */
    private static final class Session {
        final long startedAt = System.currentTimeMillis();
        final Deque<String> ring = new ArrayDeque<>();
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private DmgTrace() {}

    /** Toggle tracing for a target player. Returns true if tracing is now on. */
    public static boolean toggle(Player target) {
        UUID id = target.getUniqueId();
        if (SESSIONS.remove(id) != null) return false;
        SESSIONS.put(id, new Session());
        // Auto-expire after 60s
        Bukkit.getScheduler().runTaskLaterAsynchronously(
                Bukkit.getPluginManager().getPlugin("SoulEnchants"),
                () -> SESSIONS.remove(id, SESSIONS.get(id)),
                AUTO_EXPIRE_MS / 50L);
        return true;
    }

    public static boolean isTracing(UUID id) {
        Session s = SESSIONS.get(id);
        if (s == null) return false;
        if (System.currentTimeMillis() - s.startedAt > AUTO_EXPIRE_MS) {
            SESSIONS.remove(id);
            return false;
        }
        return true;
    }

    /** Push an entry for the attacker's ring. Called from CombatListener
     *  after the damage pipeline has computed final damage. */
    public static void record(UUID attackerId, DmgEvent event) {
        if (!isTracing(attackerId)) return;
        Session s = SESSIONS.get(attackerId);
        if (s == null) return;
        synchronized (s.ring) {
            if (s.ring.size() >= MAX_ENTRIES) s.ring.pollFirst();
            s.ring.addLast(event.format());
        }
        // Live stream to the player's chat for real-time feedback.
        Player p = Bukkit.getPlayer(attackerId);
        if (p != null && p.isOnline()) p.sendMessage(event.format());
    }

    /** Dump the full ring buffer to the requester. */
    public static void dump(UUID attackerId, Player requester) {
        Session s = SESSIONS.get(attackerId);
        if (s == null) { requester.sendMessage("§7[dmg-trace] No active trace for that player."); return; }
        requester.sendMessage("§8──────── §edmg-trace §8(last "
                + s.ring.size() + " hits) ────────");
        synchronized (s.ring) {
            for (String entry : s.ring) requester.sendMessage(entry);
        }
    }

    /** Builder for a single trace entry — accumulates named fields then
     *  renders to a single-line chat string. */
    public static final class DmgEvent {
        private final String attacker;
        private final String victim;
        private final double base;
        private double armorMult = 1.0;
        private double offBonusSum = 0;
        private double cappedBonus = 0;
        private double specialMult = 1.0;
        private double flatAdd = 0;
        private double maskMult = 1.0;
        private double finalDmg = 0;
        private final Map<String, Double> bonusBreakdown = new LinkedHashMap<>();

        public DmgEvent(String attacker, String victim, double base) {
            this.attacker = attacker; this.victim = victim; this.base = base;
        }
        public DmgEvent offBonus(String src, double amt) { bonusBreakdown.put(src, amt); offBonusSum += amt; return this; }
        public DmgEvent cappedBonus(double v) { this.cappedBonus = v; return this; }
        public DmgEvent specialMult(double v) { this.specialMult = v; return this; }
        public DmgEvent flatAdd(double v)     { this.flatAdd = v; return this; }
        public DmgEvent maskMult(double v)    { this.maskMult = v; return this; }
        public DmgEvent finalDmg(double v)    { this.finalDmg = v; return this; }
        public DmgEvent armorMult(double v)   { this.armorMult = v; return this; }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("§8§l│ §b").append(attacker).append(" §7→ §c").append(victim)
              .append(" §8│ §fbase §7=§e").append(fmt(base))
              .append(" §8· §fbonus §7=§e").append(fmt(cappedBonus)).append("§8(").append(fmt(offBonusSum)).append(" uncapped)")
              .append(" §8· §fmult §7=§e×").append(fmt(specialMult))
              .append(" §8· §fflat §7=§e+").append(fmt(flatAdd))
              .append(" §8· §fmask §7=§e×").append(fmt(maskMult))
              .append(" §8→ §aFINAL §e").append(fmt(finalDmg));
            if (!bonusBreakdown.isEmpty()) {
                sb.append("\n    §8breakdown:");
                for (Map.Entry<String, Double> e : bonusBreakdown.entrySet())
                    sb.append(" §7").append(e.getKey()).append("§8=§e").append(fmt(e.getValue()));
            }
            return sb.toString();
        }

        private static String fmt(double d) { return String.format("%.2f", d); }
    }
}
