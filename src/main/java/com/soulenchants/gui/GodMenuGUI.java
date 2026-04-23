package com.soulenchants.gui;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.loot.BossLootItems;
import com.soulenchants.masks.Mask;
import com.soulenchants.masks.MaskRegistry;
import com.soulenchants.mythic.MythicFactory;
import com.soulenchants.mythic.MythicRegistry;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Hub GUI for /ce god — central catalog of every plugin asset.
 * Click a category to drill in, click an item to receive a copy.
 * v1.1: Mythics + Masks integrated as first-class categories.
 */
public class GodMenuGUI implements Listener {

    // Titles are the "which menu am I on" sentinel. Keep them unique per screen.
    private static final String TITLE_HUB        = MessageStyle.FRAME + MessageStyle.BOLD + "✦ Soul Vault ✦";
    private static final String TITLE_LOOT       = MessageStyle.FRAME + "» Loot Items";
    private static final String TITLE_REAGENTS   = MessageStyle.TIER_UNCOMMON + "» Crafting Reagents";
    private static final String TITLE_BOSS_EGGS  = MessageStyle.SOUL_GOLD + "» Boss Spawn Eggs";
    private static final String TITLE_GODSET     = MessageStyle.SOUL_GOLD + "» Godset Items";
    private static final String TITLE_RECIPES    = MessageStyle.TIER_RARE + "» Recipe Book";
    private static final String TITLE_CONSUMABLE = MessageStyle.TIER_EPIC + "» Consumables";
    private static final String TITLE_BOXES      = MessageStyle.SOUL_GOLD + "» Loot Boxes";
    // v1.1 additions
    private static final String TITLE_MYTHICS    = MessageStyle.TIER_SOUL + "» Mythic Weapons";
    private static final String TITLE_MASKS      = MessageStyle.TIER_EPIC + "» Cosmetic Masks";

    private final SoulEnchants plugin;

    public GodMenuGUI(SoulEnchants plugin) { this.plugin = plugin; }

    public void openHub(Player p) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_HUB);
        ItemStack glass = filler();
        // Full outline makes the grid read like a panel, not a raw inventory.
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, glass);
        }
        // Row 1 — progression core
        inv.setItem(10, button(Material.BOOK,               MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Enchants",
                "Every custom enchant book",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "open catalog"));
        inv.setItem(11, button(Material.DIAMOND_CHESTPLATE, MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Godset",
                "Boss-killer loadout",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "equip full kit"));
        inv.setItem(12, button(Material.DIAMOND_SWORD,      MessageStyle.FRAME + MessageStyle.BOLD + "Boss Loot",
                "Every named boss drop"));
        inv.setItem(13, button(Material.IRON_BLOCK,         MessageStyle.TIER_UNCOMMON + MessageStyle.BOLD + "Reagents",
                "Crafting materials"));
        inv.setItem(14, button(Material.MONSTER_EGG,        MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Boss Spawn",
                "Summon Veilweaver / Colossus"));
        inv.setItem(15, button(Material.WORKBENCH,          MessageStyle.TIER_RARE + MessageStyle.BOLD + "Recipes",
                "All custom recipes"));
        inv.setItem(16, button(Material.GOLDEN_APPLE,       MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Consumables",
                "Dust, scrolls, permanent buffs"));
        // Row 2 — ancillary
        inv.setItem(19, button(Material.CHEST,              MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Loot Boxes",
                "Bronze, Silver, Gold, Boss"));
        inv.setItem(20, button(Material.SKULL_ITEM,         MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Custom Mobs",
                "60 unique mobs — /mob list"));
        inv.setItem(21, button(Material.GOLD_INGOT,         MessageStyle.TIER_LEGENDARY + MessageStyle.BOLD + "Shop",
                "Open the Quartermaster"));
        inv.setItem(22, button(Material.WRITTEN_BOOK,       MessageStyle.TIER_RARE + MessageStyle.BOLD + "Quests",
                "Tutorial + Daily quest log"));
        // v1.1 — mythics + masks
        inv.setItem(23, button(Material.NETHER_STAR,        MessageStyle.TIER_SOUL + MessageStyle.BOLD + "Mythic Weapons",
                "v1.1 " + MessageStyle.TIER_SOUL + "— " + MessageStyle.MUTED + MythicRegistry.all().size()
                        + " weapons",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "browse / give"));
        inv.setItem(24, button(Material.PUMPKIN,            MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Cosmetic Masks",
                "v1.1 " + MessageStyle.TIER_EPIC + "— " + MessageStyle.MUTED + MaskRegistry.all().size()
                        + " masks",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "browse / equip"));
        // Close
        inv.setItem(40, button(Material.BARRIER,            MessageStyle.BAD + MessageStyle.BOLD + "Close", ""));
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
        inv.setItem(10, BossLootItems.heartOfTheForge());
        inv.setItem(11, BossLootItems.veilSigil());
        inv.setItem(13, ItemFactories.dust(25));
        inv.setItem(14, ItemFactories.dust(50));
        inv.setItem(15, ItemFactories.dust(75));
        inv.setItem(16, ItemFactories.dust(100));
        inv.setItem(19, ItemFactories.whiteScroll());
        inv.setItem(20, ItemFactories.blackScroll());
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
                MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Veilweaver Spawn",
                "Right-click to summon",
                "the Veilweaver",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "/ce summon veilweaver");
        ItemStack golemEgg = bossEgg(Material.MONSTER_EGG, (short) 99,
                MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Ironheart Colossus Spawn",
                "Right-click to summon",
                "the Ironheart Colossus",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "/ce summon irongolem");
        inv.setItem(11, veilEgg);
        inv.setItem(15, golemEgg);
        inv.setItem(22, backButton());
        p.openInventory(inv);
    }

    public void openGodset(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_GODSET);
        ItemStack info = button(Material.BOOK_AND_QUILL, MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Godset",
                "Equips full boss-killer",
                "loadout — sword + 4 armor.",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "/ce bossset");
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

    // ──────────────── v1.1 — Mythic weapons browser ────────────────
    public void openMythics(Player p) {
        List<MythicWeapon> list = new ArrayList<>(MythicRegistry.all());
        int rows = Math.max(3, ((list.size() + 8) / 9) + 1);
        Inventory inv = Bukkit.createInventory(null, rows * 9, TITLE_MYTHICS);
        int slot = 0;
        for (MythicWeapon m : list) {
            inv.setItem(slot++, MythicFactory.create(m.getId()));
        }
        inv.setItem(rows * 9 - 5, backButton());
        p.openInventory(inv);
    }

    // ──────────────── v1.1 — Cosmetic masks browser ────────────────
    public void openMasks(Player p) {
        List<Mask> list = new ArrayList<>(MaskRegistry.all());
        int rows = Math.max(3, ((list.size() + 8) / 9) + 1);
        Inventory inv = Bukkit.createInventory(null, rows * 9, TITLE_MASKS);
        int slot = 0;
        for (Mask m : list) {
            inv.setItem(slot++, maskTile(m, plugin.getMaskManager().getEquipped(p)));
        }
        inv.setItem(rows * 9 - 5, backButton());
        p.openInventory(inv);
    }

    private ItemStack maskTile(Mask m, String currentlyEquippedId) {
        ItemStack it = m.buildVisual();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            boolean equipped = m.getId().equals(currentlyEquippedId);
            meta.setDisplayName(MessageStyle.TIER_EPIC + MessageStyle.BOLD + m.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(MessageStyle.MUTED + "Id: " + MessageStyle.VALUE + m.getId());
            lore.add("");
            if (equipped) {
                lore.add(MessageStyle.GOOD + MessageStyle.BOLD + "✓ EQUIPPED");
                lore.add(MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "clear");
            } else {
                lore.add(MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "equip");
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
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

        // Back button (arrows + back name)
        if (clicked.getType() == Material.ARROW) { openHub(p); return; }
        // Filler (ignore)
        if (clicked.getType() == Material.STAINED_GLASS_PANE) return;

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
                case 23: openMythics(p); return;      // v1.1
                case 24: openMasks(p); return;        // v1.1
                case 40: p.closeInventory(); return;
                default: return;
            }
        }

        // v1.1 — Mythic browser: give clicked weapon
        if (title.equals(TITLE_MYTHICS)) {
            String id = MythicRegistry.idOf(clicked);
            if (id == null) return;
            p.getInventory().addItem(MythicFactory.create(id));
            Chat.good(p, "Received " + MessageStyle.TIER_SOUL + MessageStyle.BOLD + "❖ "
                    + MythicRegistry.get(id).getDisplayName() + MessageStyle.GOOD + ".");
            return;
        }

        // v1.1 — Mask browser: equip / toggle off
        if (title.equals(TITLE_MASKS)) {
            // Find which mask by its visual material + data (since masks don't carry an NBT id tag).
            Mask target = null;
            for (Mask m : MaskRegistry.all()) {
                ItemStack v = m.buildVisual();
                if (v.getType() == clicked.getType() && v.getDurability() == clicked.getDurability()) {
                    target = m; break;
                }
            }
            if (target == null) return;
            String currentId = plugin.getMaskManager().getEquipped(p);
            if (target.getId().equals(currentId)) {
                plugin.getMaskManager().clear(p);
                Chat.info(p, "Cleared " + MessageStyle.TIER_EPIC + target.getDisplayName() + MessageStyle.MUTED + ".");
            } else {
                plugin.getMaskManager().equip(p, target.getId());
                Chat.good(p, "Equipped " + MessageStyle.TIER_EPIC + target.getDisplayName() + MessageStyle.GOOD + ".");
            }
            // Force nearby clients to re-render the helmet so the packet injector swaps it.
            p.getInventory().setHelmet(p.getInventory().getHelmet());
            openMasks(p); // refresh tile state
            return;
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
            Chat.good(p, MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "✦ Godset equipped.");
            return;
        }

        // Recipe book — clicking does nothing (display only)
        if (title.equals(TITLE_RECIPES)) return;

        // Otherwise: hand the player a copy of the clicked item
        ItemStack give = clicked.clone();
        p.getInventory().addItem(give).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
        Chat.good(p, "Added to inventory.");
    }

    private boolean isOurMenu(String title) {
        return title.equals(TITLE_HUB)
            || title.equals(TITLE_LOOT)
            || title.equals(TITLE_REAGENTS)
            || title.equals(TITLE_BOSS_EGGS)
            || title.equals(TITLE_GODSET)
            || title.equals(TITLE_RECIPES)
            || title.equals(TITLE_CONSUMABLE)
            || title.equals(TITLE_BOXES)
            || title.equals(TITLE_MYTHICS)
            || title.equals(TITLE_MASKS);
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta m = pane.getItemMeta();
        if (m != null) { m.setDisplayName(" "); pane.setItemMeta(m); }
        return pane;
    }

    private ItemStack button(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        if (lore.length > 0) {
            List<String> l = new ArrayList<>();
            for (String s : lore) l.add(s.startsWith("§") ? s : MessageStyle.MUTED + s);
            m.setLore(l);
        }
        it.setItemMeta(m);
        return it;
    }

    private ItemStack backButton() {
        return button(Material.ARROW, MessageStyle.TIER_LEGENDARY + MessageStyle.BOLD + "« Back",
                "Return to Soul Vault");
    }

    private ItemStack bossEgg(Material mat, short data, String name, String... lore) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        List<String> l = new ArrayList<>();
        for (String s : lore) l.add(s.startsWith("§") ? s : MessageStyle.MUTED + s);
        m.setLore(l);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack recipeBook(com.soulenchants.loot.LootRecipes.RecipeEntry r) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(MessageStyle.SOUL_GOLD + r.name);
        List<String> lore = new ArrayList<>();
        lore.add(MessageStyle.MUTED + "» Crafting grid:");
        for (String row : r.shape) lore.add(MessageStyle.TIER_RARE + "  " + row.replace(' ', '·'));
        lore.add("");
        lore.add(MessageStyle.MUTED + "» Ingredients:");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 9; i++) {
            String id = r.ingredientLootIds.get(i);
            Material mat = r.perSlotMaterials[i];
            if (mat == null) continue;
            String key = (id == null ? "" : id) + "|" + mat;
            if (!seen.add(key)) continue;
            String label = id != null ? prettifyId(id) : mat.name().toLowerCase().replace('_', ' ');
            lore.add(MessageStyle.VALUE + "  • " + label);
        }
        lore.add("");
        lore.add(MessageStyle.GOOD + "» Yields: " + r.result.getItemMeta().getDisplayName());
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
