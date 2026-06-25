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

/**
 * Cold Aura ability — Sneak + Jump
 * Frost ring around the player, applies Slowness + Weakness to nearby enemies.
 */
public class ColdAuraAbility {

    public static final String ID = "cold_aura";

    private final HammerMacesPlugin plugin;

    public ColdAuraAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player player, MaceConfig cfg) {
        Location center = player.getLocation();
        double radius = cfg.getColdAuraRadius();

        for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity target)) continue;

            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                cfg.getColdAuraEffectDuration(),
                cfg.getColdAuraSlownessLevel() - 1,
                false, true, true
            ));
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS,
                cfg.getColdAuraEffectDuration(),
                cfg.getColdAuraWeaknessLevel() - 1,
                false, true, true
            ));
        }

        spawnParticles(center, radius);

        player.getWorld().playSound(center, Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.6f);
        player.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 1.4f);

        String msg = plugin.getConfig().getString("messages.cold-aura-fired", "&#00e5ff❄ Cold Aura unleashed!");
        player.sendMessage(GradientUtils.parseLore(msg));
    }

    private void spawnParticles(Location center, double radius) {
        int points = 36;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(center.getWorld(), x, center.getY() + 0.1, z);
            center.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.1, 0.3, 0.1, 0.02);
        }
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 30, 0.5, 0.8, 0.5, 0.05);
        center.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, center.clone().add(0, 0.5, 0), 15, 0.4, 0.4, 0.4, 0.1);
    }
}
