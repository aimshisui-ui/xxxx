package com.soulenchants.util;

import com.soulenchants.items.ItemUtil;
import com.soulenchants.mythic.MythicRegistry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;

/**
 * Central "can this debuff land on this victim?" gate. Replaces the old
 * apply-then-strip model (fire the potion effect, then remove it a tick later)
 * with a proper deny: the proc checks the gate before it fires, and if the
 * gate returns true the enchant skips its debuff application entirely.
 *
 * Two layers of immunity:
 *
 *   1. CLARITY III (helmet) — specifically blocks POISON and BLINDNESS at
 *      proc time. Clarity I/II are NOT hard-immune here; they still rely
 *      on the chance-based slow-tick strip in BerserkTickTask (intentional
 *      — lower tiers shouldn't feel identical to the top tier).
 *
 *   2. DAWNBRINGER (mythic, aura mode) — blocks every non-soul debuff proc
 *      when the mythic is in the wielder's main hand OR hotbar. Soul enchants
 *      (Divine Immolation, Nature's Wrath, Soul Burn, Soul Burst) bypass the
 *      gate — they're the "you deserved this" class of effects.
 *
 * Enchants that call in:
 *   • Venom (POISON)            → Clarity + Dawnbringer
 *   • Cripple (SLOW, WEAKNESS)  → Dawnbringer
 *   • Frost Aspect              → Dawnbringer
 *   • Cursed Edge (WITHER)      → Dawnbringer
 *   • Bonebreaker (WEAKNESS)    → Dawnbringer
 *   • Skullcrush                → Dawnbringer
 *   • Hamstring (SLOW)          → Dawnbringer
 *   • Frostshatter              → Dawnbringer
 *   • Rending Blow (WITHER)     → Dawnbringer
 *   • Molten (fire ticks)       → Dawnbringer
 */
public final class DebuffImmunity {

    private DebuffImmunity() {}

    // ──────────────────── Clarity ────────────────────

    /**
     * Is this player's helmet warded against a given potion type?
     * Only Clarity III grants proc-time immunity — I/II fall back to the
     * chance-based slow-tick strip (see BerserkTickTask), so they still
     * visibly see the debuff tick for a moment before it gets cleared.
     */
    public static boolean clarityBlocks(LivingEntity victim, PotionEffectType type) {
        if (!(victim instanceof Player)) return false;
        if (type != PotionEffectType.POISON && type != PotionEffectType.BLINDNESS) return false;
        Player p = (Player) victim;
        ItemStack helmet = p.getInventory().getHelmet();
        if (helmet == null) return false;
        return ItemUtil.getLevel(helmet, "clarity") >= 3;
    }

    // ──────────────────── Dawnbringer aura ────────────────────

    /**
     * Does this player carry the Dawnbringer mythic in a way that should
     * activate its purge aura for themselves? Main hand or hotbar counts —
     * mirrors Dawnbringer's declared AURA proximity mode. Checks both the
     * primary mythic id and the secondary ability slot (ability-slot
     * infusion is legitimate).
     */
    public static boolean hasDawnbringerAura(LivingEntity victim) {
        if (!(victim instanceof Player)) return false;
        Player p = (Player) victim;
        PlayerInventory inv = p.getInventory();
        ItemStack held = inv.getItemInHand();
        if (isDawnbringer(held)) return true;
        for (int i = 0; i <= 8; i++) {
            ItemStack slot = inv.getItem(i);
            if (isDawnbringer(slot)) return true;
        }
        return false;
    }

    private static boolean isDawnbringer(ItemStack it) {
        if (it == null) return false;
        String core    = MythicRegistry.idOf(it);
        String ability = MythicRegistry.abilityIdOf(it);
        return "dawnbringer".equals(core) || "dawnbringer".equals(ability);
    }

    // ──────────────────── Unified gate ────────────────────

    /**
     * Return true if this non-soul debuff proc should be BLOCKED entirely.
     * Callers check this before even rolling the proc chance — no side
     * effects, no strip-later, the debuff simply never touches the victim.
     *
     * If {@code type} is null, this is a generic "any non-soul debuff"
     * check (for enchants like Hamstring/Molten that don't have a single
     * characteristic PotionEffectType). Dawnbringer blocks those too.
     */
    public static boolean isImmuneNonSoul(LivingEntity victim, PotionEffectType type) {
        if (type != null && clarityBlocks(victim, type)) return true;
        if (hasDawnbringerAura(victim)) return true;
        return false;
    }
}
