package dev.hammermaces.abilities.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UnannouncedAbility {

    private final HammerMacesPlugin plugin;
    private final Random random = new Random();

    public UnannouncedAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player player, MaceConfig cfg) {
        Location from = player.getLocation().clone();

        // Vanish effect at origin
        from.getWorld().spawnParticle(Particle.LARGE_SMOKE, from, 12, 0.3, 0.5, 0.3, 0.02);
        from.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.6f);

        // Find a valid landing spot within radius
        double radius = cfg.getUnannouncedRadius();
        Location dest = findLandingSpot(from, radius);

        player.teleport(dest);

        // Reappear effect at destination — anticlimactic on purpose
        dest.getWorld().spawnParticle(Particle.POOF, dest, 8, 0.3, 0.3, 0.3, 0.04);
        dest.getWorld().playSound(dest, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.6f, 1.0f);
    }

    private Location findLandingSpot(Location origin, double radius) {
        for (int attempts = 0; attempts < 20; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist  = 4 + random.nextDouble() * (radius - 4);
            double x = origin.getX() + dist * Math.cos(angle);
            double z = origin.getZ() + dist * Math.sin(angle);

            // Find highest solid block at that X/Z
            Location candidate = new Location(origin.getWorld(), x, origin.getY(), z);
            Location ground = origin.getWorld().getHighestBlockAt(candidate.getBlockX(), candidate.getBlockZ())
                .getLocation().add(0, 1, 0);

            // Safety check — don't teleport into the void or too high
            if (ground.getY() < -60 || ground.getY() > 320) continue;
            if (!ground.getBlock().getType().isAir()) continue;

            ground.setYaw(origin.getYaw());
            ground.setPitch(origin.getPitch());
            return ground;
        }

        // Fallback: stay in place if no valid spot
        return origin;
    }
}
