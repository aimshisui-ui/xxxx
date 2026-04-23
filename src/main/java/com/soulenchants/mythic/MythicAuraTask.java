package com.soulenchants.mythic;

import com.soulenchants.SoulEnchants;
import com.soulenchants.config.MythicConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodic tick — for every online player, find every mythic on their person
 * that matches AURA or HOTBAR proximity and fire onAuraTick. Main hand
 * effects fire regardless. Simple, stateless, 20-tick default cadence.
 */
public final class MythicAuraTask extends BukkitRunnable {

    private final SoulEnchants plugin;
    private final MythicConfig cfg;

    public MythicAuraTask(SoulEnchants plugin, MythicConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    public void start() {
        runTaskTimer(plugin, 20L, cfg.auraTickInterval);
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            PlayerInventory inv = p.getInventory();
            // Main hand
            MythicWeapon held = MythicRegistry.of(inv.getItemInHand());
            if (held != null) held.onAuraTick(p);
            // Hotbar / offhand (AURA + HOTBAR modes)
            if (!cfg.auraHotbarCountsAsHeld) continue;
            int mainSlot = inv.getHeldItemSlot();
            for (int i = 0; i <= 8; i++) {
                if (i == mainSlot) continue; // already ticked above
                MythicWeapon m = MythicRegistry.of(inv.getItem(i));
                if (m == null) continue;
                if (m.getMode() == MythicWeapon.ProximityMode.AURA
                        || m.getMode() == MythicWeapon.ProximityMode.HOTBAR) {
                    m.onAuraTick(p);
                }
            }
        }
    }
}
