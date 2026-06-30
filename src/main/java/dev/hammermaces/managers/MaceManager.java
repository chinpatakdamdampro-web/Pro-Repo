package dev.hammermaces.managers;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class MaceManager {

    private final HammerMacesPlugin plugin;

    public static final String NS        = "custommaces";
    public static final String KEY_TYPE  = "type";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_BOUND = "bound";

    private static final Set<Material> SOULBOUND_MATERIALS = EnumSet.of(
        Material.MACE,
        Material.NETHERITE_SWORD,
        Material.DIAMOND_SWORD,
        Material.IRON_SWORD,
        Material.GOLDEN_SWORD,
        Material.STONE_SWORD,
        Material.WOODEN_SWORD
    );

    // ── Color palette constants ───────────────────────────────────────────────
    // Poseidon
    private static final String C_POSEIDON_DESC   = "#7ec8e3";
    private static final String C_POSEIDON_NAME   = "#e0f7ff";
    private static final String C_POSEIDON_PASS   = "#6699aa";
    private static final String C_POSEIDON_CD     = "#aaddff";
    private static final String C_POSEIDON_ACT    = "#88bbcc";
    private static final String C_POSEIDON_FLAVOR = "#8899aa";
    // Larpers
    private static final String C_LARPER_DESC     = "#ff9999";
    private static final String C_LARPER_NAME     = "#ffe0e0";
    private static final String C_LARPER_CD       = "#ffaaaa";
    private static final String C_LARPER_ACT      = "#cc8888";
    private static final String C_LARPER_FLAVOR   = "#aa7777";
    // Paraso
    private static final String C_PARASO_DESC     = "#d4b896";
    private static final String C_PARASO_NAME     = "#f5f0e8";
    private static final String C_PARASO_PASS     = "#8a7a6a";
    private static final String C_PARASO_CD       = "#c8a96e";
    private static final String C_PARASO_ACT      = "#a08060";
    private static final String C_PARASO_FLAVOR   = "#8a7a6a";
    // Architect
    private static final String C_ARCH_DESC       = "#a0b4c8";
    private static final String C_ARCH_NAME       = "#dce8f0";
    private static final String C_ARCH_PASS       = "#6a8090";
    private static final String C_ARCH_CD         = "#8aaabb";
    private static final String C_ARCH_ACT        = "#6a9aaa";
    private static final String C_ARCH_FLAVOR     = "#556677";

    private static final String HIDDEN = "???";

    public MaceManager(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public ItemStack createMace(MaceConfig cfg, String ownerName) {
        Material mat = resolveMaterial(cfg);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(GradientUtils.buildAnimatedGradient(
            cfg.getDisplayName(), cfg.getGradientStart(), cfg.getGradientEnd(), 0f));
        meta.lore(buildLore(cfg, ownerName));
        applyEnchantments(meta, cfg.getMaceId());
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        NamespacedKey typeKey  = new NamespacedKey(NS, KEY_TYPE);
        NamespacedKey ownerKey = new NamespacedKey(NS, KEY_OWNER);
        NamespacedKey boundKey = new NamespacedKey(NS, KEY_BOUND);
        meta.getPersistentDataContainer().set(typeKey,  PersistentDataType.STRING,  cfg.getMaceId());
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,  ownerName);
        meta.getPersistentDataContainer().set(boundKey, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    public void refreshLore(ItemStack item, MaceConfig cfg, String ownerName) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.lore(buildLore(cfg, ownerName));
        item.setItemMeta(meta);
    }

    // ── Lore routing ──────────────────────────────────────────────────────────

    private List<Component> buildLore(MaceConfig cfg, String ownerName) {
        return switch (cfg.getMaceId()) {
            case "hammer_of_poseidon"    -> poseidonLore(cfg, ownerName);
            case "hammer_of_the_larpers" -> larpersLore(cfg, ownerName);
            case "the_unannounced"       -> unannouncedLore(cfg, ownerName);
            case "the_afterthought"      -> afterthoughtLore(cfg, ownerName);
            case "the_first_draft"       -> firstDraftLore(cfg, ownerName);
            default                      -> genericLore(ownerName);
        };
    }

    // ── Poseidon lore ─────────────────────────────────────────────────────────

    private List<Component> poseidonLore(MaceConfig cfg, String ownerName) {
        List<Component> l = new ArrayList<>();
        l.add(flavor("𝘈 𝘳𝘦𝘭𝘪𝘤 𝘰𝘧 𝘵𝘩𝘦 𝘥𝘦𝘦𝘱 𝘴𝘦𝘢𝘴.", C_POSEIDON_FLAVOR));
        l.add(flavor("𝘉𝘰𝘶𝘯𝘥 𝘵𝘰 𝘪𝘵𝘴 𝘸𝘪𝘦𝘭𝘥𝘦𝘳 𝘧𝘰𝘳𝘦𝘷𝘦𝘳.", C_POSEIDON_FLAVOR));
        l.add(empty());

        l.add(ability("❄", "DIAMOND SKIN",  "✦ Passive", C_POSEIDON_NAME, C_POSEIDON_PASS));
        l.add(desc("Grants Resistance I while held", C_POSEIDON_DESC));
        l.add(empty());

        l.add(ability("🌊", "AQUATIC SOUL",  "✦ Passive", C_POSEIDON_NAME, C_POSEIDON_PASS));
        l.add(desc("Water breathing and swift swimming", C_POSEIDON_DESC));
        l.add(empty());

        l.add(ability("🧊", "COLD AURA", "⟨" + cfg.getColdAuraCooldown() + "s⟩", C_POSEIDON_NAME, C_POSEIDON_CD));
        l.add(desc("Freezes nearby enemies in a frost ring", C_POSEIDON_DESC));
        l.add(activation("Shift + Jump", C_POSEIDON_ACT));
        l.add(empty());

        l.add(ability("🌀", "TIDAL SURGE", "⟨" + cfg.getTidalSurgeCooldown() + "s⟩", C_POSEIDON_NAME, C_POSEIDON_CD));
        l.add(desc("Launches a wave knocking foes back", C_POSEIDON_DESC));
        l.add(activation("Shift + Swing", C_POSEIDON_ACT));
        l.add(empty());

        l.add(footer("⚓", "#1a1a6e"));
        l.add(owner(ownerName, "#00e5ff"));
        return l;
    }

    // ── Larpers lore ──────────────────────────────────────────────────────────

    private List<Component> larpersLore(MaceConfig cfg, String ownerName) {
        List<Component> l = new ArrayList<>();
        l.add(flavor("𝘛𝘩𝘦𝘺 𝘢𝘭𝘭 𝘴𝘵𝘰𝘱𝘱𝘦𝘥.", C_LARPER_FLAVOR));
        l.add(flavor("𝘕𝘰𝘵 𝘣𝘦𝘤𝘢𝘶𝘴𝘦 𝘵𝘩𝘦𝘺 𝘩𝘢𝘥 𝘵𝘰.", C_LARPER_FLAVOR));
        l.add(flavor("𝘉𝘦𝘤𝘢𝘶𝘴𝘦 𝘵𝘩𝘦𝘺 𝘤𝘰𝘶𝘭𝘥𝘯'𝘵 𝘮𝘰𝘷𝘦.", C_LARPER_FLAVOR));
        l.add(empty());

        l.add(ability("🧊", "LARPER FREEZE", "⟨" + cfg.getLarperFreezeCooldown() + "s⟩", C_LARPER_NAME, C_LARPER_CD));
        l.add(desc("Freezes nearby players and speaks", C_LARPER_DESC));
        l.add(activation("Shift + Jump", C_LARPER_ACT));
        l.add(empty());

        l.add(ability("🎭", "FALSE SUMMON", "⟨" + cfg.getFalseSummonCooldown() + "s⟩", C_LARPER_NAME, C_LARPER_CD));
        l.add(desc("Spawns a decoy and lets you reposition", C_LARPER_DESC));
        l.add(activation("Shift + Swing", C_LARPER_ACT));
        l.add(empty());

        l.add(ability("⚜", "MONOLOGUE", "⟨" + cfg.getMonologueCooldown() + "s⟩", C_LARPER_NAME, C_LARPER_CD));
        l.add(desc("Strikes a pose — strength and knockback immunity", C_LARPER_DESC));
        l.add(activation("Shift + Shift", C_LARPER_ACT));
        l.add(empty());

        l.add(footer("⚔", "#8B0000"));
        l.add(owner(ownerName, "#FF4444"));
        return l;
    }

    // ── The Unannounced lore ──────────────────────────────────────────────────

    private List<Component> unannouncedLore(MaceConfig cfg, String ownerName) {
        List<Component> l = new ArrayList<>();
        int tier = cfg.getQuestTier();

        l.add(flavor("𝘕𝘰 𝘢𝘯𝘯𝘰𝘶𝘯𝘤𝘦𝘮𝘦𝘯𝘵. 𝘕𝘰 𝘦𝘹𝘱𝘭𝘢𝘯𝘢𝘵𝘪𝘰𝘯.", C_PARASO_FLAVOR));
        l.add(flavor("𝘑𝘶𝘴𝘵 𝘴𝘰𝘮𝘦𝘰𝘯𝘦 𝘸𝘩𝘰 𝘴𝘩𝘰𝘸𝘦𝘥 𝘶𝘱.", C_PARASO_FLAVOR));
        l.add(empty());

        // Dash — tier 2
        if (tier >= 2) {
            l.add(ability("💨", "DASH", "⟨" + cfg.getDashCooldown() + "s⟩", C_PARASO_NAME, C_PARASO_CD));
            l.add(desc("Lunge in your looking direction", C_PARASO_DESC));
            l.add(activation("Shift", C_PARASO_ACT));
        } else {
            l.add(hidden());
        }
        l.add(empty());

        // Absent — tier 4
        if (tier >= 4) {
            l.add(ability("⚡", "ABSENT", "✦ Passive", C_PARASO_NAME, C_PARASO_PASS));
            l.add(desc("Grows stronger the longer you stay", C_PARASO_DESC));
        } else {
            l.add(hidden());
        }
        l.add(empty());

        // Chaos Tax — tier 3
        if (tier >= 3) {
            l.add(ability("🎲", "CHAOS TAX", "✦ Passive", C_PARASO_NAME, C_PARASO_PASS));
            l.add(desc("Every hit has a chance of something", C_PARASO_DESC));
        } else {
            l.add(hidden());
        }
        l.add(empty());

        // Unannounced — tier 4
        if (tier >= 4) {
            l.add(ability("👻", "UNANNOUNCED", "⟨" + cfg.getUnannouncedCooldown() + "s⟩", C_PARASO_NAME, C_PARASO_CD));
            l.add(desc("Freeze the nearest player and appear behind them", C_PARASO_DESC));
            l.add(activation("Shift + Jump", C_PARASO_ACT));
        } else {
            l.add(hidden());
        }
        l.add(empty());

        // Just Showed Up — tier 5
        if (tier >= 5) {
            l.add(ability("💥", "JUST SHOWED UP", "⟨" + cfg.getJustShowedUpCooldown() + "s⟩", C_PARASO_NAME, C_PARASO_CD));
            l.add(desc("Massive burst only available at max stacks", C_PARASO_DESC));
            l.add(activation("Shift + Swing", C_PARASO_ACT));
        } else {
            l.add(hidden());
        }
        l.add(empty());

        l.add(footer("⚠", "#C8A96E"));
        l.add(owner(ownerName, "#F5F0E8"));
        return l;
    }

    // ── The Afterthought lore ─────────────────────────────────────────────────

    private List<Component> afterthoughtLore(MaceConfig cfg, String ownerName) {
        List<Component> l = new ArrayList<>();
        l.add(flavor("𝘏𝘦 𝘥𝘪𝘥𝘯'𝘵 𝘱𝘭𝘢𝘯 𝘵𝘩𝘪𝘴 𝘱𝘢𝘳𝘵.", C_PARASO_FLAVOR));
        l.add(flavor("𝘐𝘵 𝘫𝘶𝘴𝘵 𝘩𝘢𝘱𝘱𝘦𝘯𝘦𝘥.", C_PARASO_FLAVOR));
        l.add(empty());

        l.add(ability("🤢", "NAUSEA", "✦ Passive", C_PARASO_NAME, C_PARASO_PASS));
        l.add(desc("Hits inflict Nausea I for 5 seconds", C_PARASO_DESC));
        l.add(empty());

        l.add(footer("⚠", "#C8A96E"));
        l.add(owner(ownerName, "#F5F0E8"));
        return l;
    }

    // ── The First Draft lore ──────────────────────────────────────────────────

    private List<Component> firstDraftLore(MaceConfig cfg, String ownerName) {
        List<Component> l = new ArrayList<>();
        l.add(flavor("𝘛𝘩𝘪𝘴 𝘸𝘢𝘴 𝘯𝘰𝘵 𝘣𝘶𝘪𝘭𝘵 𝘵𝘰 𝘧𝘪𝘨𝘩𝘵.", C_ARCH_FLAVOR));
        l.add(flavor("𝘐𝘵 𝘸𝘢𝘴 𝘣𝘶𝘪𝘭𝘵 𝘵𝘰 𝘦𝘯𝘴𝘶𝘳𝘦 𝘵𝘩𝘦", C_ARCH_FLAVOR));
        l.add(flavor("𝘰𝘶𝘵𝘤𝘰𝘮𝘦 𝘸𝘢𝘴 𝘯𝘦𝘷𝘦𝘳 𝘪𝘯 𝘥𝘰𝘶𝘣𝘵.", C_ARCH_FLAVOR));
        l.add(empty());

        l.add(ability("👁", "SILENT READ", "✦ Passive", C_ARCH_NAME, C_ARCH_PASS));
        l.add(desc("Senses nearby players with direction and health", C_ARCH_DESC));
        l.add(empty());

        l.add(ability("🧮", "CALCULATED", "✦ Passive", C_ARCH_NAME, C_ARCH_PASS));
        l.add(desc("Fracture stacks on hit — silent burst at 3", C_ARCH_DESC));
        l.add(empty());

        l.add(ability("🛡", "CONTINGENCY", "⟨120s⟩", C_ARCH_NAME, C_ARCH_CD));
        l.add(desc("Set a trigger — fires if hit within 15s", C_ARCH_DESC));
        l.add(activation("Shift + Jump", C_ARCH_ACT));
        l.add(empty());

        l.add(ability("🎯", "PREDETERMINED", "⟨45s⟩", C_ARCH_NAME, C_ARCH_CD));
        l.add(desc("Control the space — punishes entry", C_ARCH_DESC));
        l.add(activation("Shift + Swing", C_ARCH_ACT));
        l.add(empty());

        l.add(ability("💥", "GROUND SLAM", "✦ Passive", C_ARCH_NAME, C_ARCH_PASS));
        l.add(desc("Shockwave on mace landing after a fall", C_ARCH_DESC));
        l.add(empty());

        l.add(ability("🔮", "INFINITY SLOTS", "⟨Open⟩", C_ARCH_NAME, C_ARCH_CD));
        l.add(desc("Slot items to augment this mace", C_ARCH_DESC));
        l.add(activation("Shift + Right Click", C_ARCH_ACT));
        l.add(empty());

        l.add(footer("✦", "#4A5568"));
        l.add(owner(ownerName, "#CBD5E0"));
        return l;
    }

    private List<Component> genericLore(String ownerName) {
        List<Component> l = new ArrayList<>();
        l.add(footer("✦", "#AAAAAA"));
        l.add(owner(ownerName, "#FFFFFF"));
        return l;
    }

    // ── Lore component helpers ────────────────────────────────────────────────

    private Component flavor(String text, String hex) {
        return Component.text(text)
            .color(hex(hex))
            .decoration(TextDecoration.ITALIC, true);
    }

    private Component ability(String icon, String name, String tag, String nameHex, String tagHex) {
        return Component.text(icon + " ")
            .color(hex(nameHex))
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(name)
                .color(hex(nameHex))
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false))
            .append(Component.text("  " + tag)
                .color(hex(tagHex))
                .decoration(TextDecoration.ITALIC, false));
    }

    private Component desc(String text, String hex) {
        return Component.text("  " + text)
            .color(hex(hex))
            .decoration(TextDecoration.ITALIC, false);
    }

    private Component activation(String keys, String hex) {
        return Component.text("  Activation: ")
            .color(hex("#444455"))
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(keys)
                .color(hex(hex))
                .decoration(TextDecoration.ITALIC, false));
    }

    private Component hidden() {
        return Component.text("  " + HIDDEN)
            .color(hex("#333333"))
            .decoration(TextDecoration.ITALIC, false);
    }

    private Component footer(String icon, String hex) {
        return Component.text(icon + " ꜱᴏᴜʟʙᴏᴜɴᴅ")
            .color(hex(hex))
            .decoration(TextDecoration.ITALIC, false);
    }

    private Component owner(String name, String hex) {
        return Component.text("  Bound to: ")
            .color(hex("#445566"))
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(name)
                .color(hex(hex))
                .decoration(TextDecoration.ITALIC, false));
    }

    private Component empty() { return Component.empty(); }

    private TextColor hex(String h) { return TextColor.fromHexString(h); }

    // ── Enchantments ──────────────────────────────────────────────────────────

    private void applyEnchantments(ItemMeta meta, String maceId) {
        File f = new File(plugin.getDataFolder(), "maces/" + maceId + ".yml");
        if (!f.exists()) return;
        ConfigurationSection section = YamlConfiguration.loadConfiguration(f)
            .getConfigurationSection("enchantments");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                Enchantment ench = Registry.ENCHANTMENT.get(
                    new NamespacedKey("minecraft", key.toLowerCase()));
                if (ench != null) meta.addEnchant(ench, section.getInt(key), true);
            } catch (Exception ignored) {}
        }
    }

    // ── Material resolution ───────────────────────────────────────────────────

    public Material resolveMaterial(MaceConfig cfg) {
        return switch (cfg.getItemType()) {
            case "SWORD" -> Material.NETHERITE_SWORD;
            default      -> Material.MACE;
        };
    }

    // ── PDC helpers ───────────────────────────────────────────────────────────

    public boolean isSoulboundMace(ItemStack item) {
        if (item == null) return false;
        if (!SOULBOUND_MATERIALS.contains(item.getType())) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer()
            .has(new NamespacedKey(NS, KEY_BOUND), PersistentDataType.BOOLEAN);
    }

    public String getMaceOwner(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer()
            .get(new NamespacedKey(NS, KEY_OWNER), PersistentDataType.STRING);
    }

    public String getMaceType(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer()
            .get(new NamespacedKey(NS, KEY_TYPE), PersistentDataType.STRING);
    }

    public boolean isOwner(ItemStack item, String playerName) {
        String owner = getMaceOwner(item);
        return owner != null && owner.equalsIgnoreCase(playerName);
    }
}
