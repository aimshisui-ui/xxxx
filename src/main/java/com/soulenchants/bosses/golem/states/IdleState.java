package com.soulenchants.bosses.golem.states;

import com.soulenchants.bosses.IronGolemBoss;
import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.golem.GolemCtx;

import java.util.ArrayList;
import java.util.List;

/**
 * Default state — picks the next attack on a per-attack cooldown + weighted
 * pool, then transitions into that attack's state. Matches the original
 * runAttacks() weighted-pool behavior exactly.
 *
 * Per-attack cooldowns are stored here as fields (not in individual state
 * classes) because IdleState is the re-entry point for every attack; the
 * cooldown value has to persist across attack-state transitions.
 */
public final class IdleState implements BossState<GolemCtx> {

    private int cdStomp, cdBoulder, cdRocket, cdMagnetic, cdIronWall, cdSlam;
    private int nextPickIn = 100;   // 5s initial delay so the boss can settle

    public IdleState() {}

    @Override public String name() { return "Idle"; }

    @Override
    public BossState<GolemCtx> onTick(GolemCtx ctx, int ticksInState) {
        // Tick all per-attack cooldowns down.
        if (cdStomp > 0)    cdStomp--;
        if (cdBoulder > 0)  cdBoulder--;
        if (cdRocket > 0)   cdRocket--;
        if (cdMagnetic > 0) cdMagnetic--;
        if (cdIronWall > 0) cdIronWall--;
        if (cdSlam > 0)     cdSlam--;

        if (--nextPickIn > 0) return null;
        nextPickIn = 120 + ctx.rng.nextInt(60);   // 6-9s between picks

        List<String> pool = new ArrayList<>();
        if (cdStomp <= 0)   for (int i = 0; i < 6; i++) pool.add("stomp");
        if (cdBoulder <= 0) for (int i = 0; i < 4; i++) pool.add("boulder");
        boolean phaseTwo = ctx.boss.isPhaseTwo();
        if (phaseTwo) {
            if (cdRocket <= 0)   for (int i = 0; i < 3; i++) pool.add("rocket");
            if (cdMagnetic <= 0) for (int i = 0; i < 2; i++) pool.add("magnetic");
            if (cdIronWall <= 0)                            pool.add("wall");
            if (cdSlam <= 0)                                pool.add("slam");
        }
        if (pool.isEmpty()) return null;

        String pick = pool.get(ctx.rng.nextInt(pool.size()));
        switch (pick) {
            case "stomp":    cdStomp = 200;    return new SeismicStompState();
            case "boulder":  cdBoulder = 260;  return new BoulderThrowState();
            case "rocket":   cdRocket = 240;   return new RocketChargeState();
            case "magnetic": cdMagnetic = 320; return new MagneticPullState();
            case "wall":     cdIronWall = 400; return new IronWallState();
            case "slam":     cdSlam = 380;     return new GroundSlamState();
        }
        return null;
    }
}
