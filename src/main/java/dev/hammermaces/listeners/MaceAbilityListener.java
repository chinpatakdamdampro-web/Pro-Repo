package dev.hammermaces.listeners;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.abilities.ColdAuraAbility;
import dev.hammermaces.abilities.LarperFreezeAbility;
import dev.hammermaces.abilities.TidalSurgeAbility;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.CooldownManager;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.utils.FreezeUtils;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thin input router — detects Sneak+Jump and Sneak+Swing,
 * then delegates to the appropriate ability class.
 * No ability logic lives here.
 */
public class MaceAbilityListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;
    private final CooldownManager cooldownManager;

    private final ColdAuraAbility coldAura;
    private final TidalSurgeAbility tidalSurge;
    private final LarperFreezeAbility larperFreeze;

    private final Map<UUID, Boolean> sneakingPlayers = new HashMap<>();

    public MaceAbilityListener(HammerMacesPlugin plugin) {
        this.plugin        = plugin;
        this.maceManager   = plugin.getMaceManager();
        this.cooldownManager = plugin.getCooldownManager();

        this.coldAura    = new ColdAuraAbility(plugin);
        this.tidalSurge  = new TidalSurgeAbility(plugin);
        this.larperFreeze = new LarperFreezeAbility(plugin);
    }

    // ── Sneak tracking ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSneak(PlayerToggleSneakEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        sneakingPlayers.put(uuid, event.isSneaking());

        if (!event.isSneaking()) return;

        // Watch for jump while sneaking → Sneak + Jump abilities
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            int checks = 0;

            @Override
            public void run() {
                if (checks++ > 15) return;
                if (!sneakingPlayers.getOrDefault(uuid, false)) return;

                Player p = plugin.getServer().getPlayer(uuid);
                if (p == null) return;

                if (p.getVelocity().getY() > 0.1) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> handleSneakJump(p));
                    return;
                }

                plugin.getServer().getScheduler().runTaskLater(plugin, this, 2L);
            }
        }, 1L);
    }

    // ── Sneak + Jump → Cold Aura / Larper Freeze ──────────────────────────────

    private void handleSneakJump(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!maceManager.isOwner(held, player.getName())) return;

        String maceId = maceManager.getMaceType(held);
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null) return;

        // Try Cold Aura
        if (cfg.isColdAuraEnabled()) {
            if (tryFireAbility(player, maceId, ColdAuraAbility.ID, cfg.getColdAuraCooldown())) {
                coldAura.fire(player, cfg);
                return;
            }
        }

        // Try Larper Freeze
        if (cfg.isLarperFreezeEnabled()) {
            if (tryFireAbility(player, maceId, LarperFreezeAbility.ID, cfg.getLarperFreezeCooldown())) {
                larperFreeze.fire(player, cfg);
            }
        }
    }

    // ── Sneak + Swing → Tidal Surge ───────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        if (!sneakingPlayers.getOrDefault(player.getUniqueId(), false)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!maceManager.isOwner(held, player.getName())) return;

        String maceId = maceManager.getMaceType(held);
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null || !cfg.isTidalSurgeEnabled()) return;

        if (tryFireAbility(player, maceId, TidalSurgeAbility.ID, cfg.getTidalSurgeCooldown())) {
            tidalSurge.fire(player, cfg);
        }
    }

    // ── Combat immunity damage tracking ───────────────────────────────────────

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        larperFreeze.recordDamage(player.getUniqueId());
    }

    // ── Shared cooldown helper ─────────────────────────────────────────────────

    /**
     * Checks cooldown and fires it if ready. Returns true if the ability can fire.
     */
    private boolean tryFireAbility(Player player, String maceId, String abilityId, int cooldownSeconds) {
        if (cooldownManager.isOnCooldown(player.getUniqueId(), maceId, abilityId, cooldownSeconds)) {
            int remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), maceId, abilityId, cooldownSeconds);
            sendCooldownMessage(player, abilityId, remaining);
            return false;
        }
        cooldownManager.setCooldown(player.getUniqueId(), maceId, abilityId);
        return true;
    }

    private void sendCooldownMessage(Player player, String abilityName, int seconds) {
        String template = plugin.getConfig().getString(
            "messages.on-cooldown",
            "&#1a1a6e✦ &#00e5ff{ability} &#7f7f7fis on cooldown for &#ffffff{seconds}s&#7f7f7f."
        );
        String msg = template
            .replace("{ability}", abilityName.replace("_", " "))
            .replace("{seconds}", String.valueOf(seconds));
        player.sendMessage(GradientUtils.parseLore(msg));
    }
}
