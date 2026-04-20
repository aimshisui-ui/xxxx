package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Single-class tab completer that handles every plugin command.
 * Each command's first arg presents the list of subcommands; subsequent
 * args present player names or context-specific values.
 */
public class TabCompletion implements TabCompleter {

    private final SoulEnchants plugin;
    public TabCompletion(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        String name = cmd.getName().toLowerCase();
        switch (name) {
            case "souls": return tabSouls(args);
            case "ce":    return tabCe(args);
            case "shop":  return tabShop(args);
            case "quests":return tabQuests(args);
            case "boss":  return tabBoss(args);
            case "bless": return tabPlayer(args, 0);
            case "mob":   return tabMob(args);
            case "rift":  return tabRift(args);
            default:      return new ArrayList<>();
        }
    }

    private List<String> tabRift(String[] args) {
        if (args.length == 1) return filter(args[0],
                "tp", "setspawn", "give",
                "addspawn", "removespawn", "listspawns", "clearspawns",
                "status", "abort",
                "hologram", "holo", "holos");
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give": return playerNames(args[1]);
                case "addspawn": {
                    List<String> ids = new ArrayList<>();
                    com.soulenchants.mobs.MobRegistry.all().forEach(m -> ids.add(m.id));
                    ids.add("veilweaver");
                    ids.add("irongolem");
                    return filter(args[1], ids.toArray(new String[0]));
                }
                case "hologram": case "holo": case "holos":
                    return filter(args[1], "new", "create", "delete", "remove");
            }
        }
        return new ArrayList<>();
    }

    private List<String> tabSouls(String[] args) {
        if (args.length == 1) return filter(args[0],
                "show", "tier", "rules",
                "give", "take", "set",
                "setlifetime", "settier", "debug", "simkill");
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "show": case "tier":
                case "give": case "take": case "set":
                case "setlifetime": case "settier":
                    return playerNames(args[1]);
                case "simkill": return Arrays.asList("1","5","10");
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("settier")) {
            return filter(args[2], "initiate","bronze","silver","gold","veiled","soulbound");
        }
        return new ArrayList<>();
    }

    private List<String> tabCe(String[] args) {
        if (args.length == 1) return filter(args[0],
                "list", "menu", "god", "vault", "recipe",
                "bossset", "godset", "fixhp", "loot",
                "book", "dust", "scroll", "summon", "despawn", "giveloot");
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "book": case "dust": case "scroll": case "fixhp":
                    return playerNames(args[1]);
                case "summon": {
                    // Bosses + every custom mob id
                    List<String> ids = new ArrayList<>();
                    ids.add("veilweaver"); ids.add("irongolem");
                    com.soulenchants.mobs.MobRegistry.all().forEach(m -> ids.add(m.id));
                    return filter(args[1], ids.toArray(new String[0]));
                }
                case "despawn":
                    return filter(args[1], "veilweaver","irongolem");
                case "giveloot":
                    return filter(args[1], LOOT_IDS);
                case "loot":
                    return filter(args[1], "reload");
            }
        }
        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "dust":   return filter(args[2], "25","50","75","100");
                case "scroll": return filter(args[2], "white","black");
                case "summon": return filter(args[2], "1","3","5","10","20");
                case "book": {
                    List<String> all = new ArrayList<>();
                    com.soulenchants.enchants.EnchantRegistry.all()
                            .forEach(e -> all.add(e.getId()));
                    return filter(args[2], all.toArray(new String[0]));
                }
                case "giveloot": return playerNames(args[2]);
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("book")) {
            return Arrays.asList("1","2","3","4","5","6","7");
        }
        return new ArrayList<>();
    }

    private List<String> tabShop(String[] args) {
        if (args.length == 1) return filter(args[0], "refresh");
        return new ArrayList<>();
    }

    private List<String> tabQuests(String[] args) {
        if (args.length == 1) return filter(args[0], "status", "reset");
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return playerNames(args[1]);
        return new ArrayList<>();
    }

    private List<String> tabBoss(String[] args) {
        if (args.length == 1) return filter(args[0],
                "list","kill","debug","clearshield","clearinvuln","killminions","simhit");
        if (args.length == 2 && args[0].equalsIgnoreCase("simhit"))
            return Arrays.asList("10","50","100","200","500");
        return new ArrayList<>();
    }

    private List<String> tabMob(String[] args) {
        if (args.length == 1) return filter(args[0], "list", "spawn", "killall", "clear");
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            List<String> ids = new ArrayList<>();
            com.soulenchants.mobs.MobRegistry.all().forEach(m -> ids.add(m.id));
            return filter(args[1], ids.toArray(new String[0]));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("spawn"))
            return Arrays.asList("1","3","5","10");
        return new ArrayList<>();
    }

    private List<String> tabPlayer(String[] args, int slot) {
        if (args.length == slot + 1) return playerNames(args[slot]);
        return new ArrayList<>();
    }

    // ── helpers ──
    private static List<String> playerNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
        return filter(prefix, names.toArray(new String[0]));
    }

    private static List<String> filter(String prefix, String... pool) {
        List<String> out = new ArrayList<>();
        String lp = prefix == null ? "" : prefix.toLowerCase();
        for (String s : pool) if (s.toLowerCase().startsWith(lp)) out.add(s);
        return out;
    }

    private static final String[] LOOT_IDS = {
            "ironhearts_hammer", "colossus_plating_core", "veilseekers_mantle",
            "loom_of_eternity", "apex_carapace", "heart_of_the_forge", "veil_sigil",
            "forged_bulwark_plate", "veiled_edge", "aether_bow",
            "earthshaker_treads", "shadowstep_sandals", "stoneskin_tonic", "phasing_elixir",
            "colossus_slag", "iron_heart_fragment", "forged_ember", "reinforced_plating",
            "bulwark_core", "veil_thread", "frayed_soul", "echoing_strand",
            "phantom_silk", "veil_essence",
            "box_bronze", "box_silver", "box_gold", "box_boss"
    };
}
