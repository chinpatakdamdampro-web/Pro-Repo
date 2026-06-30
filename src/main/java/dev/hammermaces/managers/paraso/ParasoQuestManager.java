package dev.hammermaces.managers.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.data.ParasoQuestData;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;

public class ParasoQuestManager {

    private final HammerMacesPlugin plugin;
    private final ParasoQuestData data;

    private static final String DAMAGE_KEY = "hammermaces_paraso_damage";

    public ParasoQuestManager(HammerMacesPlugin plugin, ParasoQuestData data) {
        this.plugin = plugin;
        this.data   = data;
    }

    // ── Called from listeners ─────────────────────────────────────────────────

    public void onLogin() {
        // Login tracking removed in quest 2 rework — kept for API compatibility
    }

    public void onBiomeEnter(String biomeName) {
        if (data.getCurrentTier() >= 1) return;
        data.addBiome(biomeName);
        data.save();
        if (data.getBiomesVisited().size() >= 5) advanceTier(1);
    }

    public void onFusionComplete(Player player) {
        if (data.getCurrentTier() != 1) return;
        advanceTier(2);
    }

    public void onPlayerHit(String targetName) {
        if (data.getCurrentTier() != 2) return;
        data.addPlayerHit(targetName);
        data.save();
        if (data.getPlayersHit().size() >= 2) advanceTier(3);
    }

    public void onHoldTimeTick(long addedSeconds) {
        if (data.getCurrentTier() != 3) return;
        data.addHoldTime(addedSeconds);
        if (data.getHoldTimeSeconds() % 10 == 0) data.save();
        if (data.getHoldTimeSeconds() >= 1200L) advanceTier(4);
    }

    public void onJustShowedUpFired() {
        if (data.getCurrentTier() != 4) return;
        if (data.isJustShowedUpFired()) return;
        data.setJustShowedUpFired(true);
        data.save();
        advanceTier(5);
    }

    // ── Tier advancement ──────────────────────────────────────────────────────

    private void advanceTier(int tier) {
        data.setCurrentTier(tier);
        data.save();

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg != null) {
            cfg.setQuestTier(tier);
            plugin.getMaceConfigManager().saveQuestTier("the_unannounced", tier);
        }

        Player paraso = findParaso();

        switch (tier) {
            case 1 -> {
                if (paraso != null) {
                    giveMace(paraso);
                    sendQuestComplete(paraso, "ᴘᴀʀᴀꜱᴏ ᴍᴜꜱᴛ ᴇxᴘʟᴏʀᴇ!", "ᴛʜᴇ ᴜɴᴀɴɴᴏᴜɴᴄᴇᴅ ʜᴀꜱ ᴄʜᴏꜱᴇɴ ʏᴏᴜ");
                }
            }
            case 2 -> {
                if (paraso != null) {
                    applyFusionBuffs(paraso);
                    refreshMaceLore(paraso);
                    sendQuestComplete(paraso, "ᴛʜᴇ ꜰᴜꜱɪᴏɴ", "ᴅᴀꜱʜ ᴜɴʟᴏᴄᴋᴇᴅ");
                }
            }
            case 3 -> {
                if (paraso != null) {
                    refreshMaceLore(paraso);
                    sendQuestComplete(paraso, "ᴛʜᴇ ɴᴜɪꜱᴀɴᴄᴇ", "ᴄʜᴀᴏꜱ ᴛᴀx ᴜɴʟᴏᴄᴋᴇᴅ");
                }
            }
            case 4 -> {
                if (paraso != null) {
                    refreshMaceLore(paraso);
                    sendQuestComplete(paraso, "ꜱᴛɪʟʟ ʜᴇʀᴇ?", "ᴘʀᴇꜱᴇɴᴄᴇ & ᴜɴᴀɴɴᴏᴜɴᴄᴇᴅ ᴜɴʟᴏᴄᴋᴇᴅ");
                }
            }
            case 5 -> {
                if (paraso != null) {
                    refreshMaceLore(paraso);
                    giveSword(paraso);
                    sendQuestComplete(paraso, "ᴛʜᴇ ᴍᴇɴᴀᴄᴇ", "ᴀʟʟ ᴀʙɪʟɪᴛɪᴇꜱ ᴜɴʟᴏᴄᴋᴇᴅ");
                }
                broadcastFinalTier();
            }
        }
    }

    // ── Fusion buffs ──────────────────────────────────────────────────────────

    /**
     * Applies +3 attack damage to The Unannounced in Paraso's inventory.
     * Uses AttributeModifier on the item — persists on the item itself.
     */
    private void applyFusionBuffs(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (!plugin.getMaceManager().isSoulboundMace(item)) continue;
            if (!"the_unannounced".equals(plugin.getMaceManager().getMaceType(item))) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            // Remove existing modifier first to avoid duplicates
            meta.getAttributeModifiers(Attribute.ATTACK_DAMAGE).forEach(mod -> {
                if (mod.getKey().getKey().equals(DAMAGE_KEY)) {
                    meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE, mod);
                }
            });

            AttributeModifier mod = new AttributeModifier(
                new NamespacedKey(plugin, DAMAGE_KEY),
                3.0,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
            );
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, mod);
            item.setItemMeta(meta);
            break;
        }
    }

    // ── Rewards ───────────────────────────────────────────────────────────────

    private void giveMace(Player player) {
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg == null) return;
        ItemStack mace = plugin.getMaceManager().createMace(cfg, player.getName());
        player.getInventory().addItem(mace);
    }

    private void giveSword(Player player) {
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_afterthought");
        if (cfg == null) return;
        ItemStack sword = plugin.getMaceManager().createMace(cfg, player.getName());
        player.getInventory().addItem(sword);
    }

    private void refreshMaceLore(Player player) {
        MaceManager mm = plugin.getMaceManager();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (!mm.isSoulboundMace(item)) continue;
            if (!"the_unannounced".equals(mm.getMaceType(item))) continue;
            MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
            if (cfg == null) continue;
            mm.refreshLore(item, cfg, player.getName());
        }
    }

    // ── Broadcast ─────────────────────────────────────────────────────────────

    private void broadcastFinalTier() {
        Component title = Component.text("The King has been reborn")
            .color(TextColor.fromHexString("#C8A96E"))
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false);

        Component subtitle = Component.text("An ")
            .color(TextColor.fromHexString("#F5F0E8"))
            .decoration(TextDecoration.ITALIC, false)
            .append(buildUnsettlingGradient())
            .append(Component.text(" aura fills the Three Dimensions.")
                .color(TextColor.fromHexString("#F5F0E8"))
                .decoration(TextDecoration.ITALIC, false));

        Title broadcastTitle = Title.title(title, subtitle,
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(1000)));

        for (Player p : Bukkit.getOnlinePlayers()) p.showTitle(broadcastTitle);
    }

    private Component buildUnsettlingGradient() {
        String word = "Unsettling";
        Component result = Component.empty();
        int[] start = {155, 0, 255};
        int[] end   = {255, 0, 0};
        for (int i = 0; i < word.length(); i++) {
            float t = (float) i / Math.max(word.length() - 1, 1);
            int r = (int)(start[0] + t * (end[0] - start[0]));
            int g = (int)(start[1] + t * (end[1] - start[1]));
            int b = (int)(start[2] + t * (end[2] - start[2]));
            result = result.append(Component.text(String.valueOf(word.charAt(i)))
                .color(TextColor.color(r, g, b))
                .decoration(TextDecoration.UNDERLINED, true)
                .decoration(TextDecoration.ITALIC, false));
        }
        return result;
    }

    private void sendQuestComplete(Player player, String title, String subtitle) {
        player.showTitle(Title.title(
            Component.text("✦ " + title + " ✦")
                .color(TextColor.fromHexString("#C8A96E"))
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(subtitle)
                .color(TextColor.fromHexString("#F5F0E8"))
                .decoration(TextDecoration.ITALIC, false),
            Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700))
        ));
    }

    private Player findParaso() {
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg == null) return null;
        return Bukkit.getPlayer(cfg.getHolderName());
    }

    public ParasoQuestData getData() { return data; }

    public Component buildTimerDisplay() {
        long total   = data.getHoldTimeSeconds();
        long minutes = total / 60;
        long seconds = total % 60;
        return Component.text(String.format("⏰ %02d:%02d / 20:00", minutes, seconds))
            .color(TextColor.fromHexString("#C8A96E"))
            .decoration(TextDecoration.ITALIC, false);
    }
}
