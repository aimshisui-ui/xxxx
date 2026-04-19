package com.soulenchants.enchants;

import org.bukkit.ChatColor;

public enum EnchantTier {
    COMMON      (ChatColor.GRAY,   "Simple",       1.0),
    UNCOMMON    (ChatColor.GREEN,  "Uncommon",     0.5),
    RARE        (ChatColor.AQUA,   "Rare",         0.2),
    EPIC        (ChatColor.YELLOW, "Elite",        0.08),
    LEGENDARY   (ChatColor.GOLD,   "Legendary",    0.03),
    SOUL_ENCHANT(ChatColor.RED,    "Soul Enchant", 0.005);

    private final ChatColor color;
    private final String display;
    private final double dropWeight;

    EnchantTier(ChatColor color, String display, double dropWeight) {
        this.color = color;
        this.display = display;
        this.dropWeight = dropWeight;
    }

    public ChatColor getColor() { return color; }
    public String getDisplay() { return display; }
    public double getDropWeight() { return dropWeight; }
    public String coloredName() { return color + display; }
    public boolean isSoul() { return this == SOUL_ENCHANT; }
}
