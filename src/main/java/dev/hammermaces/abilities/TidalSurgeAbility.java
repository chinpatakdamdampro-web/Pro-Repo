package dev.hammermaces.abilities;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class TidalSurgeAbility {

    private final HammerMacesPlugin plugin;

    public TidalSurgeAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player player, MaceConfig cfg) {
        Location origin = player.getEyeLocation();
        Vector facing   = origin.getDirection().normalize();
        double halfAngle = Math.toRadians(cfg.getTidalSurgeConeAngle() / 2.0);
        double range     = cfg.getTidalSurgeRange();
        boolean hitSomething = false;

        for (Entity entity : player.getWorld().getNearbyEntities(origin, range, range, range)) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity target)) continue;

            Vector toTarget = entity.getLocation().toVector()
                .subtract(origin.toVector()).normalize();
            if (facing.angle(toTarget) > halfAngle) continue;

            hitSomething = true;
            target.setVelocity(toTarget.multiply(cfg.getTidalSurgeKnockback()).add(new Vector(0, 0.4, 0)));
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, cfg.getTidalSurgeEffectDuration(),
                cfg.getTidalSurgeSlownessLevel() - 1, false, true, true));
        }

        spawnParticles(origin, facing, range);
        origin.getWorld().playSound(origin, Sound.ITEM_BUCKET_EMPTY, 1.2f, 0.7f);
        origin.getWorld().playSound(origin, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.5f, 0.8f);

        String msg = hitSomething
            ? "&#1a1a6e🌊 Tidal Surge launched!"
            : "&#1a1a6e🌊 Tidal Surge — no targets in range.";
        player.sendMessage(GradientUtils.parseLore(msg));
    }

    private void spawnParticles(Location origin, Vector direction, double range) {
        for (double d = 0.5; d <= range; d += 0.8) {
            Location loc = origin.clone().add(direction.clone().multiply(d));
            origin.getWorld().spawnParticle(Particle.SPLASH, loc, 5, 0.3, 0.3, 0.3, 0.05);
            origin.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, loc, 2, 0.2, 0.2, 0.2, 0.02);
        }
    }
}
