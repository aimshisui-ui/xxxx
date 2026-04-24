package com.soulenchants.bosses.golem.states;

import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.golem.GolemCtx;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** 3-second invulnerability window for the Phase I → Phase II cinematic.
 *  Strikes 4 lightning bolts around the boss, broadcasts the phase banner,
 *  then returns to Idle. Invulnerability is set via BossContext.boss
 *  directly so damage listeners pick it up. */
public final class PhaseTransitionState implements BossState<GolemCtx> {

    private static final int DURATION = 60;

    @Override public String name() { return "PhaseTransition"; }

    @Override
    public void onEnter(GolemCtx ctx) {
        ctx.boss.setInvulnerable(true);
        ctx.boss.enterPhaseTwo();
        Location loc = ctx.entity.getLocation();
        loc.getWorld().createExplosion(loc.getX(), loc.getY() + 1, loc.getZ(), 0f, false);
        loc.getWorld().playSound(loc, Sound.IRONGOLEM_HIT, 2.0f, 0.5f);
        for (int i = 0; i < 4; i++) {
            double a = i * (Math.PI / 2);
            Location strike = loc.clone().add(Math.cos(a) * 5, 0, Math.sin(a) * 5);
            strike.getWorld().strikeLightningEffect(strike);
        }
        broadcastBanner(ctx);
    }

    @Override
    public BossState<GolemCtx> onTick(GolemCtx ctx, int t) {
        if (t >= DURATION) {
            ctx.boss.setInvulnerable(false);
            return new IdleState();
        }
        return null;
    }

    @Override
    public void onExit(GolemCtx ctx) {
        // Safety: clear invuln even if tick didn't run to DURATION.
        ctx.boss.setInvulnerable(false);
    }

    private void broadcastBanner(GolemCtx ctx) {
        String titleMain = ChatColor.GOLD + "" + ChatColor.BOLD + "✦ IRONHEART AWAKENED ✦";
        String titleSub  = ChatColor.YELLOW + "The Colossus unleashes its rage";
        String[] lines = new String[] {
                ChatColor.GOLD + "" + ChatColor.BOLD + "══════════════════════════════",
                ChatColor.GOLD + "" + ChatColor.BOLD + "  ✦ IRONHEART AWAKENED — Phase II ✦",
                ChatColor.GRAY + "  • §6Rocket Charge: §fdodge sideways, not backward",
                ChatColor.GRAY + "  • §6Magnetic Pull: §fbrace to avoid stomp chain",
                ChatColor.GRAY + "  • §6Iron Wall: §fcobble cover for 10s — use it",
                ChatColor.GRAY + "  • §6Ground Slam: §fcenter is a kill zone — scatter",
                ChatColor.GRAY + "  • §cAt 25% HP it reinforces (regens). Burst it first.",
                ChatColor.GOLD + "" + ChatColor.BOLD + "══════════════════════════════"
        };
        for (Player p : ctx.nearbyPlayers(30)) {
            try {
                com.soulenchants.lunar.LunarFx.sendTitle(p, titleMain, titleSub, 250L, 2000L, 500L, 1.35f);
                for (String line : lines) p.sendMessage(line);
                p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 1.5f, 0.8f);
            } catch (Throwable ignored) {}
        }
    }
}
