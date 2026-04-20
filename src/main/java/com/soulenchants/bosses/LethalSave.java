package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Centralized check for enchants that save a player from a lethal hit.
 * Used both by the regular damage listener and by boss true-damage paths.
 */
public class LethalSave {

    public static boolean trySave(Player p, SoulEnchants plugin) {
        if (p == null || p.isDead()) return false;
        UUID id = p.getUniqueId();

        // Phoenix (Nordic-style) — 160s cooldown. Burns a RANDOM 500-8000 souls
        // and heals to full HP. If the player can't afford the random cost, the
        // save fails (and they die). Out-of-souls message is shown either way.
        int phoenix = maxArmor(p, "phoenix");
        if (phoenix > 0 && plugin.getCooldownManager().isReady("phoenix", id)) {
            int cost = ThreadLocalRandom.current().nextInt(500, 8000);
            if (plugin.getSoulManager().get(p) < cost) {
                p.sendMessage("§c§l** OUT OF SOULS **");
                return false;
            }
            plugin.getSoulManager().take(p, cost);
            plugin.getCooldownManager().set("phoenix", id, 160_000L);
            p.setHealth(p.getMaxHealth());
            p.getWorld().strikeLightningEffect(p.getLocation());
            try { p.sendTitle("§c§l** PHOENIX **",
                    "§7You have §n" + plugin.getSoulManager().get(p) + "§7 souls left."); } catch (Throwable ignored) {}
            try { p.playSound(p.getLocation(), org.bukkit.Sound.ENDERDRAGON_GROWL, 1.0f, 1.25f); } catch (Throwable ignored) {}
            // Broadcast to nearby players (Nordic flair)
            for (Entity nearby : p.getNearbyEntities(48, 48, 48)) {
                if (!(nearby instanceof Player)) continue;
                Player np = (Player) nearby;
                np.sendMessage("§c§l*** PHOENIX SOUL (§7" + p.getName() + ", -" + cost + " souls§c§l) ***");
                try { np.playSound(p.getLocation(), org.bukkit.Sound.ENDERDRAGON_GROWL, 1.0f, 1.25f); } catch (Throwable ignored) {}
            }
            // Flair: fire particle burst
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2 * i) / 16.0;
                p.getWorld().playEffect(p.getLocation().add(Math.cos(a), 1, Math.sin(a)),
                        org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
            }
            return true;
        }

        // Soul Shield — 60s cd, costs 200 souls, heal 6 HP
        int shield = maxArmor(p, "soulshield");
        if (shield > 0 && plugin.getCooldownManager().isReady("soulshield", id)) {
            int cost = 200;
            if (plugin.getSoulManager().get(p) >= cost) {
                plugin.getSoulManager().take(p, cost);
                plugin.getCooldownManager().set("soulshield", id, 60_000L);
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 6));
                p.getWorld().strikeLightningEffect(p.getLocation());
                p.sendMessage("§c✦ §4Soul Shield triggered! §7(-" + cost + " souls)");
                return true;
            }
        }

        return false;
    }

    private static int maxArmor(Player p, String id) {
        int max = 0;
        for (ItemStack a : p.getInventory().getArmorContents()) {
            if (a == null) continue;
            int lvl = ItemUtil.getLevel(a, id);
            if (lvl > max) max = lvl;
        }
        return max;
    }
}
