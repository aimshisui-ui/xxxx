package com.soulenchants.listeners;

import com.soulenchants.mythic.MythicRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Replaces the default "Alice was slain by Bob" death message with a
 * flavor line keyed to the mythic weapon the killer was wielding.
 *
 *   Flow:
 *     PlayerDeathEvent fires → resolve killer.getItemInHand() → match
 *     mythic NBT id → pick a random line from the pool → overwrite
 *     e.setDeathMessage(...).
 *
 * Non-mythic kills keep Bukkit's default message. {victim} + {killer}
 * are the two substitution tokens each line can reference.
 */
public final class MythicDeathMessages implements Listener {

    /** Per-mythic death-line pool. Each line can use {victim} / {killer}. */
    private static final Map<String, List<String>> POOL = new HashMap<>();
    static {
        // PvP mythics — Crimson Tongue (bleed-on-hit sword)
        POOL.put("crimson_tongue", Arrays.asList(
                "§c{victim} §7was §cbled dry §7by §c{killer}§7's Crimson Tongue.",
                "§c{victim}§7's veins sang for §c{killer}§7.",
                "§c{victim} §7learned why §c{killer}§7 wields Crimson Tongue.",
                "§c{victim} §7was §cdrained §7— §c{killer}§7's blade drinks.",
                "§c{killer}§7 let the Crimson Tongue taste §c{victim}§7."
        ));
        // Wraithcleaver (AoE cleave axe)
        POOL.put("wraithcleaver", Arrays.asList(
                "§5{victim} §7was §5sundered §7by §5{killer}§7's Wraithcleaver.",
                "§5{killer}§7 split §5{victim} §7between worlds.",
                "§5{victim} §7was §5cleaved clean §7— §5{killer}§7 swung once.",
                "§5{victim}§7's soul wasn't the only thing §5{killer}§7 severed.",
                "§5{killer} §7reminded §5{victim} §7why ghosts avoid the axe."
        ));
        // Stormbringer (lightning sword)
        POOL.put("stormbringer", Arrays.asList(
                "§b{victim} §7was §bstruck from above §7by §b{killer}§7's Stormbringer.",
                "§b{killer}§7 called the sky down on §b{victim}§7.",
                "§b{victim}§7 caught §b{killer}§7's thunder.",
                "§b{victim}§7 died §bmid-flash§7 — §b{killer}§7 never paused.",
                "§bThe storm answered §b{killer}§7. §b{victim}§7 didn't."
        ));
        // Voidreaver (void / dark-magic sword)
        POOL.put("voidreaver", Arrays.asList(
                "§5{victim} §7was §5unmade §7by §5{killer}§7's Voidreaver.",
                "§5{killer}§7 fed §5{victim} §7to the Void.",
                "§5{victim}§7 was §5erased §7— §5{killer}§7 didn't notice.",
                "§5{victim}§7 crossed §5{killer}§7 and then crossed nothing at all.",
                "§5The Void remembers §5{victim}§7. §5{killer}§7 does not."
        ));
        // Dawnbringer (holy / sun-themed sword)
        POOL.put("dawnbringer", Arrays.asList(
                "§e{victim} §7was §ejudged §7by §e{killer}§7's Dawnbringer.",
                "§e{killer}§7 brought the morning to §e{victim}§7.",
                "§e{victim}§7 did not survive §e{killer}§7's first light.",
                "§e{killer}§7 unsheathed the Dawn. §e{victim}§7 blinked.",
                "§e{victim} §7was §eblinded by dawn §7before being taken by it."
        ));
        // Sunderer (armor-break axe)
        POOL.put("sunderer", Arrays.asList(
                "§6{victim}§7's armor failed them against §6{killer}§7's Sunderer.",
                "§6{killer}§7 peeled §6{victim} §7out of their own kit.",
                "§6{victim}§7 was §6sundered§7 — plate, then bone.",
                "§6{killer}§7 swung once. §6{victim}§7's armor swung with them.",
                "§6{victim}§7 learned why Sunderer breaks contracts."
        ));
        // Phoenix Feather (fire-on-kill sword)
        POOL.put("phoenix_feather", Arrays.asList(
                "§c{victim} §7was §cignited §7by §c{killer}§7's Phoenix Feather.",
                "§c{killer}§7 turned §c{victim} §7to ash, then into feathers.",
                "§c{victim}§7 flew too close to §c{killer}§7.",
                "§c{victim}§7 burned §cout§7 — §c{killer}§7 walked away glowing.",
                "§cThe Phoenix Feather claimed §c{victim}§7. §c{killer}§7 just held it."
        ));
        // Soulbinder (bow that steals souls)
        POOL.put("soulbinder", Arrays.asList(
                "§5{victim}§7's soul answered §5{killer}§7's Soulbinder.",
                "§5{killer}§7 loosed an arrow. §5{victim}§7 loosed a soul.",
                "§5{victim}§7 was §5bound§7 — §5{killer}§7 already moved on.",
                "§5{killer}§7 tied §5{victim}§7 to a string of arrows.",
                "§5{victim}§7 gave §5{killer}§7 their final breath and their soul."
        ));
        // Tidecaller (fishing-rod flavor mythic)
        POOL.put("tidecaller", Arrays.asList(
                "§3{victim} §7was §3drowned §7by §3{killer}§7's Tidecaller.",
                "§3{killer}§7 reeled §3{victim} §7into the deep.",
                "§3{victim}§7 felt the tide rise. §3{killer}§7 felt nothing.",
                "§3{killer}§7 hooked §3{victim}§7 and the sea did the rest.",
                "§3{victim}§7 was §3swallowed by water §7only §3{killer}§7 could see."
        ));
        // v1.2 PvE mythics — still fire on PvP kills
        POOL.put("graverend", Arrays.asList(
                "§8{victim} §7was §8rent asunder §7by §8{killer}§7's Graverend.",
                "§8{killer}§7 opened a grave with §8{victim}§7 in it.",
                "§8{victim}§7 was §8lowered§7 — §8{killer}§7 dug fast.",
                "§8{killer}§7 found another body for the Graverend.",
                "§8{victim}§7 earned a headstone from §8{killer}§7."
        ));
        POOL.put("emberlash", Arrays.asList(
                "§c{victim} §7was §clashed into embers §7by §c{killer}§7's Emberlash.",
                "§c{killer}§7 painted §c{victim} §7in fire.",
                "§c{victim}§7 was §cburned from three sides§7 — §c{killer}§7 swung once.",
                "§c{killer} §7and Emberlash left nothing but §cash§7 of §c{victim}§7.",
                "§c{victim}§7 caught an ember from §c{killer}§7. Just one."
        ));
        POOL.put("ruinhammer", Arrays.asList(
                "§6{victim} §7was §6ruined §7by §6{killer}§7's Ruinhammer.",
                "§6{killer}§7 added §6{victim} §7to the list.",
                "§6{victim}§7 was §6hammered flat §7— §6{killer}§7 barely noticed.",
                "§6{killer}§7 rang Ruinhammer against §6{victim}§7's skull.",
                "§6{victim}§7 is part of the rubble now. §6{killer}§7 is not."
        ));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        ItemStack hand = killer.getItemInHand();
        if (hand == null) return;
        String mythicId = MythicRegistry.idOf(hand);
        if (mythicId == null) return;
        List<String> pool = POOL.get(mythicId);
        if (pool == null || pool.isEmpty()) return;

        String line = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        line = line.replace("{victim}", victim.getName())
                   .replace("{killer}", killer.getName());
        // PlayerDeathEvent.setDeathMessage controls the global broadcast the
        // server sends on player death. Overriding here keeps chat clean —
        // Bukkit doesn't double-broadcast our custom line.
        e.setDeathMessage(ChatColor.translateAlternateColorCodes('&', line));
    }

    /** Expose registry size for /lunar-style diagnostics if ever needed. */
    public static int registeredMythicCount() { return POOL.size(); }

    /** Utility for admin commands: broadcast a mythic's full line pool. */
    public static List<String> pool(String mythicId) { return POOL.get(mythicId); }

    /** Idempotent no-op registration helper so callers can ensure the class
     *  is loaded without holding a reference. */
    public static void touch() { /* loads the static POOL by class-load */ }

    public static void broadcastSample(String mythicId) {
        List<String> p = POOL.get(mythicId);
        if (p == null) return;
        for (String s : p) Bukkit.broadcastMessage("§8[§dsample§8] §r"
                + s.replace("{victim}", "Alice").replace("{killer}", "Bob"));
    }
}
