package com.soulenchants.bosses.golem.states;

import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.golem.GolemCtx;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Yank every player within 15 blocks toward the boss. Sets them up for
 *  a follow-up Seismic Stomp. Instantaneous; stays active for 10 ticks
 *  so players can read the event before the next attack lands. */
public final class MagneticPullState implements BossState<GolemCtx> {

    private static final int DURATION = 10;

    @Override public String name() { return "MagneticPull"; }

    @Override
    public void onEnter(GolemCtx ctx) {
        Location loc = ctx.entity.getLocation();
        loc.getWorld().playSound(loc, Sound.PORTAL_TRIGGER, 2.0f, 0.7f);
        for (Player p : ctx.nearbyPlayers(15)) {
            Vector pull = loc.toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.8);
            pull.setY(0.4);
            p.setVelocity(pull);
        }
    }

    @Override
    public BossState<GolemCtx> onTick(GolemCtx ctx, int t) {
        if (t >= DURATION) return new IdleState();
        return null;
    }
}
