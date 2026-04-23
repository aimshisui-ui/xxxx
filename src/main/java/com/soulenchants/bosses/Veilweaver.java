package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.attacks.VeilweaverAttacks;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Veilweaver {

    public enum Phase { ONE, TWO, THREE }

    public static final double MAX_HP = 15000.0;
    public static final String NBT_VEILWEAVER = "se_veilweaver";

    private final SoulEnchants plugin;
    private final LivingEntity entity;
    private final VeilweaverArena arena;
    private Phase phase = Phase.ONE;

    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private final List<LivingEntity> minions = new ArrayList<>();
    private final List<LivingEntity> echoClones = new ArrayList<>();
    private final VeilweaverCrystals crystals;

    /** Self-expiring invuln. Compared against System.currentTimeMillis().
     *  No abandoned BukkitRunnables, no stuck-on bug. */
    private long invulnerableUntil = 0L;
    private boolean usedFinalBind = false;
    private int ticks = 0;
    private int idleTicks = 0;
    private boolean despawnAnnounced = false;
    private int despawnAt = -1;

    // Attack cooldowns (in ticks). Wider gaps so attacks land like punctuation,
    // not a stream — players need recovery time between hits.
    private int cdThreadLash = 40;
    private int cdShatterBolt = 80;
    private int cdMinionWeave = 400;
    private int cdDimensionalRift = 160;
    private int cdLoomLaser = 220;
    private int cdEchoClones = 800;
    private int cdRealityFracture = 140;
    private int cdApocalypseWeave = 400;

    private BukkitRunnable tickTask;

    public Veilweaver(SoulEnchants plugin, Location spawnLoc) {
        this.plugin = plugin;
        this.arena = new VeilweaverArena(spawnLoc);
        Skeleton skel = (Skeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
        skel.setSkeletonType(Skeleton.SkeletonType.WITHER);
        this.entity = skel;
        configureEntity();
        de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(entity);
        nbt.setBoolean(NBT_VEILWEAVER, true);
        this.crystals = new VeilweaverCrystals(plugin, this);
    }

    public VeilweaverCrystals getCrystals() { return crystals; }

    private void configureEntity() {
        double hp = com.soulenchants.bosses.BossDamage.bossHpOverride("veilweaver", MAX_HP);
        com.soulenchants.util.BossHealthHack.apply(entity, hp);
        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(false);
        // Note: 1.8.8 has no Attribute API. Damage/armor scaling done via damage events;
        // movement speed defaults to wither skeleton baseline.
        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD);
        entity.getEquipment().setItemInHand(sword);
        entity.getEquipment().setItemInHandDropChance(0f);
        updateName();
    }

    public void start() {
        arena.spawn();
        plugin.getServer().broadcastMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD
                + "✦ The Veilweaver tears through reality! ✦");
        entity.getWorld().playSound(entity.getLocation(), Sound.WITHER_SPAWN, 2.0f, 0.6f);
        // Phase I crystal shield
        new BukkitRunnable() {
            @Override public void run() { crystals.spawnRing(4); }
        }.runTaskLater(plugin, 20L);
        tickTask = new BukkitRunnable() {
            @Override public void run() { tick(); }
        };
        tickTask.runTaskTimer(plugin, 1L, 1L);
    }

    public void tick() {
        if (entity.isDead()) { stop(false); return; }
        ticks++;
        arena.tickBorder(entity);

        // Aggressive targeting — set/refresh hostile target every second
        if (ticks % 20 == 0 && entity instanceof org.bukkit.entity.Monster) {
            Player nearest = null;
            double bestSq = Double.MAX_VALUE;
            for (Player pl : arena.playersInArena()) {
                double d = pl.getLocation().distanceSquared(entity.getLocation());
                if (d < bestSq) { bestSq = d; nearest = pl; }
            }
            if (nearest != null) ((org.bukkit.entity.Monster) entity).setTarget(nearest);
        }

        // Melee enforcer — vanilla wither-skeleton AI swings ~once every 2-3s
        // and gets distracted by ability casts. Every 24t (1.2s), if a player
        // is within 3 blocks, force a swing + apply solid melee damage. Counts
        // as the boss's own attack so all our damage hooks fire.
        if (ticks % 24 == 0) {
            Player closest = null; double bestSq = Double.MAX_VALUE;
            for (Player pl : entity.getWorld().getPlayers()) {
                double d = pl.getLocation().distanceSquared(entity.getLocation());
                if (d < bestSq) { bestSq = d; closest = pl; }
            }
            if (closest != null && bestSq <= 9.0) {                       // 3-block reach
                entity.getWorld().playSound(entity.getLocation(), Sound.HURT_FLESH, 1.0f, 0.8f);
                com.soulenchants.bosses.BossDamage.apply(closest, "veilweaver", "melee", 110, entity);
            }
        }

        // Passive anti-kite heal removed per balance pass — bosses no longer
        // regenerate unless it's tied to a phase transition. Instead, teleport
        // the boss back to the arena center if all players have left so she
        // doesn't wander off (no HP regain, though).
        if (ticks % 160 == 0 && arena.noPlayersInArena()) {
            entity.teleport(arena.getCenter());
        }

        // Constant ambient particles around boss (the "thread/runes" effect)
        Location loc = entity.getLocation().add(0, 1.2, 0);
        for (int i = 0; i < 4; i++) {
            double angle = (ticks * 0.2 + i * Math.PI / 2);
            double r = 1.2;
            Location p = loc.clone().add(Math.cos(angle) * r, Math.sin(ticks * 0.1) * 0.4, Math.sin(angle) * r);
            entity.getWorld().playEffect(p, Effect.WITCH_MAGIC, 0);
        }

        // Update name with HP bar
        if (ticks % 5 == 0) updateName();

        // No-players despawn (15 block radius). 5s grace, then 10s warning, then despawn.
        if (ticks % 20 == 0) {
            boolean any = !nearbyPlayers(15).isEmpty();
            if (any) {
                idleTicks = 0;
                if (despawnAnnounced) {
                    despawnAnnounced = false;
                    despawnAt = -1;
                    plugin.getServer().broadcastMessage(ChatColor.DARK_PURPLE
                            + "✦ The Veilweaver feels new threads — its withdrawal is cancelled.");
                }
            } else {
                idleTicks += 20;
                if (!despawnAnnounced && idleTicks >= 100) {
                    despawnAnnounced = true;
                    despawnAt = ticks + 200;
                    plugin.getServer().broadcastMessage(ChatColor.GRAY
                            + "✦ The Veilweaver finds no opposition — withdrawing into the Veil in 10s.");
                }
                if (despawnAnnounced && ticks >= despawnAt) {
                    plugin.getServer().broadcastMessage(ChatColor.DARK_GRAY
                            + "✦ The Veilweaver vanishes between worlds.");
                    entity.remove();
                    plugin.getVeilweaverManager().clearActive();
                    stop(false);
                    return;
                }
            }
        }

        // Phase transitions
        double pct = entity.getHealth() / entity.getMaxHealth();
        if (phase == Phase.ONE && pct <= 0.65) transitionTo(Phase.TWO);
        else if (phase == Phase.TWO && pct <= 0.30) transitionTo(Phase.THREE);

        // Final Thread Bind triggers once below 10%
        if (!usedFinalBind && pct <= 0.10 && phase == Phase.THREE) {
            usedFinalBind = true;
            VeilweaverAttacks.finalThreadBind(this);
        }

        if (System.currentTimeMillis() < invulnerableUntil) return;

        runAttacks();
    }

    private int nextAttackAt = 80;
    private final Random rng = new Random();

    /** Weighted random attack picker. Cheap moves dominate, signature moves still happen. */
    private void runAttacks() {
        if (cdThreadLash > 0)      cdThreadLash--;
        if (cdShatterBolt > 0)     cdShatterBolt--;
        if (cdMinionWeave > 0)     cdMinionWeave--;
        if (cdDimensionalRift > 0) cdDimensionalRift--;
        if (cdLoomLaser > 0)       cdLoomLaser--;
        if (cdEchoClones > 0)      cdEchoClones--;
        if (cdRealityFracture > 0) cdRealityFracture--;
        if (cdApocalypseWeave > 0) cdApocalypseWeave--;

        if (--nextAttackAt > 0) return;
        nextAttackAt = 90 + rng.nextInt(50); // ~4.5-7s between picks (was ~1.75-3s)

        java.util.List<String> pool = new java.util.ArrayList<>();
        if (cdThreadLash <= 0)  for (int i = 0; i < 7; i++) pool.add("lash");    // very common
        if (cdShatterBolt <= 0) for (int i = 0; i < 4; i++) pool.add("bolt");    // common
        if (cdMinionWeave <= 0)                            pool.add("minion");   // rare
        if (phase == Phase.TWO || phase == Phase.THREE) {
            if (cdDimensionalRift <= 0) for (int i = 0; i < 3; i++) pool.add("rift");
            if (cdLoomLaser <= 0)       for (int i = 0; i < 2; i++) pool.add("laser");
            if (cdEchoClones <= 0)                                  pool.add("clones");
        }
        if (phase == Phase.THREE) {
            if (cdRealityFracture <= 0) for (int i = 0; i < 2; i++) pool.add("fracture");
            if (cdApocalypseWeave <= 0)                             pool.add("apoc");
        }
        if (pool.isEmpty()) return;

        switch (pool.get(rng.nextInt(pool.size()))) {
            case "lash":     cdThreadLash = 70 + rng.nextInt(30); VeilweaverAttacks.threadLash(this); break;       // 3.5-5s
            case "bolt":     cdShatterBolt = 160;                  VeilweaverAttacks.shatterBolt(this); break;      // 8s
            case "minion":   cdMinionWeave = 900;                  VeilweaverAttacks.minionWeave(this); break;      // 45s
            case "rift":     cdDimensionalRift = 320;              VeilweaverAttacks.dimensionalRift(this); break;  // 16s
            case "laser":    cdLoomLaser = 420;                    VeilweaverAttacks.loomLaser(this); break;        // 21s
            case "clones":   cdEchoClones = 1200;                  VeilweaverAttacks.echoClones(this); break;       // 60s
            case "fracture": cdRealityFracture = 260;              VeilweaverAttacks.realityFracture(this); break;  // 13s
            case "apoc":     cdApocalypseWeave = 800;              VeilweaverAttacks.apocalypseWeave(this); break;  // 40s
        }
    }

    private void transitionTo(Phase next) {
        // 3-second self-expiring invuln. No scheduled task = no stuck-on bug.
        invulnerableUntil = System.currentTimeMillis() + 3000L;
        phase = next;
        Location loc = entity.getLocation();
        loc.getWorld().createExplosion(loc.getX(), loc.getY() + 1, loc.getZ(), 0f, false);
        loc.getWorld().playSound(loc, Sound.ENDERDRAGON_GROWL, 2.0f, 0.7f);
        for (int i = 0; i < 3; i++) {
            double a = i * (Math.PI * 2 / 3);
            Location strike = loc.clone().add(Math.cos(a) * 4, 0, Math.sin(a) * 4);
            strike.getWorld().strikeLightningEffect(strike);
        }
        // Dramatic broadcast + center-screen title + strategy hints
        String titleMain, titleSub;
        String[] broadcast;
        if (next == Phase.TWO) {
            titleMain = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ THE VEIL TEARS ✦";
            titleSub  = ChatColor.GRAY + "The Veilweaver becomes ethereal";
            broadcast = new String[] {
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "══════════════════════════════",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "  ✦ THE VEIL TEARS — Phase II ✦",
                    ChatColor.GRAY + "  • §dBoss teleports to players and rips rifts",
                    ChatColor.GRAY + "  • §dLoom Laser: 2.5s channel — break line of sight!",
                    ChatColor.GRAY + "  • §dEcho Clones: kill with §cexplosions §dfor instant-drop",
                    ChatColor.GRAY + "  • §5Arrows deal half damage in this phase",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "══════════════════════════════"
            };
        } else {
            titleMain = ChatColor.DARK_RED + "" + ChatColor.BOLD + "✦ THE SHATTERED LOOM ✦";
            titleSub  = ChatColor.RED + "Reality itself breaks down";
            broadcast = new String[] {
                    ChatColor.DARK_RED + "" + ChatColor.BOLD + "══════════════════════════════",
                    ChatColor.DARK_RED + "" + ChatColor.BOLD + "  ✦ THE SHATTERED LOOM — Phase III ✦",
                    ChatColor.GRAY + "  • §cReality Fracture: §fjump between expanding rings",
                    ChatColor.GRAY + "  • §cApocalypse Weave: §fboss goes invulnerable 4s — hide",
                    ChatColor.GRAY + "  • §cAt §4§l10% HP§c: Final Thread Bind roots top damager",
                    ChatColor.GRAY + "  • §cBurn the boss down fast before the bind completes",
                    ChatColor.DARK_RED + "" + ChatColor.BOLD + "══════════════════════════════"
            };
        }
        for (Player p : arena.playersInArena()) {
            com.soulenchants.lunar.LunarFx.sendTitle(p, titleMain, titleSub,
                    250L, 2000L, 500L, 1.35f);
            for (String line : broadcast) p.sendMessage(line);
            p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 1.5f, 1.0f);
        }
        // Destroy 2 orbs on P2 transition; remaining on P3
        if (next == Phase.TWO) arena.destroyOrbs(2);
        else if (next == Phase.THREE) arena.destroyOrbs(99);
        // Respawn crystal shield (more crystals each phase)
        final int crystalCount = next == Phase.TWO ? 5 : 6;
        new BukkitRunnable() {
            @Override public void run() { crystals.spawnRing(crystalCount); }
        }.runTaskLater(plugin, 40L);
        // Boss floats up briefly
        entity.setVelocity(entity.getVelocity().setY(0.6));
        // (invuln clear is scheduled at the top of this method)
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

    public double getIncomingDamageMultiplier(EntityDamageType type) {
        if (phase == Phase.TWO && type == EntityDamageType.ARROW) return 0.5;
        return 1.0;
    }

    /** Players within radius of the boss itself — used by attacks instead of the static arena center. */
    public List<Player> nearbyPlayers(double radius) {
        List<Player> list = new ArrayList<>();
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(entity.getLocation()) <= radius * radius) list.add(p);
        }
        return list;
    }

    public boolean isInvulnerable() { return System.currentTimeMillis() < invulnerableUntil; }
    public void forceClearInvuln() { this.invulnerableUntil = 0L; }
    public Phase getPhase() { return phase; }
    public LivingEntity getEntity() { return entity; }
    public VeilweaverArena getArena() { return arena; }
    public List<LivingEntity> getMinions() { return minions; }
    public List<LivingEntity> getEchoClones() { return echoClones; }
    public SoulEnchants getPlugin() { return plugin; }

    public void stop(boolean killed) {
        if (tickTask != null) try { tickTask.cancel(); } catch (Exception ignored) {}
        for (LivingEntity m : minions) if (m != null && !m.isDead()) m.remove();
        for (LivingEntity c : echoClones) if (c != null && !c.isDead()) c.remove();
        if (crystals != null) crystals.clearAll();
        arena.cleanup();
        if (killed) plugin.getServer().broadcastMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD
                + "✦ The Veilweaver has been silenced. ✦");
    }

    private void updateName() {
        double pct = entity.getHealth() / entity.getMaxHealth();
        int filled = (int) Math.round(pct * 20);
        StringBuilder bar = new StringBuilder(ChatColor.RED.toString());
        for (int i = 0; i < 20; i++) {
            if (i == filled) bar.append(ChatColor.DARK_GRAY);
            bar.append("|");
        }
        String phaseTag = phase == Phase.ONE ? "§7[I]" : phase == Phase.TWO ? "§5[II]" : "§4[III]";
        entity.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ The Veilweaver "
                + phaseTag + " " + bar + " §c" + (int) entity.getHealth() + "§7/§c" + (int) entity.getMaxHealth());
        entity.setCustomNameVisible(true);
    }

    public enum EntityDamageType { ARROW, MELEE, OTHER }
}
