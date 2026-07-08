package org.plazamc.server.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.server.PlazaConfig;

public final class PlazaCommand extends Command {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String SERVER_NAME = "Plaza";
    private static final String PLAZA_WEBSITE = "plazamc.org";
    private static final String BLUEVA_WEBSITE = "blueva.net";
    private static final String COLOR_PRIMARY = "#34a9d5";
    private static final String COLOR_SECONDARY = "#6afafd";
    private static final String SEPARATOR = "<dark_gray><strikethrough>----------------------------------------------</strikethrough></dark_gray>";
    private static final String PREFIX = "<bold><gradient:" + COLOR_PRIMARY + ":" + COLOR_SECONDARY + ">" + SERVER_NAME + "</gradient></bold> <gray>»</gray>";

    public PlazaCommand(final String name) {
        super(name);
        this.description = "Plaza commands";
        this.usageMessage = "/plaza [reload]";
        this.setPermission("plaza.command");

        final PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        registerPermission(pluginManager, "plaza.command", PermissionDefault.TRUE);
        registerPermission(pluginManager, "plaza.command.reload", PermissionDefault.OP);
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("plaza.command.reload")) {
                sender.sendMessage(Bukkit.permissionMessage());
                return true;
            }

            PlazaConfig.reload();
            sender.sendMessage(mm(PREFIX + " <green>Configuration reloaded.</green>"));
            return true;
        }

        if (!this.testPermission(sender)) {
            return true;
        }

        sendInfo(sender);
        return true;
    }

    @Override
    public List<String> tabComplete(
        final CommandSender sender,
        final String alias,
        final String[] args,
        final @Nullable Location location
    ) throws IllegalArgumentException {
        if (args.length == 1 && sender.hasPermission("plaza.command.reload")) {
            final String input = args[0].toLowerCase(java.util.Locale.ROOT);
            if ("reload".startsWith(input)) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
    }

    private static void sendInfo(final CommandSender sender) {
        final String version = io.papermc.paper.ServerBuildInfo.buildInfo()
            .asString(io.papermc.paper.ServerBuildInfo.StringRepresentation.VERSION_SIMPLE);
        final List<Component> lines = new ArrayList<>();
        lines.add(mm(SEPARATOR));
        lines.add(Component.empty());
        lines.add(mm("<bold><gradient:" + COLOR_PRIMARY + ":" + COLOR_SECONDARY + ">" + SERVER_NAME + "</gradient></bold> <gray>-</gray> <yellow>" + version + "</yellow>"));
        lines.add(mm("<gray>Created by Blueva (<click:open_url:'https://" + BLUEVA_WEBSITE + "'><hover:show_text:'<gray>Open " + BLUEVA_WEBSITE + "</gray>'><aqua>" + BLUEVA_WEBSITE + "</aqua></hover></click>)</gray>"));
        lines.add(mm("<gray>Website: <click:open_url:'https://" + PLAZA_WEBSITE + "'><hover:show_text:'<gray>Open " + PLAZA_WEBSITE + "</gray>'><aqua>" + PLAZA_WEBSITE + "</aqua></hover></click></gray>"));
        lines.add(Component.empty());
        lines.add(mm("<gray>Use</gray> <aqua>/plaza reload</aqua> <gray>to reload Plaza configuration.</gray>"));
        lines.add(Component.empty());
        lines.add(mm(SEPARATOR));
        for (final Component line : lines) {
            sender.sendMessage(line);
        }
    }

    private static void registerPermission(final PluginManager pluginManager, final String permission, final PermissionDefault permissionDefault) {
        if (pluginManager.getPermission(permission) == null) {
            pluginManager.addPermission(new Permission(permission, permissionDefault));
        }
    }

    private static Component mm(final String input) {
        return MINI_MESSAGE.deserialize(input);
    }
}
