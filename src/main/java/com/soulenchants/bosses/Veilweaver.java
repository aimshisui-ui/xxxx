package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.attacks.VeilweaverAttacks;
import com.soulenchants.bosses.fsm.StateMachine;
import com.soulenchants.bosses.veilweaver.VeilweaverCtx;
import com.soulenchants.bosses.veilweaver.states.IdleState;
import com.soulenchants.bosses.veilweaver.states.PhaseTransitionState;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Post-Phase-3 FSM refactor — attack-picking + phase-transition behavior
 * now lives in StateMachine&lt;VeilweaverCtx&gt;. Individual attacks are
 * still static calls to VeilweaverAttacks because they were already
 * extracted there pre-refactor; IdleState fires them + tracks cooldowns.
 */
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
    private final StateMachine<VeilweaverCtx> fsm;
    private final VeilweaverCtx ctx;

    /** Self-expiring invuln. Compared against System.currentTimeMillis().
     *  No abandoned BukkitRunnables, no stuck-on bug. */
    private long invulnerableUntil = 0L;
    private boolean usedFinalBind = false;
    private int ticks = 0;
    private int idleTicks = 0;
    private boolean despawnAnnounced = false;
    private int despawnAt = -1;

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
        this.ctx = new VeilweaverCtx(plugin, this);
        this.fsm = new StateMachine<>(new IdleState());
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

        // Phase transitions — force-transition the FSM into an invuln window
        double pct = entity.getHealth() / entity.getMaxHealth();
        if (phase == Phase.ONE && pct <= 0.65) {
            fsm.set(new PhaseTransitionState(Phase.TWO), ctx);
        } else if (phase == Phase.TWO && pct <= 0.30) {
            fsm.set(new PhaseTransitionState(Phase.THREE), ctx);
        }

        // Final Thread Bind triggers once below 10%
        if (!usedFinalBind && pct <= 0.10 && phase == Phase.THREE) {
            usedFinalBind = true;
            VeilweaverAttacks.finalThreadBind(this);
        }

        // Drive the FSM — IdleState picks + fires attacks; phase-transition
        // states own their invuln window and self-transition back to Idle.
        // Skip when invulnerable unless we're inside a PhaseTransitionState.
        if (System.currentTimeMillis() < invulnerableUntil
                && !(fsm.current() instanceof PhaseTransitionState)) return;
        fsm.tick(ctx);
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
    /** Start a self-expiring invuln window. Called from PhaseTransitionState. */
    public void setInvulnerableFor(long millis) {
        this.invulnerableUntil = System.currentTimeMillis() + Math.max(0, millis);
    }
    /** Set the current phase without running the broadcast (the FSM state
     *  handles that). */
    public void enterPhase(Phase p) { this.phase = p; }
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
