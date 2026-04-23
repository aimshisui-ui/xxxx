package com.soulenchants.mythic;

import com.soulenchants.style.MessageStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A mythic weapon is a named, lore-custom piece of gear whose effect activates
 * based on a ProximityMode. Unlike regular enchants (which require a book
 * application on gear), mythics are one-off named items — you either have
 * Stormbringer, or you don't. The NBT key `se_mythic` holds the mythic id.
 *
 * Implementations override the handlers relevant to their effect. Default
 * impls are no-ops so subclasses stay narrow.
 */
public abstract class MythicWeapon {

    public enum ProximityMode {
        /** Effect fires only when item is in the main hand. */
        HELD,
        /** Effect fires when item is anywhere in the hotbar (slots 0-8) or offhand. */
        HOTBAR,
        /** Broadcast aura — applies effects to everyone within radius of the owner. */
        AURA
    }

    private final String id;
    private final String displayName;
    private final ProximityMode mode;

    protected MythicWeapon(String id, String displayName, ProximityMode mode) {
        this.id = id;
        this.displayName = displayName;
        this.mode = mode;
    }

    public final String getId() { return id; }
    public final String getDisplayName() { return displayName; }
    public final ProximityMode getMode() { return mode; }

    /** Lore lines shown after the mythic badge on the item. Tight, no more than 6 lines. */
    public abstract List<String> getLoreLines();

    /** Called every tick when the mythic is active (per ProximityMode). No-op default. */
    public void onAuraTick(Player owner) {}

    /** Called when the owner attacks with a HELD/HOTBAR mythic. No-op default. */
    public void onAttack(Player owner, EntityDamageByEntityEvent event) {}

    /** Called when the owner takes damage holding a HELD/HOTBAR mythic. No-op default. */
    public void onDefend(Player owner, EntityDamageByEntityEvent event) {}

    /** Called when the owner kills a mob/player while mythic is active. No-op default. */
    public void onKill(Player owner, EntityDeathEvent event) {}

    /** Hook fired whenever an enchant procs on a held mythic — for Crimson Tongue etc. */
    public void onEnchantProc(Player owner, String enchantId, int level) {}

    /** Default 2-line prefix for the item's lore — the mythic badge + tag line. */
    public final List<String> prefixLore() {
        return Arrays.asList(
                MessageStyle.FRAME + MessageStyle.BOLD + "✦ MYTHIC " + MessageStyle.FRAME + "— " +
                        MessageStyle.TIER_SOUL + MessageStyle.BOLD + displayName,
                MessageStyle.FRAME + MessageStyle.ITALIC + mode.name().toLowerCase() + " effect",
                ""
        );
    }

    /** Helper subclasses use to return a single empty list. */
    protected static final List<String> NO_LORE = Collections.emptyList();
}
