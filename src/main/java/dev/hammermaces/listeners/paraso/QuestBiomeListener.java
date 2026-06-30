package dev.hammermaces.listeners.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Quest 1 — tracks biomes Paraso visits.
 *
 * IMPORTANT: This quest is what AWARDS the mace, so it must NOT require
 * already holding the mace. It only checks if this player is the configured
 * holder name in the_unannounced.yml.
 */
public class QuestBiomeListener implements Listener {

    private final HammerMacesPlugin plugin;

    public QuestBiomeListener(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg == null) return;

        // Identity check only — does NOT require holding the mace
        if (!cfg.getHolderName().equalsIgnoreCase(player.getName())) return;

        // Already past tier 1, no need to track further
        if (cfg.getQuestTier() >= 1) return;

        String biomeName = player.getWorld().getBiome(player.getLocation()).getKey().toString();
        plugin.getParasoQuestManager().onBiomeEnter(biomeName);
    }
}
