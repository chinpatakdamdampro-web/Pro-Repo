package dev.hammermaces.listeners;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Pulses passive potion effects for all soulbound maces.
 *
 * Poseidon:
 *   Diamond Skin  → Resistance I while held
 *   Aquatic Soul  → Water Breathing + Dolphin's Grace while held
 *
 * Effects re-applied every second with duration slightly over pulse interval
 * so they never flicker or expire between pulses.
 */
public class MacePassiveListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;

    public MacePassiveListener(HammerMacesPlugin plugin) {
        this.plugin      = plugin;
        this.maceManager = plugin.getMaceManager();
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > 1_000_000) tick = 0;

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
                        apply(player, PotionEffectType.RESISTANCE,
                            cfg.getDiamondSkinPulse() + 20, cfg.getDiamondSkinLevel() - 1);
                    }

                    // Aquatic Soul
                    if (cfg.isAquaticSoulEnabled() && tick % cfg.getAquaticSoulPulse() == 0) {
                        apply(player, PotionEffectType.WATER_BREATHING,
                            cfg.getAquaticSoulPulse() + 20, 0);
                        apply(player, PotionEffectType.DOLPHINS_GRACE,
                            cfg.getAquaticSoulPulse() + 20, 0);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void apply(Player player, PotionEffectType type, int duration, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
    }
}
