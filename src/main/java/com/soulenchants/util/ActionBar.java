package com.soulenchants.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 1.8 action-bar helper. Tries two paths, whichever the running Spigot
 * build supports:
 *
 *   1. Player.spigot().sendMessage(ChatMessageType.ACTION_BAR, BaseComponent[])
 *      — available on later 1.8.x builds and every modern Spigot.
 *   2. NMS PacketPlayOutChat(IChatBaseComponent, byte = 2) — falls back
 *      for older 1.8 builds that don't have the ChatMessageType overload.
 *
 * Class handles are cached on first successful resolution. Logs a single
 * warning on startup if NEITHER path can be wired up so developers see
 * why hotbar text isn't rendering.
 */
public final class ActionBar {

    private static volatile int mode;              // 0 = untried, 1 = spigot-api, 2 = nms, -1 = dead
    // spigot path
    private static Method spigotSendTyped;
    private static Object actionBarEnum;
    // nms path
    private static Method craftPlayerGetHandle;
    private static Field  playerConnectionField;
    private static Method connectionSendPacket;
    private static Method chatSerializerDeserialize;
    private static Constructor<?> packetCtor;

    private ActionBar() {}

    public static void send(Player p, String legacyText) {
        if (p == null || legacyText == null) return;
        if (mode == 0) initAll(p);
        if (mode == 1) {
            try {
                BaseComponent[] comps = TextComponent.fromLegacyText(legacyText);
                spigotSendTyped.invoke(p.spigot(), actionBarEnum, comps);
                return;
            } catch (Throwable t) {
                mode = 2;   // Try the NMS path on next call.
            }
        }
        if (mode == 2) {
            try {
                String json = "{\"text\":\"" + escape(legacyText) + "\"}";
                Object component = chatSerializerDeserialize.invoke(null, json);
                Object packet = packetCtor.newInstance(component, (byte) 2);
                Object handle = craftPlayerGetHandle.invoke(p);
                Object conn   = playerConnectionField.get(handle);
                connectionSendPacket.invoke(conn, packet);
                return;
            } catch (Throwable t) {
                mode = -1;
                Bukkit.getLogger().warning("[SE ActionBar] disabled after failure: " + t);
            }
        }
    }

    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static synchronized void initAll(Player sample) {
        if (mode != 0) return;
        // Attempt 1 — Spigot ChatMessageType overload
        try {
            Class<?> cmt = Class.forName("net.md_5.bungee.api.ChatMessageType");
            actionBarEnum = Enum.valueOf((Class<Enum>) cmt.asSubclass(Enum.class), "ACTION_BAR");
            spigotSendTyped = Player.Spigot.class.getMethod("sendMessage", cmt,
                    Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;"));
            mode = 1;
            return;
        } catch (Throwable ignored) { /* try NMS */ }

        // Attempt 2 — NMS PacketPlayOutChat reflection
        try {
            String pkg = sample.getServer().getClass().getPackage().getName();
            String version = pkg.substring(pkg.lastIndexOf('.') + 1);
            Class<?> craftPlayer  = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            craftPlayerGetHandle  = craftPlayer.getMethod("getHandle");
            Class<?> entityPlayer = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            playerConnectionField = entityPlayer.getField("playerConnection");
            Class<?> playerConn   = Class.forName("net.minecraft.server." + version + ".PlayerConnection");
            Class<?> packetClass  = Class.forName("net.minecraft.server." + version + ".Packet");
            connectionSendPacket  = playerConn.getMethod("sendPacket", packetClass);
            Class<?> iChatBase    = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> chatSerializer;
            try {
                chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            } catch (ClassNotFoundException e) {
                chatSerializer = Class.forName("net.minecraft.server." + version + ".ChatSerializer");
            }
            chatSerializerDeserialize = chatSerializer.getMethod("a", String.class);
            Class<?> packetPlayOutChat = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            packetCtor = packetPlayOutChat.getConstructor(iChatBase, byte.class);
            mode = 2;
        } catch (Throwable t) {
            mode = -1;
            Bukkit.getLogger().warning("[SE ActionBar] init failed — action bar output disabled: " + t);
        }
    }
}
