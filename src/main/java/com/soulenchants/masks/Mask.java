package com.soulenchants.masks;

import com.soulenchants.style.MessageStyle;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cosmetic mask — Nordic-style ITEM that players can attach to any
 * helmet. Unlike the v1 per-player state model, the mask's identity is
 * stored on the helmet itself via the se_mask NBT key. That means:
 *
 *   • masks can be traded, sold, stored, dropped on death
 *   • one helmet carries one mask (we don't do multi-masks yet)
 *   • no per-player bookkeeping — packet injector reads the equipped
 *     helmet's NBT each time it rewrites an ENTITY_EQUIPMENT packet
 *
 * Each Mask exposes a visual ItemStack (the one that gets shown over the
 * wearer's head on nearby clients) plus a shop/display ItemStack (the
 * actual player-head item you carry in your inventory).
 */
public final class Mask {

    private final String id;
    private final String displayName;
    private final Material visualMaterial;
    private final short visualData;
    /** Base64 skin — non-null for SKULL_ITEM visuals. */
    private final String headTexture;
    private final List<String> flavorLore;

    public Mask(String id, String displayName, Material visualMaterial, short visualData,
                String headTexture, List<String> flavorLore) {
        this.id = id;
        this.displayName = displayName;
        this.visualMaterial = visualMaterial;
        this.visualData = visualData;
        this.headTexture = headTexture;
        this.flavorLore = flavorLore == null ? java.util.Collections.emptyList() : flavorLore;
    }

    // Simple material mask (pumpkin, jack, basic skull variants)
    public Mask(String id, String displayName, Material mat, short data) {
        this(id, displayName, mat, data, null, null);
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public String getHeadTexture() { return headTexture; }

    /**
     * The ItemStack that the packet injector substitutes into the wearer's
     * helmet slot. No NBT — the client just renders its Material+data.
     */
    public ItemStack buildVisual() {
        return new ItemStack(visualMaterial, 1, visualData);
    }

    /**
     * The inventory-item version — this is what players carry, trade,
     * and drag onto helmets to attach. Carries the se_mask_item NBT
     * so the attach listener can recognise it.
     */
    public ItemStack buildInventoryItem() {
        ItemStack it = (headTexture != null)
                ? SkullUtil.skull(headTexture)
                : new ItemStack(visualMaterial, 1, visualData);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageStyle.TIER_EPIC + ChatColor.BOLD + "✦ " + displayName);
            List<String> lore = new ArrayList<>();
            lore.add(MessageStyle.MUTED + "" + ChatColor.ITALIC + "Cosmetic helmet override + aura.");
            if (!flavorLore.isEmpty()) {
                lore.add("");
                for (String line : flavorLore) lore.add(MessageStyle.MUTED + line);
            }
            List<Aura> auras = aurasFor(id);
            if (!auras.isEmpty()) {
                lore.add("");
                lore.add(MessageStyle.TIER_EPIC + ChatColor.BOLD + "Aura while worn:");
                for (Aura a : auras) {
                    lore.add(MessageStyle.GOOD + "  ▸ " + MessageStyle.VALUE + prettyEffect(a.type)
                            + " " + roman(a.amp + 1));
                }
            }
            lore.add("");
            lore.add(MessageStyle.TIER_LEGENDARY + "▸ " + MessageStyle.MUTED
                    + "Drag onto a helmet to attach");
            lore.add(MessageStyle.TIER_LEGENDARY + "▸ " + MessageStyle.MUTED
                    + "Right-click helmet in inventory to remove");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        de.tr7zw.changeme.nbtapi.NBTItem nbt = new de.tr7zw.changeme.nbtapi.NBTItem(it);
        nbt.setString(MaskRegistry.NBT_MASK_ITEM, id);
        return nbt.getItem();
    }

    /** Apply this mask to a helmet item — writes NBT + appends lore. */
    public ItemStack applyTo(ItemStack helmet) {
        if (helmet == null || helmet.getType() == Material.AIR) return helmet;
        de.tr7zw.changeme.nbtapi.NBTItem nbt = new de.tr7zw.changeme.nbtapi.NBTItem(helmet);
        nbt.setString(MaskRegistry.NBT_MASK_ATTACHED, id);
        ItemStack updated = nbt.getItem();
        ItemMeta meta = updated.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            // Strip any prior attached-line (idempotent — prevents stack-up on repeat attaches)
            lore.removeIf(s -> s != null && s.contains("ATTACHED:"));
            lore.add(MessageStyle.MUTED + ChatColor.BOLD.toString() + "ATTACHED: "
                    + MessageStyle.TIER_EPIC + displayName);
            meta.setLore(lore);
            updated.setItemMeta(meta);
        }
        return updated;
    }

    /**
     * Passive aura: one potion effect applied every tick while the mask is
     * attached to the wearer's helmet. `amp` is zero-indexed (0 = tier I).
     * Nordic-style — masks stop being purely cosmetic and start shaping the
     * loadout: pick for the buff you want, live with the visual.
     */
    public static final class Aura {
        public final PotionEffectType type;
        public final int amp;
        public Aura(PotionEffectType type, int amp) { this.type = type; this.amp = amp; }
    }

    /** Static registry of per-mask aura lists. BerserkTickTask reads these
     *  via {@link #aurasFor(String)} every tick. Empty list means no aura. */
    private static final Map<String, List<Aura>> AURAS = new HashMap<>();
    static {
        // Starter roster — subtle combat flavor over pure cosmetics.
        AURAS.put("pumpkin",       Collections.singletonList(new Aura(PotionEffectType.DAMAGE_RESISTANCE, 0))); // -20%
        AURAS.put("jack",          Arrays.asList(new Aura(PotionEffectType.FIRE_RESISTANCE, 0),
                                                 new Aura(PotionEffectType.SPEED, 0)));                         // +speed +fireproof
        AURAS.put("dragon_head",   Collections.singletonList(new Aura(PotionEffectType.INCREASE_DAMAGE, 0)));    // +3 dmg
        AURAS.put("wither_head",   Arrays.asList(new Aura(PotionEffectType.NIGHT_VISION, 0),
                                                 new Aura(PotionEffectType.DAMAGE_RESISTANCE, 0)));
        // Regen was dropped from mask auras — free HP-over-time is too strong
        // stacked with Implants/Soul Warden. Replaced with Saturation (slows
        // hunger drain) + Haste / Speed / Strength at low amps so masks still
        // feel earned without healing on top of god-tier defensive enchants.
        AURAS.put("zombie_head",   Collections.singletonList(new Aura(PotionEffectType.SATURATION, 0)));
        AURAS.put("skeleton_head", Collections.singletonList(new Aura(PotionEffectType.SPEED, 1)));
        AURAS.put("creeper_head",  Arrays.asList(new Aura(PotionEffectType.JUMP, 1),
                                                 new Aura(PotionEffectType.SPEED, 0)));
        // v1.2 roster — tuned buffs.
        AURAS.put("duelist_mask",  Collections.singletonList(new Aura(PotionEffectType.SPEED, 2)));             // Speed III
        AURAS.put("tyrant_crown",  Collections.singletonList(new Aura(PotionEffectType.INCREASE_DAMAGE, 1)));   // Strength II
        AURAS.put("battle_scar",   Arrays.asList(new Aura(PotionEffectType.DAMAGE_RESISTANCE, 0),
                                                 new Aura(PotionEffectType.SATURATION, 0)));
        AURAS.put("hunters_veil",  Arrays.asList(new Aura(PotionEffectType.SPEED, 1),
                                                 new Aura(PotionEffectType.FAST_DIGGING, 0)));
        AURAS.put("witchwood",     Arrays.asList(new Aura(PotionEffectType.FIRE_RESISTANCE, 0),
                                                 new Aura(PotionEffectType.NIGHT_VISION, 0)));
        AURAS.put("soulfire_mask", Arrays.asList(new Aura(PotionEffectType.INCREASE_DAMAGE, 0),
                                                 new Aura(PotionEffectType.FIRE_RESISTANCE, 0)));
    }

    /** Lookup helper — returns the mask's passive aura list, never null. */
    public static List<Aura> aurasFor(String maskId) {
        List<Aura> out = AURAS.get(maskId);
        return out == null ? Collections.<Aura>emptyList() : out;
    }

    private static String roman(int n) {
        switch (n) {
            case 1: return "I"; case 2: return "II"; case 3: return "III";
            case 4: return "IV"; case 5: return "V"; default: return String.valueOf(n);
        }
    }

    /** Map a PotionEffectType to a human-readable name for lore display. */
    private static String prettyEffect(PotionEffectType t) {
        if (t == PotionEffectType.INCREASE_DAMAGE)     return "Strength";
        if (t == PotionEffectType.DAMAGE_RESISTANCE)   return "Resistance";
        if (t == PotionEffectType.FAST_DIGGING)        return "Haste";
        if (t == PotionEffectType.SLOW_DIGGING)        return "Mining Fatigue";
        if (t == PotionEffectType.JUMP)                return "Jump Boost";
        if (t == PotionEffectType.REGENERATION)        return "Regeneration";
        if (t == PotionEffectType.FIRE_RESISTANCE)     return "Fire Resistance";
        if (t == PotionEffectType.SPEED)               return "Speed";
        if (t == PotionEffectType.NIGHT_VISION)        return "Night Vision";
        if (t == PotionEffectType.WATER_BREATHING)     return "Water Breathing";
        if (t == PotionEffectType.SATURATION)          return "Saturation";
        return t.getName();
    }

    /** Build the starter roster — called from MaskRegistry.registerDefaults(). */
    static List<Mask> defaults() {
        List<Mask> out = new ArrayList<>();
        out.add(new Mask("pumpkin",       "Pumpkin Head",      Material.PUMPKIN,        (short) 0));
        out.add(new Mask("jack",          "Jack-o'-Lantern",   Material.JACK_O_LANTERN, (short) 0));
        out.add(new Mask("dragon_head",   "Dragon Skull",      Material.SKULL_ITEM,     (short) 5));
        out.add(new Mask("wither_head",   "Wither Skull",      Material.SKULL_ITEM,     (short) 1));
        out.add(new Mask("zombie_head",   "Zombie Veil",       Material.SKULL_ITEM,     (short) 2));
        out.add(new Mask("skeleton_head", "Skeletal Crown",    Material.SKULL_ITEM,     (short) 0));
        out.add(new Mask("creeper_head",  "Creeper Mask",      Material.SKULL_ITEM,     (short) 4));
        // v1.2 roster — six new cosmetic masks themed for PvP + PvE variety.
        // Visuals reuse skull/pumpkin materials (1.8 only supports helmet/skull/
        // pumpkin in the helmet slot); identity is carried by the item's name +
        // lore so collectors still see them as distinct trophies.
        out.add(new Mask("duelist_mask",   "Duelist's Mask",      Material.SKULL_ITEM,     (short) 4,  null,
                Arrays.asList("The crowd loves a clean strike.", "Worn in ten tournaments. Returned from nine.")));
        out.add(new Mask("tyrant_crown",   "Tyrant's Crown",      Material.SKULL_ITEM,     (short) 1,  null,
                Arrays.asList("Every bone it touched was named.")));
        out.add(new Mask("battle_scar",    "Battle-Scar",         Material.SKULL_ITEM,     (short) 2,  null,
                Arrays.asList("Salt and dried sinew — a reminder.", "You were standing when it stopped.")));
        out.add(new Mask("hunters_veil",   "Hunter's Veil",       Material.PUMPKIN,        (short) 0,  null,
                Arrays.asList("For moving through what moves away.")));
        out.add(new Mask("witchwood",      "Witchwood Mask",      Material.SKULL_ITEM,     (short) 0,  null,
                Arrays.asList("Carved from a tree that had been asked to grow here.")));
        out.add(new Mask("soulfire_mask",  "Soulfire Mask",       Material.JACK_O_LANTERN, (short) 0,  null,
                Arrays.asList("Burns on nothing you can name.", "Worn by those who outlive their own stories.")));
        return out;
    }
}
