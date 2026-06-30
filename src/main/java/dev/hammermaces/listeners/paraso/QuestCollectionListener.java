package dev.hammermaces.listeners.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.abilities.paraso.FusionAnimationTask;
import dev.hammermaces.data.ParasoQuestData;
import dev.hammermaces.managers.MaceConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Tracks Blaze Rod and Nautilus Shell collection for Paraso's Quest 2.
 *
 * IMPORTANT: Only checks player identity against the configured holder name —
 * does NOT require holding the mace, since items can be collected any time
 * once tier 1 is reached.
 */
public class QuestCollectionListener implements Listener {

    private final HammerMacesPlugin plugin;

    public QuestCollectionListener(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isCurrentParaso(player)) return;

        Material mat = event.getItem().getItemStack().getType();
        handleCollection(player, mat);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isCurrentParaso(player)) return;

        ItemStack cursor = event.getCursor();
        if (cursor == null) return;
        handleCollection(player, cursor.getType());
    }

    private void handleCollection(Player player, Material mat) {
        if (mat != Material.BLAZE_ROD && mat != Material.NAUTILUS_SHELL) return;

        ParasoQuestData data = plugin.getParasoQuestData();

        if (mat == Material.BLAZE_ROD && data.getBlazeRodsCollected() < ParasoQuestData.BLAZE_RODS_REQUIRED) {
            data.addBlazeRod();
            data.save();
            sendProgress(player, data);
        } else if (mat == Material.NAUTILUS_SHELL && data.getNautilusCollected() < ParasoQuestData.NAUTILUS_REQUIRED) {
            data.addNautilus();
            data.save();
            sendProgress(player, data);
        }

        if (data.isFusionComplete()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                startFusion(player);
            }, 1L);
        }
    }

    private void startFusion(Player player) {
        ParasoQuestData data = plugin.getParasoQuestData();
        if (!data.isFusionComplete()) return;

        removeItems(player, Material.BLAZE_ROD, ParasoQuestData.BLAZE_RODS_REQUIRED);
        removeItems(player, Material.NAUTILUS_SHELL, ParasoQuestData.NAUTILUS_REQUIRED);

        new FusionAnimationTask(plugin, player, () -> {
            plugin.getParasoQuestManager().onFusionComplete(player);
        }).runTaskTimer(plugin, 0L, 1L);
    }

    private void removeItems(Player player, Material mat, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != mat) continue;
            if (item.getAmount() >= remaining) {
                item.setAmount(item.getAmount() - remaining);
                break;
            } else {
                remaining -= item.getAmount();
                item.setAmount(0);
            }
        }
    }

    private void sendProgress(Player player, ParasoQuestData data) {
        Component msg = Component.text("🔥 ")
            .color(TextColor.fromHexString("#FF6600"))
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(data.getBlazeRodsCollected() + "/" + ParasoQuestData.BLAZE_RODS_REQUIRED)
                .color(TextColor.fromHexString("#F5F0E8"))
                .decoration(TextDecoration.ITALIC, false))
            .append(Component.text("   🐚 ")
                .color(TextColor.fromHexString("#00CCFF"))
                .decoration(TextDecoration.ITALIC, false))
            .append(Component.text(data.getNautilusCollected() + "/" + ParasoQuestData.NAUTILUS_REQUIRED)
                .color(TextColor.fromHexString("#F5F0E8"))
                .decoration(TextDecoration.ITALIC, false));
        player.sendActionBar(msg);
    }

    /**
     * Checks identity AND that tier is exactly 1 (quest 2 active).
     * Does not require possessing the mace.
     */
    private boolean isCurrentParaso(Player player) {
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg == null) return false;
        if (!cfg.getHolderName().equalsIgnoreCase(player.getName())) return false;
        return cfg.getQuestTier() == 1;
    }
}
