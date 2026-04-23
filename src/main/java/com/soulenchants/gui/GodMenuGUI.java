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
 *
 * Layout (45-slot, 5 rows, glass-border framed):
 *
 *   Row 1 — GEAR      : Enchants · Mythics · Masks · Boss Loot · Reagents
 *   Row 2 — SUPPORT   : Consumables · Loot Boxes · Recipes · Godset (action)
 *   Row 3 — SPAWN     : Veilweaver · Colossus · Modock · Custom Mobs list
 *   Row 4 — border + Close
 *
 * v1.1 deltas vs v1.0:
 *   - Mythics + Masks promoted to row 1 (first-class categories)
 *   - Dropped Shop / Quests tiles — they're one-shot commands with no
 *     discovery value inside a GUI (players already hit /shop and /quests)
 *   - Dropped the openGodset() interstitial — tile now runs the action
 *     directly instead of burying it behind a single-button sub-screen
 *   - Unified Boss Eggs into a single Spawn menu that also includes Modock
 */
public class GodMenuGUI implements Listener {

    private static final String TITLE_HUB        = MessageStyle.FRAME + MessageStyle.BOLD + "✦ Soul Vault ✦";
    private static final String TITLE_LOOT       = MessageStyle.FRAME + "» Loot Items";
    private static final String TITLE_REAGENTS   = MessageStyle.TIER_UNCOMMON + "» Crafting Reagents";
    private static final String TITLE_SPAWN      = MessageStyle.SOUL_GOLD + "» Spawn";
    private static final String TITLE_RECIPES    = MessageStyle.TIER_RARE + "» Recipe Book";
    private static final String TITLE_CONSUMABLE = MessageStyle.TIER_EPIC + "» Consumables";
    private static final String TITLE_BOXES      = MessageStyle.SOUL_GOLD + "» Loot Boxes";
    private static final String TITLE_MYTHICS    = MessageStyle.TIER_SOUL + "» Mythic Weapons";
    private static final String TITLE_MASKS      = MessageStyle.TIER_EPIC + "» Cosmetic Masks";

    private final SoulEnchants plugin;

    public GodMenuGUI(SoulEnchants plugin) { this.plugin = plugin; }

    // ──────────────────────────────── Hub ────────────────────────────────
    public void openHub(Player p) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_HUB);
        ItemStack glass = filler();
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, glass);
        }

        // ── Row 1 — GEAR ─────────────────────────────────────────────
        inv.setItem(10, button(Material.BOOK,               MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Enchants",
                "Browse every custom enchant",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "open catalog"));
        inv.setItem(12, button(Material.NETHER_STAR,        MessageStyle.TIER_SOUL + MessageStyle.BOLD + "Mythic Weapons",
                MessageStyle.TIER_SOUL + "v1.1 " + MessageStyle.MUTED + "— "
                        + MessageStyle.VALUE + MythicRegistry.all().size()
                        + MessageStyle.MUTED + " weapons",
                "Sharpness pre-loaded, custom effect",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "browse"));
        inv.setItem(14, button(Material.PUMPKIN,            MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Cosmetic Masks",
                MessageStyle.TIER_EPIC + "v1.1 " + MessageStyle.MUTED + "— "
                        + MessageStyle.VALUE + MaskRegistry.all().size()
                        + MessageStyle.MUTED + " masks",
                "Client-side helmet skin (ProtocolLib)",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "equip / clear"));
        inv.setItem(16, button(Material.DIAMOND_SWORD,      MessageStyle.FRAME + MessageStyle.BOLD + "Boss Loot",
                "Every named boss drop",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "browse"));

        // ── Row 2 — SUPPORT ──────────────────────────────────────────
        inv.setItem(19, button(Material.IRON_BLOCK,         MessageStyle.TIER_UNCOMMON + MessageStyle.BOLD + "Reagents",
                "Crafting materials"));
        inv.setItem(20, button(Material.GOLDEN_APPLE,       MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Consumables",
                "Dust, scrolls, permanent buffs"));
        inv.setItem(21, button(Material.CHEST,              MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Loot Boxes",
                "Bronze, Silver, Gold, Boss"));
        inv.setItem(22, button(Material.WORKBENCH,          MessageStyle.TIER_RARE + MessageStyle.BOLD + "Recipes",
                "Every custom crafting recipe"));
        inv.setItem(24, button(Material.DIAMOND_CHESTPLATE, MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Godset " + MessageStyle.FRAME + "(PvE)",
                "Full boss-killer loadout",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "equip instantly"));
        inv.setItem(25, button(Material.DIAMOND_HELMET,     MessageStyle.TIER_SOUL + MessageStyle.BOLD + "God Set " + MessageStyle.FRAME + "(PvP)",
                "Full PvP-tuned kit",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "equip instantly"));

        // ── Row 3 — SPAWN ────────────────────────────────────────────
        inv.setItem(29, button(Material.MONSTER_EGG,        MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Spawn a Boss",
                "Veilweaver · Colossus · Modock",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "open spawn menu"));
        inv.setItem(31, button(Material.SKULL_ITEM,         MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Custom Mobs",
                "60+ unique mobs",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "runs /mob list"));

        // ── Close ────────────────────────────────────────────────────
        inv.setItem(40, button(Material.BARRIER,            MessageStyle.BAD + MessageStyle.BOLD + "Close", ""));
        p.openInventory(inv);
    }

    // ──────────────────────────── Sub-menus ─────────────────────────────
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

    public void openSpawn(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SPAWN);
        inv.setItem(11, bossTile(Material.MONSTER_EGG, (short) 5,
                MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Veilweaver",
                "3 phases · 15,000 HP",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "summon at your location"));
        inv.setItem(13, bossTile(Material.MONSTER_EGG, (short) 99,
                MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Ironheart Colossus",
                "2 phases · 8,000 HP",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "summon at your location"));
        inv.setItem(15, bossTile(Material.MONSTER_EGG, (short) 68,
                MessageStyle.TIER_SOUL + MessageStyle.BOLD + "Modock — King of Atlantis",
                "3-phase rift fight",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "/modock summon"));
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

    // ──────────────────────────── Click routing ─────────────────────────
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
        if (clicked.getType() == Material.STAINED_GLASS_PANE) return;
        if (clicked.getType() == Material.ARROW) { openHub(p); return; }

        if (title.equals(TITLE_HUB)) {
            switch (e.getRawSlot()) {
                // Row 1 — gear
                case 10: plugin.getEnchantMenu().open(p); return;
                case 12: openMythics(p); return;
                case 14: openMasks(p); return;
                case 16: openLoot(p); return;
                // Row 2 — support
                case 19: openReagents(p); return;
                case 20: openConsumables(p); return;
                case 21: openLootBoxes(p); return;
                case 22: openRecipeBook(p); return;
                case 24:  // Godset PvE — direct action
                    p.closeInventory();
                    com.soulenchants.items.GodSet.giveBossSet(p);
                    Chat.good(p, MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "✦ Boss-killer set equipped.");
                    return;
                case 25:  // God Set PvP — direct action
                    p.closeInventory();
                    com.soulenchants.items.GodSet.giveGodSet(p);
                    Chat.good(p, MessageStyle.TIER_SOUL + MessageStyle.BOLD + "✦ God set equipped.");
                    return;
                // Row 3 — spawn
                case 29: openSpawn(p); return;
                case 31: p.closeInventory(); p.performCommand("mob list"); return;
                // Close
                case 40: p.closeInventory(); return;
                default: return;
            }
        }

        // Mythic browser: give clicked weapon
        if (title.equals(TITLE_MYTHICS)) {
            String id = MythicRegistry.idOf(clicked);
            if (id == null) return;
            p.getInventory().addItem(MythicFactory.create(id));
            Chat.good(p, "Received " + MessageStyle.TIER_SOUL + MessageStyle.BOLD + "❖ "
                    + MythicRegistry.get(id).getDisplayName() + MessageStyle.GOOD + ".");
            return;
        }

        // Mask browser: equip / toggle off
        if (title.equals(TITLE_MASKS)) {
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
            p.getInventory().setHelmet(p.getInventory().getHelmet());
            openMasks(p);
            return;
        }

        // Spawn menu — one-click boss summons
        if (title.equals(TITLE_SPAWN)) {
            switch (e.getRawSlot()) {
                case 11:
                    p.closeInventory();
                    plugin.getVeilweaverManager().summon(p.getLocation());
                    return;
                case 13:
                    p.closeInventory();
                    plugin.getIronGolemManager().summon(p.getLocation());
                    return;
                case 15:
                    p.closeInventory();
                    p.performCommand("modock summon");
                    return;
                default: return;
            }
        }

        // Recipe book — display only
        if (title.equals(TITLE_RECIPES)) return;

        // Otherwise: any sub-menu item click gives the clicked item.
        ItemStack give = clicked.clone();
        p.getInventory().addItem(give).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
        Chat.good(p, "Added to inventory.");
    }

    private boolean isOurMenu(String title) {
        return title.equals(TITLE_HUB)
            || title.equals(TITLE_LOOT)
            || title.equals(TITLE_REAGENTS)
            || title.equals(TITLE_SPAWN)
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

    private ItemStack bossTile(Material mat, short data, String name, String... lore) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        List<String> l = new ArrayList<>();
        for (String s : lore) l.add(s.startsWith("§") || s.isEmpty() ? s : MessageStyle.MUTED + s);
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
