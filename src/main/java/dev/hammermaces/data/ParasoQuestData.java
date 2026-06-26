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
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Persistent quest progress data for Paraso.
 * Saved to /plugins/HammerMaces/paraso_quests.yml
 */
public class ParasoQuestData {

    private final HammerMacesPlugin plugin;
    private final File dataFile;
    private FileConfiguration cfg;

    // Quest progress fields
    private int currentTier;
    private Set<String> biomesVisited;
    private Set<String> loginDays;
    private Set<String> playersHit;
    private long holdTimeSeconds;
    private boolean justShowedUpFired;

    public ParasoQuestData(HammerMacesPlugin plugin) {
        this.plugin = plugin;
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

        currentTier      = cfg.getInt("current-tier", 0);
        biomesVisited    = new HashSet<>(cfg.getStringList("progress.biomes-visited"));
        loginDays        = new HashSet<>(cfg.getStringList("progress.login-days"));
        playersHit       = new HashSet<>(cfg.getStringList("progress.players-hit"));
        holdTimeSeconds  = cfg.getLong("progress.hold-time-seconds", 0);
        justShowedUpFired = cfg.getBoolean("progress.just-showed-up-fired", false);
    }

    public void save() {
        cfg.set("current-tier", currentTier);
        cfg.set("progress.biomes-visited", new ArrayList<>(biomesVisited));
        cfg.set("progress.login-days", new ArrayList<>(loginDays));
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

    public int getCurrentTier()                     { return currentTier; }
    public void setCurrentTier(int tier)            { this.currentTier = tier; }

    public Set<String> getBiomesVisited()           { return biomesVisited; }
    public void addBiome(String biome)              { biomesVisited.add(biome); }

    public Set<String> getLoginDays()               { return loginDays; }
    public void addLoginDay(String day)             { loginDays.add(day); }

    public Set<String> getPlayersHit()              { return playersHit; }
    public void addPlayerHit(String name)           { playersHit.add(name.toLowerCase()); }

    public long getHoldTimeSeconds()                { return holdTimeSeconds; }
    public void addHoldTime(long seconds)           { holdTimeSeconds += seconds; }
    public void setHoldTimeSeconds(long s)          { holdTimeSeconds = s; }

    public boolean isJustShowedUpFired()            { return justShowedUpFired; }
    public void setJustShowedUpFired(boolean b)     { justShowedUpFired = b; }
}
