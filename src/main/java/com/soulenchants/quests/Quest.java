package com.soulenchants.quests;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Quest definition. Quests track a single integer progress counter against
 * a goal value. When the counter hits the goal, the player can claim
 * rewards (souls + optional item).
 *
 * Implementations override {@link #handleEvent} for whichever in-game event
 * type advances them (mob kill, ore mined, biome visited, etc.).
 */
public abstract class Quest {

    public enum Tier { TUTORIAL, DAILY, WEEKLY, STORY }

    public final String id;
    public final String name;
    public final String description;
    public final Tier tier;
    public final int goal;
    public final long soulReward;
    public final List<ItemStack> itemRewards; // can be empty

    public Quest(String id, String name, String description, Tier tier,
                 int goal, long soulReward, List<ItemStack> itemRewards) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tier = tier;
        this.goal = goal;
        this.soulReward = soulReward;
        this.itemRewards = itemRewards;
    }

    /** Subclasses inspect the event and bump progress as appropriate. */
    public abstract int handleEvent(Player p, QuestEvent event, int currentProgress);

    public boolean isComplete(int progress) { return progress >= goal; }

    /** Tag describing what kind of action this quest watches — for UI hints. */
    public abstract String activityHint();
}
