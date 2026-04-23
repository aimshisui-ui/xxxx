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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A cosmetic + combat mask — v1.4 overhaul. Masks no longer grant free
 * potion effects; instead each mask belongs to a tier (LOW / MID / HIGH)
 * and carries one or more of the following:
 *
 *   auraBoosts         — +1 to an existing potion effect only (no free apply)
 *   outgoingDmgMult    — bonus damage dealt (e.g. 0.05 = +5%)
 *   incomingDmgReduce  — damage-taken reduction (e.g. 0.05 = -5%)
 *   effectImmune       — potion effects the wearer is immune to
 *   enchantImmune      — custom enchant effects to ignore (bleed, wither, etc.)
 *   customAbility      — keyed string triggering bespoke behavior
 *                        (stalker, ironwill, frostguard, soulharvest, phantom)
 *
 * Mask identity is still stored on the helmet itself via se_mask_attached
 * NBT — packet injector / drag-and-drop flow unchanged.
 */
public final class Mask {

    public enum Tier {
        LOW (ChatColor.GRAY,   "Low Tier"),
        MID (ChatColor.AQUA,   "Mid Tier"),
        HIGH(ChatColor.GOLD,   "High Tier");
        public final ChatColor color;
        public final String label;
        Tier(ChatColor color, String label) { this.color = color; this.label = label; }
    }

    private final String id;
    private final String displayName;
    private final Material visualMaterial;
    private final short visualData;
    /** Base64 skin — non-null for custom-textured player heads. */
    private final String headTexture;
    private final List<String> flavorLore;
    private final Tier tier;

    public Mask(String id, String displayName, Material visualMaterial, short visualData,
                String headTexture, List<String> flavorLore, Tier tier) {
        this.id = id;
        this.displayName = displayName;
        this.visualMaterial = visualMaterial;
        this.visualData = visualData;
        this.headTexture = headTexture;
        this.flavorLore = flavorLore == null ? Collections.emptyList() : flavorLore;
        this.tier = tier == null ? Tier.LOW : tier;
    }

    public String   getId()          { return id; }
    public String   getDisplayName() { return displayName; }
    public String   getHeadTexture() { return headTexture; }
    public Tier     getTier()        { return tier; }
    public Material getVisualMaterial() { return visualMaterial; }
    public short    getVisualData()  { return visualData; }

    public ItemStack buildVisual() {
        return new ItemStack(visualMaterial, 1, visualData);
    }

    /**
     * The inventory-item version — player-head with skin texture when
     * available. Lore describes the mask's power block (buffs, immunities,
     * abilities) in addition to flavor text.
     */
    public ItemStack buildInventoryItem() {
        ItemStack it = (headTexture != null)
                ? SkullUtil.skull(headTexture)
                : new ItemStack(visualMaterial, 1, visualData);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tier.color + "" + ChatColor.BOLD + "✦ " + displayName);
            List<String> lore = new ArrayList<>();
            lore.add(tier.color + "" + ChatColor.ITALIC + tier.label + " Mask");
            if (!flavorLore.isEmpty()) {
                lore.add("");
                for (String line : flavorLore) lore.add(MessageStyle.MUTED + "" + ChatColor.ITALIC + line);
            }
            MaskPower power = POWERS.get(id);
            if (power != null && !power.isEmpty()) {
                lore.add("");
                lore.add(MessageStyle.TIER_EPIC + ChatColor.BOLD + "Powers while worn:");
                for (String line : power.describeLore()) lore.add(line);
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
            lore.removeIf(s -> s != null && s.contains("ATTACHED:"));
            lore.add(MessageStyle.MUTED + ChatColor.BOLD.toString() + "ATTACHED: "
                    + tier.color + displayName);
            meta.setLore(lore);
            updated.setItemMeta(meta);
        }
        return updated;
    }

    // ──────────────────────── Power block ────────────────────────

    /**
     * Bundle of all the mechanical effects a mask can carry. Immutable;
     * builder-style construction via the static constants below.
     */
    public static final class MaskPower {
        /** Potion effect types that get +1 IF the wearer already has the effect. */
        public final List<PotionEffectType> auraBoosts;
        /** Multiplier on outgoing damage — e.g. 0.05 = +5%. */
        public final double outgoingDmgMult;
        /** Multiplier on outgoing damage vs Players specifically. */
        public final double outgoingDmgMultVsPlayers;
        /** Reduction on incoming damage — e.g. 0.05 = -5%. */
        public final double incomingDmgReduce;
        /** Reduction that only applies below 50% HP. */
        public final double lowHpIncomingReduce;
        /** Reduction for explosion damage specifically. */
        public final double explosionReduce;
        /** Fire-damage immunity shortcut (sets fire ticks to 0, blocks fire sources). */
        public final boolean fireImmune;
        /** Potion effect types the wearer is immune to (re-stripped every tick). */
        public final Set<PotionEffectType> effectImmune;
        /** Custom enchant effect ids the mask blocks ("bleed", "wither", etc.). */
        public final Set<String> enchantImmune;
        /** Custom ability id — handled by MaskAbilityTask. */
        public final String customAbility;
        /** Human-readable ability description for lore. */
        public final String customAbilityLore;

        private MaskPower(List<PotionEffectType> auraBoosts,
                          double outgoingDmgMult, double outgoingDmgMultVsPlayers,
                          double incomingDmgReduce, double lowHpIncomingReduce,
                          double explosionReduce, boolean fireImmune,
                          Set<PotionEffectType> effectImmune, Set<String> enchantImmune,
                          String customAbility, String customAbilityLore) {
            this.auraBoosts = auraBoosts == null ? Collections.emptyList() : auraBoosts;
            this.outgoingDmgMult = outgoingDmgMult;
            this.outgoingDmgMultVsPlayers = outgoingDmgMultVsPlayers;
            this.incomingDmgReduce = incomingDmgReduce;
            this.lowHpIncomingReduce = lowHpIncomingReduce;
            this.explosionReduce = explosionReduce;
            this.fireImmune = fireImmune;
            this.effectImmune = effectImmune == null ? Collections.emptySet() : effectImmune;
            this.enchantImmune = enchantImmune == null ? Collections.emptySet() : enchantImmune;
            this.customAbility = customAbility;
            this.customAbilityLore = customAbilityLore;
        }

        public boolean isEmpty() {
            return auraBoosts.isEmpty() && outgoingDmgMult == 0 && outgoingDmgMultVsPlayers == 0
                    && incomingDmgReduce == 0 && lowHpIncomingReduce == 0 && explosionReduce == 0
                    && !fireImmune && effectImmune.isEmpty() && enchantImmune.isEmpty()
                    && customAbility == null;
        }

        /** Render lore-ready lines describing this power block. */
        public List<String> describeLore() {
            List<String> out = new ArrayList<>();
            String good = MessageStyle.GOOD;
            String bad  = MessageStyle.BAD;
            String mute = MessageStyle.MUTED;
            for (PotionEffectType t : auraBoosts)
                out.add(good + "  ▸ " + mute + "+1 " + prettyEffect(t) + " " + MessageStyle.FRAME + "(boost only)");
            if (outgoingDmgMult > 0)
                out.add(good + "  ▸ " + mute + "+" + pct(outgoingDmgMult) + "% outgoing damage");
            if (outgoingDmgMultVsPlayers > 0)
                out.add(good + "  ▸ " + mute + "+" + pct(outgoingDmgMultVsPlayers) + "% outgoing damage " + MessageStyle.FRAME + "(vs players)");
            if (incomingDmgReduce > 0)
                out.add(good + "  ▸ " + mute + "-" + pct(incomingDmgReduce) + "% incoming damage");
            if (lowHpIncomingReduce > 0)
                out.add(good + "  ▸ " + mute + "-" + pct(lowHpIncomingReduce) + "% incoming damage " + MessageStyle.FRAME + "(below 50% HP)");
            if (explosionReduce > 0)
                out.add(good + "  ▸ " + mute + "-" + pct(explosionReduce) + "% explosion damage");
            if (fireImmune)
                out.add(good + "  ▸ " + mute + "Immune to fire damage");
            for (PotionEffectType t : effectImmune)
                out.add(bad + "  ▸ " + mute + "Immune to " + prettyEffect(t));
            for (String id : enchantImmune)
                out.add(bad + "  ▸ " + mute + "Immune to " + capitalize(id) + " enchant");
            if (customAbility != null)
                out.add(MessageStyle.TIER_LEGENDARY + "  ✦ " + ChatColor.BOLD + capitalize(customAbility)
                        + " " + MessageStyle.FRAME + "— " + mute + customAbilityLore);
            return out;
        }

        private static int pct(double d) { return (int) Math.round(d * 100); }
    }

    /** Builder so mask registration reads naturally. */
    public static final class PowerBuilder {
        private final List<PotionEffectType> auraBoosts = new ArrayList<>();
        private double outgoingDmgMult, outgoingDmgMultVsPlayers;
        private double incomingDmgReduce, lowHpIncomingReduce, explosionReduce;
        private boolean fireImmune;
        private final Set<PotionEffectType> effectImmune = new HashSet<>();
        private final Set<String> enchantImmune = new HashSet<>();
        private String customAbility;
        private String customAbilityLore;

        public PowerBuilder boost(PotionEffectType t) { auraBoosts.add(t); return this; }
        public PowerBuilder outgoing(double m)        { this.outgoingDmgMult = m; return this; }
        public PowerBuilder outgoingVsPlayers(double m){ this.outgoingDmgMultVsPlayers = m; return this; }
        public PowerBuilder incoming(double m)        { this.incomingDmgReduce = m; return this; }
        public PowerBuilder lowHpIncoming(double m)   { this.lowHpIncomingReduce = m; return this; }
        public PowerBuilder explosion(double m)       { this.explosionReduce = m; return this; }
        public PowerBuilder fireImmune()              { this.fireImmune = true; return this; }
        public PowerBuilder effectImmune(PotionEffectType t) { effectImmune.add(t); return this; }
        public PowerBuilder enchantImmune(String id)  { enchantImmune.add(id); return this; }
        public PowerBuilder ability(String id, String lore) {
            this.customAbility = id; this.customAbilityLore = lore; return this;
        }
        public MaskPower build() {
            return new MaskPower(new ArrayList<>(auraBoosts), outgoingDmgMult, outgoingDmgMultVsPlayers,
                    incomingDmgReduce, lowHpIncomingReduce, explosionReduce, fireImmune,
                    new HashSet<>(effectImmune), new HashSet<>(enchantImmune),
                    customAbility, customAbilityLore);
        }
    }

    private static PowerBuilder power() { return new PowerBuilder(); }

    /** Static power registry — keyed by mask id. */
    private static final Map<String, MaskPower> POWERS = new HashMap<>();
    static {
        // ───── LOW TIER — simple passive buffs ─────
        POWERS.put("pumpkin",       power().incoming(0.03).build());
        POWERS.put("jack",          power().outgoing(0.03).build());
        POWERS.put("zombie_head",   power().boost(PotionEffectType.SATURATION).build());
        POWERS.put("skeleton_head", power().boost(PotionEffectType.SPEED).build());
        POWERS.put("creeper_head",  power().explosion(0.25).build());

        // ───── MID TIER — targeted stats or small abilities ─────
        POWERS.put("duelist_mask",  power().outgoingVsPlayers(0.05).build());
        POWERS.put("battle_scar",   power().lowHpIncoming(0.05).build());
        POWERS.put("hunters_veil",  power().ability("stalker",
                "Crouch & still for 2s → Invisibility until you move or attack").build());
        POWERS.put("witchwood",     power().effectImmune(PotionEffectType.WITHER).build());
        POWERS.put("soulfire_mask", power().fireImmune().boost(PotionEffectType.INCREASE_DAMAGE).build());

        // ───── HIGH TIER — major abilities + stacked stats ─────
        POWERS.put("dragon_head",   power().outgoing(0.08).enchantImmune("bleed")
                .ability("ironwill", "Immune to Bleed — stacks can't be applied to you").build());
        POWERS.put("wither_head",   power().incoming(0.08)
                .effectImmune(PotionEffectType.SLOW).effectImmune(PotionEffectType.SLOW_DIGGING)
                .ability("frostguard", "Immune to Slow and Mining Fatigue potion effects").build());
        POWERS.put("tyrant_crown",  power().ability("soulharvest",
                "On kill · heal 20% of your max HP · 30s cooldown").build());
        POWERS.put("void_mask",     power().outgoing(0.05).incoming(0.05)
                .ability("phantomdash",
                        "While sprinting · 2s Invisibility burst · 10s cooldown").build());
    }

    public static MaskPower powerOf(String maskId) {
        MaskPower p = POWERS.get(maskId);
        return p == null ? EMPTY_POWER : p;
    }
    private static final MaskPower EMPTY_POWER = power().build();

    /** Legacy helper — replaced with a narrower boost-only view. Kept so
     *  callers that still use the Aura type compile; returns aura-boost
     *  entries at amp 0 (they're upgraded to current+1 at tick time). */
    public static final class Aura {
        public final PotionEffectType type;
        public final int amp;
        public Aura(PotionEffectType type, int amp) { this.type = type; this.amp = amp; }
    }
    public static List<Aura> aurasFor(String maskId) {
        MaskPower p = POWERS.get(maskId);
        if (p == null) return Collections.emptyList();
        List<Aura> out = new ArrayList<>();
        for (PotionEffectType t : p.auraBoosts) out.add(new Aura(t, 0));
        return out;
    }

    private static String roman(int n) {
        switch (n) {
            case 1: return "I"; case 2: return "II"; case 3: return "III";
            case 4: return "IV"; case 5: return "V"; default: return String.valueOf(n);
        }
    }

    private static String prettyEffect(PotionEffectType t) {
        if (t == PotionEffectType.INCREASE_DAMAGE)   return "Strength";
        if (t == PotionEffectType.DAMAGE_RESISTANCE) return "Resistance";
        if (t == PotionEffectType.FAST_DIGGING)      return "Haste";
        if (t == PotionEffectType.SLOW_DIGGING)      return "Mining Fatigue";
        if (t == PotionEffectType.JUMP)              return "Jump Boost";
        if (t == PotionEffectType.REGENERATION)      return "Regeneration";
        if (t == PotionEffectType.FIRE_RESISTANCE)   return "Fire Resistance";
        if (t == PotionEffectType.SPEED)             return "Speed";
        if (t == PotionEffectType.NIGHT_VISION)      return "Night Vision";
        if (t == PotionEffectType.WATER_BREATHING)   return "Water Breathing";
        if (t == PotionEffectType.SATURATION)        return "Saturation";
        if (t == PotionEffectType.SLOW)              return "Slowness";
        if (t == PotionEffectType.WITHER)            return "Wither";
        if (t == PotionEffectType.POISON)            return "Poison";
        if (t == PotionEffectType.BLINDNESS)         return "Blindness";
        return t.getName();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ──────────────────────── Roster ────────────────────────

    /** Built-in 14-mask roster. `mhf:<Name>` shorthand routes to SkullUtil
     *  via MHF-account texture lookup; null headTexture falls back to the
     *  vanilla material+data. */
    static List<Mask> defaults() {
        List<Mask> out = new ArrayList<>();

        // LOW tier
        out.add(new Mask("pumpkin",       "Pumpkin Head",      Material.PUMPKIN,        (short) 0, null, null, Tier.LOW));
        out.add(new Mask("jack",          "Jack-o'-Lantern",   Material.JACK_O_LANTERN, (short) 0, null, null, Tier.LOW));
        out.add(new Mask("zombie_head",   "Zombie Veil",       Material.SKULL_ITEM,     (short) 2, null,
                Arrays.asList("Stripped from a wandering horde."), Tier.LOW));
        out.add(new Mask("skeleton_head", "Skeletal Crown",    Material.SKULL_ITEM,     (short) 0, null,
                Arrays.asList("Bleached by seasons and cold nights."), Tier.LOW));
        out.add(new Mask("creeper_head",  "Creeper Mask",      Material.SKULL_ITEM,     (short) 4, null,
                Arrays.asList("Still faintly smoking."), Tier.LOW));

        // MID tier
        out.add(new Mask("duelist_mask",  "Duelist's Mask",    Material.SKULL_ITEM,     (short) 4, null,
                Arrays.asList("The crowd loves a clean strike.",
                              "Worn in ten tournaments. Returned from nine."), Tier.MID));
        out.add(new Mask("battle_scar",   "Battle-Scar",       Material.SKULL_ITEM,     (short) 2, null,
                Arrays.asList("Salt and dried sinew — a reminder.",
                              "You were standing when it stopped."), Tier.MID));
        out.add(new Mask("hunters_veil",  "Hunter's Veil",     Material.PUMPKIN,        (short) 0, null,
                Arrays.asList("For moving through what moves away."), Tier.MID));
        out.add(new Mask("witchwood",     "Witchwood Mask",    Material.SKULL_ITEM,     (short) 0, null,
                Arrays.asList("Carved from a tree that had been asked to grow here."), Tier.MID));
        out.add(new Mask("soulfire_mask", "Soulfire Mask",     Material.JACK_O_LANTERN, (short) 0, null,
                Arrays.asList("Burns on nothing you can name.",
                              "Worn by those who outlive their own stories."), Tier.MID));

        // HIGH tier
        out.add(new Mask("dragon_head",   "Dragon Skull",      Material.SKULL_ITEM,     (short) 5, null,
                Arrays.asList("A predator's patience — rendered in bone."), Tier.HIGH));
        out.add(new Mask("wither_head",   "Wither Skull",      Material.SKULL_ITEM,     (short) 1, null,
                Arrays.asList("Ice sleeps inside the frame."), Tier.HIGH));
        out.add(new Mask("tyrant_crown",  "Tyrant's Crown",    Material.SKULL_ITEM,     (short) 1, null,
                Arrays.asList("Every bone it touched was named."), Tier.HIGH));
        out.add(new Mask("void_mask",     "Void-Walker Mask",  Material.SKULL_ITEM,     (short) 0, null,
                Arrays.asList("The space between steps is the interesting part."), Tier.HIGH));
        return out;
    }
}
