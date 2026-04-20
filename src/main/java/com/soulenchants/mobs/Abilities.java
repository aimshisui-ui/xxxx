package com.soulenchants.mobs;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Data-driven ability factories. Every factory returns an {@link AbilitySpec}
 * (serializable {type, params}); each type has a runtime builder registered
 * with {@link AbilityFactory}. Edits from /ce loot mutate the spec's params
 * and the next spawn rebuilds the runtime behavior from the new values.
 */
public final class Abilities {

    private static final Random RNG = new Random();

    private Abilities() {}

    static { registerAll(); }

    // Called via forName on plugin load as well, to ensure registry populated.
    public static void init() { /* class-load side-effect registers all */ }

    // ── PASSIVE PERMA-EFFECTS ─────────────────────────────────────────────

    public static AbilitySpec permanentEffect(PotionEffectType type, int amp) {
        return AbilitySpec.of("permanent_effect", "effect", type.getName(), "amp", amp);
    }
    public static AbilitySpec strength(int amp)   { return permanentEffect(PotionEffectType.INCREASE_DAMAGE, amp); }
    public static AbilitySpec speed(int amp)      { return permanentEffect(PotionEffectType.SPEED, amp); }
    public static AbilitySpec resistance(int amp) { return permanentEffect(PotionEffectType.DAMAGE_RESISTANCE, amp); }
    public static AbilitySpec fireResistance()    { return permanentEffect(PotionEffectType.FIRE_RESISTANCE, 0); }
    public static AbilitySpec invisibility()      { return permanentEffect(PotionEffectType.INVISIBILITY, 0); }

    // ── ON-HIT (mob hits player) ──────────────────────────────────────────

    public static AbilitySpec hitEffect(PotionEffectType type, int amp, int durationTicks) {
        return AbilitySpec.of("hit_effect", "effect", type.getName(), "amp", amp, "duration_ticks", durationTicks);
    }
    public static AbilitySpec bonusDamageOnHit(double amount) {
        return AbilitySpec.of("bonus_damage", "amount", amount);
    }
    public static AbilitySpec lifestealOnHit(double percent) {
        return AbilitySpec.of("lifesteal", "percent", percent);
    }
    public static AbilitySpec setOnFire(int seconds) {
        return AbilitySpec.of("set_on_fire", "seconds", seconds);
    }
    public static AbilitySpec knockbackOnHit(double force) {
        return AbilitySpec.of("knockback", "force", force);
    }
    public static AbilitySpec stealSouls(int amount) {
        return AbilitySpec.of("steal_souls", "amount", amount, "chance", 0.30);
    }

    // ── ON-HURT ──────────────────────────────────────────────────────────

    public static AbilitySpec reflectPercent(double pct) {
        return AbilitySpec.of("reflect", "percent", pct);
    }
    public static AbilitySpec teleportOnHit(double range) {
        return AbilitySpec.of("teleport_on_hurt", "range", range, "chance", 0.30);
    }
    public static AbilitySpec regenOnHurt(double amount) {
        return AbilitySpec.of("regen_on_hurt", "amount", amount);
    }

    // ── ON-TICK ──────────────────────────────────────────────────────────

    public static AbilitySpec auraEffect(PotionEffectType type, int amp, int radius) {
        return AbilitySpec.of("aura", "effect", type.getName(), "amp", amp, "radius", radius);
    }
    public static AbilitySpec particleAura(Effect effect, int radius, int count) {
        return AbilitySpec.of("particle_aura", "effect", effect.name(), "radius", radius, "count", count);
    }
    public static AbilitySpec leapAtPlayer(double horizontal, double vertical, int cooldownTicks) {
        return AbilitySpec.of("leap", "horizontal", horizontal, "vertical", vertical, "cooldown_ticks", cooldownTicks);
    }
    public static AbilitySpec fireballThrow(int cooldownTicks) {
        return AbilitySpec.of("fireball", "cooldown_ticks", cooldownTicks);
    }
    public static AbilitySpec aoeBurst(double damage, int radius, int cooldownTicks) {
        return AbilitySpec.of("aoe_burst", "damage", damage, "radius", radius, "cooldown_ticks", cooldownTicks);
    }

    // ── ON-DEATH ─────────────────────────────────────────────────────────

    public static AbilitySpec deathExplode(double damage, int radius) {
        return AbilitySpec.of("death_explode", "damage", damage, "radius", radius);
    }
    public static AbilitySpec splitSpawn(String childMobId, int count) {
        return AbilitySpec.of("split_spawn", "child", childMobId, "count", count);
    }
    /**
     * Drop a fully-decorated ItemStack on death — preserves custom name, lore,
     * enchants, AND custom NBT tags (loot ids, lootbox ids, etc.). Stored as a
     * direct ItemStack reference so all NBT survives. Not YAML-persistable but
     * the registry holds these in memory, which is fine.
     */
    public static AbilitySpec deathDropItem(ItemStack item, double chance) {
        return AbilitySpec.of("death_drop",
                "item", item,                                   // full ItemStack reference
                "material", item.getType().name(),              // legacy fallback
                "amount", item.getAmount(),
                "chance", chance);
    }

    /** Cinematic boss attack — AOE + random taunt broadcast + sound. */
    public static AbilitySpec bossAttack(double damage, int radius, int cooldownTicks, java.util.List<String> taunts) {
        return AbilitySpec.of("boss_attack",
                "damage", damage, "radius", radius, "cooldown_ticks", cooldownTicks,
                "taunts", new java.util.ArrayList<>(taunts));
    }

    /** Periodic flavor broadcast — picks a random line from `lines` and broadcasts
     *  to players in `range`, on a long cooldown. No damage. */
    public static AbilitySpec ambientTaunt(int range, int cooldownTicks, java.util.List<String> lines) {
        return AbilitySpec.of("ambient_taunt",
                "range", range, "cooldown_ticks", cooldownTicks,
                "lines", new java.util.ArrayList<>(lines));
    }

    /** Telegraphed meteor — picks a random nearby player, paints a flame ring at
     *  their feet for `telegraph_ticks`, then drops AOE damage on the marked spot. */
    public static AbilitySpec meteorStrike(double damage, int radius, int telegraphTicks, int cooldownTicks) {
        return AbilitySpec.of("meteor_strike",
                "damage", damage, "radius", radius,
                "telegraph_ticks", telegraphTicks, "cooldown_ticks", cooldownTicks);
    }

    /** Lightning chains between players — first hop hits nearest, then jumps up
     *  to `chains` times within `range`. Damage falls off 30% per hop. */
    public static AbilitySpec chainLightning(double damage, int chains, int range, int cooldownTicks) {
        return AbilitySpec.of("chain_lightning",
                "damage", damage, "chains", chains, "range", range, "cooldown_ticks", cooldownTicks);
    }

    /** Periodic minion summon — spawns `count` of `child` mob near the boss on
     *  cooldown. The cooldown itself is the safety against minion overflow. */
    public static AbilitySpec summonReinforcements(String childId, int count, int cooldownTicks) {
        return AbilitySpec.of("summon_reinforcements",
                "child", childId, "count", count, "cooldown_ticks", cooldownTicks);
    }

    /** Forced melee — vanilla AI gets cancelled when the mob is busy with
     *  ability casts. This periodically swings for `damage` against the closest
     *  player within `reach` blocks, on every `interval_ticks` cycle. */
    public static AbilitySpec meleeEnforcer(double damage, double reach, int intervalTicks) {
        return AbilitySpec.of("melee_enforcer",
                "damage", damage, "reach", reach, "interval_ticks", intervalTicks);
    }

    // ── hit-effect convenience wrappers ──────────────────────────────────

    public static AbilitySpec wither(int seconds)           { return hitEffect(PotionEffectType.WITHER,    0,   seconds * 20); }
    public static AbilitySpec blind(int seconds)            { return hitEffect(PotionEffectType.BLINDNESS, 0,   seconds * 20); }
    public static AbilitySpec slow(int amp, int seconds)    { return hitEffect(PotionEffectType.SLOW,      amp, seconds * 20); }
    public static AbilitySpec weakness(int amp, int seconds){ return hitEffect(PotionEffectType.WEAKNESS,  amp, seconds * 20); }
    public static AbilitySpec nausea(int seconds)           { return hitEffect(PotionEffectType.CONFUSION, 0,   seconds * 20); }
    public static AbilitySpec hunger(int seconds)           { return hitEffect(PotionEffectType.HUNGER,    1,   seconds * 20); }

    // ─────────────────────────────────────────────────────────────────────
    //  Runtime executors — one per type. Each creates a fresh MobAbility
    //  (possibly with per-entity captured state) from the spec's params.
    // ─────────────────────────────────────────────────────────────────────

    private static PotionEffectType effect(String name) {
        PotionEffectType t = PotionEffectType.getByName(name);
        return t != null ? t : PotionEffectType.SPEED;
    }

    private static Effect bukkitEffect(String name) {
        try { return Effect.valueOf(name); } catch (Throwable t) { return Effect.SMOKE; }
    }

    private static void registerAll() {

        AbilityFactory.register("permanent_effect", spec -> {
            final PotionEffectType type = effect(spec.gets("effect", "SPEED"));
            final int amp = spec.geti("amp", 0);
            return new MobAbility() {
                @Override public void onSpawn(LivingEntity e) {
                    e.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amp, false, false));
                }
            };
        });

        AbilityFactory.register("hit_effect", spec -> {
            final PotionEffectType type = effect(spec.gets("effect", "SLOW"));
            final int amp = spec.geti("amp", 0);
            final int dur = spec.geti("duration_ticks", 60);
            return new MobAbility() {
                @Override public void onHitPlayer(LivingEntity a, Player v, EntityDamageByEntityEvent e) {
                    v.addPotionEffect(new PotionEffect(type, dur, amp, false, true), true);
                }
            };
        });

        AbilityFactory.register("bonus_damage", spec -> {
            final double amount = spec.getd("amount", 0.0);
            return new MobAbility() {
                @Override public void onHitPlayer(LivingEntity a, Player v, EntityDamageByEntityEvent e) {
                    e.setDamage(e.getDamage() + amount);
                }
            };
        });

        AbilityFactory.register("lifesteal", spec -> {
            final double pct = spec.getd("percent", 0.0);
            return new MobAbility() {
                @Override public void onHitPlayer(LivingEntity a, Player v, EntityDamageByEntityEvent e) {
                    double heal = e.getDamage() * pct;
                    a.setHealth(Math.min(a.getMaxHealth(), a.getHealth() + heal));
                }
            };
        });

        AbilityFactory.register("set_on_fire", spec -> {
            final int secs = spec.geti("seconds", 3);
            return new MobAbility() {
                @Override public void onHitPlayer(LivingEntity a, Player v, EntityDamageByEntityEvent e) {
                    v.setFireTicks(secs * 20);
                }
            };
        });

        AbilityFactory.register("knockback", spec -> {
            final double force = spec.getd("force", 1.0);
            return new MobAbility() {
                @Override public void onHitPlayer(LivingEntity a, Player v, EntityDamageByEntityEvent e) {
                    Vector kb = v.getLocation().toVector().subtract(a.getLocation().toVector()).setY(0);
                    if (kb.lengthSquared() > 0.0001) v.setVelocity(kb.normalize().multiply(force).setY(0.4));
                }
            };
        });

        AbilityFactory.register("steal_souls", spec -> {
            final int amt = spec.geti("amount", 1);
            final double chance = spec.getd("chance", 0.30);
            return new MobAbility() {
                @Override public void onHitPlayer(LivingEntity a, Player v, EntityDamageByEntityEvent e) {
                    if (RNG.nextDouble() > chance) return;
                    com.soulenchants.SoulEnchants pl = (com.soulenchants.SoulEnchants)
                            org.bukkit.Bukkit.getPluginManager().getPlugin("SoulEnchants");
                    if (pl != null && pl.getSoulManager().take(v, amt)) {
                        v.sendMessage(org.bukkit.ChatColor.RED + "✦ " + (a.getCustomName() != null ? a.getCustomName() : "A creature") + " §csiphoned §f" + amt + " §csouls.");
                        v.getWorld().playEffect(v.getLocation().add(0, 1, 0), Effect.WITCH_MAGIC, 0);
                    }
                }
            };
        });

        AbilityFactory.register("reflect", spec -> {
            final double pct = spec.getd("percent", 0.25);
            return new MobAbility() {
                @Override public void onHurt(LivingEntity v, EntityDamageEvent e) {
                    if (!(e instanceof EntityDamageByEntityEvent)) return;
                    Entity dmgr = ((EntityDamageByEntityEvent) e).getDamager();
                    if (!(dmgr instanceof Player)) return;
                    ((Player) dmgr).damage(e.getDamage() * pct, v);
                }
            };
        });

        AbilityFactory.register("teleport_on_hurt", spec -> {
            final double range = spec.getd("range", 6.0);
            final double chance = spec.getd("chance", 0.30);
            return new MobAbility() {
                @Override public void onHurt(LivingEntity v, EntityDamageEvent e) {
                    if (RNG.nextDouble() > chance) return;
                    Location to = v.getLocation().add(
                            (RNG.nextDouble() - 0.5) * range * 2, 0,
                            (RNG.nextDouble() - 0.5) * range * 2);
                    to.getWorld().playEffect(v.getLocation(), Effect.ENDER_SIGNAL, 0);
                    NaturalBehaviorBlocker.SCRIPTED_TELEPORT.set(true);
                    try { v.teleport(to); } finally { NaturalBehaviorBlocker.SCRIPTED_TELEPORT.set(false); }
                    to.getWorld().playEffect(to, Effect.ENDER_SIGNAL, 0);
                }
            };
        });

        AbilityFactory.register("regen_on_hurt", spec -> {
            final double amount = spec.getd("amount", 1.0);
            return new MobAbility() {
                @Override public void onHurt(LivingEntity v, EntityDamageEvent e) {
                    v.setHealth(Math.min(v.getMaxHealth(), v.getHealth() + amount));
                }
            };
        });

        AbilityFactory.register("aura", spec -> {
            final PotionEffectType type = effect(spec.gets("effect", "SLOW"));
            final int amp = spec.geti("amp", 0);
            final int radius = spec.geti("radius", 6);
            return new MobAbility() {
                @Override public void onTick(LivingEntity e) {
                    for (Entity near : e.getNearbyEntities(radius, radius, radius)) {
                        if (!(near instanceof Player)) continue;
                        ((Player) near).addPotionEffect(new PotionEffect(type, 60, amp, false, true), true);
                    }
                }
            };
        });

        AbilityFactory.register("particle_aura", spec -> {
            final Effect ef = bukkitEffect(spec.gets("effect", "SMOKE"));
            final int radius = spec.geti("radius", 2);
            final int count = spec.geti("count", 6);
            return new MobAbility() {
                @Override public void onTick(LivingEntity e) {
                    Location loc = e.getLocation().add(0, 1, 0);
                    for (int i = 0; i < count; i++) {
                        double angle = RNG.nextDouble() * Math.PI * 2;
                        Location p = loc.clone().add(Math.cos(angle) * radius, RNG.nextDouble() * 0.5, Math.sin(angle) * radius);
                        p.getWorld().playEffect(p, ef, ef == Effect.STEP_SOUND ? Material.OBSIDIAN.getId() : 0);
                    }
                }
            };
        });

        AbilityFactory.register("leap", spec -> {
            final double horz = spec.getd("horizontal", 1.2);
            final double vert = spec.getd("vertical", 0.6);
            final int cdTicks = spec.geti("cooldown_ticks", 60);
            return new MobAbility() {
                int cd = 0;
                @Override public void onTick(LivingEntity e) {
                    if (cd-- > 0) return;
                    Player nearest = null; double bestSq = Double.MAX_VALUE;
                    for (Entity near : e.getNearbyEntities(8, 4, 8)) {
                        if (!(near instanceof Player)) continue;
                        double d = near.getLocation().distanceSquared(e.getLocation());
                        if (d < bestSq) { bestSq = d; nearest = (Player) near; }
                    }
                    if (nearest == null || bestSq < 4) return;
                    cd = cdTicks / 20;
                    Vector dir = nearest.getLocation().toVector().subtract(e.getLocation().toVector()).normalize();
                    e.setVelocity(dir.multiply(horz).setY(vert));
                    e.getWorld().playSound(e.getLocation(), Sound.WOLF_GROWL, 1f, 1.4f);
                }
            };
        });

        AbilityFactory.register("fireball", spec -> {
            final int cdTicks = spec.geti("cooldown_ticks", 80);
            return new MobAbility() {
                int cd = 0;
                @Override public void onTick(LivingEntity e) {
                    if (cd-- > 0) return;
                    Player nearest = null; double bestSq = Double.MAX_VALUE;
                    for (Entity near : e.getNearbyEntities(15, 8, 15)) {
                        if (!(near instanceof Player)) continue;
                        double d = near.getLocation().distanceSquared(e.getLocation());
                        if (d < bestSq) { bestSq = d; nearest = (Player) near; }
                    }
                    if (nearest == null) return;
                    cd = cdTicks / 20;
                    Location origin = e.getEyeLocation();
                    Vector dir = nearest.getEyeLocation().toVector().subtract(origin.toVector()).normalize();
                    org.bukkit.entity.SmallFireball fb;
                    try { fb = (org.bukkit.entity.SmallFireball) origin.getWorld().spawnEntity(origin.add(dir), EntityType.SMALL_FIREBALL); }
                    catch (Throwable t) { return; }
                    fb.setDirection(dir.multiply(1));
                    fb.setShooter(e);
                    e.getWorld().playSound(e.getLocation(), Sound.GHAST_FIREBALL, 1f, 1.2f);
                }
            };
        });

        AbilityFactory.register("aoe_burst", spec -> {
            final double damage = spec.getd("damage", 4.0);
            final int radius = spec.geti("radius", 4);
            final int cdTicks = spec.geti("cooldown_ticks", 80);
            return new MobAbility() {
                int cd = 0;
                @Override public void onTick(LivingEntity e) {
                    if (cd-- > 0) return;
                    cd = cdTicks / 20;
                    Location loc = e.getLocation();
                    for (int i = 0; i < 30; i++) {
                        double a = i * (Math.PI * 2 / 30);
                        Location p = loc.clone().add(Math.cos(a) * radius, 0.3, Math.sin(a) * radius);
                        p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.REDSTONE_BLOCK.getId());
                    }
                    for (Entity near : e.getNearbyEntities(radius, 3, radius)) {
                        if (near instanceof Player) ((Player) near).damage(damage, e);
                    }
                    loc.getWorld().playSound(loc, Sound.BLAZE_HIT, 1.5f, 0.7f);
                }
            };
        });

        AbilityFactory.register("death_explode", spec -> {
            final double damage = spec.getd("damage", 6.0);
            final int radius = spec.geti("radius", 4);
            return new MobAbility() {
                @Override public void onDeath(LivingEntity e, Player killer) {
                    Location loc = e.getLocation();
                    loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0f, false);
                    for (Entity near : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                        if (near instanceof Player) ((Player) near).damage(damage, e);
                    }
                }
            };
        });

        AbilityFactory.register("split_spawn", spec -> {
            final String child = spec.gets("child", "");
            final int count = spec.geti("count", 2);
            return new MobAbility() {
                @Override public void onDeath(LivingEntity e, Player killer) {
                    CustomMob c = MobRegistry.get(child);
                    if (c == null) return;
                    for (int i = 0; i < count; i++) {
                        c.spawn(e.getLocation().add((RNG.nextDouble() - 0.5) * 2, 0, (RNG.nextDouble() - 0.5) * 2));
                    }
                }
            };
        });

        AbilityFactory.register("death_drop", spec -> {
            // Prefer the full ItemStack reference (preserves name + lore + NBT).
            // Fall back to material+amount for legacy specs that didn't store one.
            final Object stored = spec.params.get("item");
            final ItemStack baseItem;
            if (stored instanceof ItemStack) {
                baseItem = ((ItemStack) stored).clone();
            } else {
                Material m;
                try { m = Material.valueOf(spec.gets("material", "AIR")); } catch (Throwable t) { m = Material.AIR; }
                baseItem = new ItemStack(m, spec.geti("amount", 1));
            }
            final double chance = spec.getd("chance", 1.0);
            return new MobAbility() {
                @Override public void onDeath(LivingEntity e, Player killer) {
                    if (RNG.nextDouble() >= chance) return;
                    if (baseItem == null || baseItem.getType() == Material.AIR) return;
                    // Loot Filter — uses the unified helper so vanilla material ids
                    // ("vanilla:DIAMOND_SWORD") and custom NBT ids both resolve.
                    String lootId = com.soulenchants.loot.LootFilterManager.filterIdOf(baseItem);
                    if (lootId != null && killer != null) {
                        com.soulenchants.SoulEnchants pl = (com.soulenchants.SoulEnchants)
                                org.bukkit.Bukkit.getPluginManager().getPlugin("SoulEnchants");
                        if (pl != null && pl.getLootFilterManager() != null
                                && pl.getLootFilterManager().isFiltered(killer.getUniqueId(), lootId)) {
                            if (pl.getLootFilterManager().messagesEnabled(killer.getUniqueId())) {
                                String name = baseItem.getItemMeta() != null && baseItem.getItemMeta().getDisplayName() != null
                                        ? baseItem.getItemMeta().getDisplayName()
                                        : baseItem.getType().name();
                                killer.sendMessage(ChatColor.RED + "§l(!)§c " + name
                                        + ChatColor.RED + " filtered out — §7/lootfilter§c to edit.");
                            }
                            return;
                        }
                    }
                    e.getWorld().dropItemNaturally(e.getLocation(), baseItem.clone());
                }
            };
        });

        AbilityFactory.register("boss_attack", spec -> {
            final double damage = spec.getd("damage", 10.0);
            final int radius = spec.geti("radius", 5);
            final int cdTicks = spec.geti("cooldown_ticks", 200);
            Object t = spec.params.get("taunts");
            final java.util.List<String> taunts = (t instanceof java.util.List)
                    ? new java.util.ArrayList<String>() {{ for (Object o : (java.util.List<?>) t) add(String.valueOf(o)); }}
                    : java.util.Collections.<String>emptyList();
            return new MobAbility() {
                int cd = 0;
                @Override public void onTick(LivingEntity e) {
                    if (cd-- > 0) return;
                    cd = cdTicks / 20;
                    Location loc = e.getLocation();
                    String name = e.getCustomName() != null ? e.getCustomName() : "";
                    // Marquee taunt — title bar (NOT chat) so it doesn't pile up
                    String taunt = taunts.isEmpty() ? "" : taunts.get(RNG.nextInt(taunts.size()));
                    String titleText = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + name;
                    String subText   = ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + taunt;
                    // Wind-up sound
                    e.getWorld().playSound(loc, Sound.WITHER_SHOOT, 1.5f, 0.5f);
                    // Particle ring
                    for (int i = 0; i < 40; i++) {
                        double a = i * (Math.PI * 2 / 40);
                        Location p = loc.clone().add(Math.cos(a) * radius, 0.5, Math.sin(a) * radius);
                        p.getWorld().playEffect(p, Effect.STEP_SOUND, Material.OBSIDIAN.getId());
                        p.getWorld().playEffect(p.clone().add(0, 1, 0), Effect.WITCH_MAGIC, 0);
                    }
                    // Damage + title for nearby players
                    for (Entity near : e.getNearbyEntities(radius, radius / 2.0, radius)) {
                        if (near instanceof Player) {
                            Player p = (Player) near;
                            p.damage(damage, e);
                            try { p.sendTitle(titleText, subText); } catch (Throwable ignored) {}
                        }
                    }
                    e.getWorld().playSound(loc, Sound.WITHER_HURT, 2.0f, 0.4f);
                }
            };
        });

        AbilityFactory.register("ambient_taunt", spec -> {
            final int range = spec.geti("range", 30);
            final int cdTicks = spec.geti("cooldown_ticks", 240);
            Object t = spec.params.get("lines");
            final java.util.List<String> lines = (t instanceof java.util.List)
                    ? new java.util.ArrayList<String>() {{ for (Object o : (java.util.List<?>) t) add(String.valueOf(o)); }}
                    : java.util.Collections.<String>emptyList();
            return new MobAbility() {
                int cd = 0;
                @Override public void onTick(LivingEntity e) {
                    if (cd-- > 0) return;
                    cd = cdTicks / 20;
                    if (lines.isEmpty()) return;
                    String line = lines.get(RNG.nextInt(lines.size()));
                    String formatted = ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "  " + ChatColor.stripColor(line);
                    for (Entity near : e.getNearbyEntities(range, range, range)) {
                        if (near instanceof Player) ((Player) near).sendMessage(formatted);
                    }
                }
            };
        });

        AbilityFactory.register("meteor_strike", spec -> {
            final double damage = spec.getd("damage", 100.0);
            final int radius = spec.geti("radius", 4);
            final int telegraph = spec.geti("telegraph_ticks", 30);
            final int cdTicks = spec.geti("cooldown_ticks", 500);
            return new MobAbility() {
                int cd = 4; // brief initial delay before first cast
                @Override public void onTick(final LivingEntity e) {
                    if (cd-- > 0) return;
                    cd = cdTicks / 20;
                    java.util.List<Player> nearby = new java.util.ArrayList<>();
                    for (Entity n : e.getNearbyEntities(20, 8, 20)) if (n instanceof Player) nearby.add((Player) n);
                    if (nearby.isEmpty()) return;
                    final Player target = nearby.get(RNG.nextInt(nearby.size()));
                    final Location landing = target.getLocation().clone();
                    try {
                        target.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "✦ METEOR ✦",
                                         ChatColor.RED + "Move from the marked ground");
                    } catch (Throwable ignored) {}
                    e.getWorld().playSound(e.getLocation(), Sound.WITHER_SHOOT, 2.0f, 0.6f);
                    final com.soulenchants.SoulEnchants pl = (com.soulenchants.SoulEnchants)
                            org.bukkit.Bukkit.getPluginManager().getPlugin("SoulEnchants");
                    if (pl == null) return;
                    new BukkitRunnable() {
                        int t = 0;
                        @Override public void run() {
                            if (t++ >= telegraph / 2) {
                                landing.getWorld().createExplosion(landing.getX(), landing.getY(), landing.getZ(), 0f, false);
                                landing.getWorld().playSound(landing, Sound.EXPLODE, 2.0f, 0.7f);
                                landing.getWorld().strikeLightningEffect(landing);
                                for (Entity nn : landing.getWorld().getNearbyEntities(landing, radius, radius, radius)) {
                                    if (nn instanceof Player) ((Player) nn).damage(damage, e);
                                }
                                cancel();
                                return;
                            }
                            for (int i = 0; i < 24; i++) {
                                double a = i * (Math.PI * 2 / 24);
                                Location p = landing.clone().add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
                                p.getWorld().playEffect(p, Effect.MOBSPAWNER_FLAMES, 0);
                            }
                        }
                    }.runTaskTimer(pl, 0L, 2L);
                }
            };
        });

        AbilityFactory.register("chain_lightning", spec -> {
            final double damage = spec.getd("damage", 80.0);
            final int chains = spec.geti("chains", 3);
            final int range = spec.geti("range", 8);
            final int cdTicks = spec.geti("cooldown_ticks", 700);
            return new MobAbility() {
                int cd = 6;
                @Override public void onTick(LivingEntity e) {
                    if (cd-- > 0) return;
                    cd = cdTicks / 20;
                    Player current = null;
                    double bestSq = Double.MAX_VALUE;
                    for (Entity n : e.getNearbyEntities(15, 8, 15)) {
                        if (!(n instanceof Player)) continue;
                        double d = n.getLocation().distanceSquared(e.getLocation());
                        if (d < bestSq) { bestSq = d; current = (Player) n; }
                    }
                    if (current == null) return;
                    java.util.Set<java.util.UUID> hit = new java.util.HashSet<>();
                    double curDmg = damage;
                    Location lastLoc = e.getLocation();
                    for (int i = 0; i < chains && current != null; i++) {
                        hit.add(current.getUniqueId());
                        current.getWorld().strikeLightningEffect(current.getLocation());
                        current.damage(curDmg, e);
                        Vector dir = current.getLocation().toVector().subtract(lastLoc.toVector());
                        double dist = dir.length();
                        if (dist > 0.1) {
                            dir.normalize();
                            for (double d2 = 0; d2 < dist; d2 += 0.5) {
                                Location p = lastLoc.clone().add(dir.clone().multiply(d2));
                                p.getWorld().playEffect(p, Effect.MOBSPAWNER_FLAMES, 0);
                            }
                        }
                        lastLoc = current.getLocation();
                        curDmg *= 0.7;
                        Player next = null; double bestNextSq = Double.MAX_VALUE;
                        for (Entity n : current.getNearbyEntities(range, range, range)) {
                            if (!(n instanceof Player)) continue;
                            if (hit.contains(n.getUniqueId())) continue;
                            double d2 = n.getLocation().distanceSquared(current.getLocation());
                            if (d2 < bestNextSq) { bestNextSq = d2; next = (Player) n; }
                        }
                        current = next;
                    }
                }
            };
        });

        AbilityFactory.register("melee_enforcer", spec -> {
            final double damage = spec.getd("damage", 60.0);
            final double reach  = spec.getd("reach", 3.0);
            final int interval  = spec.geti("interval_ticks", 24);
            final double reachSq = reach * reach;
            return new MobAbility() {
                int t = 0;
                @Override public void onTick(LivingEntity e) {
                    if (++t < interval) return;
                    t = 0;
                    Player closest = null; double bestSq = Double.MAX_VALUE;
                    for (Entity n : e.getNearbyEntities(reach, reach, reach)) {
                        if (!(n instanceof Player)) continue;
                        double d = n.getLocation().distanceSquared(e.getLocation());
                        if (d < bestSq) { bestSq = d; closest = (Player) n; }
                    }
                    if (closest == null || bestSq > reachSq) return;
                    e.getWorld().playSound(e.getLocation(), Sound.HURT_FLESH, 1.0f, 0.8f);
                    closest.damage(damage, e);
                }
            };
        });

        AbilityFactory.register("summon_reinforcements", spec -> {
            final String child = spec.gets("child", "");
            final int count = spec.geti("count", 2);
            final int cdTicks = spec.geti("cooldown_ticks", 800);
            return new MobAbility() {
                int cd = 5;
                @Override public void onTick(LivingEntity e) {
                    if (cd-- > 0) return;
                    cd = cdTicks / 20;
                    CustomMob c = MobRegistry.get(child);
                    if (c == null) return;
                    for (int i = 0; i < count; i++) {
                        Location spawn = e.getLocation().add(
                                (RNG.nextDouble() - 0.5) * 4, 0, (RNG.nextDouble() - 0.5) * 4);
                        c.spawn(spawn);
                        for (int j = 0; j < 12; j++) spawn.getWorld().playEffect(
                                spawn.clone().add(0, 1, 0), Effect.PORTAL, 0);
                    }
                    e.getWorld().playSound(e.getLocation(), Sound.WITHER_SPAWN, 1.5f, 0.7f);
                    String tMain = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ THE COURT RISES ✦";
                    String tSub  = ChatColor.LIGHT_PURPLE + "The hollow ones answer the king";
                    for (Entity n : e.getNearbyEntities(30, 30, 30)) {
                        if (n instanceof Player) try { ((Player) n).sendTitle(tMain, tSub); } catch (Throwable ignored) {}
                    }
                }
            };
        });
    }
}
