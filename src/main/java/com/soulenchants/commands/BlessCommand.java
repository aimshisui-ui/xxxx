package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

public class BlessCommand implements CommandExecutor {

    private static final List<PotionEffectType> NEGATIVE = Arrays.asList(
            PotionEffectType.SLOW,
            PotionEffectType.SLOW_DIGGING,
            PotionEffectType.CONFUSION,
            PotionEffectType.BLINDNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.WEAKNESS,
            PotionEffectType.POISON,
            PotionEffectType.WITHER
    );

    private final SoulEnchants plugin;
    public BlessCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                Chat.err(sender, "Console must specify a player: /bless <player>");
                return true;
            }
            target = (Player) sender;
        } else {
            if (!sender.hasPermission("soulenchants.admin")
                    && !(sender instanceof Player && ((Player) sender).getName().equalsIgnoreCase(args[0]))) {
                Chat.err(sender, "No permission to bless others.");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                Chat.err(sender, "Player not online: " + args[0]);
                return true;
            }
        }

        int removed = 0;
        for (PotionEffectType type : NEGATIVE) {
            if (target.hasPotionEffect(type)) {
                target.removePotionEffect(type);
                removed++;
            }
        }
        if (target.getFireTicks() > 0) target.setFireTicks(0);

        // Suppress Drunk's Slow/MF from re-applying until the helmet is taken off or swapped
        plugin.getTickTask().bless(target);

        target.getWorld().playSound(target.getLocation(), Sound.LEVEL_UP, 1.0f, 1.4f);
        for (int i = 0; i < 8; i++) {
            double a = (Math.PI * 2 * i) / 8.0;
            target.getWorld().playEffect(target.getLocation().add(Math.cos(a), 1, Math.sin(a)),
                    Effect.HAPPY_VILLAGER, 0);
        }

        target.sendMessage(MessageStyle.PREFIX + MessageStyle.SOUL_GOLD + MessageStyle.BOLD
                + "✦ BLESSED " + MessageStyle.RESET + MessageStyle.MUTED
                + "Cleansed " + MessageStyle.VALUE + removed
                + MessageStyle.MUTED + " negative effect" + (removed == 1 ? "" : "s") + ".");
        if (!target.equals(sender)) Chat.good(sender, "Blessed " + MessageStyle.VALUE + target.getName()
                + MessageStyle.GOOD + ".");
        return true;
    }
}
