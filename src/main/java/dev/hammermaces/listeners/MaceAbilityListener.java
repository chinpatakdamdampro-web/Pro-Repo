package dev.hammermaces.listeners;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.abilities.ColdAuraAbility;
import dev.hammermaces.abilities.FalseSummonAbility;
import dev.hammermaces.abilities.LarperFreezeAbility;
import dev.hammermaces.abilities.MonologueAbility;
import dev.hammermaces.abilities.TidalSurgeAbility;
import dev.hammermaces.abilities.architect.InfinitySlotAbility;
import dev.hammermaces.abilities.architect.PredeterminedAbility;
import dev.hammermaces.abilities.architect.augments.AugmentRegistry;
import dev.hammermaces.abilities.paraso.DashAbility;
import dev.hammermaces.abilities.paraso.JustShowedUpAbility;
import dev.hammermaces.abilities.paraso.UnannouncedAbility;
import dev.hammermaces.listeners.architect.ArchitectListener;
import dev.hammermaces.managers.CooldownManager;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Thin input router. All ability classes live separately.
 *
 * Inputs:
 *   Sneak          → Dash (Paraso)
 *   Sneak + Jump   → Cold Aura / Larper Freeze / Unannounced / Contingency
 *   Sneak + Swing  → Tidal Surge / Just Showed Up / Predetermined
 *   Sneak + RClick → Infinity Slots (First Draft)
 */
public class MaceAbilityListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;
    private final CooldownManager cooldownManager;

    private final ColdAuraAbility coldAura;
    private final TidalSurgeAbility tidalSurge;
    private final LarperFreezeAbility larperFreeze;
    private final FalseSummonAbility falseSummon;
    private final MonologueAbility monologue;
    private final DashAbility dash;
    private final UnannouncedAbility unannounced;
    private final JustShowedUpAbility justShowedUp;
    private final PredeterminedAbility predetermined;

    // Architect listener holds contingency + infinitySlots
    private final ArchitectListener architectListener;

    private final Map<UUID, Boolean> sneaking         = new HashMap<>();
    private final Set<UUID> jumpFiredThisSneak         = new HashSet<>();
    private final Set<UUID> rclickFiredThisSneak       = new HashSet<>();

    /** Last sneak-press timestamp per player, for Monologue double-tap detection. */
    private final Map<UUID, Long> lastSneakPress = new HashMap<>();
    private static final long DOUBLE_TAP_WINDOW_MS = 400L;

    public MaceAbilityListener(HammerMacesPlugin plugin) {
        this.plugin          = plugin;
        this.maceManager     = plugin.getMaceManager();
        this.cooldownManager = plugin.getCooldownManager();

        this.coldAura       = new ColdAuraAbility(plugin);
        this.tidalSurge     = new TidalSurgeAbility(plugin);
        this.larperFreeze   = new LarperFreezeAbility(plugin);
        this.falseSummon    = new FalseSummonAbility(plugin);
        this.monologue      = new MonologueAbility(plugin);
        this.dash           = new DashAbility(plugin);
        this.unannounced    = new UnannouncedAbility(plugin);
        this.justShowedUp   = new JustShowedUpAbility(plugin);
        this.predetermined  = new PredeterminedAbility(plugin);

        // ArchitectListener is also registered as a Bukkit listener — get it from plugin
        this.architectListener = plugin.getArchitectListener();
    }

    // ── Sneak tracking ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        sneaking.put(uuid, event.isSneaking());

        if (event.isSneaking()) {
            jumpFiredThisSneak.remove(uuid);
            rclickFiredThisSneak.remove(uuid);
            handleDash(event.getPlayer());

            // Double-tap detection for Monologue
            long now = System.currentTimeMillis();
            Long last = lastSneakPress.get(uuid);
            if (last != null && now - last <= DOUBLE_TAP_WINDOW_MS) {
                lastSneakPress.remove(uuid); // consume so a 3rd tap doesn't chain-fire
                handleMonologue(event.getPlayer());
            } else {
                lastSneakPress.put(uuid, now);
            }
        }
    }

    // ── Sneak + Jump ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!sneaking.getOrDefault(uuid, false)) return;
        if (jumpFiredThisSneak.contains(uuid)) return;
        jumpFiredThisSneak.add(uuid);
        handleSneakJump(player);
    }

    // ── Sneak + Right Click → Infinity Slots ─────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!sneaking.getOrDefault(uuid, false)) return;
        if (rclickFiredThisSneak.contains(uuid)) return;

        ItemStack held = heldSoulbound(player);
        if (held == null) return;
        if (!"the_first_draft".equals(maceManager.getMaceType(held))) return;

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_first_draft");
        if (cfg == null || !cfg.isInfinitySlotsEnabled()) return;

        rclickFiredThisSneak.add(uuid);
        event.setCancelled(true);

        InfinitySlotAbility slots = architectListener.getInfinitySlots();
        if (!slots.isOpen(uuid)) {
            slots.open(player, cfg);
        }
    }

    // ── Sneak + Swing ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = event.getPlayer();
        if (!sneaking.getOrDefault(player.getUniqueId(), false)) return;
        handleSneakSwing(player);
    }

    // ── Combat damage tracking (Larper Freeze immunity) ───────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        larperFreeze.recordDamage(player.getUniqueId());
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleDash(Player player) {
        ItemStack held = heldSoulbound(player);
        if (held == null) return;
        String maceId = maceManager.getMaceType(held);
        if (!"the_unannounced".equals(maceId)) return;
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null || cfg.getQuestTier() < 2) return;
        if (fire(player, maceId, "dash", cfg.getDashCooldown())) dash.fire(player, cfg);
    }

    private void handleSneakJump(Player player) {
        ItemStack held = heldSoulbound(player);
        if (held == null) return;
        String maceId = maceManager.getMaceType(held);
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null) return;

        switch (maceId) {
            case "hammer_of_poseidon" -> {
                if (cfg.isColdAuraEnabled() && fire(player, maceId, "cold_aura", cfg.getColdAuraCooldown()))
                    coldAura.fire(player, cfg);
            }
            case "hammer_of_the_larpers" -> {
                if (cfg.isLarperFreezeEnabled() && fire(player, maceId, "larper_freeze", cfg.getLarperFreezeCooldown()))
                    larperFreeze.fire(player, cfg);
            }
            case "the_unannounced" -> {
                if (cfg.isUnannouncedEnabled() && cfg.getQuestTier() >= 4
                        && fire(player, maceId, "unannounced", cfg.getUnannouncedCooldown()))
                    unannounced.fire(player, cfg);
            }
            case "the_first_draft" -> {
                if (fire(player, maceId, "contingency", cfg.getContingencyCooldown()))
                    architectListener.getContingency().arm(player, cfg);
            }
        }
    }

    private void handleSneakSwing(Player player) {
        ItemStack held = heldSoulbound(player);
        if (held == null) return;
        String maceId = maceManager.getMaceType(held);
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null) return;

        switch (maceId) {
            case "hammer_of_poseidon" -> {
                if (cfg.isTidalSurgeEnabled() && fire(player, maceId, "tidal_surge", cfg.getTidalSurgeCooldown()))
                    tidalSurge.fire(player, cfg);
            }
            case "hammer_of_the_larpers" -> {
                if (cfg.isFalseSummonEnabled() && fire(player, maceId, "false_summon", cfg.getFalseSummonCooldown()))
                    falseSummon.fire(player, cfg);
            }
            case "the_unannounced" -> {
                if (cfg.isJustShowedUpEnabled() && cfg.getQuestTier() >= 5
                        && fire(player, maceId, "just_showed_up", cfg.getJustShowedUpCooldown()))
                    justShowedUp.fire(player, cfg);
            }
            case "the_first_draft" -> {
                AugmentRegistry reg = architectListener.getInfinitySlots().getRegistry();
                double extraRadius = reg.getNetherStarRadiusBonus(player);
                if (fire(player, maceId, "predetermined", cfg.getPredeterminedCooldown()))
                    predetermined.fire(player, cfg, extraRadius);
            }
        }
    }

    private void handleMonologue(Player player) {
        ItemStack held = heldSoulbound(player);
        if (held == null) return;
        String maceId = maceManager.getMaceType(held);
        if (!"hammer_of_the_larpers".equals(maceId)) return;
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null || !cfg.isMonologueEnabled()) return;
        if (fire(player, maceId, "monologue", cfg.getMonologueCooldown()))
            monologue.fire(player, cfg);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean fire(Player player, String maceId, String ability, int seconds) {
        UUID uuid = player.getUniqueId();
        if (cooldownManager.isOnCooldown(uuid, maceId, ability, seconds)) {
            int rem = cooldownManager.getRemainingSeconds(uuid, maceId, ability, seconds);
            player.sendMessage(GradientUtils.parseLore(
                plugin.getConfig().getString("messages.on-cooldown",
                    "&#00e5ff✦ &7{ability} is on cooldown for &f{seconds}s&7.")
                    .replace("{ability}", ability.replace("_", " "))
                    .replace("{seconds}", String.valueOf(rem))
            ));
            return false;
        }
        cooldownManager.setCooldown(uuid, maceId, ability);
        return true;
    }

    private ItemStack heldSoulbound(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return null;
        if (!maceManager.isOwner(held, player.getName())) return null;
        return held;
    }

    public UnannouncedAbility getUnannouncedAbility() { return unannounced; }
    public LarperFreezeAbility getLarperFreezeAbility() { return larperFreeze; }
}
