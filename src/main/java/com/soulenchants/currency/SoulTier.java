package com.soulenchants.currency;

import org.bukkit.ChatColor;

/**
 * Lifetime-souls progression tiers. Crossing a threshold is permanent —
 * the +souls-per-kill perk on higher tiers can't be lost by spending
 * souls or dying.
 */
public enum SoulTier {
    INITIATE  ("Initiate",  0L,           ChatColor.DARK_GRAY,   false),
    BRONZE    ("Bronze",    5_000L,       ChatColor.GRAY,        false),
    SILVER    ("Silver",    25_000L,      ChatColor.WHITE,       true),   // +1 souls/kill
    GOLD      ("Gold",      100_000L,     ChatColor.YELLOW,      true),
    VEILED    ("Veiled",    300_000L,     ChatColor.LIGHT_PURPLE, true),
    SOULBOUND ("Soulbound", 1_000_000L,   ChatColor.DARK_RED,    true);

    private final String label;
    private final long threshold;
    private final ChatColor color;
    private final boolean bonusPerKill; // adds +1 to mob soul drops

    SoulTier(String label, long threshold, ChatColor color, boolean bonusPerKill) {
        this.label = label;
        this.threshold = threshold;
        this.color = color;
        this.bonusPerKill = bonusPerKill;
    }

    public String getLabel() { return label; }
    public long getThreshold() { return threshold; }
    public ChatColor getColor() { return color; }
    public boolean grantsBonusPerKill() { return bonusPerKill; }

    /** Kept for display-only callers that still reference it. Always 0 —
     *  tier HP bonuses were removed in favour of the Vital enchant. */
    public int getBonusMaxHp() { return 0; }

    /** Pretty-printed prefix for chat / scoreboard. */
    public String prefix() {
        return color + "[" + (this == SOULBOUND ? ChatColor.BOLD + label : label) + color + "]";
    }

    /** Highest tier achieved given a lifetime-souls value. */
    public static SoulTier forLifetime(long lifetime) {
        SoulTier best = INITIATE;
        for (SoulTier t : values()) {
            if (lifetime >= t.threshold) best = t;
        }
        return best;
    }

    /** Tier strictly above the given one, or null at SOULBOUND. */
    public SoulTier next() {
        SoulTier[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }
}
