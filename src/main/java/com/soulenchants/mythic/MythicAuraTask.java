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
            // Main hand — core + bound ability both tick
            MythicWeapon heldCore    = MythicRegistry.of(inv.getItemInHand());
            MythicWeapon heldAbility = MythicRegistry.abilityOf(inv.getItemInHand());
            if (heldCore != null)    heldCore.onAuraTick(p);
            if (heldAbility != null) heldAbility.onAuraTick(p);
            // Hotbar / offhand (AURA + HOTBAR modes on core or ability)
            if (!cfg.auraHotbarCountsAsHeld) continue;
            int mainSlot = inv.getHeldItemSlot();
            for (int i = 0; i <= 8; i++) {
                if (i == mainSlot) continue;
                org.bukkit.inventory.ItemStack slot = inv.getItem(i);
                MythicWeapon core    = MythicRegistry.of(slot);
                MythicWeapon ability = MythicRegistry.abilityOf(slot);
                if (core != null
                        && (core.getMode() == MythicWeapon.ProximityMode.AURA
                            || core.getMode() == MythicWeapon.ProximityMode.HOTBAR)) {
                    core.onAuraTick(p);
                }
                if (ability != null
                        && (ability.getMode() == MythicWeapon.ProximityMode.AURA
                            || ability.getMode() == MythicWeapon.ProximityMode.HOTBAR)) {
                    ability.onAuraTick(p);
                }
            }
        }
    }
}
