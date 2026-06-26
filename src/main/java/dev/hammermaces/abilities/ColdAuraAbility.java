package dev.hammermaces.abilities;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ColdAuraAbility {

    private final HammerMacesPlugin plugin;

    public ColdAuraAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player player, MaceConfig cfg) {
        Location center = player.getLocation();
        double radius = cfg.getColdAuraRadius();
        boolean hitSomething = false;

        for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity target)) continue;
            hitSomething = true;
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, cfg.getColdAuraEffectDuration(),
                cfg.getColdAuraSlownessLevel() - 1, false, true, true));
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS, cfg.getColdAuraEffectDuration(),
                cfg.getColdAuraWeaknessLevel() - 1, false, true, true));
        }

        // Always spawn particles and sound — feedback even if no targets
        spawnParticles(center, radius);
        center.getWorld().playSound(center, Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 1.4f);

        String msg = hitSomething
            ? "&#00e5ff❄ Cold Aura unleashed!"
            : "&#00e5ff❄ Cold Aura — no targets in range.";
        player.sendMessage(dev.hammermaces.utils.GradientUtils.parseLore(msg));
    }

    private void spawnParticles(Location center, double radius) {
        int points = 36;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            Location loc = center.clone().add(
                radius * Math.cos(angle), 0.1, radius * Math.sin(angle));
            center.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.1, 0.3, 0.1, 0.02);
        }
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 30, 0.5, 0.8, 0.5, 0.05);
    }
}
