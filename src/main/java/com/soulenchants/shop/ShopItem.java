package com.soulenchants.shop;

import com.soulenchants.currency.SoulTier;
import org.bukkit.inventory.ItemStack;

/**
 * A single shop offering. `display` is the icon shown in the GUI (shows
 * price/tier in lore). `supplier` builds the actual ItemStack the player
 * receives when buying — used so every purchase of a random-within-tier
 * book rolls a fresh enchant.
 */
public class ShopItem {

    public final String id;
    public final ItemStack display;
    public final java.util.function.Supplier<ItemStack> supplier;
    public final long price;
    public final SoulTier tier;
    public final String category;
    public final long sellBack; // 0 = cannot sell back

    public ShopItem(String id, ItemStack display, java.util.function.Supplier<ItemStack> supplier,
                    long price, SoulTier tier, String category, long sellBack) {
        this.id = id;
        this.display = display;
        this.supplier = supplier;
        this.price = price;
        this.tier = tier;
        this.category = category;
        this.sellBack = sellBack;
    }
}
