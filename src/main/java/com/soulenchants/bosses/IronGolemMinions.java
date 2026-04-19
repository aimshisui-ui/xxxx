package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.items.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Iron Sentinel minions that protect the Ironheart Colossus.
 *
 *   - 5 sentinels spawn in a ring around the boss on start
 *   - Each wears 4 pieces of iron armor + iron sword, each piece holds
 *     up to 5 custom enchants (PvP-focused)
 *   - On hit they apply Slowness II for 3s
 *   - When all 5 die, a 60s respawn cooldown begins; once elapsed they
 *     respawn as long as the boss is alive
 *   - Drops are gated to 30 Souls only (handled in EntityDeathListener)
 */
public final class IronGolemMinions {

    public static final String NBT_IG_MINION = "se_ig_minion";

    /** Cross-instance static registry of every live sentinel UUID. The death
     *  listener checks this set rather than relying on NBT, which has been
     *  unreliable on some entity types. */
    public static final java.util.Set<UUID> ACTIVE_UUIDS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final SoulEnchants plugin;
    private final IronGolemBoss boss;
    private final List<UUID> active = new ArrayList<>();
    private long respawnAt = 0L;
    private BukkitRunnable monitor;

    public IronGolemMinions(SoulEnchants plugin, IronGolemBoss boss) {
        this.plugin = plugin;
        this.boss = boss;
    }

    public void start() {
        spawnAll();
        monitor = new BukkitRunnable() {
            @Override public void run() {
                if (boss.getEntity().isDead()) { cancel(); return; }
                pruneDead();
                if (active.isEmpty() && respawnAt == 0L) {
                    respawnAt = System.currentTimeMillis() + 60_000L;
                    boss.getEntity().getWorld().playSound(boss.getEntity().getLocation(),
                            Sound.PORTAL_TRIGGER, 1.5f, 0.6f);
                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "✦ "
                            + ChatColor.YELLOW + "The Iron Sentinels have fallen — they will reforge in 60s.");
                }
                if (respawnAt > 0L && System.currentTimeMillis() >= respawnAt) {
                    respawnAt = 0L;
                    spawnAll();
                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "✦ "
                            + ChatColor.YELLOW + "The Iron Sentinels have reforged!");
                }
            }
        };
        monitor.runTaskTimer(plugin, 40L, 40L);
    }

    public void stop() {
        if (monitor != null) try { monitor.cancel(); } catch (Exception ignored) {}
        for (UUID id : new ArrayList<>(active)) {
            for (org.bukkit.World w : plugin.getServer().getWorlds()) {
                w.getEntities().stream()
                        .filter(e -> e.getUniqueId().equals(id))
                        .findFirst().ifPresent(org.bukkit.entity.Entity::remove);
            }
            ACTIVE_UUIDS.remove(id);
        }
        active.clear();
    }

    private void pruneDead() {
        active.removeIf(id -> {
            for (org.bukkit.World w : plugin.getServer().getWorlds()) {
                org.bukkit.entity.Entity e = w.getEntities().stream()
                        .filter(x -> x.getUniqueId().equals(id))
                        .findFirst().orElse(null);
                if (e != null && !e.isDead()) return false;
            }
            return true;
        });
    }

    private void spawnAll() {
        Location bossLoc = boss.getEntity().getLocation();
        for (int i = 0; i < 5; i++) {
            double angle = i * (Math.PI * 2 / 5);
            Location spawn = bossLoc.clone().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4);
            Zombie z = (Zombie) spawn.getWorld().spawnEntity(spawn, EntityType.ZOMBIE);
            z.setMaxHealth(160);
            z.setHealth(160);
            z.setCustomName(ChatColor.GRAY + "" + ChatColor.BOLD + "Iron Sentinel");
            z.setCustomNameVisible(true);
            z.setRemoveWhenFarAway(false);
            z.setBaby(false);
            // Permanent passive boosts — Strength III + Speed I
            z.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 2, false, false));
            z.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
            equipSentinel(z);
            try { new de.tr7zw.changeme.nbtapi.NBTEntity(z).setBoolean(NBT_IG_MINION, true); }
            catch (Throwable ignored) {}
            active.add(z.getUniqueId());
            ACTIVE_UUIDS.add(z.getUniqueId());
            // Spawn flair
            for (int j = 0; j < 12; j++)
                spawn.getWorld().playEffect(spawn.clone().add(0, 1, 0), Effect.STEP_SOUND, Material.IRON_BLOCK.getId());
            spawn.getWorld().playSound(spawn, Sound.IRONGOLEM_HIT, 1.0f, 0.8f);
        }
    }

    private void equipSentinel(Zombie z) {
        // Sword: heavy vanilla + custom PvP edge
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta sm = sword.getItemMeta();
        sm.setDisplayName(ChatColor.GRAY + "Sentinel Edge");
        sword.setItemMeta(sm);
        try {
            sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, 5);
            sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 1);
        } catch (Throwable ignored) {}
        sword = ItemUtil.addEnchant(sword, "lifesteal",  3);
        sword = ItemUtil.addEnchant(sword, "bleed",      3);
        sword = ItemUtil.addEnchant(sword, "executioner",2);
        sword = ItemUtil.addEnchant(sword, "cripple",    2);
        sword = ItemUtil.addEnchant(sword, "bonebreaker",2);
        z.getEquipment().setItemInHand(sword);
        z.getEquipment().setItemInHandDropChance(0f);

        // Helmet — 5 enchants
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        helmet = ItemUtil.addEnchant(helmet, "drunk", 2);
        helmet = ItemUtil.addEnchant(helmet, "nightvision", 1);
        helmet = ItemUtil.addEnchant(helmet, "saturation", 2);
        helmet = ItemUtil.addEnchant(helmet, "aquatic", 1);
        helmet = ItemUtil.addEnchant(helmet, "stormcaller", 2);
        z.getEquipment().setHelmet(helmet);
        z.getEquipment().setHelmetDropChance(0f);

        // Chestplate — 5 enchants
        ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE);
        chest = ItemUtil.addEnchant(chest, "berserk", 2);
        chest = ItemUtil.addEnchant(chest, "vital", 2);
        chest = ItemUtil.addEnchant(chest, "blessed", 2);
        chest = ItemUtil.addEnchant(chest, "armored", 2);
        chest = ItemUtil.addEnchant(chest, "enlightened", 1);
        z.getEquipment().setChestplate(chest);
        z.getEquipment().setChestplateDropChance(0f);

        // Leggings — 5 enchants
        ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
        legs = ItemUtil.addEnchant(legs, "hardened", 2);
        legs = ItemUtil.addEnchant(legs, "endurance", 2);
        legs = ItemUtil.addEnchant(legs, "ironclad", 2);
        legs = ItemUtil.addEnchant(legs, "antiknockback", 2);
        legs = ItemUtil.addEnchant(legs, "armored", 2);
        z.getEquipment().setLeggings(legs);
        z.getEquipment().setLeggingsDropChance(0f);

        // Boots — 5 enchants
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        boots = ItemUtil.addEnchant(boots, "speed", 2);
        boots = ItemUtil.addEnchant(boots, "depthstrider", 2);
        boots = ItemUtil.addEnchant(boots, "jumpboost", 2);
        boots = ItemUtil.addEnchant(boots, "featherweight", 2);
        boots = ItemUtil.addEnchant(boots, "firewalker", 1);
        z.getEquipment().setBoots(boots);
        z.getEquipment().setBootsDropChance(0f);
    }

    /** True if the given entity is one of our active sentinels. */
    public boolean isOurMinion(LivingEntity le) {
        return active.contains(le.getUniqueId());
    }
}
