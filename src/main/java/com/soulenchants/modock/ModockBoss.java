package com.soulenchants.modock;

import com.soulenchants.SoulEnchants;
import com.soulenchants.util.BossHealthHack;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Modock, King of Atlantis — three-phase rift boss.
 *
 *   Phase 1 (100% -> 66% HP, in modock_phase1)
 *     - Lightning summons every 6s on random nearby player
 *     - Built-in lifesteal: heals 30% of every melee hit
 *     - Water trap on a 40s CD: encase a player in water blocks for 3s + root
 *     - Normal Atlantean minion waves every 20s (cap 4 alive)
 *
 *   Phase 2 (66% -> 33% HP, in modock_phase2)
 *     - All phase 1 abilities
 *     - 30% outgoing damage bonus when Modock is standing in water
 *     - 3 Royal Guard elite minions spawn on phase enter (no respawn)
 *     - Normal minion waves stop (only the elite guard now)
 *
 *   Phase 3 (33% -> 0% HP, in modock_phase3)
 *     - All phase 1 abilities, fast cooldowns (lightning 3s, water trap 20s)
 *     - Permanent Speed II
 *     - 1v1: all minions despawn on phase enter, no new spawns
 *
 * Phase transitions teleport the boss + all tracked participants to the next
 * world's configured boss/player spawn points. HP carries across phases.
 *
 * Damage map tracks per-player damage for top-damager + boss-killer rewards.
 */
public class ModockBoss {

    public enum Phase { ONE, TWO, THREE }

    public static final double MAX_HP = 25_000.0;
    public static final String NBT_MODOCK = "se_modock";

    private static final double PHASE2_HP = MAX_HP * 0.66;   // 16,500
    private static final double PHASE3_HP = MAX_HP * 0.33;   //  8,250

    private final SoulEnchants plugin;
    private final ModockSpawnConfig spawnCfg;
    private LivingEntity entity;
    private Phase phase = Phase.ONE;
    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private final List<LivingEntity> minions = new ArrayList<>();
    private final Set<UUID> participants = new HashSet<>();
    private final Random rng = new Random();
    private BukkitRunnable tickTask;
    private int ticks = 0;
    private int cdLightning   = 60;
    private int cdWaterTrap   = 200;
    private int cdMinionWave  = 200;
    private boolean phase2EntryDone = false;
    private boolean phase3EntryDone = false;
    private boolean stopped = false;

    // Track water blocks we placed for the trap so we can revert them
    private final List<TempBlock> tempWaterBlocks = new ArrayList<>();

    public ModockBoss(SoulEnchants plugin, ModockSpawnConfig spawnCfg) {
        this.plugin = plugin;
        this.spawnCfg = spawnCfg;
    }

    public LivingEntity getEntity()                  { return entity; }
    public Phase        getPhase()                   { return phase; }
    public Map<UUID, Double> getDamageDealt()        { return damageDealt; }
    public Set<UUID>    getParticipants()            { return participants; }
    public boolean      isAlive()                    { return entity != null && !entity.isDead(); }

    /** Spawn Modock for the first time at phase 1 spawn. Returns true on success. */
    public boolean spawn() {
        ModockSpawnConfig.Pair p1 = spawnCfg.get("phase1");
        if (p1 == null || p1.boss == null) {
            plugin.getLogger().warning("[modock] phase1 spawn not configured");
            return false;
        }
        Zombie z = (Zombie) p1.boss.getWorld().spawnEntity(p1.boss, EntityType.ZOMBIE);
        z.setBaby(false);
        z.setVillager(false);
        this.entity = z;
        configureEntity();
        new com.soulenchants.modock.ModockListener(plugin, this).register();
        startTick();
        announceSpawn();
        spawnNormalWave(2);
        return true;
    }

    private void configureEntity() {
        BossHealthHack.apply(entity, MAX_HP);
        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(false);
        // Equipment — Atlantean royal vibe: aqua-tinted leather, iron sword
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        entity.getEquipment().setItemInHand(sword);
        entity.getEquipment().setItemInHandDropChance(0f);
        entity.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        entity.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        entity.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        entity.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        for (int i = 0; i < 4; i++) {
            entity.getEquipment().setArmorContents(entity.getEquipment().getArmorContents()); // re-set
        }
        entity.getEquipment().setHelmetDropChance(0f);
        entity.getEquipment().setChestplateDropChance(0f);
        entity.getEquipment().setLeggingsDropChance(0f);
        entity.getEquipment().setBootsDropChance(0f);
        try {
            de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(entity);
            nbt.setBoolean(NBT_MODOCK, true);
        } catch (Throwable ignored) {}
        try {
            entity.setMetadata("se_modock",
                    new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        } catch (Throwable ignored) {}
        updateName();
    }

    private void announceSpawn() {
        String div = "§b" + ChatColor.STRIKETHROUGH + "                                          ";
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(div);
        Bukkit.broadcastMessage("§b§l        ✦ MODOCK, KING OF ATLANTIS RISES ✦");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§o  \"Drown in my dominion. The deep does not yield.\"");
        Bukkit.broadcastMessage("");
        Location loc = entity.getLocation();
        Bukkit.broadcastMessage("§7  Coordinates  §f" + loc.getBlockX() + ", "
                + loc.getBlockY() + ", " + loc.getBlockZ()
                + "§8  (" + loc.getWorld().getName() + ")");
        Bukkit.broadcastMessage("§c  ⚔ Three phases. Three worlds. No mercy.");
        Bukkit.broadcastMessage(div);
        Bukkit.broadcastMessage("");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 0.7f, 0.6f);
            p.playSound(p.getLocation(), Sound.AMBIENCE_THUNDER, 0.5f, 0.8f);
        }
        for (Player p : entity.getWorld().getPlayers()) {
            try { p.sendTitle("§b§l✦ MODOCK ✦", "§3§oKing of Atlantis"); } catch (Throwable ignored) {}
        }
    }

    private void startTick() {
        tickTask = new BukkitRunnable() {
            @Override public void run() { tick(); }
        };
        tickTask.runTaskTimer(plugin, 1L, 1L);
    }

    private void tick() {
        if (stopped) { try { tickTask.cancel(); } catch (Exception ignored) {} return; }
        if (entity == null || entity.isDead()) { stop(false); return; }
        ticks++;
        // HP-bar nametag refresh
        if (ticks % 5 == 0) updateName();

        // Phase transitions
        double hp = entity.getHealth();
        if (phase == Phase.ONE && hp <= PHASE2_HP) {
            transitionToPhase(Phase.TWO);
        } else if (phase == Phase.TWO && hp <= PHASE3_HP) {
            transitionToPhase(Phase.THREE);
        }

        // Aggressive targeting every second
        if (ticks % 20 == 0) {
            Player nearest = nearestPlayer(40.0);
            if (nearest != null) {
                try { ((Monster) entity).setTarget(nearest); } catch (Throwable ignored) {}
            }
        }

        // Melee enforcer — mirror our other bosses, force a hit at melee range
        if (ticks % 22 == 0) {
            Player closest = nearestPlayer(3.5);
            if (closest != null) {
                entity.getWorld().playSound(entity.getLocation(), Sound.HURT_FLESH, 1.0f, 0.7f);
                double base = phaseDamage();
                double dmg  = isInWater(entity) && phase != Phase.ONE ? base * 1.30 : base;
                applyOutgoing(closest, dmg);
            }
        }

        // ABILITIES — driven by per-attack cooldowns counted in ticks
        if (--cdLightning <= 0) {
            castLightning();
            cdLightning = phase == Phase.THREE ? 60 : 120;   // 3s in P3, 6s otherwise
        }
        if (--cdWaterTrap <= 0) {
            castWaterTrap();
            cdWaterTrap = phase == Phase.THREE ? 400 : 800;  // 20s in P3, 40s otherwise
        }
        if (--cdMinionWave <= 0) {
            // Phase 1 only — phase 2 has fixed elite guard, phase 3 is 1v1
            if (phase == Phase.ONE) spawnNormalWave(1);
            cdMinionWave = 400;
        }

        // Speed II in phase 3 if not already applied
        if (phase == Phase.THREE && ticks % 100 == 0) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, false), true);
        }

        // Cleanup expired temp water blocks
        long now = System.currentTimeMillis();
        Iterator<TempBlock> it = tempWaterBlocks.iterator();
        while (it.hasNext()) {
            TempBlock tb = it.next();
            if (now >= tb.expiresAt) {
                tb.restore();
                it.remove();
            }
        }
    }

    /** Phase-dependent base damage for melee + lightning. */
    private double phaseDamage() {
        if (phase == Phase.ONE)   return 9.0;
        if (phase == Phase.TWO)   return 12.0;
        return 14.0; // phase 3
    }

    private void castLightning() {
        Player target = nearestPlayer(40.0);
        if (target == null) return;
        Location at = target.getLocation();
        try { at.getWorld().strikeLightningEffect(at); } catch (Throwable ignored) {}
        double base = phaseDamage();
        double dmg  = isInWater(entity) && phase != Phase.ONE ? base * 1.30 : base;
        applyOutgoing(target, dmg);
        target.getWorld().playSound(target.getLocation(), Sound.AMBIENCE_THUNDER, 1.5f, 1.0f);
    }

    /** Pick the nearest player and box them in 4 water blocks for 3s with
     *  Slow VI + Jump(-129) so they can't move out. Tracked tempWaterBlocks
     *  get reverted automatically on expiry or boss death. */
    private void castWaterTrap() {
        Player target = nearestPlayer(20.0);
        if (target == null) return;
        Location feet = target.getLocation().getBlock().getLocation();
        long expires = System.currentTimeMillis() + 3_000L;
        // Box: 4 cardinal + 1 above the head
        int[][] offsets = {
                { 0, 0,  1}, { 0, 0, -1}, { 1, 0, 0}, {-1, 0, 0},
                { 0, 1,  1}, { 0, 1, -1}, { 1, 1, 0}, {-1, 1, 0},
                { 0, 2,  0},
        };
        for (int[] o : offsets) {
            Block b = feet.clone().add(o[0], o[1], o[2]).getBlock();
            // Don't overwrite anything solid — only air or water-like
            if (b.getType() == Material.AIR || b.getType() == Material.WATER
                    || b.getType() == Material.STATIONARY_WATER) {
                tempWaterBlocks.add(new TempBlock(b.getLocation(), b.getType(), b.getData(), expires));
                b.setType(Material.STATIONARY_WATER, false);
            }
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,            60, 5, false, false), true);
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,            60, 128, false, false), true);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 80, 0,   false, false), true);
        target.sendTitle("§b§l✦ DROWNED ✦", "§3§oThe deep takes you.");
        target.playSound(target.getLocation(), Sound.SPLASH, 1.5f, 0.8f);
    }

    /** Apply damage from Modock to the target. Routes through Bukkit.damage so
     *  our own enchant/proc listeners and damage-map updaters all run. */
    private void applyOutgoing(LivingEntity target, double dmg) {
        try { target.damage(dmg, entity); } catch (Throwable ignored) {}
        // Lifesteal: heal Modock 30% of damage dealt, capped at 200 HP per hit.
        // Scaled by any active anti-heal debuff (Bleed L4+ / Severance / Reaping Slash).
        double heal = com.soulenchants.listeners.CombatListener.scaleHealForAntiHeal(
                entity, Math.min(200.0, dmg * 0.30));
        entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + heal));
    }

    /** Notify damage from a player. Public — called by ModockListener on
     *  every EntityDamageByEntityEvent vs Modock so the damage map stays
     *  in sync regardless of who dealt the killing blow. */
    public void recordDamage(UUID playerId, double dmg) {
        if (dmg <= 0) return;
        damageDealt.merge(playerId, dmg, Double::sum);
        participants.add(playerId);
    }

    private void spawnNormalWave(int count) {
        // Cap alive minions so they don't accumulate when players retreat
        minions.removeIf(m -> m == null || m.isDead());
        if (minions.size() >= 4) return;
        World w = entity.getWorld();
        Location near = entity.getLocation();
        for (int i = 0; i < count; i++) {
            Location at = near.clone().add(rng.nextInt(7) - 3, 0, rng.nextInt(7) - 3);
            try {
                Zombie z = (Zombie) w.spawnEntity(at, EntityType.ZOMBIE);
                BossHealthHack.apply(z, 60.0);
                z.setRemoveWhenFarAway(false);
                z.getEquipment().setItemInHand(new ItemStack(Material.STONE_SWORD));
                z.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                z.getEquipment().setItemInHandDropChance(0f);
                z.getEquipment().setChestplateDropChance(0f);
                z.setCustomName("§3§lAtlantean Spearman");
                z.setCustomNameVisible(true);
                minions.add(z);
            } catch (Throwable ignored) {}
        }
    }

    private void spawnRoyalGuard() {
        World w = entity.getWorld();
        Location near = entity.getLocation();
        for (int i = 0; i < 3; i++) {
            double a = (Math.PI * 2 * i) / 3;
            Location at = near.clone().add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
            try {
                Zombie z = (Zombie) w.spawnEntity(at, EntityType.ZOMBIE);
                BossHealthHack.apply(z, 200.0);
                z.setRemoveWhenFarAway(false);
                z.getEquipment().setItemInHand(new ItemStack(Material.IRON_SWORD));
                z.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                z.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                z.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                z.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
                z.getEquipment().setItemInHandDropChance(0f);
                z.getEquipment().setHelmetDropChance(0f);
                z.getEquipment().setChestplateDropChance(0f);
                z.getEquipment().setLeggingsDropChance(0f);
                z.getEquipment().setBootsDropChance(0f);
                z.setCustomName("§b§l⚔ Royal Guard");
                z.setCustomNameVisible(true);
                minions.add(z);
            } catch (Throwable ignored) {}
        }
    }

    private void despawnAllMinions() {
        for (LivingEntity m : minions) {
            if (m != null && !m.isDead()) try { m.remove(); } catch (Throwable ignored) {}
        }
        minions.clear();
    }

    /** Phase change: TP boss + participants to next world, despawn minions,
     *  spawn phase-appropriate adds, broadcast a banner. */
    private void transitionToPhase(Phase next) {
        if (phase == next) return;
        Phase prev = phase;
        phase = next;
        if (next == Phase.TWO && phase2EntryDone) return;
        if (next == Phase.THREE && phase3EntryDone) return;
        if (next == Phase.TWO) phase2EntryDone = true;
        if (next == Phase.THREE) phase3EntryDone = true;

        String phaseKey = next == Phase.TWO ? "phase2" : "phase3";
        ModockSpawnConfig.Pair pair = spawnCfg.get(phaseKey);
        if (pair == null || pair.boss == null) {
            plugin.getLogger().warning("[modock] " + phaseKey + " spawn not configured — staying in current world");
            return;
        }

        // Keep current HP through the transition
        double curHp = entity.getHealth();
        despawnAllMinions();
        // Restore any temp water blocks BEFORE TP so we don't leave water in old world
        for (TempBlock tb : tempWaterBlocks) tb.restore();
        tempWaterBlocks.clear();

        // Move all participants first so they arrive ahead of the boss
        for (UUID u : new ArrayList<>(participants)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) {
                try {
                    p.teleport(pair.player);
                    p.sendTitle("§b§l✦ PHASE " + (next == Phase.TWO ? "II" : "III") + " ✦",
                            next == Phase.TWO ? "§3§oThe Royal Guard rises."
                                              : "§3§oOne warrior. One king. No witnesses.");
                    p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 0.6f, 0.5f);
                } catch (Throwable ignored) {}
            }
        }
        // TP boss
        try {
            entity.teleport(pair.boss);
            entity.setHealth(curHp);    // teleport sometimes resets, force back
        } catch (Throwable ignored) {}

        Bukkit.broadcastMessage("§b§l✦ MODOCK ENTERS PHASE " + (next == Phase.TWO ? "II" : "III") + " ✦");

        if (next == Phase.TWO) {
            spawnRoyalGuard();
        } else if (next == Phase.THREE) {
            // Permanent Speed II + faster CDs (handled via phase check in tick)
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        Integer.MAX_VALUE, 1, false, false), true);
            entity.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, false, false), true);
            cdLightning = 20;  // immediate ramp
        }
    }

    private boolean isInWater(LivingEntity e) {
        Block b = e.getLocation().getBlock();
        return b.getType() == Material.WATER || b.getType() == Material.STATIONARY_WATER;
    }

    private Player nearestPlayer(double maxDist) {
        Player best = null; double bestSq = maxDist * maxDist;
        for (Player p : entity.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(entity.getLocation());
            if (d < bestSq) { bestSq = d; best = p; }
        }
        return best;
    }

    private void updateName() {
        if (entity == null || entity.isDead()) return;
        double pct = entity.getHealth() / entity.getMaxHealth();
        int filled = (int) Math.round(pct * 20);
        ChatColor barColor = pct > 0.66 ? ChatColor.GREEN
                          : pct > 0.33 ? ChatColor.YELLOW : ChatColor.RED;
        StringBuilder bar = new StringBuilder(barColor.toString());
        for (int i = 0; i < 20; i++) {
            if (i == filled) bar.append(ChatColor.DARK_GRAY);
            bar.append('▮');
        }
        String pTag = phase == Phase.ONE ? "I" : phase == Phase.TWO ? "II" : "III";
        entity.setCustomName(ChatColor.AQUA + "" + ChatColor.BOLD + "✦ Modock "
                + ChatColor.DARK_AQUA + "[" + pTag + "] "
                + bar + " §3" + (int) entity.getHealth() + "§7/§3" + (int) entity.getMaxHealth());
        entity.setCustomNameVisible(true);
    }

    /** Stop everything — called on death or admin abort. */
    public void stop(boolean killed) {
        if (stopped) return;
        stopped = true;
        try { tickTask.cancel(); } catch (Throwable ignored) {}
        despawnAllMinions();
        for (TempBlock tb : tempWaterBlocks) tb.restore();
        tempWaterBlocks.clear();
        if (entity != null && !entity.isDead() && !killed) {
            try { entity.remove(); } catch (Throwable ignored) {}
        }
    }

    private static final class TempBlock {
        final Location loc;
        final Material origMat;
        final byte origData;
        final long expiresAt;
        TempBlock(Location loc, Material m, byte d, long expiresAt) {
            this.loc = loc; this.origMat = m; this.origData = d; this.expiresAt = expiresAt;
        }
        void restore() {
            try {
                Block b = loc.getBlock();
                b.setType(origMat, false);
                b.setData(origData, false);
            } catch (Throwable ignored) {}
        }
    }
}
