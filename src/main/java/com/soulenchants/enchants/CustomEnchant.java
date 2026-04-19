package com.soulenchants.enchants;

public class CustomEnchant {

    private final String id;
    private final String displayName;
    private final EnchantTier tier;
    private final EnchantSlot slot;
    private final int maxLevel;
    private final String description;
    private final int soulCost; // per trigger, for Soul Enchants only

    public CustomEnchant(String id, String displayName, EnchantTier tier,
                         EnchantSlot slot, int maxLevel, String description) {
        this(id, displayName, tier, slot, maxLevel, description, 0);
    }

    public CustomEnchant(String id, String displayName, EnchantTier tier,
                         EnchantSlot slot, int maxLevel, String description, int soulCost) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.slot = slot;
        this.maxLevel = maxLevel;
        this.description = description;
        this.soulCost = soulCost;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public EnchantTier getTier() { return tier; }
    public EnchantSlot getSlot() { return slot; }
    public int getMaxLevel() { return maxLevel; }
    public String getDescription() { return description; }
    public int getSoulCost() { return soulCost; }

    public String formatLore(int level) {
        return tier.getColor() + displayName + " " + roman(level);
    }

    public static String roman(int n) {
        switch (n) {
            case 1: return "I";  case 2: return "II"; case 3: return "III";
            case 4: return "IV"; case 5: return "V";
            default: return String.valueOf(n);
        }
    }
}
