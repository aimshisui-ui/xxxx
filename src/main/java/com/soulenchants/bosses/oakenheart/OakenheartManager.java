package com.soulenchants.bosses.oakenheart;

import com.soulenchants.SoulEnchants;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/** Single active-boss tracker for Oakenheart. Same contract as
 *  VeilweaverManager / IronGolemManager — at most one active boss at a
 *  time; summon() is a no-op if one's already alive. */
public final class OakenheartManager {

    private final SoulEnchants plugin;
    private OakenheartBoss active;

    public OakenheartManager(SoulEnchants plugin) { this.plugin = plugin; }

    public boolean hasActive() { return active != null && !active.getEntity().isDead(); }
    public OakenheartBoss getActive() { return active; }
    public void clearActive() { this.active = null; }

    /** Spawn Oakenheart at the given location. No-op if one is already active. */
    public boolean summon(Location loc) {
        if (hasActive()) return false;
        this.active = new OakenheartBoss(plugin, loc);
        this.active.start();
        return true;
    }

    /** True if the LivingEntity passed is Oakenheart's body. */
    public boolean isOakenheart(LivingEntity entity) {
        return active != null && active.getEntity().getUniqueId().equals(entity.getUniqueId());
    }
}
