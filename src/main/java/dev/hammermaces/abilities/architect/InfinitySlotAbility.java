package dev.hammermaces.abilities.architect;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.abilities.architect.augments.AugmentRegistry;
import dev.hammermaces.managers.MaceConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Infinity Slots — Sneak + Right Click
 *
 * Opens a private 3-slot chest GUI for The First Draft holder.
 * Items placed in slots immediately apply their augment effect.
 * Items removed immediately revoke the effect.
 *
 * The GUI is per-player and not shared or visible to others.
 * It uses a virtual inventory — no real chest is placed.
 *
 * Augment effects are managed by AugmentRegistry which maps
 * Material -> IAugment implementation.
 */
public class InfinitySlotAbility implements Listener {

    private final HammerMacesPlugin plugin;
    private final AugmentRegistry registry;

    // Player UUID -> their infinity slots inventory
    private final Map<UUID, Inventory> openGuis = new HashMap<>();

    // Player UUID -> currently slotted items (slot index -> item)
    private final Map<UUID, ItemStack[]> slottedItems = new HashMap<>();

    private static final String GUI_TITLE = "[ ᴛʜᴇ ᴘʟᴀɴ ]";
    private static final int SLOT_COUNT = 3;

    public InfinitySlotAbility(HammerMacesPlugin plugin) {
        this.plugin   = plugin;
        this.registry = new AugmentRegistry(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player holder, MaceConfig cfg) {
        UUID uuid = holder.getUniqueId();

        // Create a private 9-slot inventory (smallest chest size)
        // Only the first 3 slots are usable; rest are blocked with gray glass
        Inventory gui = Bukkit.createInventory(null, 9,
            Component.text(GUI_TITLE)
                .color(TextColor.fromHexString("#4A5568"))
                .decoration(TextDecoration.ITALIC, false));

        // Fill non-slot positions with filler
        ItemStack filler = fillerPane();
        for (int i = SLOT_COUNT; i < 9; i++) {
            gui.setItem(i, filler);
        }

        // Restore previously slotted items if any
        ItemStack[] saved = slottedItems.get(uuid);
        if (saved != null) {
            for (int i = 0; i < Math.min(SLOT_COUNT, saved.length); i++) {
                if (saved[i] != null) gui.setItem(i, saved[i].clone());
            }
        }

        openGuis.put(uuid, gui);
        holder.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        Inventory gui = openGuis.get(uuid);
        if (gui == null) return;
        if (!event.getInventory().equals(gui)) return;

        int slot = event.getRawSlot();

        // Block clicks on filler slots
        if (slot >= SLOT_COUNT && slot < 9) {
            event.setCancelled(true);
            return;
        }

        // Allow only valid slot interactions
        if (slot >= 0 && slot < SLOT_COUNT) {
            // Schedule effect update after click is processed
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                updateEffects(player, gui);
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        Inventory gui = openGuis.get(uuid);
        if (gui == null) return;
        if (!event.getInventory().equals(gui)) return;

        // Cancel drag into filler slots
        for (int slot : event.getRawSlots()) {
            if (slot >= SLOT_COUNT && slot < 9) {
                event.setCancelled(true);
                return;
            }
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateEffects(player, gui);
        }, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        Inventory gui = openGuis.remove(uuid);
        if (gui == null) return;

        // Save slotted items
        ItemStack[] saved = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            saved[i] = gui.getItem(i);
        }
        slottedItems.put(uuid, saved);

        // Final effect sync
        updateEffects(player, gui);
    }

    /**
     * Reads current slot contents and applies/removes augment effects accordingly.
     */
    private void updateEffects(Player player, Inventory gui) {
        UUID uuid = player.getUniqueId();

        // Revoke all current effects first
        registry.revokeAll(player);

        // Apply effects for whatever is currently slotted
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack item = gui.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (item.getType() == Material.GRAY_STAINED_GLASS_PANE) continue;
            registry.apply(player, item, i);
        }
    }

    /**
     * Called on plugin disable or player quit — revokes all effects and returns items.
     */
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        registry.revokeAll(player);

        Inventory gui = openGuis.remove(uuid);
        ItemStack[] saved = slottedItems.remove(uuid);

        // Return slotted items to player inventory
        ItemStack[] items = gui != null
            ? new ItemStack[]{gui.getItem(0), gui.getItem(1), gui.getItem(2)}
            : saved;

        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR
                        && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                    player.getInventory().addItem(item);
                }
            }
        }
    }

    public boolean isOpen(UUID uuid) {
        return openGuis.containsKey(uuid);
    }

    public AugmentRegistry getRegistry() { return registry; }

    private ItemStack fillerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = pane.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        pane.setItemMeta(meta);
        return pane;
    }
}
