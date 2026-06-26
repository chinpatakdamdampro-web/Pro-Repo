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

public class MaceConfigManager {

    private final HammerMacesPlugin plugin;
    private final File macesFolder;

    private final Map<String, MaceConfig> loadedMaces   = new HashMap<>();
    private final Map<String, String> holderToMaceId    = new HashMap<>();

    private static final String[] DEFAULT_MACE_FILES = {
        "hammer_of_poseidon.yml",
        "hammer_of_the_larpers.yml",
        "the_unannounced.yml",
        "the_afterthought.yml"
    };

    public MaceConfigManager(HammerMacesPlugin plugin) {
        this.plugin = plugin;
        this.macesFolder = new File(plugin.getDataFolder(), "maces");
    }

    public void loadAll() {
        loadedMaces.clear();
        holderToMaceId.clear();

        if (!macesFolder.exists()) macesFolder.mkdirs();

        copyDefaultMaceFiles();

        File[] files = macesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No mace config files found in /maces/!");
            return;
        }

        for (File file : files) {
            String maceId = file.getName().replace(".yml", "");
            try {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                MaceConfig mc = new MaceConfig(maceId, cfg);
                loadedMaces.put(maceId, mc);
                // Only map holder → maceId if holder is set
                if (!mc.getHolderName().equalsIgnoreCase("YourNameHere")) {
                    holderToMaceId.put(mc.getHolderName().toLowerCase(), maceId);
                }
                plugin.getLogger().info("Loaded: " + maceId + " → holder: " + mc.getHolderName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load mace config: " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + loadedMaces.size() + " soulbound item(s).");
    }

    private void copyDefaultMaceFiles() {
        for (String fileName : DEFAULT_MACE_FILES) {
            File dest = new File(macesFolder, fileName);
            if (!dest.exists()) {
                try (InputStream in = plugin.getResource("maces/" + fileName)) {
                    if (in != null) {
                        try (OutputStream out = Files.newOutputStream(dest.toPath())) {
                            in.transferTo(out);
                            plugin.getLogger().info("Generated default config: maces/" + fileName);
                        }
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not copy: " + fileName, e);
                }
            }
        }
    }

    /**
     * Persists the quest-tier back to the mace's yml file so it survives restarts.
     */
    public void saveQuestTier(String maceId, int tier) {
        File file = new File(macesFolder, maceId + ".yml");
        if (!file.exists()) return;
        try {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            cfg.set("quest-tier", tier);
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save quest-tier for " + maceId, e);
        }
    }

    public MaceConfig getMaceConfig(String maceId)       { return loadedMaces.get(maceId); }
    public MaceConfig getMaceConfigByHolder(String name) {
        String id = holderToMaceId.get(name.toLowerCase());
        return id != null ? loadedMaces.get(id) : null;
    }
    public Collection<MaceConfig> getAllMaceConfigs()     { return loadedMaces.values(); }
    public Collection<String> getAllMaceIds()             { return loadedMaces.keySet(); }
}
