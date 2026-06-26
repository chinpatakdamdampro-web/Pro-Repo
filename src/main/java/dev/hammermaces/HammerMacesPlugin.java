package dev.hammermaces;

import dev.hammermaces.commands.GiveMaceCommand;
import dev.hammermaces.commands.MaceReloadCommand;
import dev.hammermaces.data.ParasoQuestData;
import dev.hammermaces.listeners.MaceAbilityListener;
import dev.hammermaces.listeners.MacePassiveListener;
import dev.hammermaces.listeners.SoulboundListener;
import dev.hammermaces.listeners.paraso.ChaosTaxListener;
import dev.hammermaces.listeners.paraso.NauseaPassiveListener;
import dev.hammermaces.listeners.paraso.QuestBiomeListener;
import dev.hammermaces.managers.AnimationManager;
import dev.hammermaces.managers.CooldownManager;
import dev.hammermaces.managers.MaceConfigManager;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.managers.paraso.ParasoQuestManager;
import dev.hammermaces.managers.paraso.PresenceStackManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public class HammerMacesPlugin extends JavaPlugin {

    private static HammerMacesPlugin instance;

    private MaceConfigManager   maceConfigManager;
    private MaceManager         maceManager;
    private AnimationManager    animationManager;
    private CooldownManager     cooldownManager;
    private ParasoQuestData     parasoQuestData;
    private ParasoQuestManager  parasoQuestManager;
    private PresenceStackManager presenceStackManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Core managers
        this.maceConfigManager = new MaceConfigManager(this);
        this.maceConfigManager.loadAll();

        this.maceManager      = new MaceManager(this);
        this.cooldownManager  = new CooldownManager();
        this.animationManager = new AnimationManager(this);

        // Paraso systems
        this.parasoQuestData     = new ParasoQuestData(this);
        this.parasoQuestManager  = new ParasoQuestManager(this, parasoQuestData);
        this.presenceStackManager = new PresenceStackManager(this);
        this.presenceStackManager.start();

        // Listeners
        getServer().getPluginManager().registerEvents(new SoulboundListener(this),     this);
        getServer().getPluginManager().registerEvents(new MacePassiveListener(this),   this);
        getServer().getPluginManager().registerEvents(new MaceAbilityListener(this),   this);
        getServer().getPluginManager().registerEvents(new ChaosTaxListener(this),      this);
        getServer().getPluginManager().registerEvents(new NauseaPassiveListener(this), this);
        getServer().getPluginManager().registerEvents(new QuestBiomeListener(this),    this);

        // Commands
        GiveMaceCommand giveCmd = new GiveMaceCommand(this);
        Objects.requireNonNull(getCommand("givemace")).setExecutor(giveCmd);
        Objects.requireNonNull(getCommand("givemace")).setTabCompleter(giveCmd);

        MaceReloadCommand reloadCmd = new MaceReloadCommand(this);
        Objects.requireNonNull(getCommand("macereload")).setExecutor(reloadCmd);

        // Start animation
        animationManager.startAnimationTask();

        // Record today's login for quest 2
        parasoQuestManager.onLogin();

        getLogger().info("HammerMaces enabled.");
    }

    @Override
    public void onDisable() {
        if (animationManager    != null) animationManager.stopAnimationTask();
        if (presenceStackManager != null) presenceStackManager.stop();
        if (parasoQuestData     != null) parasoQuestData.save();
        getLogger().info("HammerMaces disabled.");
    }

    public void reload() {
        reloadConfig();
        if (animationManager != null) animationManager.stopAnimationTask();
        maceConfigManager.loadAll();
        animationManager.startAnimationTask();
        getLogger().log(Level.INFO, "HammerMaces reloaded.");
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public static HammerMacesPlugin getInstance()          { return instance; }
    public MaceConfigManager getMaceConfigManager()        { return maceConfigManager; }
    public MaceManager getMaceManager()                    { return maceManager; }
    public AnimationManager getAnimationManager()          { return animationManager; }
    public CooldownManager getCooldownManager()            { return cooldownManager; }
    public ParasoQuestData getParasoQuestData()            { return parasoQuestData; }
    public ParasoQuestManager getParasoQuestManager()      { return parasoQuestManager; }
    public PresenceStackManager getPresenceStackManager()  { return presenceStackManager; }
}
