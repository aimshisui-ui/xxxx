package com.soulenchants.bosses.golem.states;

import com.soulenchants.bosses.BossDamage;
import com.soulenchants.bosses.BossEffects;
import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.golem.GolemCtx;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Airborne telegraph → falling impact. Paints a 7-block landing-zone
 *  ring for 15 ticks while the boss is airborne, then deals 250 dmg
 *  with linear falloff (center → edge) when it hits ground. */
public final class GroundSlamState implements BossState<GolemCtx> {

    private static final int TELEGRAPH_LEN = 15;
    private static final int MAX_TICK      = 40;

    @Override public String name() { return "GroundSlam"; }

    @Override
    public void onEnter(GolemCtx ctx) {
        ctx.entity.setVelocity(new Vector(0, 1.5, 0));
        BossEffects.titleNearby(ctx.entity.getLocation(), 30,
                "§6§l✦ GROUND SLAM ✦", "§eGet out of the ring");
        ctx.entity.getWorld().playSound(ctx.entity.getLocation(), Sound.IRONGOLEM_THROW, 2.0f, 0.6f);
    }

    @Override
    public BossState<GolemCtx> onTick(GolemCtx ctx, int t) {
        if (t < TELEGRAPH_LEN && t % 2 == 0) {
            Location ground = ctx.entity.getLocation();
            for (int i = 0; i < 28; i++) {
                double a = i * (Math.PI * 2 / 28);
                Location p = ground.clone().add(Math.cos(a) * 7.0, 0.1, Math.sin(a) * 7.0);
                p.getWorld().playEffect(p, Effect.MOBSPAWNER_FLAMES, 0);
            }
        }
        if (t < TELEGRAPH_LEN) return null;
        if (ctx.entity.isOnGround() || t > MAX_TICK) {
            Location loc = ctx.entity.getLocation();
            loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0f, false);
            for (int i = 0; i < 4; i++) {
                double a = i * (Math.PI / 2);
                Location s = loc.clone().add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
                s.getWorld().strikeLightningEffect(s);
            }
            BossEffects.shockwaveRing(ctx.plugin, loc, 7.0, Material.COBBLESTONE);
            BossEffects.particleBurst(loc.clone().add(0, 1, 0), Effect.LARGE_SMOKE, 80);
            BossEffects.titleNearby(loc, 30, "§6§l✦ SHOCKWAVE ✦", "§7Run further or take it");
            for (Player p : ctx.nearbyPlayers(7)) {
                double dist = p.getLocation().distance(loc);
                double distFactor = Math.max(0.3, 1.0 - dist * 0.10);
                BossDamage.applyScaled(p, "irongolem", "shockwave", 250, distFactor, ctx.entity);
                p.setVelocity(new Vector(0, 0.8, 0));
            }
            return new IdleState();
        }
        return null;
    }
}
