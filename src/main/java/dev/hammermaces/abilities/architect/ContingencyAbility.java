package dev.hammermaces.abilities.architect;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contingency — Sneak + Jump
 *
 * Arms a trigger for window-seconds. If hit during the window:
 *   - Attacker gets knocked back hard + Blindness + Slowness
 *   - Holder teleports contingency-teleport-distance blocks away in facing direction
 *   - Holder gets Absorption for absorption-duration ticks
 *   - Charges: up to 2. Second charge within 5s fires again with double knockback
 *
 * If window expires unused:
 *   - Cooldown refunded by partial-refund-seconds
 *   - Message: [ UNNECESSARY ]
 *
 * After firing, execution-window-seconds window for bonus damage on next hit.
 */
public class ContingencyAbility {

    private final HammerMacesPlugin plugin;

    private static final String MACE_ID  = "the_first_draft";
    private static final String ABILITY  = "contingency";

    // Active contingency state per player
    private final Map<UUID, ContingencyState> activeStates = new HashMap<>();

    public ContingencyAbility(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void arm(Player holder, MaceConfig cfg) {
        UUID uuid = holder.getUniqueId();

        ContingencyState state = new ContingencyState(cfg.getContingencyCharges());
        activeStates.put(uuid, state);

        sendMsg(holder, "[ ᴄᴏɴᴛɪɴɢᴇɴᴄʏ ᴀᴄᴛɪᴠᴇ ]", "#8aaabb");
        holder.playSound(holder.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f);

        // Schedule window expiry
        long windowTicks = cfg.getContingencyWindowSeconds() * 20L;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!activeStates.containsKey(uuid)) return; // already fired
            activeStates.remove(uuid);
            // Partial refund
            int refund = cfg.getContingencyPartialRefundSeconds();
            plugin.getCooldownManager().reduceAbilityCooldown(uuid, MACE_ID, ABILITY, refund);
            sendMsg(holder, "[ ᴜɴɴᴇᴄᴇꜱꜱᴀʀʏ ]", "#556677");
        }, windowTicks);
    }

    /**
     * Called when the holder takes damage while Contingency is armed.
     * Returns true if Contingency fired.
     */
    public boolean onHolderDamaged(Player holder, Player attacker, MaceConfig cfg) {
        UUID uuid = holder.getUniqueId();
        ContingencyState state = activeStates.get(uuid);
        if (state == null) return false;

        state.chargesUsed++;
        boolean isSecondCharge = state.chargesUsed >= 2;
        double knockbackMult = isSecondCharge ? 2.0 : 1.0;

        // Knock attacker back
        Vector away = attacker.getLocation().toVector()
            .subtract(holder.getLocation().toVector())
            .normalize()
            .multiply(2.5 * knockbackMult)
            .add(new Vector(0, 0.5, 0));
        attacker.setVelocity(away);

        // Apply debuffs to attacker
        attacker.addPotionEffect(new PotionEffect(
            PotionEffectType.BLINDNESS, cfg.getContingencyBlindnessDuration(), 0, false, true, true));
        attacker.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS, cfg.getContingencySlownessDuration(), 1, false, true, true));

        // Teleport holder away in facing direction
        Location dest = getTeleportDest(holder, cfg.getContingencyTeleportDistance());
        holder.teleport(dest);

        // Absorption for holder
        holder.addPotionEffect(new PotionEffect(
            PotionEffectType.ABSORPTION, cfg.getContingencyAbsorptionDuration(), 0, false, true, true));

        // Messages
        sendMsg(holder, "[ ᴀꜱ ᴘʟᴀɴɴᴇᴅ ]", "#CBD5E0");
        attacker.sendMessage(Component.text("[ ʏᴏᴜ ꜱʜᴏᴜʟᴅ ʜᴀᴠᴇ ᴋɴᴏᴡɴ ]")
            .color(TextColor.fromHexString("#4A5568"))
            .decoration(TextDecoration.ITALIC, false));

        // Sounds
        holder.getWorld().playSound(holder.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.2f);
        holder.getWorld().playSound(holder.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.0f);

        // Particles
        plugin.getParticleEffects().contingencyFires(holder.getLocation());

        // If charges exhausted or second charge used, remove state
        if (isSecondCharge || state.chargesUsed >= cfg.getContingencyCharges()) {
            activeStates.remove(uuid);
            // Start execution window
            startExecutionWindow(holder, cfg);
        }

        return true;
    }

    private void startExecutionWindow(Player holder, MaceConfig cfg) {
        UUID uuid = holder.getUniqueId();
        plugin.getFractureManager().setExecutionWindow(uuid, true);
        sendMsg(holder, "[ ᴇxᴇᴄᴜᴛɪᴏɴ ᴡɪɴᴅᴏᴡ ᴀᴄᴛɪᴠᴇ ]", "#8aaabb");

        long windowTicks = cfg.getContingencyExecutionWindowSeconds() * 20L;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getFractureManager().setExecutionWindow(uuid, false);
        }, windowTicks);
    }

    public boolean isArmed(UUID uuid) {
        return activeStates.containsKey(uuid);
    }

    private Location getTeleportDest(Player holder, double distance) {
        Location loc = holder.getLocation().clone();
        Vector dir = loc.getDirection().normalize().multiply(distance);
        dir.setY(0); // horizontal only
        Location dest = loc.add(dir);
        // Safety: find solid ground
        dest.setY(dest.getWorld().getHighestBlockYAt(dest.getBlockX(), dest.getBlockZ()) + 1);
        return dest;
    }

    private void sendMsg(Player player, String text, String hex) {
        player.sendMessage(Component.text(text)
            .color(TextColor.fromHexString(hex))
            .decoration(TextDecoration.ITALIC, false));
    }

    private static class ContingencyState {
        final int maxCharges;
        int chargesUsed = 0;
        ContingencyState(int maxCharges) { this.maxCharges = maxCharges; }
    }
}
