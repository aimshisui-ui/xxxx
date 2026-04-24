package com.soulenchants.bosses.fsm;

import com.soulenchants.bosses.BossDamage;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared tick-time behaviors that every FSM-driven boss reuses — extracted
 * from the triplet of boilerplate that used to live in IronGolemBoss,
 * Veilweaver, and OakenheartBoss. Each boss class adopted this pattern by
 * hand previously; v1.5 centralizes the boilerplate here so adding a new
 * boss is <100 lines of unique mechanics.
 *
 * Usage: a boss's tick() method calls the relevant static helpers:
 *
 *   BossCommonBehaviors.retargetNearestPlayer(entity, 30, ticks);
 *   BossCommonBehaviors.meleeEnforcer(entity, bossId, 28, 4.0, 70, ticks);
 *   BossCommonBehaviors.ambientRing(entity, Material.IRON_BLOCK, 1.0, ticks);
 *
 * Each helper returns void and mutates state on the entity / world. They
 * noop on dead entities so callers don't need null/dead guards.
 */
public final class BossCommonBehaviors {

    private BossCommonBehaviors() {}

    /** Every `cadence` ticks, find the nearest player within `radius` and
     *  force the monster's aggro target onto them. Covers the spider-AI
     *  daylight-deaggro hole and any Bukkit drift. */
    public static void retargetNearestPlayer(LivingEntity entity, double radius, int ticks) {
        retargetNearestPlayer(entity, radius, ticks, 20);
    }

    public static void retargetNearestPlayer(LivingEntity entity, double radius, int ticks, int cadence) {
        if (entity == null || entity.isDead()) return;
        if (ticks % cadence != 0) return;
        if (!(entity instanceof Monster)) return;
        Player nearest = null;
        double bestSq = radius * radius;
        for (Player pl : entity.getWorld().getPlayers()) {
            double d = pl.getLocation().distanceSquared(entity.getLocation());
            if (d < bestSq) { bestSq = d; nearest = pl; }
        }
        if (nearest != null) {
            try { ((Monster) entity).setTarget(nearest); } catch (Throwable ignored) {}
        }
    }

    /** Every `cadence` ticks, if a player is within `reach` blocks, apply
     *  `damage` to them and play a hit sound. Overrides vanilla-AI swing
     *  gaps so the boss doesn't stall for 2-3 seconds between swings. */
    public static void meleeEnforcer(LivingEntity entity, String bossId, int cadence,
                                     double reach, double damage, int ticks) {
        if (entity == null || entity.isDead()) return;
        if (ticks % cadence != 0) return;
        Player closest = null; double bestSq = Double.MAX_VALUE;
        for (Player pl : entity.getWorld().getPlayers()) {
            double d = pl.getLocation().distanceSquared(entity.getLocation());
            if (d < bestSq) { bestSq = d; closest = pl; }
        }
        if (closest != null && bestSq <= reach * reach) {
            entity.getWorld().playSound(entity.getLocation(), Sound.HURT_FLESH, 1.0f, 0.8f);
            BossDamage.apply(closest, bossId, "melee", damage, entity);
        }
    }

    /** Particle halo around the boss's chest. Call every tick; renders a
     *  3-point ring every 4th tick. Material controls the block-break
     *  visual — STONE for earthy bosses, IRON_BLOCK for metallic, LEAVES
     *  for plant-themed. */
    public static void ambientRing(LivingEntity entity, Material material, double radius, int ticks) {
        if (entity == null || entity.isDead()) return;
        if (ticks % 4 != 0) return;
        Location loc = entity.getLocation().add(0, 1.5, 0);
        for (int i = 0; i < 3; i++) {
            double a = ticks * 0.15 + i * (Math.PI * 2 / 3);
            Location p = loc.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            p.getWorld().playEffect(p, Effect.STEP_SOUND, material.getId());
        }
    }

    /** Stateful no-players despawn countdown. Returns {@link DespawnResult}
     *  so the caller can log / broadcast when state changes. Persistence of
     *  the idleTicks / despawnAt values is the caller's responsibility (they
     *  live on the boss instance). */
    public static final class DespawnState {
        public int idleTicks;
        public boolean announced;
        public int despawnAt;
        public DespawnState() { reset(); }
        public void reset() { this.idleTicks = 0; this.announced = false; this.despawnAt = -1; }
    }

    public enum DespawnResult {
        NO_ACTION,          // nothing happened this tick
        RESUMED,            // players returned — retreat cancelled
        WARNING_ISSUED,     // 10s warning just fired
        DESPAWN_NOW         // time's up, caller should remove entity
    }

    /** Run the despawn countdown. Call every tick with the current tick
     *  count; internal logic only fires every 20 ticks. */
    public static DespawnResult tickDespawn(LivingEntity entity, DespawnState state,
                                            double checkRadius, int ticks,
                                            int graceWindow, int warnWindow) {
        if (ticks % 20 != 0) return DespawnResult.NO_ACTION;
        if (entity == null || entity.isDead()) return DespawnResult.NO_ACTION;
        boolean any = !nearbyPlayers(entity, checkRadius).isEmpty();
        if (any) {
            state.idleTicks = 0;
            if (state.announced) {
                state.announced = false;
                state.despawnAt = -1;
                return DespawnResult.RESUMED;
            }
            return DespawnResult.NO_ACTION;
        }
        state.idleTicks += 20;
        if (!state.announced && state.idleTicks >= graceWindow) {
            state.announced = true;
            state.despawnAt = ticks + warnWindow;
            return DespawnResult.WARNING_ISSUED;
        }
        if (state.announced && ticks >= state.despawnAt) {
            return DespawnResult.DESPAWN_NOW;
        }
        return DespawnResult.NO_ACTION;
    }

    /** Players within `radius` blocks of the entity in its current world. */
    public static List<Player> nearbyPlayers(LivingEntity entity, double radius) {
        List<Player> list = new ArrayList<>();
        if (entity == null) return list;
        double rSq = radius * radius;
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(entity.getLocation()) <= rSq) list.add(p);
        }
        return list;
    }

    /** Render the 20-segment HP bar as the entity's custom name. `primary`
     *  colors the filled part of the bar; phaseTag is the bracketed prefix
     *  (e.g. "§7[I]", "§4[III]"). */
    public static void renderHpBar(LivingEntity entity, ChatColor primary, String phaseTag,
                                   ChatColor nameColor, String displayName) {
        if (entity == null || entity.isDead()) return;
        double pct = entity.getHealth() / entity.getMaxHealth();
        int filled = (int) Math.round(pct * 20);
        StringBuilder bar = new StringBuilder(primary.toString());
        for (int i = 0; i < 20; i++) {
            if (i == filled) bar.append(ChatColor.DARK_GRAY);
            bar.append("|");
        }
        String hpText = (int) entity.getHealth() + "§7/§e" + (int) entity.getMaxHealth();
        entity.setCustomName(nameColor + "" + ChatColor.BOLD + "✦ " + displayName + " "
                + phaseTag + " " + bar + " §e" + hpText);
        entity.setCustomNameVisible(true);
    }
}
