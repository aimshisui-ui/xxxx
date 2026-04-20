package com.soulenchants.rifts;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Hologram management GUI — opened via /rift hologram (no args) or /rift holos.
 *
 *   ROOT screen — list every saved hologram. Each entry shows the first line,
 *                 location, and line count. Click options:
 *                   Left-click  → open detail (edit lines, teleport, delete)
 *                   Shift+click → quick delete with confirm
 *
 *   DETAIL screen — full lines listed. Buttons:
 *                   Edit lines — chat-prompt: type new lines, "&" for colors,
 *                                blank line to finish, "cancel" to abort.
 *                   Teleport   — TP the admin to the hologram.
 *                   Delete     — destroy + remove from config.
 */
public class HologramGUI implements Listener {

    private static final String T_ROOT   = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "✦ Holograms ✦";
    private static final String T_DETAIL = ChatColor.DARK_AQUA + "» Hologram: ";

    private final SoulEnchants plugin;
    private final HologramManager mgr;

    /** Player → currently-being-edited hologram UUID + new lines being collected via chat. */
    private final Map<UUID, EditState> editing = new HashMap<>();
    /** Player → currently-open detail screen target. */
    private final Map<UUID, UUID> openDetail = new HashMap<>();

    private static class EditState {
        final UUID hologramId;
        final List<String> linesSoFar = new ArrayList<>();
        EditState(UUID id) { this.hologramId = id; }
    }

    public HologramGUI(SoulEnchants plugin, HologramManager mgr) {
        this.plugin = plugin;
        this.mgr = mgr;
    }

    public void openRoot(Player p) {
        List<HologramConfig.Entry> all = mgr.config().all();
        int rows = Math.max(1, ((all.size() + 8) / 9) + 1);
        if (rows > 6) rows = 6;
        Inventory inv = Bukkit.createInventory(null, rows * 9, T_ROOT);
        int i = 0;
        for (HologramConfig.Entry e : all) {
            if (i >= (rows - 1) * 9) break;
            inv.setItem(i++, hologramItem(e));
        }
        inv.setItem(rows * 9 - 5, button(Material.EMERALD, ChatColor.GREEN + "+ New hologram",
                "Create one at your current feet"));
        inv.setItem(rows * 9 - 1, button(Material.BARRIER, ChatColor.RED + "Close", ""));
        p.openInventory(inv);
    }

    public void openDetail(Player p, UUID id) {
        HologramConfig.Entry e = mgr.config().get(id);
        if (e == null) { openRoot(p); return; }
        openDetail.put(p.getUniqueId(), id);
        Inventory inv = Bukkit.createInventory(null, 27,
                T_DETAIL + ChatColor.WHITE + (e.lines.isEmpty() ? "(empty)" : ChatColor.stripColor(e.lines.get(0))));
        // Show lines (up to 9)
        for (int i = 0; i < Math.min(e.lines.size(), 9); i++) {
            inv.setItem(i, lineItem(i, e.lines.get(i)));
        }
        inv.setItem(18, button(Material.PAPER,           ChatColor.AQUA   + "Edit lines",
                "Chat-prompt: type each line", "Use & for color codes",
                "Blank line ends, 'cancel' aborts"));
        inv.setItem(20, button(Material.ENDER_PEARL,     ChatColor.YELLOW + "Teleport here",
                "Goes to (" + (int) e.x + ", " + (int) e.y + ", " + (int) e.z + ") in " + e.world));
        inv.setItem(22, button(Material.TNT,             ChatColor.RED    + "Delete",
                "Permanently remove this hologram"));
        inv.setItem(26, button(Material.ARROW,           ChatColor.YELLOW + "« Back", ""));
        p.openInventory(inv);
    }

    private ItemStack hologramItem(HologramConfig.Entry e) {
        ItemStack it = new ItemStack(Material.NAME_TAG);
        ItemMeta m = it.getItemMeta();
        String preview = e.lines.isEmpty() ? "(empty)" : clampLine(e.lines.get(0), 28);
        m.setDisplayName(ChatColor.WHITE + preview);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Lines: " + ChatColor.WHITE + e.lines.size());
        lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + e.world);
        lore.add(ChatColor.GRAY + "At: " + ChatColor.WHITE
                + "(" + (int) e.x + ", " + (int) e.y + ", " + (int) e.z + ")");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Left-click: " + ChatColor.WHITE + "open detail");
        lore.add(ChatColor.RED    + "Shift+click: " + ChatColor.WHITE + "delete");
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    /** Trim a line for tooltip display so long user text can't push the box off-screen. */
    private static String clampLine(String s, int max) {
        if (s == null) return "";
        String stripped = ChatColor.stripColor(s);
        if (stripped.length() <= max) return s;
        return s.substring(0, Math.min(s.length(), max)) + "…";
    }

    private ItemStack lineItem(int idx, String text) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.GRAY + "Line " + (idx + 1));
        // Long lines go in lore where text wraps naturally
        m.setLore(java.util.Collections.singletonList(ChatColor.RESET + clampLine(text, 36)));
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();
        boolean root = title.equals(T_ROOT);
        boolean detail = title.startsWith(T_DETAIL);
        if (!root && !detail) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!p.hasPermission("soulenchants.admin")) { p.closeInventory(); return; }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (root) {
            int slot = e.getRawSlot();
            int totalRows = e.getInventory().getSize() / 9;
            // Footer buttons (last row): new = size-5, close = size-1
            if (slot == e.getInventory().getSize() - 5) {
                p.closeInventory();
                p.performCommand("rift hologram new");
                return;
            }
            if (slot == e.getInventory().getSize() - 1) { p.closeInventory(); return; }
            // Otherwise it's a hologram entry
            List<HologramConfig.Entry> all = mgr.config().all();
            if (slot < 0 || slot >= all.size()) return;
            HologramConfig.Entry entry = all.get(slot);
            if (e.isShiftClick()) {
                mgr.delete(entry.id);
                p.sendMessage(ChatColor.RED + "✦ Deleted hologram.");
                openRoot(p);
            } else {
                openDetail(p, entry.id);
            }
            return;
        }

        // Detail
        UUID id = openDetail.get(p.getUniqueId());
        if (id == null) { openRoot(p); return; }
        int slot = e.getRawSlot();
        if (slot == 18) {
            // Edit lines via chat
            EditState st = new EditState(id);
            editing.put(p.getUniqueId(), st);
            p.closeInventory();
            p.sendMessage(ChatColor.AQUA + "✦ Type new hologram lines, one per chat message.");
            p.sendMessage(ChatColor.GRAY + "  Use " + ChatColor.WHITE + "&" + ChatColor.GRAY + " for colors (e.g. &c, &l).");
            p.sendMessage(ChatColor.GRAY + "  Send " + ChatColor.WHITE + "done" + ChatColor.GRAY
                    + " to save, " + ChatColor.WHITE + "cancel" + ChatColor.GRAY + " to abort.");
            return;
        }
        if (slot == 20) {
            HologramConfig.Entry entry = mgr.config().get(id);
            if (entry != null) {
                org.bukkit.Location loc = entry.toLocation();
                if (loc != null) {
                    p.closeInventory();
                    p.teleport(loc);
                    p.sendMessage(ChatColor.GREEN + "✦ Teleported to hologram.");
                }
            }
            return;
        }
        if (slot == 22) {
            mgr.delete(id);
            p.sendMessage(ChatColor.RED + "✦ Deleted.");
            openRoot(p);
            return;
        }
        if (slot == 26) { openRoot(p); }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        EditState st = editing.get(p.getUniqueId());
        if (st == null) return;
        e.setCancelled(true);
        String raw = e.getMessage().trim();
        if (raw.equalsIgnoreCase("cancel")) {
            editing.remove(p.getUniqueId());
            p.sendMessage(ChatColor.YELLOW + "✦ Edit cancelled.");
            return;
        }
        if (raw.equalsIgnoreCase("done") || raw.isEmpty()) {
            editing.remove(p.getUniqueId());
            final List<String> lines = st.linesSoFar;
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() {
                    if (lines.isEmpty()) {
                        p.sendMessage(ChatColor.RED + "No lines provided — keeping old text.");
                    } else {
                        mgr.updateLines(st.hologramId, lines);
                        p.sendMessage(ChatColor.GREEN + "✦ Hologram updated (" + lines.size() + " lines).");
                    }
                    openDetail(p, st.hologramId);
                }
            });
            return;
        }
        st.linesSoFar.add(ChatColor.translateAlternateColorCodes('&', raw));
        p.sendMessage(ChatColor.GRAY + "  Line " + st.linesSoFar.size() + " added. Type "
                + ChatColor.WHITE + "done" + ChatColor.GRAY + " when finished.");
    }
}
