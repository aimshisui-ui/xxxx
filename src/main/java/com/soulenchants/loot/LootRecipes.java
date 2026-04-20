package com.soulenchants.loot;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Registers all crafting recipes. Bukkit recipes match by Material — we then
 * intercept CraftItemEvent / PrepareItemCraftEvent to ensure the actual
 * ingredients carry the right NBT loot IDs (otherwise the recipe yields nothing).
 *
 * Stored display copies are exposed via getRecipeBook() for the /ce recipe GUI.
 */
public final class LootRecipes {

    public static final List<RecipeEntry> ENTRIES = new ArrayList<>();

    public static class RecipeEntry {
        public final String name;
        public final ItemStack result;
        public final String[] shape;       // length 3 of length-3 strings, or null for shapeless
        public final List<String> ingredientLootIds;   // for shapeless / per-slot id list
        public final Material[] perSlotMaterials;      // for shaped: 9 entries (or null for empty)

        public RecipeEntry(String name, ItemStack result, String[] shape,
                           Material[] perSlotMaterials, List<String> ingredientLootIds) {
            this.name = name;
            this.result = result;
            this.shape = shape;
            this.perSlotMaterials = perSlotMaterials;
            this.ingredientLootIds = ingredientLootIds;
        }
    }

    private LootRecipes() {}

    public static void register(SoulEnchants plugin) {
        ENTRIES.clear();

        // Forged Bulwark Plate: 4 Reinforced Plating + 1 Bulwark Core + Iron Chestplate
        ShapedRecipe r1 = new ShapedRecipe(BossLootItems.forgedBulwarkPlate());
        r1.shape("RPR", "PCP", "RPR");
        r1.setIngredient('R', Material.IRON_BLOCK);   // Reinforced Plating
        r1.setIngredient('P', Material.IRON_CHESTPLATE);
        r1.setIngredient('C', Material.NETHER_STAR);  // Bulwark Core
        Bukkit.addRecipe(r1);
        ENTRIES.add(new RecipeEntry("Forged Bulwark Plate", BossLootItems.forgedBulwarkPlate(),
                new String[]{"RPR", "PCP", "RPR"},
                new Material[]{Material.IRON_BLOCK, Material.IRON_CHESTPLATE, Material.IRON_BLOCK,
                        Material.IRON_CHESTPLATE, Material.NETHER_STAR, Material.IRON_CHESTPLATE,
                        Material.IRON_BLOCK, Material.IRON_CHESTPLATE, Material.IRON_BLOCK},
                Arrays.asList("reinforced_plating", null, "reinforced_plating",
                        null, "bulwark_core", null,
                        "reinforced_plating", null, "reinforced_plating")));

        // Veiled Edge: 4 Echoing Strand + 1 Veil Essence + Iron Sword
        ShapedRecipe r2 = new ShapedRecipe(BossLootItems.veiledEdge());
        r2.shape(" E ", "ESE", " E ");
        r2.setIngredient('E', Material.QUARTZ);       // Echoing Strand
        r2.setIngredient('S', Material.IRON_SWORD);   // base
        Bukkit.addRecipe(r2);
        ENTRIES.add(new RecipeEntry("Veiled Edge", BossLootItems.veiledEdge(),
                new String[]{" E ", "ESE", " E "},
                new Material[]{null, Material.QUARTZ, null,
                        Material.QUARTZ, Material.IRON_SWORD, Material.QUARTZ,
                        null, Material.QUARTZ, null},
                Arrays.asList(null, "echoing_strand", null,
                        "echoing_strand", null, "echoing_strand",
                        null, "echoing_strand", null)));

        // Aether Bow: 4 Forged Ember + 4 Veil Thread + Bow
        ShapedRecipe r3 = new ShapedRecipe(BossLootItems.aetherBow());
        r3.shape("FTF", "TBT", "FTF");
        r3.setIngredient('F', Material.BLAZE_POWDER); // Forged Ember
        r3.setIngredient('T', Material.STRING);       // Veil Thread
        r3.setIngredient('B', Material.BOW);
        Bukkit.addRecipe(r3);
        ENTRIES.add(new RecipeEntry("Aether Bow", BossLootItems.aetherBow(),
                new String[]{"FTF", "TBT", "FTF"},
                new Material[]{Material.BLAZE_POWDER, Material.STRING, Material.BLAZE_POWDER,
                        Material.STRING, Material.BOW, Material.STRING,
                        Material.BLAZE_POWDER, Material.STRING, Material.BLAZE_POWDER},
                Arrays.asList("forged_ember", "veil_thread", "forged_ember",
                        "veil_thread", null, "veil_thread",
                        "forged_ember", "veil_thread", "forged_ember")));

        // Apex Carapace: Plating Core + Veilseeker's Mantle + 8 Diamond Block
        ShapedRecipe r4 = new ShapedRecipe(BossLootItems.apexCarapace());
        r4.shape("DDD", "DPD", "DMD");
        r4.setIngredient('D', Material.DIAMOND_BLOCK);
        r4.setIngredient('P', Material.DIAMOND_CHESTPLATE);
        r4.setIngredient('M', Material.DIAMOND_CHESTPLATE);
        Bukkit.addRecipe(r4);
        ENTRIES.add(new RecipeEntry("Apex Carapace", BossLootItems.apexCarapace(),
                new String[]{"DDD", "DPD", "DMD"},
                new Material[]{Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK,
                        Material.DIAMOND_BLOCK, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_BLOCK,
                        Material.DIAMOND_BLOCK, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_BLOCK},
                Arrays.asList(null, null, null,
                        null, "colossus_plating_core", null,
                        null, "veilseekers_mantle", null)));

        // Heart of the Forge: 8 Iron Heart Fragment + Nether Star (Bulwark Core base)
        ShapedRecipe r5 = new ShapedRecipe(BossLootItems.heartOfTheForge());
        r5.shape("FFF", "FCF", "FFF");
        r5.setIngredient('F', Material.NETHER_BRICK_ITEM); // Iron Heart Fragment
        r5.setIngredient('C', Material.NETHER_STAR);
        Bukkit.addRecipe(r5);
        ENTRIES.add(new RecipeEntry("Heart of the Forge", BossLootItems.heartOfTheForge(),
                new String[]{"FFF", "FCF", "FFF"},
                new Material[]{Material.NETHER_BRICK_ITEM, Material.NETHER_BRICK_ITEM, Material.NETHER_BRICK_ITEM,
                        Material.NETHER_BRICK_ITEM, Material.NETHER_STAR, Material.NETHER_BRICK_ITEM,
                        Material.NETHER_BRICK_ITEM, Material.NETHER_BRICK_ITEM, Material.NETHER_BRICK_ITEM},
                Arrays.asList("iron_heart_fragment", "iron_heart_fragment", "iron_heart_fragment",
                        "iron_heart_fragment", "bulwark_core", "iron_heart_fragment",
                        "iron_heart_fragment", "iron_heart_fragment", "iron_heart_fragment")));

        // Veil Sigil: 8 Veil Thread + Eye of Ender (Veil Essence base)
        ShapedRecipe r6 = new ShapedRecipe(BossLootItems.veilSigil());
        r6.shape("TTT", "TET", "TTT");
        r6.setIngredient('T', Material.STRING);
        r6.setIngredient('E', Material.NETHER_STAR);  // Veil Essence
        Bukkit.addRecipe(r6);
        ENTRIES.add(new RecipeEntry("Veil Sigil", BossLootItems.veilSigil(),
                new String[]{"TTT", "TET", "TTT"},
                new Material[]{Material.STRING, Material.STRING, Material.STRING,
                        Material.STRING, Material.NETHER_STAR, Material.STRING,
                        Material.STRING, Material.STRING, Material.STRING},
                Arrays.asList("veil_thread", "veil_thread", "veil_thread",
                        "veil_thread", "veil_essence", "veil_thread",
                        "veil_thread", "veil_thread", "veil_thread")));

        // ── CAVE RIFT recipes ─────────────────────────────────────────

        // Shardheart Blade: 4 Pale Shard + Echo Shard + Diamond Sword + 3 Glowstone
        ShapedRecipe r7 = new ShapedRecipe(BossLootItems.shardheartBlade());
        r7.shape("PEP", "PSP", "GGG");
        r7.setIngredient('P', Material.QUARTZ);              // Pale Shard
        r7.setIngredient('E', Material.PRISMARINE_SHARD);    // Echo Shard
        r7.setIngredient('S', Material.DIAMOND_SWORD);
        r7.setIngredient('G', Material.GLOWSTONE_DUST);
        Bukkit.addRecipe(r7);
        ENTRIES.add(new RecipeEntry("Shardheart Blade", BossLootItems.shardheartBlade(),
                new String[]{"PEP", "PSP", "GGG"},
                new Material[]{Material.QUARTZ, Material.PRISMARINE_SHARD, Material.QUARTZ,
                        Material.QUARTZ, Material.DIAMOND_SWORD, Material.QUARTZ,
                        Material.GLOWSTONE_DUST, Material.GLOWSTONE_DUST, Material.GLOWSTONE_DUST},
                Arrays.asList("pale_shard", "echo_shard", "pale_shard",
                        "pale_shard", null, "pale_shard",
                        null, null, null)));

        // Dripstone Cuirass: 4 Dripstone Tear + Hollow Fragment + Diamond Chest + 2 Pale Shard
        ShapedRecipe r8 = new ShapedRecipe(BossLootItems.dripstoneCuirass());
        r8.shape("DHD", "DCD", "PDP");
        r8.setIngredient('D', Material.PRISMARINE_CRYSTALS); // Dripstone Tear
        r8.setIngredient('H', Material.BONE);                // Hollow Fragment
        r8.setIngredient('C', Material.DIAMOND_CHESTPLATE);
        r8.setIngredient('P', Material.QUARTZ);              // Pale Shard
        Bukkit.addRecipe(r8);
        ENTRIES.add(new RecipeEntry("Dripstone Cuirass", BossLootItems.dripstoneCuirass(),
                new String[]{"DHD", "DCD", "PDP"},
                new Material[]{Material.PRISMARINE_CRYSTALS, Material.BONE, Material.PRISMARINE_CRYSTALS,
                        Material.PRISMARINE_CRYSTALS, Material.DIAMOND_CHESTPLATE, Material.PRISMARINE_CRYSTALS,
                        Material.QUARTZ, Material.PRISMARINE_CRYSTALS, Material.QUARTZ},
                Arrays.asList("dripstone_tear", "hollow_fragment", "dripstone_tear",
                        "dripstone_tear", null, "dripstone_tear",
                        "pale_shard", "dripstone_tear", "pale_shard")));

        // Void-Spun Boots: 4 Void Essence + 2 Dripstone Tear + Diamond Boots + 2 Echo Shard
        ShapedRecipe r9 = new ShapedRecipe(BossLootItems.voidSpunBoots());
        r9.shape("VEV", "VBV", "DED");
        r9.setIngredient('V', Material.ENDER_PEARL);         // Void Essence
        r9.setIngredient('E', Material.PRISMARINE_SHARD);    // Echo Shard
        r9.setIngredient('B', Material.DIAMOND_BOOTS);
        r9.setIngredient('D', Material.PRISMARINE_CRYSTALS); // Dripstone Tear
        Bukkit.addRecipe(r9);
        ENTRIES.add(new RecipeEntry("Void-Spun Boots", BossLootItems.voidSpunBoots(),
                new String[]{"VEV", "VBV", "DED"},
                new Material[]{Material.ENDER_PEARL, Material.PRISMARINE_SHARD, Material.ENDER_PEARL,
                        Material.ENDER_PEARL, Material.DIAMOND_BOOTS, Material.ENDER_PEARL,
                        Material.PRISMARINE_CRYSTALS, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS},
                Arrays.asList("void_essence", "echo_shard", "void_essence",
                        "void_essence", null, "void_essence",
                        "dripstone_tear", "echo_shard", "dripstone_tear")));

        Bukkit.getLogger().info("[SoulEnchants] " + ENTRIES.size() + " custom recipes registered.");
    }

    /**
     * Validates the matrix's NBT loot IDs match what the recipe expects.
     * Returns true if the recipe is satisfied.
     */
    public static RecipeEntry findMatching(ItemStack[] matrix, ItemStack result) {
        for (RecipeEntry entry : ENTRIES) {
            if (!entry.result.getType().equals(result.getType())) continue;
            // Compare display names too — multiple recipes may share a base material
            if (!nameMatches(entry.result, result)) continue;
            if (matrixMatches(matrix, entry)) return entry;
        }
        return null;
    }

    private static boolean nameMatches(ItemStack a, ItemStack b) {
        if (a.getItemMeta() == null || b.getItemMeta() == null) return true;
        String an = a.getItemMeta().getDisplayName();
        String bn = b.getItemMeta().getDisplayName();
        return an == null ? bn == null : an.equals(bn);
    }

    private static boolean matrixMatches(ItemStack[] matrix, RecipeEntry e) {
        if (matrix.length != 9) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack slot = matrix[i];
            String expectedId = e.ingredientLootIds.get(i);
            Material expectedMat = e.perSlotMaterials[i];
            if (expectedMat == null) {
                if (slot != null && slot.getType() != Material.AIR) return false;
                continue;
            }
            if (slot == null || slot.getType() != expectedMat) return false;
            String actualId = BossLootItems.getLootId(slot);
            if (expectedId != null && !expectedId.equals(actualId)) return false;
            if (expectedId == null && actualId != null) return false;
        }
        return true;
    }

    /**
     * Listener that gates crafting outputs based on actual NBT IDs in the matrix.
     * Bukkit ShapedRecipe matches by Material only, so we have to validate here
     * and clear the result if the IDs don't line up.
     */
    public static class GateListener implements Listener {

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onPrepare(org.bukkit.event.inventory.PrepareItemCraftEvent e) {
            ItemStack result = e.getInventory().getResult();
            if (result == null) return;
            // Only gate items we made
            if (BossLootItems.getLootId(result) == null) return;
            ItemStack[] matrix = e.getInventory().getMatrix();
            RecipeEntry match = findMatching(matrix, result);
            if (match == null) e.getInventory().setResult(null);
        }
    }
}
