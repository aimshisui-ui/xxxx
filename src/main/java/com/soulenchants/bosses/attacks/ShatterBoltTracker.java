package com.soulenchants.bosses.attacks;

import com.soulenchants.bosses.Veilweaver;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class ShatterBoltTracker extends BukkitRunnable {

    private final Veilweaver vw;
    private final Snowball orb;
    private int trailTicks = 0;

    public ShatterBoltTracker(Veilweaver vw, Snowball orb) {
        this.vw = vw;
        this.orb = orb;
    }

    public void start() { this.runTaskTimer(vw.getPlugin(), 1L, 1L); }

    @Override
    public void run() {
        if (orb.isDead() || trailTicks++ > 80) {
            // Impact damage if it died via collision
            if (orb.isDead()) {
                Location impact = orb.getLocation();
                for (Entity e : impact.getWorld().getNearbyEntities(impact, 1.5, 1.5, 1.5)) {
                    if (e instanceof Player) {
                        com.soulenchants.bosses.BossDamage.apply((Player) e, 14, vw.getEntity());
                        ((Player) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 1));
                        ((Player) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                    }
                }
                impact.getWorld().createExplosion(impact.getX(), impact.getY(), impact.getZ(), 0f, false);
            }
            cancel();
            return;
        }
        Location loc = orb.getLocation();
        loc.getWorld().playEffect(loc, Effect.PORTAL, 0);
        loc.getWorld().playEffect(loc, Effect.WITCH_MAGIC, 0);
    }
}
