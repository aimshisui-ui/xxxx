package com.soulenchants.gui;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.loot.BossLootItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Hub GUI for /ce god — central catalog of every plugin asset.
 * Click a category to drill in, click an item to receive a copy.
 */
public class GodMenuGUI implements Listener {

    private static final String TITLE_HUB        = ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "✦ Soul Vault ✦";
    private static final String TITLE_LOOT       = ChatColor.DARK_BLUE + "» Loot Items";
    private static final String TITLE_REAGENTS   = ChatColor.GREEN     + "» Crafting Reagents";
    private static final String TITLE_BOSS_EGGS  = ChatColor.GOLD      + "» Boss Spawn Eggs";
    private static final String TITLE_GODSET     = ChatColor.GOLD      + "» Godset Items";
    private static final String TITLE_RECIPES    = ChatColor.AQUA      + "» Recipe Book";
    private static final String TITLE_CONSUMABLE = ChatColor.DARK_PURPLE + "» Consumables";
    private static final String TITLE_BOXES      = ChatColor.GOLD      + "» Loot Boxes";

    private final SoulEnchants plugin;

    public GodMenuGUI(SoulEnchants plugin) { this.plugin = plugin; }

    public void openHub(Player p) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_HUB);
        inv.setItem(10, button(Material.BOOK,               ChatColor.LIGHT_PURPLE + "Enchants",        "All custom enchant books"));
        inv.setItem(11, button(Material.DIAMOND_CHESTPLATE, ChatColor.GOLD         + "Godset",          "/ce bossset loadout"));
        inv.setItem(12, button(Material.DIAMOND_SWORD,      ChatColor.DARK_BLUE    + "Boss Loot",       "Every named boss drop"));
        inv.setItem(13, button(Material.IRON_BLOCK,         ChatColor.GREEN        + "Reagents",        "Crafting materials"));
        inv.setItem(14, button(Material.MONSTER_EGG,        ChatColor.GOLD         + "Boss Spawn",      "Summon Veilweaver / Colossus"));
        inv.setItem(15, button(Material.WORKBENCH,          ChatColor.AQUA         + "Recipes",         "All custom recipes"));
        inv.setItem(16, button(Material.GOLDEN_APPLE,       ChatColor.DARK_PURPLE  + "Consumables",     "Dust, scrolls, permanent buffs"));
        inv.setItem(19, button(Material.CHEST,              ChatColor.GOLD         + "Loot Boxes",      "Bronze, Silver, Gold, Boss"));
        inv.setItem(20, button(Material.SKULL_ITEM,         ChatColor.LIGHT_PURPLE + "Custom Mobs",     "60 unique mobs — /mob list"));
        inv.setItem(21, button(Material.GOLD_INGOT,         ChatColor.YELLOW       + "Shop",            "Open the Quartermaster"));
        inv.setItem(22, button(Material.WRITTEN_BOOK,       ChatColor.AQUA         + "Quests",          "Tutorial + Daily quest log"));
        inv.setItem(31, button(Material.BARRIER,            ChatColor.RED          + "Close",           ""));
        p.openInventory(inv);
    }

    public void openLoot(Player p) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_LOOT);
        ItemStack[] all = new ItemStack[]{
                BossLootItems.ironheartsHammer(),
                BossLootItems.colossusPlatingCore(),
                BossLootItems.veilseekersMantle(),
                BossLootItems.loomOfEternity(),
                BossLootItems.apexCarapace(),
                BossLootItems.forgedBulwarkPlate(),
                BossLootItems.veiledEdge(),
                BossLootItems.aetherBow(),
                BossLootItems.earthshakerTreads(),
                BossLootItems.shadowstepSandals(),
                BossLootItems.stoneskinTonic(),
                BossLootItems.phasingElixir()
        };
        for (int i = 0; i < all.length; i++) inv.setItem(i, all[i]);
        inv.setItem(31, backButton());
        p.openInventory(inv);
    }

    public void openReagents(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_REAGENTS);
        ItemStack[] all = new ItemStack[]{
                BossLootItems.colossusSlag(8),
                BossLootItems.ironHeartFragment(8),
                BossLootItems.forgedEmber(8),
                BossLootItems.reinforcedPlating(8),
                BossLootItems.bulwarkCore(),
                BossLootItems.veilThread(8),
                BossLootItems.frayedSoul(8),
                BossLootItems.echoingStrand(8),
                BossLootItems.phantomSilk(8),
                BossLootItems.veilEssence()
        };
        for (int i = 0; i < all.length; i++) inv.setItem(i, all[i]);
        inv.setItem(22, backButton());
        p.openInventory(inv);
    }

    public void openConsumables(Player p) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_CONSUMABLE);
        // Permanent-buff items
        inv.setItem(10, BossLootItems.heartOfTheForge());
        inv.setItem(11, BossLootItems.veilSigil());
        // Magic dust (success rates)
        inv.setItem(13, com.soulenchants.items.ItemFactories.dust(25));
        inv.setItem(14, com.soulenchants.items.ItemFactories.dust(50));
        inv.setItem(15, com.soulenchants.items.ItemFactories.dust(75));
        inv.setItem(16, com.soulenchants.items.ItemFactories.dust(100));
        // Scrolls
        inv.setItem(19, com.soulenchants.items.ItemFactories.whiteScroll());
        inv.setItem(20, com.soulenchants.items.ItemFactories.blackScroll());
        inv.setItem(31, backButton());
        p.openInventory(inv);
    }

    public void openLootBoxes(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_BOXES);
        inv.setItem(10, com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BRONZE));
        inv.setItem(12, com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.SILVER));
        inv.setItem(14, com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.GOLD));
        inv.setItem(16, com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BOSS));
        inv.setItem(22, backButton());
        p.openInventory(inv);
    }

    public void openBossEggs(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_BOSS_EGGS);
        ItemStack veilEgg = bossEgg(Material.MONSTER_EGG, (short) 5,
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Veilweaver Spawn",
                "Right-click to summon",
                "the Veilweaver",
                "(uses /ce summon veilweaver)");
        ItemStack golemEgg = bossEgg(Material.MONSTER_EGG, (short) 99,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Ironheart Colossus Spawn",
                "Right-click to summon",
                "the Ironheart Colossus",
                "(uses /ce summon irongolem)");
        inv.setItem(11, veilEgg);
        inv.setItem(15, golemEgg);
        inv.setItem(22, backButton());
        p.openInventory(inv);
    }

    public void openGodset(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_GODSET);
        ItemStack info = button(Material.BOOK_AND_QUILL, ChatColor.GOLD + "Godset",
                "Equips full boss-killer", "loadout — sword + 4 armor.",
                "", ChatColor.YELLOW + "» Run /ce bossset");
        inv.setItem(13, info);
        inv.setItem(22, backButton());
        p.openInventory(inv);
    }

    public void openRecipeBook(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_RECIPES);
        int slot = 0;
        for (com.soulenchants.loot.LootRecipes.RecipeEntry r : com.soulenchants.loot.LootRecipes.ENTRIES) {
            inv.setItem(slot++, recipeBook(r));
        }
        inv.setItem(22, backButton());
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (title == null) return;
        if (!isOurMenu(title)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Back button
        if (clicked.getType() == Material.ARROW) { openHub(p); return; }

        if (title.equals(TITLE_HUB)) {
            switch (e.getRawSlot()) {
                case 10: plugin.getEnchantMenu().open(p); return;
                case 11: openGodset(p); return;
                case 12: openLoot(p); return;
                case 13: openReagents(p); return;
                case 14: openBossEggs(p); return;
                case 15: p.closeInventory(); plugin.getRecipeGUI().openList(p); return;
                case 16: openConsumables(p); return;
                case 19: openLootBoxes(p); return;
                case 20: p.closeInventory(); p.performCommand("mob list"); return;
                case 21: p.closeInventory(); p.performCommand("shop"); return;
                case 22: p.closeInventory(); p.performCommand("quests"); return;
                case 31: p.closeInventory(); return;
                default: return;
            }
        }

        // Boss eggs → trigger summon
        if (title.equals(TITLE_BOSS_EGGS)) {
            if (e.getRawSlot() == 11) { p.closeInventory(); plugin.getVeilweaverManager().summon(p.getLocation()); return; }
            if (e.getRawSlot() == 15) { p.closeInventory(); plugin.getIronGolemManager().summon(p.getLocation()); return; }
            return;
        }

        // Godset → run /ce bossset
        if (title.equals(TITLE_GODSET) && e.getRawSlot() == 13) {
            p.closeInventory();
            com.soulenchants.items.GodSet.giveTo(p);
            p.sendMessage(ChatColor.GOLD + "✦ Godset equipped.");
            return;
        }

        // Recipe book — clicking does nothing (display only)
        if (title.equals(TITLE_RECIPES)) return;

        // Otherwise: hand the player a copy of the clicked item
        ItemStack give = clicked.clone();
        p.getInventory().addItem(give).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
        p.sendMessage(ChatColor.GREEN + "✦ Added to inventory.");
    }

    private boolean isOurMenu(String title) {
        return title.equals(TITLE_HUB)
            || title.equals(TITLE_LOOT)
            || title.equals(TITLE_REAGENTS)
            || title.equals(TITLE_BOSS_EGGS)
            || title.equals(TITLE_GODSET)
            || title.equals(TITLE_RECIPES)
            || title.equals(TITLE_CONSUMABLE)
            || title.equals(TITLE_BOXES);
    }

    private ItemStack button(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        if (lore.length > 0) {
            java.util.List<String> l = new java.util.ArrayList<>();
            for (String s : lore) l.add(ChatColor.GRAY + s);
            m.setLore(l);
        }
        it.setItemMeta(m);
        return it;
    }

    private ItemStack backButton() {
        return button(Material.ARROW, ChatColor.YELLOW + "« Back", "Return to Soul Vault");
    }

    private ItemStack bossEgg(Material mat, short data, String name, String... lore) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        java.util.List<String> l = new java.util.ArrayList<>();
        for (String s : lore) l.add(ChatColor.GRAY + s);
        m.setLore(l);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack recipeBook(com.soulenchants.loot.LootRecipes.RecipeEntry r) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.GOLD + r.name);
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.GRAY + "» Crafting grid:");
        for (String row : r.shape) lore.add(ChatColor.AQUA + "  " + row.replace(' ', '·'));
        lore.add("");
        lore.add(ChatColor.GRAY + "» Ingredients:");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 9; i++) {
            String id = r.ingredientLootIds.get(i);
            Material mat = r.perSlotMaterials[i];
            if (mat == null) continue;
            String key = (id == null ? "" : id) + "|" + mat;
            if (!seen.add(key)) continue;
            String label = id != null ? prettifyId(id) : mat.name().toLowerCase().replace('_', ' ');
            lore.add(ChatColor.WHITE + "  • " + label);
        }
        lore.add("");
        lore.add(ChatColor.GREEN + "» Yields: " + r.result.getItemMeta().getDisplayName());
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    private static String prettifyId(String id) {
        StringBuilder sb = new StringBuilder();
        for (String word : id.split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
