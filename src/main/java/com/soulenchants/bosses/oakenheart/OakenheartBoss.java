package com.soulenchants.bosses.oakenheart;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.BossDamage;
import com.soulenchants.bosses.fsm.StateMachine;
import com.soulenchants.bosses.oakenheart.states.IdleState;
import com.soulenchants.bosses.oakenheart.states.PhaseThreeTransitionState;
import com.soulenchants.bosses.oakenheart.states.PhaseTwoTransitionState;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Oakenheart — the Forest Sovereign.
 *
 *   22,000 HP · 3 phases · 180 base bonus damage · summon-only via Ritual Sapling
 *
 *   Phase 1 (100-66%): Rooted
 *     - Thorn Lash (cone) + Root Bind (AoE slow)
 *   Phase 2 (66-33%):  Grove
 *     - Phase 1 attacks + Sapling Swarm (minion summon) + Falling Grove
 *       (oak-log meteors)
 *   Phase 3 (33-0%):   Withering
 *     - All prior + Briar Prison (1-player cobweb cage) + Withering Aura
 *       (punishes non-sneak movement in radius)
 *
 * Built FSM-native from day 1 on top of the framework introduced in the
 * IronGolem Phase-3 refactor.
 */
public final class OakenheartBoss {

    public enum Phase { ONE, TWO, THREE }

    public static final double MAX_HP = 22_000.0;
    public static final String NBT_OAKENHEART = "se_oakenheart_boss";

    private final SoulEnchants plugin;
    private final IronGolem entity;
    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private final StateMachine<OakenheartCtx> fsm;
    private final OakenheartCtx ctx;

    private Phase phase = Phase.ONE;
    private boolean invulnerable = false;
    private int ticks = 0;
    private final com.soulenchants.bosses.fsm.BossCommonBehaviors.DespawnState despawnState =
            new com.soulenchants.bosses.fsm.BossCommonBehaviors.DespawnState();

    private BukkitRunnable tickTask;

    public OakenheartBoss(SoulEnchants plugin, Location spawnLoc) {
        this.plugin = plugin;
        this.entity = (IronGolem) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);
        configureEntity();
        de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(entity);
        nbt.setBoolean(NBT_OAKENHEART, true);
        this.ctx = new OakenheartCtx(plugin, this);
        this.fsm = new StateMachine<>(new IdleState());
    }

    private void configureEntity() {
        double hp = BossDamage.bossHpOverride("oakenheart", MAX_HP);
        com.soulenchants.util.BossHealthHack.apply(entity, hp);
        entity.setRemoveWhenFarAway(false);
        entity.setPlayerCreated(false);
        updateName();
    }

    public void start() {
        plugin.getServer().broadcastMessage("");
        plugin.getServer().broadcastMessage(ChatColor.DARK_GREEN + "" + ChatColor.BOLD
                + "✦ OAKENHEART, THE FOREST SOVEREIGN AWAKENS ✦");
        plugin.getServer().broadcastMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC
                + "  \"The canopy has chosen. The roots are hungry.\"");
        plugin.getServer().broadcastMessage("");
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1.2f, 0.4f);
            if (p.getWorld().equals(entity.getWorld())
                    && p.getLocation().distanceSquared(entity.getLocation()) <= 80 * 80) {
                com.soulenchants.lunar.LunarFx.sendTitle(p,
                        ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "✦ OAKENHEART ✦",
                        ChatColor.GREEN + "" + ChatColor.ITALIC + "The Forest Sovereign wakes",
                        250L, 2200L, 500L, 1.3f);
            }
        }
        tickTask = new BukkitRunnable() {
            @Override public void run() { tick(); }
        };
        tickTask.runTaskTimer(plugin, 1L, 1L);
    }

    public void tick() {
        if (entity.isDead()) { stop(false); return; }
        ticks++;

        // Shared behaviors — targeting + melee enforcer + ambient halo
        com.soulenchants.bosses.fsm.BossCommonBehaviors.retargetNearestPlayer(entity, 30, ticks);
        com.soulenchants.bosses.fsm.BossCommonBehaviors.meleeEnforcer(
                entity, "oakenheart", 32, 4.0, 110, ticks);
        com.soulenchants.bosses.fsm.BossCommonBehaviors.ambientRing(
                entity, Material.LEAVES, 1.3, ticks);
        if (ticks % 5 == 0) updateName();

        tickDespawnCountdown();

        // Phase triggers — FSM force-transitions into invulnerable phase states
        if (phase == Phase.ONE && entity.getHealth() / entity.getMaxHealth() <= 0.66) {
            fsm.set(new PhaseTwoTransitionState(), ctx);
        } else if (phase == Phase.TWO && entity.getHealth() / entity.getMaxHealth() <= 0.33) {
            fsm.set(new PhaseThreeTransitionState(), ctx);
        }

        // Drive the FSM while not inside an invulnerable window. Phase-transition
        // states self-drive via their onTick so they still get ticked.
        if (!invulnerable
                || fsm.current() instanceof PhaseTwoTransitionState
                || fsm.current() instanceof PhaseThreeTransitionState) {
            fsm.tick(ctx);
        }
    }

    private void tickDespawnCountdown() {
        com.soulenchants.bosses.fsm.BossCommonBehaviors.DespawnResult r =
                com.soulenchants.bosses.fsm.BossCommonBehaviors.tickDespawn(
                        entity, despawnState, 20, ticks, 200, 200);
        switch (r) {
            case RESUMED:
                plugin.getServer().broadcastMessage(ChatColor.DARK_GREEN
                        + "✦ Oakenheart senses intruders — the roots hold firm.");
                break;
            case WARNING_ISSUED:
                plugin.getServer().broadcastMessage(ChatColor.GRAY
                        + "✦ Oakenheart finds no challengers — withdrawing into the canopy in 10s.");
                break;
            case DESPAWN_NOW:
                plugin.getServer().broadcastMessage(ChatColor.DARK_GRAY
                        + "✦ Oakenheart vanishes into the deep wood.");
                entity.remove();
                plugin.getOakenheartManager().clearActive();
                stop(false);
                break;
            default: break;
        }
    }

    public Phase getPhase() { return phase; }
    public boolean isPhaseTwoOrHigher()   { return phase != Phase.ONE; }
    public boolean isPhaseThree()         { return phase == Phase.THREE; }
    public void    enterPhaseTwo()        { this.phase = Phase.TWO; }
    public void    enterPhaseThree()      { this.phase = Phase.THREE; }

    public IronGolem getEntity() { return entity; }
    public SoulEnchants getPlugin() { return plugin; }
    public OakenheartCtx getCtx() { return ctx; }

    public boolean isInvulnerable() { return invulnerable; }
    public void setInvulnerable(boolean v) { this.invulnerable = v; }

    public void onDamageDealt(Player attacker, double damage) {
        damageDealt.merge(attacker.getUniqueId(), damage, Double::sum);
    }

    public Player getTopDamager() {
        if (damageDealt.isEmpty()) return null;
        UUID top = Collections.max(damageDealt.entrySet(), Map.Entry.comparingByValue()).getKey();
        return plugin.getServer().getPlayer(top);
    }

    public Map<UUID, Double> getDamageDealt() { return damageDealt; }

    public void stop(boolean killed) {
        if (tickTask != null) try { tickTask.cancel(); } catch (Throwable ignored) {}
        // Restore Oakenheart-owned temp blocks (Briar Prison cobwebs, etc.).
        com.soulenchants.util.TempBlockTracker.restoreByTag("oakenheart_briar");
        com.soulenchants.util.TempBlockTracker.restoreByTag("oakenheart_grove");
        if (killed) {
            plugin.getServer().broadcastMessage("");
            plugin.getServer().broadcastMessage(ChatColor.DARK_GREEN + "" + ChatColor.BOLD
                    + "✦ OAKENHEART HAS FALLEN ✦");
            plugin.getServer().broadcastMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC
                    + "  The canopy grieves. The saplings scatter. The season turns.");
            plugin.getServer().broadcastMessage("");
        }
    }

    private void updateName() {
        double pct = entity.getHealth() / entity.getMaxHealth();
        int filled = (int) Math.round(pct * 20);
        StringBuilder bar = new StringBuilder(ChatColor.DARK_GREEN.toString());
        for (int i = 0; i < 20; i++) {
            if (i == filled) bar.append(ChatColor.DARK_GRAY);
            bar.append("|");
        }
        String tag = phase == Phase.ONE ? "§a[I]" : phase == Phase.TWO ? "§2[II]" : "§4[III]";
        entity.setCustomName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "✦ Oakenheart "
                + tag + " " + bar + " §a" + (int) entity.getHealth() + "§7/§a" + (int) entity.getMaxHealth());
        entity.setCustomNameVisible(true);
    }
}
