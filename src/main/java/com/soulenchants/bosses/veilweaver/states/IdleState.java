package com.soulenchants.bosses.veilweaver.states;

import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.bosses.attacks.VeilweaverAttacks;
import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.veilweaver.VeilweaverCtx;

import java.util.ArrayList;
import java.util.List;

/** Veilweaver's default state. Unlike the Golem model, Veilweaver's
 *  attacks are fire-and-forget static calls to VeilweaverAttacks — each
 *  one spawns its own internal BukkitRunnable for the animation/damage
 *  window. That means IdleState just picks an attack, fires it, sets a
 *  cast-lockout timer, and stays in Idle. Phase transitions are handled
 *  by the FSM directly from the boss class.
 *
 *  Attack pool + weights are phase-gated and match the original
 *  Veilweaver.runAttacks() logic verbatim. */
public final class IdleState implements BossState<VeilweaverCtx> {

    private int cdThreadLash, cdShatterBolt, cdMinionWeave, cdDimensionalRift,
            cdLoomLaser, cdEchoClones, cdRealityFracture, cdApocalypseWeave;
    private int nextPickIn = 80;

    @Override public String name() { return "Idle"; }

    @Override
    public BossState<VeilweaverCtx> onTick(VeilweaverCtx ctx, int t) {
        if (cdThreadLash > 0)      cdThreadLash--;
        if (cdShatterBolt > 0)     cdShatterBolt--;
        if (cdMinionWeave > 0)     cdMinionWeave--;
        if (cdDimensionalRift > 0) cdDimensionalRift--;
        if (cdLoomLaser > 0)       cdLoomLaser--;
        if (cdEchoClones > 0)      cdEchoClones--;
        if (cdRealityFracture > 0) cdRealityFracture--;
        if (cdApocalypseWeave > 0) cdApocalypseWeave--;

        if (--nextPickIn > 0) return null;
        nextPickIn = 90 + ctx.rng.nextInt(50);

        List<String> pool = new ArrayList<>();
        if (cdThreadLash <= 0)  for (int i = 0; i < 7; i++) pool.add("lash");
        if (cdShatterBolt <= 0) for (int i = 0; i < 4; i++) pool.add("bolt");
        if (cdMinionWeave <= 0)                             pool.add("minion");
        Veilweaver.Phase p = ctx.boss.getPhase();
        if (p == Veilweaver.Phase.TWO || p == Veilweaver.Phase.THREE) {
            if (cdDimensionalRift <= 0) for (int i = 0; i < 3; i++) pool.add("rift");
            if (cdLoomLaser <= 0)       for (int i = 0; i < 2; i++) pool.add("laser");
            if (cdEchoClones <= 0)                                  pool.add("clones");
        }
        if (p == Veilweaver.Phase.THREE) {
            if (cdRealityFracture <= 0) for (int i = 0; i < 2; i++) pool.add("fracture");
            if (cdApocalypseWeave <= 0)                             pool.add("apoc");
        }
        if (pool.isEmpty()) return null;

        switch (pool.get(ctx.rng.nextInt(pool.size()))) {
            case "lash":     cdThreadLash = 70 + ctx.rng.nextInt(30); VeilweaverAttacks.threadLash(ctx.boss); break;
            case "bolt":     cdShatterBolt = 160;                      VeilweaverAttacks.shatterBolt(ctx.boss); break;
            case "minion":   cdMinionWeave = 900;                      VeilweaverAttacks.minionWeave(ctx.boss); break;
            case "rift":     cdDimensionalRift = 320;                  VeilweaverAttacks.dimensionalRift(ctx.boss); break;
            case "laser":    cdLoomLaser = 420;                        VeilweaverAttacks.loomLaser(ctx.boss); break;
            case "clones":   cdEchoClones = 1200;                      VeilweaverAttacks.echoClones(ctx.boss); break;
            case "fracture": cdRealityFracture = 260;                  VeilweaverAttacks.realityFracture(ctx.boss); break;
            case "apoc":     cdApocalypseWeave = 800;                  VeilweaverAttacks.apocalypseWeave(ctx.boss); break;
        }
        return null;
    }
}
