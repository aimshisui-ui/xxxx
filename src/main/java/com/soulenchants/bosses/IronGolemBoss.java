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

    public static final double MAX_HP = 500.0;
    public static final String NBT_IRONGOLEM = "se_irongolem_boss";

    private final SoulEnchants plugin;
    private final IronGolem entity;
    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private Phase phase = Phase.ONE;
    private boolean invulnerable = false;
    private boolean usedReinforce = false;
    private int ticks = 0;

    private int cdStomp = 60;
    private int cdBoulder = 100;
    private int cdRocket = 80;
    private int cdMagnetic = 150;
    private int cdIronWall = 200;
    private int cdSlam = 180;

    private BukkitRunnable tickTask;

    public IronGolemBoss(SoulEnchants plugin, Location spawnLoc) {
        this.plugin = plugin;
        this.entity = (IronGolem) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);
        configureEntity();
        de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(entity);
        nbt.setBoolean(NBT_IRONGOLEM, true);
    }

    private void configureEntity() {
        entity.setMaxHealth(MAX_HP);
        entity.setHealth(MAX_HP);
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

    private void runAttacks() {
        if (--cdStomp <= 0)   { cdStomp = 160; seismicStomp(); }
        if (--cdBoulder <= 0) { cdBoulder = 240; boulderThrow(); }
        if (phase == Phase.TWO) {
            if (--cdRocket <= 0)    { cdRocket = 200; rocketCharge(); }
            if (--cdMagnetic <= 0)  { cdMagnetic = 300; magneticPull(); }
            if (--cdIronWall <= 0)  { cdIronWall = 400; ironWall(); }
            if (--cdSlam <= 0)      { cdSlam = 360; groundSlam(); }
        }
    }

    private void transitionToTwo() {
        invulnerable = true;
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
            p.sendTitle(titleMain, titleSub);
            for (String line : broadcast) p.sendMessage(line);
            p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 1.5f, 0.8f);
        }
        new BukkitRunnable() {
            @Override public void run() { invulnerable = false; }
        }.runTaskLater(plugin, 60L);
    }

    private void reinforce() {
        invulnerable = true;
        plugin.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD
                + "✦ The Colossus reinforces! ✦");
        entity.getWorld().playSound(entity.getLocation(), Sound.IRONGOLEM_DEATH, 2.0f, 0.7f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t++ >= 100 || entity.isDead()) {
                    invulnerable = false;
                    cancel();
                    return;
                }
                entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + 1.0));
                entity.getWorld().playEffect(entity.getLocation().add(0, 1, 0),
                        Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
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
                // Impact
                loc.getWorld().playSound(loc, Sound.EXPLODE, 2.0f, 0.5f);
                loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0f, false);
                for (int r = 1; r <= 6; r++) {
                    for (int i = 0; i < 24; i++) {
                        double a = (Math.PI * 2 * i) / 24.0;
                        Location p = loc.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                        p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.STONE.getId());
                    }
                }
                for (Player p : nearbyPlayers(6)) {
                    BossDamage.apply(p, 18, entity);
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
        Vector dir = target.getEyeLocation().toVector().subtract(origin.toVector()).normalize().multiply(1.4);
        dir.setY(dir.getY() + 0.4);
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
                    BossDamage.apply(target, 22, entity);
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
            p.sendMessage("§6✦ §eThe Colossus pulls you in!");
        }
    }

    private void ironWall() {
        final Location center = entity.getLocation();
        final List<Location> wallLocs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            for (int dx = -2; dx <= 2; dx++) {
                Location wallLoc = center.clone().add(Math.cos(angle) * 4 + dx * Math.sin(angle), 1,
                        Math.sin(angle) * 4 - dx * Math.cos(angle));
                if (wallLoc.getBlock().getType() == Material.AIR) {
                    wallLoc.getBlock().setType(Material.COBBLESTONE);
                    wallLocs.add(wallLoc);
                }
            }
        }
        entity.getWorld().playSound(center, Sound.ANVIL_LAND, 2.0f, 0.5f);
        // Fade telegraph at 8s — smoke particles + sound
        new BukkitRunnable() {
            @Override public void run() {
                for (Location l : wallLocs) {
                    l.getWorld().playEffect(l.clone().add(0.5, 0.5, 0.5), Effect.SMOKE, 4);
                }
                center.getWorld().playSound(center, Sound.FIZZ, 1.0f, 1.5f);
            }
        }.runTaskLater(plugin, 160L); // 8 seconds
        // Remove walls after 10 seconds
        new BukkitRunnable() {
            @Override public void run() {
                for (Location l : wallLocs) {
                    if (l.getBlock().getType() == Material.COBBLESTONE) {
                        l.getBlock().setType(Material.AIR);
                        l.getWorld().playEffect(l.clone().add(0.5, 0.5, 0.5), Effect.STEP_SOUND,
                                Material.COBBLESTONE.getId());
                    }
                }
            }
        }.runTaskLater(plugin, 200L); // 10 seconds
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
                    for (int r = 1; r <= 7; r++) {
                        for (int i = 0; i < 24; i++) {
                            double a = (Math.PI * 2 * i) / 24.0;
                            Location p = loc.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                            p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.COBBLESTONE.getId());
                        }
                    }
                    for (Player p : nearbyPlayers(7)) {
                        double dist = p.getLocation().distance(loc);
                        double dmg = Math.max(6, 28 - dist * 3);
                        BossDamage.apply(p, dmg, entity);
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

    public boolean isInvulnerable() { return invulnerable; }
    public IronGolem getEntity() { return entity; }
    public SoulEnchants getPlugin() { return plugin; }

    public void stop(boolean killed) {
        if (tickTask != null) try { tickTask.cancel(); } catch (Exception ignored) {}
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
