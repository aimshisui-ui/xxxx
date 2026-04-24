package com.soulenchants.bosses.fsm;

/**
 * Drives a BossState&lt;C&gt;. Owns the "ticks in current state" counter so
 * states don't have to track it themselves. Calling code just needs
 * {@link #tick(Object)} every server tick; transitions are automatic.
 *
 *   StateMachine&lt;GolemCtx&gt; sm = new StateMachine&lt;&gt;(new IdleState());
 *   sm.tick(ctx);   // every tick from the boss's BukkitRunnable
 *
 * Force-transitions (phase trigger, death, despawn) call {@link #set(BossState, Object)}
 * directly — ignores whatever the current onTick would return.
 */
public final class StateMachine<C> {

    private BossState<C> current;
    private int ticksInState = 0;

    public StateMachine(BossState<C> initial) { this.current = initial; }

    /** Force-transition to a specific state, running onExit/onEnter hooks. */
    public void set(BossState<C> next, C ctx) {
        if (next == null || next == current) return;
        try { if (current != null) current.onExit(ctx); } catch (Throwable ignored) {}
        this.current = next;
        this.ticksInState = 0;
        try { current.onEnter(ctx); } catch (Throwable ignored) {}
    }

    /** Run one tick of the current state. Auto-transitions if onTick returns
     *  a non-null next state. Exceptions in onTick are swallowed with a log
     *  so a single bad state can't break the boss loop. */
    public void tick(C ctx) {
        if (current == null) return;
        BossState<C> next;
        try {
            next = current.onTick(ctx, ticksInState);
        } catch (Throwable t) {
            // Log-and-recover: boss continues running even if one state throws.
            org.bukkit.Bukkit.getLogger().warning("[FSM] " + current.name() + " onTick threw: " + t);
            t.printStackTrace();
            next = null;
        }
        ticksInState++;
        if (next != null && next != current) set(next, ctx);
    }

    public BossState<C> current() { return current; }
    public int ticksInState() { return ticksInState; }
    public String currentName() { return current == null ? "<null>" : current.name(); }
}
