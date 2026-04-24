package com.soulenchants.bosses.veilweaver.states;

import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.veilweaver.VeilweaverCtx;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/** 3-second invulnerable window covering both Phase I→II and Phase II→III
 *  transitions. Broadcast content varies per target phase; crystal-ring
 *  respawn is scheduled from onEnter. */
public final class PhaseTransitionState implements BossState<VeilweaverCtx> {

    private static final int DURATION = 60;   // 3s

    private final Veilweaver.Phase targetPhase;

    public PhaseTransitionState(Veilweaver.Phase targetPhase) { this.targetPhase = targetPhase; }

    @Override public String name() { return "PhaseTransition→" + targetPhase; }

    @Override
    public void onEnter(VeilweaverCtx ctx) {
        ctx.boss.setInvulnerableFor(DURATION * 50L);
        ctx.boss.enterPhase(targetPhase);
        Location loc = ctx.entity.getLocation();
        loc.getWorld().createExplosion(loc.getX(), loc.getY() + 1, loc.getZ(), 0f, false);
        loc.getWorld().playSound(loc, Sound.ENDERDRAGON_GROWL, 2.0f, 0.7f);
        for (int i = 0; i < 3; i++) {
            double a = i * (Math.PI * 2 / 3);
            Location strike = loc.clone().add(Math.cos(a) * 4, 0, Math.sin(a) * 4);
            strike.getWorld().strikeLightningEffect(strike);
        }
        broadcast(ctx);
        // Destroy crystal orbs for this phase step
        if (targetPhase == Veilweaver.Phase.TWO) ctx.arena.destroyOrbs(2);
        else if (targetPhase == Veilweaver.Phase.THREE) ctx.arena.destroyOrbs(99);
        // Respawn crystal shield ring
        final int crystalCount = targetPhase == Veilweaver.Phase.TWO ? 5 : 6;
        new BukkitRunnable() {
            @Override public void run() { ctx.boss.getCrystals().spawnRing(crystalCount); }
        }.runTaskLater(ctx.plugin, 40L);
        // Dramatic hover
        ctx.entity.setVelocity(ctx.entity.getVelocity().setY(0.6));
    }

    @Override
    public BossState<VeilweaverCtx> onTick(VeilweaverCtx ctx, int t) {
        if (t >= DURATION) return new IdleState();
        return null;
    }

    private void broadcast(VeilweaverCtx ctx) {
        String titleMain, titleSub;
        String[] lines;
        if (targetPhase == Veilweaver.Phase.TWO) {
            titleMain = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ THE VEIL TEARS ✦";
            titleSub  = ChatColor.GRAY + "The Veilweaver becomes ethereal";
            lines = new String[] {
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "══════════════════════════════",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "  ✦ THE VEIL TEARS — Phase II ✦",
                    ChatColor.GRAY + "  • §dBoss teleports to players and rips rifts",
                    ChatColor.GRAY + "  • §dLoom Laser: 2.5s channel — break line of sight!",
                    ChatColor.GRAY + "  • §dEcho Clones: kill with §cexplosions §dfor instant-drop",
                    ChatColor.GRAY + "  • §5Arrows deal half damage in this phase",
                    ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "══════════════════════════════"
            };
        } else {
            titleMain = ChatColor.DARK_RED + "" + ChatColor.BOLD + "✦ THE SHATTERED LOOM ✦";
            titleSub  = ChatColor.RED + "Reality itself breaks down";
            lines = new String[] {
                    ChatColor.DARK_RED + "" + ChatColor.BOLD + "══════════════════════════════",
                    ChatColor.DARK_RED + "" + ChatColor.BOLD + "  ✦ THE SHATTERED LOOM — Phase III ✦",
                    ChatColor.GRAY + "  • §cReality Fracture: §fjump between expanding rings",
                    ChatColor.GRAY + "  • §cApocalypse Weave: §fboss goes invulnerable 4s — hide",
                    ChatColor.GRAY + "  • §cAt §4§l10% HP§c: Final Thread Bind roots top damager",
                    ChatColor.GRAY + "  • §cBurn the boss down fast before the bind completes",
                    ChatColor.DARK_RED + "" + ChatColor.BOLD + "══════════════════════════════"
            };
        }
        for (Player p : ctx.arena.playersInArena()) {
            com.soulenchants.lunar.LunarFx.sendTitle(p, titleMain, titleSub,
                    250L, 2000L, 500L, 1.35f);
            for (String line : lines) p.sendMessage(line);
            p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 1.5f, 1.0f);
        }
    }
}
