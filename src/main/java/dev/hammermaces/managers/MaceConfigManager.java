package dev.hammermaces.managers;

import dev.hammermaces.HammerMacesPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handles loading of all individual mace .yml config files from /plugins/HammerMaces/maces/.
 * Copies default mace files from the plugin jar on first run.
 */
public class MaceConfigManager {

    private final HammerMacesPlugin plugin;
    private final File macesFolder;

    // maceId (filename without .yml) -> parsed MaceConfig
    private final Map<String, MaceConfig> loadedMaces = new HashMap<>();

    // holderName (lowercase) -> maceId, for fast lookup on join/events
    private final Map<String, String> holderToMaceId = new HashMap<>();

    public MaceConfigManager(HammerMacesPlugin plugin) {
        this.plugin = plugin;
        this.macesFolder = new File(plugin.getDataFolder(), "maces");
    }

    public void loadAll() {
        loadedMaces.clear();
        holderToMaceId.clear();

        // Create /maces/ directory if it doesn't exist
        if (!macesFolder.exists()) {
            macesFolder.mkdirs();
        }

        // Copy default mace files from the jar if not already present
        copyDefaultMaceFiles();

        // Load every .yml in the /maces/ folder
        File[] files = macesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No mace config files found in /maces/! Nothing to load.");
            return;
        }

        for (File file : files) {
            String maceId = file.getName().replace(".yml", "");
            try {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                MaceConfig maceConfig = new MaceConfig(maceId, cfg);
                loadedMaces.put(maceId, maceConfig);
                holderToMaceId.put(maceConfig.getHolderName().toLowerCase(), maceId);
                plugin.getLogger().info("Loaded mace: " + maceId + " → holder: " + maceConfig.getHolderName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load mace config: " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + loadedMaces.size() + " mace(s).");
    }

    /**
     * Copies all .yml files from the jar's /maces/ resource folder into the plugin data folder.
     * Only copies if the file doesn't already exist (won't overwrite edits).
     */
    private void copyDefaultMaceFiles() {
        String[] defaultMaces = { "hammer_of_poseidon.yml" };
        for (String fileName : defaultMaces) {
            File dest = new File(macesFolder, fileName);
            if (!dest.exists()) {
                try (InputStream in = plugin.getResource("maces/" + fileName)) {
                    if (in != null) {
                        try (OutputStream out = Files.newOutputStream(dest.toPath())) {
                            in.transferTo(out);
                            plugin.getLogger().info("Generated default mace config: maces/" + fileName);
                        }
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not copy default mace config: " + fileName, e);
                }
            }
        }
    }

    public MaceConfig getMaceConfig(String maceId) {
        return loadedMaces.get(maceId);
    }

    public MaceConfig getMaceConfigByHolder(String playerName) {
        String maceId = holderToMaceId.get(playerName.toLowerCase());
        return maceId != null ? loadedMaces.get(maceId) : null;
    }

    public Collection<MaceConfig> getAllMaceConfigs() {
        return loadedMaces.values();
    }

    public Collection<String> getAllMaceIds() {
        return loadedMaces.keySet();
    }
}
