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

    private final Map<String, MaceConfig> loadedMaces = new HashMap<>();
    private final Map<String, String> holderToMaceId  = new HashMap<>();

    // Add new default mace filenames here as you create them
    private static final String[] DEFAULT_MACE_FILES = {
        "hammer_of_poseidon.yml",
        "hammer_of_the_larpers.yml"
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

    private void copyDefaultMaceFiles() {
        for (String fileName : DEFAULT_MACE_FILES) {
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

    public MaceConfig getMaceConfig(String maceId)          { return loadedMaces.get(maceId); }
    public MaceConfig getMaceConfigByHolder(String name)    { return loadedMaces.get(holderToMaceId.get(name.toLowerCase())); }
    public Collection<MaceConfig> getAllMaceConfigs()        { return loadedMaces.values(); }
    public Collection<String> getAllMaceIds()                { return loadedMaces.keySet(); }
            }
