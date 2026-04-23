package com.soulenchants.style;

import org.bukkit.command.CommandSender;

/**
 * Wrappers for sending messages with the MessageStyle.PREFIX consistently
 * attached. Reduces the "did I forget the prefix" surface area across 40+
 * commands.
 */
public final class Chat {

    private Chat() {}

    public static void info(CommandSender who, String body) {
        who.sendMessage(MessageStyle.PREFIX + MessageStyle.MUTED + body);
    }

    public static void good(CommandSender who, String body) {
        who.sendMessage(MessageStyle.PREFIX + MessageStyle.GOOD + body);
    }

    public static void bad(CommandSender who, String body) {
        who.sendMessage(MessageStyle.PREFIX + MessageStyle.BAD + body);
    }

    public static void err(CommandSender who, String body) {
        who.sendMessage(MessageStyle.PREFIX + MessageStyle.BAD + MessageStyle.BOLD + "✗ " + MessageStyle.RESET + MessageStyle.BAD + body);
    }

    public static void rule(CommandSender who) {
        who.sendMessage(MessageStyle.RULE);
    }

    public static void banner(CommandSender who, String title) {
        who.sendMessage(MessageStyle.RULE);
        who.sendMessage(MessageStyle.PREFIX + MessageStyle.SOUL_GOLD + MessageStyle.BOLD + title);
        who.sendMessage(MessageStyle.RULE);
    }
}
