package com.soulenchants.pets;

import com.soulenchants.SoulEnchants;
import com.soulenchants.style.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Glue listener:
 *
 *   • Right-click egg          → summon or despawn
 *   • Sneak + right-click egg  → trigger the pet's active ability (instead
 *                                 of toggling the summon)
 *   • Player kill              → onOwnerKill hook + grant XP for mob souls
 *   • Player damaged           → onOwnerHurt hook
 *   • Quit / world change      → despawn companion cleanly
 */
public final class PetListener implements Listener {

    private final SoulEnchants plugin;

    public PetListener(SoulEnchants plugin) { this.plugin = plugin; }

    // LOWEST priority so we short-circuit BEFORE Bukkit's vanilla item-use
    // logic (which would otherwise throw egg/expbottle/snowball pet eggs as
    // projectiles). Pair with setUseItemInHand(DENY) because setCancelled
    // alone doesn't stop throwables in 1.8.8 on every code path.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (!e.getAction().name().startsWith("RIGHT_CLICK")) return;
        Player p = e.getPlayer();
        ItemStack item = p.getItemInHand();
        if (!PetItem.isPetEgg(item)) return;
        e.setCancelled(true);
        try { e.setUseItemInHand(org.bukkit.event.Event.Result.DENY); } catch (Throwable ignored) {}

        String id = PetItem.idOf(item);
        Pet pet = PetRegistry.get(id);
        if (pet == null) {
            Chat.bad(p, "This egg references a pet type that is no longer registered.");
            return;
        }

        PetManager mgr = plugin.getPetManager();

        if (p.isSneaking()) {
            // Active ability path — egg must already be summoned
            PetManager.Companion c = mgr.getActive(p);
            if (c == null || !c.eggUid.equals(PetItem.uidOf(item))) {
                Chat.info(p, "Summon this pet first (right-click the egg) to use its active.");
                return;
            }
            boolean fired;
            try {
                fired = pet.onActivate(p, c.stand, c.level);
            } catch (Throwable t) {
                plugin.getLogger().warning("[pets] " + pet.getId() + " onActivate threw: " + t);
                fired = false;
            }
            if (fired) {
                p.playSound(p.getLocation(), org.bukkit.Sound.PORTAL_TRIGGER, 0.4f, 1.7f);
            }
            return;
        }

        // Toggle summon
        PetManager.Companion c = mgr.getActive(p);
        if (c != null && c.eggUid.equals(PetItem.uidOf(item))) {
            mgr.despawn(p, true);
        } else {
            mgr.summon(p, pet, item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        PetManager.Companion c = plugin.getPetManager().getActive(killer);
        if (c == null) return;
        try {
            c.pet.onOwnerKill(killer, c.stand, e.getEntity(), c.level);
        } catch (Throwable t) {
            plugin.getLogger().warning("[pets] " + c.pet.getId() + " onOwnerKill threw: " + t);
        }
        // XP: 4 per regular mob, ceil(souls/5) cap from the drop
        long xp = 4L;
        plugin.getPetManager().grantXp(killer, xp);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerHurt(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        PetManager.Companion c = plugin.getPetManager().getActive(p);
        if (c == null) return;
        try {
            c.pet.onOwnerHurt(p, c.stand, e.getFinalDamage(), c.level);
        } catch (Throwable t) {
            plugin.getLogger().warning("[pets] " + c.pet.getId() + " onOwnerHurt threw: " + t);
        }
    }

    /**
     * Owner-deals-melee hook. Fires when the owner hits another LivingEntity.
     * Currently wired for Hellhound's Bloodfrenzy lifesteal — other pets can
     * hook in via their own onOwnerDealsMelee method if we add one to the
     * base Pet class later.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerDealsMelee(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        PetManager.Companion c = plugin.getPetManager().getActive(p);
        if (c == null) return;
        // Hellhound routes the hit through its own hook for frenzy lifesteal
        if (c.pet instanceof com.soulenchants.pets.impl.HellhoundPet) {
            try { ((com.soulenchants.pets.impl.HellhoundPet) c.pet).onOwnerDealsMelee(p, e); }
            catch (Throwable t) {
                plugin.getLogger().warning("[pets] hellhound onOwnerDealsMelee threw: " + t);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getPetManager().despawn(e.getPlayer(), false);
    }

    /**
     * Safety net: if a projectile launches with a Player shooter whose main
     * hand is a pet egg, cancel the launch. Covers every throwable pet-egg
     * icon (expbottle, snowball, egg, ender pearl) without per-material
     * whitelisting — if it's a pet egg, it never flies.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPetEggThrown(org.bukkit.event.entity.ProjectileLaunchEvent e) {
        if (e.getEntity() == null) return;
        org.bukkit.projectiles.ProjectileSource src = e.getEntity().getShooter();
        if (!(src instanceof Player)) return;
        Player p = (Player) src;
        ItemStack held = p.getItemInHand();
        if (PetItem.isPetEgg(held)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerTeleportEvent e) {
        if (e.getFrom().getWorld() == null || e.getTo() == null) return;
        if (!e.getFrom().getWorld().equals(e.getTo().getWorld())) {
            // Just let the follow task re-teleport the companion; no-op here.
            // (Kept as a hook point for per-world pet bans if we add them.)
        }
    }
}
