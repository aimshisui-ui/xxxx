package com.soulenchants.mythic.state;

import java.util.UUID;

/**
 * Per-wielder-per-mythic state blob. Extended by a concrete class for any
 * mythic whose behavior depends on time-based state that must NOT be
 * shared across wielders (cooldowns, procs-in-flight, stack counters).
 *
 * Previously mythics stored this as fields on the MythicWeapon instance
 * itself — which is a singleton in MythicRegistry, so two players wielding
 * the same mythic stepped on each other's cooldowns. MythicStateHandle +
 * MythicStateRegistry fix that latent bug.
 *
 * Thread-safety note: these handles are constructed and mutated on the
 * main thread (aura ticks, on-hit procs). Don't access them from async
 * tasks without a copy.
 */
public abstract class MythicStateHandle {

    /** UUID of the player this handle belongs to. */
    protected final UUID wielder;
    /** ID of the mythic this handle is state for — matches MythicWeapon.getId(). */
    protected final String mythicId;
    /** Wall-clock ms this handle was created. Useful for debug / telemetry. */
    protected final long createdAt = System.currentTimeMillis();

    protected MythicStateHandle(UUID wielder, String mythicId) {
        this.wielder = wielder;
        this.mythicId = mythicId;
    }

    public UUID getWielder()  { return wielder; }
    public String getMythicId() { return mythicId; }
}
