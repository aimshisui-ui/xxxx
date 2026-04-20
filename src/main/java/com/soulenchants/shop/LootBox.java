package com.soulenchants.shop;

import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.enchants.EnchantTier;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.loot.BossLootItems;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Loot box item factories + open-roll logic.
 *
 *   Bronze  — rolls Common / Uncommon
 *   Silver  — Uncommon / Rare
 *   Gold    — Rare / Epic + rare Legendary
 *   Boss    — Epic / Legendary + chance at Boss-tier named item
 *
 * Right-click consumes the box and hands the rolled item. Uses NBT `se_lootbox`.
 */
public final class LootBox {

    public static final String NBT_LOOTBOX = "se_lootbox";
    private static final Random RNG = new Random();

    public enum Kind {
        BRONZE(ChatColor.WHITE, "Bronze"),
        SILVER(ChatColor.GRAY, "Silver"),
        GOLD(ChatColor.YELLOW, "Gold"),
        BOSS(ChatColor.DARK_BLUE, "Boss");

        public final ChatColor color;
        public final String label;
        Kind(ChatColor color, String label) { this.color = color; this.label = label; }
    }

    private LootBox() {}

    public static ItemStack item(Kind kind) {
        Material mat;
        switch (kind) {
            case BRONZE: mat = Material.CHEST;        break;
            case SILVER: mat = Material.TRAPPED_CHEST;break;
            case GOLD:   mat = Material.ENDER_CHEST;  break;
            default:     mat = Material.BEACON;       break;
        }
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(kind.color + "" + ChatColor.BOLD + kind.label + " Loot Box");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  ");
        switch (kind) {
            case BRONZE:
                lore.addAll(Arrays.asList(
                        ChatColor.GRAY + "" + ChatColor.ITALIC + "Right-click to open.",
                        "",
                        ChatColor.GRAY + "» Rolls: Common (80%), Uncommon (20%)",
                        ChatColor.GRAY + "» Chance at a reagent stack"));
                break;
            case SILVER:
                lore.addAll(Arrays.asList(
                        ChatColor.GRAY + "" + ChatColor.ITALIC + "Right-click to open.",
                        "",
                        ChatColor.GRAY + "» Rolls: Uncommon (60%), Rare (35%), Epic (5%)",
                        ChatColor.GRAY + "» Guaranteed reagent"));
                break;
            case GOLD:
                lore.addAll(Arrays.asList(
                        ChatColor.GRAY + "" + ChatColor.ITALIC + "Right-click to open.",
                        "",
                        ChatColor.GRAY + "» Rolls: Rare (45%), Epic (45%), Legendary (10%)",
                        ChatColor.GRAY + "» Bonus epic reagent"));
                break;
            case BOSS:
                lore.addAll(Arrays.asList(
                        ChatColor.DARK_BLUE + "" + ChatColor.ITALIC + "Right-click to open.",
                        "",
                        ChatColor.GRAY + "» Rolls: Epic (60%), Legendary (35%)",
                        ChatColor.DARK_BLUE + "» 5% chance: Boss-tier item"));
                break;
        }
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                  ");
        m.setLore(lore);
        it.setItemMeta(m);
        NBTItem nbt = new NBTItem(it);
        nbt.setString(NBT_LOOTBOX, kind.name());
        return nbt.getItem();
    }

    public static Kind kindOf(ItemStack item) {
        if (item == null) return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(NBT_LOOTBOX)) return null;
        try { return Kind.valueOf(nbt.getString(NBT_LOOTBOX)); } catch (Exception e) { return null; }
    }

    /** Roll the box and return the item(s) to give. Never empty. */
    public static List<ItemStack> roll(Kind kind) {
        List<ItemStack> out = new ArrayList<>();
        switch (kind) {
            case BRONZE:
                out.add(rollBook(RNG.nextDouble() < 0.80 ? EnchantTier.COMMON : EnchantTier.UNCOMMON));
                if (RNG.nextDouble() < 0.40) out.add(randomReagent(false));
                break;
            case SILVER: {
                double r = RNG.nextDouble();
                EnchantTier t = r < 0.60 ? EnchantTier.UNCOMMON : r < 0.95 ? EnchantTier.RARE : EnchantTier.EPIC;
                out.add(rollBook(t));
                out.add(randomReagent(false));
                break;
            }
            case GOLD: {
                double r = RNG.nextDouble();
                EnchantTier t = r < 0.45 ? EnchantTier.RARE : r < 0.90 ? EnchantTier.EPIC : EnchantTier.LEGENDARY;
                out.add(rollBook(t));
                out.add(randomReagent(true));
                break;
            }
            case BOSS: {
                double r = RNG.nextDouble();
                if (r < 0.05) {
                    // Boss-tier named item
                    out.add(rollBossTier());
                } else {
                    EnchantTier t = r < 0.65 ? EnchantTier.EPIC : EnchantTier.LEGENDARY;
                    out.add(rollBook(t));
                }
                out.add(randomReagent(true));
                break;
            }
        }
        return out;
    }

    private static ItemStack rollBook(EnchantTier tier) {
        List<CustomEnchant> pool = new ArrayList<>();
        for (CustomEnchant e : EnchantRegistry.all()) if (e.getTier() == tier) pool.add(e);
        if (pool.isEmpty()) return new ItemStack(Material.PAPER);
        CustomEnchant chosen = pool.get(RNG.nextInt(pool.size()));
        int lvl = 1 + RNG.nextInt(chosen.getMaxLevel());
        return ItemFactories.book(chosen, lvl);
    }

    private static ItemStack randomReagent(boolean epic) {
        if (epic) {
            switch (RNG.nextInt(6)) {
                case 0: return BossLootItems.bulwarkCore();
                case 1: return BossLootItems.veilEssence();
                case 2: return BossLootItems.reinforcedPlating(4);
                case 3: return BossLootItems.phantomSilk(4);
                case 4: return BossLootItems.forgedEmber(4);
                default:return BossLootItems.echoingStrand(4);
            }
        } else {
            switch (RNG.nextInt(6)) {
                case 0: return BossLootItems.colossusSlag(4);
                case 1: return BossLootItems.veilThread(4);
                case 2: return BossLootItems.ironHeartFragment(4);
                case 3: return BossLootItems.frayedSoul(4);
                case 4: return BossLootItems.forgedEmber(2);
                default:return BossLootItems.echoingStrand(2);
            }
        }
    }

    private static ItemStack rollBossTier() {
        switch (RNG.nextInt(7)) {
            case 0: return BossLootItems.ironheartsHammer();
            case 1: return BossLootItems.colossusPlatingCore();
            case 2: return BossLootItems.veilseekersMantle();
            case 3: return BossLootItems.loomOfEternity();
            case 4: return BossLootItems.heartOfTheForge();
            case 5: return BossLootItems.veilSigil();
            default:return BossLootItems.apexCarapace();
        }
    }
}
