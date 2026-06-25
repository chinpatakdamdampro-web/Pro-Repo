package dev.hammermaces.listeners;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runs a repeating task that applies passive potion effects to holders of soulbound maces.
 *
 * Diamond Skin  → Resistance I (or II based on config)
 * Aquatic Soul  → Water Breathing + Dolphin's Grace
 *
 * Effects are re-applied every pulse-interval ticks so they never expire while held.
 * We apply with a duration slightly longer than the pulse interval to avoid flicker.
 */
public class MacePassiveListener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;

    public MacePassiveListener(HammerMacesPlugin plugin) {
        this.plugin = plugin;
        this.maceManager = plugin.getMaceManager();
        startPassiveTask();
    }

    private void startPassiveTask() {
        // Check every 20 ticks (1 second) — each ability has its own interval check
        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (!maceManager.isSoulboundMace(held)) continue;
                    if (!maceManager.isOwner(held, player.getName())) continue;

                    String maceId = maceManager.getMaceType(held);
                    if (maceId == null) continue;

                    MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
                    if (cfg == null) continue;

                    // Diamond Skin
                    if (cfg.isDiamondSkinEnabled() && tick % cfg.getDiamondSkinPulse() == 0) {
                        applyEffect(player, PotionEffectType.RESISTANCE,
                            cfg.getDiamondSkinPulse() + 20,  // duration slightly over pulse so no flicker
                            cfg.getDiamondSkinLevel() - 1);  // amplifier is level - 1
                    }

                    // Aquatic Soul
                    if (cfg.isAquaticSoulEnabled() && tick % cfg.getAquaticSoulPulse() == 0) {
                        applyEffect(player, PotionEffectType.WATER_BREATHING,
                            cfg.getAquaticSoulPulse() + 20, 0);
                        applyEffect(player, PotionEffectType.DOLPHINS_GRACE,
                            cfg.getAquaticSoulPulse() + 20, 0);
                    }
                }

                // Reset tick counter to avoid overflow on long sessions
                if (tick > 1_000_000) tick = 0;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void applyEffect(Player player, PotionEffectType type, int durationTicks, int amplifier) {
        player.addPotionEffect(new PotionEffect(
            type,
            durationTicks,
            amplifier,
            true,   // ambient (true = smaller particles, less intrusive)
            false,  // particles visible
            false   // no icon in hotbar (keeps HUD clean)
        ));
    }
}
