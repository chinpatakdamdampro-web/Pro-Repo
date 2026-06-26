package dev.hammermaces.listeners;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core soulbound enforcement:
 *
 *  1. Death — removes soulbound items from drops at HIGHEST priority,
 *             stores them, returns them at PlayerRespawnEvent (most reliable hook).
 *
 *  2. Drop  — cancels manual Q-drops of soulbound items by their owner.
 *
 *  3. Join  — gives the player their soulbound item(s) if they don't already
 *             have them (handles first join after /givemace and server restarts).
 *             Also removes any soulbound item belonging to someone else.
 */
public class SoulboundListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;

    /** Stores items to return after respawn. UUID → list of items. */
    private final Map<UUID, List<ItemStack>> pendingReturn = new HashMap<>();

    public SoulboundListener(HammerMacesPlugin plugin) {
        this.plugin      = plugin;
        this.maceManager = plugin.getMaceManager();
    }

    // ── 1. Death ──────────────────────────────────────────────────────────────

    /**
     * HIGHEST priority so we run after most other plugins,
     * ensuring our removal of soulbound items sticks.
     * ignoreCancelled = false because death events are rarely cancelled.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasPermission("hammermaces.soulbound.bypass")) return;

        List<ItemStack> toReturn = new ArrayList<>();
        Iterator<ItemStack> it = event.getDrops().iterator();

        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (!maceManager.isSoulboundMace(drop)) continue;
            if (!maceManager.isOwner(drop, player.getName())) continue;
            it.remove();
            toReturn.add(drop.clone());
        }

        if (!toReturn.isEmpty()) {
            pendingReturn.put(player.getUniqueId(), toReturn);
        }
    }

    /**
     * Return items at respawn — this is the most reliable point.
     * The player's inventory exists and is ready at this event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> items = pendingReturn.remove(player.getUniqueId());
        if (items == null || items.isEmpty()) return;

        // Add directly — inventory is clean after death
        for (ItemStack item : items) {
            player.getInventory().addItem(item);
        }

        String msg = plugin.getConfig().getString(
            "messages.soulbound-return",
            "&#00e5ff✦ Your soulbound item has returned to you."
        );
        // Small delay so the message appears after respawn UI clears
        plugin.getServer().getScheduler().runTaskLater(plugin,
            () -> player.sendMessage(GradientUtils.parseLore(msg)), 5L);
    }

    // ── 2. Drop prevention ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!maceManager.isSoulboundMace(item)) return;
        if (!maceManager.isOwner(item, event.getPlayer().getName())) return;

        event.setCancelled(true);
        String msg = plugin.getConfig().getString(
            "messages.cannot-drop",
            "&#00e5ff✦ This item is soulbound — it cannot leave your side."
        );
        event.getPlayer().sendMessage(GradientUtils.parseLore(msg));
    }

    // ── 3. Join — give & validate ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay 1 tick so inventory is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Remove any soulbound items that don't belong to this player
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null) continue;
                if (!maceManager.isSoulboundMace(item)) continue;
                if (maceManager.isOwner(item, player.getName())) continue;
                player.getInventory().remove(item);
                String msg = plugin.getConfig().getString(
                    "messages.wrong-player-removed",
                    "&#FF6347✦ A soulbound item not bound to you was removed."
                );
                player.sendMessage(GradientUtils.parseLore(msg));
            }

            // Give any soulbound items this player is supposed to have but doesn't
            giveOwedItems(player);

        }, 1L);
    }

    /**
     * Checks every loaded mace config. If this player is the holder and
     * they don't already have the item in their inventory, give it to them.
     * This handles restarts and first-time distribution.
     */
    private void giveOwedItems(Player player) {
        for (MaceConfig cfg : plugin.getMaceConfigManager().getAllMaceConfigs()) {
            if (!cfg.getHolderName().equalsIgnoreCase(player.getName())) continue;

            // Special case: The Unannounced is only given after quest tier 1 is complete
            if (cfg.getMaceId().equals("the_unannounced") && cfg.getQuestTier() < 1) continue;

            // Check if player already has this specific soulbound item
            if (alreadyHas(player, cfg.getMaceId())) continue;

            // Give it
            ItemStack item = maceManager.createMace(cfg, player.getName());
            player.getInventory().addItem(item);
            plugin.getLogger().info("Auto-gave " + cfg.getMaceId() + " to " + player.getName() + " on join.");
        }
    }

    private boolean alreadyHas(Player player, String maceId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (!maceManager.isSoulboundMace(item)) continue;
            if (maceId.equals(maceManager.getMaceType(item))) return true;
        }
        return false;
    }
}
