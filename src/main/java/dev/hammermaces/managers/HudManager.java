package dev.hammermaces.managers;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.data.ParasoQuestData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the action bar entirely. Updates every second.
 *
 * Fixes applied:
 *  - Charged Smash indicator (First Draft) only shows if a Netherite Spear
 *    is actually slotted — previously it always showed a fake countdown.
 *  - Presence stacks (Paraso, tier 4+) now show as ▲▲▲▲▲ pips, filled vs empty,
 *    so stacks are actually visible instead of only a one-time chime sound.
 */
public class HudManager {

    private final HammerMacesPlugin plugin;
    private BukkitTask task;

    private final Map<UUID, Component> silentReadOverride = new HashMap<>();
    private final Map<UUID, Integer>   silentReadTicks    = new HashMap<>();

    public HudManager(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (!plugin.getMaceManager().isSoulboundMace(held)) continue;
                    if (!plugin.getMaceManager().isOwner(held, player.getName())) continue;

                    String maceId = plugin.getMaceManager().getMaceType(held);
                    if (maceId == null) continue;

                    MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
                    if (cfg == null) continue;

                    UUID uuid = player.getUniqueId();
                    if (silentReadOverride.containsKey(uuid)) {
                        int ticks = silentReadTicks.getOrDefault(uuid, 0) - 1;
                        if (ticks <= 0) {
                            silentReadOverride.remove(uuid);
                            silentReadTicks.remove(uuid);
                        } else {
                            silentReadTicks.put(uuid, ticks);
                            player.sendActionBar(silentReadOverride.get(uuid));
                            continue;
                        }
                    }

                    Component hud = buildActionBar(player, maceId, cfg);
                    if (hud != null) player.sendActionBar(hud);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) task.cancel();
    }

    public void showSilentReadPing(Player player, Component msg) {
        silentReadOverride.put(player.getUniqueId(), msg);
        silentReadTicks.put(player.getUniqueId(), 60);
    }

    private Component buildActionBar(Player player, String maceId, MaceConfig cfg) {
        UUID uuid = player.getUniqueId();
        CooldownManager cd = plugin.getCooldownManager();

        return switch (maceId) {
            case "hammer_of_poseidon" -> row(
                entry("❄",  cd, uuid, maceId, "cold_aura",   cfg.getColdAuraCooldown()),
                entry("🌀", cd, uuid, maceId, "tidal_surge", cfg.getTidalSurgeCooldown())
            );
            case "hammer_of_the_larpers" -> row(
                entry("🧊", cd, uuid, maceId, "larper_freeze", cfg.getLarperFreezeCooldown()),
                entry("⚜",  cd, uuid, maceId, "false_summon",  cfg.getFalseSummonCooldown()),
                entry("🎭", cd, uuid, maceId, "monologue",     cfg.getMonologueCooldown())
            );
            case "the_unannounced" -> buildParasoHud(player, cfg, cd, uuid);
            case "the_first_draft" -> buildArchitectHud(player, cfg, cd, uuid);
            default -> null;
        };
    }

    private Component buildParasoHud(Player player, MaceConfig cfg, CooldownManager cd, UUID uuid) {
        int tier = cfg.getQuestTier();
        ParasoQuestData data = plugin.getParasoQuestData();

        Component questMeter = switch (tier) {
            case 0 -> txt("🌿 " + data.getBiomesVisited().size() + "/5 biomes", "#88aa66");
            case 1 -> txt("🔥 " + data.getBlazeRodsCollected() + "/4", "#FF6600")
                .append(txt("   🐚 " + data.getNautilusCollected() + "/3", "#00CCFF"));
            case 2 -> txt("⚔ " + data.getPlayersHit().size() + "/2 players hit", "#cc5555");
            case 3 -> plugin.getParasoQuestManager().buildTimerDisplay();
            default -> null;
        };

        Component abilityHud = Component.empty();
        boolean any = false;
        if (tier >= 2) {
            abilityHud = entry("💨", cd, uuid, "the_unannounced", "dash", cfg.getDashCooldown());
            any = true;
        }
        if (tier >= 4) {
            if (any) abilityHud = abilityHud.append(sep());
            abilityHud = abilityHud.append(entry("👻", cd, uuid, "the_unannounced", "unannounced", cfg.getUnannouncedCooldown()));
            any = true;

            // Presence stacks — now visible as filled/empty pips
            int stacks = plugin.getPresenceStackManager().getStacks(uuid);
            int maxStacks = cfg.getAbsentMaxStacks();
            if (any) abilityHud = abilityHud.append(sep());
            abilityHud = abilityHud.append(buildStackPips(stacks, maxStacks));
        }
        if (tier >= 5) {
            if (any) abilityHud = abilityHud.append(sep());
            abilityHud = abilityHud.append(entry("💥", cd, uuid, "the_unannounced", "just_showed_up", cfg.getJustShowedUpCooldown()));
        }

        if (questMeter != null && any) {
            return questMeter.append(txt("   ✦   ", "#555555")).append(abilityHud);
        } else if (questMeter != null) {
            return questMeter;
        } else if (any) {
            return abilityHud;
        }
        return null;
    }

    /** Builds a visual pip row like ▲▲▲△△ for current/max Presence stacks. */
    private Component buildStackPips(int current, int max) {
        Component result = txt("⚡ ", "#888888");
        for (int i = 1; i <= max; i++) {
            result = result.append(txt(i <= current ? "▲" : "△",
                i <= current ? "#C8A96E" : "#444444"));
        }
        return result;
    }

    private Component buildArchitectHud(Player player, MaceConfig cfg, CooldownManager cd, UUID uuid) {
        Component result = row(
            entry("🛡", cd, uuid, "the_first_draft", "contingency",   cfg.getContingencyCooldown()),
            entry("🎯", cd, uuid, "the_first_draft", "predetermined", cfg.getPredeterminedCooldown())
        );

        // Only show Charged Smash if a Netherite Spear is actually slotted —
        // previously this always displayed a fake countdown even with nothing slotted.
        boolean hasSpear = plugin.getArchitectListener()
            .getInfinitySlots()
            .getRegistry()
            .hasNetheriteSpearSlotted(player);

        if (hasSpear) {
            boolean smashReady = player.hasMetadata("charged_smash_ready");
            String smashLabel  = smashReady ? "✅" : cfg.getChargedSmashIntervalSeconds() + "s";
            Component smash = txt("⚡ ", "#888888")
                .append(txt(smashLabel, smashReady ? "#55cc55" : "#cc5555"));
            result = result.append(sep()).append(smash);
        }

        return result;
    }

    private Component entry(String icon, CooldownManager cd, UUID uuid,
                            String maceId, String ability, int cooldown) {
        boolean ready = !cd.isOnCooldown(uuid, maceId, ability, cooldown);
        String  label = ready ? "✅" : cd.getRemainingSeconds(uuid, maceId, ability, cooldown) + "s";
        return txt(icon + " ", "#888888").append(txt(label, ready ? "#55cc55" : "#cc5555"));
    }

    private Component sep() {
        return txt("   ", "#333333");
    }

    private Component row(Component... entries) {
        Component result = Component.empty();
        for (int i = 0; i < entries.length; i++) {
            result = result.append(entries[i]);
            if (i < entries.length - 1) result = result.append(sep());
        }
        return result;
    }

    private Component txt(String text, String hex) {
        return Component.text(text)
            .color(TextColor.fromHexString(hex))
            .decoration(TextDecoration.ITALIC, false);
    }
}
