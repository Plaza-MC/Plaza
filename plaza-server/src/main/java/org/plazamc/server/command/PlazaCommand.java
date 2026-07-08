package org.plazamc.server.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.server.command.subcommands.InfoSubCommand;
import org.plazamc.server.command.subcommands.ReloadSubCommand;
import org.plazamc.server.command.subcommands.WorldSubCommand;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;

public final class PlazaCommand extends Command {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Map<String, PlazaCommandInterface> COMMANDS = new HashMap<>();
    private static final InfoSubCommand INFO = new InfoSubCommand();

    public static final String PREFIX = "<bold><gradient:#34a9d5:#6afafd>Plaza</gradient></bold> <gray>»</gray>";

    static {
        register("reload", new ReloadSubCommand());
        register("world", new WorldSubCommand());
    }

    public PlazaCommand(final String name) {
        super(name);
        this.description = "Plaza commands";
        this.usageMessage = "/plaza [reload|world]";
        this.setPermission("plaza.command");

        final PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        registerPermission(pluginManager, "plaza.command", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.reload", PermissionDefault.OP);
        PlazaWorldCommandHandler.registerPermissions(pluginManager);
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        if (args.length == 0) {
            return INFO.onCommand(sender, this, commandLabel, args);
        }

        final PlazaCommandInterface command = COMMANDS.get(args[0].toLowerCase());
        if (command == null) {
            return INFO.onCommand(sender, this, commandLabel, args);
        }

        final String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        return command.onCommand(sender, this, commandLabel, subArgs);
    }

    @Override
    public List<String> tabComplete(
        final CommandSender sender,
        final String alias,
        final String[] args,
        final @Nullable Location location
    ) throws IllegalArgumentException {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>();
            if (sender.hasPermission("plaza.command.reload")) {
                options.add("reload");
            }
            if (sender.hasPermission("plaza.command.world")) {
                options.add("world");
            }
            return filter(options, args[0]);
        }

        if (args.length > 1) {
            final PlazaCommandInterface command = COMMANDS.get(args[0].toLowerCase());
            if (command instanceof PlazaWorldTabCompletable completable) {
                final String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return completable.onTabComplete(sender, this, args[0], subArgs);
            }
        }

        return Collections.emptyList();
    }

    private static void register(final String name, final PlazaCommandInterface command) {
        COMMANDS.put(name, command);
    }

    private static void registerPermission(final PluginManager pluginManager, final String permission, final PermissionDefault permissionDefault) {
        if (pluginManager.getPermission(permission) == null) {
            pluginManager.addPermission(new Permission(permission, permissionDefault));
        }
    }

    private static List<String> filter(final List<String> options, final String input) {
        final String lower = input.toLowerCase();
        final List<String> result = new ArrayList<>();
        for (final String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    public static Component mm(final String input) {
        return MINI_MESSAGE.deserialize(input);
    }

    public static void send(final CommandSender sender, final String message) {
        final Component prefix = MINI_MESSAGE.deserialize(PREFIX);
        final Component body = LEGACY.deserialize(message);
        sender.sendMessage(prefix.append(Component.space()).append(body));
    }

    public static void sendPermissionMessage(final CommandSender sender) {
        sender.sendMessage(Bukkit.permissionMessage());
    }
}
