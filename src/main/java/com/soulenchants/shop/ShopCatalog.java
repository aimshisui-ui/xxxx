package com.soulenchants.shop;

import com.soulenchants.currency.SoulTier;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.enchants.EnchantTier;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.loot.BossLootItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Static catalog of every shop offering. Categories drive the GUI tabs.
 *
 * Books are random-within-tier: each purchase rolls a fresh enchant from
 * that tier's pool. This keeps bosses as the only guaranteed path to
 * specific high-tier enchants.
 */
public final class ShopCatalog {

    public static final String CAT_BOOKS       = "books";
    public static final String CAT_REAGENTS    = "reagents";
    public static final String CAT_LOOT_BOXES  = "lootboxes";
    public static final String CAT_CONSUMABLES = "consumables";
    public static final String CAT_FEATURED    = "featured";

    private static final List<ShopItem> ALL = new ArrayList<>();

    private ShopCatalog() {}

    public static List<ShopItem> all() { return ALL; }

    public static List<ShopItem> byCategory(String category) {
        List<ShopItem> out = new ArrayList<>();
        for (ShopItem s : ALL) if (s.category.equals(category)) out.add(s);
        return out;
    }

    public static ShopItem byId(String id) {
        for (ShopItem s : ALL) if (s.id.equals(id)) return s;
        return null;
    }

    public static void register() {
        ALL.clear();

        // ── BOOKS ── random-within-tier, price scales with tier ──
        addBookTier("common",      EnchantTier.COMMON,      80,   SoulTier.INITIATE);
        addBookTier("uncommon",    EnchantTier.UNCOMMON,    200,  SoulTier.BRONZE);
        addBookTier("rare",        EnchantTier.RARE,        500,  SoulTier.SILVER);
        addBookTier("epic",        EnchantTier.EPIC,        1200, SoulTier.GOLD);
        addBookTier("legendary",   EnchantTier.LEGENDARY,   3000, SoulTier.VEILED);
        addBookTier("soul",        EnchantTier.SOUL_ENCHANT,8000, SoulTier.SOULBOUND);

        // ── CONSUMABLES ──
        ALL.add(new ShopItem("dust25",  ItemFactories.dust(25),  () -> ItemFactories.dust(25),  100,  SoulTier.INITIATE, CAT_CONSUMABLES, 25));
        ALL.add(new ShopItem("dust50",  ItemFactories.dust(50),  () -> ItemFactories.dust(50),  250,  SoulTier.BRONZE,   CAT_CONSUMABLES, 60));
        ALL.add(new ShopItem("dust75",  ItemFactories.dust(75),  () -> ItemFactories.dust(75),  500,  SoulTier.SILVER,   CAT_CONSUMABLES, 125));
        ALL.add(new ShopItem("dust100", ItemFactories.dust(100), () -> ItemFactories.dust(100), 1000, SoulTier.GOLD,     CAT_CONSUMABLES, 250));
        ALL.add(new ShopItem("scrollwhite", ItemFactories.whiteScroll(), ItemFactories::whiteScroll, 150, SoulTier.INITIATE, CAT_CONSUMABLES, 40));
        ALL.add(new ShopItem("scrollblack", ItemFactories.blackScroll(), ItemFactories::blackScroll, 200, SoulTier.INITIATE, CAT_CONSUMABLES, 50));

        // ── REAGENTS ── cheap, buy stacks ──
        ALL.add(reagent("veilthread",         BossLootItems.veilThread(8),         () -> BossLootItems.veilThread(8),         80,  SoulTier.BRONZE));
        ALL.add(reagent("colossusslag",       BossLootItems.colossusSlag(8),       () -> BossLootItems.colossusSlag(8),       80,  SoulTier.BRONZE));
        ALL.add(reagent("ironheartfragment",  BossLootItems.ironHeartFragment(4),  () -> BossLootItems.ironHeartFragment(4),  180, SoulTier.BRONZE));
        ALL.add(reagent("frayedsoul",         BossLootItems.frayedSoul(4),         () -> BossLootItems.frayedSoul(4),         180, SoulTier.BRONZE));
        ALL.add(reagent("forgedember",        BossLootItems.forgedEmber(2),        () -> BossLootItems.forgedEmber(2),        250, SoulTier.SILVER));
        ALL.add(reagent("reinforcedplating",  BossLootItems.reinforcedPlating(2),  () -> BossLootItems.reinforcedPlating(2),  250, SoulTier.SILVER));
        ALL.add(reagent("echoingstrand",      BossLootItems.echoingStrand(2),      () -> BossLootItems.echoingStrand(2),      250, SoulTier.SILVER));
        ALL.add(reagent("phantomsilk",        BossLootItems.phantomSilk(2),        () -> BossLootItems.phantomSilk(2),        250, SoulTier.SILVER));
        ALL.add(reagent("bulwarkcore",        BossLootItems.bulwarkCore(),         BossLootItems::bulwarkCore,         3000, SoulTier.GOLD));
        ALL.add(reagent("veilessence",        BossLootItems.veilEssence(),         BossLootItems::veilEssence,         3000, SoulTier.GOLD));

        // ── LOOT BOXES ──
        ALL.add(new ShopItem("box_bronze", LootBox.item(LootBox.Kind.BRONZE), () -> LootBox.item(LootBox.Kind.BRONZE), 250,  SoulTier.INITIATE, CAT_LOOT_BOXES, 0));
        ALL.add(new ShopItem("box_silver", LootBox.item(LootBox.Kind.SILVER), () -> LootBox.item(LootBox.Kind.SILVER), 800,  SoulTier.BRONZE,   CAT_LOOT_BOXES, 0));
        ALL.add(new ShopItem("box_gold",   LootBox.item(LootBox.Kind.GOLD),   () -> LootBox.item(LootBox.Kind.GOLD),   2500, SoulTier.SILVER,   CAT_LOOT_BOXES, 0));
        ALL.add(new ShopItem("box_boss",   LootBox.item(LootBox.Kind.BOSS),   () -> LootBox.item(LootBox.Kind.BOSS),   7500, SoulTier.GOLD,     CAT_LOOT_BOXES, 0));
    }

    private static void addBookTier(String idPrefix, EnchantTier tier, long price, SoulTier required) {
        // Display shows a placeholder book icon
        CustomEnchant example = null;
        for (CustomEnchant e : EnchantRegistry.all()) {
            if (e.getTier() == tier) { example = e; break; }
        }
        if (example == null) return;
        final CustomEnchant ex = example;
        ItemStack display = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta m = display.getItemMeta();
        m.setDisplayName(tier.getColor() + "" + ChatColor.BOLD + titleize(tier.name()) + " Book");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  ");
        lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "A random " + tier.getColor() + titleize(tier.name())
                + ChatColor.GRAY + "" + ChatColor.ITALIC + " enchant");
        lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "at a random level.");
        lore.add("");
        lore.add(ChatColor.GOLD + "Price: " + ChatColor.YELLOW + price + " souls");
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  ");
        m.setLore(lore);
        display.setItemMeta(m);

        final EnchantTier ftier = tier;
        ALL.add(new ShopItem("book_" + idPrefix, display, () -> {
            List<CustomEnchant> pool = new ArrayList<>();
            for (CustomEnchant e : EnchantRegistry.all()) if (e.getTier() == ftier) pool.add(e);
            if (pool.isEmpty()) return ItemFactories.book(ex, 1);
            CustomEnchant chosen = pool.get(new Random().nextInt(pool.size()));
            int lvl = 1 + new Random().nextInt(chosen.getMaxLevel());
            return ItemFactories.book(chosen, lvl);
        }, price, required, CAT_BOOKS, Math.max(1, price / 5)));
    }

    private static ShopItem reagent(String id, ItemStack display, java.util.function.Supplier<ItemStack> supplier,
                                    long price, SoulTier required) {
        return new ShopItem(id, display, supplier, price, required, CAT_REAGENTS, Math.max(1, price / 4));
    }

    private static String titleize(String raw) {
        String lower = raw.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
