package com.soulenchants.pets;

import com.soulenchants.style.MessageStyle;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory for pet egg ItemStacks. Eggs carry four NBT keys:
 *
 *   se_pet_id     — which Pet type this is (registry lookup)
 *   se_pet_level  — current level (1..50)
 *   se_pet_xp     — total xp accumulated
 *   se_pet_uid    — per-instance UUID. Lets us pin state to THIS egg so
 *                    duping via factory doesn't duplicate progress.
 *
 * The visible lore is rebuilt from NBT each time the egg is summoned /
 * despawned / gains xp, so the displayed level always matches the data.
 */
public final class PetItem {

    private PetItem() {}

    /** Build a fresh level-1 egg for the given pet. */
    public static ItemStack fresh(Pet pet) {
        return withState(pet, 1, 0L, UUID.randomUUID());
    }

    /** Rebuild an egg's display lore after state changes (xp gain, level up). */
    public static ItemStack rerender(ItemStack existing, Pet pet) {
        if (existing == null || pet == null) return existing;
        NBTItem n = new NBTItem(existing);
        int level = n.hasKey(PetRegistry.NBT_LEVEL) ? n.getInteger(PetRegistry.NBT_LEVEL) : 1;
        long xp   = n.hasKey(PetRegistry.NBT_XP)    ? n.getLong(PetRegistry.NBT_XP)       : 0L;
        UUID uid  = n.hasKey(PetRegistry.NBT_UID)   ? UUID.fromString(n.getString(PetRegistry.NBT_UID))
                                                    : UUID.randomUUID();
        return withState(pet, level, xp, uid);
    }

    /** Build an egg with a specific level/xp/uid — used on rerender and load. */
    public static ItemStack withState(Pet pet, int level, long xp, UUID uid) {
        ItemStack it = pet.buildEggIcon();
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(pet.getRarityColor() + ChatColor.BOLD + "✦ " + pet.getDisplayName()
                    + " " + MessageStyle.MUTED + "Lv." + MessageStyle.VALUE + level);
            List<String> lore = new ArrayList<>();
            lore.add(MessageStyle.FRAME + "§m                    ");
            lore.add(MessageStyle.MUTED + "Archetype: " + MessageStyle.VALUE + pet.getArchetype());
            lore.add("");
            lore.add(MessageStyle.TIER_EPIC + ChatColor.BOLD + "Passive");
            lore.add(MessageStyle.MUTED + "  " + pet.getPassiveDescription());
            lore.add("");
            lore.add(MessageStyle.TIER_LEGENDARY + ChatColor.BOLD + "Active " + MessageStyle.MUTED + "(Sneak + Right-Click)");
            lore.add(MessageStyle.MUTED + "  " + pet.getActiveDescription());
            lore.add("");
            // Progress bar
            long need  = Pet.xpForLevel(level + 1);
            long floor = Pet.xpForLevel(level);
            long spanNeed = Math.max(1, need - floor);
            long spanHave = Math.max(0, xp - floor);
            int bars = (int) Math.min(20L, (spanHave * 20L) / spanNeed);
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < 20; i++) {
                bar.append(i < bars ? MessageStyle.TIER_EPIC + "|" : MessageStyle.FRAME + "|");
            }
            lore.add(MessageStyle.MUTED + "XP: " + bar.toString() + " "
                    + MessageStyle.VALUE + xp + MessageStyle.MUTED + "/" + need);
            if (!pet.getFlavorLore().isEmpty()) {
                lore.add("");
                for (String line : pet.getFlavorLore()) lore.add(MessageStyle.MUTED + line);
            }
            lore.add("");
            lore.add(MessageStyle.TIER_LEGENDARY + "▸ " + MessageStyle.MUTED + "Right-click to summon / despawn");
            lore.add(MessageStyle.FRAME + "§m                    ");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        NBTItem nbt = new NBTItem(it);
        nbt.setString(PetRegistry.NBT_ID,    pet.getId());
        nbt.setInteger(PetRegistry.NBT_LEVEL, level);
        nbt.setLong(PetRegistry.NBT_XP,       xp);
        nbt.setString(PetRegistry.NBT_UID,    uid.toString());
        return nbt.getItem();
    }

    // ──────────────── NBT accessors ────────────────

    /** NBT-API refuses AIR / amount==0, so every NBTItem construction site
     *  pre-checks. Every accessor below routes through this. */
    private static boolean isValid(ItemStack it) {
        return it != null && it.getType() != org.bukkit.Material.AIR && it.getAmount() > 0;
    }

    public static boolean isPetEgg(ItemStack it) {
        if (!isValid(it)) return false;
        NBTItem n = new NBTItem(it);
        return n.hasKey(PetRegistry.NBT_ID);
    }

    public static String idOf(ItemStack it) {
        if (!isPetEgg(it)) return null;
        return new NBTItem(it).getString(PetRegistry.NBT_ID);
    }

    public static int levelOf(ItemStack it) {
        if (!isPetEgg(it)) return 0;
        NBTItem n = new NBTItem(it);
        return n.hasKey(PetRegistry.NBT_LEVEL) ? n.getInteger(PetRegistry.NBT_LEVEL) : 1;
    }

    public static long xpOf(ItemStack it) {
        if (!isPetEgg(it)) return 0L;
        NBTItem n = new NBTItem(it);
        return n.hasKey(PetRegistry.NBT_XP) ? n.getLong(PetRegistry.NBT_XP) : 0L;
    }

    public static UUID uidOf(ItemStack it) {
        if (!isPetEgg(it)) return null;
        NBTItem n = new NBTItem(it);
        if (!n.hasKey(PetRegistry.NBT_UID)) return null;
        try { return UUID.fromString(n.getString(PetRegistry.NBT_UID)); }
        catch (Exception e) { return null; }
    }
}
