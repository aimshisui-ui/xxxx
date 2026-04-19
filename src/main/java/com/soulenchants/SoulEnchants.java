package com.soulenchants;

import com.soulenchants.bosses.IronGolemManager;
import com.soulenchants.bosses.VeilweaverManager;
import com.soulenchants.commands.CECommand;
import com.soulenchants.commands.SoulsCommand;
import com.soulenchants.currency.SoulManager;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.gui.EnchantMenuGUI;
import com.soulenchants.listeners.*;
import com.soulenchants.scoreboard.ScoreboardManager;
import com.soulenchants.util.CooldownManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SoulEnchants extends JavaPlugin {

    private SoulManager soulManager;
    private VeilweaverManager veilweaverManager;
    private IronGolemManager ironGolemManager;
    private EnchantMenuGUI enchantMenu;
    private CooldownManager cooldownManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        EnchantRegistry.registerDefaults();
        this.soulManager = new SoulManager(getDataFolder());
        this.veilweaverManager = new VeilweaverManager(this);
        this.ironGolemManager = new IronGolemManager(this);
        this.cooldownManager = new CooldownManager();
        this.enchantMenu = new EnchantMenuGUI(this);
        this.scoreboardManager = new ScoreboardManager(this);

        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new VeilweaverDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new IronGolemDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GrindingListener(this), this);
        getServer().getPluginManager().registerEvents(new PvEDamageListener(this), this);
        getServer().getPluginManager().registerEvents(enchantMenu, this);

        BerserkTickTask tickTask = new BerserkTickTask(this);
        tickTask.start();
        getServer().getPluginManager().registerEvents(new ArmorChangeListener(this, tickTask), this);

        // Reset any lingering max-HP from a previous session, then let the tick re-apply correctly.
        for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
            p.setMaxHealth(20.0);
        }
        scoreboardManager.start();

        getCommand("souls").setExecutor(new SoulsCommand(this));
        getCommand("ce").setExecutor(new CECommand(this));

        getLogger().info("SoulEnchants enabled. " + EnchantRegistry.all().size() + " enchants registered.");
    }

    @Override
    public void onDisable() {
        // Reset player max HP so Vital boosts don't persist into the .dat file
        for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
            p.setMaxHealth(20.0);
        }
        if (scoreboardManager != null) scoreboardManager.stop();
        if (soulManager != null) soulManager.save();
        if (veilweaverManager != null && veilweaverManager.getActive() != null)
            veilweaverManager.getActive().stop(false);
        if (ironGolemManager != null && ironGolemManager.getActive() != null)
            ironGolemManager.getActive().stop(false);
        getLogger().info("SoulEnchants disabled, data saved.");
    }

    public SoulManager getSoulManager() { return soulManager; }
    public VeilweaverManager getVeilweaverManager() { return veilweaverManager; }
    public IronGolemManager getIronGolemManager() { return ironGolemManager; }
    public EnchantMenuGUI getEnchantMenu() { return enchantMenu; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
}
