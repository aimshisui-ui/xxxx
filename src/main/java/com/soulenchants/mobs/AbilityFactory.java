package com.soulenchants.mobs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Registry of ability-type → runtime builder. Keyed by {@link AbilitySpec#type}. */
public final class AbilityFactory {

    private static final Map<String, Function<AbilitySpec, MobAbility>> REGISTRY = new HashMap<>();

    private AbilityFactory() {}

    public static void register(String type, Function<AbilitySpec, MobAbility> fn) {
        REGISTRY.put(type, fn);
    }

    public static MobAbility build(AbilitySpec spec) {
        Function<AbilitySpec, MobAbility> fn = REGISTRY.get(spec.type);
        if (fn == null) return new MobAbility() {}; // unknown type → no-op
        try { return fn.apply(spec); }
        catch (Throwable t) { return new MobAbility() {}; }
    }

    public static boolean isRegistered(String type) { return REGISTRY.containsKey(type); }
}
