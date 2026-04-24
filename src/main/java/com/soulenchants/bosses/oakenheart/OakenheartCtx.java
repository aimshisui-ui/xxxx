package com.soulenchants.bosses.oakenheart;

import com.soulenchants.SoulEnchants;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Shared handle passed into every Oakenheart state. Same pattern as
 *  GolemCtx — states read this instead of carrying a boss reference. */
public final class OakenheartCtx {
    public final SoulEnchants plugin;
    public final OakenheartBoss boss;
    public final IronGolem entity;
    public final Random rng = new Random();

    public OakenheartCtx(SoulEnchants plugin, OakenheartBoss boss) {
        this.plugin = plugin;
        this.boss = boss;
        this.entity = boss.getEntity();
    }

    public List<Player> nearbyPlayers(double radius) {
        List<Player> list = new ArrayList<>();
        double rSq = radius * radius;
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(entity.getLocation()) <= rSq) list.add(p);
        }
        return list;
    }

    public Player pickTarget() {
        List<Player> near = nearbyPlayers(40);
        return near.isEmpty() ? null : near.get(rng.nextInt(near.size()));
    }
}
