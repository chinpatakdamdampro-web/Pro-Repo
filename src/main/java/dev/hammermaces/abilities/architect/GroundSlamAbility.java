package dev.hammermaces.abilities.architect;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Ground Slam — Passive trigger on fall damage
 *
 * When The First Draft holder takes fall damage, instead of taking it normally:
 *   - Fall damage is cancelled
 *   - A shockwave fires based on fall height
 *   - Nearby entities get knocked outward + 1 Fracture stack
 *
 * Fall height tiers:
 *   3-8 blocks   → radius 3, light knockback
 *   9-15 blocks  → radius 5, medium knockback
 *   16+ blocks   → radius 7, heavy knockback
 *
 * Damage dealt to entities scales with vanilla mace fall damage formula:
 *   ~1.5 hearts equivalent for a 2-block fall (as requested).
 *   Formula: fallBlocks * 0.75 bonus damage added to nearby entities.
 */
public class GroundSlamAbility {

    private final HammerMacesPlugin plugin;

    public GroundSlamAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Called from ArchitectListener on EntityDamageEvent with FALL cause.
     * Cancels fall damage and fires shockwave.
     */
    public void onLand(Player holder, EntityDamageEvent event, MaceConfig cfg) {
        if (!cfg.isGroundSlamEnabled()) return;

        double fallDist = event.getDamage() > 0
            ? (event.getDamage() / 0.5) + cfg.getGroundSlamMinFallDistance()
            : 0;

        if (fallDist < cfg.getGroundSlamMinFallDistance()) return;

        // Cancel the fall damage — mace absorbs it
        event.setCancelled(true);

        // Determine tier
        int radius;
        double knockback;
        if (fallDist >= 16) {
            radius    = 7;
            knockback = 2.2;
        } else if (fallDist >= 9) {
            radius    = 5;
            knockback = 1.5;
        } else {
            radius    = 3;
            knockback = 0.9;
        }

        // Bonus damage to nearby entities — 2 block fall = ~1.5 hearts = 3.0 damage
        double bonusDamage = Math.min(fallDist * 0.75, 20.0);

        Location center = holder.getLocation();

        // Spawn shockwave particles
        plugin.getParticleEffects().groundSlamShockwave(center, radius);

        // Sound — heavy impact
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.5f);
        center.getWorld().playSound(center, Sound.BLOCK_STONE_BREAK,      1.0f, 0.7f);

        // Apply to nearby entities
        double radiusSq = radius * radius;
        for (Entity entity : holder.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity.equals(holder)) continue;
            if (!(entity instanceof LivingEntity target)) continue;

            if (entity.getLocation().distanceSquared(center) > radiusSq) continue;

            // Knockback outward
            Vector away = entity.getLocation().toVector()
                .subtract(center.toVector()).normalize()
                .multiply(knockback).add(new Vector(0, 0.4, 0));
            entity.setVelocity(away);

            // Bonus damage
            target.damage(bonusDamage, holder);

            // Add 1 Fracture stack
            plugin.getFractureManager().addInitialStacks(entity.getUniqueId(), 1);
        }
    }
}
