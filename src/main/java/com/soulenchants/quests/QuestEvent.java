package com.soulenchants.quests;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

/**
 * Generic event payload sent to {@link Quest#handleEvent} for progress
 * tracking. Listeners populate the relevant fields and leave others null.
 */
public class QuestEvent {

    public enum Kind {
        MOB_KILL,
        BLOCK_BREAK,
        BOOK_APPLIED,
        SHOP_PURCHASE,
        COMMAND_RAN,
        DAMAGE_TAKEN,
        DAMAGE_DEALT,
        BOSS_KILLED
    }

    public final Kind kind;
    public final Entity entity;        // may be null
    public final ItemStack item;       // may be null
    public final String stringPayload; // command name, biome name, etc.
    public final double numeric;        // damage amount, etc.

    private QuestEvent(Kind kind, Entity entity, ItemStack item, String stringPayload, double numeric) {
        this.kind = kind;
        this.entity = entity;
        this.item = item;
        this.stringPayload = stringPayload;
        this.numeric = numeric;
    }

    public static QuestEvent mobKill(Entity mob)             { return new QuestEvent(Kind.MOB_KILL, mob, null, null, 0); }
    public static QuestEvent bossKilled(Entity boss)         { return new QuestEvent(Kind.BOSS_KILLED, boss, null, null, 0); }
    public static QuestEvent blockBreak(String materialName) { return new QuestEvent(Kind.BLOCK_BREAK, null, null, materialName, 0); }
    public static QuestEvent bookApplied(ItemStack book)     { return new QuestEvent(Kind.BOOK_APPLIED, null, book, null, 0); }
    public static QuestEvent shopPurchase(long souls)        { return new QuestEvent(Kind.SHOP_PURCHASE, null, null, null, souls); }
    public static QuestEvent commandRan(String command)      { return new QuestEvent(Kind.COMMAND_RAN, null, null, command, 0); }
    public static QuestEvent damageDealt(Entity target, double amount) { return new QuestEvent(Kind.DAMAGE_DEALT, target, null, null, amount); }
}
