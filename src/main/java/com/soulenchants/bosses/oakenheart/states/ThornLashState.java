package com.soulenchants.bosses.oakenheart.states;

import com.soulenchants.bosses.BossDamage;
import com.soulenchants.bosses.BossEffects;
import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.oakenheart.OakenheartCtx;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** 20-tick telegraph painting a thorn-dusted cone from the boss's facing
 *  direction, then 180-damage cone hit in a 12-block arc (50° spread). */
public final class ThornLashState implements BossState<OakenheartCtx> {

    private static final int TELEGRAPH = 20;
    private static final int TOTAL     = 28;
    private Vector forward;

    @Override public String name() { return "ThornLash"; }

    @Override
    public void onEnter(OakenheartCtx ctx) {
        // Lock forward vector at state-enter so the cone doesn't follow the
        // target while the telegraph plays.
        Player target = ctx.pickTarget();
        Location origin = ctx.entity.getLocation();
        if (target != null) {
            this.forward = target.getLocation().toVector().subtract(origin.toVector()).setY(0);
            if (forward.lengthSquared() > 0.0001) forward.normalize();
            else forward = new Vector(1, 0, 0);
        } else {
            forward = origin.getDirection().setY(0).normalize();
        }
        origin.getWorld().playSound(origin, Sound.DIG_WOOD, 2.0f, 0.5f);
        BossEffects.titleNearby(origin, 25, "§2§l✦ THORN LASH ✦", "§aSidestep the cone");
    }

    @Override
    public BossState<OakenheartCtx> onTick(OakenheartCtx ctx, int t) {
        Location origin = ctx.entity.getLocation();

        if (t < TELEGRAPH) {
            // Paint particle arc every 2 ticks
            if (t % 2 == 0) {
                double fillLen = (t / (double) TELEGRAPH) * 12.0;
                int samples = 14;
                for (int i = 0; i < samples; i++) {
                    double theta = (i / (double) samples) * Math.PI / 3.6 - Math.PI / 7.2;
                    Vector dir = rotateY(forward, theta).multiply(fillLen);
                    Location p = origin.clone().add(dir.getX(), 0.3, dir.getZ());
                    p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.LEAVES.getId());
                }
            }
            return null;
        }
        if (t == TELEGRAPH) {
            // Resolve — hit every player inside the 12-block / 50° cone
            origin.getWorld().playSound(origin, Sound.EXPLODE, 1.5f, 0.9f);
            for (Player p : ctx.nearbyPlayers(13)) {
                Vector toP = p.getLocation().toVector().subtract(origin.toVector()).setY(0);
                double dist = toP.length();
                if (dist < 0.1) continue;
                toP.normalize();
                double dot = toP.dot(forward);
                if (dot < Math.cos(Math.PI / 3.6)) continue;   // ~50° cone
                BossDamage.apply(p, "oakenheart", "thorn_lash", 180, ctx.entity);
                p.setVelocity(forward.clone().multiply(0.9).setY(0.3));
            }
            // Heavy particle burst along the cone
            for (int i = 0; i < 40; i++) {
                double d = Math.random() * 12.0;
                double theta = (Math.random() - 0.5) * Math.PI / 3.6;
                Vector dir = rotateY(forward, theta).multiply(d);
                Location p = origin.clone().add(dir.getX(), 0.3 + Math.random() * 1.2, dir.getZ());
                p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.CACTUS.getId());
            }
        }
        if (t >= TOTAL) return new IdleState();
        return null;
    }

    private static Vector rotateY(Vector v, double radians) {
        double cos = Math.cos(radians), sin = Math.sin(radians);
        return new Vector(v.getX() * cos - v.getZ() * sin, v.getY(), v.getX() * sin + v.getZ() * cos);
    }
}
