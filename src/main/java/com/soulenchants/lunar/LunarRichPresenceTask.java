package com.soulenchants.lunar;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.EliteBossHooks;
import com.soulenchants.bosses.IronGolemBoss;
import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.mobs.CustomMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the Lunar Client Discord Rich Presence. Polls every 2 seconds; for
 * each online player we compute a presence state:
 *
 *   • "Fighting Veilweaver [Phase II]"  (within 60 blocks of a live boss)
 *   • "Fighting Ironheart Colossus"
 *   • "Fighting Modock"
 *   • "Fighting Broodmother/Wurm-Lord/Choirmaster"
 *   • "Exploring · FabledMC"            (default / idle)
 *
 * State is diffed per-player so we only push a new presence when it changes,
 * not every tick.
 */
public final class LunarRichPresenceTask implements Listener {

    private final SoulEnchants plugin;
    private BukkitRunnable task;
    /** Last-pushed state string per player — reference equality is fine for
     *  our small set of interned literals, but we use .equals() to be safe. */
    private final Map<UUID, String> lastState = new HashMap<>();

    public LunarRichPresenceTask(SoulEnchants plugin) { this.plugin = plugin; }

    public void start() {
        task = new BukkitRunnable() {
            @Override public void run() {
                if (!LunarBridge.isAvailable()) return;
                for (Player p : plugin.getServer().getOnlinePlayers()) updatePresence(p);
            }
        };
        task.runTaskTimer(plugin, 60L, 40L);   // 3s initial delay, 2s cadence
    }

    public void stop() { if (task != null) try { task.cancel(); } catch (Throwable ignored) {} }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Give Apollo ~1s to register the player before pushing the initial
        // presence — otherwise ApolloPlayer lookup returns empty.
        new BukkitRunnable() {
            @Override public void run() { if (e.getPlayer().isOnline()) updatePresence(e.getPlayer()); }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        lastState.remove(id);
        LunarBridge.resetRichPresence(e.getPlayer());
    }

    private void updatePresence(Player p) {
        String state = computeStateFor(p);
        String prev  = lastState.get(p.getUniqueId());
        if (state.equals(prev)) return;
        lastState.put(p.getUniqueId(), state);
        applyState(p, state);
    }

    /** Compose the playerState string. IDLE is fallback. */
    private String computeStateFor(Player p) {
        // Veilweaver
        if (plugin.getVeilweaverManager() != null) {
            Veilweaver vw = plugin.getVeilweaverManager().getActive();
            if (vw != null && vw.getEntity() != null && !vw.getEntity().isDead()
                    && p.getWorld().equals(vw.getEntity().getWorld())
                    && p.getLocation().distanceSquared(vw.getEntity().getLocation()) <= 60 * 60) {
                String phase = vw.getPhase() == Veilweaver.Phase.ONE ? "I"
                        : vw.getPhase() == Veilweaver.Phase.TWO ? "II" : "III";
                return "Fighting Veilweaver [Phase " + phase + "]";
            }
        }
        // Ironheart Colossus
        if (plugin.getIronGolemManager() != null) {
            IronGolemBoss ig = plugin.getIronGolemManager().getActive();
            if (ig != null && ig.getEntity() != null && !ig.getEntity().isDead()
                    && p.getWorld().equals(ig.getEntity().getWorld())
                    && p.getLocation().distanceSquared(ig.getEntity().getLocation()) <= 60 * 60) {
                String phase = ig.getEntity().getHealth() / ig.getEntity().getMaxHealth() > 0.5 ? "I" : "II";
                return "Fighting Ironheart Colossus [Phase " + phase + "]";
            }
        }
        // Modock
        if (plugin.getModockManager() != null && plugin.getModockManager().getActive() != null) {
            com.soulenchants.modock.ModockBoss mb = plugin.getModockManager().getActive();
            try {
                LivingEntity me = mb.getEntity();
                if (me != null && !me.isDead()
                        && p.getWorld().equals(me.getWorld())
                        && p.getLocation().distanceSquared(me.getLocation()) <= 60 * 60) {
                    return "Fighting Modock, King of Atlantis";
                }
            } catch (Throwable ignored) {}
        }
        // Elite CustomMob bosses (Broodmother / Wurm-Lord / Choirmaster)
        for (org.bukkit.entity.Entity near : p.getNearbyEntities(60, 20, 60)) {
            if (!(near instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity) near;
            if (le.isDead()) continue;
            String cid = CustomMob.idOf(le);
            if (cid == null) continue;
            EliteBossHooks.Spec spec = EliteBossHooks.specOf(cid);
            if (spec == null) continue;
            int phase = EliteBossHooks.phaseByEntity.getOrDefault(le.getUniqueId(), 1);
            String ph = phase == 1 ? "I" : phase == 2 ? "II" : "III";
            return "Fighting " + spec.display + " [Phase " + ph + "]";
        }
        // Void Rift participation
        if (plugin.getVoidRiftManager() != null && plugin.getVoidRiftManager().isActive()) {
            try {
                if (plugin.getVoidRiftManager().getParticipants().contains(p.getUniqueId()))
                    return "Void Rift · " + plugin.getVoidRiftManager().shortStatus();
            } catch (Throwable ignored) {}
        }
        return "Exploring FabledMC";
    }

    private void applyState(Player p, String state) {
        String gameState = state.startsWith("Fighting") ? "Boss Fight"
                : state.startsWith("Void Rift")          ? "Void Rift"
                : "Exploring";
        LunarBridge.setRichPresence(p,
                "FabledMC",            // gameName
                "SoulEnchants",        // gameVariantName
                gameState,             // gameState
                state,                 // playerState
                null,                  // mapName
                null);                 // subServerName
    }
}
