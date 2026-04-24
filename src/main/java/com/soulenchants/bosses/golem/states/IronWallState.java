package com.soulenchants.bosses.golem.states;

import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.golem.GolemCtx;
import com.soulenchants.util.TempBlockTracker;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

/** Drops four 5×2 cobblestone walls around the boss for cover. Blocks
 *  placed through TempBlockTracker so a crash mid-fight still restores
 *  them. Auto-restored at 200 ticks; this state plays the fade effect at
 *  t=160 and exits at t=200. */
public final class IronWallState implements BossState<GolemCtx> {

    public static final String TAG = "irongolem_wall";
    private static final int FADE_TICK = 160;
    private static final int DURATION  = 200;

    private final List<Location> wallLocs = new ArrayList<>();
    private Location center;

    @Override public String name() { return "IronWall"; }

    @Override
    public void onEnter(GolemCtx ctx) {
        this.center = ctx.entity.getLocation();
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 0; dy < 2; dy++) {
                    Location wallLoc = center.clone().add(
                            Math.cos(angle) * 4 + dx * Math.sin(angle),
                            dy + 1,
                            Math.sin(angle) * 4 - dx * Math.cos(angle));
                    Material existing = wallLoc.getBlock().getType();
                    if (existing == Material.BEDROCK || existing == Material.COBBLESTONE) continue;
                    TempBlockTracker.place(wallLoc, Material.COBBLESTONE, DURATION, TAG);
                    wallLocs.add(wallLoc);
                }
            }
        }
        center.getWorld().playSound(center, Sound.ANVIL_LAND, 2.0f, 0.5f);
        ctx.plugin.getLogger().info("[IronGolem] Iron Wall placed " + wallLocs.size() + " blocks");
    }

    @Override
    public BossState<GolemCtx> onTick(GolemCtx ctx, int t) {
        if (t == FADE_TICK) {
            for (Location l : wallLocs) {
                l.getWorld().playEffect(l.clone().add(0.5, 0.5, 0.5), Effect.SMOKE, 4);
            }
            center.getWorld().playSound(center, Sound.FIZZ, 1.0f, 1.5f);
        }
        if (t >= DURATION) {
            // TempBlockTracker's sweeper will restore on its 1-Hz pass; play
            // the break-sound now for feedback.
            for (Location l : wallLocs) {
                l.getWorld().playEffect(l.clone().add(0.5, 0.5, 0.5), Effect.STEP_SOUND,
                        Material.COBBLESTONE.getId());
            }
            return new IdleState();
        }
        return null;
    }
}
