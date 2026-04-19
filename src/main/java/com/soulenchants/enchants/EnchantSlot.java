package com.soulenchants.enchants;

import org.bukkit.Material;

public enum EnchantSlot {
    SWORD("Sword"),
    BOW("Bow"),
    ARMOR("Any armor piece"),
    HELMET("Helmet"),
    CHESTPLATE("Chestplate"),
    LEGGINGS("Leggings"),
    BOOTS("Boots"),
    PICKAXE("Pickaxe"),
    AXE("Axe"),
    SHOVEL("Shovel"),
    TOOL("Any tool"),
    ANY("Any item");

    private final String display;
    EnchantSlot(String display) { this.display = display; }
    public String getDisplay() { return display; }

    public boolean matches(Material m) {
        if (m == null) return false;
        String n = m.name();
        switch (this) {
            case SWORD:      return n.endsWith("_SWORD");
            case BOW:        return m == Material.BOW;
            case ARMOR:      return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                                  || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
            case HELMET:     return n.endsWith("_HELMET");
            case CHESTPLATE: return n.endsWith("_CHESTPLATE");
            case LEGGINGS:   return n.endsWith("_LEGGINGS");
            case BOOTS:      return n.endsWith("_BOOTS");
            case PICKAXE:    return n.endsWith("_PICKAXE");
            case AXE:        return n.endsWith("_AXE") && !n.endsWith("_PICKAXE");
            case SHOVEL:     return n.endsWith("_SPADE");
            case TOOL:       return n.endsWith("_PICKAXE") || n.endsWith("_AXE")
                                  || n.endsWith("_SPADE") || n.endsWith("_HOE");
            case ANY:        return true;
        }
        return false;
    }
}
