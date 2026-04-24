package com.soulenchants.bosses.golem.states;

import com.soulenchants.bosses.fsm.BossState;
import com.soulenchants.bosses.golem.GolemCtx;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;

/** 5-second invulnerable regen below 25% HP. Heals 1 HP/tick for 100 ticks
 *  (scaled by active anti-heal) then releases back to Idle. Fires once per
 *  fight — gating is the boss's `usedReinforce` flag. */
public final class ReinforceState implements BossState<GolemCtx> {

    private static final int DURATION = 100;

    @Override public String name() { return "Reinforce"; }

    @Override
    public void onEnter(GolemCtx ctx) {
        ctx.boss.setInvulnerable(true);
        try {
            ctx.plugin.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD
                    + "✦ The Colossus reinforces! ✦");
            ctx.entity.getWorld().playSound(ctx.entity.getLocation(),
                    Sound.IRONGOLEM_DEATH, 2.0f, 0.7f);
        } catch (Throwable ignored) {}
    }

    @Override
    public BossState<GolemCtx> onTick(GolemCtx ctx, int t) {
        if (ctx.entity.isDead() || t >= DURATION) {
            ctx.boss.setInvulnerable(false);
            return new IdleState();
        }
        try {
            double heal = com.soulenchants.listeners.CombatListener.scaleHealForAntiHeal(
                    ctx.entity, 1.0);
            ctx.entity.setHealth(Math.min(ctx.entity.getMaxHealth(),
                    ctx.entity.getHealth() + heal));
            ctx.entity.getWorld().playEffect(ctx.entity.getLocation().add(0, 1, 0),
                    Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public void onExit(GolemCtx ctx) {
        ctx.boss.setInvulnerable(false);
    }
}
