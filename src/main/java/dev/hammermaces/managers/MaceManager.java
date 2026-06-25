package dev.hammermaces.managers;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and identifies custom soulbound mace ItemStacks.
 *
 * PDC keys stored on each item:
 *   custommaces:type    → maceId string
 *   custommaces:owner   → ownerName string (IGN)
 *   custommaces:bound   → true boolean
 */
public class MaceManager {

    private final HammerMacesPlugin plugin;

    public static final String NS = "custommaces";
    public static final String KEY_TYPE  = "type";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_BOUND = "bound";

    // Superscript unicode for tiny lore descriptions
    // These render tiny and clean in Minecraft on Java & Bedrock/Geyser
    private static final String LORE_DIAMOND_SKIN =
        "ˢᵗᵒⁿᵉ ˢᵏⁱⁿ ᵍʳᵃⁿᵗˢ ʸᵒᵘ ᴿᵉˢⁱˢᵗᵃⁿᶜᵉ ᴵ ʷʰⁱˡᵉ ʰᵉˡᵈ";
    private static final String LORE_AQUATIC_SOUL =
        "ᴮʳᵉᵃᵗʰᵉ ᵘⁿᵈᵉʳ ʷᵃᵗᵉʳ ᵃⁿᵈ ˢʷⁱᵐ ˢʷⁱᶠᵗˡʸ";
    private static final String LORE_COLD_AURA =
        "ᶠʳᵉᵉᶻᵉˢ ⁿᵉᵃʳᵇʸ ᵉⁿᵉᵐⁱᵉˢ ⁱⁿ ᵃ ᶠʳᵒˢᵗ ʳⁱⁿᵍ";
    private static final String LORE_TIDAL_SURGE =
        "ᴸᵃᵘⁿᶜʰᵉˢ ᵃ ʷᵃᵛᵉ ᵏⁿᵒᶜᵏⁱⁿᵍ ᶠᵒᵉˢ ᵇᵃᶜᵏ";

    public MaceManager(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a fully-formed soulbound mace ItemStack for the given mace config and owner.
     */
    public ItemStack createMace(MaceConfig cfg, String ownerName) {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Animated name (offset 0 on creation — animation task updates it each tick)
        meta.displayName(
            GradientUtils.buildAnimatedGradient(cfg.getDisplayName(), cfg.getGradientStart(), cfg.getGradientEnd(), 0f)
        );

        // Build the lore
        meta.lore(buildLore(cfg, ownerName));

        // Enchantments from the mace's config section
        applyEnchantments(meta, cfg.getMaceId());

        // Cosmetics
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        // PDC tags — these are how we identify & validate ownership
        NamespacedKey typeKey  = new NamespacedKey(NS, KEY_TYPE);
        NamespacedKey ownerKey = new NamespacedKey(NS, KEY_OWNER);
        NamespacedKey boundKey = new NamespacedKey(NS, KEY_BOUND);

        meta.getPersistentDataContainer().set(typeKey,  PersistentDataType.STRING,  cfg.getMaceId());
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,  ownerName);
        meta.getPersistentDataContainer().set(boundKey, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the lore component list with:
     *   - Flavour text in italic unicode
     *   - Bold ability names + tiny superscript descriptions
     *   - ⚓ SOULBOUND footer
     *   - Bound to: player name
     */
    private List<Component> buildLore(MaceConfig cfg, String ownerName) {
        List<Component> lore = new ArrayList<>();
        TextColor descColor = TextColor.fromHexString("#7ec8e3");  // soft cyan — readable, classy
        TextColor abilityNameColor = TextColor.fromHexString("#e0f7ff"); // near-white ice blue for ability names
        TextColor passiveTagColor  = TextColor.fromHexString("#6699aa"); // muted teal for "✦ Passive"
        TextColor cooldownColor    = TextColor.fromHexString("#aaddff"); // light blue for cooldown tags

        // Flavour lines
        lore.add(italicLine("𝘈 𝘳𝘦𝘭𝘪𝘤 𝘰𝘧 𝘵𝘩𝘦 𝘥𝘦𝘦𝘱 𝘴𝘦𝘢𝘴.", "#8899aa"));
        lore.add(italicLine("𝘉𝘰𝘶𝘯𝘥 𝘵𝘰 𝘪𝘵𝘴 𝘸𝘪𝘦𝘭𝘥𝘦𝘳 𝘧𝘰𝘳𝘦𝘷𝘦𝘳.", "#8899aa"));
        lore.add(Component.empty());

        // Diamond Skin
        lore.add(abilityLine("𝗗𝗜𝗔𝗠𝗢𝗡𝗗 𝗦𝗞𝗜𝗡", "✦ Passive", abilityNameColor, passiveTagColor));
        lore.add(descLine(LORE_DIAMOND_SKIN, descColor));
        lore.add(Component.empty());

        // Aquatic Soul
        lore.add(abilityLine("𝗔𝗤𝗨𝗔𝗧𝗜𝗖 𝗦𝗢𝗨𝗟", "✦ Passive", abilityNameColor, passiveTagColor));
        lore.add(descLine(LORE_AQUATIC_SOUL, descColor));
        lore.add(Component.empty());

        // Cold Aura
        String coldCd = "⟨" + cfg.getColdAuraCooldown() + "s⟩";
        lore.add(abilityLine("𝗖𝗢𝗟𝗗 𝗔𝗨𝗥𝗔", coldCd, abilityNameColor, cooldownColor));
        lore.add(descLine(LORE_COLD_AURA, descColor));
        lore.add(Component.empty());

        // Tidal Surge
        String tidalCd = "⟨" + cfg.getTidalSurgeCooldown() + "s⟩";
        lore.add(abilityLine("𝗧𝗜𝗗𝗔𝗟 𝗦𝗨𝗥𝗚𝗘", tidalCd, abilityNameColor, cooldownColor));
        lore.add(descLine(LORE_TIDAL_SURGE, descColor));
        lore.add(Component.empty());

        // Soulbound footer
        lore.add(
            Component.text("⚓ ꜱᴏᴜʟʙᴏᴜɴᴅ")
                .color(TextColor.fromHexString("#1a1a6e"))
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, false)
        );

        // Owner line
        lore.add(
            Component.text("  ᴮᵒᵘⁿᵈ ᵗᵒ: ")
                .color(TextColor.fromHexString("#445566"))
                .decoration(TextDecoration.ITALIC, false)
                .append(
                    Component.text(ownerName)
                        .color(TextColor.fromHexString("#00e5ff"))
                        .decoration(TextDecoration.ITALIC, false)
                )
        );

        return lore;
    }

    /** Italic flavour text line */
    private Component italicLine(String text, String hexColor) {
        return Component.text(text)
            .color(TextColor.fromHexString(hexColor))
            .decoration(TextDecoration.ITALIC, true);
    }

    /** Bold ability name + right-side tag on the same line */
    private Component abilityLine(String name, String tag, TextColor nameColor, TextColor tagColor) {
        return Component.text(name)
            .color(nameColor)
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, false)
            .append(Component.text("  " + tag).color(tagColor).decoration(TextDecoration.ITALIC, false));
    }

    /** Small superscript description line, indented slightly */
    private Component descLine(String text, TextColor color) {
        return Component.text(" " + text)
            .color(color)
            .decoration(TextDecoration.ITALIC, false);
    }

    private void applyEnchantments(ItemMeta meta, String maceId) {
        ConfigurationSection enchSection = plugin.getMaceConfigManager()
            .getMaceConfig(maceId) == null ? null :
            YamlSection(maceId);

        if (enchSection == null) return;

        for (String key : enchSection.getKeys(false)) {
            try {
                Enchantment ench = Enchantment.getByKey(
                    new NamespacedKey("minecraft", key.toLowerCase())
                );
                if (ench != null) {
                    meta.addEnchant(ench, enchSection.getInt(key), true);
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Grabs the enchantment section from the raw YAML file for this mace.
     * We re-read it here because MaceConfig doesn't store the raw section.
     */
    private ConfigurationSection YamlSection(String maceId) {
        java.io.File f = new java.io.File(plugin.getDataFolder(), "maces/" + maceId + ".yml");
        if (!f.exists()) return null;
        org.bukkit.configuration.file.FileConfiguration raw =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        return raw.getConfigurationSection("enchantments");
    }

    // ── PDC helpers ────────────────────────────────────────────────────────────

    public boolean isSoulboundMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) return false;
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
