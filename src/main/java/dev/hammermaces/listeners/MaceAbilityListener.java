package dev.hammermaces.listeners;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.CooldownManager;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles active abilities for all soulbound maces.
 *
 * ── Cold Aura ───────────────────────────────────────────────
 *   Trigger: Sneak + Jump
 *   Works on: Java (jump key while sneaking) + Bedrock via Geyser (same input)
 *   Effect: Frost ring around the player, Slowness II + Weakness on nearby enemies
 *
 * ── Tidal Surge ─────────────────────────────────────────────
 *   Trigger: Sneak + Swing (arm animation while sneaking)
 *   Works on: Java + Bedrock via Geyser (PlayerAnimationEvent fires on both)
 *   Effect: Cone knockback wave + Slowness on hit enemies
 *
 * Both use per-mace cooldowns tracked by CooldownManager.
 */
public class MaceAbilityListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;
    private final CooldownManager cooldownManager;

    // Track players who are currently sneaking so we can combine with other inputs
    private final Map<UUID, Boolean> sneakingPlayers = new HashMap<>();

    private static final String ABILITY_COLD_AURA   = "cold_aura";
    private static final String ABILITY_TIDAL_SURGE = "tidal_surge";

    public MaceAbilityListener(HammerMacesPlugin plugin) {
        this.plugin = plugin;
        this.maceManager = plugin.getMaceManager();
        this.cooldownManager = plugin.getCooldownManager();
    }

    // ── Track sneaking state ───────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSneak(PlayerToggleSneakEvent event) {
        sneakingPlayers.put(event.getPlayer().getUniqueId(), event.isSneaking());
    }

    // ── Cold Aura: Sneak + Jump ────────────────────────────────────────────────
    // PlayerToggleFlightEvent does NOT fire here (survival mode). Instead we use
    // a scheduler trick: detect when the player goes airborne while sneaking.
    // The cleanest cross-platform approach: listen for the player leaving ground
    // while sneaking by checking on every tick — but that's expensive.
    //
    // Instead: PlayerAnimationEvent covers arm swing, but NOT jumping.
    // For jump we use a different approach: we listen for the player's velocity
    // change via a scheduled task that checks sneaking + velocity.
    //
    // Actually the simplest reliable cross-platform way (Java + Geyser Bedrock):
    // Track sneak state + check for upward velocity in a fast repeating task.
    // We start a check task when the player starts sneaking.

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!event.isSneaking()) {
            sneakingPlayers.put(uuid, false);
            return;
        }

        sneakingPlayers.put(uuid, true);

        // Start a short-lived task that watches for an upward velocity spike (jump)
        // while still sneaking. Cancels itself after 1.5 seconds or if sneak released.
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            int checks = 0;

            @Override
            public void run() {
                if (checks++ > 15) return; // 15 checks × 2 ticks = 1.5 sec window

                if (!sneakingPlayers.getOrDefault(uuid, false)) return;

                Player p = plugin.getServer().getPlayer(uuid);
                if (p == null) return;

                // Detect jump: player velocity Y positive and they're on the ground
                // In Minecraft, sneaking suppresses jumping clientside but not serverside —
                // the packet still fires. Geyser sends the same behavior.
                if (p.getVelocity().getY() > 0.1) {
                    // They jumped! Fire Cold Aura
                    plugin.getServer().getScheduler().runTask(plugin, () -> fireColdAura(p));
                    return; // don't reschedule
                }

                // Reschedule check
                plugin.getServer().getScheduler().runTaskLater(plugin, this, 2L);
            }
        }, 1L);
    }

    // ── Tidal Surge: Sneak + Arm Swing ────────────────────────────────────────
    // PlayerAnimationEvent fires for ARM_SWING on both Java and Bedrock via Geyser

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        if (!sneakingPlayers.getOrDefault(player.getUniqueId(), false)) return;

        fireTidalSurge(player);
    }

    // ── Cold Aura logic ────────────────────────────────────────────────────────

    private void fireColdAura(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!maceManager.isOwner(held, player.getName())) return;

        String maceId = maceManager.getMaceType(held);
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null || !cfg.isColdAuraEnabled()) return;

        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), maceId, ABILITY_COLD_AURA, cfg.getColdAuraCooldown())) {
            int remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), maceId, ABILITY_COLD_AURA, cfg.getColdAuraCooldown());
            sendCooldownMessage(player, "Cold Aura", remaining);
            return;
        }

        cooldownManager.setCooldown(player.getUniqueId(), maceId, ABILITY_COLD_AURA);

        Location center = player.getLocation();
        double radius = cfg.getColdAuraRadius();

        // Apply effects to nearby entities in radius
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

        // Ice particle ring burst around the player
        spawnColdAuraParticles(center, radius);

        // Sound: frosty blast
        player.getWorld().playSound(center, Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.6f);
        player.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 1.4f);

        String msg = plugin.getConfig().getString("messages.cold-aura-fired", "&#00e5ff❄ Cold Aura unleashed!");
        player.sendMessage(GradientUtils.parseLore(msg));
    }

    private void spawnColdAuraParticles(Location center, double radius) {
        // Ring of snowflake particles at player level
        int points = 36;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLoc = new Location(center.getWorld(), x, center.getY() + 0.1, z);
            center.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 3, 0.1, 0.3, 0.1, 0.02);
        }

        // Central burst upward
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 30, 0.5, 0.8, 0.5, 0.05);
        center.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, center.clone().add(0, 0.5, 0), 15, 0.4, 0.4, 0.4, 0.1);
    }

    // ── Tidal Surge logic ──────────────────────────────────────────────────────

    private void fireTidalSurge(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!maceManager.isOwner(held, player.getName())) return;

        String maceId = maceManager.getMaceType(held);
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null || !cfg.isTidalSurgeEnabled()) return;

        // Check cooldown
        if (cooldownManager.isOnCooldown(player.getUniqueId(), maceId, ABILITY_TIDAL_SURGE, cfg.getTidalSurgeCooldown())) {
            int remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), maceId, ABILITY_TIDAL_SURGE, cfg.getTidalSurgeCooldown());
            sendCooldownMessage(player, "Tidal Surge", remaining);
            return;
        }

        cooldownManager.setCooldown(player.getUniqueId(), maceId, ABILITY_TIDAL_SURGE);

        Location origin = player.getEyeLocation();
        Vector facing = origin.getDirection().normalize();
        double halfAngle = Math.toRadians(cfg.getTidalSurgeConeAngle() / 2.0);
        double range = cfg.getTidalSurgeRange();

        for (Entity entity : player.getWorld().getNearbyEntities(origin, range, range, range)) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity target)) continue;

            // Check if entity is inside the forward cone
            Vector toTarget = entity.getLocation().toVector().subtract(origin.toVector()).normalize();
            double angle = facing.angle(toTarget);

            if (angle > halfAngle) continue;

            // Knockback: push them away from the player in the facing direction + upward arc
            Vector knockback = toTarget.multiply(cfg.getTidalSurgeKnockback()).add(new Vector(0, 0.4, 0));
            target.setVelocity(knockback);

            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                cfg.getTidalSurgeEffectDuration(),
                cfg.getTidalSurgeSlownessLevel() - 1,
                false, true, true
            ));
        }

        // Water particle trail in the cone direction
        spawnTidalSurgeParticles(origin, facing, range);

        // Sound: rushing water + deep boom
        player.getWorld().playSound(origin, Sound.ITEM_BUCKET_EMPTY, 1.2f, 0.7f);
        player.getWorld().playSound(origin, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.5f, 0.8f);

        String msg = plugin.getConfig().getString("messages.tidal-surge-fired", "&#1a1a6e🌊 Tidal Surge launched!");
        player.sendMessage(GradientUtils.parseLore(msg));
    }

    private void spawnTidalSurgeParticles(Location origin, Vector direction, double range) {
        // Spawn water splash particles along the surge direction
        for (double d = 0.5; d <= range; d += 0.8) {
            Location particleLoc = origin.clone().add(direction.clone().multiply(d));
            origin.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 5, 0.3, 0.3, 0.3, 0.05);
            origin.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, particleLoc, 2, 0.2, 0.2, 0.2, 0.02);
        }
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private void sendCooldownMessage(Player player, String abilityName, int seconds) {
        String template = plugin.getConfig().getString(
            "messages.on-cooldown",
            "&#1a1a6e✦ &#00e5ff{ability} &#7f7f7fis on cooldown for &#ffffff{seconds}s&#7f7f7f."
        );
        String msg = template
            .replace("{ability}", abilityName)
            .replace("{seconds}", String.valueOf(seconds));
        player.sendMessage(GradientUtils.parseLore(msg));
    }
}
