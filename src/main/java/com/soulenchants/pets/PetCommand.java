package com.soulenchants.pets;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /pet list               — list every registered pet
 * /pet give <id> [player] — admin: grant a fresh Lv.1 egg
 * /pet info               — show details of the pet in your hand
 * /pet despawn            — dismiss your active pet
 * /pet xp <amount>        — admin: grant xp to the caller's active pet
 */
public final class PetCommand implements CommandExecutor {

    private final SoulEnchants plugin;

    public PetCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list": return listPets(sender);
            case "info": return infoPet(sender);
            case "despawn": case "dismiss": return despawn(sender);
            case "give":    return givePet(sender, args);
            case "xp":      return grantXp(sender, args);
            default: help(sender); return true;
        }
    }

    // ──────────────── Subcommands ────────────────

    private boolean listPets(CommandSender to) {
        Chat.banner(to, "Pets " + MessageStyle.FRAME + "registry");
        for (Pet p : PetRegistry.all()) {
            to.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.ARROW + " "
                    + p.getRarityColor() + p.getDisplayName()
                    + MessageStyle.FRAME + "  " + MessageStyle.BAR + "  "
                    + MessageStyle.MUTED + p.getArchetype()
                    + MessageStyle.FRAME + "  " + MessageStyle.BAR + "  "
                    + MessageStyle.VALUE + "/pet give " + p.getId());
        }
        Chat.rule(to);
        return true;
    }

    private boolean infoPet(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage("§cPlayers only."); return true; }
        Player p = (Player) sender;
        ItemStack held = p.getItemInHand();
        if (!PetItem.isPetEgg(held)) {
            Chat.info(p, "Hold a pet egg to inspect it.");
            return true;
        }
        Pet pet = PetRegistry.get(PetItem.idOf(held));
        if (pet == null) { Chat.bad(p, "Unknown pet id on egg."); return true; }
        int level = PetItem.levelOf(held);
        long xp   = PetItem.xpOf(held);
        Chat.banner(p, pet.getRarityColor() + pet.getDisplayName());
        p.sendMessage(MessageStyle.MUTED + "  Archetype: " + MessageStyle.VALUE + pet.getArchetype());
        p.sendMessage(MessageStyle.MUTED + "  Level:     " + MessageStyle.VALUE + level
                + MessageStyle.MUTED + "  XP: " + MessageStyle.VALUE + xp
                + MessageStyle.MUTED + "/" + Pet.xpForLevel(level + 1));
        p.sendMessage(MessageStyle.TIER_EPIC + "  Passive: " + MessageStyle.MUTED + pet.getPassiveDescription());
        p.sendMessage(MessageStyle.TIER_LEGENDARY + "  Active:  " + MessageStyle.MUTED + pet.getActiveDescription());
        Chat.rule(p);
        return true;
    }

    private boolean despawn(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage("§cPlayers only."); return true; }
        Player p = (Player) sender;
        boolean had = plugin.getPetManager().despawn(p, true);
        if (!had) Chat.info(p, "No active pet to dismiss.");
        return true;
    }

    private boolean givePet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("soulenchants.admin")) {
            sender.sendMessage("§cNo permission."); return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pet give <id> [player]"); return true;
        }
        Pet pet = PetRegistry.get(args[1].toLowerCase());
        if (pet == null) { sender.sendMessage("§cUnknown pet id. Try /pet list."); return true; }
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) { sender.sendMessage("§cPlayer not online."); return true; }
        } else {
            if (!(sender instanceof Player)) { sender.sendMessage("§cSpecify a target from console."); return true; }
            target = (Player) sender;
        }
        ItemStack egg = PetItem.fresh(pet);
        target.getInventory().addItem(egg);
        Chat.good(target, "Received " + pet.getRarityColor() + pet.getDisplayName() + MessageStyle.GOOD + ".");
        if (sender != target) Chat.good(sender, "Gave " + pet.getRarityColor() + pet.getDisplayName()
                + MessageStyle.GOOD + " to " + target.getName() + ".");
        return true;
    }

    private boolean grantXp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("soulenchants.admin")) {
            sender.sendMessage("§cNo permission."); return true;
        }
        if (!(sender instanceof Player)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /pet xp <amount>"); return true; }
        long amount;
        try { amount = Long.parseLong(args[1]); }
        catch (NumberFormatException ex) { sender.sendMessage("§cAmount must be a number."); return true; }
        plugin.getPetManager().grantXp((Player) sender, amount);
        return true;
    }

    private static void help(CommandSender to) {
        Chat.banner(to, "Pet " + MessageStyle.FRAME + "commands");
        row(to, "/pet list",           "show every registered pet");
        row(to, "/pet info",           "inspect the egg in your hand");
        row(to, "/pet despawn",        "dismiss your active pet");
        row(to, "/pet give <id> [p]",  MessageStyle.TIER_RARE + "admin " + MessageStyle.MUTED + "— grant a fresh egg");
        row(to, "/pet xp <amount>",    MessageStyle.TIER_RARE + "admin " + MessageStyle.MUTED + "— award xp to active pet");
        Chat.rule(to);
    }

    private static void row(CommandSender s, String cmd, String desc) {
        s.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.ARROW + " "
                + MessageStyle.VALUE + cmd
                + MessageStyle.FRAME + "  " + MessageStyle.BAR + "  "
                + (desc.startsWith("§") ? desc : MessageStyle.MUTED + desc));
    }
}
