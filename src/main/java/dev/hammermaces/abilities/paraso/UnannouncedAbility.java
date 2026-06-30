package dev.hammermaces.abilities.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.utils.FreezeUtils;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unannounced — Predator Teleport
 *
 * Finds nearest player within radius. Freezes them silently.
 * Teleports Paraso directly behind them.
 *
 * If Paraso lands a hit within hit-window seconds:
 *   → Just Showed Up cooldown resets
 *   → Saturation 255 for saturation-duration ticks (immortality)
 *
 * If window expires without hit: target unfreezes, no bonus.
 */
public class UnannouncedAbility {

    private final HammerMacesPlugin plugin;
    private final Map<UUID, UUID> activeWindows = new HashMap<>();

    public UnannouncedAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player paraso, MaceConfig cfg) {
        double radius = cfg.getUnannouncedRadius();

        Player target = paraso.getWorld().getPlayers().stream()
            .filter(p -> !p.equals(paraso))
            .filter(p -> p.getLocation().distanceSquared(paraso.getLocation()) <= radius * radius)
            .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(paraso.getLocation())))
            .orElse(null);

        if (target == null) {
            paraso.sendMessage(GradientUtils.parseLore("&#C8A96E👻 No target in range."));
            plugin.getCooldownManager().clearAbility(paraso.getUniqueId(), "the_unannounced", "unannounced");
            return;
        }

        long freezeTicks = cfg.getUnannouncedFreezeDuration() * 20L;

        // Freeze target silently — no message, no particles
        FreezeUtils.freeze(plugin, target, freezeTicks);

        // Teleport behind target
        paraso.teleport(getBehindLocation(target));

        // Barely audible sound to Paraso only
        paraso.playSound(paraso.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.25f, 1.8f);

        UUID parasoId = paraso.getUniqueId();
        UUID targetId = target.getUniqueId();
        activeWindows.put(parasoId, targetId);

        // Unfreeze after freeze-duration regardless of hit
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (targetId.equals(activeWindows.get(parasoId))) {
                activeWindows.remove(parasoId);
            }
            FreezeUtils.unfreeze(targetId);
        }, freezeTicks);
    }

    /**
     * Called from ChaosTaxListener when Paraso hits someone while mace is held.
     * Returns true if the hit was on the active window target.
     */
    public boolean onHit(Player paraso, Player hitTarget, MaceConfig cfg) {
        UUID parasoId = paraso.getUniqueId();
        UUID targetId = activeWindows.get(parasoId);
        if (targetId == null || !targetId.equals(hitTarget.getUniqueId())) return false;

        // Clear window
        activeWindows.remove(parasoId);
        FreezeUtils.unfreeze(targetId);

        // Reset Just Showed Up cooldown
        plugin.getCooldownManager().clearAbility(parasoId, "the_unannounced", "just_showed_up");

        // Saturation 255 — immortality for duration
        paraso.addPotionEffect(new PotionEffect(
            PotionEffectType.SATURATION,
            cfg.getUnannouncedSaturationDuration(),
            cfg.getUnannouncedSaturationLevel() - 1,
            false, false, false
        ));

        paraso.playSound(paraso.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        return true;
    }

    public boolean hasActiveWindow(UUID parasoId) {
        return activeWindows.containsKey(parasoId);
    }

    private Location getBehindLocation(Player target) {
        Location loc = target.getLocation().clone();
        double yaw = Math.toRadians(loc.getYaw());
        loc.add(Math.sin(yaw) * 1.5, 0, -Math.cos(yaw) * 1.5);
        loc.setYaw((loc.getYaw() + 180) % 360);
        return loc;
    }
}
