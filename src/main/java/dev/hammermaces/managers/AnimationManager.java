package dev.hammermaces.managers;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Runs a repeating task that updates the display name of every soulbound mace
 * in every online player's inventory with the current animation frame.
 *
 * The offset advances each tick, making the gradient shimmer across the name.
 */
public class AnimationManager {

    private final HammerMacesPlugin plugin;
    private BukkitTask task;
    private float offset = 0f;

    // How fast the shimmer moves. Higher = faster wave.
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
                    updateInventory(player);
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    public void stopAnimationTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private void updateInventory(Player player) {
        MaceManager maceManager = plugin.getMaceManager();
        MaceConfigManager cfgManager = plugin.getMaceConfigManager();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (!maceManager.isSoulboundMace(item)) continue;

            String maceId = maceManager.getMaceType(item);
            if (maceId == null) continue;

            MaceConfig cfg = cfgManager.getMaceConfig(maceId);
            if (cfg == null) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            Component animatedName = GradientUtils.buildAnimatedGradient(
                cfg.getDisplayName(),
                cfg.getGradientStart(),
                cfg.getGradientEnd(),
                offset
            );

            meta.displayName(animatedName);
            item.setItemMeta(meta);
        }
    }
}
