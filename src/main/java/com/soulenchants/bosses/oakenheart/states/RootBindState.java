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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 30-tick expanding vine-ring telegraph, then every player within 16 blocks
 *  gets 120 damage + Slow IV + Jump(-129) for 4 seconds. Roots you in
 *  place — you have to tank the next hit or burn a cleanse. */
public final class RootBindState implements BossState<OakenheartCtx> {

    private static final int TELEGRAPH = 30;
    private static final int TOTAL     = 40;
    private static final double RADIUS = 16.0;

    @Override public String name() { return "RootBind"; }

    @Override
    public void onEnter(OakenheartCtx ctx) {
        Location loc = ctx.entity.getLocation();
        loc.getWorld().playSound(loc, Sound.ZOMBIE_WOODBREAK, 1.5f, 0.5f);
        BossEffects.titleNearby(loc, 30, "§2§l✦ ROOT BIND ✦", "§aBurn a cleanse or brace");
    }

    @Override
    public BossState<OakenheartCtx> onTick(OakenheartCtx ctx, int t) {
        Location loc = ctx.entity.getLocation();
        if (t < TELEGRAPH) {
            double r = (t / (double) TELEGRAPH) * RADIUS;
            int samples = 36;
            for (int i = 0; i < samples; i++) {
                double a = (Math.PI * 2 * i) / samples;
                Location p = loc.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.LEAVES.getId());
            }
            return null;
        }
        if (t == TELEGRAPH) {
            loc.getWorld().playSound(loc, Sound.EXPLODE, 1.5f, 0.7f);
            BossEffects.shockwaveRing(ctx.plugin, loc, RADIUS, Material.DIRT);
            for (Player p : ctx.nearbyPlayers(RADIUS)) {
                BossDamage.apply(p, "oakenheart", "root_bind", 120, ctx.entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 3, false, false), true);
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 80, 128, false, false), true);
                p.getWorld().playEffect(p.getLocation().add(0, 1, 0), Effect.STEP_SOUND, Material.VINE.getId());
            }
        }
        if (t >= TOTAL) return new IdleState();
        return null;
    }
}
