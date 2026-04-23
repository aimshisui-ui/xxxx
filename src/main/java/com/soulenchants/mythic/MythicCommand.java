package com.soulenchants.mythic;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MythicCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public MythicCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("soulenchants.admin")) {
            Chat.err(sender, "You need soulenchants.admin.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Chat.banner(sender, "Mythic Weapons");
            for (MythicWeapon m : MythicRegistry.all()) {
                sender.sendMessage(MessageStyle.FRAME + "  " + MessageStyle.TIER_SOUL + MessageStyle.BOLD +
                        "❖ " + m.getDisplayName() + MessageStyle.SEP + MessageStyle.MUTED +
                        m.getId() + MessageStyle.SEP + MessageStyle.MUTED + MessageStyle.ITALIC +
                        m.getMode().name().toLowerCase());
            }
            Chat.rule(sender);
            Chat.info(sender, "Sub-commands: " + MessageStyle.VALUE + "give / infuse / clear");
            return true;
        }
        if (args[0].equalsIgnoreCase("give") && args.length >= 2) {
            String target = args.length >= 3 ? args[2] : (sender instanceof Player ? sender.getName() : null);
            if (target == null) { Chat.err(sender, "Specify a player."); return true; }
            Player p = Bukkit.getPlayerExact(target);
            if (p == null) { Chat.err(sender, "Player not online: " + target); return true; }
            ItemStack item = MythicFactory.create(args[1].toLowerCase());
            if (item == null) { Chat.err(sender, "Unknown mythic id: " + args[1]); return true; }
            p.getInventory().addItem(item);
            Chat.good(sender, "Gave " + MessageStyle.TIER_SOUL + args[1] + MessageStyle.GOOD + " to " +
                    MessageStyle.VALUE + p.getName());
            return true;
        }
        // v1.1 — ability-slot binding. Infuses the HELD mythic with a second
        // effect, so one weapon procs core AND ability on every attack.
        if (args[0].equalsIgnoreCase("infuse") && args.length >= 2) {
            if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
            Player p = (Player) sender;
            ItemStack held = p.getItemInHand();
            String coreId = MythicRegistry.idOf(held);
            if (coreId == null) { Chat.err(p, "Hold a mythic weapon first."); return true; }
            String abilityId = args[1].toLowerCase();
            MythicWeapon ability = MythicRegistry.get(abilityId);
            if (ability == null) { Chat.err(p, "Unknown mythic id: " + abilityId); return true; }
            if (abilityId.equals(coreId)) {
                Chat.err(p, "Ability must differ from the core ("
                        + MessageStyle.TIER_SOUL + coreId + MessageStyle.BAD + ").");
                return true;
            }
            ItemStack bound = MythicRegistry.bindAbility(held, abilityId);
            bound = MythicFactory.reRender(bound);
            p.setItemInHand(bound);
            Chat.good(p, "Infused " + MessageStyle.TIER_SOUL + MessageStyle.BOLD + "❖ "
                    + MythicRegistry.get(coreId).getDisplayName()
                    + MessageStyle.GOOD + " with ability " + MessageStyle.TIER_EPIC
                    + MessageStyle.BOLD + "✦ " + ability.getDisplayName());
            p.getWorld().playSound(p.getLocation(), org.bukkit.Sound.ENDERMAN_TELEPORT, 0.8f, 1.5f);
            return true;
        }
        if (args[0].equalsIgnoreCase("clear")) {
            if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
            Player p = (Player) sender;
            ItemStack held = p.getItemInHand();
            if (MythicRegistry.idOf(held) == null) {
                Chat.err(p, "Hold a mythic weapon first.");
                return true;
            }
            if (MythicRegistry.abilityIdOf(held) == null) {
                Chat.info(p, "No ability was bound.");
                return true;
            }
            ItemStack cleared = MythicRegistry.clearAbility(held);
            cleared = MythicFactory.reRender(cleared);
            p.setItemInHand(cleared);
            Chat.good(p, "Cleared bound ability.");
            return true;
        }
        Chat.info(sender, "Usage: /mythic list  |  give <id> [player]  |  infuse <ability-id>  |  clear");
        return true;
    }
}
