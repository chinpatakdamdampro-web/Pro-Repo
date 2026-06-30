package dev.hammermaces.listeners.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.abilities.paraso.UnannouncedAbility;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public class ChaosTaxListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;
    private final Random random = new Random();

    private static final List<String> EFFECTS = List.of(
        "LAUNCH", "NAUSEA", "SLOWNESS", "AGGRO", "GLOWING"
    );

    public ChaosTaxListener(HammerMacesPlugin plugin) {
        this.plugin      = plugin;
        this.maceManager = plugin.getMaceManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!"the_unannounced".equals(maceManager.getMaceType(held))) return;
        if (!maceManager.isOwner(held, player.getName())) return;

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg == null) return;

        // Check Unannounced hit window — must do this regardless of quest tier
        if (target instanceof Player hitPlayer) {
            UnannouncedAbility ua = plugin.getUnannouncedAbility();
            if (ua != null) ua.onHit(player, hitPlayer, cfg);
        }

        // Quest 3 — track players hit
        if (target instanceof Player hitPlayer && cfg.getQuestTier() >= 2) {
            plugin.getParasoQuestManager().onPlayerHit(hitPlayer.getName());
        }

        // Chaos Tax proc — only tier 3+
        if (cfg.getQuestTier() < 3) return;
        if (random.nextInt(100) >= cfg.getChaosTaxProcChance()) return;

        String effect = EFFECTS.get(random.nextInt(EFFECTS.size()));
        switch (effect) {
            case "LAUNCH" -> target.setVelocity(new Vector(
                (random.nextDouble() - 0.5) * 0.6,
                0.8 + random.nextDouble() * 0.4,
                (random.nextDouble() - 0.5) * 0.6));
            case "NAUSEA" -> target.addPotionEffect(
                new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, true, true));
            case "SLOWNESS" -> target.addPotionEffect(
                new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, true, true));
            case "AGGRO" -> {
                for (Entity nearby : target.getWorld().getNearbyEntities(
                        target.getLocation(), 8, 4, 8)) {
                    if (nearby instanceof Mob mob && !mob.equals(target)) {
                        mob.setTarget(target instanceof LivingEntity le ? le : null);
                        break;
                    }
                }
            }
            case "GLOWING" -> target.addPotionEffect(
                new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false, false));
        }

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.25f, 0.8f);
    }
}
