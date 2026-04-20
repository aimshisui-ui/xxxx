package com.soulenchants.shop;

import com.soulenchants.SoulEnchants;
import com.soulenchants.currency.SoulTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopGUI implements Listener {

    private static final String HUB_TITLE   = ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Quartermaster ✦";
    private static final int HUB_SIZE = 27;

    private static final Map<String, String> CAT_TITLE = new LinkedHashMap<>();
    static {
        CAT_TITLE.put(ShopCatalog.CAT_FEATURED,    ChatColor.GOLD       + "» Featured (today)");
        CAT_TITLE.put(ShopCatalog.CAT_BOOKS,       ChatColor.LIGHT_PURPLE + "» Enchant Books");
        CAT_TITLE.put(ShopCatalog.CAT_REAGENTS,    ChatColor.GREEN      + "» Reagents");
        CAT_TITLE.put(ShopCatalog.CAT_LOOT_BOXES,  ChatColor.AQUA       + "» Loot Boxes");
        CAT_TITLE.put(ShopCatalog.CAT_CONSUMABLES, ChatColor.YELLOW     + "» Consumables");
    }

    private final SoulEnchants plugin;
    private final ShopFeatured featured;
    // Track which page/category each viewer is on for sell-mode toggles later.
    private final Map<UUID, String> currentCategory = new HashMap<>();

    public ShopGUI(SoulEnchants plugin, ShopFeatured featured) {
        this.plugin = plugin;
        this.featured = featured;
    }

    // ── Public openers ───────────────────────────────────────────────────────
    public void openHub(Player p) {
        featured.maybeRoll();
        Inventory inv = Bukkit.createInventory(null, HUB_SIZE, HUB_TITLE);
        inv.setItem(10, button(Material.GOLD_INGOT,        ChatColor.GOLD        + "Featured (today)",       "Rotates daily — 30% off"));
        inv.setItem(11, button(Material.ENCHANTED_BOOK,    ChatColor.LIGHT_PURPLE+ "Enchant Books",           "Random-within-tier books"));
        inv.setItem(12, button(Material.IRON_BLOCK,        ChatColor.GREEN       + "Reagents",                "Crafting materials"));
        inv.setItem(13, button(Material.CHEST,             ChatColor.AQUA        + "Loot Boxes",              "Weighted random rolls"));
        inv.setItem(14, button(Material.GLASS_BOTTLE,      ChatColor.YELLOW      + "Consumables",             "Dust + scrolls"));
        inv.setItem(16, button(Material.EMERALD,           ChatColor.DARK_GREEN  + "Your Souls",
                ChatColor.GRAY + "Current: " + ChatColor.WHITE + plugin.getSoulManager().get(p),
                ChatColor.GRAY + "Tier: " + plugin.getSoulManager().getTier(p).prefix()));
        inv.setItem(22, button(Material.BARRIER,           ChatColor.RED         + "Close", ""));
        p.openInventory(inv);
    }

    public void openCategory(Player p, String category) {
        featured.maybeRoll();
        List<ShopItem> items;
        if (category.equals(ShopCatalog.CAT_FEATURED)) {
            items = new ArrayList<>();
            for (String id : featured.getFeaturedIds()) {
                ShopItem it = ShopCatalog.byId(id);
                if (it != null) items.add(it);
            }
        } else {
            items = ShopCatalog.byCategory(category);
        }
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, CAT_TITLE.get(category));
        int slot = 0;
        for (ShopItem s : items) {
            if (slot >= 45) break;
            inv.setItem(slot++, decorate(s, p));
        }
        // Footer
        ItemStack filler = button(Material.STAINED_GLASS_PANE, " ", "");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        inv.setItem(45, button(Material.ARROW, ChatColor.YELLOW + "« Back", "Return to hub"));
        inv.setItem(49, button(Material.EMERALD, ChatColor.GREEN + "Your Souls: " + plugin.getSoulManager().get(p), ""));
        currentCategory.put(p.getUniqueId(), category);
        p.openInventory(inv);
    }

    // ── Click handling ───────────────────────────────────────────────────────
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (title == null) return;
        if (!isOurView(title)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Hub navigation
        if (HUB_TITLE.equals(title)) {
            switch (e.getRawSlot()) {
                case 10: openCategory(p, ShopCatalog.CAT_FEATURED);    return;
                case 11: openCategory(p, ShopCatalog.CAT_BOOKS);       return;
                case 12: openCategory(p, ShopCatalog.CAT_REAGENTS);    return;
                case 13: openCategory(p, ShopCatalog.CAT_LOOT_BOXES);  return;
                case 14: openCategory(p, ShopCatalog.CAT_CONSUMABLES); return;
                case 22: p.closeInventory();                           return;
                default: return;
            }
        }

        // Back button in category view
        if (clicked.getType() == Material.ARROW) { openHub(p); return; }
        if (clicked.getType() == Material.STAINED_GLASS_PANE) return;
        if (clicked.getType() == Material.EMERALD && e.getRawSlot() == 49) return;

        // Purchase or sell
        String category = currentCategory.get(p.getUniqueId());
        if (category == null) return;

        // Find the matching ShopItem by comparing display name
        String clickedName = clicked.getItemMeta() == null ? "" : clicked.getItemMeta().getDisplayName();
        ShopItem match = null;
        List<ShopItem> pool = category.equals(ShopCatalog.CAT_FEATURED)
                ? resolveFeatured()
                : ShopCatalog.byCategory(category);
        for (ShopItem s : pool) {
            String sName = s.display.getItemMeta() == null ? "" : s.display.getItemMeta().getDisplayName();
            if (sName.equals(clickedName)) { match = s; break; }
        }
        if (match == null) return;

        if (e.isShiftClick() && match.sellBack > 0) {
            trySell(p, match);
        } else if (e.isRightClick() && match.sellBack > 0) {
            trySell(p, match);
        } else {
            tryBuy(p, match);
        }
    }

    private List<ShopItem> resolveFeatured() {
        List<ShopItem> out = new ArrayList<>();
        for (String id : featured.getFeaturedIds()) {
            ShopItem it = ShopCatalog.byId(id);
            if (it != null) out.add(it);
        }
        return out;
    }

    private void tryBuy(Player p, ShopItem item) {
        SoulTier playerTier = plugin.getSoulManager().getTier(p);
        if (playerTier.ordinal() < item.tier.ordinal()) {
            p.sendMessage(ChatColor.RED + "Requires " + item.tier.prefix() + ChatColor.RED + " tier or higher.");
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1f, 0.6f);
            return;
        }
        long cost = featured.priceFor(item);
        // Soulbound discount
        if (playerTier == SoulTier.SOULBOUND) cost = (long) Math.max(1, cost * 0.90);
        else if (playerTier == SoulTier.VEILED) cost = (long) Math.max(1, cost * 0.95);
        if (!plugin.getSoulManager().take(p, cost)) {
            p.sendMessage(ChatColor.RED + "Not enough souls. Need " + cost + ".");
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1f, 0.6f);
            return;
        }
        ItemStack stack = item.supplier.get();
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(stack);
        for (ItemStack s : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
        p.sendMessage(ChatColor.GREEN + "✦ Purchased " + ChatColor.WHITE
                + (stack.getItemMeta() != null ? stack.getItemMeta().getDisplayName() : stack.getType().name())
                + ChatColor.GREEN + " for " + ChatColor.YELLOW + cost + ChatColor.GREEN + " souls.");
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.6f, 1.5f);
        // Quest: SHOP_PURCHASE
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().onEvent(p,
                    com.soulenchants.quests.QuestEvent.shopPurchase(cost));
        }
        // Refresh view
        openCategory(p, currentCategory.get(p.getUniqueId()));
    }

    private void trySell(Player p, ShopItem item) {
        // Find a matching stack in inventory by display name
        ItemStack ref = item.display;
        String refName = ref.getItemMeta() == null ? "" : ref.getItemMeta().getDisplayName();
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s == null) continue;
            if (s.getItemMeta() == null) continue;
            if (!refName.equals(s.getItemMeta().getDisplayName())) continue;
            // Remove one stack-amount equal to shop display's amount (or less if fewer)
            int sellQty = Math.min(s.getAmount(), ref.getAmount());
            long total = item.sellBack * sellQty / Math.max(1, ref.getAmount());
            if (sellQty >= s.getAmount()) p.getInventory().setItem(i, null);
            else s.setAmount(s.getAmount() - sellQty);
            plugin.getSoulManager().add(p, total);
            p.sendMessage(ChatColor.AQUA + "✦ Sold " + sellQty + "× " + ChatColor.WHITE + refName
                    + ChatColor.AQUA + " for " + ChatColor.YELLOW + total + ChatColor.AQUA + " souls.");
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.8f, 1.2f);
            openCategory(p, currentCategory.get(p.getUniqueId()));
            return;
        }
        p.sendMessage(ChatColor.GRAY + "You don't have any of those to sell.");
    }

    private boolean isOurView(String title) {
        if (title.equals(HUB_TITLE)) return true;
        for (String t : CAT_TITLE.values()) if (title.equals(t)) return true;
        return false;
    }

    private ItemStack decorate(ShopItem s, Player viewer) {
        ItemStack clone = s.display.clone();
        ItemMeta m = clone.getItemMeta();
        if (m == null) return clone;
        List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        // Strip any prior "Price:" / "Sell:" / "Tier gate:" lines we appended
        lore.removeIf(l -> ChatColor.stripColor(l).startsWith("Price:")
                || ChatColor.stripColor(l).startsWith("Sell:")
                || ChatColor.stripColor(l).startsWith("Requires tier")
                || ChatColor.stripColor(l).startsWith("Featured"));
        long displayPrice = featured.priceFor(s);
        if (featured.isFeatured(s.id)) {
            lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Featured " + ChatColor.YELLOW + "(-30%)");
        }
        lore.add(ChatColor.GOLD + "Price: " + ChatColor.YELLOW + displayPrice + " souls"
                + (featured.isFeatured(s.id) ? ChatColor.DARK_GRAY + " (was " + s.price + ")" : ""));
        if (s.sellBack > 0) {
            lore.add(ChatColor.AQUA + "Sell: " + ChatColor.WHITE + s.sellBack + " §8(right/shift click)");
        }
        SoulTier viewerTier = plugin.getSoulManager().getTier(viewer);
        if (viewerTier.ordinal() < s.tier.ordinal()) {
            lore.add(ChatColor.RED + "Requires tier: " + s.tier.prefix());
        }
        m.setLore(lore);
        clone.setItemMeta(m);
        return clone;
    }

    private ItemStack button(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m == null) return it;
        m.setDisplayName(name);
        List<String> loreList = new ArrayList<>();
        for (String s : lore) loreList.add(ChatColor.GRAY + s);
        m.setLore(loreList);
        it.setItemMeta(m);
        return it;
    }
}
