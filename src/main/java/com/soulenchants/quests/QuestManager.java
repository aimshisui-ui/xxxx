package com.soulenchants.quests;

import com.soulenchants.SoulEnchants;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Routes events to active quests, advances progress, and reports completion
 * to the player. Tutorial chain is sequenced: only the player's current
 * tutorial step receives events.
 */
public class QuestManager {

    private final SoulEnchants plugin;
    private final QuestProfile profile;

    public QuestManager(SoulEnchants plugin) {
        this.plugin = plugin;
        this.profile = new QuestProfile(plugin.getDataFolder());
    }

    public QuestProfile getProfile() { return profile; }

    public void onEvent(Player p, QuestEvent e) {
        UUID id = p.getUniqueId();

        // ── Tutorial step (only one active at a time) ──
        int step = profile.getTutorialStep(id);
        List<Quest> tut = QuestRegistry.tutorialChain();
        if (step < tut.size()) {
            Quest q = tut.get(step);
            int cur = profile.getProgress(id, q.id);
            int next = q.handleEvent(p, e, cur);
            if (next != cur) {
                profile.setProgress(id, q.id, next);
                if (q.isComplete(next) && !profile.isClaimed(id, q.id)) {
                    completeTutorialStep(p, q, step);
                } else {
                    notifyProgress(p, q, next);
                }
            }
        }

        // ── Active dailies ──
        List<String> active = profile.getActiveDailies(id);
        for (String qid : active) {
            Quest q = QuestRegistry.get(qid);
            if (q == null) continue;
            if (profile.isClaimed(id, qid)) continue;
            int cur = profile.getProgress(id, qid);
            if (q.isComplete(cur)) continue;
            int next = q.handleEvent(p, e, cur);
            if (next != cur) {
                profile.setProgress(id, qid, next);
                if (q.isComplete(next)) {
                    p.sendMessage(ChatColor.GREEN + "✦ Quest complete: " + ChatColor.WHITE + q.name
                            + ChatColor.GRAY + " — open /quests to claim");
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 1f, 1.4f);
                } else {
                    notifyProgress(p, q, next);
                }
            }
        }
    }

    private void completeTutorialStep(Player p, Quest q, int stepIdx) {
        profile.markClaimed(p.getUniqueId(), q.id);
        // Advance to next tutorial step
        profile.setTutorialStep(p.getUniqueId(), stepIdx + 1);
        plugin.getSoulManager().add(p, q.soulReward);
        for (ItemStack it : q.itemRewards) {
            java.util.Map<Integer, ItemStack> leftover = p.getInventory().addItem(it);
            for (ItemStack lo : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), lo);
        }
        p.sendMessage("");
        p.sendMessage(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.BOLD + "TUTORIAL COMPLETE: " + q.name);
        p.sendMessage(ChatColor.GRAY + "  +" + q.soulReward + " souls" + (q.itemRewards.isEmpty() ? "" : " + items"));
        if (stepIdx + 1 < QuestRegistry.tutorialChain().size()) {
            Quest next = QuestRegistry.tutorialChain().get(stepIdx + 1);
            p.sendMessage(ChatColor.GRAY + "  Next: " + ChatColor.WHITE + next.name + ChatColor.GRAY + " — " + next.description);
        } else {
            p.sendMessage(ChatColor.GOLD + "  All tutorials done! Daily quests unlocked. Use /quests to see them.");
        }
        p.sendMessage("");
        p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 0.5f, 1.5f);
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.5f, 1.2f);
    }

    /** Roll a fresh set of 3 dailies if today's haven't been rolled. */
    public List<Quest> ensureDailies(Player p) {
        UUID id = p.getUniqueId();
        List<String> active = profile.getActiveDailies(id);
        if (!active.isEmpty()) return resolve(active);
        long lifetime = plugin.getSoulManager().getLifetime(p);
        List<Quest> picks = QuestRegistry.rollDailies(lifetime);
        List<String> ids = new ArrayList<>();
        for (Quest q : picks) ids.add(q.id);
        profile.rollDailies(id, ids);
        return picks;
    }

    private List<Quest> resolve(List<String> ids) {
        List<Quest> out = new ArrayList<>();
        for (String id : ids) {
            Quest q = QuestRegistry.get(id);
            if (q != null) out.add(q);
        }
        return out;
    }

    public boolean claim(Player p, String questId) {
        UUID id = p.getUniqueId();
        Quest q = QuestRegistry.get(questId);
        if (q == null) return false;
        int cur = profile.getProgress(id, questId);
        if (!q.isComplete(cur)) return false;
        if (profile.isClaimed(id, questId)) return false;
        profile.markClaimed(id, questId);
        plugin.getSoulManager().add(p, q.soulReward);
        for (ItemStack it : q.itemRewards) {
            java.util.Map<Integer, ItemStack> leftover = p.getInventory().addItem(it);
            for (ItemStack lo : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), lo);
        }
        p.sendMessage(ChatColor.GREEN + "✦ Claimed " + ChatColor.WHITE + q.name
                + ChatColor.GREEN + " — " + ChatColor.YELLOW + "+" + q.soulReward + " souls");
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1f, 1.4f);
        return true;
    }

    private void notifyProgress(Player p, Quest q, int progress) {
        // Soft progress notification — only on milestones to avoid chat spam
        if (q.goal <= 5) return; // small quests skip mid-progress msgs
        if (progress == q.goal / 2 || progress == q.goal - 1) {
            p.sendMessage(ChatColor.GRAY + "  ▸ " + q.name + ": " + progress + "/" + q.goal);
        }
    }
}
