package dev.hammermaces.abilities.architect.augments;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps slotted items to their augment effects.
 * apply()    — called when item placed in slot
 * revokeAll()— removes all active effects for a player
 *
 * Spear reach uses Attribute.ENTITY_INTERACTION_RANGE (Paper 1.21.x).
 * Note: Spear materials in 1.21.11 end with _SPEAR (e.g. WOODEN_SPEAR).
 * Sword materials end with _SWORD.
 * These are separate item types with no overlap.
 */
public class AugmentRegistry {

    private final HammerMacesPlugin plugin;
    private static final String REACH_KEY = "hammermaces_spear_reach";

    // Track slotted items per player: UUID -> (slot -> Material)
    private final Map<UUID, Map<Integer, Material>> applied = new HashMap<>();

    public AugmentRegistry(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player, ItemStack item, int slot) {
        if (item == null) return;
        Material mat = item.getType();
        applied.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(slot, mat);

        if (isSpear(mat)) {
            applyReach(player, getSpearReach(mat));
            if (mat.name().equals("NETHERITE_SPEAR")) {
                scheduleChargedSmash(player);
            }
            return;
        }

        if (isSword(mat)) {
            // Sword bonus applied at hit time via getSwordDamageBonus()
            return;
        }

        // Other augments
        switch (mat) {
            case TURTLE_SCUTE ->
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false, false));
            case GOLDEN_APPLE ->
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false, false));
            case OBSIDIAN ->
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, true, false, false));
            case PHANTOM_MEMBRANE ->
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false, false));
            // BLAZE_ROD, ENDER_PEARL, FERMENTED_SPIDER_EYE,
            // NETHERITE_SCRAP, NETHER_STAR, ECHO_SHARD,
            // ELYTRA, TOTEM_OF_UNDYING — all handled at event time
            default -> {}
        }
    }

    public void revokeAll(Player player) {
        UUID uuid = player.getUniqueId();
        if (!applied.containsKey(uuid)) return;
        applied.remove(uuid);

        removeReach(player);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        player.removeMetadata("charged_smash_ready", plugin);
    }

    // ── Reach ─────────────────────────────────────────────────────────────────

    private void applyReach(Player player, double bonus) {
        removeReach(player);
        AttributeInstance attr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (attr == null) return;
        AttributeModifier mod = new AttributeModifier(
            new NamespacedKey(plugin, REACH_KEY),
            bonus,
            AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.MAINHAND
        );
        attr.addModifier(mod);
    }

    private void removeReach(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (attr == null) return;
        attr.getModifiers().stream()
            .filter(m -> m.getKey().getKey().equals(REACH_KEY))
            .toList()
            .forEach(attr::removeModifier);
    }

    private void scheduleChargedSmash(Player player) {
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_first_draft");
        if (cfg == null) return;
        int ticks = cfg.getChargedSmashIntervalSeconds() * 20;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.setMetadata("charged_smash_ready",
                    new FixedMetadataValue(plugin, true));
            }
        }, ticks);
    }

    // ── Material type helpers ─────────────────────────────────────────────────

    public boolean isSpear(Material mat) {
        return mat.name().endsWith("_SPEAR");
    }

    public boolean isSword(Material mat) {
        return mat.name().endsWith("_SWORD");
    }

    private double getSpearReach(Material mat) {
        return switch (mat.name()) {
            case "WOODEN_SPEAR"    -> 1.0;
            case "STONE_SPEAR"     -> 1.8;
            case "COPPER_SPEAR"    -> 2.2;
            case "IRON_SPEAR"      -> 2.7;
            case "GOLDEN_SPEAR"    -> 3.0;
            case "DIAMOND_SPEAR"   -> 3.4;
            case "NETHERITE_SPEAR" -> 4.0;
            default -> 0;
        };
    }

    // ── Damage/effect helpers (called from ArchitectListener) ─────────────────

    public double getSwordDamageBonus(Player player) {
        Map<Integer, Material> slots = applied.get(player.getUniqueId());
        if (slots == null) return 0;
        for (Material mat : slots.values()) {
            if (!isSword(mat)) continue;
            return switch (mat) {
                case WOODEN_SWORD   -> 1.0;
                case STONE_SWORD    -> 2.0;
                case IRON_SWORD     -> 3.0;
                case GOLDEN_SWORD   -> 2.0;
                case DIAMOND_SWORD  -> 4.0;
                case NETHERITE_SWORD -> 5.0;
                default -> 0;
            };
        }
        return 0;
    }

    public boolean hasSlotted(Player player, Material mat) {
        Map<Integer, Material> slots = applied.get(player.getUniqueId());
        if (slots == null) return false;
        return slots.containsValue(mat);
    }

    public double getNetherStarRadiusBonus(Player player) {
        if (!hasSlotted(player, Material.NETHER_STAR)) return 0;
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_first_draft");
        return cfg != null ? cfg.getNetherStarPredeterminedRadiusBonus() : 0;
    }

    /** Removes the Totem of Undying from the player's infinity slot after it fires. */
    public void consumeTotem(Player player) {
        Map<Integer, Material> slots = applied.get(player.getUniqueId());
        if (slots == null) return;
        slots.entrySet().removeIf(e -> e.getValue() == Material.TOTEM_OF_UNDYING);
        // Revoke and reapply remaining effects
        revokeAll(player);
        slots.forEach((slot, mat) -> apply(player, new ItemStack(mat), slot));
    }

    public Map<Integer, Material> getSlots(Player player) {
        return applied.getOrDefault(player.getUniqueId(), new HashMap<>());
    }
}
