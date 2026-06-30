package dev.hammermaces.abilities.paraso;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.packet.ParticlePacket;
import com.github.fierioziy.particlenativeapi.api.particle.ParticleList_1_13;
import dev.hammermaces.HammerMacesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.List;

/**
 * Fusion Animation — plays when Paraso completes quest 2.
 *
 * Phase 1 (ticks 0-20):  Gathering — flame + bubble particles rise toward holder
 * Phase 2 (ticks 20-40): Surge     — expanding ring burst outward
 * Phase 3 (ticks 40-60): Seal      — spiral inward + totem burst
 * Tick 60:               Complete  — title shown, quest advances
 *
 * Uses ParticleNativeAPI for all particle effects.
 * Only POOF supports packetMotion — all others use packet() only.
 */
public class FusionAnimationTask extends BukkitRunnable {

    private final HammerMacesPlugin plugin;
    private final Player player;
    private final ParticleList_1_13 p;
    private final Runnable onComplete;

    private int tick = 0;

    public FusionAnimationTask(HammerMacesPlugin plugin, Player player, Runnable onComplete) {
        this.plugin     = plugin;
        this.player     = player;
        this.p          = plugin.getParticleEffects().getParticleList();
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        if (!player.isOnline()) { cancel(); return; }

        Location center = player.getLocation().clone().add(0, 1, 0);
        List<Player> nearby = player.getWorld().getPlayers();

        // ── Phase 1 (0-20): Gathering ────────────────────────────────────────
        if (tick < 20) {
            // Flame particles rise upward around the player
            int points = 8;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i + (tick * 0.3);
                double radius = 1.5 - (tick * 0.06); // spiral inward
                Location loc = center.clone().add(
                    radius * Math.cos(angle),
                    tick * 0.05,
                    radius * Math.sin(angle)
                );
                ParticlePacket flame = p.FLAME.packet(true, loc);
                ParticlePacket soul  = p.SOUL.packet(true, loc);
                for (Player pl : nearby) { flame.sendTo(pl); soul.sendTo(pl); }
            }

            if (tick == 0) {
                player.getWorld().playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
                player.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.4f, 1.2f);
            }
        }

        // ── Phase 2 (20-40): Surge ────────────────────────────────────────────
        else if (tick < 40) {
            int phase = tick - 20;
            double radius = phase * 0.3; // expanding ring

            int points = Math.max(8, (int)(radius * 8));
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                Location loc = player.getLocation().clone().add(
                    radius * Math.cos(angle), 0.1,
                    radius * Math.sin(angle)
                );
                ParticlePacket flame    = p.FLAME.packet(true, loc);
                ParticlePacket bubble   = p.BUBBLE_COLUMN_UP.packet(true, loc);
                ParticlePacket enchant  = p.ENCHANT.packet(true, loc);
                for (Player pl : nearby) {
                    flame.sendTo(pl);
                    bubble.sendTo(pl);
                    enchant.sendTo(pl);
                }
            }

            if (tick == 20) {
                player.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.4f);
                player.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.8f);
            }
        }

        // ── Phase 3 (40-60): Seal ─────────────────────────────────────────────
        else if (tick < 60) {
            int phase = tick - 40;
            double radius = (20 - phase) * 0.15; // contracting

            int points = 12;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i - (phase * 0.2);
                Location loc = center.clone().add(
                    radius * Math.cos(angle),
                    Math.sin(phase * 0.3) * 0.5,
                    radius * Math.sin(angle)
                );
                ParticlePacket poof    = p.POOF.packetMotion(true, loc,
                    new org.bukkit.util.Vector(
                        Math.cos(angle) * -0.05, 0.05, Math.sin(angle) * -0.05));
                ParticlePacket sculk   = p.SCULK_SOUL.packet(true, loc);
                for (Player pl : nearby) { poof.sendTo(pl); sculk.sendTo(pl); }
            }

            if (tick == 55) {
                // Final burst right before complete
                ParticlePacket totem = p.TOTEM_OF_UNDYING.packet(true, center, 30);
                for (Player pl : nearby) totem.sendTo(pl);
                player.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
            }
        }

        // ── Complete ──────────────────────────────────────────────────────────
        else {
            cancel();

            // Title
            Title title = Title.title(
                Component.text("ᴛʜᴇ ꜰᴜꜱɪᴏɴ")
                    .color(TextColor.fromHexString("#C8A96E"))
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("ᴛʜᴇ ᴜɴᴀɴɴᴏᴜɴᴄᴇᴅ ʜᴀꜱ ᴀᴄᴄᴇᴘᴛᴇᴅ ʏᴏᴜ")
                    .color(TextColor.fromHexString("#F5F0E8"))
                    .decoration(TextDecoration.ITALIC, false),
                Title.Times.times(
                    Duration.ofMillis(300),
                    Duration.ofSeconds(3),
                    Duration.ofMillis(700)
                )
            );
            player.showTitle(title);
            player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            onComplete.run();
        }

        tick++;
    }
}
