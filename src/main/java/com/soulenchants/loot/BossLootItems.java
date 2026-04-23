package com.soulenchants.loot;

import com.soulenchants.items.ItemUtil;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Factories for every special loot item. Every item carries an `se_loot_id`
 * NBT tag so the listener can identify it for ability handling.
 */
public final class BossLootItems {

    public static final String NBT_LOOT_ID = "se_loot_id";

    private static final String DIVIDER = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH
            + "                                  ";

    private BossLootItems() {}

    public static String getLootId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(NBT_LOOT_ID)) return null;
        return nbt.getString(NBT_LOOT_ID);
    }

    private static ItemStack tag(ItemStack item, String id) {
        NBTItem nbt = new NBTItem(item);
        nbt.setString(NBT_LOOT_ID, id);
        return nbt.getItem();
    }

    private static ItemStack make(Material mat, int amount, LootRarity r, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(r.getColor() + "" + ChatColor.BOLD + name);
        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        for (String line : loreLines) lore.add(line);
        lore.add("");
        lore.add(r.getColor() + "» " + r.getLabel());
        lore.add(DIVIDER);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Reagents (Ironheart side) ─
    public static ItemStack colossusSlag(int n)      { return tag(make(Material.OBSIDIAN, n, LootRarity.COMMON,
            "Colossus Slag",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "Hardened residue scraped from",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "the Colossus' fractured plating.",
            "",
            ChatColor.YELLOW + "» Crafting reagent"
    ), "colossus_slag"); }

    public static ItemStack ironHeartFragment(int n) { return tag(make(Material.NETHER_BRICK_ITEM, n, LootRarity.COMMON,
            "Iron Heart Fragment",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "A glowing shard, still humming",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "with mechanical life.",
            "",
            ChatColor.YELLOW + "» Crafting reagent"
    ), "iron_heart_fragment"); }

    public static ItemStack forgedEmber(int n)       { return tag(make(Material.BLAZE_POWDER, n, LootRarity.UNCOMMON,
            "Forged Ember",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "An ember that refuses",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "to ever fully cool.",
            "",
            ChatColor.YELLOW + "» Fire-craft reagent"
    ), "forged_ember"); }

    public static ItemStack reinforcedPlating(int n) { return tag(make(Material.IRON_BLOCK, n, LootRarity.UNCOMMON,
            "Reinforced Plating",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "A slab of impossibly dense iron,",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "perfect for armor smithing.",
            "",
            ChatColor.YELLOW + "» Armor-craft reagent"
    ), "reinforced_plating"); }

    public static ItemStack bulwarkCore() { return tag(make(Material.NETHER_STAR, 1, LootRarity.EPIC,
            "Bulwark Core",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "The pulsing nucleus of an",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "indestructible heart.",
            "",
            ChatColor.GOLD + "» Endgame upgrade catalyst"
    ), "bulwark_core"); }

    // ── Reagents (Veilweaver side) ─
    public static ItemStack veilThread(int n)    { return tag(make(Material.STRING, n, LootRarity.COMMON,
            "Veil Thread",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "A strand woven from",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "the fabric of reality itself.",
            "",
            ChatColor.YELLOW + "» Crafting reagent"
    ), "veil_thread"); }

    public static ItemStack frayedSoul(int n)    { return tag(make(Material.GHAST_TEAR, n, LootRarity.COMMON,
            "Frayed Soul",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "An echo of something that was",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "almost—but never—alive.",
            "",
            ChatColor.YELLOW + "» Crafting reagent"
    ), "frayed_soul"); }

    public static ItemStack echoingStrand(int n) { return tag(make(Material.QUARTZ, n, LootRarity.UNCOMMON,
            "Echoing Strand",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "It hums with the voice",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "of every reality.",
            "",
            ChatColor.YELLOW + "» Void-craft reagent"
    ), "echoing_strand"); }

    public static ItemStack phantomSilk(int n)   { return tag(make(Material.FEATHER, n, LootRarity.UNCOMMON,
            "Phantom Silk",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "Silk so light it seems",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "to phase out of existence.",
            "",
            ChatColor.YELLOW + "» Magic-craft reagent"
    ), "phantom_silk"); }

    public static ItemStack veilEssence() { return tag(make(Material.NETHER_STAR, 1, LootRarity.EPIC,
            "Veil Essence",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "Distilled fragment of the",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "Veil itself.",
            "",
            ChatColor.GOLD + "» Endgame upgrade catalyst"
    ), "veil_essence"); }

    // ── Rare gear ─
    public static ItemStack earthshakerTreads() {
        ItemStack it = make(Material.IRON_BOOTS, 1, LootRarity.RARE,
                "Earthshaker Treads",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "Forged from Colossal slag.",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "The earth itself yields.",
                "",
                ChatColor.AQUA + "» Pre-enchanted: Hardened, Endurance, Anti-KB");
        ItemMeta m = it.getItemMeta();
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3, true);
        m.addEnchant(Enchantment.DURABILITY, 3, true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "hardened", 3);
        it = ItemUtil.addEnchant(it, "endurance", 3);
        it = ItemUtil.addEnchant(it, "antiknockback", 3);
        return tag(it, "earthshaker_treads");
    }

    public static ItemStack shadowstepSandals() {
        ItemStack it = make(Material.GOLD_BOOTS, 1, LootRarity.RARE,
                "Shadowstep Sandals",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "Spun of phantom silk.",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "Footsteps without sound.",
                "",
                ChatColor.AQUA + "» Pre-enchanted: Speed, Featherweight, Jump Boost");
        ItemMeta m = it.getItemMeta();
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.PROTECTION_FALL, 4, true);
        m.addEnchant(Enchantment.DURABILITY, 3, true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "speed", 3);
        it = ItemUtil.addEnchant(it, "featherweight", 3);
        it = ItemUtil.addEnchant(it, "jumpboost", 3);
        return tag(it, "shadowstep_sandals");
    }

    public static ItemStack stoneskinTonic() {
        ItemStack it = new ItemStack(Material.POTION, 1, (short) 8197);
        PotionMeta m = (PotionMeta) it.getItemMeta();
        m.setDisplayName(LootRarity.RARE.getColor() + "" + ChatColor.BOLD + "Stoneskin Tonic");
        m.setLore(Arrays.asList(
                DIVIDER,
                ChatColor.GRAY + "" + ChatColor.ITALIC + "Drinks like cold iron.",
                "",
                ChatColor.AQUA + "» 30s Resistance II",
                "",
                LootRarity.RARE.getColor() + "» Rare",
                DIVIDER
        ));
        m.addCustomEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 600, 1), true);
        it.setItemMeta(m);
        return tag(it, "stoneskin_tonic");
    }

    public static ItemStack phasingElixir() {
        ItemStack it = new ItemStack(Material.POTION, 1, (short) 8194);
        PotionMeta m = (PotionMeta) it.getItemMeta();
        m.setDisplayName(LootRarity.RARE.getColor() + "" + ChatColor.BOLD + "Phasing Elixir");
        m.setLore(Arrays.asList(
                DIVIDER,
                ChatColor.GRAY + "" + ChatColor.ITALIC + "Time loosens its grip.",
                "",
                ChatColor.AQUA + "» 30s Speed II + Jump III",
                "",
                LootRarity.RARE.getColor() + "» Rare",
                DIVIDER
        ));
        m.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1), true);
        m.addCustomEffect(new PotionEffect(PotionEffectType.JUMP, 600, 2), true);
        it.setItemMeta(m);
        return tag(it, "phasing_elixir");
    }

    // ── BOSS-tier (DARK_BLUE) ─
    public static ItemStack ironheartsHammer() {
        ItemStack it = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(LootRarity.BOSS.getColor() + "" + ChatColor.BOLD + "Ironheart's Hammer");
        m.setLore(new ArrayList<>(Arrays.asList(
                DIVIDER,
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "Wrought from the Colossus' own",
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "fist. The earth remembers fear.",
                "",
                ChatColor.GOLD + "» Right-click: " + ChatColor.YELLOW + "Seismic Stomp",
                ChatColor.GRAY + "  AoE knockback + 12 dmg, 6s CD",
                "",
                LootRarity.BOSS.getColor() + "» Boss"
        )));
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
        m.addEnchant(Enchantment.DURABILITY, 4, true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "bonebreaker", 3);
        it = ItemUtil.addEnchant(it, "earthshaker", 3);
        it = ItemUtil.addEnchant(it, "cleave", 3);
        ItemMeta m2 = it.getItemMeta();
        List<String> lore = m2.getLore();
        lore.add(DIVIDER);
        m2.setLore(lore);
        it.setItemMeta(m2);
        return tag(it, "ironhearts_hammer");
    }

    public static ItemStack colossusPlatingCore() {
        ItemStack it = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(LootRarity.BOSS.getColor() + "" + ChatColor.BOLD + "Colossus Plating Core");
        m.setLore(new ArrayList<>(Arrays.asList(
                DIVIDER,
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "The Colossus' own breastplate,",
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "still warm at the seams.",
                "",
                ChatColor.GOLD + "» -15% incoming damage",
                ChatColor.GOLD + "» Permanent Fire Resistance",
                ChatColor.GOLD + "» Resistance II for 3s when HP < 4",
                ChatColor.GRAY + "  (120s cooldown)",
                "",
                LootRarity.BOSS.getColor() + "» Boss"
        )));
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
        m.addEnchant(Enchantment.DURABILITY, 4, true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "vital", 3);
        it = ItemUtil.addEnchant(it, "armored", 3);
        ItemMeta m2 = it.getItemMeta();
        List<String> lore = m2.getLore();
        lore.add(DIVIDER);
        m2.setLore(lore);
        it.setItemMeta(m2);
        return tag(it, "colossus_plating_core");
    }

    public static ItemStack veilseekersMantle() {
        ItemStack it = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(LootRarity.BOSS.getColor() + "" + ChatColor.BOLD + "Veilseeker's Mantle");
        m.setLore(new ArrayList<>(Arrays.asList(
                DIVIDER,
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "Shrouded in stolen reality.",
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "You blink between worlds.",
                "",
                ChatColor.GOLD + "» -10% incoming damage",
                ChatColor.GOLD + "» Permanent Night Vision",
                ChatColor.GOLD + "» When struck below 6 HP: Phantom Step",
                ChatColor.GRAY + "  (blink 6 blocks back + 1s invuln, 30s CD)",
                "",
                LootRarity.BOSS.getColor() + "» Boss"
        )));
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
        m.addEnchant(Enchantment.DURABILITY, 4, true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "phoenix", 1);
        it = ItemUtil.addEnchant(it, "blessed", 3);
        ItemMeta m2 = it.getItemMeta();
        List<String> lore = m2.getLore();
        lore.add(DIVIDER);
        m2.setLore(lore);
        it.setItemMeta(m2);
        return tag(it, "veilseekers_mantle");
    }

    public static ItemStack loomOfEternity() {
        ItemStack it = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(LootRarity.BOSS.getColor() + "" + ChatColor.BOLD + "Loom of Eternity");
        m.setLore(new ArrayList<>(Arrays.asList(
                DIVIDER,
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "Forged by the Veilweaver's last",
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "thread. It cuts the fabric clean.",
                "",
                ChatColor.GOLD + "» Right-click: " + ChatColor.YELLOW + "Reality Tear",
                ChatColor.GRAY + "  Pulls enemies in 8 blocks + 14 magic dmg, 8s CD",
                "",
                LootRarity.BOSS.getColor() + "» Boss"
        )));
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.DAMAGE_ALL, 6, true);
        m.addEnchant(Enchantment.LOOT_BONUS_MOBS, 4, true);
        m.addEnchant(Enchantment.DURABILITY, 4, true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "soulreaper", 3);
        it = ItemUtil.addEnchant(it, "phantomstrike", 3);
        it = ItemUtil.addEnchant(it, "holysmite", 3);
        ItemMeta m2 = it.getItemMeta();
        List<String> lore = m2.getLore();
        lore.add(DIVIDER);
        m2.setLore(lore);
        it.setItemMeta(m2);
        return tag(it, "loom_of_eternity");
    }

    // ── Crafted (mid-tier) ─
    public static ItemStack forgedBulwarkPlate() {
        ItemStack it = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(LootRarity.EPIC.getColor() + "" + ChatColor.BOLD + "Forged Bulwark Plate");
        m.setLore(new ArrayList<>(Arrays.asList(
                DIVIDER,
                ChatColor.GRAY + "" + ChatColor.ITALIC + "Reforged from Colossal scrap.",
                "",
                LootRarity.EPIC.getColor() + "» Epic — Crafted"
        )));
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
        m.addEnchant(Enchantment.DURABILITY, 3, true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "armored", 3);
        it = ItemUtil.addEnchant(it, "vital", 2);
        ItemMeta m2 = it.getItemMeta();
        List<String> lore = m2.getLore();
        lore.add(DIVIDER);
        m2.setLore(lore);
        it.setItemMeta(m2);
        return tag(it, "forged_bulwark_plate");
    }

    public static ItemStack veiledEdge() {
        ItemStack it = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(LootRarity.EPIC.getColor() + "" + ChatColor.BOLD + "Veiled Edge");
        m.setLore(new ArrayList<>(Arrays.asList(
                DIVIDER,
                ChatColor.GRAY + "" + ChatColor.ITALIC + "A blade rewoven from the Veil.",
                "",
                LootRarity.EPIC.getColor() + "» Epic — Crafted"
        )));
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
        m.addEnchant(Enchantment.DURABILITY, 3, true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "soulburn", 2);
        it = ItemUtil.addEnchant(it, "phantomstrike", 2);
        ItemMeta m2 = it.getItemMeta();
        List<String> lore = m2.getLore();
        lore.add(DIVIDER);
        m2.setLore(lore);
        it.setItemMeta(m2);
        return tag(it, "veiled_edge");
    }

    public static ItemStack aetherBow() {
        ItemStack it = new ItemStack(Material.BOW);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(LootRarity.EPIC.getColor() + "" + ChatColor.BOLD + "Aether Bow");
        m.setLore(new ArrayList<>(Arrays.asList(
                DIVIDER,
                ChatColor.GRAY + "" + ChatColor.ITALIC + "Strung with Veil thread,",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "fletched with eternal embers.",
                "",
                LootRarity.EPIC.getColor() + "» Epic — Crafted"
        )));
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.ARROW_DAMAGE, 5, true);
        m.addEnchant(Enchantment.ARROW_FIRE, 1, true);
        m.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        m.addEnchant(Enchantment.DURABILITY, 3, true);
        it.setItemMeta(m);
        return tag(it, "aether_bow");
    }

    public static ItemStack apexCarapace() {
        ItemStack it = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(LootRarity.BOSS.getColor() + "" + ChatColor.BOLD + "Apex Carapace");
        m.setLore(new ArrayList<>(Arrays.asList(
                DIVIDER,
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "Plating Core fused with",
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "Veilseeker's Mantle.",
                "",
                ChatColor.GOLD + "» -20% incoming damage",
                ChatColor.GOLD + "» Permanent Fire Resistance + Night Vision",
                ChatColor.GOLD + "» Both low-HP procs (shared 90s CD)",
                "",
                LootRarity.BOSS.getColor() + "» Boss — Crafted"
        )));
        m.spigot().setUnbreakable(true);
        m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
        m.addEnchant(Enchantment.DURABILITY, 4, true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "vital", 3);
        it = ItemUtil.addEnchant(it, "armored", 4);
        it = ItemUtil.addEnchant(it, "phoenix", 1);
        ItemMeta m2 = it.getItemMeta();
        List<String> lore = m2.getLore();
        lore.add(DIVIDER);
        m2.setLore(lore);
        it.setItemMeta(m2);
        return tag(it, "apex_carapace");
    }

    // ── Permanent-buff consumables ─
    public static ItemStack heartOfTheForge() {
        ItemStack it = make(Material.GOLDEN_APPLE, 1, LootRarity.BOSS,
                "Heart of the Forge",
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "Eat to bind your heart",
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "to the Colossus.",
                "",
                ChatColor.GOLD + "» Right-click eat: " + ChatColor.YELLOW + "+2 max HP (permanent)",
                ChatColor.GRAY + "  Up to +20 cap"
        );
        return tag(it, "heart_of_the_forge");
    }

    public static ItemStack veilSigil() {
        ItemStack it = make(Material.EYE_OF_ENDER, 1, LootRarity.BOSS,
                "Veil Sigil",
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "A sigil that lets you",
                ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "hear the Veil.",
                "",
                ChatColor.GOLD + "» Right-click: " + ChatColor.YELLOW + "+1 soul per kill (permanent)",
                ChatColor.GRAY + "  Up to +10 cap"
        );
        return tag(it, "veil_sigil");
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CAVE RIFT — reagents, crafted gear, boss drops
    // ─────────────────────────────────────────────────────────────────────

    public static ItemStack paleShard(int n) {
        return tag(make(Material.QUARTZ, n, LootRarity.COMMON,
                "Pale Shard",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "A splinter of light from a",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "place where light long since died.",
                "",
                ChatColor.YELLOW + "» Crafting reagent"
        ), "pale_shard");
    }

    public static ItemStack echoShard(int n) {
        return tag(make(Material.PRISMARINE_SHARD, n, LootRarity.COMMON,
                "Echo Shard",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "It hums with the voices",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "of those who never returned.",
                "",
                ChatColor.YELLOW + "» Crafting reagent"
        ), "echo_shard");
    }

    public static ItemStack dripstoneTear(int n) {
        return tag(make(Material.PRISMARINE_CRYSTALS, n, LootRarity.COMMON,
                "Dripstone Tear",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "The mountain weeps. This is",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "what catches in your hand.",
                "",
                ChatColor.YELLOW + "» Crafting reagent"
        ), "dripstone_tear");
    }

    public static ItemStack hollowFragment(int n) {
        return tag(make(Material.BONE, n, LootRarity.UNCOMMON,
                "Hollow Fragment",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + "Bone of the king's court,",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + "still whispering its old name.",
                "",
                ChatColor.YELLOW + "» Crafting reagent"
        ), "hollow_fragment");
    }

    public static ItemStack voidEssence(int n) {
        return tag(make(Material.ENDER_PEARL, n, LootRarity.RARE,
                "Void Essence",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Bottled silence — the kind",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "between heartbeats.",
                "",
                ChatColor.YELLOW + "» Crafting reagent"
        ), "void_essence");
    }

    // ── Boss drop ──
    public static ItemStack crownOfTheHollow() {
        ItemStack it = make(Material.GOLD_HELMET, 1, LootRarity.BOSS,
                "Crown of the Hollow",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Worn by a king forgotten",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "by every mouth that ever",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "spoke his name.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Permanent Night Vision while worn",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "+4 max HP",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Souls drained from kills heal you (10%)"
        );
        ItemMeta m = it.getItemMeta();
        try { m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true); } catch (Throwable ignored) {}
        try { m.addEnchant(Enchantment.DURABILITY, 3, true); } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        return tag(it, "crown_of_the_hollow");
    }

    // ── Crafted gear ──
    public static ItemStack shardheartBlade() {
        ItemStack it = make(Material.DIAMOND_SWORD, 1, LootRarity.EPIC,
                "Shardheart Blade",
                ChatColor.AQUA + "" + ChatColor.ITALIC + "Forged from the pulse of",
                ChatColor.AQUA + "" + ChatColor.ITALIC + "light still trapped within",
                ChatColor.AQUA + "" + ChatColor.ITALIC + "the crystal's heart.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Consecutive hits build damage"
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.DAMAGE_ALL, 6, true);
            m.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            m.addEnchant(Enchantment.LOOT_BONUS_MOBS, 3, true);
            m.addEnchant(Enchantment.DURABILITY, 3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        return tag(it, "shardheart_blade");
    }

    public static ItemStack dripstoneCuirass() {
        ItemStack it = make(Material.DIAMOND_CHESTPLATE, 1, LootRarity.EPIC,
                "Dripstone Cuirass",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "The mountain's bones, hammered",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "flat and bound tight with",
                ChatColor.GRAY + "" + ChatColor.ITALIC + "sinew of old echoes.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Reflects 25% melee damage"
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
            m.addEnchant(Enchantment.THORNS, 2, true);
            m.addEnchant(Enchantment.DURABILITY, 3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        return tag(it, "dripstone_cuirass");
    }

    // ─────────────────────────────────────────────────────────────────
    //  MYTHIC SWORDS — 0.005% drop from the Hollow King.
    //  Sharp 6 + a custom enchant + roguelike permanent buffs the wearer
    //  gets for HOLDING the sword (passive aura via tick task).
    // ─────────────────────────────────────────────────────────────────

    /** "Crimson Tongue" — Bleed-themed Mythic. Stacks bleed FAST and lifesteals.
     *  While held: +1 Strength tier and +1 Speed tier above whatever you already have. */
    public static ItemStack crimsonTongue() {
        ItemStack it = make(Material.DIAMOND_SWORD, 1, LootRarity.MYTHIC,
                "Crimson Tongue",
                ChatColor.DARK_RED + "" + ChatColor.ITALIC + "A blade that drinks before it",
                ChatColor.DARK_RED + "" + ChatColor.ITALIC + "kills, and remembers every taste.",
                "",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "    ✦ MYTHIC AURA ✦",
                ChatColor.GRAY + "  Held: " + ChatColor.WHITE + "+1 Strength tier",
                ChatColor.GRAY + "  Held: " + ChatColor.WHITE + "+1 Speed tier",
                ChatColor.GRAY + "  Bleed proc: " + ChatColor.WHITE + "+1 ❤"
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.DAMAGE_ALL, 6, true);
            m.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            m.addEnchant(Enchantment.LOOT_BONUS_MOBS, 3, true);
            m.addEnchant(Enchantment.DURABILITY, 3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        // Custom-enchant NBT — these proc through CombatListener/AXE checks; we want
        // them to proc on this SWORD specifically (slot restriction is for book apply,
        // not for proc reading).
        it = ItemUtil.addEnchant(it, "bleed", 6);
        it = ItemUtil.addEnchant(it, "deepwounds", 3);
        // Mythic-tier "held" aura tag — picked up by BerserkTickTask
        it = ItemUtil.addEnchant(it, "mythic_held", 1);
        return tag(it, "crimson_tongue");
    }

    /** "Wraithcleaver" — Cleave-themed Mythic. Wide-arc area pressure.
     *  While held: +1 Haste tier and +1 Strength tier above whatever you already have. */
    public static ItemStack wraithcleaver() {
        ItemStack it = make(Material.DIAMOND_SWORD, 1, LootRarity.MYTHIC,
                "Wraithcleaver",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Each swing carries the weight",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "of every door the king ever closed.",
                "",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "    ✦ MYTHIC AURA ✦",
                ChatColor.GRAY + "  Held: " + ChatColor.WHITE + "+1 Haste tier",
                ChatColor.GRAY + "  Held: " + ChatColor.WHITE + "+1 Strength tier",
                ChatColor.GRAY + "  Cleave proc: " + ChatColor.WHITE + "+1 ❤"
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.DAMAGE_ALL, 6, true);
            m.addEnchant(Enchantment.KNOCKBACK, 2, true);
            m.addEnchant(Enchantment.LOOT_BONUS_MOBS, 3, true);
            m.addEnchant(Enchantment.DURABILITY, 3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "cleave", 7);
        it = ItemUtil.addEnchant(it, "earthshaker", 3);
        it = ItemUtil.addEnchant(it, "soulburn", 3);
        // Mythic-tier "held" aura tag — picked up by BerserkTickTask
        it = ItemUtil.addEnchant(it, "mythic_held", 2);
        return tag(it, "wraithcleaver");
    }

    public static ItemStack voidSpunBoots() {
        ItemStack it = make(Material.DIAMOND_BOOTS, 1, LootRarity.EPIC,
                "Void-Spun Boots",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Woven from the silence",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "between heartbeats.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Step lightly — no fall damage <16 blocks"
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.PROTECTION_FALL, 4, true);
            m.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
            m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            m.addEnchant(Enchantment.DURABILITY, 3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        return tag(it, "void_spun_boots");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  v1.2 — MID/LATE boss-tier gear. Below mythic power-level but clearly
    //  above vanilla-diamond. Every piece ships with vanilla Prot IV + a
    //  curated 4-enchant custom stack, leaving room under the 9-slot cap for
    //  players to round out the build with books of their choice.
    // ════════════════════════════════════════════════════════════════════════

    public static ItemStack paleAegis() {
        ItemStack it = make(Material.DIAMOND_HELMET, 1, LootRarity.BOSS,
                "Pale Aegis",
                ChatColor.WHITE + "" + ChatColor.ITALIC + "A crown cut from the sound",
                ChatColor.WHITE + "" + ChatColor.ITALIC + "of a bell that hasn't rung yet.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Sharper eyes, quieter mind."
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            m.addEnchant(Enchantment.DURABILITY, 3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "clarity",    2);
        it = ItemUtil.addEnchant(it, "oathbound",  2);
        it = ItemUtil.addEnchant(it, "wardenseye", 2);
        it = ItemUtil.addEnchant(it, "nightvision", 1);
        return tag(it, "pale_aegis");
    }

    public static ItemStack emberforgeHarness() {
        ItemStack it = make(Material.DIAMOND_CHESTPLATE, 1, LootRarity.BOSS,
                "Emberforge Harness",
                ChatColor.GOLD + "" + ChatColor.ITALIC + "Hammered cold — the heat kept anyway,",
                ChatColor.GOLD + "" + ChatColor.ITALIC + "the way a grudge keeps a name.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Burns the things that touch you."
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            m.addEnchant(Enchantment.DURABILITY, 3, true);
            m.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "vital",     4);
        it = ItemUtil.addEnchant(it, "armored",   3);
        it = ItemUtil.addEnchant(it, "molten",    2);
        it = ItemUtil.addEnchant(it, "laststand", 2);
        return tag(it, "emberforge_harness");
    }

    public static ItemStack runeforgedGreaves() {
        ItemStack it = make(Material.DIAMOND_LEGGINGS, 1, LootRarity.BOSS,
                "Runeforged Greaves",
                ChatColor.DARK_AQUA + "" + ChatColor.ITALIC + "Every rune is a name",
                ChatColor.DARK_AQUA + "" + ChatColor.ITALIC + "the wearer has out-lived.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Hardened by the count."
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
            m.addEnchant(Enchantment.DURABILITY, 3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "hardened",  2);
        it = ItemUtil.addEnchant(it, "endurance", 2);
        it = ItemUtil.addEnchant(it, "entombed",  2);
        it = ItemUtil.addEnchant(it, "ironclad",  2);
        return tag(it, "runeforged_greaves");
    }

    public static ItemStack sunpiercerBlade() {
        ItemStack it = make(Material.DIAMOND_SWORD, 1, LootRarity.BOSS,
                "Sunpiercer Blade",
                ChatColor.YELLOW + "" + ChatColor.ITALIC + "A morning that arrives early",
                ChatColor.YELLOW + "" + ChatColor.ITALIC + "and only visits the undead.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "+25% damage vs undead · ignites on hit"
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.DAMAGE_ALL,      5, true);
            m.addEnchant(Enchantment.FIRE_ASPECT,     2, true);
            m.addEnchant(Enchantment.LOOT_BONUS_MOBS, 3, true);
            m.addEnchant(Enchantment.DURABILITY,      3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "lifesteal",       3);
        it = ItemUtil.addEnchant(it, "holysmite",       3);
        it = ItemUtil.addEnchant(it, "criticalstrike",  3);
        it = ItemUtil.addEnchant(it, "executioner",     2);
        return tag(it, "sunpiercer_blade");
    }

    public static ItemStack wraithsteelAxe() {
        ItemStack it = make(Material.DIAMOND_AXE, 1, LootRarity.BOSS,
                "Wraithsteel Axe",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Cold to the grip — cold after,",
                ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "cold for whoever it finds.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Bleed + Cleave · softens everything it meets."
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.DAMAGE_ALL,      5, true);
            m.addEnchant(Enchantment.LOOT_BONUS_MOBS, 3, true);
            m.addEnchant(Enchantment.DURABILITY,      3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        it = ItemUtil.addEnchant(it, "bleed",         4);
        it = ItemUtil.addEnchant(it, "cleave",        4);
        it = ItemUtil.addEnchant(it, "skullcrush",    2);
        it = ItemUtil.addEnchant(it, "shieldbreaker", 2);
        return tag(it, "wraithsteel_axe");
    }

    public static ItemStack stormwardenBow() {
        ItemStack it = make(Material.BOW, 1, LootRarity.BOSS,
                "Stormwarden Bow",
                ChatColor.BLUE + "" + ChatColor.ITALIC + "Drawn strings hum the weather",
                ChatColor.BLUE + "" + ChatColor.ITALIC + "before the weather arrives.",
                "",
                ChatColor.GOLD + "» " + ChatColor.YELLOW + "Power V · infinity · fires through crowds."
        );
        ItemMeta m = it.getItemMeta();
        try {
            m.addEnchant(Enchantment.ARROW_DAMAGE,   5, true);
            m.addEnchant(Enchantment.ARROW_KNOCKBACK,2, true);
            m.addEnchant(Enchantment.ARROW_FIRE,     1, true);
            m.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
            m.addEnchant(Enchantment.DURABILITY,     3, true);
        } catch (Throwable ignored) {}
        m.spigot().setUnbreakable(true);
        it.setItemMeta(m);
        return tag(it, "stormwarden_bow");
    }
}
