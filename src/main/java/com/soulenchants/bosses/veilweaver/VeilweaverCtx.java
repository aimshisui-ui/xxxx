package com.soulenchants.bosses.veilweaver;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.bosses.VeilweaverArena;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

/** Shared handle passed into every Veilweaver state. Unlike the Golem
 *  context this one exposes the arena directly since many attacks
 *  target arena-bounded players specifically. */
public final class VeilweaverCtx {
    public final SoulEnchants plugin;
    public final Veilweaver boss;
    public final LivingEntity entity;
    public final VeilweaverArena arena;
    public final Random rng = new Random();

    public VeilweaverCtx(SoulEnchants plugin, Veilweaver boss) {
        this.plugin = plugin;
        this.boss = boss;
        this.entity = boss.getEntity();
        this.arena = boss.getArena();
    }
}
