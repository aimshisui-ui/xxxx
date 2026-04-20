package com.soulenchants.quests;

import com.soulenchants.enchants.EnchantTier;
import com.soulenchants.enchants.CustomEnchant;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.items.ItemFactories;
import org.bukkit.Material;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Static registry of every quest definition. Tutorial chain is the first
 * priority — every other system unlocks once tutorial is complete.
 *
 * Daily/weekly quests are random selections from a pool sized by player tier.
 */
public final class QuestRegistry {

    private static final Map<String, Quest> ALL = new LinkedHashMap<>();
    private static final Random RNG = new Random();

    private QuestRegistry() {}

    public static Quest get(String id) { return ALL.get(id); }
    public static java.util.Collection<Quest> all() { return ALL.values(); }

    public static List<Quest> tutorialChain() {
        List<Quest> out = new ArrayList<>();
        for (Quest q : ALL.values()) if (q.tier == Quest.Tier.TUTORIAL) out.add(q);
        out.sort(java.util.Comparator.comparingInt(q -> tutorialOrder(q.id)));
        return out;
    }

    public static List<Quest> dailyPool(long lifetimeSouls) {
        List<Quest> pool = new ArrayList<>();
        for (Quest q : ALL.values()) {
            if (q.tier != Quest.Tier.DAILY) continue;
            if (lifetimeSouls < 10_000 && q.id.startsWith("d_mid_"))   continue;
            if (lifetimeSouls < 50_000 && q.id.startsWith("d_late_"))  continue;
            pool.add(q);
        }
        return pool;
    }

    /** Choose 3 distinct dailies for a player. */
    public static List<Quest> rollDailies(long lifetimeSouls) {
        List<Quest> pool = dailyPool(lifetimeSouls);
        Collections.shuffle(pool, RNG);
        return pool.subList(0, Math.min(3, pool.size()));
    }

    public static void register() {
        ALL.clear();
        // ── TUTORIAL CHAIN ──────────────────────────────────────────────────
        register(simpleMob("t1_awakening", Quest.Tier.TUTORIAL, "Awakening",
                "Slay any hostile mob.", 1, 50,
                java.util.Arrays.asList(rollCommonBook())));
        register(itemApplied("t2_first_words", "First Words",
                "Apply an enchant book to any item.", 1, 100,
                java.util.Arrays.asList(ItemFactories.dust(25))));
        register(simpleMob("t3_sharper_steel", Quest.Tier.TUTORIAL, "Sharper Steel",
                "Slay 10 hostile mobs.", 10, 200,
                java.util.Arrays.asList(ItemFactories.whiteScroll())));
        register(commandRan("t4_archive", "The Archive",
                "Open the enchant catalog with /ce list.", "ce", 50,
                java.util.Arrays.asList(rollCommonBook(), rollCommonBook())));
        register(shopPurchase("t5_soul_bond", "Soul Bond",
                "Spend 300 souls at the Quartermaster shop.", 300, 200,
                java.util.Arrays.asList(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BRONZE))));

        // ── DAILY: EARLY POOL — target specific custom mob IDs ─────────────
        register(mobIdQuest("d_rotlings", "Hunter: Rotlings",
                "Kill 10 Rotlings.", 10, 100,
                java.util.Arrays.asList(), "rotling"));
        register(mobIdQuest("d_bonepup", "Hunter: Bone Pups",
                "Kill 8 Bone Pups.", 8, 100,
                java.util.Arrays.asList(), "bone_pup"));
        register(mobIdQuest("d_creeplets", "Hunter: Creeplets",
                "Kill 5 Creeplets.", 5, 150,
                java.util.Arrays.asList(), "creeplet"));
        register(mobIdQuest("d_weblurkers", "Hunter: Web Lurkers",
                "Kill 6 Web Lurkers.", 6, 110,
                java.util.Arrays.asList(), "web_lurker"));
        register(blockBreak("d_ores", "Prospector",
                "Mine 50 ore blocks.", 50, 80,
                java.util.Arrays.asList(com.soulenchants.loot.BossLootItems.veilThread(2))));
        register(itemApplied("d_apply", "Inscriber",
                "Apply 1 enchant book to any item.", 1, 80,
                java.util.Arrays.asList(ItemFactories.dust(25))));
        register(mobIdQuest("d_ashwalkers", "Hunter: Ash Walkers",
                "Kill 5 Ash-Walkers.", 5, 140,
                java.util.Arrays.asList(), "ash_walker"));

        // ── DAILY: MID POOL ────────────────────────────────────────────────
        // "any custom mob" daily still works — uses CustomMob.idOf != null
        register(new SimpleQuest("d_mid_hostile", "Hunter: Veteran",
                "Defeat 30 of the Veil's custom mobs.",
                Quest.Tier.DAILY, 30, 250,
                java.util.Arrays.asList(), "Slay any custom mob") {
            @Override public int handleEvent(Player p, QuestEvent e, int cur) {
                if (e.kind != QuestEvent.Kind.MOB_KILL || e.entity == null) return cur;
                if (!(e.entity instanceof org.bukkit.entity.LivingEntity)) return cur;
                return com.soulenchants.mobs.CustomMob.idOf((org.bukkit.entity.LivingEntity) e.entity) != null
                        ? cur + 1 : cur;
            }
        });
        register(shopPurchase("d_mid_shop", "Big Spender",
                "Spend 1000 souls at the shop.", 1000, 200,
                java.util.Arrays.asList(rollUncommonBook())));
        register(mobIdQuest("d_mid_emberimp", "Hunter: Ember Imps",
                "Kill 3 Ember Imps.", 3, 300,
                java.util.Arrays.asList(), "ember_imp"));
        register(mobIdQuest("d_mid_witchling", "Hunter: Witchlings",
                "Kill 3 Witchlings.", 3, 350,
                java.util.Arrays.asList(), "witchling"));
        register(mobIdQuest("d_mid_voidstalker", "Hunter: Voidstalkers",
                "Kill 2 Voidstalkers.", 2, 400,
                java.util.Arrays.asList(), "voidstalker"));
        register(mobIdQuest("d_mid_revenant", "Hunter: Revenants",
                "Kill 2 Revenants.", 2, 400,
                java.util.Arrays.asList(), "revenant"));

        // ── DAILY: LATE POOL ───────────────────────────────────────────────
        register(bossKilled("d_late_boss", "Slayer",
                "Defeat any boss.", 1, 1500,
                java.util.Arrays.asList(rollEpicBook())));
    }

    /** Order indexes for the tutorial chain so they show in sequence. */
    private static int tutorialOrder(String id) {
        switch (id) {
            case "t1_awakening":    return 1;
            case "t2_first_words":  return 2;
            case "t3_sharper_steel":return 3;
            case "t4_archive":      return 4;
            case "t5_soul_bond":    return 5;
            default: return 99;
        }
    }

    // ── Quest factories ──────────────────────────────────────────────────────

    private static SimpleQuest simpleMob(String id, Quest.Tier tier, String name, String desc,
                                         int goal, long souls, List<ItemStack> items) {
        return new SimpleQuest(id, name, desc, tier, goal, souls, items, "Slay mobs") {
            @Override public int handleEvent(Player p, QuestEvent e, int cur) {
                return e.kind == QuestEvent.Kind.MOB_KILL && e.entity instanceof Monster ? cur + 1 : cur;
            }
        };
    }

    /** Daily quest for a specific mob class (Zombie, Creeper, etc.). */
    private static SimpleQuest mobTypeQuest(String id, String name, String desc, int goal,
                                            long souls, List<ItemStack> items,
                                            final Class<?> mobClass) {
        return new SimpleQuest(id, name, desc, Quest.Tier.DAILY, goal, souls, items, "Slay specific mob") {
            @Override public int handleEvent(Player p, QuestEvent e, int cur) {
                if (e.kind != QuestEvent.Kind.MOB_KILL || e.entity == null) return cur;
                return mobClass.isInstance(e.entity) ? cur + 1 : cur;
            }
        };
    }

    /** Daily quest matching a specific custom mob id (from NBT). */
    private static SimpleQuest mobIdQuest(String id, String name, String desc, int goal,
                                          long souls, List<ItemStack> items,
                                          final String targetMobId) {
        return new SimpleQuest(id, name, desc, Quest.Tier.DAILY, goal, souls, items, "Slay custom mob") {
            @Override public int handleEvent(Player p, QuestEvent e, int cur) {
                if (e.kind != QuestEvent.Kind.MOB_KILL || e.entity == null) return cur;
                if (!(e.entity instanceof org.bukkit.entity.LivingEntity)) return cur;
                String killedId = com.soulenchants.mobs.CustomMob.idOf((org.bukkit.entity.LivingEntity) e.entity);
                return targetMobId.equals(killedId) ? cur + 1 : cur;
            }
        };
    }

    private static SimpleQuest blockBreak(String id, String name, String desc, int goal,
                                          long souls, List<ItemStack> items) {
        return new SimpleQuest(id, name, desc, Quest.Tier.DAILY, goal, souls, items, "Mine blocks") {
            @Override public int handleEvent(Player p, QuestEvent e, int cur) {
                if (e.kind != QuestEvent.Kind.BLOCK_BREAK) return cur;
                String n = e.stringPayload != null ? e.stringPayload.toUpperCase() : "";
                if (n.endsWith("_ORE")) return cur + 1;
                return cur;
            }
        };
    }

    private static SimpleQuest itemApplied(String id, String name, String desc, int goal,
                                           long souls, List<ItemStack> items) {
        return new SimpleQuest(id, name, desc, Quest.Tier.TUTORIAL, goal, souls, items, "Apply enchant") {
            @Override public int handleEvent(Player p, QuestEvent e, int cur) {
                return e.kind == QuestEvent.Kind.BOOK_APPLIED ? cur + 1 : cur;
            }
        };
    }

    private static SimpleQuest commandRan(String id, String name, String desc, String cmd,
                                          long souls, List<ItemStack> items) {
        final String want = cmd.toLowerCase();
        return new SimpleQuest(id, name, desc, Quest.Tier.TUTORIAL, 1, souls, items, "Run command") {
            @Override public int handleEvent(Player p, QuestEvent e, int cur) {
                return e.kind == QuestEvent.Kind.COMMAND_RAN
                        && e.stringPayload != null
                        && e.stringPayload.toLowerCase().equals(want) ? cur + 1 : cur;
            }
        };
    }

    private static SimpleQuest shopPurchase(String id, String name, String desc, int totalSouls,
                                             long reward, List<ItemStack> items) {
        return new SimpleQuest(id, name, desc, Quest.Tier.TUTORIAL, totalSouls, reward, items, "Spend at shop") {
            @Override public int handleEvent(Player p, QuestEvent e, int cur) {
                return e.kind == QuestEvent.Kind.SHOP_PURCHASE ? cur + (int) e.numeric : cur;
            }
        };
    }

    private static SimpleQuest bossKilled(String id, String name, String desc, int goal,
                                          long souls, List<ItemStack> items) {
        return new SimpleQuest(id, name, desc, Quest.Tier.DAILY, goal, souls, items, "Slay boss") {
            @Override public int handleEvent(Player p, QuestEvent e, int cur) {
                return e.kind == QuestEvent.Kind.BOSS_KILLED ? cur + 1 : cur;
            }
        };
    }

    private static void register(Quest q) { ALL.put(q.id, q); }

    private static ItemStack rollCommonBook() {
        return rollBookOfTier(EnchantTier.COMMON);
    }
    private static ItemStack rollUncommonBook() {
        return rollBookOfTier(EnchantTier.UNCOMMON);
    }
    private static ItemStack rollEpicBook() {
        return rollBookOfTier(EnchantTier.EPIC);
    }
    private static ItemStack rollBookOfTier(EnchantTier tier) {
        List<CustomEnchant> pool = new ArrayList<>();
        for (CustomEnchant e : EnchantRegistry.all()) if (e.getTier() == tier) pool.add(e);
        if (pool.isEmpty()) return new ItemStack(Material.PAPER);
        CustomEnchant chosen = pool.get(RNG.nextInt(pool.size()));
        return ItemFactories.book(chosen, 1 + RNG.nextInt(chosen.getMaxLevel()));
    }
}
