package com.soulenchants.loot;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Rolls boss loot. Always-drops fire 100%; rest is rolled by chance.
 * All drops spawn naturally at the boss's death location.
 */
public final class BossLootTable {

    private static final Random RNG = new Random();

    private BossLootTable() {}

    public static void dropIronGolem(Location loc) {
        World w = loc.getWorld();

        // Filler — always
        w.dropItemNaturally(loc, new ItemStack(org.bukkit.Material.IRON_INGOT, 16 + RNG.nextInt(17)));
        w.dropItemNaturally(loc, BossLootItems.colossusSlag(4 + RNG.nextInt(5)));

        // Common
        roll(w, loc, 0.80, BossLootItems.ironHeartFragment(2 + RNG.nextInt(3)));

        // Uncommon
        roll(w, loc, 0.50, BossLootItems.forgedEmber(1 + RNG.nextInt(3)));
        roll(w, loc, 0.40, BossLootItems.reinforcedPlating(1 + RNG.nextInt(2)));

        // Rare
        roll(w, loc, 0.20, BossLootItems.stoneskinTonic());
        roll(w, loc, 0.15, BossLootItems.earthshakerTreads());

        // Epic
        roll(w, loc, 0.05, BossLootItems.bulwarkCore());

        // Boss-tier
        roll(w, loc, 0.01,  BossLootItems.ironheartsHammer());
        roll(w, loc, 0.005, BossLootItems.colossusPlatingCore());
        roll(w, loc, 0.005, BossLootItems.heartOfTheForge());
    }

    public static void dropVeilweaver(Location loc) {
        World w = loc.getWorld();

        // Filler — always
        w.dropItemNaturally(loc, new ItemStack(org.bukkit.Material.ENDER_PEARL, 4 + RNG.nextInt(5)));
        w.dropItemNaturally(loc, BossLootItems.veilThread(4 + RNG.nextInt(5)));

        // Common
        roll(w, loc, 0.80, BossLootItems.frayedSoul(2 + RNG.nextInt(3)));

        // Uncommon
        roll(w, loc, 0.50, BossLootItems.echoingStrand(1 + RNG.nextInt(3)));
        roll(w, loc, 0.40, BossLootItems.phantomSilk(1 + RNG.nextInt(2)));

        // Rare
        roll(w, loc, 0.20, BossLootItems.phasingElixir());
        roll(w, loc, 0.15, BossLootItems.shadowstepSandals());

        // Epic
        roll(w, loc, 0.05, BossLootItems.veilEssence());

        // Boss-tier
        roll(w, loc, 0.01,  BossLootItems.loomOfEternity());
        roll(w, loc, 0.005, BossLootItems.veilseekersMantle());
        roll(w, loc, 0.005, BossLootItems.veilSigil());
    }

    private static void roll(World w, Location loc, double chance, ItemStack item) {
        if (RNG.nextDouble() < chance) w.dropItemNaturally(loc, item);
    }
}
