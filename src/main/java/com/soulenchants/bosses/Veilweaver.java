package com.soulenchants.bosses;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.attacks.VeilweaverAttacks;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Veilweaver {

    public enum Phase { ONE, TWO, THREE }

    public static final double MAX_HP = 750.0;
    public static final String NBT_VEILWEAVER = "se_veilweaver";

    private final SoulEnchants plugin;
    private final LivingEntity entity;
    private final VeilweaverArena arena;
    private Phase phase = Phase.ONE;

    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private final List<LivingEntity> minions = new ArrayList<>();
    private final List<LivingEntity> echoClones = new ArrayList<>();

    private boolean invulnerable = false;
    private boolean usedFinalBind = false;
    private int ticks = 0;

    // Attack cooldowns (in ticks). Smaller initial values = faster engagement.
    private int cdThreadLash = 10;
    private int cdShatterBolt = 30;
    private int cdMinionWeave = 200;
    private int cdDimensionalRift = 80;
    private int cdLoomLaser = 120;
    private int cdEchoClones = 500;
    private int cdRealityFracture = 60;
    private int cdApocalypseWeave = 200;

    private BukkitRunnable tickTask;

    public Veilweaver(SoulEnchants plugin, Location spawnLoc) {
        this.plugin = plugin;
        this.arena = new VeilweaverArena(spawnLoc);
        Skeleton skel = (Skeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
        skel.setSkeletonType(Skeleton.SkeletonType.WITHER);
        this.entity = skel;
        configureEntity();
        de.tr7zw.changeme.nbtapi.NBTEntity nbt = new de.tr7zw.changeme.nbtapi.NBTEntity(entity);
        nbt.setBoolean(NBT_VEILWEAVER, true);
    }

    private void configureEntity() {
        entity.setMaxHealth(MAX_HP);
        entity.setHealth(MAX_HP);
        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(false);
        // Note: 1.8.8 has no Attribute API. Damage/armor scaling done via damage events;
        // movement speed defaults to wither skeleton baseline.
        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD);
        entity.getEquipment().setItemInHand(sword);
        entity.getEquipment().setItemInHandDropChance(0f);
        updateName();
    }

    public void start() {
        arena.spawn();
        plugin.getServer().broadcastMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD
                + "✦ The Veilweaver tears through reality! ✦");
        entity.getWorld().playSound(entity.getLocation(), Sound.WITHER_SPAWN, 2.0f, 0.6f);
        tickTask = new BukkitRunnable() {
            @Override public void run() { tick(); }
        };
        tickTask.runTaskTimer(plugin, 1L, 1L);
    }

    public void tick() {
        if (entity.isDead()) { stop(false); return; }
        ticks++;
        arena.tickBorder(entity);

        // Aggressive targeting — set/refresh hostile target every second
        if (ticks % 20 == 0 && entity instanceof org.bukkit.entity.Monster) {
            Player nearest = null;
            double bestSq = Double.MAX_VALUE;
            for (Player pl : arena.playersInArena()) {
                double d = pl.getLocation().distanceSquared(entity.getLocation());
                if (d < bestSq) { bestSq = d; nearest = pl; }
            }
            if (nearest != null) ((org.bukkit.entity.Monster) entity).setTarget(nearest);
        }

        // Heal if no players in arena (anti-kite)
        if (ticks % 160 == 0 && arena.noPlayersInArena()) {
            double heal = MAX_HP * 0.05;
            entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + heal));
        }

        // Constant ambient particles around boss (the "thread/runes" effect)
        Location loc = entity.getLocation().add(0, 1.2, 0);
        for (int i = 0; i < 4; i++) {
            double angle = (ticks * 0.2 + i * Math.PI / 2);
            double r = 1.2;
            Location p = loc.clone().add(Math.cos(angle) * r, Math.sin(ticks * 0.1) * 0.4, Math.sin(angle) * r);
            entity.getWorld().playEffect(p, Effect.WITCH_MAGIC, 0);
        }

        // Update name with HP bar
        if (ticks % 5 == 0) updateName();

        // Phase transitions
        double pct = entity.getHealth() / entity.getMaxHealth();
        if (phase == Phase.ONE && pct <= 0.65) transitionTo(Phase.TWO);
        else if (phase == Phase.TWO && pct <= 0.30) transitionTo(Phase.THREE);

        // Final Thread Bind triggers once below 10%
        if (!usedFinalBind && pct <= 0.10 && phase == Phase.THREE) {
            usedFinalBind = true;
            VeilweaverAttacks.finalThreadBind(this);
        }

        if (invulnerable) return;

        runAttacks();
    }

    private void runAttacks() {
        // Phase 1 attacks (always available in P1, plus carried into later phases)
        if (--cdThreadLash <= 0) { cdThreadLash = 24 + new Random().nextInt(8); VeilweaverAttacks.threadLash(this); }
        if (--cdShatterBolt <= 0) { cdShatterBolt = 60; VeilweaverAttacks.shatterBolt(this); }
        if (--cdMinionWeave <= 0) { cdMinionWeave = 500; VeilweaverAttacks.minionWeave(this); }

        if (phase == Phase.TWO || phase == Phase.THREE) {
            if (--cdDimensionalRift <= 0) { cdDimensionalRift = 160; VeilweaverAttacks.dimensionalRift(this); }
            if (--cdLoomLaser <= 0) { cdLoomLaser = 240; VeilweaverAttacks.loomLaser(this); }
            if (--cdEchoClones <= 0) { cdEchoClones = 700; VeilweaverAttacks.echoClones(this); }
        }

        if (phase == Phase.THREE) {
            if (--cdRealityFracture <= 0) { cdRealityFracture = 120; VeilweaverAttacks.realityFracture(this); }
            if (--cdApocalypseWeave <= 0) { cdApocalypseWeave = 400; VeilweaverAttacks.apocalypseWeave(this); }
        }
    }

    private void transitionTo(Phase next) {
        invulnerable = true;
        phase = next;
        Location loc = entity.getLocation();
        loc.getWorld().createExplosion(loc.getX(), loc.getY() + 1, loc.getZ(), 0f, false);
        loc.getWorld().playSound(loc, Sound.ENDERDRAGON_GROWL, 2.0f, 0.7f);
        // Flashier: triple lightning around boss
        for (int i = 0; i < 3; i++) {
            double a = i * (Math.PI * 2 / 3);
            Location strike = loc.clone().add(Math.cos(a) * 4, 0, Math.sin(a) * 4);
            strike.getWorld().strikeLightningEffect(strike);
        }
        for (Player p : arena.playersInArena()) {
            p.sendTitle(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "The Veil Tears",
                    ChatColor.GRAY + (next == Phase.TWO ? "Phase II — The Veil Tears" : "Phase III — The Shattered Loom"));
        }
        // Destroy 2 orbs on P2 transition; remaining on P3
        if (next == Phase.TWO) arena.destroyOrbs(2);
        else if (next == Phase.THREE) arena.destroyOrbs(99);
        // Boss floats up briefly
        entity.setVelocity(entity.getVelocity().setY(0.6));
        // Brief invuln
        new BukkitRunnable() {
            @Override public void run() { invulnerable = false; }
        }.runTaskLater(plugin, 60L);
    }

    public void onDamageDealt(Player attacker, double damage) {
        damageDealt.merge(attacker.getUniqueId(), damage, Double::sum);
    }

    public Player getTopDamager() {
        if (damageDealt.isEmpty()) return null;
        UUID top = Collections.max(damageDealt.entrySet(), Map.Entry.comparingByValue()).getKey();
        return plugin.getServer().getPlayer(top);
    }

    public double getIncomingDamageMultiplier(EntityDamageType type) {
        if (phase == Phase.TWO && type == EntityDamageType.ARROW) return 0.5;
        return 1.0;
    }

    public boolean isInvulnerable() { return invulnerable; }
    public Phase getPhase() { return phase; }
    public LivingEntity getEntity() { return entity; }
    public VeilweaverArena getArena() { return arena; }
    public List<LivingEntity> getMinions() { return minions; }
    public List<LivingEntity> getEchoClones() { return echoClones; }
    public SoulEnchants getPlugin() { return plugin; }

    public void stop(boolean killed) {
        if (tickTask != null) try { tickTask.cancel(); } catch (Exception ignored) {}
        for (LivingEntity m : minions) if (m != null && !m.isDead()) m.remove();
        for (LivingEntity c : echoClones) if (c != null && !c.isDead()) c.remove();
        arena.cleanup();
        if (killed) plugin.getServer().broadcastMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD
                + "✦ The Veilweaver has been silenced. ✦");
    }

    private void updateName() {
        double pct = entity.getHealth() / entity.getMaxHealth();
        int filled = (int) Math.round(pct * 20);
        StringBuilder bar = new StringBuilder(ChatColor.RED.toString());
        for (int i = 0; i < 20; i++) {
            if (i == filled) bar.append(ChatColor.DARK_GRAY);
            bar.append("|");
        }
        String phaseTag = phase == Phase.ONE ? "§7[I]" : phase == Phase.TWO ? "§5[II]" : "§4[III]";
        entity.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ The Veilweaver "
                + phaseTag + " " + bar + " §c" + (int) entity.getHealth() + "§7/§c" + (int) entity.getMaxHealth());
        entity.setCustomNameVisible(true);
    }

    public enum EntityDamageType { ARROW, MELEE, OTHER }
}
