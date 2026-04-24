package com.soulenchants.bosses.golem.states;

import com.soulenchants.bosses.BossDamage;
import com.soulenchants.bosses.BossEffects;
import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.golem.GolemCtx;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Dash attack. 30-tick telegraph paints a flame trail from boss → target's
 *  live position each 2 ticks so sidestepping dodges. Then launches the
 *  boss toward the locked target for 30 ticks; contact within 2 blocks
 *  applies 340 dmg + small pop-up. */
public final class RocketChargeState implements BossState<GolemCtx> {

    private static final int TELEGRAPH = 30;
    private static final int DASH      = 30;
    private Player target;
    private Location origin;
    private int dashHitTick = -1;

    @Override public String name() { return "RocketCharge"; }

    @Override
    public void onEnter(GolemCtx ctx) {
        this.target = ctx.pickTarget();
        this.origin = ctx.entity.getLocation().clone();
        BossEffects.titleNearby(origin, 30, "§6§l✦ ROCKET CHARGE ✦", "§eStep sideways — fast");
        ctx.entity.getWorld().playSound(origin, Sound.GHAST_CHARGE, 2.0f, 0.7f);
    }

    @Override
    public BossState<GolemCtx> onTick(GolemCtx ctx, int t) {
        if (target == null || ctx.entity.isDead()) return new IdleState();

        if (t < TELEGRAPH) {
            if (t % 2 == 0) paintFlameTrail(ctx);
            return null;
        }
        if (t == TELEGRAPH) {
            Vector dir = target.getLocation().toVector()
                    .subtract(ctx.entity.getLocation().toVector()).normalize().multiply(2.5);
            dir.setY(0.4);
            ctx.entity.setVelocity(dir);
            ctx.entity.getWorld().playSound(ctx.entity.getLocation(), Sound.GHAST_FIREBALL, 2.0f, 0.7f);
            dashHitTick = t;
        }
        // During dash window, check for contact
        if (dashHitTick >= 0 && t - dashHitTick <= DASH) {
            if (ctx.entity.getLocation().distanceSquared(target.getLocation()) < 4) {
                BossDamage.apply(target, "irongolem", "charge", 340, ctx.entity);
                target.setVelocity(new Vector(0, 0.6, 0));
                return new IdleState();
            }
        }
        if (t >= TELEGRAPH + DASH) return new IdleState();
        return null;
    }

    private void paintFlameTrail(GolemCtx ctx) {
        Location to = target.getLocation();
        Vector path = to.toVector().subtract(origin.toVector());
        double dist = path.length();
        if (dist < 0.1) return;
        path.normalize();
        for (double d = 0; d < dist; d += 0.6) {
            Location p = origin.clone().add(path.clone().multiply(d));
            p.getWorld().playEffect(p, Effect.MOBSPAWNER_FLAMES, 0);
        }
    }
}
