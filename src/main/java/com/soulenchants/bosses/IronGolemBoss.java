package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.fsm.StateMachine;
import com.soulenchants.bosses.golem.GolemCtx;
import com.soulenchants.bosses.golem.states.IdleState;
import com.soulenchants.bosses.golem.states.IronWallState;
import com.soulenchants.bosses.golem.states.PhaseTransitionState;
import com.soulenchants.bosses.golem.states.ReinforceState;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ironheart Colossus — mid-late-game boss. Post-Phase-3 refactor: all
 * behaviour lives in a {@link StateMachine}&lt;{@link GolemCtx}&gt; driven
 * from the boss's 1-tick BukkitRunnable. Each attack is its own
 * {@code golem.states.*State} class; this class is now just entity
 * config, HP/phase tracking, ambient effects, and the FSM harness.
 *
 * What used to be ~550 lines of switch-case + nested BukkitRunnables
 * is now ~150 lines of orchestration plus 7 small state files.
 */
public class IronGolemBoss {

    public enum Phase { ONE, TWO }

    public static final double MAX_HP = 8000.0;
    public static final String NBT_IRONGOLEM = "se_irongolem_boss";

    private final SoulEnchants plugin;
    private final IronGolem entity;
    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private final IronGolemMinions minions;
    private final StateMachine<GolemCtx> fsm;
    private final GolemCtx ctx;

    private final com.soulenchants.bosses.fsm.BossCommonBehaviors.DespawnState despawnState =
            new com.soulenchants.bosses.fsm.BossCommonBehaviors.DespawnState();
    private Phase phase = Phase.ONE;
    private boolean invulnerable = false;
    private boolean usedReinforce = false;
    private int ticks = 0;

    private BukkitRunnable tickTask;

    public IronGolemBoss(SoulEnchants plugin, Location spawnLoc) {
        this.plugin = plugin;
        this.entity = (IronGolem) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);
        configureEntity();
        de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(entity);
        nbt.setBoolean(NBT_IRONGOLEM, true);
        this.minions = new IronGolemMinions(plugin, this);
        this.ctx = new GolemCtx(plugin, this);
        this.fsm = new StateMachine<>(new IdleState());
    }

    public IronGolemMinions getMinions() { return minions; }

    private void configureEntity() {
        double hp = BossDamage.bossHpOverride("irongolem", MAX_HP);
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
        new BukkitRunnable() {
            @Override public void run() { minions.start(); }
        }.runTaskLater(plugin, 40L);
    }

    /** Master tick — runs ambient effects, melee enforcer, despawn
     *  countdown, and drives the FSM. Attack selection + execution lives
     *  entirely inside the state machine. */
    public void tick() {
        if (entity.isDead()) { stop(false); return; }
        ticks++;

        // ── Ambient / universal behavior via BossCommonBehaviors helpers ──
        com.soulenchants.bosses.fsm.BossCommonBehaviors.retargetNearestPlayer(entity, 30, ticks);
        com.soulenchants.bosses.fsm.BossCommonBehaviors.meleeEnforcer(
                entity, "irongolem", 28, 4.0, 70, ticks);
        com.soulenchants.bosses.fsm.BossCommonBehaviors.ambientRing(
                entity, Material.IRON_BLOCK, 1.0, ticks);
        if (ticks % 5 == 0) updateName();

        // No-players despawn countdown — centralized state machine
        tickDespawnCountdown();

        // ── Phase triggers — force-transition the FSM into invulnerable states ──

        if (phase == Phase.ONE && entity.getHealth() / entity.getMaxHealth() <= 0.50) {
            fsm.set(new PhaseTransitionState(), ctx);
        }
        if (!usedReinforce && phase == Phase.TWO
                && entity.getHealth() / entity.getMaxHealth() < 0.25) {
            usedReinforce = true;
            fsm.set(new ReinforceState(), ctx);
        }

        // Drive the FSM for the current state's onTick
        if (!invulnerable || fsm.current() instanceof PhaseTransitionState
                || fsm.current() instanceof ReinforceState) {
            fsm.tick(ctx);
        }
    }

    /** Idle-timer-based auto-despawn when no players are near. Delegates to
     *  BossCommonBehaviors.tickDespawn() and translates the result into
     *  boss-specific broadcast flavor. */
    private void tickDespawnCountdown() {
        com.soulenchants.bosses.fsm.BossCommonBehaviors.DespawnResult r =
                com.soulenchants.bosses.fsm.BossCommonBehaviors.tickDespawn(
                        entity, despawnState, 15, ticks, 100, 200);
        switch (r) {
            case RESUMED:
                plugin.getServer().broadcastMessage(ChatColor.GOLD
                        + "✦ The Ironheart Colossus senses prey — its retreat is cancelled.");
                break;
            case WARNING_ISSUED:
                plugin.getServer().broadcastMessage(ChatColor.GRAY
                        + "✦ The Ironheart Colossus finds no challengers — retreating in 10s.");
                break;
            case DESPAWN_NOW:
                plugin.getServer().broadcastMessage(ChatColor.DARK_GRAY
                        + "✦ The Ironheart Colossus vanishes into the dust.");
                entity.remove();
                plugin.getIronGolemManager().clearActive();
                stop(false);
                break;
            default: break;
        }
    }

    // ── Public accessors for states ──

    public List<Player> nearbyPlayers(double radius) { return ctx.nearbyPlayers(radius); }

    public boolean isPhaseTwo() { return phase == Phase.TWO; }

    /** Called from PhaseTransitionState.onEnter — flips the phase flag
     *  without running the transition cinematic (that's the state's job). */
    public void enterPhaseTwo() { this.phase = Phase.TWO; }

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
    public void setInvulnerable(boolean v) { this.invulnerable = v; }
    public void forceClearInvuln() { this.invulnerable = false; }
    public IronGolem getEntity() { return entity; }
    public SoulEnchants getPlugin() { return plugin; }

    public void stop(boolean killed) {
        if (tickTask != null) try { tickTask.cancel(); } catch (Exception ignored) {}
        if (minions != null) minions.stop();
        // Force-restore every TempBlockTracker entry tagged as this boss's
        // Iron Wall — covers mid-fight despawn before natural sweeper.
        int restored = com.soulenchants.util.TempBlockTracker.restoreByTag(IronWallState.TAG);
        if (restored > 0) plugin.getLogger().info("[IronGolem] stop() restored " + restored + " Iron Wall blocks");
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
