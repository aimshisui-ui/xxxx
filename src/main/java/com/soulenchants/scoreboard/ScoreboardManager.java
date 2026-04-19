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

import java.text.DecimalFormat;
import java.util.*;

public class ScoreboardManager {

    private static final String TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ Soul Enchants ✦";
    private static final DecimalFormat FMT = new DecimalFormat("#,###");

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
        String sep = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                ";

        lines.add(sep);
        lines.add(ChatColor.RED + "❤ HP: " + ChatColor.WHITE + (int) p.getHealth()
                + ChatColor.GRAY + "/" + ChatColor.WHITE + (int) p.getMaxHealth());
        lines.add(ChatColor.GOLD + "✦ Souls: " + ChatColor.WHITE + FMT.format(plugin.getSoulManager().get(p)));

        // Cooldowns section
        Map<String, Long> cds = plugin.getCooldownManager().getActiveCooldowns(id);
        if (!cds.isEmpty()) {
            lines.add(sep + " ");
            lines.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "Cooldowns");
            for (Map.Entry<String, Long> e : cds.entrySet()) {
                if (lines.size() >= 14) break;
                long secs = (e.getValue() + 999) / 1000;
                lines.add(ChatColor.GRAY + e.getKey() + ": " + ChatColor.RED + secs + "s");
            }
        }

        // Boss section
        Veilweaver vw = plugin.getVeilweaverManager().getActive();
        IronGolemBoss ig = plugin.getIronGolemManager().getActive();
        if (vw != null && !vw.getEntity().isDead()) {
            lines.add(sep + "  ");
            String phaseTag = vw.getPhase() == Veilweaver.Phase.ONE ? "I"
                    : vw.getPhase() == Veilweaver.Phase.TWO ? "II" : "III";
            lines.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Veilweaver "
                    + ChatColor.LIGHT_PURPLE + "[" + phaseTag + "]");
            lines.add(ChatColor.RED + "HP: " + ChatColor.WHITE
                    + (int) vw.getEntity().getHealth() + ChatColor.GRAY + "/"
                    + ChatColor.WHITE + (int) vw.getEntity().getMaxHealth());
        }
        if (ig != null && !ig.getEntity().isDead()) {
            lines.add(sep + "   ");
            String phaseTag = ig.getEntity().getHealth() / ig.getEntity().getMaxHealth() > 0.5 ? "I" : "II";
            lines.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Ironheart "
                    + ChatColor.YELLOW + "[" + phaseTag + "]");
            lines.add(ChatColor.RED + "HP: " + ChatColor.WHITE
                    + (int) ig.getEntity().getHealth() + ChatColor.GRAY + "/"
                    + ChatColor.WHITE + (int) ig.getEntity().getMaxHealth());
        }

        lines.add(sep + "    ");
        if (lines.size() > 15) lines = lines.subList(0, 15);
        return lines;
    }
}
