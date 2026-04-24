package com.soulenchants.bosses.golem;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.IronGolemBoss;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Shared read/write handle passed into every IronGolem boss state.
 * Keeps states small — they don't need a boss reference, they just read
 * fields here. Rng + nearbyPlayers helpers live here so every state
 * computes them the same way.
 */
public final class GolemCtx {
    public final SoulEnchants plugin;
    public final IronGolemBoss boss;
    public final IronGolem entity;
    public final Random rng = new Random();

    public GolemCtx(SoulEnchants plugin, IronGolemBoss boss) {
        this.plugin = plugin;
        this.boss = boss;
        this.entity = boss.getEntity();
    }

    /** Players within `radius` blocks of the boss, in current world. */
    public List<Player> nearbyPlayers(double radius) {
        List<Player> list = new ArrayList<>();
        double rSq = radius * radius;
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(entity.getLocation()) <= rSq) list.add(p);
        }
        return list;
    }

    /** Random player within 30 blocks, or null. */
    public Player pickTarget() {
        List<Player> near = nearbyPlayers(30);
        return near.isEmpty() ? null : near.get(rng.nextInt(near.size()));
    }
}
