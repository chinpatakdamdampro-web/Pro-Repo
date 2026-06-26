package dev.hammermaces.managers.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages Paraso's Absent passive — presence stacks that build over time.
 *
 * Stacks accumulate every stack-interval seconds while holding the mace.
 * Each stack grants additional effects. Stacks reset on log off or mace swap.
 *
 * Stack effects:
 *  1 → Speed I
 *  2 → Speed I + Strength I
 *  3 → Speed I + Strength I + Haste I
 *  4 → Speed II + Strength I + Haste I
 *  5 → Speed II + Strength II + Haste I + Resistance I
 */
public class PresenceStackManager {

    private final HammerMacesPlugin plugin;
    private final Map<UUID, Integer> stacks = new HashMap<>();
    private final Map<UUID, Long> ticksSinceLastStack = new HashMap<>();
    private BukkitTask task;

    public PresenceStackManager(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
                if (cfg == null) return;
                if (cfg.getQuestTier() < 4) return; // stacks only active tier 4+

                int intervalTicks = cfg.getAbsentStackInterval() * 20;

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (!isHoldingUnannounced(player)) continue;

                    UUID uuid = player.getUniqueId();
                    long ticks = ticksSinceLastStack.getOrDefault(uuid, 0L) + 1;
                    ticksSinceLastStack.put(uuid, ticks);

                    // Apply current stack effects every second
                    applyStackEffects(player, getStacks(uuid));

                    // Tick hold time for quest 4
                    if (ticks % 20 == 0) {
                        plugin.getParasoQuestManager().onHoldTimeTick(1);
                        // Show timer on action bar during quest 4 progress
                        if (cfg.getQuestTier() == 3) {
                            player.sendActionBar(plugin.getParasoQuestManager().buildTimerDisplay());
                        }
                    }

                    // Gain a stack
                    if (ticks >= intervalTicks) {
                        ticksSinceLastStack.put(uuid, 0L);
                        int current = stacks.getOrDefault(uuid, 0);
                        if (current < cfg.getAbsentMaxStacks()) {
                            int newStacks = current + 1;
                            stacks.put(uuid, newStacks);
                            // Quiet chime to holder only
                            player.playSound(player.getLocation(),
                                org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, 1.2f + (newStacks * 0.1f));
                            player.sendActionBar(
                                Component.text("▲ Presence " + newStacks + "/" + cfg.getAbsentMaxStacks())
                                    .color(TextColor.fromHexString("#C8A96E"))
                                    .decoration(TextDecoration.ITALIC, false)
                            );
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) task.cancel();
    }

    public int getStacks(UUID uuid) {
        return stacks.getOrDefault(uuid, 0);
    }

    public void resetStacks(UUID uuid) {
        stacks.remove(uuid);
        ticksSinceLastStack.remove(uuid);
    }

    public void consumeStacks(UUID uuid) {
        resetStacks(uuid);
    }

    private boolean isHoldingUnannounced(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!plugin.getMaceManager().isSoulboundMace(held)) return false;
        return "the_unannounced".equals(plugin.getMaceManager().getMaceType(held));
    }

    private void applyStackEffects(Player player, int stackCount) {
        if (stackCount <= 0) return;

        int duration = 60; // 3 seconds — re-applied every second so no flicker

        switch (stackCount) {
            case 1 -> {
                apply(player, PotionEffectType.SPEED, duration, 0);
            }
            case 2 -> {
                apply(player, PotionEffectType.SPEED, duration, 0);
                apply(player, PotionEffectType.STRENGTH, duration, 0);
            }
            case 3 -> {
                apply(player, PotionEffectType.SPEED, duration, 0);
                apply(player, PotionEffectType.STRENGTH, duration, 0);
                apply(player, PotionEffectType.HASTE, duration, 0);
            }
            case 4 -> {
                apply(player, PotionEffectType.SPEED, duration, 1);
                apply(player, PotionEffectType.STRENGTH, duration, 0);
                apply(player, PotionEffectType.HASTE, duration, 0);
            }
            default -> { // 5+
                apply(player, PotionEffectType.SPEED, duration, 1);
                apply(player, PotionEffectType.STRENGTH, duration, 1);
                apply(player, PotionEffectType.HASTE, duration, 0);
                apply(player, PotionEffectType.RESISTANCE, duration, 0);
            }
        }
    }

    private void apply(Player player, PotionEffectType type, int duration, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
    }
}
