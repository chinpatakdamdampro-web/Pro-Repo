package dev.hammermaces.managers;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Holds all parsed settings for a single mace from its individual .yml file.
 */
public class MaceConfig {

    private final String maceId;
    private final String holderName;
    private final String displayName;
    private final String gradientStart;
    private final String gradientEnd;

    // Diamond Skin
    private final boolean diamondSkinEnabled;
    private final int diamondSkinLevel;
    private final int diamondSkinPulse;

    // Aquatic Soul
    private final boolean aquaticSoulEnabled;
    private final int aquaticSoulPulse;

    // Cold Aura
    private final boolean coldAuraEnabled;
    private final int coldAuraCooldown;
    private final double coldAuraRadius;
    private final int coldAuraEffectDuration;
    private final int coldAuraSlownessLevel;
    private final int coldAuraWeaknessLevel;

    // Tidal Surge
    private final boolean tidalSurgeEnabled;
    private final int tidalSurgeCooldown;
    private final double tidalSurgeKnockback;
    private final double tidalSurgeConeAngle;
    private final double tidalSurgeRange;
    private final int tidalSurgeEffectDuration;
    private final int tidalSurgeSlownessLevel;

    // Larper Freeze
    private final boolean larperFreezeEnabled;
    private final int larperFreezeCooldown;
    private final double larperFreezeRadius;
    private final int larperFreezeDuration;
    private final int larperFreezeLine1Delay;
    private final int larperFreezeLine2Delay;
    private final String larperFreezeLine1;
    private final String larperFreezeLine2;
    private final int larperFreezeStrengthLevel;
    private final int larperFreezeStrengthDuration;
    private final boolean larperFreezeCombatImmunity;
    private final int larperFreezeCombatImmunityWindow;

    public MaceConfig(String maceId, FileConfiguration cfg) {
        this.maceId      = maceId;
        this.holderName  = cfg.getString("holder", "YourNameHere");
        this.displayName = cfg.getString("display-name", "Unknown Mace");
        this.gradientStart = cfg.getString("gradient-start", "#FFFFFF");
        this.gradientEnd   = cfg.getString("gradient-end", "#AAAAAA");

        // Diamond Skin
        this.diamondSkinEnabled = cfg.getBoolean("abilities.diamond_skin.enabled", false);
        this.diamondSkinLevel   = cfg.getInt("abilities.diamond_skin.level", 1);
        this.diamondSkinPulse   = cfg.getInt("abilities.diamond_skin.pulse-interval", 60);

        // Aquatic Soul
        this.aquaticSoulEnabled = cfg.getBoolean("abilities.aquatic_soul.enabled", false);
        this.aquaticSoulPulse   = cfg.getInt("abilities.aquatic_soul.pulse-interval", 60);

        // Cold Aura
        this.coldAuraEnabled        = cfg.getBoolean("abilities.cold_aura.enabled", false);
        this.coldAuraCooldown       = cfg.getInt("abilities.cold_aura.cooldown", 40);
        this.coldAuraRadius         = cfg.getDouble("abilities.cold_aura.radius", 5.0);
        this.coldAuraEffectDuration = cfg.getInt("abilities.cold_aura.effect-duration", 100);
        this.coldAuraSlownessLevel  = cfg.getInt("abilities.cold_aura.slowness-level", 2);
        this.coldAuraWeaknessLevel  = cfg.getInt("abilities.cold_aura.weakness-level", 1);

        // Tidal Surge
        this.tidalSurgeEnabled        = cfg.getBoolean("abilities.tidal_surge.enabled", false);
        this.tidalSurgeCooldown       = cfg.getInt("abilities.tidal_surge.cooldown", 15);
        this.tidalSurgeKnockback      = cfg.getDouble("abilities.tidal_surge.knockback-strength", 2.5);
        this.tidalSurgeConeAngle      = cfg.getDouble("abilities.tidal_surge.cone-angle", 60.0);
        this.tidalSurgeRange          = cfg.getDouble("abilities.tidal_surge.range", 6.0);
        this.tidalSurgeEffectDuration = cfg.getInt("abilities.tidal_surge.effect-duration", 60);
        this.tidalSurgeSlownessLevel  = cfg.getInt("abilities.tidal_surge.slowness-level", 2);

        // Larper Freeze
        this.larperFreezeEnabled             = cfg.getBoolean("abilities.larper_freeze.enabled", false);
        this.larperFreezeCooldown            = cfg.getInt("abilities.larper_freeze.cooldown", 120);
        this.larperFreezeRadius              = cfg.getDouble("abilities.larper_freeze.radius", 14.0);
        this.larperFreezeDuration            = cfg.getInt("abilities.larper_freeze.freeze-duration", 4);
        this.larperFreezeLine1Delay          = cfg.getInt("abilities.larper_freeze.dialogue-line-1-delay", 0);
        this.larperFreezeLine2Delay          = cfg.getInt("abilities.larper_freeze.dialogue-line-2-delay", 2);
        this.larperFreezeLine1               = cfg.getString("abilities.larper_freeze.dialogue-line-1", "«Most players would die here.»");
        this.larperFreezeLine2               = cfg.getString("abilities.larper_freeze.dialogue-line-2", "«But you should know by now that I'm not most players.»");
        this.larperFreezeStrengthLevel       = cfg.getInt("abilities.larper_freeze.strength-level", 1);
        this.larperFreezeStrengthDuration    = cfg.getInt("abilities.larper_freeze.strength-duration", 400);
        this.larperFreezeCombatImmunity      = cfg.getBoolean("abilities.larper_freeze.combat-immunity", true);
        this.larperFreezeCombatImmunityWindow = cfg.getInt("abilities.larper_freeze.combat-immunity-window", 100);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getMaceId()        { return maceId; }
    public String getHolderName()    { return holderName; }
    public String getDisplayName()   { return displayName; }
    public String getGradientStart() { return gradientStart; }
    public String getGradientEnd()   { return gradientEnd; }

    public boolean isDiamondSkinEnabled() { return diamondSkinEnabled; }
    public int getDiamondSkinLevel()      { return diamondSkinLevel; }
    public int getDiamondSkinPulse()      { return diamondSkinPulse; }

    public boolean isAquaticSoulEnabled() { return aquaticSoulEnabled; }
    public int getAquaticSoulPulse()      { return aquaticSoulPulse; }

    public boolean isColdAuraEnabled()       { return coldAuraEnabled; }
    public int getColdAuraCooldown()         { return coldAuraCooldown; }
    public double getColdAuraRadius()        { return coldAuraRadius; }
    public int getColdAuraEffectDuration()   { return coldAuraEffectDuration; }
    public int getColdAuraSlownessLevel()    { return coldAuraSlownessLevel; }
    public int getColdAuraWeaknessLevel()    { return coldAuraWeaknessLevel; }

    public boolean isTidalSurgeEnabled()     { return tidalSurgeEnabled; }
    public int getTidalSurgeCooldown()       { return tidalSurgeCooldown; }
    public double getTidalSurgeKnockback()   { return tidalSurgeKnockback; }
    public double getTidalSurgeConeAngle()   { return tidalSurgeConeAngle; }
    public double getTidalSurgeRange()       { return tidalSurgeRange; }
    public int getTidalSurgeEffectDuration() { return tidalSurgeEffectDuration; }
    public int getTidalSurgeSlownessLevel()  { return tidalSurgeSlownessLevel; }

    public boolean isLarperFreezeEnabled()          { return larperFreezeEnabled; }
    public int getLarperFreezeCooldown()            { return larperFreezeCooldown; }
    public double getLarperFreezeRadius()           { return larperFreezeRadius; }
    public int getLarperFreezeDuration()            { return larperFreezeDuration; }
    public int getLarperFreezeLine1Delay()          { return larperFreezeLine1Delay; }
    public int getLarperFreezeLine2Delay()          { return larperFreezeLine2Delay; }
    public String getLarperFreezeLine1()            { return larperFreezeLine1; }
    public String getLarperFreezeLine2()            { return larperFreezeLine2; }
    public int getLarperFreezeStrengthLevel()       { return larperFreezeStrengthLevel; }
    public int getLarperFreezeStrengthDuration()    { return larperFreezeStrengthDuration; }
    public boolean isLarperFreezeCombatImmunity()   { return larperFreezeCombatImmunity; }
    public int getLarperFreezeCombatImmunityWindow(){ return larperFreezeCombatImmunityWindow; }
            }
