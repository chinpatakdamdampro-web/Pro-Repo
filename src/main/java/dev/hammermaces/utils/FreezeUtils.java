package dev.hammermaces.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freezes players in place using PacketEvents position+look packets.
 * Sends the player's own frozen coordinates back to them every tick,
 * preventing movement and locking camera rotation.
 * Works on Java and Bedrock via Geyser.
 */
public class FreezeUtils {

    private static final Set<UUID> frozenPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final RelativeFlag ABSOLUTE    = new RelativeFlag(0);

    public static void freeze(Plugin plugin, Player player, long ticks) {
        UUID uuid = player.getUniqueId();
        if (frozenPlayers.contains(uuid)) return;

        frozenPlayers.add(uuid);

        final double x     = player.getLocation().getX();
        final double y     = player.getLocation().getY();
        final double z     = player.getLocation().getZ();
        final float  yaw   = player.getLocation().getYaw();
        final float  pitch = player.getLocation().getPitch();

        new BukkitRunnable() {
            long elapsed = 0;

            @Override
            public void run() {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p == null || !p.isOnline() || !frozenPlayers.contains(uuid) || elapsed >= ticks) {
                    frozenPlayers.remove(uuid);
                    cancel();
                    return;
                }
                sendLock(p, x, y, z, yaw, pitch);
                elapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static void unfreeze(UUID uuid) {
        frozenPlayers.remove(uuid);
    }

    public static boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    private static void sendLock(Player player, double x, double y, double z, float yaw, float pitch) {
        try {
            var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) return;
            user.sendPacket(new WrapperPlayServerPlayerPositionAndLook(
                0,
                new Vector3d(x, y, z),
                new Vector3d(0, 0, 0),
                yaw,
                pitch,
                ABSOLUTE
            ));
        } catch (Exception ignored) {}
    }
}
