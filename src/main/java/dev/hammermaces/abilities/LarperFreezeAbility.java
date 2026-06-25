package dev.hammermaces.abilities;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.utils.FreezeUtils;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Larper Freeze ability — Sneak + Jump
 *
 * Freezes nearby players in place (position + camera), sends dramatic
 * dialogue lines in chat, then releases them and grants the wielder Strength.
 *
 * Combat immunity: players recently hit are not frozen.
 */
public class LarperFreezeAbility {

    public static final String ID = "larper_freeze";

    private final HammerMacesPlugin plugin;

    // Tracks last damage time per player for combat immunity (UUID -> System.currentTimeMillis())
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();

    public LarperFreezeAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player wielder, MaceConfig cfg) {
        double radius = cfg.getLarperFreezeRadius();
        long freezeTicks = cfg.getLarperFreezeDuration() * 20L;

        List<Player> targets = new ArrayList<>();

        for (Entity entity : wielder.getWorld().getNearbyEntities(wielder.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(wielder)) continue;

            // Combat immunity check
            if (cfg.isLarperFreezeCombatImmunity()) {
                Long lastHit = lastDamageTime.get(target.getUniqueId());
                if (lastHit != null) {
                    long elapsed = System.currentTimeMillis() - lastHit;
                    if (elapsed < cfg.getLarperFreezeCombatImmunityWindow() * 50L) continue; // ticks to ms
                }
            }

            targets.add(target);
        }

        if (targets.isEmpty()) return;

        // Activation sound — Elder Guardian Curse for all in range
        wielder.getWorld().playSound(
            wielder.getLocation(),
            Sound.ENTITY_ELDER_GUARDIAN_CURSE,
            2.0f, 0.8f
        );

        // Apply blindness + freeze to all targets
        for (Player target : targets) {
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                (int) freezeTicks + 10,
                0, false, false, false
            ));
            FreezeUtils.freeze(plugin, target, freezeTicks);
        }

        // Schedule dialogue lines
        int line1DelayTicks = cfg.getLarperFreezeLine1Delay() * 20;
        int line2DelayTicks = cfg.getLarperFreezeLine2Delay() * 20;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Component line1 = buildDialogueLine(wielder.getName(), cfg.getLarperFreezeLine1());
            for (Player target : targets) {
                if (target.isOnline()) target.sendMessage(line1);
            }
            wielder.sendMessage(line1);
        }, line1DelayTicks);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Component line2 = buildDialogueLine(wielder.getName(), cfg.getLarperFreezeLine2());
            for (Player target : targets) {
                if (target.isOnline()) target.sendMessage(line2);
            }
            wielder.sendMessage(line2);
        }, line2DelayTicks);

        // On freeze end: release, grant Strength, play release sound
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player target : targets) {
                if (target.isOnline()) {
                    FreezeUtils.unfreeze(target.getUniqueId());
                }
            }

            wielder.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH,
                cfg.getLarperFreezeStrengthDuration(),
                cfg.getLarperFreezeStrengthLevel() - 1,
                false, true, true
            ));

            // Release sound — Ravager Roar
            wielder.getWorld().playSound(
                wielder.getLocation(),
                Sound.ENTITY_RAVAGER_ROAR,
                2.0f, 0.9f
            );

        }, freezeTicks);
    }

    /**
     * Records when a player last took damage, used for combat immunity checks.
     * Call this from an EntityDamageEvent listener.
     */
    public void recordDamage(UUID uuid) {
        lastDamageTime.put(uuid, System.currentTimeMillis());
    }

    /**
     * Builds the crimson red dialogue line component.
     * Format: [WielderName] «dialogue text»
     */
    private Component buildDialogueLine(String wielderName, String line) {
        return Component.empty()
            .append(
                Component.text(wielderName)
                    .color(TextColor.fromHexString("#DC143C"))
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false)
            )
            .append(
                Component.text(" " + line)
                    .color(TextColor.fromHexString("#FF6666"))
                    .decoration(TextDecoration.ITALIC, true)
                    .decoration(TextDecoration.BOLD, false)
            );
    }
                      }
