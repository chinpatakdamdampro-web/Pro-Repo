package dev.hammermaces;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import dev.hammermaces.abilities.architect.SilentReadAbility;
import dev.hammermaces.abilities.paraso.UnannouncedAbility;
import dev.hammermaces.commands.GiveMaceCommand;
import dev.hammermaces.commands.MaceReloadCommand;
import dev.hammermaces.data.ParasoQuestData;
import dev.hammermaces.listeners.MaceAbilityListener;
import dev.hammermaces.listeners.MacePassiveListener;
import dev.hammermaces.listeners.SoulboundListener;
import dev.hammermaces.listeners.architect.ArchitectListener;
import dev.hammermaces.listeners.paraso.ChaosTaxListener;
import dev.hammermaces.listeners.paraso.NauseaPassiveListener;
import dev.hammermaces.listeners.paraso.QuestBiomeListener;
import dev.hammermaces.listeners.paraso.QuestCollectionListener;
import dev.hammermaces.managers.AnimationManager;
import dev.hammermaces.managers.CooldownManager;
import dev.hammermaces.managers.FractureManager;
import dev.hammermaces.managers.HudManager;
import dev.hammermaces.managers.MaceConfigManager;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.managers.paraso.ParasoQuestManager;
import dev.hammermaces.managers.paraso.PresenceStackManager;
import dev.hammermaces.utils.particles.ParticleEffects;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public class HammerMacesPlugin extends JavaPlugin {

    private static HammerMacesPlugin instance;

    private MaceConfigManager    maceConfigManager;
    private MaceManager          maceManager;
    private AnimationManager     animationManager;
    private CooldownManager      cooldownManager;
    private FractureManager      fractureManager;
    private HudManager           hudManager;
    private ParasoQuestData      parasoQuestData;
    private ParasoQuestManager   parasoQuestManager;
    private PresenceStackManager presenceStackManager;
    private SilentReadAbility    silentReadAbility;
    private ParticleEffects      particleEffects;
    private ArchitectListener    architectListener;
    private MaceAbilityListener  abilityListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        ParticleNativeAPI particleAPI = ParticleNativeCore.loadAPI(this);
        this.particleEffects = new ParticleEffects(this, particleAPI);

        this.maceConfigManager = new MaceConfigManager(this);
        this.maceConfigManager.loadAll();
        this.maceManager      = new MaceManager(this);
        this.cooldownManager  = new CooldownManager();
        this.fractureManager  = new FractureManager(this);
        this.animationManager = new AnimationManager(this);
        this.hudManager       = new HudManager(this);

        this.parasoQuestData      = new ParasoQuestData(this);
        this.parasoQuestManager   = new ParasoQuestManager(this, parasoQuestData);
        this.presenceStackManager = new PresenceStackManager(this);
        this.presenceStackManager.start();

        this.silentReadAbility = new SilentReadAbility(this);
        this.silentReadAbility.start();

        this.architectListener = new ArchitectListener(this);
        this.abilityListener   = new MaceAbilityListener(this);

        getServer().getPluginManager().registerEvents(new SoulboundListener(this),        this);
        getServer().getPluginManager().registerEvents(new MacePassiveListener(this),      this);
        getServer().getPluginManager().registerEvents(abilityListener,                    this);
        getServer().getPluginManager().registerEvents(architectListener,                  this);
        getServer().getPluginManager().registerEvents(new ChaosTaxListener(this),         this);
        getServer().getPluginManager().registerEvents(new NauseaPassiveListener(this),    this);
        getServer().getPluginManager().registerEvents(new QuestBiomeListener(this),       this);
        getServer().getPluginManager().registerEvents(new QuestCollectionListener(this),  this);

        GiveMaceCommand giveCmd = new GiveMaceCommand(this);
        Objects.requireNonNull(getCommand("givemace")).setExecutor(giveCmd);
        Objects.requireNonNull(getCommand("givemace")).setTabCompleter(giveCmd);
        Objects.requireNonNull(getCommand("macereload")).setExecutor(new MaceReloadCommand(this));

        animationManager.startAnimationTask();
        hudManager.start();

        getLogger().info("HammerMaces enabled.");
    }

    @Override
    public void onDisable() {
        if (animationManager     != null) animationManager.stopAnimationTask();
        if (presenceStackManager != null) presenceStackManager.stop();
        if (silentReadAbility    != null) silentReadAbility.stop();
        if (hudManager           != null) hudManager.stop();
        if (parasoQuestData      != null) parasoQuestData.save();
        if (architectListener    != null)
            getServer().getOnlinePlayers().forEach(p -> architectListener.getInfinitySlots().cleanup(p));
        getLogger().info("HammerMaces disabled.");
    }

    public void reload() {
        reloadConfig();
        if (animationManager != null) animationManager.stopAnimationTask();
        maceConfigManager.loadAll();
        animationManager.startAnimationTask();
        getLogger().log(Level.INFO, "HammerMaces reloaded.");
    }

    public static HammerMacesPlugin getInstance()          { return instance; }
    public MaceConfigManager getMaceConfigManager()        { return maceConfigManager; }
    public MaceManager getMaceManager()                    { return maceManager; }
    public AnimationManager getAnimationManager()          { return animationManager; }
    public CooldownManager getCooldownManager()            { return cooldownManager; }
    public FractureManager getFractureManager()            { return fractureManager; }
    public HudManager getHudManager()                      { return hudManager; }
    public ParasoQuestData getParasoQuestData()            { return parasoQuestData; }
    public ParasoQuestManager getParasoQuestManager()      { return parasoQuestManager; }
    public PresenceStackManager getPresenceStackManager()  { return presenceStackManager; }
    public SilentReadAbility getSilentReadAbility()        { return silentReadAbility; }
    public ParticleEffects getParticleEffects()            { return particleEffects; }
    public ArchitectListener getArchitectListener()        { return architectListener; }
    public UnannouncedAbility getUnannouncedAbility() {
        return abilityListener != null ? abilityListener.getUnannouncedAbility() : null;
    }
}
