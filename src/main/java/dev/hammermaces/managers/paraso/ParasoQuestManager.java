package dev.hammermaces.managers.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.data.ParasoQuestData;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Manages Paraso's quest progression.
 *
 * Tier 1 — Visit 5 biomes           → Mace given, all abilities locked
 * Tier 2 — Login 3 separate days    → Dash unlocks
 * Tier 3 — Hit 2 different players  → Chaos Tax unlocks
 * Tier 4 — Hold mace 20 minutes     → Presence + Unannounced unlock
 * Tier 5 — Fire Just Showed Up      → Everything unlocks + sword + broadcast
 */
public class ParasoQuestManager {

    private final HammerMacesPlugin plugin;
    private final ParasoQuestData data;

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Quest thresholds
    public static final int BIOMES_REQUIRED    = 5;
    public static final int LOGIN_DAYS_REQUIRED = 3;
    public static final int PLAYERS_HIT_REQUIRED = 2;
    public static final long HOLD_TIME_REQUIRED = 1200L; // 20 minutes in seconds

    public ParasoQuestManager(HammerMacesPlugin plugin, ParasoQuestData data) {
        this.plugin = plugin;
        this.data   = data;
    }

    // ── Called from listeners ─────────────────────────────────────────────────

    public void onBiomeEnter(String biomeName) {
        if (data.getCurrentTier() >= 1) return;
        data.addBiome(biomeName);
        data.save();
        checkTier1();
    }

    public void onLogin() {
        String today = LocalDate.now().format(DAY_FORMAT);
        boolean added = data.getLoginDays().add(today);
        if (added) {
            data.save();
            checkTier2();
        }
    }

    public void onPlayerHit(String targetName) {
        if (data.getCurrentTier() < 1 || data.getCurrentTier() >= 3) return;
        data.addPlayerHit(targetName);
        data.save();
        checkTier3();
    }

    public void onHoldTimeTick(long addedSeconds) {
        if (data.getCurrentTier() < 3 || data.getCurrentTier() >= 4) return;
        data.addHoldTime(addedSeconds);
        // Save every 60 seconds of accumulated time
        if (data.getHoldTimeSeconds() % 60 == 0) data.save();
        checkTier4();
    }

    public void onJustShowedUpFired() {
        if (data.getCurrentTier() < 4 || data.isJustShowedUpFired()) return;
        data.setJustShowedUpFired(true);
        data.save();
        checkTier5();
    }

    // ── Tier checks ───────────────────────────────────────────────────────────

    private void checkTier1() {
        if (data.getCurrentTier() >= 1) return;
        if (data.getBiomesVisited().size() >= BIOMES_REQUIRED) {
            advanceTier(1);
        }
    }

    private void checkTier2() {
        if (data.getCurrentTier() >= 2 || data.getCurrentTier() < 1) return;
        if (data.getLoginDays().size() >= LOGIN_DAYS_REQUIRED) {
            advanceTier(2);
        }
    }

    private void checkTier3() {
        if (data.getCurrentTier() >= 3 || data.getCurrentTier() < 2) return;
        if (data.getPlayersHit().size() >= PLAYERS_HIT_REQUIRED) {
            advanceTier(3);
        }
    }

    private void checkTier4() {
        if (data.getCurrentTier() >= 4 || data.getCurrentTier() < 3) return;
        if (data.getHoldTimeSeconds() >= HOLD_TIME_REQUIRED) {
            advanceTier(4);
        }
    }

    private void checkTier5() {
        if (data.getCurrentTier() >= 5 || data.getCurrentTier() < 4) return;
        advanceTier(5);
    }

    // ── Tier advancement ──────────────────────────────────────────────────────

    private void advanceTier(int tier) {
        data.setCurrentTier(tier);
        data.save();

        // Update mace config tier in memory and persist to yml
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg != null) {
            cfg.setQuestTier(tier);
            plugin.getMaceConfigManager().saveQuestTier("the_unannounced", tier);
        }

        // Find Paraso online and update his mace lore
        Player paraso = findParaso();

        switch (tier) {
            case 1 -> {
                // Give Paraso the mace for the first time
                if (paraso != null) {
                    giveMace(paraso);
                    sendQuestComplete(paraso, "ᴘᴀʀᴀꜱᴏ ᴍᴜꜱᴛ ᴇxᴘʟᴏʀᴇ!", "ᴛʜᴇ ᴜɴᴀɴɴᴏᴜɴᴄᴇᴅ ʜᴀs ᴄʜᴏsᴇɴ ʏᴏᴜ");
                }
            }
            case 2 -> {
                if (paraso != null) {
                    refreshMaceLore(paraso);
                    sendQuestComplete(paraso, "ʜᴇ's ʙᴀᴄᴋ", "ᴅᴀsʜ ᴜɴʟᴏᴄᴋᴇᴅ");
                }
            }
            case 3 -> {
                if (paraso != null) {
                    refreshMaceLore(paraso);
                    sendQuestComplete(paraso, "ᴛʜᴇ ɴᴜɪsᴀɴᴄᴇ", "ᴄʜᴀᴏs ᴛᴀx ᴜɴʟᴏᴄᴋᴇᴅ");
                }
            }
            case 4 -> {
                if (paraso != null) {
                    refreshMaceLore(paraso);
                    sendQuestComplete(paraso, "sᴛɪʟʟ ʜᴇʀᴇ?", "ᴘʀᴇsᴇɴᴄᴇ & ᴜɴᴀɴɴᴏᴜɴᴄᴇᴅ ᴜɴʟᴏᴄᴋᴇᴅ");
                }
            }
            case 5 -> {
                if (paraso != null) {
                    refreshMaceLore(paraso);
                    giveSword(paraso);
                    sendQuestComplete(paraso, "ᴛʜᴇ ᴍᴇɴᴀᴄᴇ", "ᴀʟʟ ᴀʙɪʟɪᴛɪᴇs ᴜɴʟᴏᴄᴋᴇᴅ");
                }
                broadcastFinalTier();
            }
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
            plugin.getMaceManager().refreshLore(item, cfg, player.getName());
        }
    }

    // ── Broadcast ─────────────────────────────────────────────────────────────

    private void broadcastFinalTier() {
        // Title line: "The King has been reborn" in gold
        Component title = Component.text("The King has been reborn")
            .color(TextColor.fromHexString("#C8A96E"))
            .decoration(TextDecoration.BOLD, true)
            .decoration(TextDecoration.ITALIC, false);

        // Subtitle: "An " + gradient "Unsettling" underlined + " aura fills the Three Dimensions."
        Component subtitle = Component.text("An ")
            .color(TextColor.fromHexString("#F5F0E8"))
            .decoration(TextDecoration.ITALIC, false)
            .append(buildUnsettlingGradient())
            .append(
                Component.text(" aura fills the Three Dimensions.")
                    .color(TextColor.fromHexString("#F5F0E8"))
                    .decoration(TextDecoration.ITALIC, false)
            );

        Title broadcastTitle = Title.title(
            title,
            subtitle,
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(4),
                Duration.ofMillis(1000)
            )
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(broadcastTitle);
        }
    }

    /**
     * Builds "Unsettling" with purple→red gradient and underline.
     */
    private Component buildUnsettlingGradient() {
        String word = "Unsettling";
        Component result = Component.empty();
        int len = word.length();

        // Purple #9B00FF → Red #FF0000
        int[] startRGB = {155, 0, 255};
        int[] endRGB   = {255, 0, 0};

        for (int i = 0; i < len; i++) {
            float t = (float) i / Math.max(len - 1, 1);
            int r = (int) (startRGB[0] + t * (endRGB[0] - startRGB[0]));
            int g = (int) (startRGB[1] + t * (endRGB[1] - startRGB[1]));
            int b = (int) (startRGB[2] + t * (endRGB[2] - startRGB[2]));

            result = result.append(
                Component.text(String.valueOf(word.charAt(i)))
                    .color(TextColor.color(r, g, b))
                    .decoration(TextDecoration.UNDERLINED, true)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, false)
            );
        }
        return result;
    }

    // ── Quest complete notification ────────────────────────────────────────────

    private void sendQuestComplete(Player player, String questName, String reward) {
        Title t = Title.title(
            Component.text("✦ " + questName + " ✦")
                .color(TextColor.fromHexString("#C8A96E"))
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false),
            Component.text(reward)
                .color(TextColor.fromHexString("#F5F0E8"))
                .decoration(TextDecoration.ITALIC, false),
            Title.Times.times(
                Duration.ofMillis(300),
                Duration.ofSeconds(3),
                Duration.ofMillis(700)
            )
        );
        player.showTitle(t);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Player findParaso() {
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_unannounced");
        if (cfg == null) return null;
        return Bukkit.getPlayer(cfg.getHolderName());
    }

    public ParasoQuestData getData() { return data; }

    /**
     * Builds the action bar timer display for quest 4.
     * Format: ⏰ MM:SS / 20:00
     */
    public Component buildTimerDisplay() {
        long total = data.getHoldTimeSeconds();
        long minutes = total / 60;
        long seconds = total % 60;
        String current = String.format("%02d:%02d", minutes, seconds);

        return Component.text("⏰ " + current + " / 20:00")
            .color(TextColor.fromHexString("#C8A96E"))
            .decoration(TextDecoration.ITALIC, false);
    }
}
