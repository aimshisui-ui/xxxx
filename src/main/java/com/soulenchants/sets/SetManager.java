package com.soulenchants.sets;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which set is currently active for each player. Re-evaluates only
 * when armor changes (slot listener) or every 40 ticks as a safety sweep
 * — avoids rescanning armor on every damage event.
 *
 * Cache value:
 *   - {@code null} if the player has no full set
 *   - the {@link SetBonus} when piece-count >= set's requiredPieces()
 */
public class SetManager {

    private final SoulEnchants plugin;
    private final Map<UUID, SetBonus> active = new HashMap<>();
    private BukkitRunnable sweepTask;

    public SetManager(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() {
        // Safety sweep every 2s in case ArmorChangeListener missed an event
        // (drop → pickup, /clear, plugin gear-give, etc.)
        sweepTask = new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) reevaluate(p);
            }
        };
        sweepTask.runTaskTimer(plugin, 40L, 40L);
    }

    public void stop() {
        if (sweepTask != null) try { sweepTask.cancel(); } catch (Exception ignored) {}
    }

    public SetBonus getActive(Player p) { return active.get(p.getUniqueId()); }

    /** Re-scan armor and fire equip/unequip lifecycle hooks if the set changed. */
    public void reevaluate(Player p) {
        SetBonus current = computeActive(p);
        SetBonus prev    = active.get(p.getUniqueId());
        if (current == prev) return;
        if (prev != null) {
            try { prev.onUnequip(p); } catch (Throwable t) { plugin.getLogger().warning("[sets] onUnequip " + prev.id() + ": " + t); }
        }
        if (current != null) {
            try { current.onEquip(p); } catch (Throwable t) { plugin.getLogger().warning("[sets] onEquip " + current.id() + ": " + t); }
            active.put(p.getUniqueId(), current);
        } else {
            active.remove(p.getUniqueId());
        }
    }

    /** Remove cache entry for a player who left the server. */
    public void clearPlayer(UUID id) { active.remove(id); }

    private SetBonus computeActive(Player p) {
        // Count matching set pieces across the 4 armor slots
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack piece : p.getInventory().getArmorContents()) {
            String id = SetRegistry.idOf(piece);
            if (id == null) continue;
            counts.merge(id.toLowerCase(), 1, Integer::sum);
        }
        // Find the highest-quality set whose piece-count meets the threshold
        SetBonus best = null;
        int bestPieces = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            SetBonus s = SetRegistry.get(e.getKey());
            if (s == null) continue;
            if (e.getValue() < s.requiredPieces()) continue;
            if (e.getValue() > bestPieces) {
                best = s;
                bestPieces = e.getValue();
            }
        }
        return best;
    }
}
