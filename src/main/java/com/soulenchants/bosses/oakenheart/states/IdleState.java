package com.soulenchants.bosses.oakenheart.states;

import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.oakenheart.OakenheartBoss;
import com.soulenchants.bosses.oakenheart.OakenheartCtx;

import java.util.ArrayList;
import java.util.List;

/** Default state — weighted attack picker with per-attack cooldowns.
 *  Attacks unlock as the boss phases up:
 *    phase 1 → ThornLash, RootBind
 *    phase 2 → +SaplingSwarm, FallingGrove
 *    phase 3 → +BriarPrison, WitheringAura
 *  Idle picks a new attack every 6-9s. */
public final class IdleState implements BossState<OakenheartCtx> {

    private int cdThorn, cdRoot, cdSwarm, cdGrove, cdBriar, cdAura;
    private int nextPickIn = 80;   // 4s initial delay

    @Override public String name() { return "Idle"; }

    @Override
    public BossState<OakenheartCtx> onTick(OakenheartCtx ctx, int t) {
        if (cdThorn > 0) cdThorn--;
        if (cdRoot  > 0) cdRoot--;
        if (cdSwarm > 0) cdSwarm--;
        if (cdGrove > 0) cdGrove--;
        if (cdBriar > 0) cdBriar--;
        if (cdAura  > 0) cdAura--;

        if (--nextPickIn > 0) return null;
        nextPickIn = 120 + ctx.rng.nextInt(60);

        List<String> pool = new ArrayList<>();
        if (cdThorn <= 0) for (int i = 0; i < 5; i++) pool.add("thorn");
        if (cdRoot  <= 0) for (int i = 0; i < 3; i++) pool.add("root");
        if (ctx.boss.isPhaseTwoOrHigher()) {
            if (cdSwarm <= 0) for (int i = 0; i < 2; i++) pool.add("swarm");
            if (cdGrove <= 0) for (int i = 0; i < 3; i++) pool.add("grove");
        }
        if (ctx.boss.isPhaseThree()) {
            if (cdBriar <= 0)                         pool.add("briar");
            if (cdAura  <= 0) for (int i = 0; i < 2; i++) pool.add("aura");
        }
        if (pool.isEmpty()) return null;

        String pick = pool.get(ctx.rng.nextInt(pool.size()));
        switch (pick) {
            case "thorn": cdThorn = 200; return new ThornLashState();
            case "root":  cdRoot  = 260; return new RootBindState();
            case "swarm": cdSwarm = 600; return new SaplingSwarmState();
            case "grove": cdGrove = 340; return new FallingGroveState();
            case "briar": cdBriar = 420; return new BriarPrisonState();
            case "aura":  cdAura  = 360; return new WitheringAuraState();
        }
        return null;
    }
}
