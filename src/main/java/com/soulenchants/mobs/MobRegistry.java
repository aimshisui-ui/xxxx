package com.soulenchants.mobs;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Static catalog of every custom mob. Easy to extend — just add another
 * register(...) call.
 *
 * Spawn balance is handled by {@link MobSpawner}.
 */
public final class MobRegistry {

    private static final Map<String, CustomMob> ALL = new LinkedHashMap<>();

    private MobRegistry() {}

    public static CustomMob get(String id) { return ALL.get(id); }
    public static Collection<CustomMob> all() { return ALL.values(); }

    public static List<CustomMob> byTier(CustomMob.Tier tier) {
        List<CustomMob> out = new ArrayList<>();
        for (CustomMob m : ALL.values()) if (m.tier == tier) out.add(m);
        return out;
    }

    private static void add(CustomMob m) { ALL.put(m.id, m); }

    public static void register() {
        ALL.clear();

        // ════════════════════════════════════════════════════════════════════
        // EARLY GAME (T1) — 1-shot to a few hits, basic abilities
        // ════════════════════════════════════════════════════════════════════

        add(build("rotling",        "Rotling",            EntityType.ZOMBIE,   CustomMob.Tier.EARLY, 12,  1, 4,
                Arrays.asList(Abilities.speed(0)),
                null, true));

        add(build("plagued_walker", "Plagued Walker",     EntityType.ZOMBIE,   CustomMob.Tier.EARLY, 26,  1, 6,
                Arrays.asList(Abilities.hunger(6))));

        add(build("ash_walker",     "Ash-Walker",         EntityType.ZOMBIE,   CustomMob.Tier.EARLY, 30,  2, 7,
                Arrays.asList(Abilities.setOnFire(3), Abilities.fireResistance())));

        add(build("bone_pup",       "Bone Pup",           EntityType.SKELETON, CustomMob.Tier.EARLY, 18,  0, 5,
                Arrays.asList(Abilities.speed(1))));

        add(build("brittle_archer", "Brittle Archer",     EntityType.SKELETON, CustomMob.Tier.EARLY, 24,  0, 6,
                Arrays.asList(Abilities.bonusDamageOnHit(2))));

        add(build("husk_marauder",  "Husk Marauder",      EntityType.ZOMBIE,   CustomMob.Tier.EARLY, 32,  3, 8,
                Arrays.asList(Abilities.knockbackOnHit(0.6))));

        add(build("creeplet",       "Creeplet",           EntityType.CREEPER,  CustomMob.Tier.EARLY, 14,  0, 8,
                Arrays.asList(Abilities.deathExplode(8, 3))));

        add(build("web_lurker",     "Web Lurker",         EntityType.SPIDER,   CustomMob.Tier.EARLY, 22,  2, 6,
                Arrays.asList(Abilities.slow(0, 3),
                        // Spider AI de-aggros in daylight — keep the lurker
                        // locked onto the closest player so Broodmother's
                        // summoned swarm can't drift off.
                        Abilities.stickyTargeter(28.0))));

        add(build("cave_creeper",   "Cave Creeper",       EntityType.CAVE_SPIDER, CustomMob.Tier.EARLY, 18, 1, 7,
                Arrays.asList(Abilities.hitEffect(org.bukkit.potion.PotionEffectType.POISON, 0, 80))));

        add(build("silver_skitter", "Silver Skitter",     EntityType.SILVERFISH, CustomMob.Tier.EARLY, 8, 1, 3,
                Arrays.asList(Abilities.speed(2))));

        add(build("forest_wisp",    "Forest Wisp",        EntityType.SPIDER,   CustomMob.Tier.EARLY, 16,  1, 5,
                Arrays.asList(Abilities.invisibility(), Abilities.particleAura(Effect.HAPPY_VILLAGER, 1, 4))));

        add(build("gravewight",     "Gravewight",         EntityType.ZOMBIE,   CustomMob.Tier.EARLY, 28,  2, 7,
                Arrays.asList(Abilities.weakness(0, 4))));

        add(build("scorched_husk",  "Scorched Husk",      EntityType.ZOMBIE,   CustomMob.Tier.EARLY, 34,  3, 9,
                Arrays.asList(Abilities.fireResistance(), Abilities.setOnFire(5))));

        add(build("swamp_shambler", "Swamp Shambler",     EntityType.ZOMBIE,   CustomMob.Tier.EARLY, 25,  1, 5,
                Arrays.asList(Abilities.hitEffect(org.bukkit.potion.PotionEffectType.POISON, 0, 40))));

        add(build("tundra_runner",  "Tundra Runner",      EntityType.ZOMBIE,   CustomMob.Tier.EARLY, 20,  2, 6,
                Arrays.asList(Abilities.speed(2), Abilities.slow(0, 2))));

        add(build("gnoll_raider",   "Gnoll Raider",       EntityType.ZOMBIE,   CustomMob.Tier.EARLY, 35,  3, 8,
                Arrays.asList(Abilities.strength(0), Abilities.knockbackOnHit(0.4))));

        add(build("salt_crawler",   "Salt Crawler",       EntityType.SPIDER,   CustomMob.Tier.EARLY, 20,  1, 5,
                Arrays.asList(Abilities.hunger(4))));

        add(build("grave_piglet",   "Grave Piglet",       EntityType.PIG_ZOMBIE, CustomMob.Tier.EARLY, 28, 2, 9,
                Arrays.asList(Abilities.speed(0), Abilities.bonusDamageOnHit(2))));

        // ════════════════════════════════════════════════════════════════════
        // MID GAME (T2) — Special abilities, tougher fights
        // ════════════════════════════════════════════════════════════════════

        add(build("frostbite_archer","Frostbite Archer",  EntityType.SKELETON, CustomMob.Tier.MID,   45,  3, 18,
                Arrays.asList(Abilities.slow(1, 4), Abilities.bonusDamageOnHit(3),
                        Abilities.particleAura(Effect.SNOW_SHOVEL, 1, 3))));

        add(build("witchling",      "Witchling",          EntityType.WITCH,    CustomMob.Tier.MID,   50,  2, 22,
                Arrays.asList(Abilities.hitEffect(org.bukkit.potion.PotionEffectType.POISON, 1, 100),
                        Abilities.weakness(0, 6))));

        add(build("revenant",       "Revenant",           EntityType.SKELETON, CustomMob.Tier.MID,   60,  4, 22,
                Arrays.asList(Abilities.lifestealOnHit(0.30), Abilities.wither(4)),
                null, false, Skeleton.SkeletonType.WITHER));

        add(build("ember_imp",      "Ember Imp",          EntityType.BLAZE,    CustomMob.Tier.MID,   55,  3, 25,
                Arrays.asList(Abilities.fireResistance(), Abilities.fireballThrow(60),
                        Abilities.setOnFire(4))));

        add(build("voidstalker",    "Voidstalker",        EntityType.ENDERMAN, CustomMob.Tier.MID,   70,  5, 30,
                Arrays.asList(Abilities.teleportOnHit(8), Abilities.blind(3))));

        add(build("brute_zombie",   "Brute",              EntityType.ZOMBIE,   CustomMob.Tier.MID,   100, 6, 28,
                Arrays.asList(Abilities.strength(2), Abilities.knockbackOnHit(1.2),
                        Abilities.resistance(0))));

        add(build("plague_doctor",  "Plague Doctor",      EntityType.WITCH,    CustomMob.Tier.MID,   55,  3, 25,
                Arrays.asList(Abilities.hitEffect(org.bukkit.potion.PotionEffectType.POISON, 1, 120),
                        Abilities.hunger(8), Abilities.regenOnHurt(0.5))));

        add(build("razorshell",     "Razorshell",         EntityType.SPIDER,   CustomMob.Tier.MID,   65,  4, 22,
                Arrays.asList(Abilities.reflectPercent(0.20), Abilities.bonusDamageOnHit(4))));

        add(build("magma_jumper",   "Magma Jumper",       EntityType.MAGMA_CUBE, CustomMob.Tier.MID, 75, 5, 24,
                Arrays.asList(Abilities.fireResistance(), Abilities.setOnFire(4),
                        Abilities.leapAtPlayer(1.4, 0.7, 80))));

        add(build("gloomstalker",   "Gloomstalker",       EntityType.SKELETON, CustomMob.Tier.MID,   62,  4, 24,
                Arrays.asList(Abilities.invisibility(), Abilities.bonusDamageOnHit(5),
                        Abilities.lifestealOnHit(0.20))));

        add(build("soulgnawer",     "Soulgnawer",         EntityType.PIG_ZOMBIE, CustomMob.Tier.MID, 80, 4, 35,
                Arrays.asList(Abilities.stealSouls(15), Abilities.lifestealOnHit(0.40))));

        add(build("frostbite_brute","Frostbite Brute",    EntityType.ZOMBIE,   CustomMob.Tier.MID,   110, 6, 30,
                Arrays.asList(Abilities.strength(1), Abilities.slow(2, 4),
                        Abilities.knockbackOnHit(0.8))));

        add(build("cursed_archer",  "Cursed Archer",      EntityType.SKELETON, CustomMob.Tier.MID,   55,  4, 25,
                Arrays.asList(Abilities.wither(3), Abilities.bonusDamageOnHit(3)),
                null, false, Skeleton.SkeletonType.WITHER));

        add(build("silt_leviathan", "Silt Leviathan",     EntityType.SPIDER,   CustomMob.Tier.MID,   90,  5, 28,
                Arrays.asList(Abilities.slow(1, 5), Abilities.bonusDamageOnHit(4),
                        Abilities.resistance(0))));

        add(build("ghoul_king",     "Ghoul King",         EntityType.ZOMBIE,   CustomMob.Tier.MID,   85,  5, 30,
                Arrays.asList(Abilities.lifestealOnHit(0.25), Abilities.knockbackOnHit(1.0),
                        Abilities.regenOnHurt(0.3))));

        add(build("spectral_monk",  "Spectral Monk",      EntityType.SKELETON, CustomMob.Tier.MID,   60,  3, 24,
                Arrays.asList(Abilities.invisibility(), Abilities.blind(2),
                        Abilities.weakness(1, 4))));

        add(build("bogstriker",     "Bogstriker",         EntityType.WITCH,    CustomMob.Tier.MID,   70,  4, 28,
                Arrays.asList(Abilities.hitEffect(org.bukkit.potion.PotionEffectType.POISON, 1, 80),
                        Abilities.slow(1, 3), Abilities.hunger(6))));

        add(build("kinetic_abomination","Kinetic Abomination", EntityType.MAGMA_CUBE, CustomMob.Tier.MID, 95, 5, 32,
                Arrays.asList(Abilities.leapAtPlayer(1.8, 0.9, 60), Abilities.bonusDamageOnHit(5))));

        // ════════════════════════════════════════════════════════════════════
        // LATE GAME (T3) — Elite enemies with significant abilities
        // ════════════════════════════════════════════════════════════════════

        add(build("voidweaver",     "Voidweaver",         EntityType.ENDERMAN, CustomMob.Tier.LATE,  150, 8, 60,
                Arrays.asList(Abilities.teleportOnHit(12), Abilities.blind(5),
                        Abilities.bonusDamageOnHit(6), Abilities.particleAura(Effect.WITCH_MAGIC, 1, 5))));

        add(build("ironclad_revenant","Ironclad Revenant",EntityType.SKELETON, CustomMob.Tier.LATE,  180, 10, 70,
                Arrays.asList(Abilities.resistance(1), Abilities.strength(2),
                        Abilities.bonusDamageOnHit(8)),
                null, false, Skeleton.SkeletonType.WITHER));

        add(build("pyroclast",      "Pyroclast",          EntityType.BLAZE,    CustomMob.Tier.LATE,  140, 6, 65,
                Arrays.asList(Abilities.fireResistance(), Abilities.fireballThrow(40),
                        Abilities.setOnFire(8), Abilities.aoeBurst(12, 4, 200))));

        add(build("dread_specter",  "Dread Specter",      EntityType.SKELETON, CustomMob.Tier.LATE,  120, 6, 55,
                Arrays.asList(Abilities.invisibility(), Abilities.wither(6),
                        Abilities.lifestealOnHit(0.40), Abilities.teleportOnHit(6)),
                null, false, Skeleton.SkeletonType.WITHER));

        add(build("colossus_acolyte","Colossus Acolyte",  EntityType.IRON_GOLEM, CustomMob.Tier.LATE, 250, 12, 100,
                Arrays.asList(Abilities.resistance(0), Abilities.knockbackOnHit(1.5),
                        Abilities.aoeBurst(20, 5, 240))));

        add(build("veiled_huntress","Veiled Huntress",    EntityType.SKELETON, CustomMob.Tier.LATE,  140, 8, 70,
                Arrays.asList(Abilities.invisibility(), Abilities.bonusDamageOnHit(10),
                        Abilities.weakness(2, 6), Abilities.teleportOnHit(8))));

        add(build("soulrender",     "Soulrender",         EntityType.PIG_ZOMBIE, CustomMob.Tier.LATE, 175, 9, 90,
                Arrays.asList(Abilities.stealSouls(50), Abilities.wither(5),
                        Abilities.lifestealOnHit(0.50))));

        add(build("tempest_witch",  "Tempest Witch",      EntityType.WITCH,    CustomMob.Tier.LATE,  130, 6, 65,
                Arrays.asList(Abilities.hitEffect(org.bukkit.potion.PotionEffectType.POISON, 2, 140),
                        Abilities.wither(4), Abilities.regenOnHurt(1.5),
                        Abilities.nausea(4))));

        add(build("obsidian_titan", "Obsidian Titan",     EntityType.IRON_GOLEM, CustomMob.Tier.LATE, 220, 10, 85,
                Arrays.asList(Abilities.resistance(1), Abilities.knockbackOnHit(1.8),
                        Abilities.strength(1))));

        add(build("eclipse_reaper", "Eclipse Reaper",     EntityType.SKELETON, CustomMob.Tier.LATE,  115, 7, 75,
                Arrays.asList(Abilities.invisibility(), Abilities.wither(7),
                        Abilities.bonusDamageOnHit(9), Abilities.stealSouls(35)),
                null, false, Skeleton.SkeletonType.WITHER));

        add(build("molten_ravager", "Molten Ravager",     EntityType.MAGMA_CUBE, CustomMob.Tier.LATE, 160, 8, 70,
                Arrays.asList(Abilities.fireResistance(), Abilities.setOnFire(10),
                        Abilities.leapAtPlayer(2.0, 1.0, 60), Abilities.aoeBurst(10, 4, 180))));

        add(build("voidpiercer",    "Voidpiercer",        EntityType.SKELETON, CustomMob.Tier.LATE,  125, 7, 75,
                Arrays.asList(Abilities.teleportOnHit(10), Abilities.bonusDamageOnHit(12),
                        Abilities.weakness(1, 5))));

        // ════════════════════════════════════════════════════════════════════
        // ELITE (T4) — Mini-boss tier, rare spawns or specific encounters
        // ════════════════════════════════════════════════════════════════════

        add(build("doomsworn_titan","Doomsworn Titan",    EntityType.IRON_GOLEM, CustomMob.Tier.ELITE, 600, 18, 250,
                Arrays.asList(Abilities.resistance(1), Abilities.strength(3),
                        Abilities.knockbackOnHit(2.0), Abilities.aoeBurst(30, 6, 200),
                        Abilities.deathExplode(20, 5),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.reinforcedPlating(2), 0.40),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.bulwarkCore(), 0.05))));

        add(build("voidshaper",     "Voidshaper",         EntityType.ENDERMAN, CustomMob.Tier.ELITE, 350, 12, 200,
                Arrays.asList(Abilities.teleportOnHit(15), Abilities.bonusDamageOnHit(15),
                        Abilities.blind(8), Abilities.regenOnHurt(2.0),
                        Abilities.particleAura(Effect.PORTAL, 2, 8),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.phantomSilk(3), 0.40),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.veilEssence(), 0.05))));

        add(build("infernal_ravager","Infernal Ravager",  EntityType.BLAZE,    CustomMob.Tier.ELITE, 400, 10, 220,
                Arrays.asList(Abilities.fireResistance(), Abilities.fireballThrow(20),
                        Abilities.setOnFire(12), Abilities.aoeBurst(25, 5, 140),
                        Abilities.deathExplode(30, 6),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.forgedEmber(3), 0.50))));

        add(build("plague_matriarch","Plague Matriarch",  EntityType.WITCH,    CustomMob.Tier.ELITE, 320, 8, 180,
                Arrays.asList(Abilities.hitEffect(org.bukkit.potion.PotionEffectType.POISON, 2, 200),
                        Abilities.wither(8), Abilities.regenOnHurt(3.0),
                        Abilities.splitSpawn("plague_doctor", 2),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.frayedSoul(3), 0.50))));

        add(build("wretched_king",  "Wretched King",      EntityType.ZOMBIE,   CustomMob.Tier.ELITE, 450, 14, 210,
                Arrays.asList(Abilities.strength(2), Abilities.lifestealOnHit(0.50),
                        Abilities.knockbackOnHit(1.8), Abilities.splitSpawn("ghoul_king", 2),
                        Abilities.resistance(1),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.ironHeartFragment(4), 0.60),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.bulwarkCore(), 0.07))));

        add(build("archon_of_dusk", "Archon of Dusk",     EntityType.ENDERMAN, CustomMob.Tier.ELITE, 400, 14, 220,
                Arrays.asList(Abilities.teleportOnHit(20), Abilities.wither(10),
                        Abilities.blind(10), Abilities.lifestealOnHit(0.30),
                        Abilities.stealSouls(100),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.veilEssence(), 0.08),
                        Abilities.deathDropItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.GOLD), 0.15))));

        // Extra filler variety
        add(build("thorn_hatchling","Thorn Hatchling",    EntityType.SPIDER,   CustomMob.Tier.EARLY, 16,  1, 5,
                Arrays.asList(Abilities.reflectPercent(0.15))));

        add(build("bone_brute",     "Bone Brute",         EntityType.SKELETON, CustomMob.Tier.EARLY, 32,  3, 8,
                Arrays.asList(Abilities.strength(0), Abilities.bonusDamageOnHit(2))));

        add(build("pyre_kindling",  "Pyre Kindling",      EntityType.BLAZE,    CustomMob.Tier.EARLY, 22,  2, 9,
                Arrays.asList(Abilities.fireResistance(), Abilities.setOnFire(2))));

        add(build("marsh_hag",      "Marsh Hag",          EntityType.WITCH,    CustomMob.Tier.EARLY, 30,  2, 11,
                Arrays.asList(Abilities.slow(0, 3), Abilities.hunger(4))));

        add(build("glass_phantom",  "Glass Phantom",      EntityType.GHAST,    CustomMob.Tier.MID,   40,  5, 40,
                Arrays.asList(Abilities.invisibility(), Abilities.bonusDamageOnHit(6))));

        // ─────────────────────────────────────────────────────────────────
        //  CAVE RIFT roster — fills the Veil-cave encounter
        // ─────────────────────────────────────────────────────────────────

        add(build("shardling",         "Shardling",        EntityType.ZOMBIE,   CustomMob.Tier.LATE,  35,  5, 25,
                Arrays.asList(
                        Abilities.particleAura(org.bukkit.Effect.WITCH_MAGIC, 1, 4),
                        Abilities.wither(3),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.paleShard(1), 0.55)
                ), null, true /* baby */));

        add(build("echo_stalker",      "Echo Stalker",     EntityType.SKELETON, CustomMob.Tier.LATE,  60,  8, 50,
                Arrays.asList(
                        Abilities.teleportOnHit(8),
                        Abilities.blind(4),
                        Abilities.particleAura(org.bukkit.Effect.PORTAL, 2, 6),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.echoShard(1), 0.40)
                )));

        add(build("dripstone_crawler", "Dripstone Crawler",EntityType.CAVE_SPIDER, CustomMob.Tier.LATE, 50, 6, 45,
                Arrays.asList(
                        Abilities.leapAtPlayer(1.5, 0.7, 60),
                        Abilities.slow(1, 5),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.dripstoneTear(1), 0.45)
                )));

        add(build("gloommaw",          "Gloommaw",         EntityType.ZOMBIE,   CustomMob.Tier.LATE,  120, 14, 80,
                Arrays.asList(
                        Abilities.strength(1),
                        Abilities.wither(6),
                        Abilities.regenOnHurt(2),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.hollowFragment(1), 0.30)
                )));

        add(build("ruinwraith",        "Ruinwraith",       EntityType.SKELETON, CustomMob.Tier.LATE,  90, 12, 70,
                Arrays.asList(
                        Abilities.teleportOnHit(10),
                        Abilities.auraEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 0, 4),
                        Abilities.particleAura(org.bukkit.Effect.SMOKE, 3, 10),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.voidEssence(1), 0.20)
                ), null, false, org.bukkit.entity.Skeleton.SkeletonType.WITHER));

        // ── BOSS — The Hollow King (END-GAME tier) ─────────────────────
        // The hardest single fight in the plugin. 25k HP, 110 bonus dmg, slow and
        // measured attack pacing. Diamond armor + Veiled Edge sword. No stacked AOEs.
        add(build("hollow_king",       "The Hollow King",  EntityType.SKELETON, CustomMob.Tier.ELITE, 25000, 110, 5000,
                Arrays.asList(
                        Abilities.strength(3),                                                  // Strength IV
                        // No Resistance potion — diamond + Prot IV armor already gives ~75% reduction.
                        // Stacking Resistance III on top made him take ~3 dmg per hit which combined
                        // with regenOnHurt = net heal. He's still very tanky from gear alone.
                        Abilities.fireResistance(),
                        // Visual aura — purely cosmetic, no gameplay impact
                        Abilities.particleAura(org.bukkit.Effect.WITCH_MAGIC, 4, 12),
                        // Soft proximity debuffs — Slow only, no Weakness (let players fight)
                        Abilities.auraEffect(org.bukkit.potion.PotionEffectType.SLOW, 0, 6),
                        // Forced melee — endgame tier (Veilweaver=110, IronGolem=70). 1.2s cycle.
                        Abilities.meleeEnforcer(150, 3.0, 24),
                        // Signature AOE — 20s cooldown (was 10s) so it lands like a punctuation mark
                        Abilities.bossAttack(210, 8, 400, Arrays.asList(
                                "§5§l✦ §r\"§5§oYou are not the first. You will not be the last.§r§5§l\"",
                                "§5§l✦ §r\"§5§oDown here, your name is no shield.§r§5§l\"",
                                "§5§l✦ §r\"§5§oI ruled when the mountains were small.§r§5§l\"",
                                "§5§l✦ §r\"§5§oNow I rule what the mountains forgot.§r§5§l\"")),
                        // NEW — telegraphed meteor on a random player every ~30s
                        Abilities.meteorStrike(180, 4, 30, 600),
                        // NEW — chain lightning between players every ~40s
                        Abilities.chainLightning(120, 3, 8, 800),
                        // NEW — periodic court summon: 2 ruinwraiths every ~50s
                        Abilities.summonReinforcements("ruinwraith", 2, 1000),
                        // Idle taunts — much rarer (was every 19s, now every 60s) to cut chat noise
                        Abilities.ambientTaunt(40, 1200, Arrays.asList(
                                "§8§o\"They built a throne. He built a tomb. They are the same room.\"",
                                "§8§oA crown is a heavy thing. A hollow crown is heavier still.")),
                        // On-hit procs — wither + lifesteal, no AOE chains
                        Abilities.wither(8),
                        Abilities.lifestealOnHit(0.20),                                         // was 0.30 — softer
                        Abilities.splitSpawn("shardling", 6),                                   // 6 minions on death
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.crownOfTheHollow(), 1.0),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.hollowFragment(12), 1.0),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.voidEssence(5), 1.0),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.paleShard(16), 1.0),
                        Abilities.deathDropItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BOSS), 0.85),
                        Abilities.deathDropItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.GOLD), 1.0),
                        // Mythic 0.005% chance drops — endgame swords (Crimson Tongue / Wraithcleaver)
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.crimsonTongue(), 0.00005),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.wraithcleaver(), 0.00005)
                ), null, false, org.bukkit.entity.Skeleton.SkeletonType.WITHER));

        // ═══════════════════════════════════════════════════════════════════════
        //  GODSET-TIER BOSSES (v1.2) — tuned so unequipped players cannot survive
        //  the opening minute. All three use particle-heavy telegraphs + bossAttack
        //  cycles punctuated by taunts. Three-phase roster: swarmer, earth-caster,
        //  soul-caster. Each has a distinct approach gradient so godsets with
        //  different strengths (DPS / tank / sustain) each have a "best fit".
        // ═══════════════════════════════════════════════════════════════════════

        // BROODMOTHER — swarming spider warden. Web-trap aura + periodic minion
        // summons + venom burst. Tank the legs, kill the pups fast, then chunk her.
        add(build("broodmother", "The Broodmother", EntityType.SPIDER, CustomMob.Tier.ELITE, 18000, 110, 3500,
                Arrays.asList(
                        Abilities.strength(2),
                        Abilities.fireResistance(),
                        Abilities.particleAura(Effect.WITCH_MAGIC, 3, 14),
                        Abilities.auraEffect(org.bukkit.potion.PotionEffectType.SLOW, 0, 5),
                        Abilities.auraEffect(org.bukkit.potion.PotionEffectType.SLOW_DIGGING, 0, 5),
                        // Spider vanilla AI de-aggros targets in sunlight — keep her
                        // locked onto the nearest player every tick so she can't
                        // wander off in daylight fights.
                        Abilities.stickyTargeter(40.0),
                        // Hit triggers — venom + minor lifesteal
                        Abilities.hitEffect(org.bukkit.potion.PotionEffectType.POISON, 2, 140),
                        Abilities.lifestealOnHit(0.20),
                        // Forced melee (1.5s cadence, 3.0 reach)
                        Abilities.meleeEnforcer(100, 3.0, 30),
                        // Venom-cloud AoE every 24s — radius 8, 160 dmg, chat flavor
                        Abilities.bossAttack(160, 8, 480, Arrays.asList(
                                "§2§l✦ §r\"§2§oYour skin is a thin boundary.§r§2§l\"",
                                "§2§l✦ §r\"§2§oMine eat what yours bleeds.§r§2§l\"",
                                "§2§l✦ §r\"§2§oI lay where you stand. Always have.§r§2§l\"")),
                        // Summon a new wave of web-lurkers every ~35s
                        Abilities.summonReinforcements("web_lurker", 4, 700),
                        // Signature: every 15-30s, encase EVERY player in 30 blocks
                        // with a 2-block cobweb column that decays after ~4s.
                        Abilities.webTrap(300, 80),
                        Abilities.ambientTaunt(40, 1400, Arrays.asList(
                                "§8§o\"The walls breathe for her. The floor drinks for her.\"",
                                "§8§o\"You will not unknow the smell.\"")),
                        // Death spawns 8 pups + reagent haul
                        Abilities.splitSpawn("web_lurker", 8),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.frayedSoul(4), 1.0),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.phantomSilk(4), 1.0),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.veilThread(3), 0.75),
                        Abilities.deathDropItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BOSS), 0.70),
                        Abilities.deathDropItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.GOLD), 1.0),
                        // New PvE mythic at razor-thin odds — Graverend
                        Abilities.deathDropItem(com.soulenchants.mythic.MythicFactory.create("graverend"), 0.00005),
                        // Boss-tier gear with better odds — not mythic but strong.
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.wraithsteelAxe(), 0.06),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.paleAegis(),      0.04)
                )));

        // WURM-LORD — subterranean pig-zombie. Meteor-barrage phase + magma-cube
        // summons. Pushes players into cover; godset fire-resist is borderline
        // mandatory. Fire + earth theme, every attack kicks a dust column.
        add(build("wurm_lord", "The Wurm-Lord", EntityType.PIG_ZOMBIE, CustomMob.Tier.ELITE, 22000, 240, 4200,
                Arrays.asList(
                        Abilities.strength(3),
                        Abilities.fireResistance(),
                        Abilities.particleAura(Effect.MOBSPAWNER_FLAMES, 4, 20),
                        Abilities.auraEffect(org.bukkit.potion.PotionEffectType.SLOW, 0, 7),
                        // Hits set player on fire — knockback intentionally removed so
                        // he can't kite players into his own meteor drops by punting them.
                        Abilities.setOnFire(8),
                        Abilities.lifestealOnHit(0.25),
                        // Heavy melee pressure
                        Abilities.meleeEnforcer(220, 3.2, 28),
                        // Signature AOE — 25s cadence, radius 9, 260 dmg
                        Abilities.bossAttack(260, 9, 500, Arrays.asList(
                                "§4§l✦ §r\"§4§oI was buried. You invited me back.§r§4§l\"",
                                "§4§l✦ §r\"§4§oBreath of the mountain — mine now.§r§4§l\"",
                                "§4§l✦ §r\"§4§oYou will lie lower than I did.§r§4§l\"")),
                        // Signature: the Wurm dives underground, pops up beneath a
                        // random player, erupts for big damage + knock-up.
                        Abilities.burrowStrike(180, 40, 520),
                        // Summon magma jumpers every ~40s
                        Abilities.summonReinforcements("magma_jumper", 3, 800),
                        // Ground-burst every ~20s — flat 120 dmg in 6 blocks
                        Abilities.aoeBurst(120, 6, 400),
                        Abilities.ambientTaunt(40, 1200, Arrays.asList(
                                "§8§o\"The earth you love is a lid over an older thing.\"",
                                "§8§o\"I am what waited between the strata.\"")),
                        // Death spawns 4 magma cubes + loot
                        Abilities.splitSpawn("magma_jumper", 4),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.forgedEmber(5), 1.0),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.ironHeartFragment(4), 1.0),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.reinforcedPlating(3), 0.85),
                        Abilities.deathDropItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BOSS), 0.85),
                        Abilities.deathDropItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.GOLD), 1.0),
                        // New PvE mythic drop — Ruinhammer (axe, theme match)
                        Abilities.deathDropItem(com.soulenchants.mythic.MythicFactory.create("ruinhammer"), 0.00008),
                        // Boss-tier gear drops (fire theme).
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.emberforgeHarness(), 0.08),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.sunpiercerBlade(),   0.05)
                )));

        // CHOIRMASTER — soul-caster wither skeleton. Chain lightning + soul steal +
        // summoned monks. Low HP vs the other two, but his chain + steal can delete
        // an unshielded back-line in seconds. Dawnbringer shines here; Clarity helps
        // against the blind ticks.
        // Balance pass: every damage number in this block was nuked down. The
        // old chain-lightning at 240 dmg + a 210 base-bonus + 200-dmg
        // meleeEnforcer was deleting unshielded players in one tick. New
        // numbers leave Choirmaster painful but survivable — he's still
        // the back-line cleaner via chain + soul-mark, just not a one-shot.
        add(build("choirmaster", "The Choirmaster", EntityType.SKELETON, CustomMob.Tier.ELITE, 20000, 110, 4000,
                Arrays.asList(
                        Abilities.strength(1),
                        Abilities.fireResistance(),
                        Abilities.particleAura(Effect.WITCH_MAGIC, 4, 16),
                        Abilities.particleAura(Effect.PORTAL, 3, 10),
                        Abilities.auraEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 0, 7),
                        // Hit triggers — wither + blind (durations trimmed too)
                        Abilities.hitEffect(org.bukkit.potion.PotionEffectType.WITHER,    0, 80),
                        Abilities.hitEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 0, 60),
                        Abilities.stealSouls(50),
                        Abilities.lifestealOnHit(0.20),
                        Abilities.meleeEnforcer(110, 3.0, 26),
                        // Signature AOE — 22s cadence, 9-block radius, 140 dmg (was 280)
                        Abilities.bossAttack(140, 9, 440, Arrays.asList(
                                "§5§l✦ §r\"§5§oSing. Do not make me ask twice.§r§5§l\"",
                                "§5§l✦ §r\"§5§oEach voice is a coin. Your debt is immense.§r§5§l\"",
                                "§5§l✦ §r\"§5§oI was a church. Now I am only a bell.§r§5§l\"")),
                        // Chain lightning — was the one-shotter. 240 → 110,
                        // 4 bounces → 3, 8-block chain range unchanged.
                        Abilities.chainLightning(110, 3, 8, 360),
                        // Summon spectral monks every ~45s
                        Abilities.summonReinforcements("spectral_monk", 3, 900),
                        // Soul mark: 25/stack × 8 = 200 dmg detonation → 12/stack
                        // × 8 = 96 dmg ceiling. Same mechanic, far softer payoff.
                        Abilities.soulMark(12.0, 8, 10, 60),
                        Abilities.ambientTaunt(40, 1000, Arrays.asList(
                                "§8§o\"The hymn is not over. You are a rest between bars.\"",
                                "§8§o\"Every name is sung eventually.\"",
                                "§8§o\"I keep the ledger. You are in it.\"")),
                        // Death: 6 monks + loot
                        Abilities.splitSpawn("spectral_monk", 6),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.veilEssence(), 0.30),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.echoShard(3), 1.0),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.frayedSoul(4), 1.0),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.voidEssence(2), 0.65),
                        Abilities.deathDropItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.BOSS), 0.85),
                        Abilities.deathDropItem(com.soulenchants.shop.LootBox.item(com.soulenchants.shop.LootBox.Kind.GOLD), 1.0),
                        // New PvE mythic drop — Emberlash (sword, soul-caster theme: offering)
                        Abilities.deathDropItem(com.soulenchants.mythic.MythicFactory.create("emberlash"), 0.00008),
                        // Boss-tier gear drops (utility / ranged theme).
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.stormwardenBow(),   0.07),
                        Abilities.deathDropItem(com.soulenchants.loot.BossLootItems.runeforgedGreaves(), 0.06)
                ), null, false, org.bukkit.entity.Skeleton.SkeletonType.WITHER));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static CustomMob build(String id, String name, EntityType base, CustomMob.Tier tier,
                                   int hp, int bonusDmg, int souls, List<AbilitySpec> abilities) {
        return build(id, name, base, tier, hp, bonusDmg, souls, abilities, null, false, null);
    }
    private static CustomMob build(String id, String name, EntityType base, CustomMob.Tier tier,
                                   int hp, int bonusDmg, int souls, List<AbilitySpec> abilities,
                                   String biomeFilter, boolean baby) {
        return build(id, name, base, tier, hp, bonusDmg, souls, abilities, biomeFilter, baby, null);
    }
    private static CustomMob build(String id, String name, EntityType base, CustomMob.Tier tier,
                                   int hp, int bonusDmg, int souls, List<AbilitySpec> abilities,
                                   String biomeFilter, boolean baby, Skeleton.SkeletonType skType) {
        // bonus damage is already captured as a CustomMob field; no need to duplicate as an ability
        List<AbilitySpec> all = new ArrayList<>(abilities);
        return new CustomMob(id, name, base, tier, hp, bonusDmg, souls,
                Collections.emptyList(), all, biomeFilter, skType, baby);
    }
}
