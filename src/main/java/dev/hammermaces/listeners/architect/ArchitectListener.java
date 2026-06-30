package dev.hammermaces.listeners.architect;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.abilities.architect.ContingencyAbility;
import dev.hammermaces.abilities.architect.GroundSlamAbility;
import dev.hammermaces.abilities.architect.InfinitySlotAbility;
import dev.hammermaces.abilities.architect.augments.AugmentRegistry;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;

/**
 * Handles all First Draft runtime events.
 */
public class ArchitectListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;
    private final ContingencyAbility contingency;
    private final GroundSlamAbility groundSlam;
    private final InfinitySlotAbility infinitySlots;

    public ArchitectListener(HammerMacesPlugin plugin) {
        this.plugin       = plugin;
        this.maceManager  = plugin.getMaceManager();
        this.contingency  = new ContingencyAbility(plugin);
        this.groundSlam   = new GroundSlamAbility(plugin);
        this.infinitySlots = new InfinitySlotAbility(plugin);
    }

    // ── Holder hits something ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player holder)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack held = holder.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!maceManager.isOwner(held, holder.getName())) return;
        if (!"the_first_draft".equals(maceManager.getMaceType(held))) return;

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_first_draft");
        if (cfg == null) return;

        AugmentRegistry reg = infinitySlots.getRegistry();

        // Sword damage bonus
        double swordBonus = reg.getSwordDamageBonus(holder);
        if (swordBonus > 0) event.setDamage(event.getDamage() + swordBonus);

        // Blaze Rod — fire aspect
        if (reg.hasSlotted(holder, Material.BLAZE_ROD)) target.setFireTicks(100);

        // Charged Smash
        if (holder.hasMetadata("charged_smash_ready")) {
            holder.removeMetadata("charged_smash_ready", plugin);
            event.setDamage(event.getDamage() + 3.0);
            plugin.getParticleEffects().fractureBurst(target.getLocation());
            scheduleChargedSmash(holder, cfg);
            holder.sendActionBar(net.kyori.adventure.text.Component.text("⚡ Charged Smash fired — +3 damage")
                .color(org.bukkit.NamedTextColor.YELLOW));
        }

        // Calculated — fracture stacks
        plugin.getFractureManager().onHit(holder, target, event,
            cfg.getCalculatedMaxFractureStacks(),
            cfg.getCalculatedMissingHealthPercent());
    }

    // ── Holder takes damage ───────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHurt(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player holder)) return;

        ItemStack held = holder.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!maceManager.isOwner(held, holder.getName())) return;
        if (!"the_first_draft".equals(maceManager.getMaceType(held))) return;

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_first_draft");
        if (cfg == null) return;

        AugmentRegistry reg = infinitySlots.getRegistry();

        // Netherite Scrap — -10% damage
        if (reg.hasSlotted(holder, Material.NETHERITE_SCRAP)) {
            event.setDamage(event.getDamage() * 0.9);
            holder.sendActionBar(net.kyori.adventure.text.Component.text("🛡 Scrap absorbed 10% damage")
                .color(org.bukkit.NamedTextColor.GRAY));
        }

        // Fermented Spider Eye — attacker gets Blindness
        if (event.getDamager() instanceof LivingEntity attacker
                && reg.hasSlotted(holder, Material.FERMENTED_SPIDER_EYE)) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, true, true));
            holder.sendActionBar(net.kyori.adventure.text.Component.text("🕸 Attacker blinded")
                .color(org.bukkit.NamedTextColor.DARK_PURPLE));
        }

        // Ender Pearl — 20% dodge
        if (reg.hasSlotted(holder, Material.ENDER_PEARL) && Math.random() < 0.20) {
            event.setCancelled(true);
            holder.sendActionBar(net.kyori.adventure.text.Component.text("✦ Dodged — Ender Pearl")
                .color(org.bukkit.NamedTextColor.LIGHT_PURPLE));
            return;
        }

        // Contingency
        if (event.getDamager() instanceof Player attacker && contingency.isArmed(holder.getUniqueId()))
            contingency.onHolderDamaged(holder, attacker, cfg);
    }

    // ── Fall → Ground Slam ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player holder)) return;

        ItemStack held = holder.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!maceManager.isOwner(held, holder.getName())) return;
        if (!"the_first_draft".equals(maceManager.getMaceType(held))) return;

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_first_draft");
        if (cfg == null) return;

        groundSlam.onLand(holder, event, cfg);
    }

    // ── Elytra augment — Jump while airborne ─────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!maceManager.isOwner(held, player.getName())) return;
        if (!"the_first_draft".equals(maceManager.getMaceType(held))) return;

        AugmentRegistry reg = infinitySlots.getRegistry();
        if (!reg.hasSlotted(player, Material.ELYTRA)) return;
        if (!player.isGliding() && player.isOnGround()) return; // only if airborne

        // Short glide burst
        player.setGliding(true);
        Vector boost = player.getLocation().getDirection().normalize().multiply(1.5);
        boost.setY(Math.max(boost.getY(), 0.3));
        player.setVelocity(boost);

        // Cancel glide after 3 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.isGliding()) player.setGliding(false);
        }, 60L);
    }

    // ── Totem augment — Death prevention ─────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!maceManager.isOwner(held, player.getName())) return;
        if (!"the_first_draft".equals(maceManager.getMaceType(held))) return;

        AugmentRegistry reg = infinitySlots.getRegistry();
        if (!reg.hasSlotted(player, Material.TOTEM_OF_UNDYING)) return;

        // Consume totem from slot
        reg.consumeTotem(player);

        // Cancel death — restore health + apply totem effects
        event.setCancelled(true);
        player.setHealth(1.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        plugin.getParticleEffects().justShowedUpBurst(player.getLocation(), 3.0);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        infinitySlots.cleanup(event.getPlayer());
    }

    private void scheduleChargedSmash(Player player, MaceConfig cfg) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline())
                player.setMetadata("charged_smash_ready", new FixedMetadataValue(plugin, true));
        }, cfg.getChargedSmashIntervalSeconds() * 20L);
    }

    public ContingencyAbility getContingency()     { return contingency; }
    public InfinitySlotAbility getInfinitySlots()  { return infinitySlots; }
}
