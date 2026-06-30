package dev.hammermaces.utils.particles;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.packet.ParticlePacket;
import com.github.fierioziy.particlenativeapi.api.particle.ParticleList_1_13;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * All particle effects verified against the actual ParticleNativeAPI jar.
 *
 * Type map (from ParticleSupplier_1_13):
 *   ParticleTypeMotion  (supports packetMotion): POOF
 *   ParticleTypeBlockMotion (supports .of(Material).packetMotion): BLOCK
 *   ParticleType        (packet() only): everything else we use —
 *     SPLASH, SNOWFLAKE, SOUL, LARGE_SMOKE, CAMPFIRE_COSY_SMOKE,
 *     ENCHANT, CRIT, DAMAGE_INDICATOR, SCULK_SOUL, SCULK_CHARGE_POP,
 *     SONIC_BOOM, WHITE_SMOKE, TOTEM_OF_UNDYING, BUBBLE_COLUMN_UP
 *
 * ParticlePacket.sendTo(Player) is the correct send method.
 */
public class ParticleEffects {

    private final ParticleNativeAPI api;
    private final ParticleList_1_13 p;
    private final Plugin plugin;

    public ParticleEffects(Plugin plugin, ParticleNativeAPI api) {
        this.plugin = plugin;
        this.api    = api;
        this.p      = api.LIST_1_13;
    }

    // ── Ground slam shockwave ─────────────────────────────────────────────────

    public void groundSlamShockwave(Location center, int maxRadius) {
        World world = center.getWorld();
        if (world == null) return;

        Material mat = center.getBlock().getType().isSolid()
            ? center.getBlock().getType() : Material.DIRT;

        Collection<Player> nearby = world.getPlayers();

        // Center burst — blocks fly upward using BLOCK (ParticleTypeBlockMotion)
        spawnDebrisBurst(center, mat, nearby);

        new BukkitRunnable() {
            int r = 1;
            @Override
            public void run() {
                if (r > maxRadius) { cancel(); return; }
                spawnRingLayer(center, r, mat, world.getPlayers());
                r++;
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private void spawnDebrisBurst(Location center, Material mat, Collection<Player> nearby) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location loc = center.clone().add(x, 0.1, z);
                Vector motion = new Vector(
                    x * 0.15 + (Math.random() - 0.5) * 0.2,
                    0.35 + Math.random() * 0.3,
                    z * 0.15 + (Math.random() - 0.5) * 0.2
                );
                // BLOCK is ParticleTypeBlockMotion — supports .of(Material).packetMotion()
                ParticlePacket pkt = p.BLOCK.of(mat).packetMotion(true, loc, motion);
                for (Player pl : nearby) pkt.sendTo(pl);
            }
        }
    }

    private void spawnRingLayer(Location center, int radius, Material mat, Collection<Player> nearby) {
        int points = Math.max(12, radius * 8);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location loc = new Location(center.getWorld(), x, center.getY() + 0.05, z);

            Vector motion = new Vector(Math.cos(angle) * 0.1, 0.18 + Math.random() * 0.15, Math.sin(angle) * 0.1);
            ParticlePacket blockPkt = p.BLOCK.of(mat).packetMotion(true, loc, motion);
            // WHITE_SMOKE is ParticleType — packet() only
            ParticlePacket smokePkt = p.WHITE_SMOKE.packet(true, loc);

            for (Player pl : nearby) {
                blockPkt.sendTo(pl);
                smokePkt.sendTo(pl);
            }
        }
    }

    // ── Cold Aura frost ring ──────────────────────────────────────────────────

    public void coldAuraRing(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();

        int points = 48;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            Location loc = center.clone().add(radius * Math.cos(angle), 0.1, radius * Math.sin(angle));
            // SNOWFLAKE is ParticleType — packet() only
            ParticlePacket pkt = p.SNOWFLAKE.packet(true, loc);
            for (Player pl : nearby) pkt.sendTo(pl);
        }
        // Center burst — also packet() only for SNOWFLAKE
        ParticlePacket burst = p.SNOWFLAKE.packet(true, center, 20);
        for (Player pl : nearby) burst.sendTo(pl);
    }

    // ── Tidal Surge wave ──────────────────────────────────────────────────────

    public void tidalSurgeTrail(Location origin, Vector direction, double range) {
        World world = origin.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();

        for (double d = 0.5; d <= range; d += 0.7) {
            Location loc = origin.clone().add(direction.clone().multiply(d));
            // SPLASH and BUBBLE_COLUMN_UP are ParticleType — packet() only
            ParticlePacket splash = p.SPLASH.packet(true, loc, 5);
            ParticlePacket bubble = p.BUBBLE_COLUMN_UP.packet(true, loc);
            for (Player pl : nearby) {
                splash.sendTo(pl);
                bubble.sendTo(pl);
            }
        }
    }

    // ── Dash trail ────────────────────────────────────────────────────────────

    public void dashTrail(Location from, Vector direction, double length) {
        World world = from.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();

        for (double d = 0; d <= length; d += 0.4) {
            Location loc = from.clone().add(direction.clone().multiply(d));
            // LARGE_SMOKE is ParticleType — packet() only
            ParticlePacket pkt = p.LARGE_SMOKE.packet(true, loc);
            for (Player pl : nearby) pkt.sendTo(pl);
        }
    }

    // ── Unannounced teleport ──────────────────────────────────────────────────

    public void teleportVanish(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();
        // SOUL and LARGE_SMOKE are ParticleType — packet() only
        ParticlePacket soul  = p.SOUL.packet(true, location, 10);
        ParticlePacket smoke = p.LARGE_SMOKE.packet(true, location, 5);
        for (Player pl : nearby) { soul.sendTo(pl); smoke.sendTo(pl); }
    }

    public void teleportReappear(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();
        // POOF is ParticleTypeMotion — supports packetMotion()
        ParticlePacket poof = p.POOF.packetMotion(true, location, new Vector(0.3, 0.4, 0.3));
        for (Player pl : nearby) poof.sendTo(pl);
    }

    // ── Contingency armed ring ────────────────────────────────────────────────

    public void contingencyArmed(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        new BukkitRunnable() {
            double radius = 0.2;
            @Override
            public void run() {
                if (radius > 2.5) { cancel(); return; }
                Collection<Player> nearby = world.getPlayers();
                int points = 16;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI / points) * i;
                    Location loc = center.clone().add(radius * Math.cos(angle), 0.05, radius * Math.sin(angle));
                    // ENCHANT is ParticleType — packet() only
                    ParticlePacket pkt = p.ENCHANT.packet(true, loc);
                    for (Player pl : nearby) pkt.sendTo(pl);
                }
                radius += 0.3;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ── Contingency fires ─────────────────────────────────────────────────────

    public void contingencyFires(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();

        // Use POOF (ParticleTypeMotion) for the outward burst direction
        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            Vector motion = new Vector(Math.cos(angle) * 0.4, 0.1, Math.sin(angle) * 0.4);
            ParticlePacket poof  = p.POOF.packetMotion(true, center, motion);
            // SCULK_SOUL is ParticleType — packet() only
            ParticlePacket sculk = p.SCULK_SOUL.packet(true, center);
            for (Player pl : nearby) { poof.sendTo(pl); sculk.sendTo(pl); }
        }
    }

    // ── Predetermined zone ────────────────────────────────────────────────────

    public void predeterminedActivate(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();

        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            Location loc = center.clone().add(radius * Math.cos(angle), 0.1, radius * Math.sin(angle));
            // SCULK_CHARGE_POP is ParticleType — packet() only
            ParticlePacket pkt = p.SCULK_CHARGE_POP.packet(true, loc);
            for (Player pl : nearby) pkt.sendTo(pl);
        }
    }

    public void predeterminedTrigger(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();
        // SONIC_BOOM is ParticleType — packet() only
        ParticlePacket sonic = p.SONIC_BOOM.packet(true, center);
        for (Player pl : nearby) sonic.sendTo(pl);
    }

    // ── Fracture burst ────────────────────────────────────────────────────────

    public void fractureBurst(Location target) {
        World world = target.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();
        // CRIT and DAMAGE_INDICATOR are ParticleType — packet() only
        ParticlePacket crit = p.CRIT.packet(true, target, 8);
        ParticlePacket dmg  = p.DAMAGE_INDICATOR.packet(true, target, 3);
        for (Player pl : nearby) { crit.sendTo(pl); dmg.sendTo(pl); }
    }

    // ── Just Showed Up burst ──────────────────────────────────────────────────

    public void justShowedUpBurst(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        Collection<Player> nearby = world.getPlayers();

        // POOF is ParticleTypeMotion — use packetMotion for outward burst
        int points = 40;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            Location loc = center.clone().add(radius * Math.cos(angle), 0.1, radius * Math.sin(angle));
            Vector motion = new Vector(Math.cos(angle) * 0.2, 0.3, Math.sin(angle) * 0.2);
            ParticlePacket poof = p.POOF.packetMotion(true, loc, motion);
            for (Player pl : nearby) poof.sendTo(pl);
        }

        // TOTEM_OF_UNDYING is ParticleType — packet() only
        ParticlePacket totem = p.TOTEM_OF_UNDYING.packet(true, center, 40);
        for (Player pl : nearby) totem.sendTo(pl);
    }
    public ParticleList_1_13 getParticleList() { return p; }
}

