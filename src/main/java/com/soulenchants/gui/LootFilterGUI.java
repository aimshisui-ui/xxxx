package com.soulenchants.gui;

import com.soulenchants.SoulEnchants;
import com.soulenchants.loot.CustomLootRegistry;
import com.soulenchants.loot.LootFilterManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
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
 * Per-player loot filter UI:
 *   /lootfilter → main: 5 category tiles (BOSS, CRAFTED, RARE_GEAR, REAGENTS, LOOTBOXES)
 *   click category → sub: every loot id in the category, click to toggle filter.
 *
 * Filtered items render with a STAINED_GLASS_PANE overlay (preserves icon name)
 * + lore line "» FILTERED — click to allow again".
 */
public class LootFilterGUI implements Listener {

    private static final String T_MAIN = ChatColor.RED + "" + ChatColor.BOLD + "✦ Loot Filter ✦";
    private static final String T_SUB_PREFIX = ChatColor.RED + "» Filter: ";

    private final SoulEnchants plugin;
    private final LootFilterManager mgr;
    /** Track which sub-page each player is currently on, so toggling keeps them on the right page. */
    private final Map<UUID, CustomLootRegistry.Category> currentSub = new HashMap<>();

    public LootFilterGUI(SoulEnchants plugin, LootFilterManager mgr) {
        this.plugin = plugin;
        this.mgr = mgr;
    }

    public void openMain(Player p) {
        currentSub.remove(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, T_MAIN);
        CustomLootRegistry.Category[] cats = CustomLootRegistry.Category.values();
        // Center the 5 category tiles in row 2: slots 11..15
        int slot = 11;
        for (CustomLootRegistry.Category cat : cats) {
            int n = mgr.countFilteredInCategory(p.getUniqueId(), cat);
            int total = CustomLootRegistry.byCategory(cat).size();
            inv.setItem(slot++, categoryIcon(cat, n, total));
        }
        inv.setItem(22, button(Material.BOOK, ChatColor.YELLOW + "Drop Messages: "
                        + (mgr.messagesEnabled(p.getUniqueId()) ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"),
                ChatColor.GRAY + "Click to toggle whether you get a chat",
                ChatColor.GRAY + "message when a drop is filtered out."));
        inv.setItem(26, button(Material.BARRIER, ChatColor.RED + "Close", ""));
        p.openInventory(inv);
    }

    public void openSub(Player p, CustomLootRegistry.Category cat) {
        currentSub.put(p.getUniqueId(), cat);
        List<CustomLootRegistry.Entry> entries = CustomLootRegistry.byCategory(cat);
        Inventory inv = Bukkit.createInventory(null, 54, T_SUB_PREFIX + fancy(cat));
        int slot = 0;
        for (CustomLootRegistry.Entry e : entries) {
            if (slot >= 45) break;          // leave bottom row for nav
            ItemStack icon = e.create();
            boolean filtered = mgr.isFiltered(p.getUniqueId(), e.id);
            inv.setItem(slot++, decorate(icon, e.id, filtered));
        }
        inv.setItem(45, button(Material.ARROW, ChatColor.YELLOW + "« Back",
                ChatColor.GRAY + "Return to category list."));
        inv.setItem(53, button(Material.BARRIER, ChatColor.RED + "Close", ""));
        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null) return;
        String title = e.getView().getTitle();
        if (title == null) return;
        if (!title.equals(T_MAIN) && !title.startsWith(T_SUB_PREFIX)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BARRIER) { p.closeInventory(); return; }

        if (title.equals(T_MAIN)) {
            // Toggle messages
            if (clicked.getType() == Material.BOOK) {
                mgr.toggleMessages(p.getUniqueId());
                openMain(p);
                return;
            }
            // Category tile — read category from display name
            CustomLootRegistry.Category cat = categoryFromDisplay(clicked);
            if (cat != null) openSub(p, cat);
            return;
        }

        // Sub view
        if (clicked.getType() == Material.ARROW) { openMain(p); return; }
        // Toggle filter — read tagged id from item meta
        String id = readTaggedId(clicked);
        if (id == null) return;
        boolean nowFiltered = mgr.toggle(p.getUniqueId(), id);
        p.sendMessage((nowFiltered ? ChatColor.RED + "§l(!)§c Filtered out " : ChatColor.GREEN + "§l(!)§a Allowing ")
                + niceName(clicked) + (nowFiltered ? ChatColor.RED + " from drops." : ChatColor.GREEN + " in drops."));
        // Refresh same sub page
        CustomLootRegistry.Category cat = currentSub.get(p.getUniqueId());
        if (cat != null) openSub(p, cat);
    }

    // ── Item builders ────────────────────────────────────────────────────

    private ItemStack categoryIcon(CustomLootRegistry.Category cat, int filtered, int total) {
        Material mat;
        ChatColor c;
        switch (cat) {
            case BOSS:      mat = Material.NETHER_STAR; c = ChatColor.GOLD; break;
            case CRAFTED:   mat = Material.IRON_INGOT;  c = ChatColor.AQUA; break;
            case RARE_GEAR: mat = Material.DIAMOND;     c = ChatColor.LIGHT_PURPLE; break;
            case REAGENTS:  mat = Material.BLAZE_POWDER;c = ChatColor.YELLOW; break;
            case LOOTBOXES: mat = Material.CHEST;       c = ChatColor.GREEN; break;
            case VANILLA:   mat = Material.GRASS;       c = ChatColor.WHITE; break;
            default:        mat = Material.PAPER;       c = ChatColor.WHITE; break;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(c + "" + ChatColor.BOLD + fancy(cat) + " Filter");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to edit this loot filter.");
        lore.add("");
        lore.add(c + "Filtered: " + ChatColor.WHITE + filtered + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + total);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private ItemStack decorate(ItemStack base, String lootId, boolean filtered) {
        ItemStack out;
        if (filtered) {
            // White stained glass overlay — preserve original name + lore + add tag
            out = new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.RED.getData());
            ItemMeta src = base.getItemMeta();
            ItemMeta dst = out.getItemMeta();
            if (src != null && src.getDisplayName() != null) dst.setDisplayName(src.getDisplayName());
            List<String> lore = new ArrayList<>();
            if (src != null && src.getLore() != null) lore.addAll(src.getLore());
            lore.add("");
            lore.add(ChatColor.RED + "» FILTERED — click to allow again");
            lore.add(ChatColor.DARK_GRAY + "[id:" + lootId + "]");
            dst.setLore(lore);
            out.setItemMeta(dst);
        } else {
            out = base.clone();
            ItemMeta dst = out.getItemMeta();
            List<String> lore = (dst != null && dst.getLore() != null) ? new ArrayList<>(dst.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GREEN + "» Click to filter out from drops");
            lore.add(ChatColor.DARK_GRAY + "[id:" + lootId + "]");
            if (dst != null) { dst.setLore(lore); out.setItemMeta(dst); }
        }
        return out;
    }

    private ItemStack button(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(name);
        List<String> ll = new ArrayList<>();
        for (String s : lore) ll.add(s);
        meta.setLore(ll);
        i.setItemMeta(meta);
        return i;
    }

    private CustomLootRegistry.Category categoryFromDisplay(ItemStack item) {
        if (item.getItemMeta() == null) return null;
        String stripped = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (stripped == null) return null;
        for (CustomLootRegistry.Category c : CustomLootRegistry.Category.values()) {
            if (stripped.startsWith(fancy(c))) return c;
        }
        return null;
    }

    /** Last lore line is `[id:foo]` — parse it back so we can identify clicks. */
    private String readTaggedId(ItemStack item) {
        if (item.getItemMeta() == null || item.getItemMeta().getLore() == null) return null;
        for (String line : item.getItemMeta().getLore()) {
            String s = ChatColor.stripColor(line);
            if (s != null && s.startsWith("[id:") && s.endsWith("]")) {
                return s.substring(4, s.length() - 1);
            }
        }
        return null;
    }

    private String niceName(ItemStack item) {
        if (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null)
            return item.getItemMeta().getDisplayName();
        return item.getType().name().toLowerCase().replace('_', ' ');
    }

    private String fancy(CustomLootRegistry.Category cat) {
        switch (cat) {
            case BOSS:      return "Boss";
            case CRAFTED:   return "Crafted";
            case RARE_GEAR: return "Rare Gear";
            case REAGENTS:  return "Reagents";
            case LOOTBOXES: return "Loot Boxes";
            case VANILLA:   return "Vanilla";
            default:        return cat.name();
        }
    }
}
