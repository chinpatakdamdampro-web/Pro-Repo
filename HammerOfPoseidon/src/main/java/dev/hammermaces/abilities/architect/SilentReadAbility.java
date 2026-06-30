package dev.hammermaces.abilities.architect;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Silent Read — Passive
 * Notifies the holder when players enter near/far range.
 * Pings are routed through HudManager.showSilentReadPing()
 * so there's no action bar conflict.
 */
public class SilentReadAbility {

    private final HammerMacesPlugin plugin;
    private BukkitTask task;
    private final Map<String, Long> lastPing = new HashMap<>();

    public SilentReadAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player holder : plugin.getServer().getOnlinePlayers()) {
                    ItemStack held = holder.getInventory().getItemInMainHand();
                    if (!plugin.getMaceManager().isSoulboundMace(held)) continue;
                    if (!"the_first_draft".equals(plugin.getMaceManager().getMaceType(held))) continue;
                    if (!plugin.getMaceManager().isOwner(held, holder.getName())) continue;

                    MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_first_draft");
                    if (cfg == null) continue;

                    double farRange  = cfg.getSilentReadFarRange();
                    double nearRange = cfg.getSilentReadNearRange();
                    long pingIntervalMs = cfg.getSilentReadPingIntervalTicks() * 50L;

                    for (Player target : holder.getWorld().getPlayers()) {
                        if (target.equals(holder)) continue;
                        double dist = holder.getLocation().distance(target.getLocation());
                        if (dist > farRange) continue;

                        String key = holder.getUniqueId() + ":" + target.getUniqueId();
                        long now = System.currentTimeMillis();
                        Long last = lastPing.get(key);
                        if (last != null && now - last < pingIntervalMs) continue;
                        lastPing.put(key, now);

                        boolean isNear = dist <= nearRange;
                        Component msg  = isNear ? buildNear(target) : buildFar(holder, target);
                        Sound sound    = isNear ? Sound.BLOCK_NOTE_BLOCK_HAT : Sound.BLOCK_NOTE_BLOCK_CHIME;
                        float pitch    = isNear ? 1.8f : 1.4f;

                        // Route through HudManager — no direct sendActionBar
                        plugin.getHudManager().showSilentReadPing(holder, msg);
                        holder.playSound(holder.getLocation(), sound, 0.2f, pitch);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 10L);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) task.cancel();
    }

    private Component buildFar(Player holder, Player target) {
        String dir    = getDirection(holder, target);
        String health = getHealthLabel(target);
        return txt("[ ", "#4A5568")
            .append(txt(dir + " — ", "#6a8090"))
            .append(txt(target.getName(), "#CBD5E0"))
            .append(txt(" — ", "#6a8090"))
            .append(txt(health, healthHex(target)))
            .append(txt(" ]", "#4A5568"));
    }

    private Component buildNear(Player target) {
        return txt("[ ", "#4A5568")
            .append(txt("TOO CLOSE — ", "#8aaabb"))
            .append(txt(target.getName(), "#CBD5E0"))
            .append(txt(" ]", "#4A5568"));
    }

    private String getDirection(Player from, Player to) {
        double angle = Math.toDegrees(Math.atan2(
            to.getLocation().getZ() - from.getLocation().getZ(),
            to.getLocation().getX() - from.getLocation().getX()
        ));
        double adj = (angle + 90 + 360) % 360;
        if (adj < 22.5 || adj >= 337.5) return "N";
        if (adj < 67.5)  return "NE";
        if (adj < 112.5) return "E";
        if (adj < 157.5) return "SE";
        if (adj < 202.5) return "S";
        if (adj < 247.5) return "SW";
        if (adj < 292.5) return "W";
        return "NW";
    }

    private String getHealthLabel(Player p) {
        double pct = p.getHealth() / p.getMaxHealth();
        if (pct > 0.66) return "FULL";
        if (pct > 0.33) return "MID";
        return "LOW";
    }

    private String healthHex(Player p) {
        double pct = p.getHealth() / p.getMaxHealth();
        if (pct > 0.66) return "#55cc55";
        if (pct > 0.33) return "#ffaa00";
        return "#cc3333";
    }

    private Component txt(String text, String hex) {
        return Component.text(text)
            .color(TextColor.fromHexString(hex))
            .decoration(TextDecoration.ITALIC, false);
    }
}
