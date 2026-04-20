package com.soulenchants.commands;

import com.soulenchants.SoulEnchants;
import com.soulenchants.bosses.IronGolemBoss;
import com.soulenchants.bosses.Veilweaver;
import com.soulenchants.bosses.attacks.ApocalypseInvuln;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /boss subcommands for boss control + admin debug.
 *   /boss list                  — show active bosses + their state
 *   /boss kill                  — instakill active boss(es) → triggers loot
 *   /boss debug                 — dump invuln state, crystal count, sentinel count
 *   /boss clearshield           — wipe Veilweaver crystal shield
 *   /boss clearinvuln           — force-clear all invuln states (perma-invuln rescue)
 *   /boss killminions           — wipe Veilweaver minions/clones + Iron Sentinels
 */
public class BossCommand implements CommandExecutor {

    private final SoulEnchants plugin;
    public BossCommand(SoulEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String sub = args[0].toLowerCase();

        if (sub.equals("kill")) {
            if (!isAdmin(sender)) return true;
            int killed = 0;
            Veilweaver vw = plugin.getVeilweaverManager().getActive();
            if (vw != null && !vw.getEntity().isDead()) {
                vw.getEntity().setHealth(0);
                killed++;
                sender.sendMessage("§a✦ Killed Veilweaver");
            }
            IronGolemBoss b = plugin.getIronGolemManager().getActive();
            if (b != null && !b.getEntity().isDead()) {
                b.getEntity().setHealth(0);
                killed++;
                sender.sendMessage("§a✦ Killed Ironheart Colossus");
            }
            if (killed == 0) sender.sendMessage("§7No active boss to kill.");
            return true;
        }

        if (sub.equals("list")) {
            sender.sendMessage("§5✦ §dActive bosses:");
            Veilweaver vw = plugin.getVeilweaverManager().getActive();
            if (vw != null && !vw.getEntity().isDead()) {
                sender.sendMessage("§7  ▸ §dVeilweaver §8| §fHP " + (int) vw.getEntity().getHealth()
                        + "§7/§f" + (int) vw.getEntity().getMaxHealth()
                        + " §8| §7phase " + vw.getPhase());
            }
            IronGolemBoss b = plugin.getIronGolemManager().getActive();
            if (b != null && !b.getEntity().isDead()) {
                sender.sendMessage("§7  ▸ §6Ironheart Colossus §8| §fHP " + (int) b.getEntity().getHealth()
                        + "§7/§f" + (int) b.getEntity().getMaxHealth());
            }
            if ((vw == null || vw.getEntity().isDead()) && (b == null || b.getEntity().isDead())) {
                sender.sendMessage("§7  none");
            }
            return true;
        }

        if (sub.equals("debug")) {
            if (!isAdmin(sender)) return true;
            sender.sendMessage("§5✦ §dBoss debug state:");
            Veilweaver vw = plugin.getVeilweaverManager().getActive();
            if (vw != null) {
                sender.sendMessage("§7  Veilweaver:");
                sender.sendMessage("§7    invulnerable=§f" + vw.isInvulnerable()
                        + " §8| §7apocalypseInvuln=§f" + ApocalypseInvuln.isInvuln(vw)
                        + " §8| §7crystalShield=§f" + (vw.getCrystals() != null && vw.getCrystals().anyAlive()));
                sender.sendMessage("§7    minions=§f" + vw.getMinions().size()
                        + " §8| §7clones=§f" + vw.getEchoClones().size());
            } else {
                sender.sendMessage("§7  Veilweaver: §8(none active)");
            }
            IronGolemBoss b = plugin.getIronGolemManager().getActive();
            if (b != null) {
                sender.sendMessage("§7  Ironheart Colossus:");
                sender.sendMessage("§7    invulnerable=§f" + b.isInvulnerable()
                        + " §8| §7sentinels=§f" + com.soulenchants.bosses.IronGolemMinions.ACTIVE_UUIDS.size());
            } else {
                sender.sendMessage("§7  Colossus: §8(none active)");
            }
            return true;
        }

        if (sub.equals("clearshield")) {
            if (!isAdmin(sender)) return true;
            Veilweaver vw = plugin.getVeilweaverManager().getActive();
            if (vw == null) { sender.sendMessage("§7No active Veilweaver."); return true; }
            if (vw.getCrystals() != null) vw.getCrystals().clearAll();
            sender.sendMessage("§a✦ Wiped crystal shield.");
            return true;
        }

        if (sub.equals("clearinvuln")) {
            if (!isAdmin(sender)) return true;
            Veilweaver vw = plugin.getVeilweaverManager().getActive();
            if (vw != null) {
                ApocalypseInvuln.clear(vw);
                vw.forceClearInvuln();
                if (vw.getCrystals() != null) vw.getCrystals().clearAll();
            }
            IronGolemBoss b = plugin.getIronGolemManager().getActive();
            if (b != null) b.forceClearInvuln();
            sender.sendMessage("§a✦ Cleared all boss invuln states.");
            return true;
        }

        if (sub.equals("simhit")) {
            if (!isAdmin(sender)) return true;
            if (!(sender instanceof Player)) { sender.sendMessage("§cPlayer only."); return true; }
            if (args.length < 2) { sender.sendMessage("§c/boss simhit <amount>"); return true; }
            double amount;
            try { amount = Double.parseDouble(args[1]); }
            catch (NumberFormatException ex) { sender.sendMessage("§cInvalid number."); return true; }
            Player p = (Player) sender;
            double before = p.getHealth();
            p.damage(amount);
            org.bukkit.scheduler.BukkitRunnable check = new org.bukkit.scheduler.BukkitRunnable() {
                @Override public void run() {
                    double after = p.isDead() ? 0 : p.getHealth();
                    double taken = before - after;
                    p.sendMessage("§5✦ §dSimulated " + amount + " raw → took §c" + String.format("%.2f", taken)
                            + " §7(reduction: " + String.format("%.1f%%", 100 * (1 - taken / amount)) + ")");
                }
            };
            check.runTaskLater(plugin, 1L);
            return true;
        }

        if (sub.equals("killminions")) {
            if (!isAdmin(sender)) return true;
            int killed = 0;
            Veilweaver vw = plugin.getVeilweaverManager().getActive();
            if (vw != null) {
                for (org.bukkit.entity.LivingEntity m : vw.getMinions()) if (m != null && !m.isDead()) { m.remove(); killed++; }
                for (org.bukkit.entity.LivingEntity c : vw.getEchoClones()) if (c != null && !c.isDead()) { c.remove(); killed++; }
                vw.getMinions().clear();
                vw.getEchoClones().clear();
            }
            IronGolemBoss b = plugin.getIronGolemManager().getActive();
            if (b != null && b.getMinions() != null) {
                b.getMinions().stop();
                killed += com.soulenchants.bosses.IronGolemMinions.ACTIVE_UUIDS.size();
            }
            sender.sendMessage("§a✦ Cleared §f" + killed + " §aminions.");
            return true;
        }

        help(sender);
        return true;
    }

    private boolean isAdmin(CommandSender s) {
        if (!s.hasPermission("soulenchants.admin")) {
            s.sendMessage("§cNo permission.");
            return false;
        }
        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage("§5✦ §dBoss commands:");
        s.sendMessage("§7  /boss list");
        s.sendMessage("§7  /boss kill §8(admin — instakill + loot)");
        s.sendMessage("§7  /boss debug §8(admin — invuln/crystal state)");
        s.sendMessage("§7  /boss clearshield §8(admin — wipe Veilweaver crystals)");
        s.sendMessage("§7  /boss clearinvuln §8(admin — rescue stuck-invuln bosses)");
        s.sendMessage("§7  /boss killminions §8(admin — wipe all minions)");
        s.sendMessage("§7  /boss simhit <amount> §8(admin — test how much dmg you'd actually take)");
    }
}
