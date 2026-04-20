package com.soulenchants.loot;

import org.bukkit.ChatColor;

public enum LootRarity {
    FILLER   ("Filler",   ChatColor.WHITE),
    COMMON   ("Common",   ChatColor.GRAY),
    UNCOMMON ("Uncommon", ChatColor.GREEN),
    RARE     ("Rare",     ChatColor.AQUA),
    EPIC     ("Epic",     ChatColor.GOLD),
    BOSS     ("Boss",     ChatColor.DARK_BLUE),
    MYTHIC   ("Mythic",   ChatColor.LIGHT_PURPLE);

    private final String label;
    private final ChatColor color;

    LootRarity(String label, ChatColor color) {
        this.label = label; this.color = color;
    }
    public String getLabel() { return label; }
    public ChatColor getColor() { return color; }
}
