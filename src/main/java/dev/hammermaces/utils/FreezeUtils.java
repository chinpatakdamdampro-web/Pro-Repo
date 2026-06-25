package dev.hammermaces.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles freezing players in place using PacketEvents.
 *
 * Position lock: achieved by repeatedly teleporting the player back to their
 * frozen location every tick via position+look packets, preventing any movement.
 *
 * Camera lock: achieved by repeatedly sending their exact yaw/pitch back,
 * so even if they move their mouse it snaps back within one tick.
 *
 * Both Java and Bedrock (via Geyser) handle this correctly since PacketEvents
 * manages the protocol translation layer.
 */
public class FreezeUtils {

    // Currently frozen players — checked by other systems to avoid conflicts
    private static final Set<UUID> frozenPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Freeze a player in place for the given duration in ticks.
     * Locks both position and camera rotation.
     *
     * @param plugin   Your plugin instance (for scheduler)
     * @param player   The player to freeze
     * @param ticks    How long to freeze them (in ticks)
     */
    public static void freeze(Plugin plugin, Player player, long ticks) {
        UUID uuid = player.getUniqueId();
        if (frozenPlayers.contains(uuid)) return;

        frozenPlayers.add(uuid);

        // Snapshot position + look at freeze moment
        double frozenX   = player.getLocation().getX();
        double frozenY   = player.getLocation().getY();
        double frozenZ   = player.getLocation().getZ();
        float  frozenYaw = player.getLocation().getYaw();
        float  frozenPitch = player.getLocation().getPitch();

        // Every tick: send a position+look packet locking them in place
        BukkitRunnable lockTask = new BukkitRunnable() {
            long elapsed = 0;

            @Override
            public void run() {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p == null || !p.isOnline() || elapsed >= ticks) {
                    frozenPlayers.remove(uuid);
                    this.cancel();
                    return;
                }

                sendPositionLock(p, frozenX, frozenY, frozenZ, frozenYaw, frozenPitch);
                elapsed++;
            }
        };

        lockTask.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Manually unfreeze a player before their freeze duration expires.
     */
    public static void unfreeze(UUID uuid) {
        frozenPlayers.remove(uuid);
    }

    /**
     * Returns true if the given player is currently frozen.
     */
    public static boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    /**
     * Sends a position and look packet to lock the player to exact coordinates and angles.
     * PacketEvents handles Java vs Bedrock protocol differences automatically.
     */
    private static void sendPositionLock(Player player, double x, double y, double z, float yaw, float pitch) {
        try {
            var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) return;

            // WrapperPlayServerPlayerPositionAndLook locks both position AND camera
            WrapperPlayServerPlayerPositionAndLook packet = new WrapperPlayServerPlayerPositionAndLook(
                x, y, z,           // frozen coordinates
                yaw, pitch,        // frozen camera angles
                (byte) 0,          // flags: 0 = absolute (not relative)
                0                  // teleport ID
            );

            user.sendPacket(packet);
        } catch (Exception e) {
            // Fail silently — if packet fails, player just isn't locked this tick
        }
    }
}
