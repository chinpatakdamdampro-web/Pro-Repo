package dev.hammermaces.managers;

import org.bukkit.configuration.file.FileConfiguration;

public class MaceConfig {

    // ── Core ──────────────────────────────────────────────────────────────────
    private final String maceId;
    private final String holderName;
    private final String displayName;
    private final String gradientStart;
    private final String gradientEnd;
    private final String itemType;
    private int questTier;

    // ── Poseidon ──────────────────────────────────────────────────────────────
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

    // ── Larpers ───────────────────────────────────────────────────────────────
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

    // False Summon
    private final boolean falseSummonEnabled;
    private final int falseSummonCooldown;
    private final double falseSummonRadius;
    private final int falseSummonDecoyDuration;
    private final int falseSummonInvisDuration;
    private final int falseSummonSpeedDuration;
    private final int falseSummonWeaknessDuration;

    // Monologue
    private final boolean monologueEnabled;
    private final int monologueCooldown;
    private final int monologueStrengthLevel;
    private final int monologueDuration;
    private final String monologueLine;

    // ── Paraso — The Unannounced ──────────────────────────────────────────────
    private final int dashCooldown;
    private final double dashVelocity;
    private final int absentStackInterval;
    private final int absentMaxStacks;
    private final int chaosTaxProcChance;
    private final boolean unannouncedEnabled;
    private final int unannouncedCooldown;
    private final double unannouncedRadius;
    private final int unannouncedFreezeDuration;
    private final int unannouncedHitWindow;
    private final int unannouncedSaturationLevel;
    private final int unannouncedSaturationDuration;
    private final boolean justShowedUpEnabled;
    private final int justShowedUpCooldown;
    private final int justShowedUpMinStacks;
    private final double justShowedUpRadius;
    private final int justShowedUpEffectDuration;

    // ── The Afterthought ──────────────────────────────────────────────────────
    private final boolean nauseaPassiveEnabled;
    private final int nauseaDuration;
    private final int nauseaCooldownPerTarget;

    // ── The First Draft ───────────────────────────────────────────────────────
    private final double silentReadFarRange;
    private final double silentReadNearRange;
    private final int silentReadPingIntervalTicks;
    private final int calculatedMaxFractureStacks;
    private final int calculatedMissingHealthPercent;
    private final int contingencyCooldown;
    private final int contingencyWindowSeconds;
    private final int contingencyCharges;
    private final int contingencyPartialRefundSeconds;
    private final double contingencyTeleportDistance;
    private final int contingencyBlindnessDuration;
    private final int contingencySlownessDuration;
    private final int contingencyAbsorptionDuration;
    private final int contingencyExecutionWindowSeconds;
    private final int predeterminedCooldown;
    private final double predeterminedRadius;
    private final int predeterminedDurationSeconds;
    private final int predeterminedSpeedDuration;
    private final boolean groundSlamEnabled;
    private final double groundSlamMinFallDistance;
    private final boolean infinitySlotsEnabled;
    private final int infinitySlots;
    private final int chargedSmashIntervalSeconds;
    private final double netherStarPredeterminedRadiusBonus;
    private final int netherStarNauseaDurationBonus;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MaceConfig(String maceId, FileConfiguration cfg) {
        this.maceId        = maceId;
        this.holderName    = cfg.getString("holder", "YourNameHere");
        this.displayName   = cfg.getString("display-name", "Unknown");
        this.gradientStart = cfg.getString("gradient-start", "#FFFFFF");
        this.gradientEnd   = cfg.getString("gradient-end", "#AAAAAA");
        this.itemType      = cfg.getString("type", "MACE").toUpperCase();
        this.questTier     = cfg.getInt("quest-tier", 5);

        // Poseidon
        this.diamondSkinEnabled = cfg.getBoolean("abilities.diamond_skin.enabled", false);
        this.diamondSkinLevel   = cfg.getInt("abilities.diamond_skin.level", 1);
        this.diamondSkinPulse   = cfg.getInt("abilities.diamond_skin.pulse-interval", 60);
        this.aquaticSoulEnabled = cfg.getBoolean("abilities.aquatic_soul.enabled", false);
        this.aquaticSoulPulse   = cfg.getInt("abilities.aquatic_soul.pulse-interval", 60);
        this.coldAuraEnabled        = cfg.getBoolean("abilities.cold_aura.enabled", false);
        this.coldAuraCooldown       = cfg.getInt("abilities.cold_aura.cooldown", 40);
        this.coldAuraRadius         = cfg.getDouble("abilities.cold_aura.radius", 5.0);
        this.coldAuraEffectDuration = cfg.getInt("abilities.cold_aura.effect-duration", 100);
        this.coldAuraSlownessLevel  = cfg.getInt("abilities.cold_aura.slowness-level", 2);
        this.coldAuraWeaknessLevel  = cfg.getInt("abilities.cold_aura.weakness-level", 1);
        this.tidalSurgeEnabled        = cfg.getBoolean("abilities.tidal_surge.enabled", false);
        this.tidalSurgeCooldown       = cfg.getInt("abilities.tidal_surge.cooldown", 15);
        this.tidalSurgeKnockback      = cfg.getDouble("abilities.tidal_surge.knockback-strength", 2.5);
        this.tidalSurgeConeAngle      = cfg.getDouble("abilities.tidal_surge.cone-angle", 60.0);
        this.tidalSurgeRange          = cfg.getDouble("abilities.tidal_surge.range", 6.0);
        this.tidalSurgeEffectDuration = cfg.getInt("abilities.tidal_surge.effect-duration", 60);
        this.tidalSurgeSlownessLevel  = cfg.getInt("abilities.tidal_surge.slowness-level", 2);

        // Larpers
        this.larperFreezeEnabled              = cfg.getBoolean("abilities.larper_freeze.enabled", false);
        this.larperFreezeCooldown             = cfg.getInt("abilities.larper_freeze.cooldown", 120);
        this.larperFreezeRadius               = cfg.getDouble("abilities.larper_freeze.radius", 14.0);
        this.larperFreezeDuration             = cfg.getInt("abilities.larper_freeze.freeze-duration", 4);
        this.larperFreezeLine1Delay           = cfg.getInt("abilities.larper_freeze.dialogue-line-1-delay", 0);
        this.larperFreezeLine2Delay           = cfg.getInt("abilities.larper_freeze.dialogue-line-2-delay", 2);
        this.larperFreezeLine1                = cfg.getString("abilities.larper_freeze.dialogue-line-1", "«Most players would die here.»");
        this.larperFreezeLine2                = cfg.getString("abilities.larper_freeze.dialogue-line-2", "«But you should know by now that I'm not most players.»");
        this.larperFreezeStrengthLevel        = cfg.getInt("abilities.larper_freeze.strength-level", 1);
        this.larperFreezeStrengthDuration     = cfg.getInt("abilities.larper_freeze.strength-duration", 400);
        this.larperFreezeCombatImmunity       = cfg.getBoolean("abilities.larper_freeze.combat-immunity", true);
        this.larperFreezeCombatImmunityWindow = cfg.getInt("abilities.larper_freeze.combat-immunity-window", 100);

        this.falseSummonEnabled          = cfg.getBoolean("abilities.false_summon.enabled", true);
        this.falseSummonCooldown         = cfg.getInt("abilities.false_summon.cooldown", 45);
        this.falseSummonRadius           = cfg.getDouble("abilities.false_summon.radius", 5.0);
        this.falseSummonDecoyDuration    = cfg.getInt("abilities.false_summon.decoy-duration", 100);
        this.falseSummonInvisDuration    = cfg.getInt("abilities.false_summon.invis-duration", 80);
        this.falseSummonSpeedDuration    = cfg.getInt("abilities.false_summon.speed-duration", 80);
        this.falseSummonWeaknessDuration = cfg.getInt("abilities.false_summon.weakness-duration", 60);

        this.monologueEnabled       = cfg.getBoolean("abilities.monologue.enabled", true);
        this.monologueCooldown      = cfg.getInt("abilities.monologue.cooldown", 30);
        this.monologueStrengthLevel = cfg.getInt("abilities.monologue.strength-level", 2);
        this.monologueDuration      = cfg.getInt("abilities.monologue.duration", 100);
        this.monologueLine          = cfg.getString("abilities.monologue.line", "«Encore.»");

        // Paraso
        this.dashCooldown            = cfg.getInt("abilities.dash.cooldown", 8);
        this.dashVelocity            = cfg.getDouble("abilities.dash.velocity", 1.8);
        this.absentStackInterval     = cfg.getInt("abilities.absent.stack-interval", 180);
        this.absentMaxStacks         = cfg.getInt("abilities.absent.max-stacks", 5);
        this.chaosTaxProcChance      = cfg.getInt("abilities.chaos_tax.proc-chance", 15);
        this.unannouncedEnabled          = cfg.getBoolean("abilities.unannounced.enabled", false);
        this.unannouncedCooldown         = cfg.getInt("abilities.unannounced.cooldown", 20);
        this.unannouncedRadius           = cfg.getDouble("abilities.unannounced.radius", 12.0);
        this.unannouncedFreezeDuration   = cfg.getInt("abilities.unannounced.freeze-duration", 10);
        this.unannouncedHitWindow        = cfg.getInt("abilities.unannounced.hit-window", 10);
        this.unannouncedSaturationLevel  = cfg.getInt("abilities.unannounced.saturation-level", 255);
        this.unannouncedSaturationDuration = cfg.getInt("abilities.unannounced.saturation-duration", 200);
        this.justShowedUpEnabled        = cfg.getBoolean("abilities.just_showed_up.enabled", false);
        this.justShowedUpCooldown       = cfg.getInt("abilities.just_showed_up.cooldown", 45);
        this.justShowedUpMinStacks      = cfg.getInt("abilities.just_showed_up.min-stacks", 3);
        this.justShowedUpRadius         = cfg.getDouble("abilities.just_showed_up.radius", 6.0);
        this.justShowedUpEffectDuration = cfg.getInt("abilities.just_showed_up.effect-duration", 80);

        // Afterthought
        this.nauseaPassiveEnabled    = cfg.getBoolean("abilities.nausea_passive.enabled", false);
        this.nauseaDuration          = cfg.getInt("abilities.nausea_passive.duration", 100);
        this.nauseaCooldownPerTarget = cfg.getInt("abilities.nausea_passive.cooldown-per-target", 7);

        // First Draft
        this.silentReadFarRange           = cfg.getDouble("abilities.silent_read.far-range", 20.0);
        this.silentReadNearRange          = cfg.getDouble("abilities.silent_read.near-range", 8.0);
        this.silentReadPingIntervalTicks  = cfg.getInt("abilities.silent_read.ping-interval-ticks", 200);
        this.calculatedMaxFractureStacks  = cfg.getInt("abilities.calculated.max-fracture-stacks", 3);
        this.calculatedMissingHealthPercent = cfg.getInt("abilities.calculated.missing-health-percent", 40);
        this.contingencyCooldown              = cfg.getInt("abilities.contingency.cooldown", 120);
        this.contingencyWindowSeconds         = cfg.getInt("abilities.contingency.window-seconds", 15);
        this.contingencyCharges               = cfg.getInt("abilities.contingency.charges", 2);
        this.contingencyPartialRefundSeconds  = cfg.getInt("abilities.contingency.partial-refund-seconds", 60);
        this.contingencyTeleportDistance      = cfg.getDouble("abilities.contingency.teleport-distance", 6.0);
        this.contingencyBlindnessDuration     = cfg.getInt("abilities.contingency.blindness-duration", 60);
        this.contingencySlownessDuration      = cfg.getInt("abilities.contingency.slowness-duration", 100);
        this.contingencyAbsorptionDuration    = cfg.getInt("abilities.contingency.absorption-duration", 160);
        this.contingencyExecutionWindowSeconds = cfg.getInt("abilities.contingency.execution-window-seconds", 5);
        this.predeterminedCooldown        = cfg.getInt("abilities.predetermined.cooldown", 45);
        this.predeterminedRadius          = cfg.getDouble("abilities.predetermined.radius", 5.0);
        this.predeterminedDurationSeconds = cfg.getInt("abilities.predetermined.duration-seconds", 12);
        this.predeterminedSpeedDuration   = cfg.getInt("abilities.predetermined.speed-duration", 80);
        this.groundSlamEnabled          = cfg.getBoolean("abilities.ground_slam.enabled", true);
        this.groundSlamMinFallDistance  = cfg.getDouble("abilities.ground_slam.min-fall-distance", 3.0);
        this.infinitySlotsEnabled       = cfg.getBoolean("abilities.infinity_slots.enabled", true);
        this.infinitySlots              = cfg.getInt("abilities.infinity_slots.slots", 3);
        this.chargedSmashIntervalSeconds       = cfg.getInt("abilities.infinity_slots.charged-smash-interval-seconds", 60);
        this.netherStarPredeterminedRadiusBonus = cfg.getDouble("abilities.infinity_slots.nether-star-predetermined-radius-bonus", 3.0);
        this.netherStarNauseaDurationBonus      = cfg.getInt("abilities.infinity_slots.nether-star-nausea-duration-bonus", 40);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getMaceId()        { return maceId; }
    public String getHolderName()    { return holderName; }
    public String getDisplayName()   { return displayName; }
    public String getGradientStart() { return gradientStart; }
    public String getGradientEnd()   { return gradientEnd; }
    public String getItemType()      { return itemType; }
    public int getQuestTier()        { return questTier; }
    public void setQuestTier(int t)  { this.questTier = t; }

    // Poseidon
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

    // Larpers
    public boolean isLarperFreezeEnabled()           { return larperFreezeEnabled; }
    public int getLarperFreezeCooldown()             { return larperFreezeCooldown; }
    public double getLarperFreezeRadius()            { return larperFreezeRadius; }
    public int getLarperFreezeDuration()             { return larperFreezeDuration; }
    public int getLarperFreezeLine1Delay()           { return larperFreezeLine1Delay; }
    public int getLarperFreezeLine2Delay()           { return larperFreezeLine2Delay; }
    public String getLarperFreezeLine1()             { return larperFreezeLine1; }
    public String getLarperFreezeLine2()             { return larperFreezeLine2; }
    public int getLarperFreezeStrengthLevel()        { return larperFreezeStrengthLevel; }
    public int getLarperFreezeStrengthDuration()     { return larperFreezeStrengthDuration; }
    public boolean isLarperFreezeCombatImmunity()    { return larperFreezeCombatImmunity; }
    public int getLarperFreezeCombatImmunityWindow() { return larperFreezeCombatImmunityWindow; }

    public boolean isFalseSummonEnabled()       { return falseSummonEnabled; }
    public int getFalseSummonCooldown()         { return falseSummonCooldown; }
    public double getFalseSummonRadius()        { return falseSummonRadius; }
    public int getFalseSummonDecoyDuration()    { return falseSummonDecoyDuration; }
    public int getFalseSummonInvisDuration()    { return falseSummonInvisDuration; }
    public int getFalseSummonSpeedDuration()    { return falseSummonSpeedDuration; }
    public int getFalseSummonWeaknessDuration() { return falseSummonWeaknessDuration; }

    public boolean isMonologueEnabled()       { return monologueEnabled; }
    public int getMonologueCooldown()         { return monologueCooldown; }
    public int getMonologueStrengthLevel()    { return monologueStrengthLevel; }
    public int getMonologueDuration()         { return monologueDuration; }
    public String getMonologueLine()          { return monologueLine; }

    // Paraso
    public int getDashCooldown()               { return dashCooldown; }
    public double getDashVelocity()            { return dashVelocity; }
    public int getAbsentStackInterval()        { return absentStackInterval; }
    public int getAbsentMaxStacks()            { return absentMaxStacks; }
    public int getChaosTaxProcChance()         { return chaosTaxProcChance; }
    public boolean isUnannouncedEnabled()          { return unannouncedEnabled; }
    public int getUnannouncedCooldown()            { return unannouncedCooldown; }
    public double getUnannouncedRadius()           { return unannouncedRadius; }
    public int getUnannouncedFreezeDuration()      { return unannouncedFreezeDuration; }
    public int getUnannouncedHitWindow()           { return unannouncedHitWindow; }
    public int getUnannouncedSaturationLevel()     { return unannouncedSaturationLevel; }
    public int getUnannouncedSaturationDuration()  { return unannouncedSaturationDuration; }
    public boolean isJustShowedUpEnabled()         { return justShowedUpEnabled; }
    public int getJustShowedUpCooldown()           { return justShowedUpCooldown; }
    public int getJustShowedUpMinStacks()          { return justShowedUpMinStacks; }
    public double getJustShowedUpRadius()          { return justShowedUpRadius; }
    public int getJustShowedUpEffectDuration()     { return justShowedUpEffectDuration; }

    // Afterthought
    public boolean isNauseaPassiveEnabled()  { return nauseaPassiveEnabled; }
    public int getNauseaDuration()           { return nauseaDuration; }
    public int getNauseaCooldownPerTarget()  { return nauseaCooldownPerTarget; }

    // First Draft
    public double getSilentReadFarRange()              { return silentReadFarRange; }
    public double getSilentReadNearRange()             { return silentReadNearRange; }
    public int getSilentReadPingIntervalTicks()        { return silentReadPingIntervalTicks; }
    public int getCalculatedMaxFractureStacks()        { return calculatedMaxFractureStacks; }
    public int getCalculatedMissingHealthPercent()     { return calculatedMissingHealthPercent; }
    public int getContingencyCooldown()                { return contingencyCooldown; }
    public int getContingencyWindowSeconds()           { return contingencyWindowSeconds; }
    public int getContingencyCharges()                 { return contingencyCharges; }
    public int getContingencyPartialRefundSeconds()    { return contingencyPartialRefundSeconds; }
    public double getContingencyTeleportDistance()     { return contingencyTeleportDistance; }
    public int getContingencyBlindnessDuration()       { return contingencyBlindnessDuration; }
    public int getContingencySlownessDuration()        { return contingencySlownessDuration; }
    public int getContingencyAbsorptionDuration()      { return contingencyAbsorptionDuration; }
    public int getContingencyExecutionWindowSeconds()  { return contingencyExecutionWindowSeconds; }
    public int getPredeterminedCooldown()              { return predeterminedCooldown; }
    public double getPredeterminedRadius()             { return predeterminedRadius; }
    public int getPredeterminedDurationSeconds()       { return predeterminedDurationSeconds; }
    public int getPredeterminedSpeedDuration()         { return predeterminedSpeedDuration; }
    public boolean isGroundSlamEnabled()               { return groundSlamEnabled; }
    public double getGroundSlamMinFallDistance()       { return groundSlamMinFallDistance; }
    public boolean isInfinitySlotsEnabled()            { return infinitySlotsEnabled; }
    public int getInfinitySlots()                      { return infinitySlots; }
    public int getChargedSmashIntervalSeconds()        { return chargedSmashIntervalSeconds; }
    public double getNetherStarPredeterminedRadiusBonus() { return netherStarPredeterminedRadiusBonus; }
    public int getNetherStarNauseaDurationBonus()      { return netherStarNauseaDurationBonus; }
}
