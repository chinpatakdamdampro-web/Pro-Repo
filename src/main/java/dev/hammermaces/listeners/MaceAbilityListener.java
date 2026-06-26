package dev.hammermaces.listeners;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.abilities.ColdAuraAbility;
import dev.hammermaces.abilities.LarperFreezeAbility;
import dev.hammermaces.abilities.TidalSurgeAbility;
import dev.hammermaces.abilities.paraso.DashAbility;
import dev.hammermaces.abilities.paraso.JustShowedUpAbility;
import dev.hammermaces.abilities.paraso.UnannouncedAbility;
import dev.hammermaces.managers.CooldownManager;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerJumpEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thin input router. Detects:
 *   Sneak + Jump  → Cold Aura (Poseidon) / Larper Freeze / Unannounced (Paraso)
 *   Sneak + Swing → Tidal Surge (Poseidon) / Just Showed Up (Paraso)
 *   Sneak alone   → Dash (Paraso) — fires on sneak press
 *
 * Uses Paper's PlayerJumpEvent (1.17+) for reliable jump detection on Java and Bedrock/Geyser.
 * No velocity polling, no double-handlers.
 */
public class MaceAbilityListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;
    private final CooldownManager cooldownManager;

    // Ability instances
    private final ColdAuraAbility coldAura;
    private final TidalSurgeAbility tidalSurge;
    private final LarperFreezeAbility larperFreeze;
    private final DashAbility dash;
    private final UnannouncedAbility unannounced;
    private final JustShowedUpAbility justShowedUp;

    /** Tracks current sneak state per player */
    private final Map<UUID, Boolean> sneaking = new HashMap<>();

    public MaceAbilityListener(HammerMacesPlugin plugin) {
        this.plugin          = plugin;
        this.maceManager     = plugin.getMaceManager();
        this.cooldownManager = plugin.getCooldownManager();

        this.coldAura     = new ColdAuraAbility(plugin);
        this.tidalSurge   = new TidalSurgeAbility(plugin);
        this.larperFreeze = new LarperFreezeAbility(plugin);
        this.dash         = new DashAbility(plugin);
        this.unannounced  = new UnannouncedAbility(plugin);
        this.justShowedUp = new JustShowedUpAbility(plugin);
    }

    // ── Sneak state tracking ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        sneaking.put(uuid, event.isSneaking());

        // Dash fires on sneak press for Paraso (Bedrock-friendly — no second input needed)
        if (event.isSneaking()) {
            handleDash(event.getPlayer());
        }
    }

    // ── Sneak + Jump ──────────────────────────────────────────────────────────

    /**
     * PlayerJumpEvent is fired by Paper when the server detects a jump packet.
     * Works reliably on Java and Bedrock via Geyser.
     * We check if the player is sneaking at the moment of the jump.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (!sneaking.getOrDefault(player.getUniqueId(), false)) return;
        handleSneakJump(player);
    }

    // ── Sneak + Swing ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = event.getPlayer();
        if (!sneaking.getOrDefault(player.getUniqueId(), false)) return;
        handleSneakSwing(player);
    }

    // ── Combat damage tracking (for Larper Freeze immunity) ──────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        larperFreeze.recordDamage(player.getUniqueId());
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    private void handleDash(Player player) {
        ItemStack held = heldSoulboundMace(player);
        if (held == null) return;
        String maceId = maceManager.getMaceType(held);
        if (!"the_unannounced".equals(maceId)) return;
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null || cfg.getQuestTier() < 2) return; // Dash unlocks at tier 2

        if (fire(player, maceId, "dash", cfg.getDashCooldown())) {
            dash.fire(player, cfg);
        }
    }

    private void handleSneakJump(Player player) {
        ItemStack held = heldSoulboundMace(player);
        if (held == null) return;
        String maceId = maceManager.getMaceType(held);
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null) return;

        switch (maceId) {
            case "hammer_of_poseidon" -> {
                if (cfg.isColdAuraEnabled() && fire(player, maceId, "cold_aura", cfg.getColdAuraCooldown())) {
                    coldAura.fire(player, cfg);
                }
            }
            case "hammer_of_the_larpers" -> {
                if (cfg.isLarperFreezeEnabled() && fire(player, maceId, "larper_freeze", cfg.getLarperFreezeCooldown())) {
                    larperFreeze.fire(player, cfg);
                }
            }
            case "the_unannounced" -> {
                if (cfg.isUnannouncedEnabled() && cfg.getQuestTier() >= 4
                        && fire(player, maceId, "unannounced", cfg.getUnannouncedCooldown())) {
                    unannounced.fire(player, cfg);
                }
            }
        }
    }

    private void handleSneakSwing(Player player) {
        ItemStack held = heldSoulboundMace(player);
        if (held == null) return;
        String maceId = maceManager.getMaceType(held);
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null) return;

        switch (maceId) {
            case "hammer_of_poseidon" -> {
                if (cfg.isTidalSurgeEnabled() && fire(player, maceId, "tidal_surge", cfg.getTidalSurgeCooldown())) {
                    tidalSurge.fire(player, cfg);
                }
            }
            case "the_unannounced" -> {
                if (cfg.isJustShowedUpEnabled() && cfg.getQuestTier() >= 5
                        && fire(player, maceId, "just_showed_up", cfg.getJustShowedUpCooldown())) {
                    justShowedUp.fire(player, cfg);
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Checks and sets cooldown. Returns true if ability can fire (not on cooldown).
     * Cooldown is only set AFTER we confirm the check passes — never on failure.
     */
    private boolean fire(Player player, String maceId, String ability, int cooldownSeconds) {
        UUID uuid = player.getUniqueId();
        if (cooldownManager.isOnCooldown(uuid, maceId, ability, cooldownSeconds)) {
            int rem = cooldownManager.getRemainingSeconds(uuid, maceId, ability, cooldownSeconds);
            sendCooldown(player, ability, rem);
            return false;
        }
        cooldownManager.setCooldown(uuid, maceId, ability);
        return true;
    }

    private ItemStack heldSoulboundMace(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return null;
        if (!maceManager.isOwner(held, player.getName())) return null;
        return held;
    }

    private void sendCooldown(Player player, String ability, int seconds) {
        String template = plugin.getConfig().getString(
            "messages.on-cooldown",
            "&#00e5ff✦ &7{ability} is on cooldown for &f{seconds}s&7."
        );
        player.sendMessage(GradientUtils.parseLore(
            template.replace("{ability}", ability.replace("_", " "))
                    .replace("{seconds}", String.valueOf(seconds))
        ));
    }

    public LarperFreezeAbility getLarperFreezeAbility() { return larperFreeze; }
}
