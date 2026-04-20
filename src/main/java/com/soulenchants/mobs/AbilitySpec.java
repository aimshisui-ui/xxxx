package com.soulenchants.mobs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data-form of an ability: a type key plus a param map.
 * Build() resolves to a runtime MobAbility via AbilityFactory.
 * Persisted to YAML; editable via the /ce loot GUI.
 */
public final class AbilitySpec {

    public final String type;
    public final Map<String, Object> params;

    public AbilitySpec(String type, Map<String, Object> params) {
        this.type = type;
        this.params = params != null ? params : new LinkedHashMap<>();
    }

    public static AbilitySpec of(String type, Object... kv) {
        Map<String, Object> p = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            p.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return new AbilitySpec(type, p);
    }

    public AbilitySpec copy() {
        return new AbilitySpec(type, new LinkedHashMap<>(params));
    }

    public MobAbility build() { return AbilityFactory.build(this); }

    public Map<String, Object> serialize() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("params", new LinkedHashMap<>(params));
        return m;
    }

    @SuppressWarnings("unchecked")
    public static AbilitySpec deserialize(Map<String, Object> m) {
        String type = String.valueOf(m.get("type"));
        Object p = m.get("params");
        Map<String, Object> params = (p instanceof Map) ? new LinkedHashMap<>((Map<String, Object>) p) : new LinkedHashMap<>();
        return new AbilitySpec(type, params);
    }

    public int geti(String k, int def)    { Object o = params.get(k); return (o instanceof Number) ? ((Number) o).intValue() : def; }
    public double getd(String k, double def) { Object o = params.get(k); return (o instanceof Number) ? ((Number) o).doubleValue() : def; }
    public String gets(String k, String def) { Object o = params.get(k); return o == null ? def : String.valueOf(o); }
}
