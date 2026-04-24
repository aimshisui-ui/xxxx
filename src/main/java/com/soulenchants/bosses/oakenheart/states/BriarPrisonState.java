package com.soulenchants.bosses.oakenheart.states;

import com.soulenchants.bosses.BossDamage;
import com.soulenchants.bosses.BossEffects;
import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.oakenheart.OakenheartCtx;
import com.soulenchants.util.TempBlockTracker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Phase-3-exclusive. Picks a random player and encases them in a cobweb
 *  ring placed via TempBlockTracker ("oakenheart_briar" tag). The ring
 *  persists 120 ticks; while trapped the victim takes 60 dmg/tick every
 *  20 ticks until they break free or the ring decays. */
public final class BriarPrisonState implements BossState<OakenheartCtx> {

    private static final int DURATION      = 120;   // 6s cobweb lifetime
    private static final int DAMAGE_PERIOD = 20;    // 1s
    private Location cage;
    private Player   victim;

    @Override public String name() { return "BriarPrison"; }

    @Override
    public void onEnter(OakenheartCtx ctx) {
        this.victim = ctx.pickTarget();
        if (victim == null) return;
        BossEffects.titleNearby(victim.getLocation(), 15,
                "§4§l✦ BRIAR PRISON ✦", "§cBreak the ring or take the ticks");
        victim.playSound(victim.getLocation(), Sound.ZOMBIE_WOODBREAK, 2.0f, 0.5f);
        cage = victim.getLocation().clone();
        // Place a ring of cobwebs around the victim + two above. Only AIR slots
        // get overwritten (no destroying player structures).
        int[][] offsets = new int[][] {
                { 1, 0,  0}, {-1, 0,  0}, { 0, 0,  1}, { 0, 0, -1},
                { 1, 0,  1}, {-1, 0,  1}, { 1, 0, -1}, {-1, 0, -1},
                { 0, 1,  0}, { 0, 2,  0}
        };
        for (int[] o : offsets) {
            Location l = cage.clone().add(o[0], o[1], o[2]);
            if (l.getBlock().getType() == Material.AIR) {
                TempBlockTracker.place(l, Material.WEB, DURATION, "oakenheart_briar");
            }
        }
    }

    @Override
    public BossState<OakenheartCtx> onTick(OakenheartCtx ctx, int t) {
        if (victim == null) return new IdleState();
        if (t > 0 && t % DAMAGE_PERIOD == 0 && !victim.isDead()) {
            // Only tick damage if the victim is still inside the 2-block cage sphere.
            if (victim.getLocation().distanceSquared(cage) < 4) {
                BossDamage.apply(victim, "oakenheart", "briar_prison", 60, ctx.entity);
                victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0),
                        org.bukkit.Effect.STEP_SOUND, Material.WEB.getId());
            }
        }
        if (t >= DURATION) {
            // TempBlockTracker sweeper will restore on its own pass.
            return new IdleState();
        }
        return null;
    }
}
