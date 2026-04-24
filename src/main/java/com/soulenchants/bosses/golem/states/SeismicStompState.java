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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Ground-slam stomp. 15-tick telegraph (expanding dust ring), then 6-block
 *  shockwave dealing 300 dmg + anti-mining slow for 4s. */
public final class SeismicStompState implements BossState<GolemCtx> {

    private static final int TELEGRAPH = 15;
    private static final int TOTAL     = 20;   // 15 telegraph + 5 settle
    private Location anchor;

    @Override public String name() { return "SeismicStomp"; }

    @Override
    public void onEnter(GolemCtx ctx) {
        this.anchor = ctx.entity.getLocation();
        ctx.entity.setVelocity(new Vector(0, 0.6, 0));
        anchor.getWorld().playSound(anchor, Sound.IRONGOLEM_THROW, 1.5f, 0.6f);
    }

    @Override
    public BossState<GolemCtx> onTick(GolemCtx ctx, int t) {
        if (t < TELEGRAPH) {
            double r = (t / (double) TELEGRAPH) * 5.0;
            for (int i = 0; i < 20; i++) {
                double a = (Math.PI * 2 * i) / 20.0;
                Location p = anchor.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.STONE.getId());
            }
            return null;
        }
        if (t == TELEGRAPH) {
            anchor.getWorld().playSound(anchor, Sound.EXPLODE, 2.0f, 0.5f);
            anchor.getWorld().createExplosion(anchor.getX(), anchor.getY(), anchor.getZ(), 0f, false);
            BossEffects.shockwaveRing(ctx.plugin, anchor, 6.0, Material.STONE);
            BossEffects.particleBurst(anchor.clone().add(0, 1, 0), Effect.SMOKE, 60);
            for (Player p : ctx.nearbyPlayers(6)) {
                BossDamage.apply(p, "irongolem", "ground_slam", 300, ctx.entity);
                p.setVelocity(new Vector(p.getVelocity().getX(), 1.0, p.getVelocity().getZ()));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 80, 1));
            }
        }
        if (t >= TOTAL) return new IdleState();
        return null;
    }
}
