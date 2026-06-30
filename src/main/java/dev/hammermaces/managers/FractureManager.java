package dev.hammermaces.managers;

import dev.hammermaces.HammerMacesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages Fracture stacks for The First Draft's Calculated passive.
 *
 * Each hit on a target while holding The First Draft adds a Fracture stack.
 * At max stacks (default 3): silent burst fires on the target.
 *   - Weakness III + Slowness III for 3 seconds
 *   - Bonus damage = missing-health-percent% of target's missing HP
 *   - Stacks reset after burst
 *
 * Execution Window: after Contingency fires, the next hit deals bonus damage.
 */
public class FractureManager {

    private final HammerMacesPlugin plugin;

    // Target UUID -> fracture stack count
    private final Map<UUID, Integer> fractureStacks = new HashMap<>();

    // Holder UUIDs with active execution window
    private final Set<UUID> executionWindows = new HashSet<>();

    public FractureManager(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Called on EntityDamageByEntityEvent from ArchitectListener.
     * Adds a fracture stack to the target and checks for burst.
     *
     * @param holder    The First Draft holder
     * @param target    Entity being hit
     * @param event     The damage event (to add bonus damage)
     * @param maxStacks From config
     * @param missingHealthPercent From config
     */
    public void onHit(Player holder, LivingEntity target, EntityDamageByEntityEvent event,
                      int maxStacks, int missingHealthPercent) {
        UUID targetId = target.getUniqueId();

        int current = fractureStacks.getOrDefault(targetId, 0);
        int newCount = current + 1;

        boolean hasExecWindow = executionWindows.contains(holder.getUniqueId());

        if (newCount >= maxStacks) {
            fractureStacks.put(targetId, 0);
            fireBurst(holder, target, event, missingHealthPercent, hasExecWindow);
            if (hasExecWindow) executionWindows.remove(holder.getUniqueId());
        } else {
            fractureStacks.put(targetId, newCount);
            // Private progress indicator — only the holder sees this
            String targetName = target instanceof org.bukkit.entity.Player p ? p.getName() : target.getName();
            holder.sendActionBar(net.kyori.adventure.text.Component.text(
                "🧮 " + targetName + " " + buildFracturePips(newCount, maxStacks))
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }
    }

    private String buildFracturePips(int current, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= max; i++) sb.append(i <= current ? "●" : "○");
        return sb.toString();
    }

    private void fireBurst(Player holder, LivingEntity target, EntityDamageByEntityEvent event,
                           int missingHealthPercent, boolean executionBonus) {
        // Stays silent TO THE TARGET on purpose — they shouldn't know it's coming.
        // The holder gets a private confirmation since they earned the setup.
        double maxHp = getMaxHealth(target);
        double missingHp = maxHp - target.getHealth();
        double bonusDamage = missingHp * (missingHealthPercent / 100.0);
        if (executionBonus) bonusDamage *= 2.0;

        event.setDamage(event.getDamage() + bonusDamage);

        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 2, false, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false, false));

        plugin.getParticleEffects().fractureBurst(target.getLocation());

        String targetName = target instanceof org.bukkit.entity.Player p ? p.getName() : target.getName();
        holder.sendActionBar(net.kyori.adventure.text.Component.text(
            "🧮 Fracture — " + targetName + " (+" + String.format("%.1f", bonusDamage) + " dmg)")
            .color(net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA));
    }

    public void setExecutionWindow(UUID holderUUID, boolean active) {
        if (active) {
            executionWindows.add(holderUUID);
        } else {
            executionWindows.remove(holderUUID);
        }
    }

    public boolean hasExecutionWindow(UUID holderUUID) {
        return executionWindows.contains(holderUUID);
    }

    public int getStacks(UUID targetId) {
        return fractureStacks.getOrDefault(targetId, 0);
    }

    /**
     * Adds initial fracture stacks when a player enters Predetermined zone.
     */
    public void addInitialStacks(UUID targetId, int count) {
        int current = fractureStacks.getOrDefault(targetId, 0);
        fractureStacks.put(targetId, Math.min(current + count, 10));
    }

    public void clearTarget(UUID targetId) {
        fractureStacks.remove(targetId);
    }

    private double getMaxHealth(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }
}
