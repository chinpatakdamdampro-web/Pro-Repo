package dev.hammermaces.data;

import dev.hammermaces.HammerMacesPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class ParasoQuestData {

    private final HammerMacesPlugin plugin;
    private final File dataFile;
    private FileConfiguration cfg;

    private int currentTier;
    private Set<String> biomesVisited;
    private int blazeRodsCollected;
    private int nautilusCollected;
    private Set<String> playersHit;
    private long holdTimeSeconds;
    private boolean justShowedUpFired;

    // Required counts for quest 2
    public static final int BLAZE_RODS_REQUIRED  = 4;
    public static final int NAUTILUS_REQUIRED     = 3;

    public ParasoQuestData(HammerMacesPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "paraso_quests.yml");
        load();
    }

    public void load() {
        if (!dataFile.exists()) {
            try (InputStream in = plugin.getResource("paraso_quests.yml")) {
                if (in != null) {
                    try (OutputStream out = Files.newOutputStream(dataFile.toPath())) {
                        in.transferTo(out);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create paraso_quests.yml", e);
            }
        }

        cfg = YamlConfiguration.loadConfiguration(dataFile);
        currentTier         = cfg.getInt("current-tier", 0);
        biomesVisited       = new HashSet<>(cfg.getStringList("progress.biomes-visited"));
        blazeRodsCollected  = cfg.getInt("progress.blaze-rods-collected", 0);
        nautilusCollected   = cfg.getInt("progress.nautilus-collected", 0);
        playersHit          = new HashSet<>(cfg.getStringList("progress.players-hit"));
        holdTimeSeconds     = cfg.getLong("progress.hold-time-seconds", 0);
        justShowedUpFired   = cfg.getBoolean("progress.just-showed-up-fired", false);
    }

    public void save() {
        cfg.set("current-tier", currentTier);
        cfg.set("progress.biomes-visited", new ArrayList<>(biomesVisited));
        cfg.set("progress.blaze-rods-collected", blazeRodsCollected);
        cfg.set("progress.nautilus-collected", nautilusCollected);
        cfg.set("progress.players-hit", new ArrayList<>(playersHit));
        cfg.set("progress.hold-time-seconds", holdTimeSeconds);
        cfg.set("progress.just-showed-up-fired", justShowedUpFired);
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save paraso_quests.yml", e);
        }
    }

    // ── Getters & setters ─────────────────────────────────────────────────────

    public int getCurrentTier()                    { return currentTier; }
    public void setCurrentTier(int t)             { this.currentTier = t; }

    public Set<String> getBiomesVisited()          { return biomesVisited; }
    public void addBiome(String b)                { biomesVisited.add(b); }

    public int getBlazeRodsCollected()             { return blazeRodsCollected; }
    public void addBlazeRod()                     { blazeRodsCollected = Math.min(blazeRodsCollected + 1, BLAZE_RODS_REQUIRED); }

    public int getNautilusCollected()              { return nautilusCollected; }
    public void addNautilus()                     { nautilusCollected = Math.min(nautilusCollected + 1, NAUTILUS_REQUIRED); }

    public Set<String> getPlayersHit()             { return playersHit; }
    public void addPlayerHit(String n)            { playersHit.add(n.toLowerCase()); }

    public long getHoldTimeSeconds()               { return holdTimeSeconds; }
    public void addHoldTime(long s)               { holdTimeSeconds += s; }
    public void setHoldTimeSeconds(long s)        { holdTimeSeconds = s; }

    public boolean isJustShowedUpFired()           { return justShowedUpFired; }
    public void setJustShowedUpFired(boolean b)   { justShowedUpFired = b; }

    public boolean isFusionComplete() {
        return blazeRodsCollected >= BLAZE_RODS_REQUIRED
            && nautilusCollected  >= NAUTILUS_REQUIRED;
    }
}
