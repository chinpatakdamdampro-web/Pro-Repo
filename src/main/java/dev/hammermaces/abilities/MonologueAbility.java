package dev.hammermaces.abilities;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Monologue — Sneak + Sneak (double-tap)
 *
 * Pure villain power move. No freeze, no trick — just a dramatic pose
 * that grants Strength + temporary knockback immunity, rewarding
 * committing to the fight right after delivering the line.
 *
 * Knockback immunity implemented by zeroing incoming knockback velocity
 * for the buff duration via a lightweight scheduled check, since there's
 * no direct "immune to knockback" potion effect in vanilla.
 */
public class MonologueAbility {

    private final HammerMacesPlugin plugin;

    public MonologueAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player wielder, MaceConfig cfg) {
        // Strength buff
        wielder.addPotionEffect(new PotionEffect(
            PotionEffectType.STRENGTH, cfg.getMonologueDuration(),
            cfg.getMonologueStrengthLevel() - 1, false, true, true));

        // Knockback resistance via attribute — cleaner than velocity clamping
        applyTemporaryKnockbackResistance(wielder, cfg.getMonologueDuration());

        // Dialogue line
        wielder.sendMessage(buildLine(wielder.getName(), cfg.getMonologueLine()));

        // Pose particles + sound
        wielder.getWorld().spawnParticle(Particle.CRIT, wielder.getLocation().add(0, 1, 0),
            20, 0.4, 0.6, 0.4, 0.1);
        wielder.getWorld().playSound(wielder.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.3f);
        wielder.getWorld().playSound(wielder.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 0.6f);
    }

    private void applyTemporaryKnockbackResistance(Player player, int durationTicks) {
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
        if (attr == null) return;

        double original = attr.getBaseValue();
        attr.setBaseValue(1.0); // full knockback immunity

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) attr.setBaseValue(original);
        }, durationTicks);
    }

    private Component buildLine(String name, String text) {
        return Component.text(name)
            .color(TextColor.fromHexString("#DC143C"))
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(" " + text)
                .color(TextColor.fromHexString("#FF6666"))
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, true));
    }
}
