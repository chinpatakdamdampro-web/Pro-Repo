package dev.hammermaces.abilities.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.paraso.PresenceStackManager;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class JustShowedUpAbility {

    private final HammerMacesPlugin plugin;

    public JustShowedUpAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire(Player player, MaceConfig cfg) {
        PresenceStackManager psm = plugin.getPresenceStackManager();
        int stacks = psm.getStacks(player.getUniqueId());

        if (stacks < cfg.getJustShowedUpMinStacks()) {
            player.sendMessage(GradientUtils.parseLore(
                "&#C8A96Eˢᵗⁱˡˡ ʷᵃʳᵐⁱⁿᵍ ᵘᵖ... (" + stacks + "/" + cfg.getJustShowedUpMinStacks() + " stacks)"));
            // Refund cooldown — ability didn't actually fire
            plugin.getCooldownManager().clearAbility(player.getUniqueId(), "the_unannounced", "just_showed_up");
            return;
        }

        // Consume all stacks
        psm.consumeStacks(player.getUniqueId());

        Location center = player.getLocation();
        double radius = cfg.getJustShowedUpRadius();
        double knockbackMult = 1.0 + (stacks * 0.2); // more stacks = stronger burst

        for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity target)) continue;

            Vector away = target.getLocation().toVector()
                .subtract(center.toVector()).normalize()
                .multiply(1.8 * knockbackMult)
                .add(new Vector(0, 0.5, 0));
            target.setVelocity(away);

            target.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS, cfg.getJustShowedUpEffectDuration(), 1, false, true, true));
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, cfg.getJustShowedUpEffectDuration(), 1, false, true, true));
        }

        // Visual burst
        center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 1, 0),
            60, 0.5, 0.8, 0.5, 0.15);
        center.getWorld().spawnParticle(Particle.POOF, center, 30, 0.4, 0.4, 0.4, 0.08);

        center.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 1.0f);

        player.sendMessage(GradientUtils.parseLore("&#C8A96E⚡ Just Showed Up — " + stacks + " stacks consumed!"));

        // Notify quest manager
        plugin.getParasoQuestManager().onJustShowedUpFired();
    }
}
