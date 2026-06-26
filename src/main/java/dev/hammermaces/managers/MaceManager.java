package dev.hammermaces.managers;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.utils.GradientUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

/**
 * Creates, identifies, and updates soulbound items.
 *
 * PDC keys:
 *   custommaces:type  → maceId string
 *   custommaces:owner → ownerName (IGN)
 *   custommaces:bound → true
 */
public class MaceManager {

    private final HammerMacesPlugin plugin;

    public static final String NS        = "custommaces";
    public static final String KEY_TYPE  = "type";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_BOUND = "bound";

    /** All item types that can be soulbound */
    private static final Set<Material> SOULBOUND_MATERIALS = EnumSet.of(
        Material.MACE,
        Material.NETHERITE_SWORD,
        Material.DIAMOND_SWORD,
        Material.IRON_SWORD,
        Material.GOLDEN_SWORD,
        Material.STONE_SWORD,
        Material.WOODEN_SWORD
    );

    // ── Lore text constants ───────────────────────────────────────────────────

    // Poseidon
    private static final String DESC_DIAMOND_SKIN  = "ᵍʳᵃⁿᵗˢ ᴿᵉˢⁱˢᵗᵃⁿᶜᵉ ᴵ ʷʰⁱˡᵉ ʰᵉˡᵈ";
    private static final String DESC_AQUATIC_SOUL  = "ʷᵃᵗᵉʳ ᵇʳᵉᵃᵗʰⁱⁿᵍ ᵃⁿᵈ ˢʷⁱᶠᵗ ˢʷⁱᵐᵐⁱⁿᵍ ʷʰⁱˡᵉ ʰᵉˡᵈ";
    private static final String DESC_COLD_AURA     = "ᶠʳᵉᵉᶻᵉˢ ⁿᵉᵃʳᵇʸ ᵉⁿᵉᵐⁱᵉˢ ⁱⁿ ᵃ ᶠʳᵒˢᵗ ʳⁱⁿᵍ";
    private static final String ACT_COLD_AURA      = "ˢʰⁱᶠᵗ ⁺ ᴶᵘᵐᵖ";
    private static final String DESC_TIDAL_SURGE   = "ˡᵃᵘⁿᶜʰᵉˢ ᵃ ʷᵃᵛᵉ ᵏⁿᵒᶜᵏⁱⁿᵍ ᶠᵒᵉˢ ᵇᵃᶜᵏ";
    private static final String ACT_TIDAL_SURGE    = "ˢʰⁱᶠᵗ ⁺ ˢʷⁱⁿᵍ";

    // Larpers
    private static final String DESC_LARPER_FREEZE = "ᶠʳᵉᵉᶻᵉˢ ⁿᵉᵃʳᵇʸ ᵖˡᵃʸᵉʳˢ ᵃⁿᵈ ˢᵖᵉᵃᵏˢ";
    private static final String ACT_LARPER_FREEZE  = "ˢʰⁱᶠᵗ ⁺ ᴶᵘᵐᵖ";

    // Paraso
    private static final String DESC_DASH          = "ˡᵘⁿᵍᵉˢ ⁱⁿ ʸᵒᵘʳ ˡᵒᵒᵏⁱⁿᵍ ᵈⁱʳᵉᶜᵗⁱᵒⁿ";
    private static final String ACT_DASH           = "ˢʰⁱᶠᵗ";
    private static final String DESC_ABSENT        = "ˢˡᵒʷˡʸ ᵍʳᵒʷˢ ˢᵗʳᵒⁿᵍᵉʳ ᵃˢ ʸᵒᵘ ˢᵗᵃʸ ˡᵒⁿᵍᵉʳ";
    private static final String DESC_CHAOS_TAX     = "ᵉᵛᵉʳʸ ʰⁱᵗ ʰᵃˢ ᵃ ᶜʰᵃⁿᶜᵉ ᵒᶠ ˢᵒᵐᵉᵗʰⁱⁿᵍ";
    private static final String DESC_UNANNOUNCED   = "ᵈⁱˢᵃᵖᵖᵉᵃʳ ᵃⁿᵈ ʳᵉᵃᵖᵖᵉᵃʳ ˢᵒᵐᵉʷʰᵉʳᵉ ⁿᵉᵃʳᵇʸ";
    private static final String ACT_UNANNOUNCED    = "ˢʰⁱᶠᵗ ⁺ ᴶᵘᵐᵖ";
    private static final String DESC_JUST_SHOWED   = "ᵐᵃˢˢⁱᵛᵉ ᵇᵘʳˢᵗ ᵃᵗ ᵐᵃˣ ˢᵗᵃᶜᵏˢ";
    private static final String ACT_JUST_SHOWED    = "ˢʰⁱᶠᵗ ⁺ ˢʷⁱⁿᵍ";

    // Sword
    private static final String DESC_NAUSEA        = "ʰⁱᵗˢ ⁱⁿᶠˡⁱᶜᵗ ᴺᵃᵘˢᵉᵃ ᴵ ᶠᵒʳ ⁵ˢ";

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

        meta.displayName(
            GradientUtils.buildAnimatedGradient(
                cfg.getDisplayName(), cfg.getGradientStart(), cfg.getGradientEnd(), 0f)
        );

        meta.lore(buildLore(cfg, ownerName));
        applyEnchantments(meta, cfg.getMaceId());

        meta.setUnbreakable(true);
        meta.addItemFlags(
            ItemFlag.HIDE_UNBREAKABLE,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_ATTRIBUTES
        );

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
     * Updates the lore on an existing soulbound item in-place (e.g. after quest tier change).
     */
    public void refreshLore(ItemStack item, MaceConfig cfg, String ownerName) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.lore(buildLore(cfg, ownerName));
        item.setItemMeta(meta);
    }

    // ── Lore building ─────────────────────────────────────────────────────────

    private List<Component> buildLore(MaceConfig cfg, String ownerName) {
        return switch (cfg.getMaceId()) {
            case "hammer_of_poseidon"      -> buildPoseidonLore(cfg, ownerName);
            case "hammer_of_the_larpers"   -> buildLarpersLore(cfg, ownerName);
            case "the_unannounced"         -> buildUnannouncedLore(cfg, ownerName);
            case "the_afterthought"        -> buildAfterthoughtLore(cfg, ownerName);
            default                        -> buildGenericLore(cfg, ownerName);
        };
    }

    private List<Component> buildPoseidonLore(MaceConfig cfg, String ownerName) {
        List<Component> lore = new ArrayList<>();
        TextColor desc   = hex("#7ec8e3");
        TextColor name   = hex("#e0f7ff");
        TextColor passive = hex("#6699aa");
        TextColor cd     = hex("#aaddff");
        TextColor act    = hex("#88bbcc");

        lore.add(italic("𝘈 𝘳𝘦𝘭𝘪𝘤 𝘰𝘧 𝘵𝘩𝘦 𝘥𝘦𝘦𝘱 𝘴𝘦𝘢𝘴.", "#8899aa"));
        lore.add(italic("𝘉𝘰𝘶𝘯𝘥 𝘵𝘰 𝘪𝘵𝘴 𝘸𝘪𝘦𝘭𝘥𝘦𝘳 𝘧𝘰𝘳𝘦𝘷𝘦𝘳.", "#8899aa"));
        lore.add(Component.empty());

        lore.add(abilityLine("𝗗𝗜𝗔𝗠𝗢𝗡𝗗 𝗦𝗞𝗜𝗡", "✦ Passive", name, passive));
        lore.add(desc(DESC_DIAMOND_SKIN, desc));
        lore.add(Component.empty());

        lore.add(abilityLine("𝗔𝗤𝗨𝗔𝗧𝗜𝗖 𝗦𝗢𝗨𝗟", "✦ Passive", name, passive));
        lore.add(desc(DESC_AQUATIC_SOUL, desc));
        lore.add(Component.empty());

        lore.add(abilityLine("𝗖𝗢𝗟𝗗 𝗔𝗨𝗥𝗔", "⟨" + cfg.getColdAuraCooldown() + "s⟩", name, cd));
        lore.add(desc(DESC_COLD_AURA, desc));
        lore.add(activation(ACT_COLD_AURA, act));
        lore.add(Component.empty());

        lore.add(abilityLine("𝗧𝗜𝗗𝗔𝗟 𝗦𝗨𝗥𝗚𝗘", "⟨" + cfg.getTidalSurgeCooldown() + "s⟩", name, cd));
        lore.add(desc(DESC_TIDAL_SURGE, desc));
        lore.add(activation(ACT_TIDAL_SURGE, act));
        lore.add(Component.empty());

        lore.add(soulboundFooter("⚓", "#1a1a6e"));
        lore.add(ownerLine(ownerName, "#00e5ff"));
        return lore;
    }

    private List<Component> buildLarpersLore(MaceConfig cfg, String ownerName) {
        List<Component> lore = new ArrayList<>();
        TextColor desc   = hex("#ff9999");
        TextColor name   = hex("#ffe0e0");
        TextColor cd     = hex("#ffaaaa");
        TextColor act    = hex("#cc8888");

        lore.add(italic("𝘛𝘩𝘦𝘺 𝘢𝘭𝘭 𝘴𝘵𝘰𝘱𝘱𝘦𝘥.", "#aa7777"));
        lore.add(italic("𝘕𝘰𝘵 𝘣𝘦𝘤𝘢𝘶𝘴𝘦 𝘵𝘩𝘦𝘺 𝘩𝘢𝘥 𝘵𝘰.", "#aa7777"));
        lore.add(italic("𝘉𝘦𝘤𝘢𝘶𝘴𝘦 𝘵𝘩𝘦𝘺 𝘤𝘰𝘶𝘭𝘥𝘯'𝘵 𝘮𝘰𝘷𝘦.", "#aa7777"));
        lore.add(Component.empty());

        lore.add(abilityLine("𝗟𝗔𝗥𝗣𝗘𝗥 𝗙𝗥𝗘𝗘𝗭𝗘", "⟨" + cfg.getLarperFreezeCooldown() + "s⟩", name, cd));
        lore.add(desc(DESC_LARPER_FREEZE, desc));
        lore.add(activation(ACT_LARPER_FREEZE, act));
        lore.add(Component.empty());

        lore.add(soulboundFooter("⚔", "#8B0000"));
        lore.add(ownerLine(ownerName, "#FF4444"));
        return lore;
    }

    private List<Component> buildUnannouncedLore(MaceConfig cfg, String ownerName) {
        List<Component> lore = new ArrayList<>();
        int tier = cfg.getQuestTier();

        TextColor desc   = hex("#d4b896");
        TextColor name   = hex("#f5f0e8");
        TextColor passive = hex("#8a7a6a");
        TextColor cd     = hex("#c8a96e");
        TextColor act    = hex("#a08060");

        lore.add(italic("𝘕𝘰 𝘢𝘯𝘯𝘰𝘶𝘯𝘤𝘦𝘮𝘦𝘯𝘵. 𝘕𝘰 𝘦𝘹𝘱𝘭𝘢𝘯𝘢𝘵𝘪𝘰𝘯.", "#8a7a6a"));
        lore.add(italic("𝘑𝘶𝘴𝘵 𝘴𝘰𝘮𝘦𝘰𝘯𝘦 𝘸𝘩𝘰 𝘴𝘩𝘰𝘸𝘦𝘥 𝘶𝘱.", "#8a7a6a"));
        lore.add(Component.empty());

        // Dash — unlocks tier 2
        if (tier >= 2) {
            lore.add(abilityLine("𝗗𝗔𝗦𝗛", "⟨" + cfg.getDashCooldown() + "s⟩", name, cd));
            lore.add(desc(DESC_DASH, desc));
            lore.add(activation(ACT_DASH, act));
        } else {
            lore.add(hiddenLine());
        }
        lore.add(Component.empty());

        // Absent — unlocks tier 4
        if (tier >= 4) {
            lore.add(abilityLine("𝗔𝗕𝗦𝗘𝗡𝗧", "✦ Passive", name, passive));
            lore.add(desc(DESC_ABSENT, desc));
        } else {
            lore.add(hiddenLine());
        }
        lore.add(Component.empty());

        // Chaos Tax — unlocks tier 3
        if (tier >= 3) {
            lore.add(abilityLine("𝗖𝗛𝗔𝗢𝗦 𝗧𝗔𝗫", "✦ Passive", name, passive));
            lore.add(desc(DESC_CHAOS_TAX, desc));
        } else {
            lore.add(hiddenLine());
        }
        lore.add(Component.empty());

        // Unannounced — unlocks tier 4
        if (tier >= 4) {
            lore.add(abilityLine("𝗨𝗡𝗔𝗡𝗡𝗢𝗨𝗡𝗖𝗘𝗗", "⟨" + cfg.getUnannouncedCooldown() + "s⟩", name, cd));
            lore.add(desc(DESC_UNANNOUNCED, desc));
            lore.add(activation(ACT_UNANNOUNCED, act));
        } else {
            lore.add(hiddenLine());
        }
        lore.add(Component.empty());

        // Just Showed Up — unlocks tier 5
        if (tier >= 5) {
            lore.add(abilityLine("𝗝𝗨𝗦𝗧 𝗦𝗛𝗢𝗪𝗘𝗗 𝗨𝗣", "⟨" + cfg.getJustShowedUpCooldown() + "s⟩", name, cd));
            lore.add(desc(DESC_JUST_SHOWED, desc));
            lore.add(activation(ACT_JUST_SHOWED, act));
        } else {
            lore.add(hiddenLine());
        }
        lore.add(Component.empty());

        lore.add(soulboundFooter("⚠", "#C8A96E"));
        lore.add(ownerLine(ownerName, "#F5F0E8"));
        return lore;
    }

    private List<Component> buildAfterthoughtLore(MaceConfig cfg, String ownerName) {
        List<Component> lore = new ArrayList<>();
        TextColor desc   = hex("#d4b896");
        TextColor name   = hex("#f5f0e8");
        TextColor passive = hex("#8a7a6a");

        lore.add(italic("𝘏𝘦 𝘥𝘪𝘥𝘯'𝘵 𝘱𝘭𝘢𝘯 𝘵𝘩𝘪𝘴 𝘱𝘢𝘳𝘵.", "#8a7a6a"));
        lore.add(italic("𝘐𝘵 𝘫𝘶𝘴𝘵 𝘩𝘢𝘱𝘱𝘦𝘯𝘦𝘥.", "#8a7a6a"));
        lore.add(Component.empty());

        lore.add(abilityLine("𝗡𝗔𝗨𝗦𝗘𝗔", "✦ Passive", name, passive));
        lore.add(desc(DESC_NAUSEA, desc));
        lore.add(Component.empty());

        lore.add(soulboundFooter("⚠", "#C8A96E"));
        lore.add(ownerLine(ownerName, "#F5F0E8"));
        return lore;
    }

    private List<Component> buildGenericLore(MaceConfig cfg, String ownerName) {
        List<Component> lore = new ArrayList<>();
        lore.add(soulboundFooter("✦", "#AAAAAA"));
        lore.add(ownerLine(ownerName, "#FFFFFF"));
        return lore;
    }

    // ── Lore component helpers ────────────────────────────────────────────────

    private Component italic(String text, String hex) {
        return Component.text(text)
            .color(TextColor.fromHexString(hex))
            .decoration(TextDecoration.ITALIC, true);
    }

    private Component abilityLine(String abilityName, String tag, TextColor nameColor, TextColor tagColor) {
        return Component.text(abilityName)
            .color(nameColor)
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, false)
            .append(Component.text("  " + tag)
                .color(tagColor)
                .decoration(TextDecoration.ITALIC, false));
    }

    private Component desc(String text, TextColor color) {
        return Component.text(" " + text)
            .color(color)
            .decoration(TextDecoration.ITALIC, false);
    }

    private Component activation(String keys, TextColor color) {
        return Component.text(" ᴬᶜᵗⁱᵛᵃᵗⁱᵒⁿ: ")
            .color(hex("#555555"))
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(keys).color(color).decoration(TextDecoration.ITALIC, false));
    }

    private Component hiddenLine() {
        return Component.text("  " + HIDDEN)
            .color(hex("#444444"))
            .decoration(TextDecoration.ITALIC, false);
    }

    private Component soulboundFooter(String icon, String hexColor) {
        return Component.text(icon + " ꜱᴏᴜʟʙᴏᴜɴᴅ")
            .color(TextColor.fromHexString(hexColor))
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, false);
    }

    private Component ownerLine(String ownerName, String hexAccent) {
        return Component.text("  ᴮᵒᵘⁿᵈ ᵗᵒ: ")
            .color(hex("#445566"))
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(ownerName)
                .color(TextColor.fromHexString(hexAccent))
                .decoration(TextDecoration.ITALIC, false));
    }

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
                Enchantment ench = Enchantment.getByKey(
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

    // ── PDC identification ────────────────────────────────────────────────────

    /**
     * Returns true for ANY soulbound item — mace or sword.
     */
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
