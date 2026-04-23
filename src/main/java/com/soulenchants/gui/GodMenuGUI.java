package com.soulenchants.gui;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.loot.BossLootItems;
import com.soulenchants.masks.Mask;
import com.soulenchants.masks.MaskRegistry;
import com.soulenchants.mythic.MythicFactory;
import com.soulenchants.mythic.MythicRegistry;
import com.soulenchants.mythic.MythicWeapon;
import com.soulenchants.pets.Pet;
import com.soulenchants.pets.PetItem;
import com.soulenchants.pets.PetRegistry;
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
 *   Row 1 — GEAR      : Enchants · Mythics · Masks · Boss Loot
 *   Row 2 — SUPPORT   : Reagents · Consumables · Loot Boxes · Recipes · Pets · Godset · God Set
 *   Row 3 — SPAWN     : Soul Gem · Summon Boss · Custom Mobs list
 *   Row 4 — border + Close
 *
 * v1.2 deltas:
 *   - Added Pets tile (slot 23) — opens a picker of every registered pet;
 *     click an egg to mint yourself a fresh Lv.1 egg (admin utility).
 *   - Tile counts now pull from registries live so they never drift from
 *     the actual content roster.
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
    private static final String TITLE_SOUL_GEM   = MessageStyle.TIER_SOUL + "» Soul Gem Mint";
    private static final String TITLE_PETS       = MessageStyle.TIER_LEGENDARY + "» Pet Registry";
    private static final long[] MINT_PRESETS = { 100L, 500L, 1_000L, 5_000L, 10_000L, 50_000L, 100_000L };

    private final SoulEnchants plugin;

    public GodMenuGUI(SoulEnchants plugin) { this.plugin = plugin; }

    // ──────────────────────────────── Hub ────────────────────────────────
    public void openHub(Player p) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_HUB);
        ItemStack glass = filler();
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, glass);
        }

        // Every tile follows the same lore template:
        //   line 1 : one-sentence description (grey)
        //   line 2 : count / stat summary (optional, mixed-chroma)
        //   blank
        //   line 3 : click hint — "Click ▸ <verb>" in soul-gold+grey
        // Keeps eye-flow consistent across the whole panel.

        // ── Row 1 — GEAR ─────────────────────────────────────────────
        inv.setItem(10, tile(Material.BOOK, MessageStyle.TIER_EPIC, "Enchants",
                "The paginated catalog of every", "custom enchant in the registry.",
                MessageStyle.VALUE + com.soulenchants.enchants.EnchantRegistry.all().size()
                        + MessageStyle.MUTED + " enchants across " + MessageStyle.VALUE + "6 " + MessageStyle.MUTED + "tiers",
                "open catalog"));
        inv.setItem(12, tile(Material.NETHER_STAR, MessageStyle.TIER_SOUL, "Mythic Weapons",
                "Named gear with unique procs —", "aura, chain lightning, ignite…",
                MessageStyle.VALUE + MythicRegistry.all().size() + MessageStyle.MUTED + " weapons · "
                        + MessageStyle.VALUE + "Sharpness V+" + MessageStyle.MUTED + " pre-forged",
                "browse / give"));
        inv.setItem(14, tile(Material.PUMPKIN, MessageStyle.TIER_EPIC, "Cosmetic Masks",
                "Client-side helmet skins —", "real helmet stays equipped.",
                MessageStyle.VALUE + MaskRegistry.all().size() + MessageStyle.MUTED + " masks · "
                        + MessageStyle.VALUE + "ProtocolLib" + MessageStyle.MUTED + " required",
                "equip / clear"));
        inv.setItem(16, tile(Material.DIAMOND_SWORD, MessageStyle.FRAME, "Boss Loot",
                "Named drops from every boss —", "hammers, mantles, cores, plates.",
                MessageStyle.VALUE + "12" + MessageStyle.MUTED + " named items · "
                        + MessageStyle.VALUE + "Legendary+" + MessageStyle.MUTED + " tier",
                "browse"));

        // ── Row 2 — SUPPORT ──────────────────────────────────────────
        inv.setItem(19, tile(Material.IRON_BLOCK, MessageStyle.TIER_UNCOMMON, "Reagents",
                "Crafting materials for every", "custom recipe in the book.",
                MessageStyle.VALUE + "15" + MessageStyle.MUTED + " reagents · "
                        + MessageStyle.VALUE + "stackable",
                "grab a sample"));
        inv.setItem(20, tile(Material.GOLDEN_APPLE, MessageStyle.TIER_EPIC, "Consumables",
                "Magic Dust, Black/White Scrolls,", "Heart of the Forge, Veil Sigil.",
                MessageStyle.VALUE + "4" + MessageStyle.MUTED + " dust tiers · "
                        + MessageStyle.VALUE + "2" + MessageStyle.MUTED + " scrolls · "
                        + MessageStyle.VALUE + "2" + MessageStyle.MUTED + " buffs",
                "browse"));
        inv.setItem(21, tile(Material.CHEST, MessageStyle.SOUL_GOLD, "Loot Boxes",
                "Sealed crates — click-to-open", "animation, rarity-weighted rolls.",
                MessageStyle.VALUE + "4" + MessageStyle.MUTED + " tiers · "
                        + MessageStyle.VALUE + "Bronze→Boss",
                "browse"));
        inv.setItem(22, tile(Material.WORKBENCH, MessageStyle.TIER_RARE, "Recipes",
                "Every custom 3×3 crafting recipe —", "grid preview, ingredient list.",
                MessageStyle.VALUE + com.soulenchants.loot.LootRecipes.ENTRIES.size()
                        + MessageStyle.MUTED + " recipes",
                "open recipe book"));
        inv.setItem(23, tile(Material.BONE, MessageStyle.TIER_LEGENDARY, "Pets",
                "Hybrid companions — egg-in-bag spawns", "a follower + grants passives.",
                MessageStyle.VALUE + PetRegistry.all().size() + MessageStyle.MUTED + " archetypes · "
                        + MessageStyle.VALUE + "Lv.1→50" + MessageStyle.MUTED + " progression",
                "browse / give eggs"));
        inv.setItem(24, tile(Material.DIAMOND_CHESTPLATE, MessageStyle.SOUL_GOLD, "Boss-Killer Set (PvE)",
                "Full PvE loadout — armor + 5 mythics,", "every mythic perfectly enchanted.",
                MessageStyle.VALUE + "4 armor" + MessageStyle.MUTED + " + "
                        + MessageStyle.VALUE + "7 weapons" + MessageStyle.MUTED + " · boss-tuned",
                "equip instantly"));
        inv.setItem(25, tile(Material.DIAMOND_HELMET, MessageStyle.TIER_SOUL, "God Set (PvP)",
                "Full PvP-tuned kit — enchanted", "sword, axe, armor, 2 mythics.",
                MessageStyle.VALUE + "Tuned" + MessageStyle.MUTED + " for player combat",
                "equip instantly"));

        // ── Row 3 — SPAWN ────────────────────────────────────────────
        inv.setItem(27, tile(Material.EMERALD, MessageStyle.TIER_SOUL, "Soul Gem",
                "Portable soul battery — required", "to use soul-tier enchants.",
                MessageStyle.VALUE + "Required" + MessageStyle.MUTED + " for soul procs · drains before ledger",
                "open mint menu"));
        inv.setItem(29, tile(Material.MONSTER_EGG, MessageStyle.SOUL_GOLD, "Summon a Boss",
                "Spawn any registered world boss", "at your current location.",
                MessageStyle.VALUE + "7" + MessageStyle.MUTED + " bosses · "
                        + MessageStyle.VALUE + "Veilweaver · Colossus · Modock · Hollow · +3 elites",
                "open spawn menu"));
        inv.setItem(31, tile(Material.SKULL_ITEM, MessageStyle.TIER_EPIC, "Custom Mobs",
                "Every registered custom mob —", "Hollow King, elites, cave + rift roster.",
                MessageStyle.VALUE + com.soulenchants.mobs.MobRegistry.all().size()
                        + MessageStyle.MUTED + " mobs · "
                        + MessageStyle.VALUE + "/mob list",
                "close and run /mob list"));

        // ── Close ────────────────────────────────────────────────────
        inv.setItem(40, button(Material.BARRIER, MessageStyle.BAD + MessageStyle.BOLD + "Close",
                MessageStyle.MUTED + "Dismiss the Soul Vault."));
        p.openInventory(inv);
    }

    // ──────────────────────────── Sub-menus ─────────────────────────────
    public void openLoot(Player p) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_LOOT);
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
                BossLootItems.phasingElixir(),
                // v1.2 — boss-tier mid/late gear (below mythic power-level)
                BossLootItems.paleAegis(),
                BossLootItems.emberforgeHarness(),
                BossLootItems.runeforgedGreaves(),
                BossLootItems.sunpiercerBlade(),
                BossLootItems.wraithsteelAxe(),
                BossLootItems.stormwardenBow()
        };
        for (int i = 0; i < all.length; i++) inv.setItem(i, all[i]);
        inv.setItem(40, backButton());
        p.openInventory(inv);
    }

    public void openReagents(Player p) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_REAGENTS);
        // Legacy boss reagents (Veilweaver / Colossus)
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
                BossLootItems.veilEssence(),
                // v1.2 — Hollow King + cave rift + new elite boss reagents
                BossLootItems.paleShard(8),
                BossLootItems.echoShard(8),
                BossLootItems.dripstoneTear(8),
                BossLootItems.hollowFragment(8),
                BossLootItems.voidEssence(8)
        };
        for (int i = 0; i < all.length; i++) inv.setItem(i, all[i]);
        inv.setItem(31, backButton());
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
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_SPAWN);
        ItemStack glass = filler();
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, glass);
        }
        // Row 1 — encounter bosses (their own code paths)
        inv.setItem(10, bossTile(Material.MONSTER_EGG, (short) 5,
                MessageStyle.TIER_EPIC + MessageStyle.BOLD + "Veilweaver",
                "3 phases · 15,000 HP",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "summon at your location"));
        inv.setItem(12, bossTile(Material.MONSTER_EGG, (short) 99,
                MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "Ironheart Colossus",
                "2 phases · 8,000 HP",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "summon at your location"));
        inv.setItem(14, bossTile(Material.MONSTER_EGG, (short) 68,
                MessageStyle.TIER_SOUL + MessageStyle.BOLD + "Modock — King of Atlantis",
                "3-phase rift fight",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "/modock summon"));
        inv.setItem(16, bossTile(Material.SKULL_ITEM, (short) 1,
                MessageStyle.TIER_SOUL + MessageStyle.BOLD + "The Hollow King",
                "25,000 HP · meteor + chain lightning",
                "",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "summon at your location"));
        // Row 2 — v1.2 godset-tier elite bosses
        inv.setItem(20, bossTile(Material.SPIDER_EYE, (short) 0,
                MessageStyle.TIER_UNCOMMON + MessageStyle.BOLD + "The Broodmother",
                "18,000 HP · swarm warden",
                MessageStyle.MUTED + "Web-aura · venom cloud · 8-pup death-split",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "summon at your location"));
        inv.setItem(22, bossTile(Material.MAGMA_CREAM, (short) 0,
                MessageStyle.BAD + MessageStyle.BOLD + "The Wurm-Lord",
                "22,000 HP · meteor-storm",
                MessageStyle.MUTED + "Fire aura · 5-block meteors · magma-cube spawns",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "summon at your location"));
        inv.setItem(24, bossTile(Material.BONE, (short) 0,
                MessageStyle.TIER_LEGENDARY + MessageStyle.BOLD + "The Choirmaster",
                "20,000 HP · chain-lightning caster",
                MessageStyle.MUTED + "Soul-steal aura · 4-bounce chain · monk summons",
                MessageStyle.VALUE + "Click ▸ " + MessageStyle.MUTED + "summon at your location"));
        inv.setItem(40, backButton());
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

    public void openSoulGemMint(Player p) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_SOUL_GEM);
        long ledger  = plugin.getSoulManager().get(p);
        long gemTotal = com.soulenchants.items.SoulGemUtil.totalGemBalance(p);
        // Info tile — Soul Bank balance + carried gem balance + license state
        inv.setItem(4, tile(Material.BOOK, MessageStyle.TIER_SOUL, "Soul Balance",
                "Soul Bank: " + MessageStyle.VALUE + com.soulenchants.items.SoulGem.formatNum(ledger),
                "In Gems:   " + MessageStyle.VALUE + com.soulenchants.items.SoulGem.formatNum(gemTotal),
                com.soulenchants.items.SoulGemUtil.hasGem(p)
                        ? MessageStyle.GOOD + "✓ license active"
                        : MessageStyle.BAD + "✗ no gem — soul enchants blocked",
                null));
        // Warning tile — centred between info and presets
        inv.setItem(13, tile(Material.BARRIER, MessageStyle.BAD, "⚠ One-Way Mint",
                "Souls withdrawn from the Soul Bank",
                "cannot be deposited back.",
                MessageStyle.BAD + "Gems are one-way ammunition",
                null));
        // Mint preset tiles — left side of the 7-slot row
        int[] slots = { 19, 20, 21, 22, 23, 24, 25 };
        for (int i = 0; i < slots.length && i < MINT_PRESETS.length; i++) {
            long amt = MINT_PRESETS[i];
            boolean afford = ledger >= amt;
            inv.setItem(slots[i], tile(Material.EMERALD,
                    afford ? MessageStyle.TIER_SOUL : MessageStyle.FRAME,
                    com.soulenchants.items.SoulGem.formatNum(amt) + " Gem",
                    "Mint a Soul Gem carrying",
                    MessageStyle.VALUE + com.soulenchants.items.SoulGem.formatNum(amt) + MessageStyle.MUTED + " souls.",
                    afford ? MessageStyle.GOOD + "Affordable" : MessageStyle.BAD + "Short by "
                            + MessageStyle.VALUE + com.soulenchants.items.SoulGem.formatNum(amt - ledger),
                    afford ? "mint" : null));
        }
        inv.setItem(31, backButton());
        p.openInventory(inv);
    }

    public void openPets(Player p) {
        List<Pet> list = new ArrayList<>(PetRegistry.all());
        int rows = Math.max(3, ((list.size() + 8) / 9) + 1);
        Inventory inv = Bukkit.createInventory(null, rows * 9, TITLE_PETS);
        int slot = 0;
        for (Pet pet : list) {
            // Click gives a fresh Lv.1 egg. Identity is carried by the
            // egg's NBT so each click mints a distinct instance.
            inv.setItem(slot++, PetItem.fresh(pet));
        }
        inv.setItem(rows * 9 - 5, backButton());
        p.openInventory(inv);
    }

    public void openMasks(Player p) {
        // v1.4 — masks are grouped by tier. Six-row inventory with two rows
        // reserved for each tier (header divider + up to 9 masks). Click a
        // mask icon to receive the inventory-item; drag it onto a helmet
        // to attach.
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MASKS);
        tierHeader(inv, 0,  org.bukkit.ChatColor.GRAY, "Low Tier Masks");
        tierHeader(inv, 18, org.bukkit.ChatColor.AQUA, "Mid Tier Masks");
        tierHeader(inv, 36, org.bukkit.ChatColor.GOLD, "High Tier Masks");
        int lowIdx = 0, midIdx = 0, highIdx = 0;
        for (Mask m : MaskRegistry.all()) {
            int slot;
            switch (m.getTier()) {
                case LOW:  slot = 9  + (lowIdx++);  break;
                case MID:  slot = 27 + (midIdx++);  break;
                default:   slot = 45 + (highIdx++); break;
            }
            if (slot < 54) inv.setItem(slot, m.buildInventoryItem());
        }
        inv.setItem(49, backButton());
        p.openInventory(inv);
    }

    /** Render a 9-slot tier banner — colored glass-pane row with a title. */
    private static void tierHeader(Inventory inv, int start, org.bukkit.ChatColor color, String label) {
        short data;
        switch (color) {
            case GRAY: data = 8; break;   // light gray glass
            case AQUA: data = 3; break;   // light blue glass
            case GOLD: data = 4; break;   // yellow glass
            default:   data = 0; break;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
            ItemMeta m = pane.getItemMeta();
            m.setDisplayName(color + "" + org.bukkit.ChatColor.BOLD + "— " + label + " —");
            pane.setItemMeta(m);
            inv.setItem(start + i, pane);
        }
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
                case 22:
                    // RecipeGUI already handles the paginated list + renders each
                    // recipe in an actual InventoryType.WORKBENCH view — use it
                    // directly so the simulated crafting grid is the only path.
                    p.closeInventory();
                    plugin.getRecipeGUI().openList(p);
                    return;
                case 23: openPets(p); return;
                case 24:
                    p.closeInventory();
                    com.soulenchants.items.GodSet.giveBossSet(p);
                    Chat.good(p, MessageStyle.SOUL_GOLD + MessageStyle.BOLD + "✦ Boss-killer set equipped.");
                    return;
                case 25:
                    p.closeInventory();
                    com.soulenchants.items.GodSet.giveGodSet(p);
                    Chat.good(p, MessageStyle.TIER_SOUL + MessageStyle.BOLD + "✦ God set equipped.");
                    return;
                // Row 3 — spawn + soul gem
                case 27: openSoulGemMint(p); return;
                case 29: openSpawn(p); return;
                case 31: p.closeInventory(); p.performCommand("mob list"); return;
                // Close
                case 40: p.closeInventory(); return;
                default: return;
            }
        }

        // Soul Gem mint menu — one-way; deposit path removed
        if (title.equals(TITLE_SOUL_GEM)) {
            int rawSlot = e.getRawSlot();
            for (int i = 0; i < MINT_PRESETS.length; i++) {
                if (rawSlot == 19 + i) {
                    long amt = MINT_PRESETS[i];
                    if (!com.soulenchants.items.SoulGemUtil.withdraw(plugin, p, amt)) {
                        Chat.err(p, "Not enough Soul Bank balance.");
                        return;
                    }
                    Chat.good(p, "Minted a " + MessageStyle.TIER_SOUL
                            + com.soulenchants.items.SoulGem.formatNum(amt) + "-soul gem"
                            + MessageStyle.GOOD + ". "
                            + MessageStyle.BAD + MessageStyle.ITALIC
                            + "(One-way — cannot be deposited back.)");
                    p.playSound(p.getLocation(), org.bukkit.Sound.ORB_PICKUP, 0.9f, 1.3f);
                    openSoulGemMint(p);
                    return;
                }
            }
            return;
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

        // Pet browser: click gives a fresh Lv.1 egg of that pet archetype.
        if (title.equals(TITLE_PETS)) {
            String petId = PetItem.idOf(clicked);
            if (petId == null) return;
            Pet pet = PetRegistry.get(petId);
            if (pet == null) return;
            p.getInventory().addItem(PetItem.fresh(pet)).values()
                    .forEach(over -> p.getWorld().dropItemNaturally(p.getLocation(), over));
            Chat.good(p, "Received " + pet.getRarityColor() + "✦ " + pet.getDisplayName()
                    + MessageStyle.GOOD + " egg (Lv.1). "
                    + MessageStyle.MUTED + "Right-click to summon; sneak+right-click to use the active.");
            return;
        }

        // Mask browser: v1.1 Nordic-style — click gives the mask as an item.
        // Player then drags it onto a helmet to attach. Detach is a
        // right-click on the attached helmet (handled by MaskAttachListener).
        if (title.equals(TITLE_MASKS)) {
            String maskId = MaskRegistry.maskItemId(clicked);
            if (maskId == null) return;
            Mask mask = MaskRegistry.get(maskId);
            if (mask == null) return;
            p.getInventory().addItem(mask.buildInventoryItem()).values()
                    .forEach(over -> p.getWorld().dropItemNaturally(p.getLocation(), over));
            Chat.good(p, "Received " + MessageStyle.TIER_EPIC + MessageStyle.BOLD + "✦ "
                    + mask.getDisplayName() + MessageStyle.GOOD
                    + ". " + MessageStyle.MUTED + "Drag it onto any helmet to attach.");
            return;
        }

        // Spawn menu — one-click boss summons. Encounter bosses run their
        // dedicated managers; CustomMob bosses route through /ce summon so
        // stats/abilities/drops hit the same code path players' spawns do.
        if (title.equals(TITLE_SPAWN)) {
            switch (e.getRawSlot()) {
                case 10:
                    p.closeInventory();
                    plugin.getVeilweaverManager().summon(p.getLocation());
                    return;
                case 12:
                    p.closeInventory();
                    plugin.getIronGolemManager().summon(p.getLocation());
                    return;
                case 14:
                    p.closeInventory();
                    p.performCommand("modock summon");
                    return;
                case 16:
                    p.closeInventory();
                    p.performCommand("ce summon hollow_king");
                    return;
                case 20:
                    p.closeInventory();
                    p.performCommand("ce summon broodmother");
                    return;
                case 22:
                    p.closeInventory();
                    p.performCommand("ce summon wurm_lord");
                    return;
                case 24:
                    p.closeInventory();
                    p.performCommand("ce summon choirmaster");
                    return;
                case 40:
                    openHub(p);
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
            || title.equals(TITLE_MASKS)
            || title.equals(TITLE_SOUL_GEM)
            || title.equals(TITLE_PETS);
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

    /**
     * Tile factory for the hub. Standard layout:
     *   desc1     — grey description, line 1
     *   desc2     — grey description, line 2
     *   stats     — coloured stat summary (value-mute mix)
     *   (blank)
     *   ▸ <verb>  — soul-gold click hint
     *
     * Any null/empty line is omitted so short-blurb tiles don't carry dead rows.
     */
    private ItemStack tile(Material mat, String accent, String title,
                           String desc1, String desc2, String stats, String clickVerb) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(accent + MessageStyle.BOLD + title);
        List<String> l = new ArrayList<>();
        if (desc1 != null && !desc1.isEmpty()) l.add(MessageStyle.MUTED + desc1);
        if (desc2 != null && !desc2.isEmpty()) l.add(MessageStyle.MUTED + desc2);
        if (stats != null && !stats.isEmpty()) l.add(stats);
        if (clickVerb != null && !clickVerb.isEmpty()) {
            l.add("");
            l.add(MessageStyle.SOUL_GOLD + "▸ Click " + MessageStyle.MUTED + "to " + clickVerb);
        }
        m.setLore(l);
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
