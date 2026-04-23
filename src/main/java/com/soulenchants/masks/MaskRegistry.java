package com.soulenchants.masks;

import org.bukkit.Material;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Starter roster of masks. Operators can add more via runtime registration. */
public final class MaskRegistry {

    private static final Map<String, Mask> MASKS = new LinkedHashMap<>();

    private MaskRegistry() {}

    public static void registerDefaults() {
        MASKS.clear();
        register(new Mask("pumpkin",     "Pumpkin Head",   Material.PUMPKIN,   (short) 0));
        register(new Mask("jack",        "Jack-o'-Lantern", Material.JACK_O_LANTERN, (short) 0));
        register(new Mask("dragon_head", "Dragon Skull",   Material.SKULL_ITEM,(short) 5));
        register(new Mask("wither_head", "Wither Skull",   Material.SKULL_ITEM,(short) 1));
        register(new Mask("zombie_head", "Zombie Veil",    Material.SKULL_ITEM,(short) 2));
        register(new Mask("skeleton_head","Skeletal Crown",Material.SKULL_ITEM,(short) 0));
        register(new Mask("creeper_head","Creeper Mask",   Material.SKULL_ITEM,(short) 4));
    }

    public static void register(Mask m) { MASKS.put(m.getId(), m); }
    public static Mask get(String id)   { return id == null ? null : MASKS.get(id); }
    public static Collection<Mask> all(){ return MASKS.values(); }
}
