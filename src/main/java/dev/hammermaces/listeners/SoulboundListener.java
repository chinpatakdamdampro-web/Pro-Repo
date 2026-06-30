package dev.hammermaces.listeners;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
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
 *  1. Death — removes soulbound drops, returns at respawn
 *  2. Drop  — cancels Q-drops
 *  3. Join  — gives owed items, removes wrong-owner items, records login for quests
 *  4. Fall  — Paraso's 1-block mace trigger (tier 2+)
 */
public class SoulboundListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;
    private final Map<UUID, List<ItemStack>> pendingReturn = new HashMap<>();

    public SoulboundListener(HammerMacesPlugin plugin) {
        this.plugin      = plugin;
        this.maceManager = plugin.getMaceManager();
    }

    // ── 1. Death ──────────────────────────────────────────────────────────────

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
        if (!toReturn.isEmpty()) pendingReturn.put(player.getUniqueId(), toReturn);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> items = pendingReturn.remove(player.getUniqueId());
        if (items == null || items.isEmpty()) return;
        for (ItemStack item : items) player.getInventory().addItem(item);
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
            player.sendMessage(GradientUtils.parseLore(plugin.getConfig().getString(
                "messages.soulbound-return", "&#00e5ff✦ Your soulbound item has returned to you."))), 5L);
    }

    // ── 2. Drop prevention ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!maceManager.isSoulboundMace(item)) return;
        if (!maceManager.isOwner(item, event.getPlayer().getName())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(GradientUtils.parseLore(plugin.getConfig().getString(
            "messages.cannot-drop", "&#00e5ff✦ This item is soulbound — it cannot leave your side.")));
    }

    // ── 3. Join — give & validate, record login ───────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Record login for Paraso quest tracking
        MaceConfig paraso = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (paraso != null && paraso.getHolderName().equalsIgnoreCase(player.getName())) {
            plugin.getParasoQuestManager().onLogin();
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Remove soulbound items that don't belong to this player
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null) continue;
                if (!maceManager.isSoulboundMace(item)) continue;
                if (maceManager.isOwner(item, player.getName())) continue;
                player.getInventory().remove(item);
                player.sendMessage(GradientUtils.parseLore(plugin.getConfig().getString(
                    "messages.wrong-player-removed", "&#FF6347✦ A soulbound item not bound to you was removed.")));
            }

            // Give owed items
            giveOwedItems(player);
        }, 1L);
    }

    private void giveOwedItems(Player player) {
        for (MaceConfig cfg : plugin.getMaceConfigManager().getAllMaceConfigs()) {
            if (!cfg.getHolderName().equalsIgnoreCase(player.getName())) continue;
            if (cfg.getMaceId().equals("the_unannounced") && cfg.getQuestTier() < 1) continue;
            if (alreadyHas(player, cfg.getMaceId())) continue;
            ItemStack item = maceManager.createMace(cfg, player.getName());
            player.getInventory().addItem(item);
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

    // ── 4. Paraso 1-block fall trigger ────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onParasoFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // Only apply to Paraso at tier 2+
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg == null || cfg.getQuestTier() < 2) return;
        if (!cfg.getHolderName().equalsIgnoreCase(player.getName())) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!"the_unannounced".equals(maceManager.getMaceType(held))) return;

        // Trigger mace hit at 1 block fall (vanilla requires 1.5)
        if (player.getFallDistance() >= 1.0f) {
            // Cancel the fall damage — mace absorbs it
            event.setCancelled(true);
            // Simulate mace smash by giving the player the MACE attack bonus
            // via a slight velocity push downward so vanilla mace hit detection fires
            player.setVelocity(player.getVelocity().setY(-0.5));
        }
    }
}
