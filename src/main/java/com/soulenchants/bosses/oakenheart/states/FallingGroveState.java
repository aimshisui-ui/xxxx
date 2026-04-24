package com.soulenchants.bosses.oakenheart.states;

import com.soulenchants.bosses.BossDamage;
import com.soulenchants.bosses.BossEffects;
import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.oakenheart.OakenheartCtx;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** 40-tick telegraph — paints 3 crosshairs on the ground under random
 *  players. At impact, 3 oak logs drop from the sky as FallingBlocks.
 *  Anyone still in the crosshair ring takes 220 dmg + knockup. */
public final class FallingGroveState implements BossState<OakenheartCtx> {

    private static final int TELEGRAPH = 40;
    private static final int IMPACT_TICKS = 30;  // extra after telegraph while logs fall
    private static final int TOTAL = TELEGRAPH + IMPACT_TICKS;

    private final List<Location> markers = new ArrayList<>();

    @Override public String name() { return "FallingGrove"; }

    @Override
    public void onEnter(OakenheartCtx ctx) {
        List<Player> pool = ctx.nearbyPlayers(40);
        int want = Math.min(3, pool.size());
        for (int i = 0; i < want; i++) {
            Player p = pool.get(ctx.rng.nextInt(pool.size()));
            markers.add(p.getLocation());
        }
        ctx.entity.getWorld().playSound(ctx.entity.getLocation(), Sound.ZOMBIE_WOODBREAK, 2.0f, 0.4f);
        BossEffects.titleNearby(ctx.entity.getLocation(), 40,
                "§2§l✦ FALLING GROVE ✦", "§aMove off the crosshairs");
    }

    @Override
    public BossState<OakenheartCtx> onTick(OakenheartCtx ctx, int t) {
        if (t < TELEGRAPH) {
            if (t % 3 == 0) {
                for (Location m : markers) {
                    int samples = 20;
                    for (int i = 0; i < samples; i++) {
                        double a = (Math.PI * 2 * i) / samples;
                        Location p = m.clone().add(Math.cos(a) * 3.0, 0.1, Math.sin(a) * 3.0);
                        p.getWorld().playEffect(p, Effect.MOBSPAWNER_FLAMES, 0);
                    }
                }
            }
            return null;
        }
        if (t == TELEGRAPH) {
            for (Location m : markers) {
                Location drop = m.clone().add(0, 25, 0);
                try {
                    FallingBlock log = m.getWorld().spawnFallingBlock(drop, Material.LOG, (byte) 0);
                    log.setDropItem(false);
                    log.setVelocity(new Vector(0, -1.8, 0));
                } catch (Throwable ignored) {}
            }
        }
        if (t > TELEGRAPH && t % 4 == 0) {
            // Damage any player currently inside a crosshair ring
            for (Location m : markers) {
                for (Player p : ctx.nearbyPlayers(40)) {
                    if (!p.getWorld().equals(m.getWorld())) continue;
                    if (p.getLocation().distanceSquared(m) < 3 * 3
                            && Math.abs(p.getLocation().getY() - m.getY()) < 4) {
                        BossDamage.apply(p, "oakenheart", "falling_grove", 220, ctx.entity);
                        p.setVelocity(p.getVelocity().setY(0.8));
                        m.getWorld().playSound(m, Sound.EXPLODE, 1.5f, 0.6f);
                    }
                }
            }
        }
        if (t >= TOTAL) {
            markers.clear();
            return new IdleState();
        }
        return null;
    }
}
