package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class IronGolemBoss {

    public enum Phase { ONE, TWO }

    // Mid-late game tier. Less brutal than endgame Hollow King (25k) or
    // late-game Veilweaver (15k). Solid wall of HP without being absurd.
    public static final double MAX_HP = 8000.0;
    public static final String NBT_IRONGOLEM = "se_irongolem_boss";

    private final SoulEnchants plugin;
    private final IronGolem entity;
    private final Map<UUID, Double> damageDealt = new HashMap<>();
    // Persistent record of every temp block we've placed; emergency cleanup on boss death
    private final List<Location> activeTempBlocks = new ArrayList<>();
    private final IronGolemMinions minions;
    private int idleTicks = 0;
    private boolean despawnAnnounced = false;
    private int despawnAt = -1;
    private Phase phase = Phase.ONE;
    private boolean invulnerable = false;
    private boolean usedReinforce = false;
    private int ticks = 0;

    private int cdStomp = 100;
    private int cdBoulder = 160;
    private int cdRocket = 140;
    private int cdMagnetic = 220;
    private int cdIronWall = 320;
    private int cdSlam = 300;
    private int nextAttackAt = 100;
    private final java.util.Random rng = new java.util.Random();

    private BukkitRunnable tickTask;

    public IronGolemBoss(SoulEnchants plugin, Location spawnLoc) {
        this.plugin = plugin;
        this.entity = (IronGolem) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);
        configureEntity();
        de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(entity);
        nbt.setBoolean(NBT_IRONGOLEM, true);
        this.minions = new IronGolemMinions(plugin, this);
    }

    public IronGolemMinions getMinions() { return minions; }

    private void configureEntity() {
        double hp = com.soulenchants.bosses.BossDamage.bossHpOverride("irongolem", MAX_HP);
        com.soulenchants.util.BossHealthHack.apply(entity, hp);
        entity.setRemoveWhenFarAway(false);
        entity.setPlayerCreated(false);
        updateName();
    }

    public void start() {
        plugin.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD
                + "✦ The Ironheart Colossus rises! ✦");
        entity.getWorld().playSound(entity.getLocation(), Sound.IRONGOLEM_DEATH, 2.0f, 0.5f);
        tickTask = new BukkitRunnable() {
            @Override public void run() { tick(); }
        };
        tickTask.runTaskTimer(plugin, 1L, 1L);
        // Sentinels spawn with a small delay so the boss is settled
        new BukkitRunnable() {
            @Override public void run() { minions.start(); }
        }.runTaskLater(plugin, 40L);
    }

    public void tick() {
        if (entity.isDead()) { stop(false); return; }
        ticks++;

        // Force hostile targeting every second
        if (ticks % 20 == 0) {
            Player nearest = null;
            double bestSq = Double.MAX_VALUE;
            for (Player pl : nearbyPlayers(30)) {
                double d = pl.getLocation().distanceSquared(entity.getLocation());
                if (d < bestSq) { bestSq = d; nearest = pl; }
            }
            if (nearest != null) entity.setTarget(nearest);
        }

        // Melee enforcer — vanilla iron-golem AI swings get cancelled by ability
        // channels. Every 28t (1.4s), if a player is within 4 blocks (golem reach),
        // force a swing for 70 dmg. Mid-late tier scaling (Veilweaver=110, HK=150).
        if (ticks % 28 == 0) {
            Player closest = null; double bestSq = Double.MAX_VALUE;
            for (Player pl : entity.getWorld().getPlayers()) {
                double d = pl.getLocation().distanceSquared(entity.getLocation());
                if (d < bestSq) { bestSq = d; closest = pl; }
            }
            if (closest != null && bestSq <= 16.0) {                      // 4-block reach
                entity.getWorld().playSound(entity.getLocation(), Sound.IRONGOLEM_HIT, 1.2f, 0.7f);
                BossDamage.apply(closest, "irongolem", "melee", 70, entity);
            }
        }

        // Constant ambient particles
        if (ticks % 4 == 0) {
            Location loc = entity.getLocation().add(0, 1.5, 0);
            for (int i = 0; i < 3; i++) {
                double a = ticks * 0.15 + i * (Math.PI * 2 / 3);
                Location p = loc.clone().add(Math.cos(a) * 1.0, 0, Math.sin(a) * 1.0);
                p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
            }
        }
        if (ticks % 5 == 0) updateName();

        // No-players despawn (15 block radius). 5s grace, then 10s warning, then despawn.
        if (ticks % 20 == 0) {
            boolean any = !nearbyPlayers(15).isEmpty();
            if (any) {
                idleTicks = 0;
                if (despawnAnnounced) {
                    despawnAnnounced = false;
                    despawnAt = -1;
                    plugin.getServer().broadcastMessage(ChatColor.GOLD
                            + "✦ The Ironheart Colossus senses prey — its retreat is cancelled.");
                }
            } else {
                idleTicks += 20;
                if (!despawnAnnounced && idleTicks >= 100) {
                    despawnAnnounced = true;
                    despawnAt = ticks + 200;
                    plugin.getServer().broadcastMessage(ChatColor.GRAY
                            + "✦ The Ironheart Colossus finds no challengers — retreating in 10s.");
                }
                if (despawnAnnounced && ticks >= despawnAt) {
                    plugin.getServer().broadcastMessage(ChatColor.DARK_GRAY
                            + "✦ The Ironheart Colossus vanishes into the dust.");
                    entity.remove();
                    plugin.getIronGolemManager().clearActive();
                    stop(false);
                    return;
                }
            }
        }

        // Phase transition
        if (phase == Phase.ONE && entity.getHealth() / entity.getMaxHealth() <= 0.50) transitionToTwo();

        // Reinforce — once below 25% HP
        if (!usedReinforce && entity.getHealth() / entity.getMaxHealth() < 0.25 && phase == Phase.TWO) {
            usedReinforce = true;
            reinforce();
        }

        if (invulnerable) return;
        runAttacks();
    }

    /**
     * Weighted random attack picker. Weaker attacks (Stomp, Boulder) are heavily
     * favored; stronger ones still appear regularly. Each attack respects its
     * own per-attack cooldown so the same one doesn't fire back-to-back.
     */
    private void runAttacks() {
        // tick all per-attack cooldowns down so they're available when picked
        if (cdStomp > 0)    cdStomp--;
        if (cdBoulder > 0)  cdBoulder--;
        if (cdRocket > 0)   cdRocket--;
        if (cdMagnetic > 0) cdMagnetic--;
        if (cdIronWall > 0) cdIronWall--;
        if (cdSlam > 0)     cdSlam--;

        if (--nextAttackAt > 0) return;
        nextAttackAt = 120 + rng.nextInt(60); // 6-9s between picks (was 3-5s)

        // Build weighted pool of attacks that are off cooldown
        java.util.List<String> pool = new java.util.ArrayList<>();
        if (cdStomp <= 0)   for (int i = 0; i < 6; i++) pool.add("stomp");    // common
        if (cdBoulder <= 0) for (int i = 0; i < 4; i++) pool.add("boulder");  // common
        if (phase == Phase.TWO) {
            if (cdRocket <= 0)   for (int i = 0; i < 3; i++) pool.add("rocket");   // mid
            if (cdMagnetic <= 0) for (int i = 0; i < 2; i++) pool.add("magnetic"); // mid
            if (cdIronWall <= 0)                            pool.add("wall");      // rare
            if (cdSlam <= 0)                                pool.add("slam");      // rare
        }
        if (pool.isEmpty()) return;

        switch (pool.get(rng.nextInt(pool.size()))) {
            case "stomp":    cdStomp = 200;    seismicStomp(); break;
            case "boulder":  cdBoulder = 260;  boulderThrow(); break;
            case "rocket":   cdRocket = 240;   rocketCharge(); break;
            case "magnetic": cdMagnetic = 320; magneticPull(); break;
            case "wall":     cdIronWall = 400; ironWall();     break;
            case "slam":     cdSlam = 380;     groundSlam();   break;
        }
    }

    private void transitionToTwo() {
        invulnerable = true;
        // Schedule invuln-clear FIRST so any later exception in the broadcast
        // loop (disconnected player, missing sound, etc.) can't leave the boss
        // stuck invulnerable forever.
        new BukkitRunnable() {
            @Override public void run() { invulnerable = false; }
        }.runTaskLater(plugin, 60L);
        phase = Phase.TWO;
        Location loc = entity.getLocation();
        loc.getWorld().createExplosion(loc.getX(), loc.getY() + 1, loc.getZ(), 0f, false);
        loc.getWorld().playSound(loc, Sound.IRONGOLEM_HIT, 2.0f, 0.5f);
        for (int i = 0; i < 4; i++) {
            double a = i * (Math.PI / 2);
            Location strike = loc.clone().add(Math.cos(a) * 5, 0, Math.sin(a) * 5);
            strike.getWorld().strikeLightningEffect(strike);
        }
        String titleMain = ChatColor.GOLD + "" + ChatColor.BOLD + "✦ IRONHEART AWAKENED ✦";
        String titleSub  = ChatColor.YELLOW + "The Colossus unleashes its rage";
        String[] broadcast = new String[] {
                ChatColor.GOLD + "" + ChatColor.BOLD + "══════════════════════════════",
                ChatColor.GOLD + "" + ChatColor.BOLD + "  ✦ IRONHEART AWAKENED — Phase II ✦",
                ChatColor.GRAY + "  • §6Rocket Charge: §fdodge sideways, not backward",
                ChatColor.GRAY + "  • §6Magnetic Pull: §fbrace to avoid stomp chain",
                ChatColor.GRAY + "  • §6Iron Wall: §fcobble cover for 10s — use it",
                ChatColor.GRAY + "  • §6Ground Slam: §fcenter is a kill zone — scatter",
                ChatColor.GRAY + "  • §cAt 25% HP it reinforces (regens). Burst it first.",
                ChatColor.GOLD + "" + ChatColor.BOLD + "══════════════════════════════"
        };
        for (Player p : nearbyPlayers(30)) {
            try {
                p.sendTitle(titleMain, titleSub);
                for (String line : broadcast) p.sendMessage(line);
                p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 1.5f, 0.8f);
            } catch (Throwable ignored) {}
        }
    }

    private void reinforce() {
        invulnerable = true;
        // Hard guarantee: even if the every-tick task below crashes, this fires
        // after 5s and forcibly clears invuln so the boss can't get stuck.
        new BukkitRunnable() {
            @Override public void run() { invulnerable = false; }
        }.runTaskLater(plugin, 110L);
        try {
            plugin.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD
                    + "✦ The Colossus reinforces! ✦");
            entity.getWorld().playSound(entity.getLocation(), Sound.IRONGOLEM_DEATH, 2.0f, 0.7f);
        } catch (Throwable ignored) {}
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 100 || entity.isDead()) {
                    invulnerable = false;
                    cancel();
                    return;
                }
                try {
                    entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + 1.0));
                    entity.getWorld().playEffect(entity.getLocation().add(0, 1, 0),
                            Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
                } catch (Throwable ignored) {}
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── ATTACKS ──────────────────────────────────────────────────────────────

    private void seismicStomp() {
        final Location loc = entity.getLocation();
        // Telegraph: boss raises fist (small upward jump), ground crack particles for 15 ticks
        entity.setVelocity(new org.bukkit.util.Vector(0, 0.6, 0));
        loc.getWorld().playSound(loc, Sound.IRONGOLEM_THROW, 1.5f, 0.6f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t < 15) {
                    // Pre-shock dust ring
                    double r = (t / 15.0) * 5.0;
                    for (int i = 0; i < 20; i++) {
                        double a = (Math.PI * 2 * i) / 20.0;
                        Location p = loc.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                        p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.STONE.getId());
                    }
                    t++;
                    return;
                }
                // Cinematic shockwave + sound
                loc.getWorld().playSound(loc, Sound.EXPLODE, 2.0f, 0.5f);
                loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0f, false);
                com.soulenchants.bosses.BossEffects.shockwaveRing(plugin, loc, 6.0, Material.STONE);
                com.soulenchants.bosses.BossEffects.particleBurst(loc.clone().add(0, 1, 0), Effect.SMOKE, 60);
                for (Player p : nearbyPlayers(6)) {
                    BossDamage.apply(p, "irongolem", "ground_slam", 300, entity);
                    p.setVelocity(new org.bukkit.util.Vector(p.getVelocity().getX(), 1.0, p.getVelocity().getZ()));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 80, 1));
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void boulderThrow() {
        Player target = pickTarget();
        if (target == null) return;
        Location origin = entity.getEyeLocation();
        org.bukkit.entity.FallingBlock boulder = entity.getWorld().spawnFallingBlock(
                origin, Material.IRON_BLOCK, (byte) 0);
        boulder.setDropItem(false);
        // Lead the target by their current velocity so a moving player doesn't escape
        Vector targetPos = target.getEyeLocation().toVector()
                .add(target.getVelocity().clone().multiply(8));
        Vector toTarget = targetPos.subtract(origin.toVector());
        double horizontalDist = Math.sqrt(toTarget.getX() * toTarget.getX() + toTarget.getZ() * toTarget.getZ());
        // Solve a flat-ish arc: high horizontal speed, just enough Y to compensate for gravity drop
        double speed = 2.4;
        Vector dir = toTarget.normalize().multiply(speed);
        // Anti-gravity Y bump scaled to distance (~0.04/block) — keeps trajectory near a straight line
        dir.setY(dir.getY() + Math.min(0.6, horizontalDist * 0.04));
        boulder.setVelocity(dir);
        new BoulderTracker(plugin, boulder, entity).start();
        entity.getWorld().playSound(entity.getLocation(), Sound.ZOMBIE_METAL, 2.0f, 0.5f);
    }

    private void rocketCharge() {
        Player target = pickTarget();
        if (target == null) return;
        Vector dir = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(2.5);
        dir.setY(0.4);
        entity.setVelocity(dir);
        entity.getWorld().playSound(entity.getLocation(), Sound.GHAST_FIREBALL, 2.0f, 0.7f);
        // Damage target if reached within 30 ticks
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ > 30 || entity.isDead()) { cancel(); return; }
                if (entity.getLocation().distanceSquared(target.getLocation()) < 4) {
                    BossDamage.apply(target, "irongolem", "charge", 340, entity);
                    target.setVelocity(new Vector(0, 0.6, 0));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void magneticPull() {
        Location loc = entity.getLocation();
        loc.getWorld().playSound(loc, Sound.PORTAL_TRIGGER, 2.0f, 0.7f);
        for (Player p : nearbyPlayers(15)) {
            Vector pull = loc.toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.8);
            pull.setY(0.4);
            p.setVelocity(pull);
        }
    }

    private void ironWall() {
        final Location center = entity.getLocation();
        final List<Location> wallLocs = new ArrayList<>();
        // Place 4 walls × 5 wide × 2 tall = 40 blocks. Replace anything except bedrock.
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 0; dy < 2; dy++) {
                    Location wallLoc = center.clone().add(
                            Math.cos(angle) * 4 + dx * Math.sin(angle),
                            dy + 1,
                            Math.sin(angle) * 4 - dx * Math.cos(angle));
                    Material existing = wallLoc.getBlock().getType();
                    if (existing == Material.BEDROCK || existing == Material.COBBLESTONE) continue;
                    wallLoc.getBlock().setType(Material.COBBLESTONE);
                    wallLocs.add(wallLoc);
                    activeTempBlocks.add(wallLoc);
                }
            }
        }
        entity.getWorld().playSound(center, Sound.ANVIL_LAND, 2.0f, 0.5f);
        plugin.getLogger().info("[IronGolem] Iron Wall placed " + wallLocs.size() + " blocks");

        // Fade telegraph at 8s
        new BukkitRunnable() {
            @Override public void run() {
                for (Location l : wallLocs) {
                    l.getWorld().playEffect(l.clone().add(0.5, 0.5, 0.5), Effect.SMOKE, 4);
                }
                center.getWorld().playSound(center, Sound.FIZZ, 1.0f, 1.5f);
            }
        }.runTaskLater(plugin, 160L);

        // Remove walls at 10s — unconditional removal for blocks we placed
        new BukkitRunnable() {
            @Override public void run() {
                int removed = 0;
                for (Location l : wallLocs) {
                    if (l.getBlock().getType() == Material.COBBLESTONE) {
                        l.getBlock().setType(Material.AIR);
                        l.getWorld().playEffect(l.clone().add(0.5, 0.5, 0.5), Effect.STEP_SOUND,
                                Material.COBBLESTONE.getId());
                        removed++;
                    }
                    activeTempBlocks.remove(l);
                }
                plugin.getLogger().info("[IronGolem] Iron Wall removed " + removed + " blocks");
            }
        }.runTaskLater(plugin, 200L);
    }

    private void groundSlam() {
        entity.setVelocity(new Vector(0, 1.5, 0));
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ < 15) return;
                if (entity.isOnGround() || t > 40) {
                    Location loc = entity.getLocation();
                    loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0f, false);
                    // Four lightning flashes around impact
                    for (int i = 0; i < 4; i++) {
                        double a = i * (Math.PI / 2);
                        Location s = loc.clone().add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
                        s.getWorld().strikeLightningEffect(s);
                    }
                    com.soulenchants.bosses.BossEffects.shockwaveRing(plugin, loc, 7.0, Material.COBBLESTONE);
                    com.soulenchants.bosses.BossEffects.particleBurst(loc.clone().add(0, 1, 0), Effect.LARGE_SMOKE, 80);
                    com.soulenchants.bosses.BossEffects.titleNearby(loc, 30, "§6§l✦ SHOCKWAVE ✦", "§7Run further or take it");
                    for (Player p : nearbyPlayers(7)) {
                        double dist = p.getLocation().distance(loc);
                        // Linear falloff: 100% at center → ~30% at edge of 7-block radius
                        double distFactor = Math.max(0.3, 1.0 - dist * 0.10);
                        BossDamage.applyScaled(p, "irongolem", "shockwave", 250, distFactor, entity);
                        p.setVelocity(new Vector(0, 0.8, 0));
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        entity.getWorld().playSound(entity.getLocation(), Sound.IRONGOLEM_THROW, 2.0f, 0.6f);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Player pickTarget() {
        List<Player> players = nearbyPlayers(30);
        return players.isEmpty() ? null : players.get(new Random().nextInt(players.size()));
    }

    public List<Player> nearbyPlayers(double radius) {
        List<Player> list = new ArrayList<>();
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(entity.getLocation()) <= radius * radius) list.add(p);
        }
        return list;
    }

    public void onDamageDealt(Player attacker, double damage) {
        damageDealt.merge(attacker.getUniqueId(), damage, Double::sum);
    }

    public Player getTopDamager() {
        if (damageDealt.isEmpty()) return null;
        UUID top = Collections.max(damageDealt.entrySet(), Map.Entry.comparingByValue()).getKey();
        return plugin.getServer().getPlayer(top);
    }

    public Map<UUID, Double> getDamageDealt() { return damageDealt; }

    public boolean isInvulnerable() { return invulnerable; }
    public void forceClearInvuln() { this.invulnerable = false; }
    public IronGolem getEntity() { return entity; }
    public SoulEnchants getPlugin() { return plugin; }

    public void stop(boolean killed) {
        if (tickTask != null) try { tickTask.cancel(); } catch (Exception ignored) {}
        if (minions != null) minions.stop();
        // Safety net: clean up any temp blocks still in the world
        for (Location l : new ArrayList<>(activeTempBlocks)) {
            if (l.getBlock().getType() == Material.COBBLESTONE) l.getBlock().setType(Material.AIR);
        }
        activeTempBlocks.clear();
        if (killed) plugin.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD
                + "✦ The Ironheart Colossus has fallen. ✦");
    }

    private void updateName() {
        double pct = entity.getHealth() / entity.getMaxHealth();
        int filled = (int) Math.round(pct * 20);
        StringBuilder bar = new StringBuilder(ChatColor.GOLD.toString());
        for (int i = 0; i < 20; i++) {
            if (i == filled) bar.append(ChatColor.DARK_GRAY);
            bar.append("|");
        }
        String tag = phase == Phase.ONE ? "§7[I]" : "§6[II]";
        entity.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Ironheart Colossus "
                + tag + " " + bar + " §e" + (int) entity.getHealth() + "§7/§e" + (int) entity.getMaxHealth());
        entity.setCustomNameVisible(true);
    }
}
