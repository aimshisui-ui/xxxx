package com.soulenchants.quests;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Concrete Quest base used when handleEvent is overridden inline.
 * Removes boilerplate from QuestRegistry.
 */
public abstract class SimpleQuest extends Quest {

    private final String activityHint;

    public SimpleQuest(String id, String name, String description, Tier tier,
                       int goal, long soulReward, List<ItemStack> itemRewards,
                       String activityHint) {
        super(id, name, description, tier, goal, soulReward, itemRewards);
        this.activityHint = activityHint;
    }

    @Override
    public String activityHint() { return activityHint; }
}
