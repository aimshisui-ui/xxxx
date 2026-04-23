package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.items.ItemFactories;
import com.soulenchants.style.Chat;
import com.soulenchants.style.MessageStyle;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CECommand implements CommandExecutor {

    private final SoulEnchants plugin;
    public CECommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            Chat.banner(sender, "Enchant Registry " + MessageStyle.MUTED
                    + "(" + EnchantRegistry.all().size() + ")");
            for (CustomEnchant e : EnchantRegistry.all()) {
                sender.sendMessage(MessageStyle.FRAME + "  " + MessageStyle.tier(e.getTier())
                        + MessageStyle.DIAMOND + " " + e.getDisplayName()
                        + MessageStyle.SEP + MessageStyle.MUTED + "max "
                        + MessageStyle.VALUE + CustomEnchant.roman(e.getMaxLevel())
                        + MessageStyle.SEP + MessageStyle.MUTED + e.getDescription());
            }
            Chat.rule(sender);
            return true;
        }
        if (!sender.hasPermission("soulenchants.admin")) {
            Chat.err(sender, "You need soulenchants.admin.");
            return true;
        }
        if (sub.equals("book")) {
            if (args.length < 4) { Chat.info(sender, "Usage: /ce book <player> <enchant> <level>"); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { Chat.err(sender, "Player not online: " + args[1]); return true; }
            CustomEnchant e = EnchantRegistry.get(args[2]);
            if (e == null) { Chat.err(sender, "Unknown enchant '" + args[2] + "' — try /ce list."); return true; }
            int lvl;
            try { lvl = Math.min(Integer.parseInt(args[3]), e.getMaxLevel()); }
            catch (NumberFormatException ex) { Chat.err(sender, "Invalid level: " + args[3]); return true; }
            target.getInventory().addItem(ItemFactories.book(e, lvl));
            Chat.good(sender, "Gave " + MessageStyle.VALUE + target.getName()
                    + MessageStyle.GOOD + " a " + MessageStyle.tier(e.getTier()) + e.getDisplayName()
                    + " " + CustomEnchant.roman(lvl) + MessageStyle.GOOD + " book.");
            return true;
        }
        if (sub.equals("dust")) {
            if (args.length < 3) { Chat.info(sender, "Usage: /ce dust <player> <25|50|75|100>"); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { Chat.err(sender, "Player not online: " + args[1]); return true; }
            int rate;
            try { rate = Integer.parseInt(args[2]); }
            catch (NumberFormatException ex) { Chat.err(sender, "Invalid rate: " + args[2]); return true; }
            target.getInventory().addItem(ItemFactories.dust(rate));
            Chat.good(sender, "Gave " + MessageStyle.VALUE + target.getName()
                    + MessageStyle.GOOD + " Magic Dust " + MessageStyle.VALUE + rate + "%" + MessageStyle.GOOD + ".");
            return true;
        }
        if (sub.equals("scroll")) {
            if (args.length < 3) { Chat.info(sender, "Usage: /ce scroll <player> <black|white|transmog>"); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { Chat.err(sender, "Player not online: " + args[1]); return true; }
            ItemStack scroll;
            if (args[2].equalsIgnoreCase("transmog"))   scroll = com.soulenchants.items.TransmogScroll.item();
            else if (args[2].equalsIgnoreCase("white")) scroll = ItemFactories.whiteScroll();
            else                                        scroll = ItemFactories.blackScroll();
            target.getInventory().addItem(scroll);
            Chat.good(sender, "Gave " + MessageStyle.VALUE + target.getName()
                    + MessageStyle.GOOD + " a " + MessageStyle.VALUE + args[2]
                    + MessageStyle.GOOD + " scroll.");
            return true;
        }
        if (sub.equals("bossset")) {
            if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
            Player p = (Player) sender;
            com.soulenchants.items.GodSet.giveBossSet(p);
            Chat.good(p, MessageStyle.TIER_LEGENDARY + MessageStyle.BOLD + "✦ Boss-killer set equipped "
                    + MessageStyle.RESET + MessageStyle.MUTED + "(PvE). Try "
                    + MessageStyle.VALUE + "/ce summon veilweaver");
            return true;
        }
        if (sub.equals("godset")) {
            if (!(sender instanceof Player)) { Chat.err(sender, "Players only."); return true; }
            Player p = (Player) sender;
            com.soulenchants.items.GodSet.giveGodSet(p);
            Chat.good(p, MessageStyle.TIER_SOUL + MessageStyle.BOLD + "✦ God set equipped "
                    + MessageStyle.RESET + MessageStyle.MUTED + "(PvP). Designed for player combat.");
            return true;
        }
        if (sub.equals("fixhp")) {
            Player target = args.length >= 2 ? Bukkit.getPlayer(args[1])
                    : (sender instanceof Player ? (Player) sender : null);
            if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
            // Wipe permanent consumable buffs so the tick task can't re-apply them
            int heartsCleared = plugin.getLootProfile().getHeartStacks(target.getUniqueId());
            plugin.getLootProfile().clearHearts(target.getUniqueId());
            target.setMaxHealth(20.0);
            target.setHealth(20.0);
            sender.sendMessage("§a✦ Reset §f" + target.getName()
                    + "§a's max HP to 20 (cleared §f" + heartsCleared + "§a Heart of the Forge stacks).");
            if (target != sender) target.sendMessage("§a✦ An admin reset your max HP and consumable buffs.");
            return true;
        }
        if (sub.equals("menu")) {
            if (!(sender instanceof Player)) { sender.sendMessage("§cMust be a player."); return true; }
            plugin.getEnchantMenu().open((Player) sender);
            return true;
        }
        if (sub.equals("god") || sub.equals("vault")) {
            if (!(sender instanceof Player)) { sender.sendMessage("§cMust be a player."); return true; }
            plugin.getGodMenu().openHub((Player) sender);
            return true;
        }
        if (sub.equals("kill") || sub.equals("killall")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage("§cNo permission."); return true; }
            int killed = 0;
            // Custom mobs (any with se_custom_mob NBT/metadata)
            for (org.bukkit.World w : org.bukkit.Bukkit.getWorlds()) {
                for (org.bukkit.entity.Entity en : new java.util.ArrayList<>(w.getEntities())) {
                    if (!(en instanceof org.bukkit.entity.LivingEntity)) continue;
                    if (com.soulenchants.mobs.CustomMob.idOf((org.bukkit.entity.LivingEntity) en) == null) continue;
                    en.remove();
                    killed++;
                }
            }
            // Active bosses
            if (plugin.getVeilweaverManager().getActive() != null) {
                try { plugin.getVeilweaverManager().getActive().getEntity().remove();
                      plugin.getVeilweaverManager().getActive().stop(false); killed++; } catch (Throwable ignored) {}
            }
            if (plugin.getIronGolemManager().getActive() != null) {
                try { plugin.getIronGolemManager().getActive().getEntity().remove();
                      plugin.getIronGolemManager().getActive().stop(false); killed++; } catch (Throwable ignored) {}
            }
            sender.sendMessage("§a✦ Removed §f" + killed + "§a custom entities (mobs + bosses).");
            return true;
        }
        if (sub.equals("reload")) {
            plugin.reloadEnchantConfigs();
            com.soulenchants.style.Chat.good(sender, "Reloaded enchants.yml + mythics.yml.");
            return true;
        }
        if (sub.equals("loot")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
                plugin.getLootConfig().reload();
                sender.sendMessage("§a✦ Loot overrides reloaded from YAML.");
                return true;
            }
            if (!(sender instanceof Player)) { sender.sendMessage("§cMust be a player."); return true; }
            plugin.getLootEditorGUI().openRoot((Player) sender);
            return true;
        }
        if (sub.equals("giveloot") || sub.equals("loot")) {
            if (!sender.hasPermission("soulenchants.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (args.length < 2) {
                sender.sendMessage("§c/ce giveloot <id> [player]");
                sender.sendMessage("§7Boss: §fironhearts_hammer, colossus_plating_core, veilseekers_mantle,");
                sender.sendMessage("§7  loom_of_eternity, apex_carapace, heart_of_the_forge, veil_sigil");
                sender.sendMessage("§7Crafted: §fforged_bulwark_plate, veiled_edge, aether_bow");
                sender.sendMessage("§7Rare gear: §fearthshaker_treads, shadowstep_sandals, stoneskin_tonic, phasing_elixir");
                sender.sendMessage("§7Reagents: §fcolossus_slag, iron_heart_fragment, forged_ember, reinforced_plating,");
                sender.sendMessage("§7  bulwark_core, veil_thread, frayed_soul, echoing_strand, phantom_silk, veil_essence");
                sender.sendMessage("§7Loot boxes: §fbox_bronze, box_silver, box_gold, box_boss");
                return true;
            }
            Player giveTarget = args.length >= 3 ? Bukkit.getPlayer(args[2])
                    : (sender instanceof Player ? (Player) sender : null);
            if (giveTarget == null) { sender.sendMessage("§cTarget player not found."); return true; }
            org.bukkit.inventory.ItemStack stack = lootById(args[1]);
            if (stack == null) { sender.sendMessage("§cUnknown loot ID. Run /ce giveloot for list."); return true; }
            giveTarget.getInventory().addItem(stack);
            sender.sendMessage("§a✦ Gave " + giveTarget.getName() + " §f" + args[1]);
            return true;
        }

        if (sub.equals("recipe") || sub.equals("recipes")) {
            // Visual GUI for players; text fallback for console
            if (sender instanceof Player) { plugin.getRecipeGUI().openList((Player) sender); return true; }
            sender.sendMessage("§9✦ §3Custom Recipes:");
            for (com.soulenchants.loot.LootRecipes.RecipeEntry r : com.soulenchants.loot.LootRecipes.ENTRIES) {
                sender.sendMessage("§7  ▸ §f" + r.name);
                for (String row : r.shape) sender.sendMessage("§7      §b" + row.replace(' ', '·'));
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (int i = 0; i < 9; i++) {
                    String id = r.ingredientLootIds.get(i);
                    org.bukkit.Material mat = r.perSlotMaterials[i];
                    if (mat == null) continue;
                    String key = (id == null ? "" : id) + "|" + mat;
                    if (!seen.add(key)) continue;
                    String ingLabel = id != null ? id.replace('_', ' ')
                            : mat.name().toLowerCase().replace('_', ' ');
                    sender.sendMessage("§7        • §f" + ingLabel);
                }
                sender.sendMessage("§7      §a→ §f" + r.result.getItemMeta().getDisplayName());
            }
            if (sender instanceof Player) sender.sendMessage("§7  §o(or use /ce god → Recipes)");
            return true;
        }
        if (sub.equals("summon") && args.length >= 2) {
            if (!(sender instanceof Player)) { sender.sendMessage("§cMust be a player."); return true; }
            Player p = (Player) sender;
            String id = args[1].toLowerCase();
            // Bosses keep their special spawn paths (BossHealthHack, encounter, etc.)
            if (id.equals("veilweaver")) {
                boolean ok = plugin.getVeilweaverManager().summon(p.getLocation());
                if (!ok) { sender.sendMessage("§cThe Veilweaver is already manifested."); return true; }
                sender.sendMessage("§a✦ The Veilweaver has been summoned.");
                return true;
            }
            if (id.equals("irongolem") || id.equals("colossus")) {
                boolean ok = plugin.getIronGolemManager().summon(p.getLocation());
                if (!ok) { sender.sendMessage("§cThe Ironheart Colossus is already alive."); return true; }
                sender.sendMessage("§a✦ The Ironheart Colossus has been summoned.");
                return true;
            }
            // Any custom mob from the registry — optional [count] arg defaults to 1
            com.soulenchants.mobs.CustomMob cm = com.soulenchants.mobs.MobRegistry.get(id);
            if (cm == null) {
                sender.sendMessage("§c/ce summon <id>  (use Tab — supports any custom mob id)");
                return true;
            }
            int count = 1;
            if (args.length >= 3) {
                try { count = Math.max(1, Math.min(50, Integer.parseInt(args[2]))); }
                catch (NumberFormatException ignored) {}
            }
            int placed = 0;
            for (int i = 0; i < count; i++) {
                org.bukkit.entity.LivingEntity le = cm.spawn(p.getLocation());
                if (le != null) placed++;
            }
            sender.sendMessage("§a✦ Spawned §f" + placed + "× §a" + cm.tier.color + cm.displayName);
            return true;
        }
        if (sub.equals("despawn") && args.length >= 2) {
            String boss = args[1].toLowerCase();
            if (boss.equals("veilweaver")) {
                if (plugin.getVeilweaverManager().getActive() == null) { sender.sendMessage("§cNo active Veilweaver."); return true; }
                plugin.getVeilweaverManager().getActive().getEntity().remove();
                plugin.getVeilweaverManager().getActive().stop(false);
                sender.sendMessage("§a✦ Veilweaver despawned.");
                return true;
            }
            if (boss.equals("irongolem") || boss.equals("colossus")) {
                if (plugin.getIronGolemManager().getActive() == null) { sender.sendMessage("§cNo active Colossus."); return true; }
                plugin.getIronGolemManager().getActive().getEntity().remove();
                plugin.getIronGolemManager().getActive().stop(false);
                sender.sendMessage("§a✦ Colossus despawned.");
                return true;
            }
            sender.sendMessage("§c/ce despawn <veilweaver|irongolem>");
            return true;
        }
        help(sender);
        return true;
    }

    private void help(CommandSender s) {
        Chat.banner(s, "SoulEnchants " + MessageStyle.MUTED + "v1.1 " + MessageStyle.FRAME + "commands");
        group(s, "General");
        row(s, "/ce list",                            "browse every registered enchant");
        row(s, "/ce menu",                            "paginated enchant catalog " + MessageStyle.FRAME + "(admin)");
        row(s, "/ce god",                             "hub menu " + MessageStyle.FRAME + "— everything in one GUI");
        row(s, "/ce recipe",                          "every custom crafting recipe");
        row(s, "/ce reload",                          MessageStyle.TIER_EPIC + "v1.1 " + MessageStyle.MUTED + "reload enchants.yml + mythics.yml live");

        group(s, "Give");
        row(s, "/ce book <player> <enchant> <level>", "hand out an enchant book");
        row(s, "/ce dust <player> <25|50|75|100>",    "hand out Magic Dust");
        row(s, "/ce scroll <player> <black|white|transmog>", "hand out a scroll");
        row(s, "/ce bossset",                         "equip the full boss-killer loadout");
        row(s, "/ce godset",                          "equip the PvP god set");
        row(s, "/ce giveloot <id> [player]",          "spawn any registered loot item");

        group(s, "Bosses");
        row(s, "/ce summon <boss|mob>",               "spawn any boss or custom mob");
        row(s, "/ce despawn <veilweaver|irongolem>",  "force-kill a running boss");
        row(s, "/ce loot",                            "GUI editor for mob stats / drops");
        row(s, "/ce loot reload",                     "re-read loot overrides from YAML");

        group(s, "Related");
        row(s, "/mythic list | give <id> [player]",   MessageStyle.TIER_SOUL + "v1.1 " + MessageStyle.MUTED + "mythic weapons");
        row(s, "/mask list | equip <id> | clear",     MessageStyle.TIER_EPIC + "v1.1 " + MessageStyle.MUTED + "cosmetic helmet overrides");
        Chat.rule(s);
    }

    private static void group(CommandSender s, String label) {
        s.sendMessage(MessageStyle.FRAME + "  " + MessageStyle.BAR + MessageStyle.BAR + " "
                + MessageStyle.SOUL_GOLD + MessageStyle.BOLD + label + " "
                + MessageStyle.FRAME + MessageStyle.BAR + MessageStyle.BAR);
    }
    private static void row(CommandSender s, String cmd, String desc) {
        s.sendMessage(MessageStyle.FRAME + "   " + MessageStyle.ARROW + " "
                + MessageStyle.VALUE + cmd + MessageStyle.FRAME + "  " + MessageStyle.BAR + "  "
                + MessageStyle.MUTED + desc);
    }

    /** Resolve a loot id string to its factory-built ItemStack. Returns null if unknown. */
    private static org.bukkit.inventory.ItemStack lootById(String id) {
        switch (id.toLowerCase()) {
            // Boss-tier
            case "ironhearts_hammer":     return com.soulenchants.loot.BossLootItems.ironheartsHammer();
            case "colossus_plating_core": return com.soulenchants.loot.BossLootItems.colossusPlatingCore();
            case "veilseekers_mantle":    return com.soulenchants.loot.BossLootItems.veilseekersMantle();
            case "loom_of_eternity":      return com.soulenchants.loot.BossLootItems.loomOfEternity();
            case "apex_carapace":         return com.soulenchants.loot.BossLootItems.apexCarapace();
            case "heart_of_the_forge":    return com.soulenchants.loot.BossLootItems.heartOfTheForge();
            case "veil_sigil":            return com.soulenchants.loot.BossLootItems.veilSigil();
            // Crafted (mid)
            case "forged_bulwark_plate":  return com.soulenchants.loot.BossLootItems.forgedBulwarkPlate();
            case "veiled_edge":           return com.soulenchants.loot.BossLootItems.veiledEdge();
            case "aether_bow":            return com.soulenchants.loot.BossLootItems.aetherBow();
            // Rare gear
            case "earthshaker_treads":    return com.soulenchants.loot.BossLootItems.earthshakerTreads();
            case "shadowstep_sandals":    return com.soulenchants.loot.BossLootItems.shadowstepSandals();
            case "stoneskin_tonic":       return com.soulenchants.loot.BossLootItems.stoneskinTonic();
            case "phasing_elixir":        return com.soulenchants.loot.BossLootItems.phasingElixir();
            // Reagents (default stack of 4 for granular ones)
            case "colossus_slag":         return com.soulenchants.loot.BossLootItems.colossusSlag(8);
            case "iron_heart_fragment":   return com.soulenchants.loot.BossLootItems.ironHeartFragment(8);
            case "forged_ember":          return com.soulenchants.loot.BossLootItems.forgedEmber(4);
            case "reinforced_plating":    return com.soulenchants.loot.BossLootItems.reinforcedPlating(4);
            case "bulwark_core":          return com.soulenchants.loot.BossLootItems.bulwarkCore();
            case "veil_thread":           return com.soulenchants.loot.BossLootItems.veilThread(8);
            case "frayed_soul":           return com.soulenchants.loot.BossLootItems.frayedSoul(8);
            case "echoing_strand":        return com.soulenchants.loot.BossLootItems.echoingStrand(4);
            case "phantom_silk":          return com.soulenchants.loot.BossLootItems.phantomSilk(4);
            case "veil_essence":          return com.soulenchants.loot.BossLootItems.veilEssence();
            // Loot boxes
            case "box_bronze":            return com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BRONZE);
            case "box_silver":            return com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.SILVER);
            case "box_gold":              return com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.GOLD);
            case "box_boss":              return com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BOSS);
            default: return null;
        }
    }
}
