package com.soulenchants.gui;

import com.soulenchants.loot.BossLootItems;
import com.soulenchants.loot.LootRecipes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Visual recipe browser. The list screen shows the result of every recipe
 * as a clickable item; clicking one opens a single-recipe page laid out
 * to mimic a 3x3 crafting grid:
 *
 *   _______________________________________
 *  | . . . . . . . . . |   ← row 1: spacer / nav
 *  | . [G][G][G] . [R] . . |  ← row 2: top row + result
 *  | . [G][G][G] . . . . |  ← row 3: middle row
 *  | . [G][G][G] . . . . |  ← row 4: bottom row
 *  | . . . . . . . . [<] |  ← row 5: back button
 *
 * G = ingredient slots (positions 10..12, 19..21, 28..30 in a 54-slot inv)
 * R = result slot (position 24)
 */
public class RecipeGUI implements Listener {

    private static final String T_LIST = ChatColor.AQUA + "" + ChatColor.BOLD + "✦ Recipes ✦";
    private static final String T_VIEW = ChatColor.AQUA + "» Recipe: ";

    /** Slots in 54-slot inv that form the visual 3x3 grid. */
    private static final int[] GRID_SLOTS = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30
    };
    private static final int RESULT_SLOT = 24;
    private static final int BACK_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    private final Map<UUID, Integer> currentPage = new HashMap<>();

    public void openList(Player p) {
        openList(p, 0);
    }

    public void openList(Player p, int page) {
        List<LootRecipes.RecipeEntry> all = LootRecipes.ENTRIES;
        int perPage = 28;          // 7x4 area in slots 10-43
        int pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        currentPage.put(p.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
                T_LIST + ChatColor.GRAY + " (" + (page + 1) + "/" + pages + ")");
        // Place recipe results in a grid 7-wide starting at slot 10
        int start = page * perPage;
        int end = Math.min(start + perPage, all.size());
        int slot = 10;
        for (int i = start; i < end; i++) {
            inv.setItem(slot, recipeIcon(all.get(i)));
            slot++;
            if (slot % 9 == 8) slot += 2;     // skip end-of-row + spacer
        }

        if (page > 0) inv.setItem(45, navButton(ChatColor.GREEN + "« Previous", page));
        if (page < pages - 1) inv.setItem(53, navButton(ChatColor.GREEN + "Next »", page + 2));
        inv.setItem(49, button(Material.BARRIER, ChatColor.RED + "Close", ""));
        p.openInventory(inv);
    }

    private ItemStack recipeIcon(LootRecipes.RecipeEntry r) {
        // Strip the result's atmospheric lore — would push tooltips off-screen
        // for items with long flavor text. Detail page shows the real item.
        ItemStack icon = r.result.clone();
        ItemMeta m = icon.getItemMeta();
        m.setLore(java.util.Collections.singletonList(ChatColor.YELLOW + "» View recipe"));
        icon.setItemMeta(m);
        return icon;
    }

    private ItemStack navButton(String name, int targetPageOneIndexed) {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.BOLD + name);
        m.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Page " + targetPageOneIndexed));
        it.setItemMeta(m);
        return it;
    }

    private ItemStack button(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        if (lore.length > 0) {
            List<String> l = new ArrayList<>();
            for (String s : lore) l.add(ChatColor.GRAY + s);
            m.setLore(l);
        }
        it.setItemMeta(m);
        return it;
    }

    public void openRecipe(Player p, LootRecipes.RecipeEntry recipe) {
        Inventory inv = Bukkit.createInventory(null, 54, T_VIEW + ChatColor.RESET
                + (recipe.result.getItemMeta() != null && recipe.result.getItemMeta().hasDisplayName()
                ? recipe.result.getItemMeta().getDisplayName()
                : recipe.name));
        // Background panes for visual separation
        ItemStack glass = pane((short) 7, " ");
        for (int s = 0; s < 54; s++) inv.setItem(s, glass);
        // Crafting bench-style header
        inv.setItem(4, banner(recipe.name));
        // Place ingredients in the 3x3 grid
        for (int i = 0; i < 9; i++) {
            ItemStack ing = ingredientItem(recipe, i);
            inv.setItem(GRID_SLOTS[i], ing);
        }
        // Arrow indicator (between grid and result)
        inv.setItem(23, button(Material.ARROW, ChatColor.GREEN + "" + ChatColor.BOLD + "→",
                "Combine ingredients", "in a crafting table"));
        // Result
        inv.setItem(RESULT_SLOT, recipe.result.clone());
        // Footer
        inv.setItem(BACK_SLOT, button(Material.ARROW, ChatColor.YELLOW + "« Back to list", ""));
        inv.setItem(CLOSE_SLOT, button(Material.BARRIER, ChatColor.RED + "Close", ""));
        p.openInventory(inv);
    }

    private ItemStack pane(short data, String name) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack banner(String text) {
        ItemStack it = new ItemStack(Material.WORKBENCH);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + text);
        m.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Crafting recipe"));
        it.setItemMeta(m);
        return it;
    }

    private ItemStack ingredientItem(LootRecipes.RecipeEntry recipe, int idx) {
        Material mat = recipe.perSlotMaterials[idx];
        if (mat == null || mat == Material.AIR) {
            return pane((short) 15, " ");  // empty slot — black pane
        }
        String lootId = recipe.ingredientLootIds.get(idx);
        ItemStack it;
        if (lootId != null) {
            // Render the actual named loot item so the icon shows custom name + lore
            it = renderLootById(lootId, mat);
        } else {
            it = new ItemStack(mat, 1);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(ChatColor.WHITE + prettify(mat.name()));
            it.setItemMeta(m);
        }
        return it;
    }

    /**
     * Map known loot IDs → their factory item so the recipe view shows the
     * actual custom-named item, not just the base material.
     */
    private static ItemStack renderLootById(String id, Material fallback) {
        switch (id) {
            case "reinforced_plating": return BossLootItems.reinforcedPlating(1);
            case "bulwark_core":       return BossLootItems.bulwarkCore();
            case "echoing_strand":     return BossLootItems.echoingStrand(1);
            case "veil_essence":       return BossLootItems.veilEssence();
            case "iron_heart_fragment":return BossLootItems.ironHeartFragment(1);
            case "veil_thread":        return BossLootItems.veilThread(1);
            case "forged_ember":       return BossLootItems.forgedEmber(1);
            case "colossus_plating_core": return BossLootItems.colossusPlatingCore();
            case "veilseekers_mantle": return BossLootItems.veilseekersMantle();
            case "pale_shard":         return BossLootItems.paleShard(1);
            case "echo_shard":         return BossLootItems.echoShard(1);
            case "dripstone_tear":     return BossLootItems.dripstoneTear(1);
            case "hollow_fragment":    return BossLootItems.hollowFragment(1);
            case "void_essence":       return BossLootItems.voidEssence(1);
            default: return new ItemStack(fallback, 1);
        }
    }

    private static String prettify(String name) {
        StringBuilder sb = new StringBuilder();
        for (String w : name.toLowerCase().split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();
        boolean isList = title.startsWith(T_LIST);
        boolean isView = title.startsWith(T_VIEW);
        if (!isList && !isView) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        if (isList) {
            int slot = e.getRawSlot();
            int page = currentPage.getOrDefault(p.getUniqueId(), 0);
            if (slot == 45) { openList(p, page - 1); return; }
            if (slot == 53) { openList(p, page + 1); return; }
            if (slot == 49) { p.closeInventory(); return; }
            if (clicked.getType() == Material.AIR) return;
            // Map clicked slot to recipe index
            // Slot indexes go 10-16, 19-25, 28-34, 37-43
            // Per-row: 7 entries; 4 rows = 28
            int perRow = 7;
            int row;
            if (slot >= 10 && slot <= 16) row = 0;
            else if (slot >= 19 && slot <= 25) row = 1;
            else if (slot >= 28 && slot <= 34) row = 2;
            else if (slot >= 37 && slot <= 43) row = 3;
            else return;
            int col = (slot % 9) - 1;
            int idx = page * 28 + row * perRow + col;
            if (idx < 0 || idx >= LootRecipes.ENTRIES.size()) return;
            openRecipe(p, LootRecipes.ENTRIES.get(idx));
            return;
        }
        // View screen
        if (e.getRawSlot() == BACK_SLOT) { openList(p, currentPage.getOrDefault(p.getUniqueId(), 0)); return; }
        if (e.getRawSlot() == CLOSE_SLOT) { p.closeInventory(); return; }
    }
}
