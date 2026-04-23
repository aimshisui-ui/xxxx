package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.mobs.CustomMob;
import com.soulenchants.mobs.MobRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Central integration point for CustomMob-based "elite" bosses (Broodmother,
 * Wurm-Lord, Choirmaster). Mirrors the lifecycle that Veilweaver / Iron
 * Golem Colossus already get:
 *
 *   SPAWN   → cinematic chat banner (rises! / coordinates / flavor quote)
 *   DAMAGE  → HP-threshold phase-transition banners (Phase II at 66%, Phase III at 33%)
 *   WAYPOINT → Apollo waypoint while alive, cleared on death
 *   SCOREBOARD → shows a name+phase line (HP bar stays above their head only)
 *   DEATH   → guild-point awards by contributed damage + top-damage bonus
 *
 * Per-boss config — flavor quote, phase scripts, waypoint colour, guild
 * base points, scoreboard abbreviation — all lives in the SPECS map below.
 */
public final class EliteBossHooks implements Listener {

    // ──────────────────────── Spec ────────────────────────

    public static final class Spec {
        public final String id;
        public final String display;
        public final ChatColor color;
        public final String flavorQuote;
        public final int waypointRgb;
        public final String scoreboardName;   // sidebar line
        public final String scoreboardTag;    // the [Elite] / [II] bracket
        public final long   guildBasePoints;
        /** Per-phase messages, index 0 = Phase II (at 66%), 1 = Phase III (at 33%). */
        public final String[][] phases;
        Spec(String id, String display, ChatColor color, String flavor, int rgb,
             String scoreboardName, String scoreboardTag, long guildBase, String[][] phases) {
            this.id = id; this.display = display; this.color = color;
            this.flavorQuote = flavor; this.waypointRgb = rgb;
            this.scoreboardName = scoreboardName; this.scoreboardTag = scoreboardTag;
            this.guildBasePoints = guildBase; this.phases = phases;
        }
    }

    /** Registry of every elite boss CustomMob that should get the full treatment. */
    public static final Map<String, Spec> SPECS = new LinkedHashMap<>();
    static {
        SPECS.put("broodmother", new Spec(
                "broodmother",
                "The Broodmother",
                ChatColor.DARK_GREEN,
                "\"The walls breathe for me. The floor drinks for me.\"",
                0x4E7F3E,
                "Broodmother",
                "Elite",
                320L,
                new String[][] {
                        {   // Phase II — 66%
                                "§2§l✦ BROODMOTHER STIRS — Phase II ✦",
                                "§a• §7Web Trap cadence tightens — §fevery 10-20s instead of 15-30s",
                                "§a• §7Venom cloud now applies §fPoison III",
                                "§a• §7Web-lurker summons are §fpermanent while she lives — kill them fast",
                                "§a§lShe remembers your footing now."
                        },
                        {   // Phase III — 33%
                                "§4§l✦ BROODMOTHER UNBOUND — Phase III ✦",
                                "§c• §7Every hit now triggers a §fmini-web-trap §7on the victim",
                                "§c• §7Venom cloud radius §fDOUBLED",
                                "§c• §7She will not stop until one of you is still.",
                                "§c§lYou're in the web now."
                        }
                }
        ));

        SPECS.put("wurm_lord", new Spec(
                "wurm_lord",
                "The Wurm-Lord",
                ChatColor.DARK_RED,
                "\"I was buried. You invited me back.\"",
                0xB13B2F,
                "Wurm-Lord",
                "Elite",
                380L,
                new String[][] {
                        {   // Phase II
                                "§4§l✦ WURM-LORD AWAKENED — Phase II ✦",
                                "§c• §7Burrow strikes now hit §ftwo players §7per cast",
                                "§c• §7Fire aura radius expands — §fstand in cover or burn",
                                "§c• §7Ground bursts chain — §fdon't cluster",
                                "§c§lThe earth remembers you."
                        },
                        {   // Phase III
                                "§4§l✦ WURM-LORD TITAN — Phase III ✦",
                                "§c• §7Meteor cadence halved — §fexpect overlapping drops",
                                "§c• §7Melee cycle accelerates — §fno time to heal in range",
                                "§c• §7Magma-cube spawns double on death",
                                "§c§lHe finishes what he started."
                        }
                }
        ));

        SPECS.put("choirmaster", new Spec(
                "choirmaster",
                "The Choirmaster",
                ChatColor.DARK_PURPLE,
                "\"Sing. Do not make me ask twice.\"",
                0x6B2F8A,
                "Choirmaster",
                "Elite",
                360L,
                new String[][] {
                        {   // Phase II
                                "§5§l✦ CHOIRMASTER LISTENS — Phase II ✦",
                                "§d• §7Soul Mark detonation threshold drops — §f6 stacks instead of 8",
                                "§d• §7Chain lightning bounce count §f+1",
                                "§d• §7Spectral monks respawn on any death within 8 blocks",
                                "§d§lThe hymn is louder."
                        },
                        {   // Phase III
                                "§5§l✦ CHOIRMASTER DIRGE — Phase III ✦",
                                "§d• §7Soul Mark detonation at §f4 stacks",
                                "§d• §7Every player in 10 blocks suffers §fWither II",
                                "§d• §7He doesn't need you to sing anymore.",
                                "§d§lYou sing for yourself now."
                        }
                }
        ));
    }

    public static boolean isElite(String id)       { return id != null && SPECS.containsKey(id); }
    public static Spec    specOf(String id)        { return SPECS.get(id); }
    public static Spec    specOf(LivingEntity le)  { return specOf(CustomMob.idOf(le)); }

    // ──────────────────── Per-entity state ────────────────────

    /** Phase state per entity UUID (1 = spawn, 2 = Phase II, 3 = Phase III). */
    public static final Map<UUID, Integer> phaseByEntity = new HashMap<>();
    /** Which entity UUIDs we've already announced on spawn (so rebuildResolved
     *  doesn't re-fire the banner every tick if state drifts). */
    public static final Set<UUID> announcedSpawn = new HashSet<>();
    /** Per-boss accumulated damage-by-player, for the guild-points award. */
    public static final Map<UUID, Map<UUID, Double>> damageMap = new HashMap<>();

    // ──────────────────── Static entry point ────────────────────

    private final SoulEnchants plugin;

    public EliteBossHooks(SoulEnchants plugin) { this.plugin = plugin; }

    /** Broadcast the "rises!" banner once per spawned entity. Called from
     *  MobListener.onSpawn / MobSpawner path. */
    public static void announceSpawn(LivingEntity boss) {
        if (boss == null) return;
        UUID id = boss.getUniqueId();
        if (announcedSpawn.contains(id)) return;
        Spec s = specOf(boss);
        if (s == null) return;
        announcedSpawn.add(id);
        phaseByEntity.put(id, 1);

        Location loc = boss.getLocation();
        String div = s.color + "" + ChatColor.STRIKETHROUGH + "                                          ";
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(div);
        Bukkit.broadcastMessage(s.color + "" + ChatColor.BOLD + "        ✦ " + s.display.toUpperCase() + " RISES ✦");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "  " + s.flavorQuote);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GRAY + "  Coordinates  " + ChatColor.WHITE
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                + ChatColor.DARK_GRAY + "  (" + loc.getWorld().getName() + ")");
        Bukkit.broadcastMessage(ChatColor.RED + "  ⚔ Bring company. Bring blades. Bring everything.");
        Bukkit.broadcastMessage(div);
        Bukkit.broadcastMessage("");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 0.8f, 0.5f);
        }
    }

    /** Watch HP thresholds and emit the Phase II / Phase III banners when
     *  the boss crosses them. Called from MobListener.onMobHurt path. */
    public static void onDamaged(LivingEntity boss) {
        if (boss == null || boss.isDead()) return;
        Spec s = specOf(boss);
        if (s == null) return;
        UUID id = boss.getUniqueId();
        int currentPhase = phaseByEntity.getOrDefault(id, 1);
        double hpPct = boss.getHealth() / boss.getMaxHealth();
        int targetPhase = hpPct <= 0.33 ? 3 : hpPct <= 0.66 ? 2 : 1;
        if (targetPhase <= currentPhase) return;
        phaseByEntity.put(id, targetPhase);
        String div = s.color + "" + ChatColor.STRIKETHROUGH + "                                          ";
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(div);
        for (String line : s.phases[targetPhase - 2]) {
            Bukkit.broadcastMessage("  " + line);
        }
        Bukkit.broadcastMessage(div);
        Bukkit.broadcastMessage("");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 0.7f, 1.2f);
        }
    }

    /** Record damage contribution so the death handler can award guild points. */
    public static void trackDamage(LivingEntity boss, Player attacker, double dmg) {
        if (boss == null || attacker == null || dmg <= 0) return;
        Spec s = specOf(boss);
        if (s == null) return;
        Map<UUID, Double> map = damageMap.computeIfAbsent(boss.getUniqueId(), k -> new HashMap<>());
        map.merge(attacker.getUniqueId(), dmg, Double::sum);
    }

    /** Death payload — guild points + banner. */
    public static void onDeath(SoulEnchants plugin, LivingEntity boss, Player killer) {
        Spec s = specOf(boss);
        if (s == null) return;
        UUID id = boss.getUniqueId();
        Map<UUID, Double> dmg = damageMap.remove(id);
        announcedSpawn.remove(id);
        phaseByEntity.remove(id);
        // Forget the waypoint for everyone immediately
        for (Player p : Bukkit.getOnlinePlayers()) {
            com.soulenchants.lunar.LunarBridge.clearWaypoint(p, s.scoreboardName);
        }
        // Guild points — same 10%-contrib threshold the Veilweaver uses
        if (plugin.getGuildManager() != null && dmg != null && !dmg.isEmpty()) {
            Map<com.soulenchants.guilds.Guild, Double> byGuild = new HashMap<>();
            for (Map.Entry<UUID, Double> e : dmg.entrySet()) {
                com.soulenchants.guilds.Guild g =
                        plugin.getGuildManager().getByMember(e.getKey());
                if (g == null) continue;
                byGuild.merge(g, e.getValue(), Double::sum);
            }
            double max = boss.getMaxHealth();
            for (Map.Entry<com.soulenchants.guilds.Guild, Double> e : byGuild.entrySet()) {
                double pct = e.getValue() / max;
                if (pct < 0.10) continue;
                long pts = Math.max(1L, (long)(s.guildBasePoints * Math.min(1.0, pct)));
                plugin.getGuildManager().awardPoints(e.getKey(), pts,
                        s.id + " kill");
            }
        }
        String div = s.color + "" + ChatColor.STRIKETHROUGH + "                                          ";
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(div);
        Bukkit.broadcastMessage(s.color + "" + ChatColor.BOLD + "        ☠ " + s.display.toUpperCase() + " HAS FALLEN ☠");
        if (killer != null) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "  Slain by  " + ChatColor.WHITE + killer.getName());
        }
        Bukkit.broadcastMessage(div);
        Bukkit.broadcastMessage("");
    }

    // ──────────────────── Listeners ────────────────────

    /** Track damage contribution for the guild-points split. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamagesBoss(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        if (!(e.getDamager() instanceof Player)) return;
        LivingEntity le = (LivingEntity) e.getEntity();
        if (!isElite(CustomMob.idOf(le))) return;
        trackDamage(le, (Player) e.getDamager(), e.getFinalDamage());
    }

    /** Fire the death banner + guild-points award. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDeath(EntityDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (!isElite(CustomMob.idOf(v))) return;
        onDeath(plugin, v, v.getKiller());
    }

    /**
     * Prevent Wurm-Lord's magma-cube minions from splitting on death. We
     * cancel any SLIME_SPLIT CreatureSpawn whose location lies within 3
     * blocks of a dying custom-tagged MagmaCube — the parent is still in
     * the world with its NBT at the moment the child spawns.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSlimeSplit(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) return;
        if (!(e.getEntity() instanceof MagmaCube)) return;
        Location loc = e.getLocation();
        for (org.bukkit.entity.Entity near : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (!(near instanceof MagmaCube)) continue;
            if (CustomMob.idOf((LivingEntity) near) != null) {
                e.setCancelled(true);
                return;
            }
        }
    }

    /** Banner fires the first time we see an elite CustomMob in the world. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGenericSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity le = (LivingEntity) e.getEntity();
        // CustomMob tag is written AFTER spawn, so defer one tick.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (le.isDead()) return;
            if (isElite(CustomMob.idOf(le))) announceSpawn(le);
        }, 2L);
    }
}
