package dev.hammermaces.listeners;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Handles all soulbound logic:
 *   - Prevents mace from dropping on death, returns it on respawn
 *   - Prevents the holder from manually dropping their mace
 *   - On join: removes any soulbound mace that doesn't belong to this player
 */
public class SoulboundListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;

    public SoulboundListener(HammerMacesPlugin plugin) {
        this.plugin = plugin;
        this.maceManager = plugin.getMaceManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Admins with bypass can lose their mace normally
        if (player.hasPermission("hammermaces.soulbound.bypass")) return;

        List<ItemStack> toReturn = new ArrayList<>();
        Iterator<ItemStack> drops = event.getDrops().iterator();

        while (drops.hasNext()) {
            ItemStack drop = drops.next();
            if (!maceManager.isSoulboundMace(drop)) continue;
            if (!maceManager.isOwner(drop, player.getName())) continue;

            drops.remove();
            toReturn.add(drop.clone());
        }

        if (toReturn.isEmpty()) return;

        // Give maces back 1 second after death — safely after respawn screen
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player respawned = plugin.getServer().getPlayer(player.getUniqueId());
            if (respawned == null) return;

            for (ItemStack mace : toReturn) {
                respawned.getInventory().addItem(mace);
            }

            String msg = plugin.getConfig().getString(
                "messages.soulbound-return",
                "&#1a1a6e✦ &#00e5ffYour soulbound mace has returned to you."
            );
            respawned.sendMessage(GradientUtils.parseLore(msg));

        }, 20L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!maceManager.isSoulboundMace(item)) return;

        Player player = event.getPlayer();
        if (!maceManager.isOwner(item, player.getName())) return;

        event.setCancelled(true);

        String msg = plugin.getConfig().getString(
            "messages.cannot-drop",
            "&#1a1a6e✦ &#00e5ffThis mace is soulbound — it cannot leave your side."
        );
        player.sendMessage(GradientUtils.parseLore(msg));
    }

    /**
     * On join: verify every soulbound mace in the player's inventory belongs to them.
     * If not, remove it. This is a safety net — shouldn't happen in normal play.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null) continue;
                if (!maceManager.isSoulboundMace(item)) continue;
                if (maceManager.isOwner(item, player.getName())) continue;

                // Wrong player — remove
                player.getInventory().remove(item);
                String msg = plugin.getConfig().getString(
                    "messages.wrong-player-removed",
                    "&#FF6347✦ A soulbound mace not bound to you was removed."
                );
                player.sendMessage(GradientUtils.parseLore(msg));
            }
        }, 5L);
    }
}
