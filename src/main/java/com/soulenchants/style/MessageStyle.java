package com.soulenchants.style;

/**
 * Central palette for every user-facing string. One place to change a colour
 * and every surface follows — messages, lore, titles, scoreboards. Glyphs
 * picked from the default Minecraft font so they render consistently.
 *
 * Usage: prefer constants here over raw §-codes in listeners/commands. When
 * a string needs dynamic colour from a tier, call MessageStyle.tier(tier).
 */
public final class MessageStyle {

    private MessageStyle() {}

    // ──────────────── Brand palette ────────────────
    /** The primary "frame" colour — used for borders, accent lines, titles. */
    public static final String FRAME        = "§8";        // dark grey
    /** Accent gold used for soul-related text. */
    public static final String SOUL_GOLD    = "§6";
    /** Bright accent for positive feedback (heal, buff gained). */
    public static final String GOOD         = "§a";
    /** Warning / negative feedback (debuff applied). */
    public static final String BAD          = "§c";
    /** Secondary text — descriptions, numeric values. */
    public static final String MUTED        = "§7";
    /** Pure white for emphasized numbers inside muted text. */
    public static final String VALUE        = "§f";
    /** Bold modifier. */
    public static final String BOLD         = "§l";
    /** Italic modifier. */
    public static final String ITALIC       = "§o";
    /** Reset to inherit parent colour. */
    public static final String RESET        = "§r";

    // ──────────────── Tier chromatics ────────────────
    public static final String TIER_COMMON    = "§7";
    public static final String TIER_UNCOMMON  = "§a";
    public static final String TIER_RARE      = "§b";
    public static final String TIER_EPIC      = "§e";
    public static final String TIER_LEGENDARY = "§6";
    public static final String TIER_SOUL      = "§4";

    // ──────────────── Glyphs ────────────────
    public static final String DIAMOND        = "◆";       // ◆
    public static final String STAR           = "✦";       // ✦
    public static final String ARROW          = "➜";       // ➜
    public static final String CROSS_SWORDS   = "⚔";       // ⚔
    public static final String SHIELD         = "⚐";       // ⚐  (flag — 1.8 font trade-off)
    public static final String SKULL          = "☠";       // ☠
    public static final String HEART          = "❤";       // ❤
    public static final String SOUL_ORB       = "❖";       // ❖
    public static final String BAR            = "━";       // ━
    public static final String SEP            = "§8 " + BAR + " ";

    /** Fixed-width horizontal rule for chat separators. */
    public static final String RULE = FRAME + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR +
            BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR +
            BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR + BAR;

    /** Plugin-wide chat prefix. */
    public static final String PREFIX = FRAME + "✦ " + SOUL_GOLD + BOLD + "SoulEnchants " + FRAME + BAR + " " + RESET;

    /** Colour the display name for a tier label. */
    public static String tier(com.soulenchants.enchants.EnchantTier t) {
        if (t == null) return TIER_COMMON;
        switch (t) {
            case UNCOMMON:     return TIER_UNCOMMON;
            case RARE:         return TIER_RARE;
            case EPIC:         return TIER_EPIC;
            case LEGENDARY:    return TIER_LEGENDARY;
            case SOUL_ENCHANT: return TIER_SOUL;
            default:           return TIER_COMMON;
        }
    }

    /** Pretty a tier name with tier colour and a diamond glyph. */
    public static String tierBadge(com.soulenchants.enchants.EnchantTier t) {
        return tier(t) + BOLD + DIAMOND + " " + (t == null ? "COMMON" : t.name().replace('_', ' ')) + RESET;
    }

    /** Format a numeric value with bright-white emphasis against muted label text. */
    public static String labeledValue(String label, String value) {
        return MUTED + label + ": " + VALUE + value;
    }

    /** Shorthand for "[good] +X [muted label]" positive-feedback lines. */
    public static String posDelta(String amount, String label) {
        return GOOD + "+" + amount + " " + MUTED + label;
    }
}
