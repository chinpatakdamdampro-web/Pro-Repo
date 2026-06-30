package dev.hammermaces.abilities.architect;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Predetermined — Sneak + Swing
 *
 * Sets a zone (radius blocks) around the holder for duration-seconds.
 * Any player entering that zone:
 *   - Gets pulled slightly toward the holder
 *   - Gets Slowness II + Weakness I
 *   - Starts with 1 Fracture stack already applied
 *
 * Holder gets Speed II for speed-duration ticks on activation.
 * Message to holder: [ ᴇxᴇᴄᴜᴛɪɴɢ ᴘʟᴀɴ ]
 * Targets get no message or indicator.
 *
 * If Nether Star is slotted in Infinity Slots, radius gets +netherStarBonus blocks.
 */
public class PredeterminedAbility {

    private final HammerMacesPlugin plugin;

    // Active zones: holder UUID -> zone state
    private final Map<UUID, ZoneState> activeZones = new HashMap<>();

    public PredeterminedAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player holder, MaceConfig cfg, double extraRadius) {
        UUID uuid = holder.getUniqueId();

        // Cancel existing zone if any
        ZoneState existing = activeZones.get(uuid);
        if (existing != null && existing.task != null) existing.task.cancel();

        double radius = cfg.getPredeterminedRadius() + extraRadius;
        Location center = holder.getLocation().clone();
        long durationTicks = cfg.getPredeterminedDurationSeconds() * 20L;

        // Speed II to holder
        holder.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED, cfg.getPredeterminedSpeedDuration(), 1, false, false, false));

        // Particles showing zone boundary
        plugin.getParticleEffects().predeterminedActivate(center, radius);

        // Message to holder only
        holder.sendMessage(Component.text("[ ᴇxᴇᴄᴜᴛɪɴɢ ᴘʟᴀɴ ]")
            .color(TextColor.fromHexString("#6a8090"))
            .decoration(TextDecoration.ITALIC, false));

        // Sound — mechanical, precise
        holder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 1.2f);

        // Set to track players already processed this tick to avoid spam
        Set<UUID> hitThisActivation = new HashSet<>();

        ZoneState state = new ZoneState();
        state.task = new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || !holder.isOnline()) {
                    activeZones.remove(uuid);
                    cancel();
                    return;
                }

                // Check every 5 ticks
                if (ticks % 5 == 0) {
                    for (Player target : holder.getWorld().getPlayers()) {
                        if (target.equals(holder)) continue;
                        if (target.getLocation().distanceSquared(center) > radius * radius) continue;
                        if (hitThisActivation.contains(target.getUniqueId())) continue;

                        hitThisActivation.add(target.getUniqueId());
                        onEnter(holder, target, center);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeZones.put(uuid, state);

        // Auto-cancel after duration
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ZoneState s = activeZones.get(uuid);
            if (s != null && s.task != null) s.task.cancel();
            activeZones.remove(uuid);
        }, durationTicks);
    }

    private void onEnter(Player holder, Player target, Location center) {
        // Pull slightly toward center
        Vector pull = center.toVector().subtract(target.getLocation().toVector())
            .normalize().multiply(0.4).add(new Vector(0, 0.15, 0));
        target.setVelocity(pull);

        // Debuffs
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true, true));

        // Add initial fracture stack
        plugin.getFractureManager().addInitialStacks(target.getUniqueId(), 1);

        // Trigger particle
        plugin.getParticleEffects().predeterminedTrigger(target.getLocation());
    }

    public boolean isActive(UUID uuid) {
        return activeZones.containsKey(uuid);
    }

    private static class ZoneState {
        BukkitTask task;
    }
}
