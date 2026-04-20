package com.soulenchants.loot;

import com.soulenchants.shop.LootBox;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Central registry of every named custom item that admins can drop into
 * mob/boss loot tables via /ce loot. Each entry pairs a stable id (matches
 * the NBT loot id where possible) with a factory function that produces a
 * fresh ItemStack copy.
 *
 * Categories are kept for the picker UI grouping.
 */
public final class CustomLootRegistry {

    public enum Category { BOSS, CRAFTED, RARE_GEAR, REAGENTS, LOOTBOXES, VANILLA }

    public static final class Entry {
        public final String id;
        public final String displayName;
        public final Category category;
        public final Supplier<ItemStack> factory;
        public Entry(String id, String displayName, Category category, Supplier<ItemStack> factory) {
            this.id = id; this.displayName = displayName;
            this.category = category; this.factory = factory;
        }
        public ItemStack create() { return factory.get(); }
    }

    private static final Map<String, Entry> BY_ID = new LinkedHashMap<>();
    private static boolean initialized = false;

    private CustomLootRegistry() {}

    public static synchronized void initIfNeeded() {
        if (initialized) return;
        initialized = true;

        // ── BOSS-tier items ──
        add("ironhearts_hammer",      "Ironheart's Hammer",        Category.BOSS,      BossLootItems::ironheartsHammer);
        add("colossus_plating_core",  "Colossus Plating Core",     Category.BOSS,      BossLootItems::colossusPlatingCore);
        add("veilseekers_mantle",     "Veilseeker's Mantle",       Category.BOSS,      BossLootItems::veilseekersMantle);
        add("loom_of_eternity",       "Loom of Eternity",          Category.BOSS,      BossLootItems::loomOfEternity);
        add("apex_carapace",          "Apex Carapace",             Category.BOSS,      BossLootItems::apexCarapace);
        add("heart_of_the_forge",     "Heart of the Forge",        Category.BOSS,      BossLootItems::heartOfTheForge);
        add("veil_sigil",             "Veil Sigil",                Category.BOSS,      BossLootItems::veilSigil);
        add("crown_of_the_hollow",    "Crown of the Hollow",       Category.BOSS,      BossLootItems::crownOfTheHollow);
        add("crimson_tongue",         "Crimson Tongue (Mythic)",   Category.BOSS,      BossLootItems::crimsonTongue);
        add("wraithcleaver",          "Wraithcleaver (Mythic)",    Category.BOSS,      BossLootItems::wraithcleaver);

        // ── Crafted gear ──
        add("forged_bulwark_plate",   "Forged Bulwark Plate",      Category.CRAFTED,   BossLootItems::forgedBulwarkPlate);
        add("veiled_edge",            "Veiled Edge",               Category.CRAFTED,   BossLootItems::veiledEdge);
        add("aether_bow",             "Aether Bow",                Category.CRAFTED,   BossLootItems::aetherBow);
        add("shardheart_blade",       "Shardheart Blade",          Category.CRAFTED,   BossLootItems::shardheartBlade);
        add("dripstone_cuirass",      "Dripstone Cuirass",         Category.CRAFTED,   BossLootItems::dripstoneCuirass);
        add("void_spun_boots",        "Void-Spun Boots",           Category.CRAFTED,   BossLootItems::voidSpunBoots);

        // ── Rare gear ──
        add("earthshaker_treads",     "Earthshaker Treads",        Category.RARE_GEAR, BossLootItems::earthshakerTreads);
        add("shadowstep_sandals",     "Shadowstep Sandals",        Category.RARE_GEAR, BossLootItems::shadowstepSandals);
        add("stoneskin_tonic",        "Stoneskin Tonic",           Category.RARE_GEAR, BossLootItems::stoneskinTonic);
        add("phasing_elixir",         "Phasing Elixir",            Category.RARE_GEAR, BossLootItems::phasingElixir);

        // ── Reagents ──
        add("colossus_slag",          "Colossus Slag",             Category.REAGENTS,  () -> BossLootItems.colossusSlag(1));
        add("iron_heart_fragment",    "Iron Heart Fragment",       Category.REAGENTS,  () -> BossLootItems.ironHeartFragment(1));
        add("forged_ember",           "Forged Ember",              Category.REAGENTS,  () -> BossLootItems.forgedEmber(1));
        add("reinforced_plating",     "Reinforced Plating",        Category.REAGENTS,  () -> BossLootItems.reinforcedPlating(1));
        add("bulwark_core",           "Bulwark Core",              Category.REAGENTS,  BossLootItems::bulwarkCore);
        add("veil_thread",            "Veil Thread",               Category.REAGENTS,  () -> BossLootItems.veilThread(1));
        add("frayed_soul",            "Frayed Soul",               Category.REAGENTS,  () -> BossLootItems.frayedSoul(1));
        add("echoing_strand",         "Echoing Strand",            Category.REAGENTS,  () -> BossLootItems.echoingStrand(1));
        add("phantom_silk",           "Phantom Silk",              Category.REAGENTS,  () -> BossLootItems.phantomSilk(1));
        add("veil_essence",           "Veil Essence",              Category.REAGENTS,  BossLootItems::veilEssence);
        add("pale_shard",             "Pale Shard",                Category.REAGENTS,  () -> BossLootItems.paleShard(1));
        add("echo_shard",             "Echo Shard",                Category.REAGENTS,  () -> BossLootItems.echoShard(1));
        add("dripstone_tear",         "Dripstone Tear",            Category.REAGENTS,  () -> BossLootItems.dripstoneTear(1));
        add("hollow_fragment",        "Hollow Fragment",           Category.REAGENTS,  () -> BossLootItems.hollowFragment(1));
        add("void_essence_reagent",   "Void Essence (small)",      Category.REAGENTS,  () -> BossLootItems.voidEssence(1));

        // ── Loot boxes ──
        add("box_bronze",             "Bronze Loot Box",           Category.LOOTBOXES, () -> LootBox.item(LootBox.Kind.BRONZE));
        add("box_silver",             "Silver Loot Box",           Category.LOOTBOXES, () -> LootBox.item(LootBox.Kind.SILVER));
        add("box_gold",               "Gold Loot Box",             Category.LOOTBOXES, () -> LootBox.item(LootBox.Kind.GOLD));
        add("box_boss",               "Boss Loot Box",             Category.LOOTBOXES, () -> LootBox.item(LootBox.Kind.BOSS));
        add("transmog_scroll",        "Transmog Scroll",           Category.LOOTBOXES, com.soulenchants.items.TransmogScroll::item);

        // ── Vanilla gear / drops ──
        // Anything you might want to filter out of routine mob/block drops. The id
        // uses the "vanilla:" prefix so it doesn't collide with custom NBT ids.
        addVanilla(org.bukkit.Material.ROTTEN_FLESH);
        addVanilla(org.bukkit.Material.BONE);
        addVanilla(org.bukkit.Material.STRING);
        addVanilla(org.bukkit.Material.SPIDER_EYE);
        addVanilla(org.bukkit.Material.SULPHUR);
        addVanilla(org.bukkit.Material.FEATHER);
        addVanilla(org.bukkit.Material.LEATHER);
        addVanilla(org.bukkit.Material.RAW_BEEF);
        addVanilla(org.bukkit.Material.RAW_CHICKEN);
        addVanilla(org.bukkit.Material.PORK);
        addVanilla(org.bukkit.Material.WOOL);
        addVanilla(org.bukkit.Material.ARROW);
        addVanilla(org.bukkit.Material.IRON_INGOT);
        addVanilla(org.bukkit.Material.GOLD_INGOT);
        addVanilla(org.bukkit.Material.SADDLE);
        addVanilla(org.bukkit.Material.NETHER_STAR);
        addVanilla(org.bukkit.Material.ENDER_PEARL);
        addVanilla(org.bukkit.Material.GHAST_TEAR);
        addVanilla(org.bukkit.Material.BLAZE_ROD);
        addVanilla(org.bukkit.Material.MAGMA_CREAM);
        addVanilla(org.bukkit.Material.SLIME_BALL);
        addVanilla(org.bukkit.Material.IRON_SWORD);
        addVanilla(org.bukkit.Material.IRON_HELMET);
        addVanilla(org.bukkit.Material.IRON_CHESTPLATE);
        addVanilla(org.bukkit.Material.IRON_LEGGINGS);
        addVanilla(org.bukkit.Material.IRON_BOOTS);
        addVanilla(org.bukkit.Material.GOLD_SWORD);
        addVanilla(org.bukkit.Material.GOLD_HELMET);
        addVanilla(org.bukkit.Material.GOLD_CHESTPLATE);
        addVanilla(org.bukkit.Material.GOLD_LEGGINGS);
        addVanilla(org.bukkit.Material.GOLD_BOOTS);
        addVanilla(org.bukkit.Material.LEATHER_HELMET);
        addVanilla(org.bukkit.Material.LEATHER_CHESTPLATE);
        addVanilla(org.bukkit.Material.LEATHER_LEGGINGS);
        addVanilla(org.bukkit.Material.LEATHER_BOOTS);
        addVanilla(org.bukkit.Material.BOW);
    }

    /** Convenience: register a vanilla material with id "vanilla:MATERIAL_NAME". */
    private static void addVanilla(org.bukkit.Material mat) {
        String id = "vanilla:" + mat.name();
        String pretty = prettify(mat.name());
        add(id, pretty, Category.VANILLA, () -> new org.bukkit.inventory.ItemStack(mat));
    }

    private static String prettify(String materialName) {
        StringBuilder sb = new StringBuilder();
        for (String w : materialName.split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static void add(String id, String name, Category cat, Supplier<ItemStack> fn) {
        BY_ID.put(id, new Entry(id, name, cat, fn));
    }

    public static Entry get(String id) {
        initIfNeeded();
        return BY_ID.get(id);
    }

    public static List<Entry> all() {
        initIfNeeded();
        return new ArrayList<>(BY_ID.values());
    }

    public static List<Entry> byCategory(Category cat) {
        initIfNeeded();
        List<Entry> out = new ArrayList<>();
        for (Entry e : BY_ID.values()) if (e.category == cat) out.add(e);
        return out;
    }
}
