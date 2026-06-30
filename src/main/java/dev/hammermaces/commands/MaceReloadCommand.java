package dev.hammermaces.commands;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.utils.GradientUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * /macereload — Reloads all mace config files and global config without restarting the server.
 * Requires: hammermaces.reload (op by default)
 */
public class MaceReloadCommand implements CommandExecutor {

    private final HammerMacesPlugin plugin;

    public MaceReloadCommand(HammerMacesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("hammermaces.reload")) {
            sender.sendMessage(GradientUtils.parseLore("&cYou don't have permission to reload mace configs."));
            return true;
        }

        long start = System.currentTimeMillis();
        plugin.reload();
        long elapsed = System.currentTimeMillis() - start;

        sender.sendMessage(GradientUtils.parseLore(
            "&#00e5ff✦ HammerMaces reloaded in &f" + elapsed + "ms&7. Loaded &f" +
            plugin.getMaceConfigManager().getAllMaceIds().size() + " &7mace(s)."));

        return true;
    }
}
