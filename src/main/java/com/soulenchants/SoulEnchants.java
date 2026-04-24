package com.soulenchants;

import com.soulenchants.bosses.IronGolemManager;
import com.soulenchants.bosses.VeilweaverManager;
import com.soulenchants.commands.BlessCommand;
import com.soulenchants.commands.CECommand;
import com.soulenchants.commands.SoulsCommand;
import com.soulenchants.config.ConfigBinder;
import com.soulenchants.config.EnchantConfig;
import com.soulenchants.config.MythicConfig;
import com.soulenchants.currency.SoulManager;
import com.soulenchants.enchants.EnchantRegistry;
import com.soulenchants.gui.EnchantMenuGUI;
import com.soulenchants.listeners.*;
import com.soulenchants.lunar.LunarBridge;
import com.soulenchants.lunar.LunarPingListener;
import com.soulenchants.masks.MaskAttachListener;
import com.soulenchants.masks.MaskCommand;
import com.soulenchants.masks.MaskPacketInjector;
import com.soulenchants.masks.MaskRegistry;
import com.soulenchants.mythic.MythicAuraTask;
import com.soulenchants.mythic.MythicCommand;
import com.soulenchants.mythic.MythicListener;
import com.soulenchants.mythic.MythicRegistry;
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
    private BerserkTickTask tickTask;
    private com.soulenchants.loot.LootProfile lootProfile;
    private com.soulenchants.gui.GodMenuGUI godMenu;
    private com.soulenchants.shop.ShopFeatured shopFeatured;
    private com.soulenchants.shop.ShopGUI shopGUI;
    private com.soulenchants.shop.LootBoxRevealGUI lootBoxRevealGUI;
    private com.soulenchants.quests.QuestManager questManager;
    private com.soulenchants.quests.QuestGUI questGUI;
    private com.soulenchants.mobs.MobListener mobListener;
    private com.soulenchants.config.LootConfig lootConfig;
    private com.soulenchants.gui.LootEditorGUI lootEditorGUI;
    private com.soulenchants.rifts.RiftSpawnConfig riftSpawnConfig;
    private com.soulenchants.rifts.VoidRiftManager voidRiftManager;
    private com.soulenchants.rifts.HologramConfig hologramConfig;
    private com.soulenchants.rifts.HologramManager hologramManager;
    private com.soulenchants.rifts.HologramGUI hologramGUI;
    private com.soulenchants.gui.RecipeGUI recipeGUI;
    private com.soulenchants.loot.LootFilterManager lootFilterManager;
    private com.soulenchants.gui.LootFilterGUI lootFilterGUI;
    private com.soulenchants.scoreboard.PvPStats pvpStats;
    private com.soulenchants.guilds.GuildManager guildManager;
    private com.soulenchants.listeners.ClarityTask clarityTask;
    private com.soulenchants.sets.SetManager setManager;
    private com.soulenchants.modock.ModockSpawnConfig modockSpawnConfig;
    private com.soulenchants.modock.ModockManager modockManager;
    private EnchantConfig enchantConfig;
    private MythicConfig mythicConfig;
    private MythicAuraTask mythicAuraTask;
    private MaskPacketInjector maskPacketInjector;
    private LunarPingListener lunarPingListener;
    private com.soulenchants.pets.PetManager petManager;

    /** Phase-4 god-class split — onEnable is now a lifecycle list, not a body.
     *  Every phase below is wrapped in safePhase() so a throwing subsystem
     *  marks itself failed and the plugin keeps loading the rest.
     *  Phase order is load-bearing; see each method's header comment. */
    @Override
    public void onEnable() {
        System.err.println(">>> SE onEnable starting");
        saveDefaultConfig();
        safePhase("cleanup-infra", this::initCleanupInfrastructure);
        safePhase("world-bootstrap", this::initWorlds);
        safePhase("configs",         this::initConfigs);
        safePhase("registries",      this::initRegistries);
        safePhase("managers",        this::initCoreManagers);
        safePhase("mobs",            this::initMobs);
        safePhase("listeners",       this::initListeners);
        safePhase("tick-tasks",      this::initTickTasks);
        safePhase("commands",        this::initCommands);
        safePhase("rifts",           this::initRifts);
        safePhase("recipe-gui",      this::initRecipeGui);
        safePhase("loot-filter",     this::initLootFilter);
        safePhase("transmog",        this::initTransmog);
        safePhase("pvp-stats",       this::initPvpStats);
        safePhase("guilds",          this::initGuilds);
        safePhase("armor-sets",      this::initArmorSets);
        safePhase("modock",          this::initModock);
        safePhase("mythics",         this::initMythicSubsystem);
        safePhase("masks",           this::initMaskSubsystem);
        safePhase("soul-gems",       this::initSoulGems);
        safePhase("pets",            this::initPets);
        safePhase("lunar",           this::initLunar);
        safePhase("tab-completion",  this::initTabCompletion);
        getLogger().info("SoulEnchants enabled. " + EnchantRegistry.all().size() + " enchants, "
                + MythicRegistry.all().size() + " mythics, " + MaskRegistry.all().size() + " masks.");
    }

    /** Run an init phase in isolation — a throwing subsystem logs + marks
     *  itself failed but the rest of the plugin still loads. Replaces the
     *  previous monolithic onEnable body where any unhandled Throwable
     *  left the plugin in half-loaded state. */
    private void safePhase(String name, Runnable phase) {
        try {
            phase.run();
        } catch (Throwable t) {
            getLogger().severe("[phase:" + name + "] FAILED — " + t);
            t.printStackTrace();
        }
    }

    // ────────────────────── phases (exact-order-preserving) ──────────────

    /** Phase 1 — cleanup infrastructure MUST load first: MapManager's quit
     *  listener evicts UUID-cache entries for every subsystem below, and
     *  TempBlockTracker recovers any blocks a prior crash left in the world. */
    private void initCleanupInfrastructure() {
        com.soulenchants.util.MapManager.install(this);
        com.soulenchants.util.TempBlockTracker.install(this);
        com.soulenchants.util.BossHealthHack.raise();
    }

    /** Void Rift + Modock arena worlds — load up-front so first-time access
     *  doesn't pause the server thread mid-fight. */
    private void initWorlds() {
        com.soulenchants.rifts.RiftWorld.ensure(this);
        com.soulenchants.modock.ModockWorlds.ensureAll(this);
    }

    /** Reflection-bound YAML config loading. Defaults match prior hardcoded values. */
    private void initConfigs() {
        this.enchantConfig = new EnchantConfig();
        ConfigBinder.bind(this, "enchants.yml", enchantConfig);
        this.mythicConfig = new MythicConfig();
        ConfigBinder.bind(this, "mythics.yml", mythicConfig);
    }

    /** Static registries — must load before any Manager tries to resolve an id. */
    private void initRegistries() {
        EnchantRegistry.registerDefaults();
        MythicRegistry.registerDefaults(this, mythicConfig);
        MaskRegistry.registerDefaults();
    }

    /** Core managers — souls, boss handles, scoreboard, loot/shop. Order
     *  matters: SoulManager reads its YAML, VeilweaverManager + IronGolemManager
     *  are referenced by listeners initialised in initListeners(). */
    private void initCoreManagers() {
        this.soulManager = new SoulManager(this, getDataFolder());
        this.veilweaverManager = new VeilweaverManager(this);
        this.ironGolemManager = new IronGolemManager(this);
        this.cooldownManager = new CooldownManager();
        this.enchantMenu = new EnchantMenuGUI(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.lootProfile = new com.soulenchants.loot.LootProfile(getDataFolder());
        this.godMenu = new com.soulenchants.gui.GodMenuGUI(this);
        com.soulenchants.shop.ShopCatalog.register();
        this.shopFeatured = new com.soulenchants.shop.ShopFeatured(getDataFolder());
        this.shopFeatured.maybeRoll();
        this.shopGUI = new com.soulenchants.shop.ShopGUI(this, shopFeatured);
        getServer().getPluginManager().registerEvents(shopGUI, this);
        this.lootBoxRevealGUI = new com.soulenchants.shop.LootBoxRevealGUI(this);
        getServer().getPluginManager().registerEvents(lootBoxRevealGUI, this);
        getServer().getPluginManager().registerEvents(new com.soulenchants.shop.LootBoxListener(this), this);
        com.soulenchants.quests.QuestRegistry.register();
        this.questManager = new com.soulenchants.quests.QuestManager(this);
        this.questGUI = new com.soulenchants.quests.QuestGUI(this);
        getServer().getPluginManager().registerEvents(questGUI, this);
        getServer().getPluginManager().registerEvents(new com.soulenchants.quests.QuestEventListener(this), this);
    }

    /** Custom-mob registry + listener + spawner. MobRegistry.register() MUST
     *  precede LootConfig which MUST precede MobListener (spawn-time id lookup). */
    private void initMobs() {
        com.soulenchants.mobs.MobRegistry.register();
        this.lootConfig = new com.soulenchants.config.LootConfig(this);
        this.lootConfig.applyMobOverrides();
        com.soulenchants.bosses.BossDamage.init(this.lootConfig);
        this.lootEditorGUI = new com.soulenchants.gui.LootEditorGUI(this);
        getServer().getPluginManager().registerEvents(lootEditorGUI, this);
        this.mobListener = new com.soulenchants.mobs.MobListener(this);
        getServer().getPluginManager().registerEvents(mobListener, this);
        getServer().getPluginManager().registerEvents(new com.soulenchants.mobs.MobSpawner(this, mobListener), this);
        getServer().getPluginManager().registerEvents(new com.soulenchants.mobs.NaturalBehaviorBlocker(), this);
        getLogger().info("[mobs] " + com.soulenchants.mobs.MobRegistry.all().size() + " custom mobs registered");
    }

    /** Combat + loot + boss-mechanic listeners. Runs after managers so
     *  every listener can reach its dependency via plugin.getX(). */
    private void initListeners() {
        com.soulenchants.loot.LootRecipes.register(this);
        getServer().getPluginManager().registerEvents(new com.soulenchants.loot.LootItemListener(this), this);
        getServer().getPluginManager().registerEvents(new com.soulenchants.loot.LootRecipes.GateListener(), this);
        getServer().getPluginManager().registerEvents(godMenu, this);
        getServer().getPluginManager().registerEvents(new BoulderBlockBlocker(), this);
        getServer().getPluginManager().registerEvents(new CrystalListener(this), this);
        getServer().getPluginManager().registerEvents(new IronSentinelListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnerTagListener(), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new OrbListener(this), this);
        getServer().getPluginManager().registerEvents(new VeilweaverDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new com.soulenchants.bosses.BossDamageImmunity(this), this);
        getServer().getPluginManager().registerEvents(new com.soulenchants.bosses.EliteBossHooks(this), this);
        getServer().getPluginManager().registerEvents(new IronGolemDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GrindingListener(this), this);
        getServer().getPluginManager().registerEvents(new PvEDamageListener(this), this);
        getServer().getPluginManager().registerEvents(enchantMenu, this);
    }

    /** Periodic tick tasks — berserker tick, clarity splash cancel,
     *  armor-change syncer, scoreboard refresher. */
    private void initTickTasks() {
        tickTask = new BerserkTickTask(this);
        tickTask.start();
        this.clarityTask = new com.soulenchants.listeners.ClarityTask(this);
        this.clarityTask.start();
        getServer().getPluginManager().registerEvents(clarityTask, this);
        getServer().getPluginManager().registerEvents(new ArmorChangeListener(this, tickTask), this);
        // Reset lingering max-HP from prior session; tick will re-apply correctly.
        for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) p.setMaxHealth(20.0);
        scoreboardManager.start();
    }

    /** Core commands — /souls /ce /bless /boss /shop /quests /mob. */
    private void initCommands() {
        getCommand("souls").setExecutor(new SoulsCommand(this));
        getCommand("ce").setExecutor(new CECommand(this));
        getCommand("bless").setExecutor(new BlessCommand(this));
        getCommand("boss").setExecutor(new com.soulenchants.commands.BossCommand(this));
        getCommand("shop").setExecutor(new com.soulenchants.shop.ShopCommand(this));
        getCommand("quests").setExecutor(new com.soulenchants.quests.QuestCommand(this));
        getCommand("mob").setExecutor(new com.soulenchants.mobs.MobCommand(this, mobListener));
    }

    /** Void Rift system + associated holograms. */
    private void initRifts() {
        this.riftSpawnConfig = new com.soulenchants.rifts.RiftSpawnConfig(this);
        this.voidRiftManager = new com.soulenchants.rifts.VoidRiftManager(this, riftSpawnConfig);
        this.hologramConfig = new com.soulenchants.rifts.HologramConfig(this);
        this.hologramManager = new com.soulenchants.rifts.HologramManager(this, hologramConfig);
        this.hologramManager.bootstrap();
        this.hologramGUI = new com.soulenchants.rifts.HologramGUI(this, hologramManager);
        getServer().getPluginManager().registerEvents(hologramGUI, this);
        getServer().getPluginManager().registerEvents(
                new com.soulenchants.rifts.VoidRiftListener(this, voidRiftManager), this);
        getCommand("rift").setExecutor(
                new com.soulenchants.rifts.RiftCommand(this, riftSpawnConfig, voidRiftManager,
                        hologramManager, hologramGUI));
        getServer().getPluginManager().registerEvents(new com.soulenchants.rifts.RiftChunkHolder(), this);
    }

    private void initRecipeGui() {
        this.recipeGUI = new com.soulenchants.gui.RecipeGUI();
        getServer().getPluginManager().registerEvents(recipeGUI, this);
    }

    private void initLootFilter() {
        this.lootFilterManager = new com.soulenchants.loot.LootFilterManager(this, getDataFolder());
        this.lootFilterGUI = new com.soulenchants.gui.LootFilterGUI(this, lootFilterManager);
        getServer().getPluginManager().registerEvents(lootFilterGUI, this);
        getServer().getPluginManager().registerEvents(new com.soulenchants.loot.LootFilterListener(this), this);
        getCommand("lootfilter").setExecutor(new com.soulenchants.commands.LootFilterCommand(this));
    }

    /** Transmog Scroll — sort enchants by tier when applied to gear. */
    private void initTransmog() {
        getServer().getPluginManager().registerEvents(new com.soulenchants.items.TransmogScroll(), this);
    }

    /** PvP kills/deaths/KDR tracker (feeds sidebar). */
    private void initPvpStats() {
        this.pvpStats = new com.soulenchants.scoreboard.PvPStats(getDataFolder());
        getServer().getPluginManager().registerEvents(pvpStats, this);
    }

    /** Guild system — friendly-fire detection, ally-aware procs, vault,
     *  leaderboard, /guild command. */
    private void initGuilds() {
        this.guildManager = new com.soulenchants.guilds.GuildManager(this, getDataFolder());
        this.guildManager.start();
        getServer().getPluginManager().registerEvents(new com.soulenchants.guilds.GuildListener(this), this);
        getCommand("guild").setExecutor(new com.soulenchants.guilds.GuildCommand(this));
    }

    /** Armor-set bonuses — NBT-tagged pieces trigger lifecycle + per-event bonuses. */
    private void initArmorSets() {
        com.soulenchants.sets.SetRegistry.add(new com.soulenchants.sets.impl.BossKillerSet(this));
        com.soulenchants.sets.SetRegistry.add(new com.soulenchants.sets.impl.DuelistSet(this));
        this.setManager = new com.soulenchants.sets.SetManager(this);
        this.setManager.start();
        getServer().getPluginManager().registerEvents(new com.soulenchants.sets.SetBonusListener(this), this);
    }

    /** Modock — three-phase Atlantis boss across modock_phase1/2/3. */
    private void initModock() {
        this.modockSpawnConfig = new com.soulenchants.modock.ModockSpawnConfig(this);
        this.modockManager = new com.soulenchants.modock.ModockManager(this, modockSpawnConfig);
        getServer().getPluginManager().registerEvents(new com.soulenchants.modock.ModockSpawner(this), this);
        if (getCommand("modock") != null)
            getCommand("modock").setExecutor(new com.soulenchants.modock.ModockCommand(this));
    }

    private void initMythicSubsystem() {
        getServer().getPluginManager().registerEvents(new MythicListener(this), this);
        getServer().getPluginManager().registerEvents(
                new com.soulenchants.listeners.MythicDeathMessages(), this);
        this.mythicAuraTask = new MythicAuraTask(this, mythicConfig);
        mythicAuraTask.start();
        if (getCommand("mythic") != null)
            getCommand("mythic").setExecutor(new MythicCommand(this));
    }

    /** Cosmetic masks: packet injector (helmet-replace), attach listener,
     *  and the Stalker/Phantom/Soul Harvest ability driver. */
    private void initMaskSubsystem() {
        this.maskPacketInjector = new MaskPacketInjector(this);
        maskPacketInjector.attach();
        getServer().getPluginManager().registerEvents(new MaskAttachListener(this), this);
        if (getCommand("mask") != null)
            getCommand("mask").setExecutor(new MaskCommand(this));
        com.soulenchants.masks.MaskAbilityTask maskTask =
                new com.soulenchants.masks.MaskAbilityTask(this);
        maskTask.start();
        getServer().getPluginManager().registerEvents(maskTask, this);
    }

    private void initSoulGems() {
        getServer().getPluginManager().registerEvents(
                new com.soulenchants.listeners.SoulGemListener(this), this);
    }

    /** Hybrid pet system — egg item + follower armor stand + XP on NBT. */
    private void initPets() {
        com.soulenchants.pets.PetRegistry.registerDefaults(this);
        this.petManager = new com.soulenchants.pets.PetManager(this);
        this.petManager.start();
        getServer().getPluginManager().registerEvents(new com.soulenchants.pets.PetListener(this), this);
        if (getCommand("pet") != null)
            getCommand("pet").setExecutor(new com.soulenchants.pets.PetCommand(this));
    }

    /** Lunar Client bridge — optional. Failure here should never disable
     *  any core gameplay; LunarBridge.init is already defensive. */
    private void initLunar() {
        LunarBridge.init(this);
        this.lunarPingListener = new LunarPingListener(this);
        lunarPingListener.start();
        if (getCommand("lunar") != null)
            getCommand("lunar").setExecutor(new com.soulenchants.lunar.LunarCommand());
    }

    private void initTabCompletion() {
        com.soulenchants.commands.TabCompletion tab = new com.soulenchants.commands.TabCompletion(this);
        for (String c : new String[]{"souls","ce","shop","quests","boss","bless","mob","rift","modock","mythic","mask"}) {
            if (getCommand(c) != null) getCommand(c).setTabCompleter(tab);
        }
    }

    /** /ce reload — re-read enchants.yml and mythics.yml live. */
    public void reloadEnchantConfigs() {
        ConfigBinder.reload(enchantConfig);
        ConfigBinder.reload(mythicConfig);
    }

    @Override
    public void onDisable() {
        // Reset player max HP so Vital boosts don't persist into the .dat file
        for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
            p.setMaxHealth(20.0);
        }
        // Stop subsystems in reverse-init order. Each wrapped in try/catch so
        // a single throwing stop() can't abort the rest of the shutdown.
        safe(new Runnable() { public void run() { if (scoreboardManager != null) scoreboardManager.stop(); }});
        safe(new Runnable() { public void run() { if (soulManager != null) soulManager.save(); }});
        safe(new Runnable() { public void run() { if (lootFilterManager != null) lootFilterManager.save(); }});
        safe(new Runnable() { public void run() { if (pvpStats != null) pvpStats.save(); }});
        safe(new Runnable() { public void run() { if (guildManager != null) { guildManager.stop(); guildManager.save(); } }});
        safe(new Runnable() { public void run() { if (clarityTask != null) clarityTask.stop(); }});
        safe(new Runnable() { public void run() { if (petManager != null) petManager.stop(); }});
        safe(new Runnable() { public void run() { if (setManager != null) setManager.stop(); }});
        safe(new Runnable() { public void run() { if (veilweaverManager != null && veilweaverManager.getActive() != null) veilweaverManager.getActive().stop(false); }});
        safe(new Runnable() { public void run() { if (ironGolemManager != null && ironGolemManager.getActive() != null) ironGolemManager.getActive().stop(false); }});
        safe(new Runnable() { public void run() { if (modockManager != null && modockManager.getActive() != null) modockManager.getActive().stop(false); }});
        // Phase 1 — drain registered UUID caches and restore every tracked
        // temporary block before we unload. Belt-and-suspenders so a crash
        // mid-shutdown doesn't leave world damage behind.
        safe(new Runnable() { public void run() {
            int blocks = com.soulenchants.util.TempBlockTracker.restoreAll();
            getLogger().info("[cleanup] Restored " + blocks + " temp blocks on shutdown.");
        }});
        safe(new Runnable() { public void run() { com.soulenchants.util.TempBlockTracker.shutdown(); }});
        safe(new Runnable() { public void run() { com.soulenchants.util.MapManager.clearAll(); }});
        // Cancel any outstanding scheduler tasks owned by this plugin. A
        // BukkitRunnable started without being tracked still gets cancelled
        // by this sweep. Load-bearing for /reload.
        safe(new Runnable() { public void run() { getServer().getScheduler().cancelTasks(SoulEnchants.this); }});
        getLogger().info("SoulEnchants disabled, data saved.");
    }

    /** Run a shutdown step with try/catch so one failing manager can't stop
     *  the rest of the plugin from draining cleanly. */
    private void safe(Runnable step) {
        try { step.run(); }
        catch (Throwable t) { getLogger().warning("[onDisable step] " + t); }
    }

    public SoulManager getSoulManager() { return soulManager; }
    public VeilweaverManager getVeilweaverManager() { return veilweaverManager; }
    public IronGolemManager getIronGolemManager() { return ironGolemManager; }
    public EnchantMenuGUI getEnchantMenu() { return enchantMenu; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public BerserkTickTask getTickTask() { return tickTask; }
    public com.soulenchants.loot.LootProfile getLootProfile() { return lootProfile; }
    public com.soulenchants.gui.GodMenuGUI getGodMenu() { return godMenu; }
    public com.soulenchants.shop.ShopFeatured getShopFeatured() { return shopFeatured; }
    public com.soulenchants.shop.ShopGUI getShopGUI() { return shopGUI; }
    public com.soulenchants.shop.LootBoxRevealGUI getLootBoxRevealGUI() { return lootBoxRevealGUI; }
    public com.soulenchants.quests.QuestManager getQuestManager() { return questManager; }
    public com.soulenchants.quests.QuestGUI getQuestGUI() { return questGUI; }
    public com.soulenchants.config.LootConfig getLootConfig() { return lootConfig; }
    public com.soulenchants.gui.LootEditorGUI getLootEditorGUI() { return lootEditorGUI; }
    public com.soulenchants.rifts.RiftSpawnConfig getRiftSpawnConfig() { return riftSpawnConfig; }
    public com.soulenchants.rifts.VoidRiftManager getVoidRiftManager() { return voidRiftManager; }
    public com.soulenchants.rifts.HologramManager getHologramManager() { return hologramManager; }
    public com.soulenchants.rifts.HologramGUI getHologramGUI() { return hologramGUI; }
    public com.soulenchants.gui.RecipeGUI getRecipeGUI() { return recipeGUI; }
    public com.soulenchants.loot.LootFilterManager getLootFilterManager() { return lootFilterManager; }
    public com.soulenchants.gui.LootFilterGUI getLootFilterGUI() { return lootFilterGUI; }
    public com.soulenchants.scoreboard.PvPStats getPvPStats() { return pvpStats; }
    public com.soulenchants.guilds.GuildManager getGuildManager() { return guildManager; }
    public com.soulenchants.sets.SetManager getSetManager() { return setManager; }
    public com.soulenchants.modock.ModockManager getModockManager() { return modockManager; }
    public EnchantConfig getEnchantConfig() { return enchantConfig; }
    public MythicConfig getMythicConfig() { return mythicConfig; }
    public com.soulenchants.masks.MaskPacketInjector getMaskPacketInjector() { return maskPacketInjector; }
    public com.soulenchants.pets.PetManager getPetManager() { return petManager; }
}
