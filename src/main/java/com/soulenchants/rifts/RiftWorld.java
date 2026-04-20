package com.soulenchants.rifts;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.Plugin;

/**
 * Ensures a superflat "rift_world" exists. Uses System.out + Bukkit.getLogger
 * instead of plugin.getLogger so output is always visible in console.
 */
public final class RiftWorld {

    public static final String NAME = "rift_world";

    private RiftWorld() {}

    private static void log(String msg) {
        Bukkit.getLogger().info("[SoulEnchants] [rift] " + msg);
        System.out.println("[SoulEnchants] [rift] " + msg);
    }
    private static void warn(String msg) {
        Bukkit.getLogger().warning("[SoulEnchants] [rift] " + msg);
        System.out.println("[SoulEnchants] [rift] WARN: " + msg);
    }

    public static World ensure(Plugin plugin) {
        log("ensure() called.");

        try {
            World existing = Bukkit.getWorld(NAME);
            if (existing != null) { log("already loaded."); return existing; }
        } catch (Throwable t) {
            warn("getWorld() threw: " + t);
        }

        World w = null;
        try {
            WorldCreator wc = new WorldCreator(NAME);
            wc.type(WorldType.FLAT);
            wc.generatorSettings("2;7;1");
            wc.generateStructures(false);
            wc.environment(World.Environment.NORMAL);
            log("calling Bukkit.createWorld for " + NAME + " ...");
            w = Bukkit.createWorld(wc);
        } catch (Throwable t) {
            warn("createWorld threw: " + t);
            t.printStackTrace();
            try {
                WorldCreator wc2 = new WorldCreator(NAME);
                wc2.type(WorldType.FLAT);
                log("retrying with default flat settings ...");
                w = Bukkit.createWorld(wc2);
            } catch (Throwable t2) {
                warn("fallback createWorld threw: " + t2);
                t2.printStackTrace();
            }
        }

        if (w == null) { warn("createWorld returned null — world not created."); return null; }

        try {
            w.setSpawnFlags(false, false);
            w.setKeepSpawnInMemory(false);
            w.setPVP(true);
            w.setGameRuleValue("doDaylightCycle", "false");
            w.setGameRuleValue("doMobSpawning", "false");
            w.setGameRuleValue("keepInventory", "true");
            w.setTime(18000L);
            w.setStorm(false);
            w.setThundering(false);
        } catch (Throwable t) {
            warn("post-create config threw: " + t);
        }

        log(NAME + " ready — spawn at (" + w.getSpawnLocation().getBlockX() + ", "
                + w.getSpawnLocation().getBlockY() + ", " + w.getSpawnLocation().getBlockZ()
                + "), PvP on, flat bedrock.");
        return w;
    }
}
