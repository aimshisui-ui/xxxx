package com.soulenchants.gui;

import com.soulenchants.SoulEnchants;
import com.soulenchants.mobs.AbilitySpec;
import com.soulenchants.mobs.CustomMob;
import com.soulenchants.mobs.DropSpec;
import com.soulenchants.mobs.MobRegistry;
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
 * Per-mob / per-boss loot + stats editor. Opened via /ce loot.
 *
 * Screens:
 *   ROOT   — picks mob (by tier) or boss
 *   MOB    — shows stat buttons + abilities list + drops list
 *   ABIL   — edits one ability's params
 *   DROP   — edits one drop entry's params
 *   BOSS   — shows boss HP + named damage keys + drop list
 *
 * Number edits route through a chat-prompt: click → close → type value → auto-reopen.
 */
public class LootEditorGUI implements Listener {

    // ── Titles (unique, used for routing clicks) ────────────────────────
    private static final String T_ROOT = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "✦ Loot Editor ✦";
    private static final String T_PICK_EARLY = ChatColor.WHITE + "» Early Mobs";
    private static final String T_PICK_MID   = ChatColor.YELLOW + "» Mid Mobs";
    private static final String T_PICK_LATE  = ChatColor.LIGHT_PURPLE + "» Late Mobs";
    private static final String T_PICK_ELITE = ChatColor.GOLD + "» Elite Mobs";
    private static final String T_PICK_BOSS  = ChatColor.DARK_RED + "» Bosses";
    private static final String T_MOB_PREFIX = ChatColor.DARK_AQUA + "Edit: ";
    private static final String T_ABIL_PREFIX = ChatColor.BLUE + "Ability: ";
    private static final String T_DROP_PREFIX = ChatColor.GREEN + "Drop: ";
    private static final String T_BOSS_PREFIX = ChatColor.DARK_RED + "Boss: ";
    private static final String T_LOOT_PICKER = ChatColor.GOLD + "" + ChatColor.BOLD + "» Pick Custom Item";

    private final SoulEnchants plugin;
    // Chat-prompt state: player UUID → pending edit
    private final Map<UUID, PendingEdit> pending = new HashMap<>();
    // Navigation: which mob/boss/index each player is currently editing
    private final Map<UUID, EditCtx> ctx = new HashMap<>();

    public LootEditorGUI(SoulEnchants plugin) { this.plugin = plugin; }

    // ── ROOT screen ─────────────────────────────────────────────────────
    public void openRoot(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, T_ROOT);
        inv.setItem(10, button(Material.ROTTEN_FLESH,   ChatColor.WHITE        + "Early Mobs",  "T1 roster"));
        inv.setItem(11, button(Material.SPIDER_EYE,     ChatColor.YELLOW       + "Mid Mobs",    "T2 roster"));
        inv.setItem(12, button(Material.GHAST_TEAR,     ChatColor.LIGHT_PURPLE + "Late Mobs",   "T3 roster"));
        inv.setItem(13, button(Material.NETHER_STAR,    ChatColor.GOLD         + "Elite Mobs",  "T4 roster"));
        inv.setItem(15, button(Material.DRAGON_EGG,     ChatColor.DARK_RED     + "Bosses",      "Veilweaver / Colossus"));
        inv.setItem(22, button(Material.BOOK,           ChatColor.AQUA         + "Reload YAML", "Re-read from disk"));
        inv.setItem(26, button(Material.BARRIER,        ChatColor.RED          + "Close",       ""));
        p.openInventory(inv);
    }

    public void openPicker(Player p, CustomMob.Tier tier) {
        String title = tier == CustomMob.Tier.EARLY ? T_PICK_EARLY
                     : tier == CustomMob.Tier.MID   ? T_PICK_MID
                     : tier == CustomMob.Tier.LATE  ? T_PICK_LATE
                                                    : T_PICK_ELITE;
        Inventory inv = Bukkit.createInventory(null, 54, title);
        int slot = 0;
        for (CustomMob cm : MobRegistry.all()) {
            if (cm.tier != tier) continue;
            if (slot >= 45) break;
            inv.setItem(slot++, mobButton(cm));
        }
        inv.setItem(49, backButton());
        p.openInventory(inv);
    }

    public void openBossPicker(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, T_PICK_BOSS);
        inv.setItem(10, bossButton("veilweaver", ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Veilweaver", Material.SKULL_ITEM));
        inv.setItem(13, bossButton("irongolem",  ChatColor.GOLD        + "" + ChatColor.BOLD + "Ironheart Colossus", Material.IRON_BLOCK));
        // Hollow King is a CustomMob — link to mob editor for full stats/abilities/drops control
        inv.setItem(16, bossButton("__hollow_king",
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "The Hollow King",
                Material.GOLD_HELMET));
        inv.setItem(22, backButton());
        p.openInventory(inv);
    }

    // ── MOB editor ──────────────────────────────────────────────────────
    public void openMob(Player p, String mobId) {
        CustomMob cm = MobRegistry.get(mobId);
        if (cm == null) { p.sendMessage(ChatColor.RED + "No such mob: " + mobId); return; }
        ctx.put(p.getUniqueId(), new EditCtx(mobId, false));
        Inventory inv = Bukkit.createInventory(null, 54, T_MOB_PREFIX + cm.tier.color + cm.displayName);

        // Row 0: stats — click adjustments built into each tile
        inv.setItem(0, statTile(Material.REDSTONE,   ChatColor.RED    + "HP",        cm.maxHp));
        inv.setItem(1, statTile(Material.IRON_SWORD, ChatColor.YELLOW + "Bonus DMG", cm.bonusDamage));
        inv.setItem(2, statTile(Material.EMERALD,    ChatColor.GREEN  + "Souls",     cm.souls));
        inv.setItem(4, button(Material.BOOK_AND_QUILL, ChatColor.AQUA + "Reset to Defaults",
                "Restore registry baseline values"));
        inv.setItem(7, button(Material.BOOK,           ChatColor.GREEN + "Save",
                "Write to mob-overrides.yml"));
        inv.setItem(8, backButton());

        // Rows 1-3 (slot 9-35): abilities
        inv.setItem(9, header(Material.BLAZE_POWDER, ChatColor.BLUE + "» Abilities", "Click empty slot to add"));
        int slot = 10;
        for (int i = 0; i < cm.abilitySpecs.size() && slot < 36; i++) {
            AbilitySpec a = cm.abilitySpecs.get(i);
            inv.setItem(slot++, abilityButton(a, i));
        }
        inv.setItem(17, button(Material.CHEST, ChatColor.DARK_GREEN + "+ Add ability", "Prompts for type name"));

        // Rows 4-5 (slot 36-53): drops
        inv.setItem(36, header(Material.CHEST, ChatColor.GREEN + "» Drop Table", "Click empty slot to add"));
        int ds = 37;
        for (int i = 0; i < cm.dropSpecs.size() && ds < 53; i++) {
            DropSpec d = cm.dropSpecs.get(i);
            inv.setItem(ds++, dropButton(d, i));
        }
        inv.setItem(43, button(Material.HOPPER,    ChatColor.DARK_GREEN + "+ Add vanilla drop", "Prompts for material name"));
        inv.setItem(44, button(Material.GOLD_INGOT, ChatColor.GOLD       + "+ Add custom item",  "Picks from named loot pool"));

        p.openInventory(inv);
    }

    private ItemStack mobButton(CustomMob cm) {
        ItemStack it = new ItemStack(Material.SKULL_ITEM, 1);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(cm.tier.color + cm.displayName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "id: " + ChatColor.WHITE + cm.id);
        lore.add(ChatColor.GRAY + "hp " + ChatColor.RED + cm.maxHp
               + ChatColor.GRAY + "  dmg " + ChatColor.YELLOW + String.format("%.1f", cm.bonusDamage)
               + ChatColor.GRAY + "  souls " + ChatColor.GREEN + cm.souls);
        lore.add(ChatColor.GRAY + "abilities " + ChatColor.WHITE + cm.abilitySpecs.size()
               + ChatColor.GRAY + "  drops " + ChatColor.WHITE + cm.dropSpecs.size());
        lore.add("");
        lore.add(ChatColor.YELLOW + "» Click to edit");
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    private ItemStack abilityButton(AbilitySpec a, int index) {
        ItemStack it = new ItemStack(Material.BLAZE_ROD);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.BLUE + a.type + ChatColor.GRAY + "  #" + index);
        List<String> lore = new ArrayList<>();
        for (Map.Entry<String, Object> e : a.params.entrySet()) {
            lore.add(ChatColor.GRAY + e.getKey() + ": " + ChatColor.WHITE + summarize(e.getValue()));
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "L-click " + ChatColor.GRAY + "edit");
        lore.add(ChatColor.RED    + "R-click " + ChatColor.GRAY + "remove");
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    /** Render a param value as a short, single-line tooltip-safe string. */
    private static String summarize(Object value) {
        if (value == null) return "—";
        if (value instanceof java.util.Collection) {
            return "[" + ((java.util.Collection<?>) value).size() + " items]";
        }
        if (value instanceof org.bukkit.inventory.ItemStack) {
            org.bukkit.inventory.ItemStack is = (org.bukkit.inventory.ItemStack) value;
            String name = is.getItemMeta() != null && is.getItemMeta().hasDisplayName()
                    ? is.getItemMeta().getDisplayName()
                    : is.getType().name();
            return name + " ×" + is.getAmount();
        }
        String s = String.valueOf(value);
        // Hard cap so unusually long strings can't blow out the tooltip width
        if (s.length() > 24) s = s.substring(0, 22) + "…";
        return s;
    }

    private ItemStack dropButton(DropSpec d, int index) {
        // Use the icon material (preserves visual identity) but build a SHORT
        // tooltip from scratch so long custom-item lore doesn't clip off-screen.
        Material iconMat;
        String shownName;
        if (d.isCustom()) {
            com.soulenchants.loot.CustomLootRegistry.Entry entry =
                    com.soulenchants.loot.CustomLootRegistry.get(d.lootId);
            if (entry != null) {
                ItemStack source = entry.create();
                iconMat = source.getType();
                shownName = source.getItemMeta() != null && source.getItemMeta().hasDisplayName()
                        ? source.getItemMeta().getDisplayName()
                        : entry.displayName;
            } else {
                iconMat = d.material == null || d.material == Material.AIR ? Material.STONE : d.material;
                shownName = ChatColor.GREEN + d.lootId;
            }
        } else {
            iconMat = d.material == null || d.material == Material.AIR ? Material.STONE : d.material;
            shownName = ChatColor.GREEN + (d.material != null ? d.material.name() : "AIR");
        }
        ItemStack it = new ItemStack(iconMat, Math.max(1, d.min));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(shownName + ChatColor.GRAY + "  #" + index);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "amount " + ChatColor.WHITE + d.min + "-" + d.max
                + ChatColor.GRAY + "  chance " + ChatColor.WHITE + String.format("%.2f", d.chance));
        if (d.isCustom()) lore.add(ChatColor.DARK_GRAY + d.lootId);
        lore.add("");
        lore.add(ChatColor.YELLOW + "L-click " + ChatColor.GRAY + "edit");
        lore.add(ChatColor.RED    + "R-click " + ChatColor.GRAY + "remove");
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    // ── ABIL editor ─────────────────────────────────────────────────────
    public void openAbility(Player p, String mobId, int index) {
        CustomMob cm = MobRegistry.get(mobId);
        if (cm == null || index < 0 || index >= cm.abilitySpecs.size()) { openMob(p, mobId); return; }
        AbilitySpec a = cm.abilitySpecs.get(index);
        EditCtx c = ctx.getOrDefault(p.getUniqueId(), new EditCtx(mobId, false));
        c.abilityIndex = index;
        ctx.put(p.getUniqueId(), c);

        Inventory inv = Bukkit.createInventory(null, 27, T_ABIL_PREFIX + a.type);
        int slot = 10;
        for (Map.Entry<String, Object> e : a.params.entrySet()) {
            if (slot > 16) break;
            inv.setItem(slot++, paramButton(e.getKey(), e.getValue()));
        }
        inv.setItem(22, button(Material.BOOK, ChatColor.GREEN + "Save & Return", "Persist to YAML"));
        inv.setItem(26, button(Material.ARROW, ChatColor.YELLOW + "« Back", ""));
        p.openInventory(inv);
    }

    private ItemStack paramButton(String key, Object value) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.AQUA + key + ChatColor.GRAY + ": " + ChatColor.WHITE + summarize(value));
        List<String> lore = new ArrayList<>();
        boolean numeric = value instanceof Number;
        if (numeric) {
            lore.add(ChatColor.GREEN + "L " + ChatColor.GRAY + "+1");
            lore.add(ChatColor.RED   + "R " + ChatColor.GRAY + "-1");
            lore.add(ChatColor.GREEN + "S+L " + ChatColor.GRAY + "+10");
            lore.add(ChatColor.RED   + "S+R " + ChatColor.GRAY + "-10");
            lore.add(ChatColor.AQUA  + "Q " + ChatColor.GRAY + "type exact");
        } else {
            lore.add(ChatColor.YELLOW + "» Click to set");
        }
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    // ── DROP editor ─────────────────────────────────────────────────────
    public void openDrop(Player p, String mobId, int index, boolean isBoss) {
        CustomMob cm = isBoss ? null : MobRegistry.get(mobId);
        List<DropSpec> list = isBoss ? plugin.getLootConfig().bossDrops(mobId) : (cm == null ? null : cm.dropSpecs);
        if (list == null || index < 0 || index >= list.size()) { if (isBoss) openBoss(p, mobId); else openMob(p, mobId); return; }
        DropSpec d = list.get(index);
        EditCtx c = ctx.getOrDefault(p.getUniqueId(), new EditCtx(mobId, isBoss));
        c.dropIndex = index;
        c.isBoss = isBoss;
        ctx.put(p.getUniqueId(), c);

        Inventory inv = Bukkit.createInventory(null, 27, T_DROP_PREFIX + (d.material == null ? "AIR" : d.material.name()));
        inv.setItem(10, paramButton("material", d.material == null ? "AIR" : d.material.name()));
        inv.setItem(11, paramButton("min", d.min));
        inv.setItem(12, paramButton("max", d.max));
        inv.setItem(13, paramButton("chance", d.chance));
        inv.setItem(22, button(Material.BOOK, ChatColor.GREEN + "Save & Return", "Persist to YAML"));
        inv.setItem(26, button(Material.ARROW, ChatColor.YELLOW + "« Back", ""));
        p.openInventory(inv);
    }

    // ── BOSS editor ─────────────────────────────────────────────────────
    public void openBoss(Player p, String bossId) {
        ctx.put(p.getUniqueId(), new EditCtx(bossId, true));
        String displayName = bossId.equals("veilweaver") ? "Veilweaver" : "Ironheart Colossus";
        Inventory inv = Bukkit.createInventory(null, 54, T_BOSS_PREFIX + displayName);

        double hp = plugin.getLootConfig().bossHp(bossId,
                bossId.equals("veilweaver") ? com.soulenchants.bosses.Veilweaver.MAX_HP : com.soulenchants.bosses.IronGolemBoss.MAX_HP);
        inv.setItem(0, statTile(Material.REDSTONE, ChatColor.RED + "Boss HP", hp));
        inv.setItem(7, button(Material.BOOK, ChatColor.GREEN + "Save", "Persist to boss-overrides.yml"));
        inv.setItem(8, backButton());

        inv.setItem(9, header(Material.IRON_SWORD, ChatColor.BLUE + "» Attack Damage", "Click to edit"));
        int slot = 10;
        String[] keys = bossId.equals("veilweaver")
                ? new String[]{"cleave","tether_pull","shatter_ring","lightning","apocalypse_tick","shatter_bolt"}
                : new String[]{"boulder","ground_slam","charge","shockwave"};
        double[] defaults = bossId.equals("veilweaver")
                ? new double[]{70, 95, 55, 14, 70.0/12.0, 10}
                : new double[]{260, 280, 340, 290};
        for (int i = 0; i < keys.length && slot < 18; i++) {
            double cur = plugin.getLootConfig().bossDamage(bossId, keys[i], defaults[i]);
            inv.setItem(slot++, damageKeyButton(keys[i], cur));
        }

        inv.setItem(36, header(Material.CHEST, ChatColor.GREEN + "» Extra Drops", "Stacks on top of fixed boss loot"));
        int ds = 37;
        java.util.List<DropSpec> bdrops = plugin.getLootConfig().bossDrops(bossId);
        for (int i = 0; i < bdrops.size() && ds < 53; i++) {
            inv.setItem(ds++, dropButton(bdrops.get(i), i));
        }
        inv.setItem(43, button(Material.HOPPER,    ChatColor.DARK_GREEN + "+ Add vanilla drop", "Prompts for material name"));
        inv.setItem(44, button(Material.GOLD_INGOT, ChatColor.GOLD       + "+ Add custom item",  "Picks from named loot pool"));

        p.openInventory(inv);
    }

    private ItemStack damageKeyButton(String key, double cur) {
        ItemStack it = new ItemStack(Material.IRON_SWORD);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.BLUE + key);
        m.setLore(Arrays.asList(
                ChatColor.GRAY + "current: " + ChatColor.WHITE + String.format("%.2f", cur),
                "",
                ChatColor.YELLOW + "» Click to type new value"));
        it.setItemMeta(m);
        return it;
    }

    // ── Click routing ──────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();
        if (!isOurs(title)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (!p.hasPermission("soulenchants.admin")) { p.closeInventory(); return; }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        int slot = e.getRawSlot();

        if (title.equals(T_ROOT)) {
            switch (slot) {
                case 10: openPicker(p, CustomMob.Tier.EARLY); return;
                case 11: openPicker(p, CustomMob.Tier.MID);   return;
                case 12: openPicker(p, CustomMob.Tier.LATE);  return;
                case 13: openPicker(p, CustomMob.Tier.ELITE); return;
                case 15: openBossPicker(p); return;
                case 22: plugin.getLootConfig().reload(); p.sendMessage(ChatColor.GREEN + "✦ Loot config reloaded."); return;
                case 26: p.closeInventory(); return;
            }
            return;
        }

        if (title.equals(T_PICK_EARLY) || title.equals(T_PICK_MID) || title.equals(T_PICK_LATE) || title.equals(T_PICK_ELITE)) {
            if (slot == 49) { openRoot(p); return; }
            // slot 0-44 is a mob — look up by index into the tier list
            CustomMob.Tier tier = title.equals(T_PICK_EARLY) ? CustomMob.Tier.EARLY
                               : title.equals(T_PICK_MID)   ? CustomMob.Tier.MID
                               : title.equals(T_PICK_LATE)  ? CustomMob.Tier.LATE
                                                            : CustomMob.Tier.ELITE;
            int idx = 0;
            for (CustomMob cm : MobRegistry.all()) {
                if (cm.tier != tier) continue;
                if (idx == slot) { openMob(p, cm.id); return; }
                idx++;
            }
            return;
        }

        if (title.equals(T_PICK_BOSS)) {
            if (slot == 10) { openBoss(p, "veilweaver"); return; }
            if (slot == 13) { openBoss(p, "irongolem"); return; }
            if (slot == 16) {
                // Hollow King is a CustomMob — route to the mob editor for full control
                openMob(p, "hollow_king");
                return;
            }
            if (slot == 22) { openRoot(p); return; }
            return;
        }

        if (title.startsWith(T_MOB_PREFIX)) { handleMobScreen(p, e, slot); return; }
        if (title.startsWith(T_ABIL_PREFIX)) { handleAbilityScreen(p, e, slot); return; }
        if (title.startsWith(T_DROP_PREFIX)) { handleDropScreen(p, e, slot); return; }
        if (title.startsWith(T_BOSS_PREFIX)) { handleBossScreen(p, e, slot); return; }
        if (title.equals(T_LOOT_PICKER)) { handleLootPicker(p, e, slot); return; }
    }

    private void handleMobScreen(Player p, InventoryClickEvent e, int slot) {
        EditCtx c = ctx.get(p.getUniqueId());
        if (c == null || c.mobId == null) return;
        CustomMob cm = MobRegistry.get(c.mobId);
        if (cm == null) return;

        // HP / DMG / Souls — click adjustments. Drop key (Q) for chat-prompt set.
        if (slot == 0 || slot == 1 || slot == 2) {
            org.bukkit.event.inventory.ClickType ct = e.getClick();
            if (ct == org.bukkit.event.inventory.ClickType.DROP || ct == org.bukkit.event.inventory.ClickType.CONTROL_DROP) {
                PromptKind kind = slot == 0 ? PromptKind.HP : slot == 1 ? PromptKind.DMG : PromptKind.SOULS;
                String label  = slot == 0 ? "HP (integer)" : slot == 1 ? "bonus damage (decimal)" : "souls (integer)";
                prompt(p, kind, c.mobId, -1, "Type new " + label + ":");
                return;
            }
            // Compute delta
            int sign = e.isRightClick() ? -1 : 1;
            int mag = e.isShiftClick() ? 10 : 1;
            int delta = sign * mag;
            if (slot == 0) cm.maxHp = Math.max(1, cm.maxHp + delta);
            else if (slot == 1) cm.bonusDamage = Math.max(0, cm.bonusDamage + delta);
            else cm.souls = Math.max(0, cm.souls + delta);
            openMob(p, c.mobId);
            return;
        }
        if (slot == 4) { cm.resetToDefaults(); plugin.getLootConfig().clearMob(c.mobId); p.sendMessage(ChatColor.YELLOW + "✦ Reset " + c.mobId + " to registry defaults."); openMob(p, c.mobId); return; }
        if (slot == 7) { plugin.getLootConfig().saveMob(cm); p.sendMessage(ChatColor.GREEN + "✦ Saved " + c.mobId + "."); return; }
        if (slot == 8) { openRoot(p); return; }
        if (slot == 17) { prompt(p, PromptKind.ADD_ABILITY, c.mobId, -1, "Type ability type (e.g. bonus_damage, leap, fireball):"); return; }
        if (slot == 43) { prompt(p, PromptKind.ADD_DROP, c.mobId, -1, "Type drop material name (e.g. DIAMOND):"); return; }
        if (slot == 44) { openLootPicker(p, c.mobId, false); return; }

        // Abilities 10..16 (indices 0..6)
        if (slot >= 10 && slot <= 16) {
            int idx = slot - 10;
            if (idx >= cm.abilitySpecs.size()) return;
            if (e.isRightClick()) {
                cm.abilitySpecs.remove(idx);
                p.sendMessage(ChatColor.YELLOW + "✦ Removed ability #" + idx);
                openMob(p, c.mobId);
            } else {
                openAbility(p, c.mobId, idx);
            }
            return;
        }
        // Drops 37..52
        if (slot >= 37 && slot <= 52) {
            int idx = slot - 37;
            if (idx >= cm.dropSpecs.size()) return;
            if (e.isRightClick()) {
                cm.dropSpecs.remove(idx);
                p.sendMessage(ChatColor.YELLOW + "✦ Removed drop #" + idx);
                openMob(p, c.mobId);
            } else {
                openDrop(p, c.mobId, idx, false);
            }
            return;
        }
    }

    private void handleAbilityScreen(Player p, InventoryClickEvent e, int slot) {
        EditCtx c = ctx.get(p.getUniqueId());
        if (c == null || c.mobId == null || c.abilityIndex < 0) return;
        CustomMob cm = MobRegistry.get(c.mobId);
        if (cm == null || c.abilityIndex >= cm.abilitySpecs.size()) return;

        if (slot == 22) { plugin.getLootConfig().saveMob(cm); p.sendMessage(ChatColor.GREEN + "✦ Saved."); openMob(p, c.mobId); return; }
        if (slot == 26) { openMob(p, c.mobId); return; }

        if (slot >= 10 && slot <= 16) {
            int idx = slot - 10;
            AbilitySpec a = cm.abilitySpecs.get(c.abilityIndex);
            String key = keyAt(a.params, idx);
            if (key == null) return;
            Object cur = a.params.get(key);
            org.bukkit.event.inventory.ClickType ct = e.getClick();
            // Drop key (Q) → chat-prompt for exact value (e.g., for non-numeric params)
            if (ct == org.bukkit.event.inventory.ClickType.DROP || ct == org.bukkit.event.inventory.ClickType.CONTROL_DROP) {
                prompt(p, PromptKind.ABILITY_PARAM, c.mobId, c.abilityIndex, "Type new value for §e" + key + "§7:");
                pending.get(p.getUniqueId()).paramKey = key;
                return;
            }
            // Numeric quick-adjust — click=+1, right=-1, shift=×10
            if (cur instanceof Number) {
                int sign = e.isRightClick() ? -1 : 1;
                int mag = e.isShiftClick() ? 10 : 1;
                if (cur instanceof Integer || cur instanceof Long) {
                    int v = ((Number) cur).intValue() + sign * mag;
                    a.params.put(key, Math.max(0, v));
                } else {
                    double v = ((Number) cur).doubleValue() + sign * mag;
                    a.params.put(key, Math.max(0.0, v));
                }
                openAbility(p, c.mobId, c.abilityIndex);
                return;
            }
            // Non-numeric (string/list) — chat-prompt
            prompt(p, PromptKind.ABILITY_PARAM, c.mobId, c.abilityIndex, "Type new value for §e" + key + "§7:");
            pending.get(p.getUniqueId()).paramKey = key;
        }
    }

    private void handleDropScreen(Player p, InventoryClickEvent e, int slot) {
        EditCtx c = ctx.get(p.getUniqueId());
        if (c == null || c.mobId == null || c.dropIndex < 0) return;

        if (slot == 22) {
            if (c.isBoss) {
                // save already done via direct list mutation through bossSection; serialize now
                plugin.getLootConfig().setBossDrops(c.mobId, plugin.getLootConfig().bossDrops(c.mobId));
            } else {
                CustomMob cm = MobRegistry.get(c.mobId);
                if (cm != null) plugin.getLootConfig().saveMob(cm);
            }
            p.sendMessage(ChatColor.GREEN + "✦ Saved.");
            if (c.isBoss) openBoss(p, c.mobId); else openMob(p, c.mobId);
            return;
        }
        if (slot == 26) {
            if (c.isBoss) openBoss(p, c.mobId); else openMob(p, c.mobId);
            return;
        }

        String[] keys = { "material", "min", "max", "chance" };
        if (slot >= 10 && slot <= 13) {
            String k = keys[slot - 10];
            DropSpec d = dropAt(c.mobId, c.dropIndex, c.isBoss);
            org.bukkit.event.inventory.ClickType ct = e.getClick();
            // Material always needs chat. Numerics: left=+1, right=-1, shift=×10, Q=chat exact
            boolean wantsChat = k.equals("material")
                    || ct == org.bukkit.event.inventory.ClickType.DROP
                    || ct == org.bukkit.event.inventory.ClickType.CONTROL_DROP
                    || d == null;
            if (wantsChat) {
                prompt(p, PromptKind.DROP_PARAM, c.mobId, c.dropIndex,
                        "Type new value for §e" + k + "§7" + (k.equals("material") ? " (e.g. DIAMOND):" : ":"));
                pending.get(p.getUniqueId()).paramKey = k;
                pending.get(p.getUniqueId()).isBoss = c.isBoss;
                return;
            }
            int sign = e.isRightClick() ? -1 : 1;
            int mag = e.isShiftClick() ? 10 : 1;
            if (k.equals("min")) d.min = Math.max(1, d.min + sign * mag);
            else if (k.equals("max")) d.max = Math.max(d.min, d.max + sign * mag);
            else if (k.equals("chance")) {
                double step = e.isShiftClick() ? 0.10 : 0.01;
                d.chance = Math.max(0.0, Math.min(1.0, d.chance + sign * step));
            }
            openDrop(p, c.mobId, c.dropIndex, c.isBoss);
        }
    }

    private void handleBossScreen(Player p, InventoryClickEvent e, int slot) {
        EditCtx c = ctx.get(p.getUniqueId());
        if (c == null || c.mobId == null) return;
        String bossId = c.mobId;

        if (slot == 0) { prompt(p, PromptKind.BOSS_HP, bossId, -1, "Type new boss HP (decimal):"); return; }
        if (slot == 7) { p.sendMessage(ChatColor.GREEN + "✦ Boss overrides saved. Next spawn uses new values."); openRoot(p); return; }
        if (slot == 8) { openRoot(p); return; }
        if (slot == 43) { prompt(p, PromptKind.ADD_BOSS_DROP, bossId, -1, "Type drop material (e.g. DIAMOND):"); return; }
        if (slot == 44) { openLootPicker(p, bossId, true); return; }

        // Damage keys slots 10..17
        if (slot >= 10 && slot <= 17) {
            String[] keys = bossId.equals("veilweaver")
                    ? new String[]{"cleave","tether_pull","shatter_ring","lightning","apocalypse_tick","shatter_bolt"}
                    : new String[]{"boulder","ground_slam","charge","shockwave"};
            int idx = slot - 10;
            if (idx >= keys.length) return;
            prompt(p, PromptKind.BOSS_DAMAGE, bossId, -1, "Type new damage for §e" + keys[idx] + "§7:");
            pending.get(p.getUniqueId()).paramKey = keys[idx];
            return;
        }
        if (slot >= 37 && slot <= 52) {
            int idx = slot - 37;
            List<DropSpec> drops = plugin.getLootConfig().bossDrops(bossId);
            if (idx >= drops.size()) return;
            if (e.isRightClick()) {
                drops.remove(idx);
                plugin.getLootConfig().setBossDrops(bossId, drops);
                p.sendMessage(ChatColor.YELLOW + "✦ Removed drop #" + idx);
                openBoss(p, bossId);
            } else {
                openDrop(p, bossId, idx, true);
            }
        }
    }

    // ── Chat-prompt handling ───────────────────────────────────────────

    private enum PromptKind { HP, DMG, SOULS, ABILITY_PARAM, DROP_PARAM, ADD_ABILITY, ADD_DROP, BOSS_HP, BOSS_DAMAGE, ADD_BOSS_DROP }

    private static class PendingEdit {
        PromptKind kind;
        String mobId;
        int index;
        String paramKey;
        boolean isBoss;
        PendingEdit(PromptKind k, String m, int i) { kind = k; mobId = m; index = i; }
    }

    private static class EditCtx {
        final String mobId;
        boolean isBoss;
        int abilityIndex = -1;
        int dropIndex = -1;
        EditCtx(String mobId, boolean isBoss) { this.mobId = mobId; this.isBoss = isBoss; }
    }

    private void prompt(Player p, PromptKind kind, String mobId, int index, String msg) {
        pending.put(p.getUniqueId(), new PendingEdit(kind, mobId, index));
        p.closeInventory();
        p.sendMessage(ChatColor.AQUA + "» " + msg);
        p.sendMessage(ChatColor.GRAY + "  (Type " + ChatColor.RED + "cancel" + ChatColor.GRAY + " to abort.)");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        PendingEdit pe = pending.remove(p.getUniqueId());
        if (pe == null) return;
        e.setCancelled(true);
        String raw = ChatColor.stripColor(e.getMessage()).trim();
        if (raw.equalsIgnoreCase("cancel")) { p.sendMessage(ChatColor.YELLOW + "✦ Edit cancelled."); reopen(p, pe); return; }

        // Must run sync to touch world state
        final PendingEdit fpe = pe;
        final String fraw = raw;
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() { applyEdit(p, fpe, fraw); reopen(p, fpe); }
        });
    }

    private void applyEdit(Player p, PendingEdit pe, String raw) {
        try {
            switch (pe.kind) {
                case HP: {
                    CustomMob cm = MobRegistry.get(pe.mobId);
                    if (cm != null) cm.maxHp = Integer.parseInt(raw);
                    break;
                }
                case DMG: {
                    CustomMob cm = MobRegistry.get(pe.mobId);
                    if (cm != null) cm.bonusDamage = Double.parseDouble(raw);
                    break;
                }
                case SOULS: {
                    CustomMob cm = MobRegistry.get(pe.mobId);
                    if (cm != null) cm.souls = Integer.parseInt(raw);
                    break;
                }
                case ABILITY_PARAM: {
                    CustomMob cm = MobRegistry.get(pe.mobId);
                    if (cm == null || pe.index < 0 || pe.index >= cm.abilitySpecs.size()) break;
                    AbilitySpec a = cm.abilitySpecs.get(pe.index);
                    Object parsed = parseNumeric(raw, a.params.get(pe.paramKey));
                    a.params.put(pe.paramKey, parsed);
                    break;
                }
                case DROP_PARAM: {
                    DropSpec d = dropAt(pe.mobId, pe.index, pe.isBoss);
                    if (d == null) break;
                    if (pe.paramKey.equals("material")) {
                        try { d.material = Material.valueOf(raw.toUpperCase()); }
                        catch (Throwable t) { p.sendMessage(ChatColor.RED + "Unknown material: " + raw); }
                    } else if (pe.paramKey.equals("min")) d.min = Integer.parseInt(raw);
                    else if (pe.paramKey.equals("max")) d.max = Integer.parseInt(raw);
                    else if (pe.paramKey.equals("chance")) d.chance = Math.max(0, Math.min(1, Double.parseDouble(raw)));
                    // If boss, persist the edited list now
                    if (pe.isBoss) {
                        List<DropSpec> all = plugin.getLootConfig().bossDrops(pe.mobId);
                        if (pe.index >= 0 && pe.index < all.size()) all.set(pe.index, d);
                        plugin.getLootConfig().setBossDrops(pe.mobId, all);
                    }
                    break;
                }
                case ADD_ABILITY: {
                    CustomMob cm = MobRegistry.get(pe.mobId);
                    if (cm == null) break;
                    String type = raw.toLowerCase().replace(' ', '_');
                    if (!com.soulenchants.mobs.AbilityFactory.isRegistered(type)) {
                        p.sendMessage(ChatColor.RED + "Unknown ability type: " + type);
                        p.sendMessage(ChatColor.GRAY + "Valid: permanent_effect, hit_effect, bonus_damage, lifesteal, set_on_fire, knockback, steal_souls, reflect, teleport_on_hurt, regen_on_hurt, aura, particle_aura, leap, fireball, aoe_burst, death_explode, split_spawn, death_drop");
                        break;
                    }
                    cm.abilitySpecs.add(AbilitySpec.of(type));
                    p.sendMessage(ChatColor.GREEN + "✦ Added empty ability '" + type + "'. Click it to fill in params.");
                    break;
                }
                case ADD_DROP: {
                    CustomMob cm = MobRegistry.get(pe.mobId);
                    if (cm == null) break;
                    Material mat;
                    try { mat = Material.valueOf(raw.toUpperCase()); }
                    catch (Throwable t) { p.sendMessage(ChatColor.RED + "Unknown material: " + raw); break; }
                    cm.dropSpecs.add(new DropSpec(mat, 1, 1, 1.0));
                    p.sendMessage(ChatColor.GREEN + "✦ Added drop " + mat.name() + ". Click it to adjust min/max/chance.");
                    break;
                }
                case BOSS_HP: {
                    plugin.getLootConfig().setBossHp(pe.mobId, Double.parseDouble(raw));
                    p.sendMessage(ChatColor.GREEN + "✦ " + pe.mobId + " HP set to " + raw + ".");
                    break;
                }
                case BOSS_DAMAGE: {
                    plugin.getLootConfig().setBossDamage(pe.mobId, pe.paramKey, Double.parseDouble(raw));
                    p.sendMessage(ChatColor.GREEN + "✦ " + pe.mobId + "." + pe.paramKey + " set to " + raw + ".");
                    break;
                }
                case ADD_BOSS_DROP: {
                    Material mat;
                    try { mat = Material.valueOf(raw.toUpperCase()); }
                    catch (Throwable t) { p.sendMessage(ChatColor.RED + "Unknown material: " + raw); break; }
                    List<DropSpec> all = plugin.getLootConfig().bossDrops(pe.mobId);
                    all.add(new DropSpec(mat, 1, 1, 1.0));
                    plugin.getLootConfig().setBossDrops(pe.mobId, all);
                    p.sendMessage(ChatColor.GREEN + "✦ Added drop " + mat.name() + ".");
                    break;
                }
            }
        } catch (NumberFormatException nfe) {
            p.sendMessage(ChatColor.RED + "Not a valid number: " + raw);
        } catch (Throwable t) {
            p.sendMessage(ChatColor.RED + "Edit failed: " + t.getMessage());
        }
    }

    private DropSpec dropAt(String mobId, int index, boolean isBoss) {
        if (isBoss) {
            List<DropSpec> all = plugin.getLootConfig().bossDrops(mobId);
            return (index >= 0 && index < all.size()) ? all.get(index) : null;
        }
        CustomMob cm = MobRegistry.get(mobId);
        if (cm == null) return null;
        return (index >= 0 && index < cm.dropSpecs.size()) ? cm.dropSpecs.get(index) : null;
    }

    private void reopen(Player p, PendingEdit pe) {
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                switch (pe.kind) {
                    case ABILITY_PARAM: openAbility(p, pe.mobId, pe.index); break;
                    case DROP_PARAM:    openDrop(p, pe.mobId, pe.index, pe.isBoss); break;
                    case BOSS_HP:
                    case BOSS_DAMAGE:
                    case ADD_BOSS_DROP: openBoss(p, pe.mobId); break;
                    default:            openMob(p, pe.mobId);
                }
            }
        }, 2L);
    }

    private Object parseNumeric(String raw, Object existing) {
        // Preserve the existing param's numeric type. Fall back to double.
        if (existing instanceof Integer) { return Integer.parseInt(raw); }
        if (existing instanceof Long)    { return Long.parseLong(raw); }
        if (existing instanceof Double)  { return Double.parseDouble(raw); }
        try { return Integer.parseInt(raw); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(raw); } catch (NumberFormatException ignored) {}
        return raw;
    }

    private String keyAt(Map<String, Object> m, int index) {
        int i = 0;
        for (String k : m.keySet()) { if (i == index) return k; i++; }
        return null;
    }

    private boolean isOurs(String title) {
        return title.equals(T_ROOT) || title.equals(T_PICK_EARLY) || title.equals(T_PICK_MID)
            || title.equals(T_PICK_LATE) || title.equals(T_PICK_ELITE) || title.equals(T_PICK_BOSS)
            || title.startsWith(T_MOB_PREFIX) || title.startsWith(T_ABIL_PREFIX)
            || title.startsWith(T_DROP_PREFIX) || title.startsWith(T_BOSS_PREFIX)
            || title.equals(T_LOOT_PICKER);
    }

    /** Custom-item picker — shows every CustomLootRegistry entry grouped by category.
     *  Tooltips are deliberately short (no original lore preserved) so they don't
     *  spill off-screen for items with long flavor text. */
    public void openLootPicker(Player p, String targetId, boolean isBoss) {
        EditCtx c = ctx.getOrDefault(p.getUniqueId(), new EditCtx(targetId, isBoss));
        c.isBoss = isBoss;
        ctx.put(p.getUniqueId(), c);
        Inventory inv = Bukkit.createInventory(null, 54, T_LOOT_PICKER);
        com.soulenchants.loot.CustomLootRegistry.Category[] cats = {
                com.soulenchants.loot.CustomLootRegistry.Category.BOSS,
                com.soulenchants.loot.CustomLootRegistry.Category.CRAFTED,
                com.soulenchants.loot.CustomLootRegistry.Category.RARE_GEAR,
                com.soulenchants.loot.CustomLootRegistry.Category.REAGENTS,
                com.soulenchants.loot.CustomLootRegistry.Category.LOOTBOXES };
        int slot = 0;
        for (com.soulenchants.loot.CustomLootRegistry.Category cat : cats) {
            java.util.List<com.soulenchants.loot.CustomLootRegistry.Entry> list =
                    com.soulenchants.loot.CustomLootRegistry.byCategory(cat);
            if (list.isEmpty()) continue;
            for (com.soulenchants.loot.CustomLootRegistry.Entry e : list) {
                if (slot >= 45) break;
                ItemStack icon = e.create();
                ItemMeta m = icon.getItemMeta();
                // Compact tooltip — just enough to identify + act
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + cat.name().toLowerCase().replace('_', ' '));
                lore.add(ChatColor.DARK_GRAY + e.id);
                lore.add("");
                lore.add(ChatColor.YELLOW + "» Click to add");
                m.setLore(lore);
                icon.setItemMeta(m);
                inv.setItem(slot++, icon);
            }
            // Skip to next row break for visual separation between categories
            while (slot % 9 != 0 && slot < 45) slot++;
        }
        inv.setItem(49, button(Material.ARROW, ChatColor.YELLOW + "« Back", ""));
        p.openInventory(inv);
    }

    private void handleLootPicker(Player p, InventoryClickEvent e, int slot) {
        EditCtx c = ctx.get(p.getUniqueId());
        if (c == null || c.mobId == null) return;
        if (slot == 49) {
            if (c.isBoss) openBoss(p, c.mobId); else openMob(p, c.mobId);
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        // Match the icon back to a registry entry by display name
        String wanted = clicked.getItemMeta() != null ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : null;
        if (wanted == null) return;
        com.soulenchants.loot.CustomLootRegistry.Entry match = null;
        for (com.soulenchants.loot.CustomLootRegistry.Entry entry : com.soulenchants.loot.CustomLootRegistry.all()) {
            if (entry.displayName.equals(wanted)) { match = entry; break; }
        }
        if (match == null) {
            p.sendMessage(ChatColor.RED + "Couldn't resolve that item — picker out of sync?");
            return;
        }
        DropSpec d = DropSpec.custom(match.id, 1, 1.0);
        if (c.isBoss) {
            java.util.List<DropSpec> all = plugin.getLootConfig().bossDrops(c.mobId);
            all.add(d);
            plugin.getLootConfig().setBossDrops(c.mobId, all);
            p.sendMessage(ChatColor.GREEN + "✦ Added " + match.displayName + " to " + c.mobId + " boss drops.");
            openBoss(p, c.mobId);
        } else {
            CustomMob cm = MobRegistry.get(c.mobId);
            if (cm == null) return;
            cm.dropSpecs.add(d);
            plugin.getLootConfig().saveMob(cm);
            p.sendMessage(ChatColor.GREEN + "✦ Added " + match.displayName + " to " + c.mobId + " drops.");
            openMob(p, c.mobId);
        }
    }

    // ── tiny item helpers ─────────────────────────────────────────────
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
    private ItemStack header(Material mat, String name, String... lore) { return button(mat, name, lore); }
    private ItemStack bossButton(String id, String name, Material mat) {
        return button(mat, name, "id: " + id, "Click: edit");
    }
    /** Click-adjust stat tile. Left=+1, Right=-1, Shift+Left=+10, Shift+Right=-10, Drop key=set via chat. */
    private ItemStack statTile(Material mat, String label, double value) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        String shown = (value == (int) value) ? String.valueOf((int) value) : String.format("%.2f", value);
        m.setDisplayName(label + ChatColor.GRAY + ": " + ChatColor.WHITE + shown);
        m.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + " ",
                ChatColor.GREEN + "  ▸ Left-click " + ChatColor.GRAY + " +1",
                ChatColor.RED   + "  ▸ Right-click" + ChatColor.GRAY + " -1",
                ChatColor.GREEN + "  ▸ Shift+Left " + ChatColor.GRAY + " +10",
                ChatColor.RED   + "  ▸ Shift+Right" + ChatColor.GRAY + " -10",
                ChatColor.AQUA  + "  ▸ Drop key (Q) " + ChatColor.GRAY + "→ type exact value"
        ));
        it.setItemMeta(m);
        return it;
    }
    private ItemStack statTile(Material mat, String label, int value) { return statTile(mat, label, (double) value); }
    private ItemStack backButton() { return button(Material.ARROW, ChatColor.YELLOW + "« Back", ""); }
}
