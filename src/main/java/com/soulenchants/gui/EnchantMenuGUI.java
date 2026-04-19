package com.soulenchants.gui;

import com.soulenchants.SoulEnchants;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.items.ItemUtil;
import de.tr7zw.changeme.nbtapi.NBTItem;
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

import java.util.*;

public class EnchantMenuGUI implements Listener {

    private static final String TITLE_PREFIX = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ Enchant Menu ";
    private static final int PAGE_SIZE = 45; // 5 rows × 9
    private static final int NAV_PREV = 45;
    private static final int NAV_INFO = 49;
    private static final int NAV_NEXT = 53;

    private final SoulEnchants plugin;
    private final Map<UUID, Integer> playerPage = new HashMap<>();

    public EnchantMenuGUI(SoulEnchants plugin) { this.plugin = plugin; }

    public void open(Player p) { open(p, 0); }

    public void open(Player p, int page) {
        List<CustomEnchant> all = EnchantRegistry.sortedForMenu();
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPage.put(p.getUniqueId(), page);

        String title = TITLE_PREFIX + ChatColor.GRAY + "(" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, all.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, decoratedBook(all.get(i)));
        }

        // Filler glass on bottom row
        ItemStack filler = filler();
        for (int s = 45; s < 54; s++) inv.setItem(s, filler);

        // Nav buttons
        if (page > 0) inv.setItem(NAV_PREV, navButton(ChatColor.GREEN + "« Previous Page",
                "Page " + page + " of " + totalPages));
        if (page < totalPages - 1) inv.setItem(NAV_NEXT, navButton(ChatColor.GREEN + "Next Page »",
                "Page " + (page + 2) + " of " + totalPages));
        inv.setItem(NAV_INFO, infoButton(page + 1, totalPages, all.size()));

        p.openInventory(inv);
    }

    private ItemStack decoratedBook(CustomEnchant e) {
        ItemStack item = ItemFactories.book(e, e.getMaxLevel(), 100, 0);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>(meta.getLore());
        lore.add("");
        lore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "▶ Click to receive max-level book");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack navButton(String name, String subtitle) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.setDisplayName(ChatColor.BOLD + name);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + subtitle));
        arrow.setItemMeta(meta);
        return arrow;
    }

    private ItemStack infoButton(int currentPage, int totalPages, int totalEnchants) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ Enchant Catalog");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Page " + ChatColor.WHITE + currentPage + ChatColor.GRAY + " of " + ChatColor.WHITE + totalPages,
                ChatColor.GRAY + "Total enchants: " + ChatColor.WHITE + totalEnchants,
                "",
                ChatColor.YELLOW + "" + ChatColor.ITALIC + "Click any book to receive it"
        ));
        book.setItemMeta(meta);
        return book;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!p.hasPermission("soulenchants.admin")) {
            p.sendMessage("§cNo permission.");
            p.closeInventory();
            return;
        }
        int slot = e.getSlot();
        if (slot == NAV_PREV) {
            int cur = playerPage.getOrDefault(p.getUniqueId(), 0);
            open(p, cur - 1);
            return;
        }
        if (slot == NAV_NEXT) {
            int cur = playerPage.getOrDefault(p.getUniqueId(), 0);
            open(p, cur + 1);
            return;
        }
        if (slot == NAV_INFO) return;
        if (slot >= 45) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.ENCHANTED_BOOK) return;
        NBTItem nbt = new NBTItem(clicked);
        if (!nbt.hasKey(ItemUtil.NBT_BOOK_ENCHANT)) return;
        String id = nbt.getString(ItemUtil.NBT_BOOK_ENCHANT);
        int level = nbt.getInteger(ItemUtil.NBT_BOOK_LEVEL);
        CustomEnchant enchant = EnchantRegistry.get(id);
        if (enchant == null) return;
        ItemStack fresh = ItemFactories.book(enchant, level, 100, 0);
        p.getInventory().addItem(fresh).values()
                .forEach(over -> p.getWorld().dropItemNaturally(p.getLocation(), over));
        p.sendMessage("§a✦ Received " + fresh.getItemMeta().getDisplayName());
    }
}
