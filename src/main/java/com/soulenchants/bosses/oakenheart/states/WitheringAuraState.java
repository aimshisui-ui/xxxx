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

/** Phase-3-exclusive pressure attack. For 80 ticks (4 seconds), every 20
 *  ticks damages every non-sneaking player within 16 blocks for 80 dmg.
 *  Sneaking avoids the tick — rewards careful positioning. */
public final class WitheringAuraState implements BossState<OakenheartCtx> {

    private static final int DURATION      = 80;
    private static final int DAMAGE_PERIOD = 20;
    private static final double RADIUS     = 16.0;

    @Override public String name() { return "WitheringAura"; }

    @Override
    public void onEnter(OakenheartCtx ctx) {
        ctx.entity.getWorld().playSound(ctx.entity.getLocation(), Sound.PORTAL, 1.5f, 0.4f);
        BossEffects.titleNearby(ctx.entity.getLocation(), 30,
                "§4§l✦ WITHERING AURA ✦", "§cSneak or bleed");
    }

    @Override
    public BossState<OakenheartCtx> onTick(OakenheartCtx ctx, int t) {
        Location loc = ctx.entity.getLocation();

        // Paint a 16-block decay ring each tick
        if (t % 3 == 0) {
            int samples = 28;
            for (int i = 0; i < samples; i++) {
                double a = (Math.PI * 2 * i) / samples + t * 0.05;
                Location p = loc.clone().add(Math.cos(a) * RADIUS, 0.3, Math.sin(a) * RADIUS);
                p.getWorld().playEffect(p, Effect.SMOKE, 4);
            }
        }
        if (t > 0 && t % DAMAGE_PERIOD == 0) {
            for (Player p : ctx.nearbyPlayers(RADIUS)) {
                if (p.isSneaking()) continue;   // sneak skips the tick
                BossDamage.apply(p, "oakenheart", "withering_aura", 80, ctx.entity);
                p.getWorld().playEffect(p.getLocation().add(0, 1, 0), Effect.STEP_SOUND, Material.DEAD_BUSH.getId());
            }
        }
        if (t >= DURATION) return new IdleState();
        return null;
    }
}
