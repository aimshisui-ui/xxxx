package com.soulenchants.quests;

import com.soulenchants.SoulEnchants;
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
import java.util.UUID;

public class QuestGUI implements Listener {

    private static final String TITLE = ChatColor.AQUA + "" + ChatColor.BOLD + "✦ Quest Log ✦";

    private final SoulEnchants plugin;

    public QuestGUI(SoulEnchants plugin) { this.plugin = plugin; }

    public void open(Player p) {
        UUID id = p.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Tutorial section: top row(s)
        int slot = 0;
        List<Quest> tut = QuestRegistry.tutorialChain();
        int tutStep = plugin.getQuestManager().getProfile().getTutorialStep(id);
        for (int i = 0; i < tut.size(); i++) {
            Quest q = tut.get(i);
            inv.setItem(slot++, decorate(q, plugin.getQuestManager().getProfile().getProgress(id, q.id),
                    plugin.getQuestManager().getProfile().isClaimed(id, q.id),
                    i == tutStep, i < tutStep));
        }

        // Dailies section: rows below
        slot = 27;
        if (tutStep >= tut.size()) {
            List<Quest> dailies = plugin.getQuestManager().ensureDailies(p);
            for (Quest q : dailies) {
                inv.setItem(slot++, decorate(q,
                        plugin.getQuestManager().getProfile().getProgress(id, q.id),
                        plugin.getQuestManager().getProfile().isClaimed(id, q.id),
                        true, false));
            }
        } else {
            ItemStack locked = new ItemStack(Material.IRON_FENCE);
            ItemMeta m = locked.getItemMeta();
            m.setDisplayName(ChatColor.RED + "Daily quests locked");
            m.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "Complete the tutorial chain first.",
                    ChatColor.GRAY + "Step " + (tutStep + 1) + "/" + tut.size()));
            locked.setItemMeta(m);
            inv.setItem(31, locked);
        }

        // Header indicator at slot 49
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Your Soul Tier: " + plugin.getSoulManager().getTier(p).prefix());
        im.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Lifetime souls: " + ChatColor.WHITE + plugin.getSoulManager().getLifetime(p),
                ChatColor.GRAY + "Spendable: " + ChatColor.WHITE + plugin.getSoulManager().get(p)));
        info.setItemMeta(im);
        inv.setItem(49, info);

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle() == null) return;
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getItemMeta() == null) return;

        // Identify quest by display name
        String name = clicked.getItemMeta().getDisplayName();
        for (Quest q : QuestRegistry.all()) {
            if (name.contains(q.name)) {
                if (plugin.getQuestManager().claim(p, q.id)) {
                    open(p);
                }
                return;
            }
        }
    }

    private ItemStack decorate(Quest q, int progress, boolean claimed, boolean active, boolean locked) {
        Material mat;
        if (claimed) mat = Material.EMERALD;
        else if (q.isComplete(progress)) mat = Material.GOLD_INGOT;
        else if (active) mat = Material.PAPER;
        else mat = Material.MAP;
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        ChatColor color = q.tier == Quest.Tier.TUTORIAL ? ChatColor.LIGHT_PURPLE : ChatColor.AQUA;
        m.setDisplayName(color + "" + ChatColor.BOLD + q.name);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  ");
        lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + q.description);
        lore.add("");
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.WHITE + progress + "/" + q.goal);
        lore.add(ChatColor.GRAY + "Reward: " + ChatColor.YELLOW + q.soulReward + " souls"
                + (q.itemRewards.isEmpty() ? "" : ChatColor.GRAY + " + " + q.itemRewards.size() + " item(s)"));
        lore.add("");
        if (claimed) {
            lore.add(ChatColor.GREEN + "✓ Claimed");
        } else if (q.isComplete(progress)) {
            lore.add(ChatColor.GOLD + "» Click to claim!");
        } else if (locked) {
            lore.add(ChatColor.RED + "✕ Already past this step");
        } else if (active) {
            lore.add(ChatColor.AQUA + "» In progress");
        } else {
            lore.add(ChatColor.RED + "✕ Locked");
        }
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  ");
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }
}
