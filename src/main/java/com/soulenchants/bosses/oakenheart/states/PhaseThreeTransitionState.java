package com.soulenchants.bosses.oakenheart.states;

import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.oakenheart.OakenheartCtx;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Phase 2→3 transition — 4-second invulnerable window, 6 lightning bolts,
 *  chat + title broadcast. On exit, Oakenheart is free to use the full
 *  attack pool including Briar Prison + Withering Aura. */
public final class PhaseThreeTransitionState implements BossState<OakenheartCtx> {

    private static final int DURATION = 80;

    @Override public String name() { return "Phase3Transition"; }

    @Override
    public void onEnter(OakenheartCtx ctx) {
        ctx.boss.setInvulnerable(true);
        ctx.boss.enterPhaseThree();
        Location loc = ctx.entity.getLocation();
        loc.getWorld().playSound(loc, Sound.ENDERDRAGON_GROWL, 2.0f, 0.6f);
        loc.getWorld().createExplosion(loc.getX(), loc.getY() + 1, loc.getZ(), 0f, false);
        for (int i = 0; i < 6; i++) {
            double a = i * (Math.PI / 3);
            loc.getWorld().strikeLightningEffect(loc.clone().add(Math.cos(a) * 6, 0, Math.sin(a) * 6));
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
        String titleMain = ChatColor.DARK_RED + "" + ChatColor.BOLD + "✦ THE WITHERING ✦";
        String titleSub  = ChatColor.RED + "Phase III — the canopy rots";
        String[] lines = new String[] {
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "══════════════════════════════",
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "  ✦ OAKENHEART — Phase III ✦",
                ChatColor.GRAY + "  • §cWithering Aura: §fsneak inside 16 blocks or bleed",
                ChatColor.GRAY + "  • §cBriar Prison: §fweb cage + DoT — break out or die",
                ChatColor.GRAY + "  • §fFull attack rotation — no rest between casts",
                ChatColor.GRAY + "  • §cKill or be killed",
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "══════════════════════════════"
        };
        for (Player p : ctx.nearbyPlayers(50)) {
            try {
                com.soulenchants.lunar.LunarFx.sendTitle(p, titleMain, titleSub, 250L, 2200L, 500L, 1.4f);
                for (String s : lines) p.sendMessage(s);
                p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1.4f, 0.5f);
            } catch (Throwable ignored) {}
        }
    }
}
