package com.soulenchants.bosses.oakenheart.states;

import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.oakenheart.OakenheartCtx;
import com.soulenchants.mobs.CustomMob;
import com.soulenchants.mobs.MobRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Phase 1→2 transition — 3-second invulnerable window with a grove-burst
 *  cinematic. Summons 3 sapling_sprouts on entry so Phase II starts with
 *  minion pressure. */
public final class PhaseTwoTransitionState implements BossState<OakenheartCtx> {

    private static final int DURATION = 60;

    @Override public String name() { return "Phase2Transition"; }

    @Override
    public void onEnter(OakenheartCtx ctx) {
        ctx.boss.setInvulnerable(true);
        ctx.boss.enterPhaseTwo();
        Location loc = ctx.entity.getLocation();
        loc.getWorld().playSound(loc, Sound.WITHER_SPAWN, 1.5f, 0.8f);
        loc.getWorld().createExplosion(loc.getX(), loc.getY() + 1, loc.getZ(), 0f, false);
        for (int i = 0; i < 4; i++) {
            double a = i * (Math.PI / 2);
            loc.getWorld().strikeLightningEffect(loc.clone().add(Math.cos(a) * 5, 0, Math.sin(a) * 5));
        }
        // Spawn 3 saplings on entry so the phase shift adds immediate pressure.
        CustomMob mob = MobRegistry.get("sapling_sprout");
        if (mob != null) {
            for (int i = 0; i < 3; i++) {
                double a = i * (Math.PI * 2 / 3);
                mob.spawn(loc.clone().add(Math.cos(a) * 3.0, 0, Math.sin(a) * 3.0));
            }
        }
        broadcast(ctx);
    }

    @Override
    public BossState<OakenheartCtx> onTick(OakenheartCtx ctx, int t) {
        if (t >= DURATION) {
            ctx.boss.setInvulnerable(false);
            return new IdleState();
        }
        return null;
    }

    @Override
    public void onExit(OakenheartCtx ctx) { ctx.boss.setInvulnerable(false); }

    private void broadcast(OakenheartCtx ctx) {
        String titleMain = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "✦ THE GROVE AWAKENS ✦";
        String titleSub  = ChatColor.GREEN + "Phase II — saplings rise";
        String[] lines = new String[] {
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "══════════════════════════════",
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "  ✦ OAKENHEART — Phase II ✦",
                ChatColor.GRAY + "  • §aSapling Swarm: §fkill adds before they pile on",
                ChatColor.GRAY + "  • §aFalling Grove: §foak logs drop from above — move off crosshairs",
                ChatColor.GRAY + "  • §cAt 33% HP the Withering begins",
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "══════════════════════════════"
        };
        for (Player p : ctx.nearbyPlayers(50)) {
            try {
                com.soulenchants.lunar.LunarFx.sendTitle(p, titleMain, titleSub, 250L, 2000L, 500L, 1.35f);
                for (String s : lines) p.sendMessage(s);
                p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 1.2f, 0.8f);
            } catch (Throwable ignored) {}
        }
    }
}
