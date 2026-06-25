package dev.hammermaces;

import dev.hammermaces.commands.GiveMaceCommand;
import dev.hammermaces.commands.MaceReloadCommand;
import dev.hammermaces.listeners.MaceAbilityListener;
import dev.hammermaces.listeners.MacePassiveListener;
import dev.hammermaces.listeners.SoulboundListener;
import dev.hammermaces.managers.AnimationManager;
import dev.hammermaces.managers.CooldownManager;
import dev.hammermaces.managers.MaceConfigManager;
import dev.hammermaces.managers.MaceManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public class HammerMacesPlugin extends JavaPlugin {

    private static HammerMacesPlugin instance;

    private MaceConfigManager maceConfigManager;
    private MaceManager maceManager;
    private AnimationManager animationManager;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Load all mace configs from /maces/ folder
        this.maceConfigManager = new MaceConfigManager(this);
        this.maceConfigManager.loadAll();

        this.maceManager = new MaceManager(this);
        this.animationManager = new AnimationManager(this);
        this.cooldownManager = new CooldownManager();

        // Register listeners
        getServer().getPluginManager().registerEvents(new SoulboundListener(this), this);
        getServer().getPluginManager().registerEvents(new MacePassiveListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceAbilityListener(this), this);

        // Register commands
        GiveMaceCommand giveMaceCmd = new GiveMaceCommand(this);
        Objects.requireNonNull(getCommand("givemace")).setExecutor(giveMaceCmd);
        Objects.requireNonNull(getCommand("givemace")).setTabCompleter(giveMaceCmd);

        MaceReloadCommand reloadCmd = new MaceReloadCommand(this);
        Objects.requireNonNull(getCommand("macereload")).setExecutor(reloadCmd);

        // Start animation task
        animationManager.startAnimationTask();

        getLogger().info("HammerMaces enabled — your SMP just got legendary.");
    }

    @Override
    public void onDisable() {
        if (animationManager != null) {
            animationManager.stopAnimationTask();
        }
        getLogger().info("HammerMaces disabled.");
    }

    public void reload() {
        reloadConfig();
        if (animationManager != null) animationManager.stopAnimationTask();
        maceConfigManager.loadAll();
        animationManager.startAnimationTask();
        getLogger().log(Level.INFO, "HammerMaces reloaded successfully.");
    }

    public static HammerMacesPlugin getInstance() { return instance; }
    public MaceConfigManager getMaceConfigManager() { return maceConfigManager; }
    public MaceManager getMaceManager() { return maceManager; }
    public AnimationManager getAnimationManager() { return animationManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
}
