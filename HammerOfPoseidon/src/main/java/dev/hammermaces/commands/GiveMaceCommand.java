package dev.hammermaces.commands;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * /givemace <player> <mace_id>
 * Gives the specified player their soulbound mace.
 * Requires: hammermaces.give permission (op by default)
 */
public class GiveMaceCommand implements CommandExecutor, TabCompleter {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;

    public GiveMaceCommand(HammerMacesPlugin plugin) {
        this.plugin = plugin;
        this.maceManager = plugin.getMaceManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("hammermaces.give")) {
            sender.sendMessage(GradientUtils.parseLore("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(GradientUtils.parseLore("&cUsage: /givemace <player> <mace_id>"));
            sender.sendMessage(GradientUtils.parseLore("&7Available IDs: &f" +
                String.join(", ", plugin.getMaceConfigManager().getAllMaceIds())));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(GradientUtils.parseLore("&cPlayer &f" + args[0] + "&c not found or is offline."));
            return true;
        }

        String maceId = args[1].toLowerCase();
        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig(maceId);
        if (cfg == null) {
            sender.sendMessage(GradientUtils.parseLore("&cUnknown mace ID: &f" + maceId));
            sender.sendMessage(GradientUtils.parseLore("&7Available IDs: &f" +
                String.join(", ", plugin.getMaceConfigManager().getAllMaceIds())));
            return true;
        }

        // Don't give duplicates
        for (ItemStack item : target.getInventory().getContents()) {
            if (item == null) continue;
            if (!maceManager.isSoulboundMace(item)) continue;
            if (maceId.equals(maceManager.getMaceType(item)) && maceManager.isOwner(item, target.getName())) {
                sender.sendMessage(GradientUtils.parseLore(
                    "&e" + target.getName() + " already has the &f" + maceId + "&e mace!"));
                return true;
            }
        }

        ItemStack mace = maceManager.createMace(cfg, target.getName());
        target.getInventory().addItem(mace);

        target.sendMessage(GradientUtils.parseLore(
            "&#1a1a6e✦ &#00e5ffYou have been granted a soulbound mace: &f" + cfg.getDisplayName()));
        sender.sendMessage(GradientUtils.parseLore(
            "&aGave &f" + target.getName() + " &athe mace: &f" + maceId));

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            for (String id : plugin.getMaceConfigManager().getAllMaceIds()) {
                if (id.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(id);
                }
            }
        }

        return completions;
    }
}
