package com.soulenchants.scoreboard;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.IronGolemBoss;
import com.soulenchants.bosses.Veilweaver;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScoreboardManager {

    private static final String TITLE = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Fabled"
            + ChatColor.WHITE + ChatColor.BOLD + "MC";
    private static final DecimalFormat FMT = new DecimalFormat("#,###");
    private static final DecimalFormat KDR = new DecimalFormat("0.00");
    private static final DateFormat DATE_FMT = new SimpleDateFormat("MM/dd");

    private final SoulEnchants plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitRunnable task;

    public ScoreboardManager(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() {
        task = new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) update(p);
            }
        };
        task.runTaskTimer(plugin, 20L, 10L);
    }

    public void stop() { if (task != null) try { task.cancel(); } catch (Exception ignored) {} }

    public void update(Player p) {
        List<String> lines = buildLines(p);

        Scoreboard board = boards.computeIfAbsent(p.getUniqueId(),
                k -> Bukkit.getScoreboardManager().getNewScoreboard());

        // Unregister old objective and rebuild cleanly
        Objective old = board.getObjective("se_main");
        if (old != null) old.unregister();
        Objective obj = board.registerNewObjective("se_main", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(TITLE);

        int score = lines.size();
        Set<String> used = new HashSet<>();
        for (String line : lines) {
            String entry = makeUniqueEntry(line, used);
            used.add(entry);
            obj.getScore(entry).setScore(score--);
        }

        p.setScoreboard(board);
    }

    /** Ensures entry is unique and under 40 chars (1.8 sidebar limit). */
    private String makeUniqueEntry(String line, Set<String> existing) {
        String entry = line.length() > 40 ? line.substring(0, 40) : line;
        if (!existing.contains(entry)) return entry;
        for (int i = 0; i < 32; i++) {
            String prefix = ChatColor.values()[i % 16].toString();
            String candidate = prefix + line;
            if (candidate.length() > 40) candidate = candidate.substring(0, 40);
            if (!existing.contains(candidate)) return candidate;
        }
        return line + UUID.randomUUID().toString().substring(0, 4);
    }

    private List<String> buildLines(Player p) {
        List<String> lines = new ArrayList<>();
        UUID id = p.getUniqueId();
        // Colored divider — different shades per section so they don't visually merge
        String divPurple = ChatColor.DARK_PURPLE + "" + ChatColor.STRIKETHROUGH + "                       ";
        String divGold   = ChatColor.GOLD        + "" + ChatColor.STRIKETHROUGH + "                       ";
        String divBlue   = ChatColor.AQUA        + "" + ChatColor.STRIKETHROUGH + "                       ";
        String divRed    = ChatColor.DARK_RED    + "" + ChatColor.STRIKETHROUGH + "                       ";
        String divGray   = ChatColor.DARK_GRAY   + "" + ChatColor.STRIKETHROUGH + "                       ";

        // ── HEADER / VITALS ─────────────
        lines.add(divPurple);
        int hp = (int) Math.ceil(p.getHealth());
        int maxHp = (int) Math.ceil(p.getMaxHealth());
        ChatColor hpColor = hp > maxHp * 0.66 ? ChatColor.GREEN
                          : hp > maxHp * 0.33 ? ChatColor.YELLOW : ChatColor.RED;
        lines.add(ChatColor.RED + "❤ Health  " + hpColor + hp + ChatColor.DARK_GRAY + " / " + ChatColor.GRAY + maxHp);
        lines.add(ChatColor.GOLD + "✦ Souls   " + ChatColor.YELLOW + FMT.format(plugin.getSoulManager().get(p)));

        // Tier line (always show — grounds the player in their progression)
        com.soulenchants.currency.SoulTier tier = plugin.getSoulManager().getTier(p);
        lines.add(ChatColor.LIGHT_PURPLE + "✦ Tier    " + tier.getColor() + tier.getLabel());

        // ── PVP STATS ──────────────────
        com.soulenchants.scoreboard.PvPStats stats = plugin.getPvPStats();
        if (stats != null) {
            int kills = stats.getKills(p);
            int deaths = stats.getDeaths(p);
            double kdr = stats.getKDR(p);
            ChatColor kdrColor = kdr >= 1.0 ? ChatColor.GREEN : ChatColor.RED;
            lines.add(ChatColor.GREEN + "⚔ K/D     " + ChatColor.WHITE + kills
                    + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + deaths
                    + " " + kdrColor + "(" + KDR.format(kdr) + ")");
        }
        lines.add(ChatColor.AQUA + "✉ Ping    " + ChatColor.WHITE + getPing(p) + ChatColor.GRAY + "ms"
                + ChatColor.DARK_GRAY + "  " + ChatColor.GRAY + DATE_FMT.format(new Date()));

        // ── COOLDOWNS ───────────────────
        Map<String, Long> cds = plugin.getCooldownManager().getActiveCooldowns(id);
        if (!cds.isEmpty()) {
            lines.add(divBlue);
            lines.add(ChatColor.AQUA + "" + ChatColor.BOLD + "▎ Cooldowns");
            int shown = 0;
            for (Map.Entry<String, Long> e : cds.entrySet()) {
                if (shown++ >= 3) break;
                if (lines.size() >= 13) break;
                long secs = (e.getValue() + 999) / 1000;
                String name = e.getKey();
                if (name.length() > 12) name = name.substring(0, 12);
                lines.add(ChatColor.GRAY + " " + name + " " + ChatColor.RED + secs + "s");
            }
        }

        // ── BOSS SECTION ────────────────
        Veilweaver vw = plugin.getVeilweaverManager().getActive();
        IronGolemBoss ig = plugin.getIronGolemManager().getActive();
        if ((vw != null && !vw.getEntity().isDead()) || (ig != null && !ig.getEntity().isDead())) {
            lines.add(divRed);
            lines.add(ChatColor.RED + "" + ChatColor.BOLD + "▎ Boss");
        }
        if (vw != null && !vw.getEntity().isDead()) {
            String phaseTag = vw.getPhase() == Veilweaver.Phase.ONE ? "I"
                    : vw.getPhase() == Veilweaver.Phase.TWO ? "II" : "III";
            lines.add(ChatColor.DARK_PURPLE + " Veilweaver " + ChatColor.LIGHT_PURPLE + "[" + phaseTag + "]");
            lines.add(ChatColor.RED + " ❤ " + ChatColor.WHITE
                    + (int) vw.getEntity().getHealth() + ChatColor.DARK_GRAY + "/"
                    + ChatColor.GRAY + (int) vw.getEntity().getMaxHealth());
        }
        if (ig != null && !ig.getEntity().isDead()) {
            String phaseTag = ig.getEntity().getHealth() / ig.getEntity().getMaxHealth() > 0.5 ? "I" : "II";
            lines.add(ChatColor.GOLD + " Ironheart " + ChatColor.YELLOW + "[" + phaseTag + "]");
            lines.add(ChatColor.RED + " ❤ " + ChatColor.WHITE
                    + (int) ig.getEntity().getHealth() + ChatColor.DARK_GRAY + "/"
                    + ChatColor.GRAY + (int) ig.getEntity().getMaxHealth());
        }

        // ── RIFT SECTION ────────────────
        com.soulenchants.rifts.VoidRiftManager rifts = plugin.getVoidRiftManager();
        if (rifts != null && rifts.isActive()) {
            lines.add(divGold);
            lines.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "▎ ✦ Void Rift");
            if (rifts.getState() == com.soulenchants.rifts.VoidRiftManager.State.PORTAL_OPEN) {
                lines.add(ChatColor.YELLOW + " Portal open");
                lines.add(ChatColor.GRAY  + " " + rifts.shortStatus());
            } else {
                lines.add(ChatColor.LIGHT_PURPLE + " " + rifts.timerLabel()
                        + ChatColor.GRAY + " left");
                lines.add(ChatColor.RED + " ☠ " + ChatColor.WHITE + rifts.threatsRemaining()
                        + ChatColor.GRAY + " threats");
                lines.add(ChatColor.AQUA + " ⚔ " + ChatColor.WHITE + rifts.participantCount()
                        + ChatColor.GRAY + " inside");
            }
        }

        // Footer
        lines.add(divGray);
        if (lines.size() > 15) lines = lines.subList(0, 15);
        return lines;
    }

    /** Reflective ping access — works on Spigot 1.8.x without a hard import. */
    private int getPing(Player p) {
        try {
            Object handle = p.getClass().getMethod("getHandle").invoke(p);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (Throwable t) {
            return 0;
        }
    }
}
