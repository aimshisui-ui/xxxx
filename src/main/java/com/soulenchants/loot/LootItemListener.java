package com.soulenchants.loot;

import com.soulenchants.SoulEnchants;
import com.soulenchants.util.CooldownManager;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LootItemListener implements Listener {

    private final SoulEnchants plugin;
    private final CooldownManager cd = new CooldownManager();

    private boolean ready(java.util.UUID id, String type) { return cd.isReady(type, id); }
    private void start(java.util.UUID id, String type, int ticks) { cd.set(type, id, ticks * 50L); }
    private final Set<UUID> phantomInvuln = new HashSet<>();

    public LootItemListener(SoulEnchants plugin) {
        this.plugin = plugin;
        startPassiveTick();
    }

    // ── Right-click active items + consumables ─
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getItemInHand();
        String id = BossLootItems.getLootId(hand);
        if (id == null) return;

        switch (id) {
            case "ironhearts_hammer":
                if (!ready(p.getUniqueId(), "hammer")) {
                    p.sendMessage(ChatColor.GRAY + "Seismic Stomp recharging…");
                    return;
                }
                start(p.getUniqueId(), "hammer", 6 * 20);
                seismicStomp(p);
                e.setCancelled(true);
                break;
            case "loom_of_eternity":
                if (!ready(p.getUniqueId(), "loom")) {
                    p.sendMessage(ChatColor.GRAY + "Reality Tear recharging…");
                    return;
                }
                start(p.getUniqueId(), "loom", 8 * 20);
                realityTear(p);
                e.setCancelled(true);
                break;
            case "heart_of_the_forge":
                if (plugin.getLootProfile().addHeart(p.getUniqueId())) {
                    consume(p);
                    int stacks = plugin.getLootProfile().getHeartStacks(p.getUniqueId());
                    p.sendMessage(ChatColor.GOLD + "✦ Your heart binds to the Forge. " +
                            ChatColor.YELLOW + "+2 max HP" + ChatColor.GOLD +
                            " (stack " + stacks + "/10)");
                    p.getWorld().playSound(p.getLocation(), Sound.LEVEL_UP, 1f, 1.4f);
                } else {
                    p.sendMessage(ChatColor.RED + "Your heart cannot bear another forge.");
                }
                e.setCancelled(true);
                break;
            case "veil_sigil":
                if (plugin.getLootProfile().addSigil(p.getUniqueId())) {
                    consume(p);
                    int stacks = plugin.getLootProfile().getSigilStacks(p.getUniqueId());
                    p.sendMessage(ChatColor.DARK_PURPLE + "✦ The Veil whispers to you. " +
                            ChatColor.LIGHT_PURPLE + "+1 soul/kill" + ChatColor.DARK_PURPLE +
                            " (stack " + stacks + "/10)");
                    p.getWorld().playSound(p.getLocation(), Sound.PORTAL_TRIGGER, 0.7f, 1.6f);
                } else {
                    p.sendMessage(ChatColor.RED + "The Veil refuses you further.");
                }
                e.setCancelled(true);
                break;
            default:
        }
    }

    private void consume(Player p) {
        ItemStack hand = p.getItemInHand();
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else p.setItemInHand(null);
    }

    private void seismicStomp(Player p) {
        Location c = p.getLocation();
        c.getWorld().playSound(c, Sound.EXPLODE, 1.4f, 0.5f);
        for (int i = 0; i < 60; i++) {
            double a = i * (Math.PI * 2 / 60);
            Location ring = c.clone().add(Math.cos(a) * 4, 0.2, Math.sin(a) * 4);
            ring.getWorld().playEffect(ring, Effect.STEP_SOUND, org.bukkit.Material.STONE.getId());
        }
        for (Entity ent : p.getNearbyEntities(5, 3, 5)) {
            if (!(ent instanceof LivingEntity) || ent == p) continue;
            LivingEntity le = (LivingEntity) ent;
            le.damage(12, p);
            Vector knock = ent.getLocation().toVector().subtract(c.toVector()).setY(0.6).normalize().multiply(1.4);
            ent.setVelocity(knock);
        }
    }

    private void realityTear(Player p) {
        Location c = p.getEyeLocation();
        c.getWorld().playSound(c, Sound.ENDERMAN_TELEPORT, 1.2f, 0.8f);
        for (int i = 0; i < 30; i++) {
            double a = i * (Math.PI * 2 / 30);
            Location ring = c.clone().add(Math.cos(a) * 6, 0, Math.sin(a) * 6);
            ring.getWorld().playEffect(ring, Effect.WITCH_MAGIC, 0);
        }
        for (Entity ent : p.getNearbyEntities(8, 4, 8)) {
            if (!(ent instanceof LivingEntity) || ent == p) continue;
            LivingEntity le = (LivingEntity) ent;
            le.damage(14, p);
            Vector pull = c.toVector().subtract(ent.getLocation().toVector()).normalize().multiply(0.9).setY(0.3);
            ent.setVelocity(pull);
        }
    }

    // ── Passive: damage reduction (Plating Core / Mantle / Apex) ─
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();

        if (phantomInvuln.contains(p.getUniqueId())) {
            e.setCancelled(true);
            return;
        }

        ItemStack chest = p.getInventory().getChestplate();
        String id = BossLootItems.getLootId(chest);
        double mult = 1.0;
        if ("colossus_plating_core".equals(id)) mult -= 0.15;
        if ("veilseekers_mantle".equals(id))    mult -= 0.10;
        if ("apex_carapace".equals(id))         mult -= 0.20;
        if (mult < 1.0) e.setDamage(e.getDamage() * mult);

        // Fire resistance immunity check (passive)
        if (("colossus_plating_core".equals(id) || "apex_carapace".equals(id))
                && (e.getCause() == EntityDamageEvent.DamageCause.FIRE
                || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || e.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            e.setCancelled(true);
            return;
        }
    }

    // Low-HP procs evaluated AFTER damage is applied
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        final Player p = (Player) e.getEntity();
        new BukkitRunnable() {
            @Override public void run() {
                if (p.isDead()) return;
                ItemStack chest = p.getInventory().getChestplate();
                String id = BossLootItems.getLootId(chest);
                if (id == null) return;

                if (("colossus_plating_core".equals(id) || "apex_carapace".equals(id))
                        && p.getHealth() < 4.0
                        && ready(p.getUniqueId(), "platingcore")) {
                    start(p.getUniqueId(), "platingcore", 120 * 20);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 1, false, false), true);
                    p.getWorld().playSound(p.getLocation(), Sound.ANVIL_LAND, 0.8f, 1.5f);
                    p.sendMessage(ChatColor.GOLD + "✦ The Plating Core hardens! " + ChatColor.GRAY + "(120s CD)");
                }

                if (("veilseekers_mantle".equals(id) || "apex_carapace".equals(id))
                        && p.getHealth() < 6.0
                        && ready(p.getUniqueId(), "mantleblink")) {
                    start(p.getUniqueId(), "mantleblink", "apex_carapace".equals(id) ? 90 * 20 : 30 * 20);
                    phantomStep(p);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private void phantomStep(Player p) {
        Vector back = p.getLocation().getDirection().multiply(-6);
        Location target = p.getLocation().add(back);
        // Find a safe Y
        for (int i = 0; i < 4; i++) {
            if (target.getBlock().isEmpty() && target.clone().add(0, 1, 0).getBlock().isEmpty()) break;
            target.add(0, 1, 0);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, 1.2f);
        p.teleport(target);
        p.getWorld().playSound(target, Sound.ENDERMAN_TELEPORT, 1f, 1.4f);
        for (int i = 0; i < 20; i++) target.getWorld().playEffect(target, Effect.WITCH_MAGIC, 0);
        phantomInvuln.add(p.getUniqueId());
        new BukkitRunnable() {
            @Override public void run() { phantomInvuln.remove(p.getUniqueId()); }
        }.runTaskLater(plugin, 20L);
        p.sendMessage(ChatColor.DARK_PURPLE + "✦ Phantom Step!");
    }

    // ── Passive armor effects: Fire Resistance, Night Vision ─
    private void startPassiveTick() {
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    ItemStack chest = p.getInventory().getChestplate();
                    String id = BossLootItems.getLootId(chest);
                    if (id == null) continue;
                    if ("colossus_plating_core".equals(id) || "apex_carapace".equals(id)) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 220, 0, false, false), true);
                    }
                    if ("veilseekers_mantle".equals(id) || "apex_carapace".equals(id)) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0, false, false), true);
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 100L);
    }

    // ── Loom of Eternity ranged-hit bonus ─
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwordHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        String id = BossLootItems.getLootId(p.getItemInHand());
        if (!"loom_of_eternity".equals(id)) return;
        // 25% chance to apply 4s wither on hit — ranged-from-Veilweaver flavor
        if (Math.random() < 0.25 && e.getEntity() instanceof LivingEntity) {
            ((LivingEntity) e.getEntity()).addPotionEffect(
                    new PotionEffect(PotionEffectType.WITHER, 80, 0, false, false), true);
            e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.WITCH_MAGIC, 0);
        }
    }
}
