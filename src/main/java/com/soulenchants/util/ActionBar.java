package com.soulenchants.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 1.8 action-bar helper. Sends PacketPlayOutChat with byte action = 2 via
 * NMS reflection, which renders above the hotbar.
 *
 * Class/Method handles are cached on first call. Silently no-ops on any
 * reflection failure so a Spigot rebuild with rearranged internals can't
 * crash gameplay.
 */
public final class ActionBar {

    private static volatile boolean ready;
    private static volatile boolean attempted;
    private static Method craftPlayerGetHandle;
    private static Field  playerConnectionField;
    private static Method connectionSendPacket;
    private static Method chatSerializerDeserialize;
    private static Constructor<?> packetCtor;

    private ActionBar() {}

    public static void send(Player p, String legacyText) {
        if (p == null || legacyText == null) return;
        if (!attempted) init(p);
        if (!ready) return;
        try {
            String json = "{\"text\":\"" + escape(legacyText) + "\"}";
            Object component = chatSerializerDeserialize.invoke(null, json);
            Object packet = packetCtor.newInstance(component, (byte) 2);
            Object handle = craftPlayerGetHandle.invoke(p);
            Object conn   = playerConnectionField.get(handle);
            connectionSendPacket.invoke(conn, packet);
        } catch (Throwable ignored) {}
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static synchronized void init(Player sample) {
        if (attempted) return;
        attempted = true;
        try {
            String version = sample.getServer().getClass().getPackage().getName();
            version = version.substring(version.lastIndexOf('.') + 1);
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
            ready = true;
        } catch (Throwable t) {
            ready = false;
        }
    }
}
