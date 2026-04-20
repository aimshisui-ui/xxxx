package com.soulenchants.listeners;

import com.soulenchants.SoulEnchants;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Prefixes player chat with their lifetime soul tier badge.
 *   [Bronze] aimshisui-ui: hello
 *
 * Skips Initiate (no badge for newcomers).
 */
public class TierChatPrefix implements Listener {

    private final SoulEnchants plugin;
    public TierChatPrefix(SoulEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        com.soulenchants.currency.SoulTier tier = plugin.getSoulManager().getTier(e.getPlayer());
        if (tier == com.soulenchants.currency.SoulTier.INITIATE) return;
        e.setFormat(tier.prefix() + " " + e.getFormat());
    }
}
