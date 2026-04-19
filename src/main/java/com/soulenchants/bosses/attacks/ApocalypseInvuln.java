package com.soulenchants.bosses.attacks;

import com.soulenchants.bosses.Veilweaver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApocalypseInvuln {

    private static final Map<UUID, Long> until = new HashMap<>();

    public static void setUntil(Veilweaver vw, long timestampMs) {
        until.put(vw.getEntity().getUniqueId(), timestampMs);
    }

    public static boolean isInvuln(Veilweaver vw) {
        Long ts = until.get(vw.getEntity().getUniqueId());
        return ts != null && System.currentTimeMillis() < ts;
    }

    public static void clear(Veilweaver vw) {
        until.remove(vw.getEntity().getUniqueId());
    }
}
