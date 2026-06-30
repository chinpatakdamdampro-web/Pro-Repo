package dev.hammermaces.listeners.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Quest 1 — tracks biomes Paraso visits while holding The Unannounced.
 * Uses PlayerMoveEvent block-level checks to avoid spam.
 */
public class QuestBiomeListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;

    public QuestBiomeListener(HammerMacesPlugin plugin) {
        this.plugin      = plugin;
        this.maceManager = plugin.getMaceManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Only fire when crossing a block boundary to reduce overhead
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!"the_unannounced".equals(maceManager.getMaceType(held))) return;
        if (!maceManager.isOwner(held, player.getName())) return;

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg == null || cfg.getQuestTier() >= 1) return; // already passed tier 1

        // World.getBiome(Location) is the non-deprecated way in Paper 1.21.x
        String biomeName = player.getWorld().getBiome(player.getLocation()).getKey().toString();
        plugin.getParasoQuestManager().onBiomeEnter(biomeName);
    }
}
