package com.soulenchants.modock;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;

/**
 * Loads + holds references to the three Modock arena worlds (phase1/2/3).
 * Folders must already exist on disk under the server root — they were
 * pre-converted from 1.20+ via Amulet, no in-game generation happens here.
 *
 * Bukkit lazy-loads worlds on first access via WorldCreator, but we eagerly
 * call ensureAll() at plugin enable so first-time access doesn't pause the
 * server thread mid-fight.
 */
public final class ModockWorlds {

    public static final String PHASE1 = "modock_phase1";
    public static final String PHASE2 = "modock_phase2";
    public static final String PHASE3 = "modock_phase3";

    private ModockWorlds() {}

    public static void ensureAll(Plugin plugin) {
        for (String name : new String[]{PHASE1, PHASE2, PHASE3}) {
            ensure(plugin, name);
        }
    }

    public static World ensure(Plugin plugin, String name) {
        World w = Bukkit.getWorld(name);
        if (w != null) return w;
        // Self-heal: Spigot refuses to load worlds with duplicate uid.dat.
        // If our converted folder shipped with a UUID inherited from a template,
        // delete it BEFORE calling createWorld so Spigot can regenerate one.
        java.io.File worldDir = new java.io.File(
                org.bukkit.Bukkit.getWorldContainer(), name);
        java.io.File uid = new java.io.File(worldDir, "uid.dat");
        if (worldDir.isDirectory() && uid.exists()) {
            // Only delete if there's no other loaded world that already claimed
            // this UUID (the duplicate-prevention case). Cheap check — if any
            // already-loaded world's container has the same uid file content,
            // we'd just lose a regen — which is fine, UUIDs in level data don't
            // matter for Modock arenas (no per-world player anchors here).
            try { uid.delete(); }
            catch (Throwable ignored) {}
        }
        try {
            WorldCreator wc = new WorldCreator(name);
            w = Bukkit.createWorld(wc);
            if (w == null) {
                plugin.getLogger().warning("[modock] createWorld returned NULL for "
                        + name + " — check server log for 'duplicate', 'access denied',"
                        + " or version-mismatch messages above this line.");
                return null;
            }
            w.setKeepSpawnInMemory(false);
            w.setPVP(true);
            w.setGameRuleValue("doDaylightCycle", "false");
            w.setGameRuleValue("doMobSpawning", "false");
            w.setGameRuleValue("keepInventory", "true");
            plugin.getLogger().info("[modock] loaded world " + name
                    + " spawn=(" + w.getSpawnLocation().getBlockX()
                    + "," + w.getSpawnLocation().getBlockY()
                    + "," + w.getSpawnLocation().getBlockZ() + ")");
        } catch (Throwable t) {
            plugin.getLogger().warning("[modock] failed to load " + name + ": " + t);
        }
        return w;
    }
}
