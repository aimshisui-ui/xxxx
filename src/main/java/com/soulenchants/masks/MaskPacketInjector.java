package com.soulenchants.masks;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * ProtocolLib packet hook — intercepts ENTITY_EQUIPMENT packets, and if
 * the wearer's real helmet has an attached mask (via se_mask_attached
 * NBT), substitutes the mask's visual ItemStack before the packet leaves
 * the server. Other players see the mask; the wearer's real helmet
 * (and its enchants, durability, armor points) stays untouched.
 *
 * Fully reflection-driven so the plugin compiles without ProtocolLib on
 * the classpath. If ProtocolLib isn't installed at runtime, attach()
 * logs a single warning and every equipment packet goes through
 * unmodified.
 */
public final class MaskPacketInjector {

    private final SoulEnchants plugin;
    private boolean attached;

    public MaskPacketInjector(SoulEnchants plugin) { this.plugin = plugin; }

    public void attach() {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            plugin.getLogger().info("[masks] ProtocolLib not found — masks will render as real helmets");
            return;
        }
        try {
            Class<?> protocolLib = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object manager = protocolLib.getMethod("getProtocolManager").invoke(null);

            Class<?> packetTypePlay = Class.forName("com.comphenix.protocol.PacketType$Play$Server");
            Object equipPacketType = packetTypePlay.getField("ENTITY_EQUIPMENT").get(null);

            Class<?> packetListener = Class.forName("com.comphenix.protocol.events.PacketListener");
            Class<?> packetType     = Class.forName("com.comphenix.protocol.PacketType");

            Object packetTypeArray = java.lang.reflect.Array.newInstance(packetType, 1);
            java.lang.reflect.Array.set(packetTypeArray, 0, equipPacketType);

            // Build a Proxy that implements PacketListener so we don't need
            // ProtocolLib on the compile classpath.
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                    packetListener.getClassLoader(),
                    new Class<?>[]{packetListener},
                    (proxy, method, args) -> handle(method, args));

            Method addListener = manager.getClass().getMethod("addPacketListener", packetListener);
            addListener.invoke(manager, listener);

            this.attached = true;
            plugin.getLogger().info("[masks] ProtocolLib hook attached — attached masks will render");
        } catch (Throwable t) {
            plugin.getLogger().warning("[masks] ProtocolLib hook failed: " + t.getClass().getSimpleName()
                    + ": " + t.getMessage() + " — masks will render as real helmets");
        }
    }

    public boolean isAttached() { return attached; }

    private Object handle(Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if ("onPacketSending".equals(name) && args != null && args.length >= 1) {
            rewriteHelmetPacket(args[0]);
            return null;
        }
        if ("getPlugin".equals(name))             return plugin;
        if ("getSendingWhitelist".equals(name))   return null;
        if ("getReceivingWhitelist".equals(name)) return null;
        if ("toString".equals(name))              return "MaskPacketInjector$Proxy";
        if ("hashCode".equals(name))              return System.identityHashCode(this);
        if ("equals".equals(name))                return args != null && args.length == 1 && args[0] == this;
        return null;
    }

    private void rewriteHelmetPacket(Object event) {
        try {
            Method getPacket = event.getClass().getMethod("getPacket");
            Object packet = getPacket.invoke(event);

            Method getIntegers = packet.getClass().getMethod("getIntegers");
            Object integers = getIntegers.invoke(packet);
            Method readInt = integers.getClass().getMethod("read", int.class);
            int entityId = (Integer) readInt.invoke(integers, 0);
            int armorSlot = (Integer) readInt.invoke(integers, 1);
            if (armorSlot != 4) return; // 4 = helmet slot in 1.8

            Player wearer = null;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getEntityId() == entityId) { wearer = online; break; }
            }
            if (wearer == null) return;

            ItemStack realHelmet = wearer.getInventory().getHelmet();
            String maskId = MaskRegistry.attachedMaskId(realHelmet);
            if (maskId == null) return;
            Mask mask = MaskRegistry.get(maskId);
            if (mask == null) return;

            // Swap the ItemStack modifier slot 0 to the mask's visual.
            Method getItemModifier = packet.getClass().getMethod("getItemModifier");
            Object itemModifier = getItemModifier.invoke(packet);
            Method write = itemModifier.getClass().getMethod("write", int.class, Object.class);
            write.invoke(itemModifier, 0, mask.buildVisual());
        } catch (Throwable t) {
            // One failed packet is fine — don't spam the log.
        }
    }
}
