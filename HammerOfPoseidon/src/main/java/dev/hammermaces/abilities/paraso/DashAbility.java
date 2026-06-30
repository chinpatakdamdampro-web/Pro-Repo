package dev.hammermaces.abilities.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DashAbility {

    private final HammerMacesPlugin plugin;

    public DashAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player player, MaceConfig cfg) {
        Location from = player.getLocation();
        Vector direction = player.getLocation().getDirection().normalize();
        direction.setY(Math.max(direction.getY(), 0.1)); // always slight upward arc

        player.setVelocity(direction.multiply(cfg.getDashVelocity()));

        // Trail particles from launch point
        for (double d = 0.3; d <= 2.0; d += 0.3) {
            Location trailLoc = from.clone().add(direction.clone().multiply(d));
            from.getWorld().spawnParticle(Particle.LARGE_SMOKE, trailLoc, 2, 0.05, 0.05, 0.05, 0.01);
        }

        player.getWorld().playSound(from, Sound.ENTITY_BREEZE_WIND_BURST, 0.8f, 1.2f);
    }
}
