package com.soulenchants.bosses.fsm;

/**
 * One beat of a boss's behavior — idle, a telegraphed attack, a phase
 * transition, a retreat. Each state owns its own tick counter (via the
 * StateMachine) and decides when to hand control back.
 *
 *   onEnter   — one-time setup (spawn particles, broadcast title, lock
 *               invulnerability). Called once on transition IN.
 *   onTick    — per-tick logic while active. Return the state you want
 *               next (usually IdleState after the attack resolves), or
 *               null to stay in the current state.
 *   onExit    — one-time cleanup (clear invulnerability, stop sounds).
 *               Called once on transition OUT.
 *
 * States are re-entrant: a new instance is created each time the state
 * becomes active so per-run internal fields stay isolated. Don't keep
 * mutable state across transitions — use BossContext fields for that.
 */
public interface BossState<C> {

    /** Short tag for debug logs / diagnostics. */
    String name();

    /** Called once as this state becomes active. ticksInState = 0. */
    default void onEnter(C ctx) {}

    /** Called every server tick while this state is active. Return the
     *  next state to transition, or null to stay. ticksInState is the
     *  number of ticks this state has been active (0-indexed). */
    BossState<C> onTick(C ctx, int ticksInState);

    /** Called once as this state is replaced. */
    default void onExit(C ctx) {}
}
