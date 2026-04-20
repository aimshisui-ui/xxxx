package com.soulenchants.shop;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Spin-and-reveal loot-box opener inspired by CosmicMysteryChest.
 *
 * Phases (frame = 1 tick):
 *   0-49   spin   — 5 spinner rows cycle stained-glass colors every 2 frames,
 *                   reveal row shows shrinking-question-mark teaser
 *   50-89  reveal — real loot appears one slot at a time, ding per reveal
 *   90+    grant  — inventory closes, items go into the player's inventory
 *                   (overflow drops at feet); session torn down
 *
 * All clicks/drags are cancelled while the holder is a RevealHolder so the
 * player can't lift items mid-spin or shift-click them out. If the player
 * closes the GUI early or quits, remainingItems are still delivered via
 * deliver() called from the close handler.
 */
public final class LootBoxRevealGUI implements Listener {

    private static final int SIZE = 45;                       // 5 rows
    private static final int[] SPINNER_SLOTS = {              // rows 1 + 3 (top & bottom of reveal)
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            27, 28, 29, 30, 31, 32, 33, 34, 35
    };
    private static final int[] REVEAL_SLOTS = { 21, 22, 23 }; // middle-row 3 wide
    private static final byte[] GLASS_COLORS = { 14, 1, 4, 5, 11, 10, 6, 9 };

    private static final int SPIN_END_FRAME    = 50;
    private static final int REVEAL_PER_FRAME  = 12;          // one reveal every ~0.6s after SPIN_END
    private static final int CLOSE_FRAME       = 90;

    private static final Map<UUID, Session> active = new HashMap<>();

    private final SoulEnchants plugin;

    public LootBoxRevealGUI(SoulEnchants plugin) { this.plugin = plugin; }

    /** Public entry — open the reveal GUI for `player`, animate, then deliver. */
    public boolean open(Player player, LootBox.Kind kind, List<ItemStack> rolled) {
        if (active.containsKey(player.getUniqueId())) return false;
        RevealHolder holder = new RevealHolder();
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                kind.color + "" + ChatColor.BOLD + "Opening " + kind.label + " Loot Box");
        holder.inventory = inv;

        // Frame 0 paint: spinners + teaser items
        for (int s : SPINNER_SLOTS) inv.setItem(s, glass(GLASS_COLORS[0]));
        for (int s : REVEAL_SLOTS)  inv.setItem(s, mysteryTeaser());

        Session session = new Session(player.getUniqueId(), kind, new ArrayList<>(rolled), inv);
        active.put(player.getUniqueId(), session);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.CHEST_OPEN, 0.8f, 1.4f);

        session.task = new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline()) { teardown(player.getUniqueId()); cancel(); return; }
                tick(player, session);
                if (session.frame >= CLOSE_FRAME) {
                    cancel();
                    finish(player, session);
                }
                session.frame++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
        return true;
    }

    private void tick(Player player, Session s) {
        // Spinners always cycle, slow to a stop after SPIN_END
        boolean spinning = s.frame < SPIN_END_FRAME;
        int speed = spinning ? 2 : 4;
        if (s.frame % speed == 0) {
            int colorIdx = (s.frame / speed) % GLASS_COLORS.length;
            for (int i = 0; i < SPINNER_SLOTS.length; i++) {
                s.inventory.setItem(SPINNER_SLOTS[i],
                        glass(GLASS_COLORS[(colorIdx + i) % GLASS_COLORS.length]));
            }
            if (spinning && s.frame % 4 == 0) {
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 0.6f, 1.0f + (s.frame % 8) * 0.04f);
            }
        }

        // Reveal phase: pop loot into REVEAL_SLOTS one at a time, left → right
        if (s.frame >= SPIN_END_FRAME) {
            int sinceReveal = s.frame - SPIN_END_FRAME;
            int slotIdx = sinceReveal / REVEAL_PER_FRAME;
            if (slotIdx < REVEAL_SLOTS.length && sinceReveal % REVEAL_PER_FRAME == 0) {
                ItemStack pick = slotIdx < s.rolled.size() ? s.rolled.get(slotIdx) : filler();
                s.inventory.setItem(REVEAL_SLOTS[slotIdx], pick);
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7f, 1.2f + slotIdx * 0.2f);
            }
        }
    }

    private void finish(Player player, Session s) {
        // Closing the inventory triggers onClose, which calls deliver().
        // Doing it via a tick later avoids a "ghost item" sticking to the cursor
        // on some clients when the GUI is force-closed mid-frame.
        new BukkitRunnable() {
            @Override public void run() {
                if (player.isOnline()) player.closeInventory();
                else deliver(s);     // offline → drop at last seen
                teardown(player.getUniqueId());
            }
        }.runTaskLater(plugin, 1L);
    }

    private static void teardown(UUID id) {
        Session s = active.remove(id);
        if (s != null && s.task != null) try { s.task.cancel(); } catch (Throwable ignored) {}
    }

    /** Push items into player's inventory; overflow drops at feet. */
    private void deliver(Session s) {
        Player p = Bukkit.getPlayer(s.player);
        if (p == null) {
            // Offline edge: best-effort, drop at world spawn
            org.bukkit.World w = Bukkit.getWorlds().get(0);
            for (ItemStack it : s.rolled) {
                if (it != null) w.dropItemNaturally(w.getSpawnLocation(), it);
            }
            return;
        }
        p.sendMessage(s.kind.color + "✦ " + ChatColor.BOLD + s.kind.label + " Loot Box"
                + ChatColor.RESET + ChatColor.GRAY + " opened. Contents:");
        for (ItemStack it : s.rolled) {
            if (it == null) continue;
            String name = (it.getItemMeta() != null && it.getItemMeta().hasDisplayName())
                    ? it.getItemMeta().getDisplayName() : it.getType().name();
            p.sendMessage(ChatColor.GRAY + "  ▸ " + ChatColor.WHITE + it.getAmount() + "× " + name);
            Map<Integer, ItemStack> leftover = p.getInventory().addItem(it);
            for (ItemStack lo : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), lo);
        }
    }

    // ── Click / drag / close protection ──────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory() == null) return;
        if (e.getInventory().getHolder() instanceof RevealHolder
                || (e.getClickedInventory() != null
                    && e.getClickedInventory().getHolder() instanceof RevealHolder)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof RevealHolder) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof RevealHolder)) return;
        UUID id = e.getPlayer().getUniqueId();
        Session s = active.get(id);
        if (s == null) return;
        // If they closed mid-spin, finish immediately so they still get their loot
        if (s.task != null) try { s.task.cancel(); } catch (Throwable ignored) {}
        deliver(s);
        active.remove(id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static ItemStack glass(byte color) {
        ItemStack g = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 0, color);
        ItemMeta m = g.getItemMeta();
        m.setDisplayName(ChatColor.GRAY + "✦");
        g.setItemMeta(m);
        return g;
    }

    private static ItemStack mysteryTeaser() {
        ItemStack q = new ItemStack(Material.NETHER_STAR);
        ItemMeta m = q.getItemMeta();
        m.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "?");
        q.setItemMeta(m);
        return q;
    }

    private static ItemStack filler() {
        ItemStack f = new ItemStack(Material.SULPHUR);
        ItemMeta m = f.getItemMeta();
        m.setDisplayName(ChatColor.DARK_GRAY + "—");
        f.setItemMeta(m);
        return f;
    }

    /** Marker holder so click/close listeners can distinguish reveal GUIs. */
    public static final class RevealHolder implements InventoryHolder {
        Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    private static final class Session {
        final UUID player;
        final LootBox.Kind kind;
        final List<ItemStack> rolled;
        final Inventory inventory;
        int frame = 0;
        BukkitTask task;
        Session(UUID player, LootBox.Kind kind, List<ItemStack> rolled, Inventory inv) {
            this.player = player; this.kind = kind; this.rolled = rolled; this.inventory = inv;
        }
    }
}
