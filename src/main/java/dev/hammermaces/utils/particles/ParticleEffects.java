package dev.hammermaces.utils.particles;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.particle.ParticleList_1_13;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Centralised particle effect library using ParticleNativeAPI.
 * All effects are non-blocking and run on the server tick thread.
 */
public class ParticleEffects {

    private final ParticleNativeAPI api;
    private final ParticleList_1_13 particles;
    private final Plugin plugin;

    public ParticleEffects(Plugin plugin, ParticleNativeAPI api) {
        this.plugin    = plugin;
        this.api       = api;
        this.particles = api.LIST_1_13;
    }

    // ── Shockwave ground slam ─────────────────────────────────────────────────

    /**
     * Expanding block debris shockwave from a ground slam.
     * Center blocks launch upward with velocity then ripple outward.
     *
     * @param location  Impact location
     * @param maxRadius How far the ring expands (based on fall height)
     */
    public void groundSlamShockwave(Location location, int maxRadius) {
        World world = location.getWorld();
        if (world == null) return;

        // Center burst — blocks fly upward immediately
        spawnDebrisBurst(location, 0, 1);

        // Expanding ring every 2 ticks
        new BukkitRunnable() {
            int currentRadius = 1;

            @Override
            public void run() {
                if (currentRadius > maxRadius) {
                    cancel();
                    return;
                }
                spawnRingLayer(location, currentRadius);
                currentRadius++;
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private void spawnDebrisBurst(Location center, int radius, int layers) {
        World world = center.getWorld();
        if (world == null) return;

        org.bukkit.block.Block block = center.getBlock();
        org.bukkit.Material mat = block.getType().isSolid() ? block.getType() : org.bukkit.Material.DIRT;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius + 1) continue;
                Location loc = center.clone().add(x, 0.1, z);

                // Launch upward with slight random spread
                double vx = (Math.random() - 0.5) * 0.3;
                double vy = 0.3 + Math.random() * 0.4;
                double vz = (Math.random() - 0.5) * 0.3;

                Object packet = particles.BLOCK.packetMotion(true, loc, vx, vy, vz,
                    new org.bukkit.block.data.BlockData[]{mat.createBlockData()});
                sendToNearby(world, center, 30, packet);
            }
        }
    }

    private void spawnRingLayer(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;

        org.bukkit.Material mat = center.getBlock().getType().isSolid()
            ? center.getBlock().getType() : org.bukkit.Material.DIRT;

        int points = Math.max(12, radius * 8);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, center.getY() + 0.05, z);

            double vx = Math.cos(angle) * 0.1;
            double vy = 0.15 + Math.random() * 0.2;
            double vz = Math.sin(angle) * 0.1;

            Object packet = particles.BLOCK.packetMotion(true, loc, vx, vy, vz,
                new org.bukkit.block.data.BlockData[]{mat.createBlockData()});
            sendToNearby(world, center, 30, packet);
        }

        // Smoke ring at same radius
        for (int i = 0; i < points / 2; i++) {
            double angle = (2 * Math.PI / (points / 2)) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, center.getY() + 0.1, z);

            Object smoke = particles.WHITE_SMOKE.packet(true, loc);
            sendToNearby(world, center, 30, smoke);
        }
    }

    // ── Cold Aura frost ring ───────────────────────────────────────────────────

    public void coldAuraRing(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;

        int points = 48;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, center.getY() + 0.1, z);

            Object pkt = particles.SNOWFLAKE.packet(true, loc);
            sendToNearby(world, center, 32, pkt);
        }

        Object burst = particles.SNOWFLAKE.packetMotion(true, center, 0.5, 0.6, 0.5, 0.04);
        sendToNearby(world, center, 32, burst);
    }

    // ── Tidal Surge wave ──────────────────────────────────────────────────────

    public void tidalSurgeTrail(Location origin, org.bukkit.util.Vector direction, double range) {
        World world = origin.getWorld();
        if (world == null) return;

        for (double d = 0.5; d <= range; d += 0.7) {
            Location loc = origin.clone().add(direction.clone().multiply(d));
            Object splash = particles.SPLASH.packetMotion(true, loc, 0.3, 0.3, 0.3, 0.04);
            Object bubble = particles.BUBBLE_COLUMN_UP.packet(true, loc);
            sendToNearby(world, origin, 32, splash);
            sendToNearby(world, origin, 32, bubble);
        }
    }

    // ── Dash trail ────────────────────────────────────────────────────────────

    public void dashTrail(Location from, Location to) {
        World world = from.getWorld();
        if (world == null) return;

        org.bukkit.util.Vector dir = to.toVector().subtract(from.toVector());
        double length = dir.length();
        if (length == 0) return;
        dir.normalize();

        for (double d = 0; d <= length; d += 0.4) {
            Location loc = from.clone().add(dir.clone().multiply(d));
            Object smoke = particles.CAMPFIRE_COSY_SMOKE.packetMotion(true, loc,
                0.05, 0.05, 0.05, 0.01);
            sendToNearby(world, from, 24, smoke);
        }
    }

    // ── Unannounced teleport ──────────────────────────────────────────────────

    public void teleportVanish(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Smoke spiral inward (simulated by outward burst of soul particles)
        Object soul = particles.SOUL.packetMotion(true, location, 0.4, 0.5, 0.4, 0.05);
        Object smoke = particles.LARGE_SMOKE.packet(true, location);
        sendToNearby(world, location, 24, soul);
        sendToNearby(world, location, 24, smoke);
    }

    public void teleportReappear(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Object poof = particles.POOF.packetMotion(true, location, 0.3, 0.4, 0.3, 0.05);
        sendToNearby(world, location, 24, poof);
    }

    // ── Contingency armed ring ────────────────────────────────────────────────

    public void contingencyArmed(Location center, Plugin plugin) {
        World world = center.getWorld();
        if (world == null) return;

        // Single slow-expanding ring
        new BukkitRunnable() {
            double radius = 0.2;

            @Override
            public void run() {
                if (radius > 2.5) { cancel(); return; }
                int points = 16;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI / points) * i;
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location loc = new Location(world, x, center.getY() + 0.05, z);
                    Object pkt = particles.ENCHANT.packet(true, loc);
                    sendToNearby(world, center, 16, pkt);
                }
                radius += 0.3;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── Contingency fires ─────────────────────────────────────────────────────

    public void contingencyFires(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        // Geometric outward burst
        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double vx = Math.cos(angle) * 0.4;
            double vz = Math.sin(angle) * 0.4;
            Object enchant = particles.ENCHANT.packetMotion(true, center, vx, 0.1, vz, 0.05);
            Object sculk   = particles.SCULK_SOUL.packet(true, center);
            sendToNearby(world, center, 24, enchant);
            sendToNearby(world, center, 24, sculk);
        }
    }

    // ── Predetermined zone ────────────────────────────────────────────────────

    public void predeterminedActivate(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;

        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, center.getY() + 0.1, z);
            Object pkt = particles.SCULK_CHARGE_POP.packet(true, loc);
            sendToNearby(world, center, 24, pkt);
        }
    }

    public void predeterminedTrigger(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        Object sonic = particles.SONIC_BOOM.packet(true, center);
        sendToNearby(world, center, 24, sonic);
    }

    // ── Fracture burst ────────────────────────────────────────────────────────

    public void fractureBurst(Location target) {
        World world = target.getWorld();
        if (world == null) return;
        Object crit = particles.CRIT.packetMotion(true, target, 0.3, 0.3, 0.3, 0.08);
        Object dmg  = particles.DAMAGE_INDICATOR.packet(true, target);
        sendToNearby(world, target, 20, crit);
        sendToNearby(world, target, 20, dmg);
    }

    // ── Just Showed Up burst ──────────────────────────────────────────────────

    public void justShowedUpBurst(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;

        int points = 40;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(world, x, center.getY() + 0.1, z);
            Object poof = particles.POOF.packetMotion(true, loc,
                Math.cos(angle) * 0.2, 0.3, Math.sin(angle) * 0.2, 0.05);
            sendToNearby(world, center, 24, poof);
        }

        Object totem = particles.TOTEM_OF_UNDYING.packetMotion(true, center, 0.4, 0.8, 0.4, 0.1);
        sendToNearby(world, center, 24, totem);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void sendToNearby(World world, Location center, double range, Object packet) {
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= range * range) {
                api.getPlayerManager().sendPacket(player, packet);
            }
        }
    }
}
