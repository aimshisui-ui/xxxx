package com.soulenchants.rifts;

import com.soulenchants.SoulEnchants;
import com.soulenchants.mobs.CustomMob;
import com.soulenchants.mobs.MobRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Core Void Rift lifecycle.
 *
 * States:
 *   IDLE              — nothing active
 *   PORTAL_OPEN (15s) — portal block placed at spawn location, anyone within
 *                       2 blocks gets TP'd to rift_world. A hologram above
 *                       counts down the remaining seconds.
 *   ACTIVE (10 min)   — portal has closed; mobs/bosses from RiftSpawnConfig
 *                       have been spawned in rift_world; players must kill
 *                       them all. Failure = timer expires. Success = all
 *                       tracked mobs are dead.
 *
 * Only one rift is active at a time. Subsequent spawn attempts are rejected.
 */
public final class VoidRiftManager {

    public enum State { IDLE, PORTAL_OPEN, ACTIVE }

    private final SoulEnchants plugin;
    private final RiftSpawnConfig spawns;

    private State state = State.IDLE;
    private Location portalLoc;            // block turned into END_PORTAL
    private Material portalOld;            // so we can restore on close
    private byte portalOldData;
    private Hologram portalHologram;
    private Hologram riftHologram;         // inside rift_world — status/timer
    private int portalSecondsLeft;
    private int activeSecondsLeft;
    private BukkitTask portalTask;
    private BukkitTask activeTask;
    private final Set<UUID> trackedMobs = new HashSet<>();
    private final Set<UUID> riftParticipants = new HashSet<>();
    private String opener;                 // who opened the rift

    public VoidRiftManager(SoulEnchants plugin, RiftSpawnConfig spawns) {
        this.plugin = plugin;
        this.spawns = spawns;
    }

    public State getState() { return state; }
    public boolean isActive() { return state != State.IDLE; }
    public Location getPortalLoc() { return portalLoc; }
    public Set<UUID> getParticipants() { return riftParticipants; }

    // ── 1. Open the portal at the spawner-placement location ─────────────
    public boolean open(Location at, Player by) {
        if (state != State.IDLE) {
            by.sendMessage(ChatColor.RED + "✦ A Void Rift is already active. Only one may exist at a time.");
            by.sendMessage(ChatColor.GRAY + "  Current state: " + status());
            return false;
        }
        if (spawns.list().isEmpty()) {
            by.sendMessage(ChatColor.RED + "✦ The rift refuses to open — no encounter is configured.");
            by.sendMessage(ChatColor.GRAY + "  Use " + ChatColor.WHITE + "/rift addspawn <mob_id>"
                    + ChatColor.GRAY + " inside rift_world to place threats first.");
            return false;
        }
        this.opener = by.getName();
        this.portalLoc = at.getBlock().getLocation().clone();
        this.portalOld = portalLoc.getBlock().getType();
        this.portalOldData = portalLoc.getBlock().getData();
        this.portalSecondsLeft = 15;

        // Use purple stained glass as the visual marker — it's striking and
        // (unlike END_PORTAL) doesn't have any vanilla teleport semantics that
        // would whisk players to the End. The actual TP is handled by the
        // proximity tick in startPortalCountdown.
        portalLoc.getBlock().setType(Material.STAINED_GLASS);
        portalLoc.getBlock().setData((byte) 10); // 10 = purple
        // Particle burst
        World w = portalLoc.getWorld();
        for (int i = 0; i < 80; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * 2;
            Location p = portalLoc.clone().add(0.5 + Math.cos(a) * r, Math.random() * 2, 0.5 + Math.sin(a) * r);
            w.playEffect(p, Effect.PORTAL, 0);
        }
        w.playSound(portalLoc, Sound.ENDERDRAGON_GROWL, 1.5f, 0.7f);

        // Hologram above portal
        Location holoAt = portalLoc.clone().add(0.5, 2.8, 0.5);
        portalHologram = new Hologram(holoAt, java.util.Arrays.asList(
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ VOID RIFT ✦",
                ChatColor.GRAY + "Opened by " + ChatColor.WHITE + opener,
                ChatColor.LIGHT_PURPLE + "Step inside to enter",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Closes in 15s"
        ));

        // Global broadcast — thematic + informative
        int bx = portalLoc.getBlockX(), by_ = portalLoc.getBlockY(), bz = portalLoc.getBlockZ();
        String worldName = portalLoc.getWorld().getName();
        String div = ChatColor.DARK_PURPLE + "" + ChatColor.STRIKETHROUGH + "                                          ";
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(div);
        Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "        ✦ A VOID RIFT HAS TORN OPEN ✦");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC
                + "  \"Something old breathes again. Run, or follow.\"");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GRAY + "  Torn open by " + ChatColor.LIGHT_PURPLE + opener);
        Bukkit.broadcastMessage(ChatColor.GRAY + "  Coordinates  " + ChatColor.WHITE
                + bx + ", " + by_ + ", " + bz + ChatColor.DARK_GRAY + "  (" + worldName + ")");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "  ⏳ The portal closes in " + ChatColor.WHITE + "15 seconds");
        Bukkit.broadcastMessage(ChatColor.RED    + "  ⚔ PvP is enabled within " + ChatColor.GRAY + "— enter at your own risk");
        Bukkit.broadcastMessage(ChatColor.GRAY   + "  Step through to join the hunt.");
        Bukkit.broadcastMessage(div);
        Bukkit.broadcastMessage("");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 0.5f, 0.4f);
            p.playSound(p.getLocation(), Sound.PORTAL_TRIGGER, 0.4f, 0.7f);
        }

        state = State.PORTAL_OPEN;
        startPortalCountdown();
        return true;
    }

    private void startPortalCountdown() {
        // Per-second tick — handles countdown + player TP + close transition
        portalTask = new BukkitRunnable() {
            @Override public void run() {
                if (state != State.PORTAL_OPEN) { cancel(); return; }
                // Pull nearby players into the rift
                for (Player p : portalLoc.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(portalLoc.clone().add(0.5, 0.5, 0.5)) <= 2.5) {
                        sendToRift(p);
                    }
                }
                portalSecondsLeft--;
                if (portalHologram != null) {
                    portalHologram.updateLine(3, ChatColor.YELLOW + "" + ChatColor.BOLD + "Closes in " + portalSecondsLeft + "s");
                }
                if (portalSecondsLeft <= 0) {
                    closePortal();
                    if (riftParticipants.isEmpty()) {
                        // Auto-close: nobody walked through. Don't waste anyone's time
                        // spawning the encounter into an empty world.
                        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC
                                + "  The rift sealed without a single soul stepping through.");
                        cleanup();
                        return;
                    }
                    beginActive();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Per-tick effects — heavy portal particles around the marker so the
        // stained-glass block reads as a tear in reality, not just a glass cube.
        new BukkitRunnable() {
            @Override public void run() {
                if (state != State.PORTAL_OPEN || portalLoc == null) { cancel(); return; }
                Location center = portalLoc.clone().add(0.5, 0.5, 0.5);
                for (int i = 0; i < 12; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 0.4 + Math.random() * 1.4;
                    double dy = Math.random() * 2.4;
                    Location p = center.clone().add(Math.cos(a) * r, dy, Math.sin(a) * r);
                    p.getWorld().playEffect(p, Effect.PORTAL, 0);
                }
                // Occasional witch magic for purple bloom
                if (Math.random() < 0.4) {
                    center.getWorld().playEffect(center.clone().add(0, 1.5, 0), Effect.WITCH_MAGIC, 0);
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private void closePortal() {
        try { portalLoc.getBlock().setType(portalOld); portalLoc.getBlock().setData(portalOldData); }
        catch (Throwable ignored) {}
        if (portalHologram != null) { portalHologram.destroy(); portalHologram = null; }
        if (portalTask != null) { portalTask.cancel(); portalTask = null; }
    }

    private void sendToRift(Player p) {
        World rift = Bukkit.getWorld(RiftWorld.NAME);
        if (rift == null) return;
        if (p.getWorld().equals(rift)) return;
        p.teleport(rift.getSpawnLocation());
        riftParticipants.add(p.getUniqueId());
        p.playSound(p.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, 0.6f);
        p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 0.4f, 0.4f);

        // Long guided entry message — this is ULTRA late-game content, players
        // need to know what they're walking into.
        String div = ChatColor.DARK_PURPLE + "" + ChatColor.STRIKETHROUGH + "                                          ";
        p.sendMessage("");
        p.sendMessage(div);
        p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "        ✦ YOU HAVE CROSSED THE VEIL ✦");
        p.sendMessage("");
        p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC
                + "  The stone closes behind you. The light bleeds out");
        p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC
                + "  through the cracks above. Something is breathing.");
        p.sendMessage("");
        p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "  ⚔ YOUR PURPOSE");
        p.sendMessage(ChatColor.GRAY + "    ▸ Find and destroy " + ChatColor.WHITE + "every threat" + ChatColor.GRAY + " within these halls");
        p.sendMessage(ChatColor.GRAY + "    ▸ The rift seals itself in " + ChatColor.WHITE + "10 minutes");
        p.sendMessage(ChatColor.GRAY + "    ▸ Slay them all to claim what they guard");
        p.sendMessage("");
        p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "  ⚠ KNOW BEFORE YOU SWING");
        p.sendMessage(ChatColor.GRAY + "    ▸ " + ChatColor.RED + "PvP is open" + ChatColor.GRAY + " — every soul here is a threat");
        p.sendMessage(ChatColor.GRAY + "    ▸ Death " + ChatColor.GREEN + "preserves your gear" + ChatColor.GRAY + " — you respawn within");
        p.sendMessage(ChatColor.GRAY + "    ▸ Failure " + ChatColor.RED + "expels you" + ChatColor.GRAY + " back to the surface");
        p.sendMessage(ChatColor.GRAY + "    ▸ The Hollow King " + ChatColor.RED + "does not forgive" + ChatColor.GRAY + " an under-prepared blade");
        p.sendMessage("");
        p.sendMessage(ChatColor.YELLOW + "  Track your progress on the right-side scoreboard.");
        p.sendMessage(div);
        p.sendMessage("");
    }

    // ── 2. Rift becomes active — spawn mobs, start the 10-min timer ─────
    private void beginActive() {
        state = State.ACTIVE;
        this.activeSecondsLeft = 600;

        World rift = Bukkit.getWorld(RiftWorld.NAME);
        if (rift == null) {
            plugin.getLogger().warning("[rift] cannot begin active — rift_world not loaded.");
            fail("rift world missing");
            return;
        }

        // Configured spawns ONLY — no procedural fallback. Force-load each
        // chunk before spawning so distant placements don't silently fail.
        trackedMobs.clear();
        int spawned = 0;
        for (RiftSpawnConfig.Entry e : spawns.list()) {
            try {
                Location loc = e.toLocation(rift);
                if (loc != null) {
                    try { loc.getChunk().load(true); } catch (Throwable ignored) {}
                }
                LivingEntity le = spawnOne(e, rift);
                if (le != null) { trackedMobs.add(le.getUniqueId()); spawned++; }
                else plugin.getLogger().warning("[rift] spawnOne returned null for " + e.mob + " @ " + loc);
            } catch (Throwable t) {
                plugin.getLogger().warning("[rift] spawn " + e.mob + " failed: " + t);
            }
        }
        plugin.getLogger().info("[rift] active — spawned " + spawned + " enemies from configured list");

        if (spawned == 0) {
            // Hard fail — the configured list spawned nothing. Don't auto-clear
            // an empty rift; alert participants and end.
            broadcastRift(ChatColor.RED + "✦ The rift fractured before forming — no enemies could be summoned.");
            broadcastRift(ChatColor.GRAY + "  (Check chunk loading at spawn coords + that mob ids exist.)");
            fail("encounter spawn failed");
            return;
        }
        setSpawnedCount(spawned);

        // In-rift status hologram at world spawn
        Location holoAt = rift.getSpawnLocation().clone().add(0.5, 3.5, 0.5);
        riftHologram = new Hologram(holoAt, java.util.Arrays.asList(
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ VOID RIFT ✦",
                ChatColor.RED + "⚠ PvP enabled",
                ChatColor.RED + "⚠ Death = keep gear",
                ChatColor.GRAY + "Threats remaining: " + ChatColor.WHITE + spawned,
                ChatColor.YELLOW + "Timer: " + formatTime(activeSecondsLeft)
        ));

        for (UUID u : riftParticipants) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ The rift seals behind you.");
                p.sendMessage(ChatColor.GRAY + "  " + spawned + " threats detected. Timer started.");
            }
        }

        // Tick every second
        final int totalSpawned = spawned;
        activeTask = new BukkitRunnable() {
            @Override public void run() {
                if (state != State.ACTIVE) { cancel(); return; }

                // Prune ONLY confirmed-dead mobs. If lookup() returns null
                // it just means the chunk is unloaded — the mob is still alive
                // out there. Auto-removing those would falsely complete the rift.
                trackedMobs.removeIf(u -> {
                    org.bukkit.entity.Entity ent = lookup(u);
                    return ent != null && ent.isDead();
                });

                if (trackedMobs.isEmpty()) { complete(totalSpawned); cancel(); return; }

                activeSecondsLeft--;
                if (riftHologram != null) {
                    riftHologram.updateLine(3, ChatColor.GRAY + "Threats remaining: " + ChatColor.WHITE + trackedMobs.size());
                    riftHologram.updateLine(4, ChatColor.YELLOW + "Timer: " + formatTime(activeSecondsLeft));
                }

                // Warnings
                if (activeSecondsLeft == 300 || activeSecondsLeft == 60 || activeSecondsLeft == 30
                        || activeSecondsLeft == 10) {
                    broadcastRift(ChatColor.YELLOW + "⚠ " + formatTime(activeSecondsLeft) + " remaining — "
                            + trackedMobs.size() + " threats left.");
                }

                if (activeSecondsLeft <= 0) { fail("timer expired"); cancel(); }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private LivingEntity spawnOne(RiftSpawnConfig.Entry e, World rift) {
        Location loc = e.toLocation(rift);
        String id = e.mob.toLowerCase();
        if (id.equals("veilweaver")) {
            plugin.getVeilweaverManager().summon(loc);
            // The manager spawns and tracks internally — we can't easily get the entity.
            // For rift completion, we'll also count an "imaginary" boss mob via a sentinel.
            return null;
        }
        if (id.equals("irongolem") || id.equals("colossus")) {
            plugin.getIronGolemManager().summon(loc);
            return null;
        }
        CustomMob cm = MobRegistry.get(id);
        if (cm == null) {
            plugin.getLogger().warning("[rift] unknown mob id: " + id);
            return null;
        }
        return cm.spawn(loc);
    }

    // ── 3. Completion / failure ──────────────────────────────────────────
    private void complete(int totalSpawned) {
        broadcastRift("");
        broadcastRift(ChatColor.GREEN + "" + ChatColor.BOLD + "✦ RIFT CLEARED ✦");
        broadcastRift(ChatColor.GRAY + "  All " + totalSpawned + " threats slain. Rewards granted.");
        // Distribute rewards: souls + a Boss loot box, to anyone still inside
        for (UUID u : new ArrayList<>(riftParticipants)) {
            Player p = Bukkit.getPlayer(u);
            if (p == null || !p.isOnline()) continue;
            int soulsReward = 5000;
            plugin.getSoulManager().add(p, soulsReward);
            p.sendMessage(ChatColor.LIGHT_PURPLE + "✦ +" + soulsReward + " souls " + ChatColor.GRAY + "(rift clear)");
            try {
                p.getInventory().addItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BOSS))
                        .values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
            } catch (Throwable ignored) {}
        }
        // 10-second grace, then expel
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() { expelAll(true); cleanup(); }
        }, 20L * 10);
    }

    private void fail(String reason) {
        broadcastRift("");
        broadcastRift(ChatColor.RED + "" + ChatColor.BOLD + "✦ RIFT COLLAPSED ✦");
        broadcastRift(ChatColor.GRAY + "  " + reason + ". You are expelled.");
        expelAll(false);
        cleanup();
    }

    private void expelAll(boolean success) {
        World rift = Bukkit.getWorld(RiftWorld.NAME);
        if (rift == null) return;
        Location main = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (UUID u : new ArrayList<>(riftParticipants)) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            if (!p.getWorld().equals(rift)) continue;
            p.teleport(main);
            p.sendMessage(success
                    ? ChatColor.GREEN + "✦ You emerge from the void."
                    : ChatColor.RED + "✦ The void rejects you.");
        }
    }

    public void cleanup() {
        if (portalTask != null) { portalTask.cancel(); portalTask = null; }
        if (activeTask != null) { activeTask.cancel(); activeTask = null; }
        if (portalHologram != null) { portalHologram.destroy(); portalHologram = null; }
        if (riftHologram != null) { riftHologram.destroy(); riftHologram = null; }
        // Remove any still-living tracked mobs
        for (UUID u : trackedMobs) {
            org.bukkit.entity.Entity ent = lookup(u);
            if (ent != null) { try { ent.remove(); } catch (Throwable ignored) {} }
        }
        trackedMobs.clear();
        riftParticipants.clear();
        state = State.IDLE;
        portalLoc = null;
        opener = null;
    }

    // ── Utilities ────────────────────────────────────────────────────────
    private void broadcastRift(String msg) {
        for (UUID u : riftParticipants) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(msg);
        }
        // Also log to console so admins watching can see
        plugin.getLogger().info("[rift] " + ChatColor.stripColor(msg));
    }

    /** Bukkit 1.8.8 has no Bukkit.getEntity(UUID) — fall back to scanning loaded worlds. */
    private static org.bukkit.entity.Entity lookup(UUID id) {
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity e : w.getEntities()) {
                if (e.getUniqueId().equals(id)) return e;
            }
        }
        return null;
    }

    private static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    public String status() {
        if (state == State.IDLE) return "idle";
        int t = state == State.PORTAL_OPEN ? portalSecondsLeft : activeSecondsLeft;
        return state.name().toLowerCase() + " — " + formatTime(t) + " · "
                + trackedMobs.size() + " mobs · " + riftParticipants.size() + " participants";
    }

    /** Compact label for the scoreboard. */
    public String shortStatus() {
        if (state == State.PORTAL_OPEN) return portalSecondsLeft + "s left";
        if (state == State.ACTIVE)      return formatTime(activeSecondsLeft) + " · " + trackedMobs.size() + " mobs";
        return "idle";
    }

    public String timerLabel() {
        if (state == State.PORTAL_OPEN) return portalSecondsLeft + "s";
        if (state == State.ACTIVE) return formatTime(activeSecondsLeft);
        return "—";
    }

    public int threatsRemaining() { return trackedMobs.size(); }
    public int participantCount() { return riftParticipants.size(); }

    /** Called by MobListener when a custom mob inside the rift dies. */
    public void notifyMobDied(java.util.UUID mobId) {
        if (state != State.ACTIVE) return;
        if (!trackedMobs.remove(mobId)) return;
        if (trackedMobs.isEmpty()) {
            // Capture the count BEFORE clearing so the message reports the spawn total
            complete(spawnedCount);
            if (activeTask != null) { activeTask.cancel(); activeTask = null; }
        }
    }

    /** Called by VoidRiftListener when a player respawns out of the rift on death. */
    public void notifyParticipantExpelled(java.util.UUID playerId) {
        if (state != State.ACTIVE) return;
        if (!riftParticipants.remove(playerId)) return;
        if (riftParticipants.isEmpty()) {
            broadcastRift("");
            broadcastRift(ChatColor.RED + "" + ChatColor.BOLD + "✦ All who entered have fallen.");
            fail("all participants down");
        }
    }

    public java.util.Set<java.util.UUID> getTrackedMobs() { return trackedMobs; }

    /** Stash spawned count so notifyMobDied can use it for the success message. */
    private int spawnedCount = 0;
    public void setSpawnedCount(int n) { this.spawnedCount = n; }
}
