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

    // Ability toggles & settings
    private final boolean diamondSkinEnabled;
    private final int diamondSkinLevel;
    private final int diamondSkinPulse;

    private final boolean aquaticSoulEnabled;
    private final int aquaticSoulPulse;

    private final boolean coldAuraEnabled;
    private final int coldAuraCooldown;
    private final double coldAuraRadius;
    private final int coldAuraEffectDuration;
    private final int coldAuraSlownessLevel;
    private final int coldAuraWeaknessLevel;

    private final boolean tidalSurgeEnabled;
    private final int tidalSurgeCooldown;
    private final double tidalSurgeKnockback;
    private final double tidalSurgeConeAngle;
    private final double tidalSurgeRange;
    private final int tidalSurgeEffectDuration;
    private final int tidalSurgeSlownessLevel;

    public MaceConfig(String maceId, FileConfiguration cfg) {
        this.maceId      = maceId;
        this.holderName  = cfg.getString("holder", "YourNameHere");
        this.displayName = cfg.getString("display-name", "Unknown Mace");
        this.gradientStart = cfg.getString("gradient-start", "#FFFFFF");
        this.gradientEnd   = cfg.getString("gradient-end", "#AAAAAA");

        // Diamond Skin
        this.diamondSkinEnabled = cfg.getBoolean("abilities.diamond_skin.enabled", true);
        this.diamondSkinLevel   = cfg.getInt("abilities.diamond_skin.level", 1);
        this.diamondSkinPulse   = cfg.getInt("abilities.diamond_skin.pulse-interval", 60);

        // Aquatic Soul
        this.aquaticSoulEnabled = cfg.getBoolean("abilities.aquatic_soul.enabled", true);
        this.aquaticSoulPulse   = cfg.getInt("abilities.aquatic_soul.pulse-interval", 60);

        // Cold Aura
        this.coldAuraEnabled        = cfg.getBoolean("abilities.cold_aura.enabled", true);
        this.coldAuraCooldown       = cfg.getInt("abilities.cold_aura.cooldown", 40);
        this.coldAuraRadius         = cfg.getDouble("abilities.cold_aura.radius", 5.0);
        this.coldAuraEffectDuration = cfg.getInt("abilities.cold_aura.effect-duration", 100);
        this.coldAuraSlownessLevel  = cfg.getInt("abilities.cold_aura.slowness-level", 2);
        this.coldAuraWeaknessLevel  = cfg.getInt("abilities.cold_aura.weakness-level", 1);

        // Tidal Surge
        this.tidalSurgeEnabled        = cfg.getBoolean("abilities.tidal_surge.enabled", true);
        this.tidalSurgeCooldown       = cfg.getInt("abilities.tidal_surge.cooldown", 15);
        this.tidalSurgeKnockback      = cfg.getDouble("abilities.tidal_surge.knockback-strength", 2.5);
        this.tidalSurgeConeAngle      = cfg.getDouble("abilities.tidal_surge.cone-angle", 60.0);
        this.tidalSurgeRange          = cfg.getDouble("abilities.tidal_surge.range", 6.0);
        this.tidalSurgeEffectDuration = cfg.getInt("abilities.tidal_surge.effect-duration", 60);
        this.tidalSurgeSlownessLevel  = cfg.getInt("abilities.tidal_surge.slowness-level", 2);
    }

    public String getMaceId()           { return maceId; }
    public String getHolderName()       { return holderName; }
    public String getDisplayName()      { return displayName; }
    public String getGradientStart()    { return gradientStart; }
    public String getGradientEnd()      { return gradientEnd; }

    public boolean isDiamondSkinEnabled()   { return diamondSkinEnabled; }
    public int getDiamondSkinLevel()        { return diamondSkinLevel; }
    public int getDiamondSkinPulse()        { return diamondSkinPulse; }

    public boolean isAquaticSoulEnabled()   { return aquaticSoulEnabled; }
    public int getAquaticSoulPulse()        { return aquaticSoulPulse; }

    public boolean isColdAuraEnabled()          { return coldAuraEnabled; }
    public int getColdAuraCooldown()            { return coldAuraCooldown; }
    public double getColdAuraRadius()           { return coldAuraRadius; }
    public int getColdAuraEffectDuration()      { return coldAuraEffectDuration; }
    public int getColdAuraSlownessLevel()       { return coldAuraSlownessLevel; }
    public int getColdAuraWeaknessLevel()       { return coldAuraWeaknessLevel; }

    public boolean isTidalSurgeEnabled()        { return tidalSurgeEnabled; }
    public int getTidalSurgeCooldown()          { return tidalSurgeCooldown; }
    public double getTidalSurgeKnockback()      { return tidalSurgeKnockback; }
    public double getTidalSurgeConeAngle()      { return tidalSurgeConeAngle; }
    public double getTidalSurgeRange()          { return tidalSurgeRange; }
    public int getTidalSurgeEffectDuration()    { return tidalSurgeEffectDuration; }
    public int getTidalSurgeSlownessLevel()     { return tidalSurgeSlownessLevel; }
}
