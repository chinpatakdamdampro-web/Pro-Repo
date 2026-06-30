package dev.hammermaces.managers;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Updates animated gradient names on soulbound items.
 *
 * IMPORTANT: Only updates items in the hotbar (slots 0-8) and offhand —
 * NOT the entire inventory contents array. Updating items that aren't
 * currently rendered on screen (backpack slots) causes the client to
 * receive unnecessary item-update packets, which is what caused the
 * "animation spams when pulling the item out" visual bug — every single
 * inventory item was getting a brand new ItemMeta identity every 2 ticks,
 * including ones not even visible, flooding the client with updates.
 */
public class AnimationManager {

    private final HammerMacesPlugin plugin;
    private BukkitTask task;
    private float offset = 0f;

    private static final float OFFSET_INCREMENT = 0.025f;

    public AnimationManager(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void startAnimationTask() {
        int interval = plugin.getConfig().getInt("animation.update-interval", 2);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                offset += OFFSET_INCREMENT;
                if (offset >= 1f) offset -= 1f;

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    updateVisibleItems(player);
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    public void stopAnimationTask() {
        if (task != null && !task.isCancelled()) task.cancel();
    }

    /**
     * Only updates hotbar (0-8) and offhand — the only slots actually
     * rendered to the player at any time. Backpack slots are skipped
     * entirely since animating them is wasted bandwidth and causes
     * visible item flicker when the player later opens their inventory.
     */
    private void updateVisibleItems(Player player) {
        PlayerInventory inv = player.getInventory();

        for (int slot = 0; slot <= 8; slot++) {
            updateSlot(inv, slot, inv.getItem(slot));
        }
        updateSlot(inv, 40, inv.getItemInOffHand()); // 40 = offhand slot index
    }

    private void updateSlot(PlayerInventory inv, int slot, ItemStack item) {
        if (item == null) return;

        MaceManager mm = plugin.getMaceManager();
        if (!mm.isSoulboundMace(item)) return;

        String maceId = mm.getMaceType(item);
        if (maceId == null) return;

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Only the display name component changes — meta object itself is reused,
        // not replaced, minimizing the diff the client has to process.
        Component animatedName = GradientUtils.buildAnimatedGradient(
            cfg.getDisplayName(), cfg.getGradientStart(), cfg.getGradientEnd(), offset);

        meta.displayName(animatedName);
        item.setItemMeta(meta);

        // Push back into the exact slot explicitly rather than relying on
        // array reference mutation from getContents(), which is not
        // guaranteed to reflect back into the live inventory on all builds.
        inv.setItem(slot, item);
    }
}
