package com.soulenchants.masks;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * A cosmetic "mask" — a client-side-only helmet override. The player's real
 * helmet stays equipped (so enchant procs and armor points still apply); the
 * mask only changes what other players see on the player's head.
 *
 * Each mask has an id, display name, and an ItemStack that's the visual.
 * The MaskPacketInjector intercepts helmet equipment packets and substitutes
 * the mask ItemStack if the player has one equipped.
 */
public final class Mask {

    private final String id;
    private final String displayName;
    private final Material visualMaterial;
    private final short data;
    private final String headSkin;   // nullable — base64 skin for SKULL_ITEM masks

    public Mask(String id, String displayName, Material visualMaterial, short data) {
        this(id, displayName, visualMaterial, data, null);
    }

    public Mask(String id, String displayName, Material visualMaterial, short data, String headSkin) {
        this.id = id;
        this.displayName = displayName;
        this.visualMaterial = visualMaterial;
        this.data = data;
        this.headSkin = headSkin;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    /** Build the visual ItemStack that replaces the helmet in outgoing packets. */
    public ItemStack buildVisual() {
        return new ItemStack(visualMaterial, 1, data);
    }

    public String getHeadSkin() { return headSkin; }
}
