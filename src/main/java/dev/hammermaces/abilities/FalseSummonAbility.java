package dev.hammermaces.abilities;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * False Summon — Sneak + Swing
 *
 * Spawns a decoy ArmorStand wearing the wielder's exact armor/equipment at a
 * nearby random spot, posed and visible for decoy-duration ticks. The real
 * wielder gets brief Invisibility + Speed to reposition while the decoy draws
 * attention. Anyone who attacks the decoy gets Weakness applied — punished
 * for falling for the bit.
 */
public class FalseSummonAbility implements Listener {

    private final HammerMacesPlugin plugin;

    /** Tracks active decoy entity UUIDs so we know which armor stands to punish hits on. */
    private final Set<UUID> activeDecoys = new HashSet<>();

    public FalseSummonAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void fire(Player wielder, MaceConfig cfg) {
        Location spawnLoc = findDecoySpot(wielder, cfg.getFalseSummonRadius());

        ArmorStand decoy = wielder.getWorld().spawn(spawnLoc, ArmorStand.class, stand -> {
            stand.setBasePlate(true);
            stand.setArms(true);
            stand.setGravity(false);
            stand.setInvulnerable(false);
            stand.setCustomName(wielder.getName());
            stand.setCustomNameVisible(true);

            EntityEquipment eq = stand.getEquipment();
            if (eq != null) {
                eq.setHelmet(wielder.getInventory().getHelmet());
                eq.setChestplate(wielder.getInventory().getChestplate());
                eq.setLeggings(wielder.getInventory().getLeggings());
                eq.setBoots(wielder.getInventory().getBoots());
                eq.setItemInMainHand(wielder.getInventory().getItemInMainHand());
            }

            // Taunting pose
            stand.setRightArmPose(new org.bukkit.util.EulerAngle(Math.toRadians(-30), 0, Math.toRadians(10)));
            stand.setLeftArmPose(new org.bukkit.util.EulerAngle(Math.toRadians(-30), 0, Math.toRadians(-10)));
            stand.setHeadPose(new org.bukkit.util.EulerAngle(0, Math.toRadians(15), 0));
        });

        activeDecoys.add(decoy.getUniqueId());

        // Despawn after duration
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            activeDecoys.remove(decoy.getUniqueId());
            if (!decoy.isDead()) decoy.remove();
        }, cfg.getFalseSummonDecoyDuration());

        // Wielder buffs — invis + speed to reposition
        wielder.addPotionEffect(new PotionEffect(
            PotionEffectType.INVISIBILITY, cfg.getFalseSummonInvisDuration(), 0, false, false, false));
        wielder.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED, cfg.getFalseSummonSpeedDuration(), 1, false, true, true));

        // Particles + sound on spawn
        spawnLoc.getWorld().spawnParticle(Particle.POOF, spawnLoc, 12, 0.3, 0.5, 0.3, 0.05);
        wielder.getWorld().playSound(wielder.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.2f, 1.0f);
        wielder.getWorld().playSound(spawnLoc, Sound.ENTITY_VILLAGER_CELEBRATE, 0.6f, 0.7f);

        wielder.sendMessage(GradientUtils.parseLore("&#FF6666🎭 False Summon — let them find the decoy."));
    }

    /**
     * Punishes attackers who hit an active decoy with Weakness.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDecoyHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        if (!activeDecoys.contains(stand.getUniqueId())) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Look up the duration from any active Larper mace config — use default if unavailable
        int weaknessDuration = 60;
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("hammer_of_the_larpers");
        if (cfg != null) weaknessDuration = cfg.getFalseSummonWeaknessDuration();

        attacker.addPotionEffect(new PotionEffect(
            PotionEffectType.WEAKNESS, weaknessDuration, 1, false, true, true));
        attacker.sendMessage(GradientUtils.parseLore("&#FF6666«You fell for it.»"));

        // Decoy "pops" once hit
        event.setCancelled(true);
        stand.getWorld().spawnParticle(Particle.POOF, stand.getLocation(), 10, 0.3, 0.5, 0.3, 0.05);
        activeDecoys.remove(stand.getUniqueId());
        stand.remove();
    }

    private Location findDecoySpot(Player wielder, double radius) {
        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = Math.random() * 2 * Math.PI;
            double dist  = 2 + Math.random() * (radius - 2);
            double x = wielder.getLocation().getX() + dist * Math.cos(angle);
            double z = wielder.getLocation().getZ() + dist * Math.sin(angle);

            Location ground = wielder.getWorld()
                .getHighestBlockAt((int) x, (int) z)
                .getLocation().add(0.5, 1, 0.5);

            if (ground.getBlock().getType().isAir()) {
                ground.setYaw(wielder.getLocation().getYaw());
                return ground;
            }
        }
        return wielder.getLocation().clone();
    }
}
