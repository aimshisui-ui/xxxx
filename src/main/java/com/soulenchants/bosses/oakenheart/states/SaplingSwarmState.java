package com.soulenchants.bosses.oakenheart.states;

import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.oakenheart.OakenheartCtx;
import com.soulenchants.mobs.CustomMob;
import com.soulenchants.mobs.MobRegistry;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;

/** 20-tick pulse, then summon 4 (phase 2) or 6 (phase 3) sapling_sprout
 *  custom mobs in a ring around the boss. The adds chase nearby players
 *  on their own via CustomMob tick behavior. Phase-2-locked attack. */
public final class SaplingSwarmState implements BossState<OakenheartCtx> {

    private static final int TELEGRAPH = 20;
    private static final int TOTAL     = 30;

    @Override public String name() { return "SaplingSwarm"; }

    @Override
    public void onEnter(OakenheartCtx ctx) {
        ctx.entity.getWorld().playSound(ctx.entity.getLocation(), Sound.ZOMBIE_WOOD, 1.6f, 0.7f);
    }

    @Override
    public BossState<OakenheartCtx> onTick(OakenheartCtx ctx, int t) {
        if (t < TELEGRAPH) {
            if (t % 2 == 0) {
                Location loc = ctx.entity.getLocation();
                int samples = 20;
                for (int i = 0; i < samples; i++) {
                    double a = (Math.PI * 2 * i) / samples;
                    Location p = loc.clone().add(Math.cos(a) * 3.0, 0.3, Math.sin(a) * 3.0);
                    p.getWorld().playEffect(p, Effect.STEP_SOUND, org.bukkit.Material.SAPLING.getId());
                }
            }
            return null;
        }
        if (t == TELEGRAPH) {
            int count = ctx.boss.isPhaseThree() ? 6 : 4;
            CustomMob mob = MobRegistry.get("sapling_sprout");
            if (mob != null) {
                Location center = ctx.entity.getLocation();
                for (int i = 0; i < count; i++) {
                    double a = (Math.PI * 2 * i) / count;
                    Location spawn = center.clone().add(Math.cos(a) * 3.5, 0, Math.sin(a) * 3.5);
                    mob.spawn(spawn);
                }
                center.getWorld().playSound(center, Sound.DIG_GRASS, 2.0f, 1.0f);
            }
        }
        if (t >= TOTAL) return new IdleState();
        return null;
    }
}
