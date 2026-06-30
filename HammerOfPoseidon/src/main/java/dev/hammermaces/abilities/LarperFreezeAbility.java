package dev.hammermaces.abilities;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.utils.FreezeUtils;
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

public class LarperFreezeAbility {

    private final HammerMacesPlugin plugin;
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();

    public LarperFreezeAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player wielder, MaceConfig cfg) {
        double radius = cfg.getLarperFreezeRadius();
        long freezeTicks = cfg.getLarperFreezeDuration() * 20L;

        List<Player> targets = new ArrayList<>();
        for (Entity entity : wielder.getWorld().getNearbyEntities(
                wielder.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof Player target) || target.equals(wielder)) continue;
            if (cfg.isLarperFreezeCombatImmunity()) {
                Long lastHit = lastDamageTime.get(target.getUniqueId());
                if (lastHit != null && System.currentTimeMillis() - lastHit
                        < cfg.getLarperFreezeCombatImmunityWindow() * 50L) continue;
            }
            targets.add(target);
        }

        if (targets.isEmpty()) {
            wielder.sendMessage(buildLine(wielder.getName(), "«...No one worth stopping.»"));
            return;
        }

        wielder.getWorld().playSound(wielder.getLocation(),
            Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.8f);

        for (Player target : targets) {
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS, (int) freezeTicks + 10, 0, false, false, false));
            FreezeUtils.freeze(plugin, target, freezeTicks);
        }

        int line1Ticks = cfg.getLarperFreezeLine1Delay() * 20;
        int line2Ticks = cfg.getLarperFreezeLine2Delay() * 20;
        Component line1 = buildLine(wielder.getName(), cfg.getLarperFreezeLine1());
        Component line2 = buildLine(wielder.getName(), cfg.getLarperFreezeLine2());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            targets.forEach(t -> { if (t.isOnline()) t.sendMessage(line1); });
            wielder.sendMessage(line1);
        }, line1Ticks);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            targets.forEach(t -> { if (t.isOnline()) t.sendMessage(line2); });
            wielder.sendMessage(line2);
        }, line2Ticks);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            targets.forEach(t -> FreezeUtils.unfreeze(t.getUniqueId()));
            wielder.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH, cfg.getLarperFreezeStrengthDuration(),
                cfg.getLarperFreezeStrengthLevel() - 1, false, true, true));
            wielder.getWorld().playSound(wielder.getLocation(),
                Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.9f);
        }, freezeTicks);
    }

    public void recordDamage(UUID uuid) {
        lastDamageTime.put(uuid, System.currentTimeMillis());
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
