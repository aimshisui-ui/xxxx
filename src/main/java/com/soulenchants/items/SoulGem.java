package com.soulenchants.items;

import com.soulenchants.style.MessageStyle;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Soul Gem — portable soul container, a.k.a. the "license" required to use
 * soul-tier enchants. Hybrid design:
 *
 *   • Carries its own soul balance in NBT (unlike ledger souls which live in
 *     souls.yml).
 *   • Right-click to deposit back into the ledger.
 *   • Each gem is unique (amount=1 ItemStack) with a random UUID, so two
 *     gems never auto-stack by vanilla rules — merging is done by the
 *     SoulGemListener on inventory click (anvil-combine feel).
 *   • When a soul-enchant procs, SoulGemUtil.chargeSoulCost() debits the
 *     held/first-found gem first and falls back to the ledger only if the
 *     gem runs short. If the player has no gem at all, the proc is blocked
 *     entirely (license gate).
 */
public final class SoulGem {

    public static final String NBT_TYPE   = "se_soul_gem";     // marker — true
    public static final String NBT_AMOUNT = "se_soul_gem_amt"; // long
    public static final String NBT_UUID   = "se_soul_gem_uid"; // string — dedupe / merge identity

    private static final String DIVIDER =
            MessageStyle.FRAME + "" + ChatColor.STRIKETHROUGH + "                                  ";

    private SoulGem() {}

    /** Build a fresh Soul Gem carrying the given soul balance. */
    public static ItemStack create(long amount) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(buildDisplayName(amount));
            meta.setLore(buildLore(amount));
            item.setItemMeta(meta);
        }
        NBTItem nbt = new NBTItem(item);
        nbt.setBoolean(NBT_TYPE, true);
        nbt.setLong(NBT_AMOUNT, amount);
        nbt.setString(NBT_UUID, UUID.randomUUID().toString());
        return nbt.getItem();
    }

    /** Is this ItemStack a Soul Gem? */
    public static boolean isGem(ItemStack item) {
        if (item == null || item.getType() != Material.EMERALD) return false;
        NBTItem nbt = new NBTItem(item);
        return nbt.hasKey(NBT_TYPE) && nbt.getBoolean(NBT_TYPE);
    }

    /** Read the soul amount carried by this gem, or 0 if not a gem. */
    public static long amount(ItemStack item) {
        if (!isGem(item)) return 0;
        return new NBTItem(item).getLong(NBT_AMOUNT);
    }

    /**
     * Return a new ItemStack with the amount set to {@code amount}, lore +
     * display name refreshed. Leaves the NBT UUID intact so inventory-listener
     * merge logic can still identify the stack.
     */
    public static ItemStack withAmount(ItemStack item, long amount) {
        if (!isGem(item)) return item;
        NBTItem nbt = new NBTItem(item);
        nbt.setLong(NBT_AMOUNT, amount);
        ItemStack updated = nbt.getItem();
        ItemMeta meta = updated.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(buildDisplayName(amount));
            meta.setLore(buildLore(amount));
            updated.setItemMeta(meta);
        }
        return updated;
    }

    /** Nordic-style colour tiering by amount — the bigger the gem, the hotter it reads. */
    private static String buildDisplayName(long amount) {
        String col;
        if (amount >= 500_000)      col = ChatColor.DARK_RED + "" + ChatColor.BOLD;
        else if (amount >= 100_000) col = ChatColor.RED + "" + ChatColor.BOLD;
        else if (amount >= 10_000)  col = ChatColor.GOLD + "" + ChatColor.BOLD;
        else if (amount >= 1_000)   col = ChatColor.AQUA + "" + ChatColor.BOLD;
        else if (amount >= 100)     col = ChatColor.GREEN + "" + ChatColor.BOLD;
        else                        col = ChatColor.WHITE + "" + ChatColor.BOLD;
        return col + "❖ Soul Gem " + MessageStyle.FRAME + "(" + MessageStyle.VALUE + formatNum(amount) + MessageStyle.FRAME + ")";
    }

    private static List<String> buildLore(long amount) {
        return Arrays.asList(
                DIVIDER,
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "A crystallised fragment of soul-ledger,",
                MessageStyle.MUTED + "" + MessageStyle.ITALIC + "carried into battle as living ammunition.",
                "",
                MessageStyle.TIER_SOUL + "▸ " + MessageStyle.MUTED + "Required to use "
                        + MessageStyle.TIER_SOUL + "Soul Enchants",
                MessageStyle.GOOD    + "▸ " + MessageStyle.MUTED + "Balance: "
                        + MessageStyle.VALUE + formatNum(amount) + MessageStyle.MUTED + " souls",
                MessageStyle.TIER_LEGENDARY + "▸ " + MessageStyle.MUTED + "Every soul-enchant proc "
                        + MessageStyle.VALUE + "drains this gem",
                "",
                MessageStyle.BAD + "" + MessageStyle.BOLD + "⚠ WARNING",
                MessageStyle.BAD + "Souls withdrawn from the Soul Bank",
                MessageStyle.BAD + "cannot be deposited back. Once minted,",
                MessageStyle.BAD + "the gem is one-way ammunition — spend it",
                MessageStyle.BAD + "or lose it.",
                "",
                MessageStyle.TIER_LEGENDARY + "" + MessageStyle.ITALIC + "▶ Drop another gem on top to merge",
                DIVIDER
        );
    }

    /** 12,500 -> "12,500" — compact comma-grouping for display. */
    public static String formatNum(long n) {
        return String.format(java.util.Locale.US, "%,d", n);
    }
}
