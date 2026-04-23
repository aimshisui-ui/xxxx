package com.soulenchants.masks;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * ProtocolLib-driven helmet override. Loaded reflectively so the plugin still
 * compiles and runs without ProtocolLib present.
 *
 * Hook points (1.8 ProtocolLib names):
 *   PacketType.Play.Server.ENTITY_EQUIPMENT — armor slot packet
 * If ProtocolLib is absent the whole subsystem no-ops and players see the
 * real helmet — graceful fallback.
 */
public final class MaskPacketInjector {

    private final SoulEnchants plugin;
    private final MaskManager manager;
    private boolean attached;

    public MaskPacketInjector(SoulEnchants plugin, MaskManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void attach() {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            plugin.getLogger().info("[masks] ProtocolLib not found — masks will no-op");
            return;
        }
        try {
            Class<?> protocolLib = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object manager = protocolLib.getMethod("getProtocolManager").invoke(null);

            Class<?> packetTypePlay = Class.forName("com.comphenix.protocol.PacketType$Play$Server");
            Object equipPacketType = packetTypePlay.getField("ENTITY_EQUIPMENT").get(null);

            Class<?> packetAdapter = Class.forName("com.comphenix.protocol.events.PacketAdapter");
            Class<?> packetEvent = Class.forName("com.comphenix.protocol.events.PacketEvent");
            Class<?> packetListener = Class.forName("com.comphenix.protocol.events.PacketListener");

            // Build an anonymous adapter via ProtocolLib's dynamic listener factory.
            // Simpler path: use PacketAdapter.params + anonymous subclass — but we're doing it all reflectively.
            // For safety, wire a Java Proxy that dispatches only onPacketSending.
            java.lang.reflect.Constructor<?> ctor = packetAdapter.getConstructor(
                    org.bukkit.plugin.Plugin.class,
                    Class.forName("com.comphenix.protocol.PacketType[]"));
            // Build the array param as a 1-element PacketType[]
            Object packetTypeArray = java.lang.reflect.Array.newInstance(
                    Class.forName("com.comphenix.protocol.PacketType"), 1);
            java.lang.reflect.Array.set(packetTypeArray, 0, equipPacketType);

            // Use ProtocolLib's simpler factory: addPacketListener expects a PacketListener.
            // We build a subclass via Javassist-style dynamic proxy fallback — but on
            // 1.8 ProtocolLib this is painful. Instead we use the simple lambda-style
            // addPacketListener(PacketListener) combined with a Proxy implementing
            // PacketListener interface.
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                    packetListener.getClassLoader(),
                    new Class<?>[]{packetListener},
                    (proxy, method, args) -> handle(method, args));

            Method addListener = manager.getClass().getMethod("addPacketListener", packetListener);
            addListener.invoke(manager, listener);

            this.attached = true;
            plugin.getLogger().info("[masks] ProtocolLib hook attached");
        } catch (Throwable t) {
            plugin.getLogger().warning("[masks] ProtocolLib hook failed: " + t.getClass().getSimpleName()
                    + ": " + t.getMessage() + " — masks will no-op");
        }
    }

    public boolean isAttached() { return attached; }

    /** Proxy dispatcher — handles onPacketSending, passthrough for everything else. */
    private Object handle(Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if ("onPacketSending".equals(name) && args != null && args.length >= 1) {
            Object event = args[0];
            rewriteHelmetPacket(event);
            return null;
        }
        if ("getPlugin".equals(name)) return plugin;
        if ("getSendingWhitelist".equals(name)) return null;
        if ("getReceivingWhitelist".equals(name)) return null;
        // equals/hashCode/toString on the proxy
        if ("toString".equals(name)) return "MaskPacketInjector$Proxy";
        if ("hashCode".equals(name)) return System.identityHashCode(this);
        if ("equals".equals(name))   return args != null && args.length == 1 && args[0] == this;
        return null;
    }

    private void rewriteHelmetPacket(Object event) {
        try {
            // event.getPacket().getItemModifier() (for slot 4 helmet)
            // event.getPacket().getIntegers() (entity id)
            // event.getPlayer() — the *receiver*, not the wearer
            Method getPacket = event.getClass().getMethod("getPacket");
            Object packet = getPacket.invoke(event);

            // Entity id is in integers.read(0); armor slot in integers.read(1)
            Method getIntegers = packet.getClass().getMethod("getIntegers");
            Object integers = getIntegers.invoke(packet);
            Method readInt = integers.getClass().getMethod("read", int.class);
            int entityId = (Integer) readInt.invoke(integers, 0);
            int armorSlot = (Integer) readInt.invoke(integers, 1);
            if (armorSlot != 4) return; // 4 = helmet in 1.8

            // Resolve the wearer by entity id.
            Player wearer = null;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getEntityId() == entityId) { wearer = online; break; }
            }
            if (wearer == null) return;
            String maskId = manager.getEquipped(wearer);
            if (maskId == null) return;
            Mask mask = MaskRegistry.get(maskId);
            if (mask == null) return;

            // Rewrite the ItemStack modifier slot 0 to the mask visual.
            Method getItemModifier = packet.getClass().getMethod("getItemModifier");
            Object itemModifier = getItemModifier.invoke(packet);
            Method write = itemModifier.getClass().getMethod("write", int.class, Object.class);
            ItemStack visual = mask.buildVisual();
            write.invoke(itemModifier, 0, visual);
        } catch (Throwable t) {
            // Swallow — one failed packet is fine, don't spam log.
        }
    }
}
