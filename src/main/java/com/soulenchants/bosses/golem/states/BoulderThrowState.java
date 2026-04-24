package com.soulenchants.bosses.golem.states;

import com.soulenchants.bosses.BoulderTracker;
import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.golem.GolemCtx;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Lob an iron-block projectile on a flat-ish arc. Instantaneous cast —
 *  flight resolution is handled by BoulderTracker. Stays active for 15
 *  ticks before returning to Idle so the boss has a brief visual recovery. */
public final class BoulderThrowState implements BossState<GolemCtx> {

    private static final int DURATION = 15;

    @Override public String name() { return "BoulderThrow"; }

    @Override
    public void onEnter(GolemCtx ctx) {
        Player target = ctx.pickTarget();
        if (target == null) return;
        Location origin = ctx.entity.getEyeLocation();
        FallingBlock boulder = ctx.entity.getWorld().spawnFallingBlock(origin, Material.IRON_BLOCK, (byte) 0);
        boulder.setDropItem(false);
        Vector targetPos = target.getEyeLocation().toVector()
                .add(target.getVelocity().clone().multiply(8));
        Vector toTarget = targetPos.subtract(origin.toVector());
        double horizontalDist = Math.sqrt(toTarget.getX() * toTarget.getX() + toTarget.getZ() * toTarget.getZ());
        double speed = 2.4;
        Vector dir = toTarget.normalize().multiply(speed);
        dir.setY(dir.getY() + Math.min(0.6, horizontalDist * 0.04));
        boulder.setVelocity(dir);
        new BoulderTracker(ctx.plugin, boulder, ctx.entity).start();
        ctx.entity.getWorld().playSound(ctx.entity.getLocation(), Sound.ZOMBIE_METAL, 2.0f, 0.5f);
    }

    @Override
    public BossState<GolemCtx> onTick(GolemCtx ctx, int t) {
        if (t >= DURATION) return new IdleState();
        return null;
    }
}
