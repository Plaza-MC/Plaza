package org.plazamc.server.command.world;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.subcommands.WorldCloneSubCommand;
import org.plazamc.server.command.world.subcommands.WorldCreateSubCommand;
import org.plazamc.server.command.world.subcommands.WorldDeleteSubCommand;
import org.plazamc.server.command.world.subcommands.WorldExportSubCommand;
import org.plazamc.server.command.world.subcommands.WorldImportSubCommand;
import org.plazamc.server.command.world.subcommands.WorldInfoSubCommand;
import org.plazamc.server.command.world.subcommands.WorldListSubCommand;
import org.plazamc.server.command.world.subcommands.WorldLoadSubCommand;
import org.plazamc.server.command.world.subcommands.WorldMigrateSubCommand;
import org.plazamc.server.command.world.subcommands.WorldSaveSubCommand;
import org.plazamc.server.command.world.subcommands.WorldSetSpawnSubCommand;
import org.plazamc.server.command.world.subcommands.WorldUnloadSubCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatches {@code /plaza world ...} subcommands.
 */
public final class PlazaWorldCommandHandler {

    private static final Map<String, PlazaCommandInterface> COMMANDS = new HashMap<>();

    static {
        register("create", new WorldCreateSubCommand());
        register("load", new WorldLoadSubCommand());
        register("unload", new WorldUnloadSubCommand());
        register("delete", new WorldDeleteSubCommand());
        register("clone", new WorldCloneSubCommand());
        register("migrate", new WorldMigrateSubCommand());
        register("import", new WorldImportSubCommand());
        register("export", new WorldExportSubCommand());
        register("list", new WorldListSubCommand());
        register("info", new WorldInfoSubCommand());
        register("setspawn", new WorldSetSpawnSubCommand());
        register("save", new WorldSaveSubCommand());
    }

    private PlazaWorldCommandHandler() {
    }

    public static void registerPermissions(final PluginManager pluginManager) {
        registerPermission(pluginManager, "plaza.command.world", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.create", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.load", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.unload", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.delete", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.clone", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.migrate", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.import", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.export", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.list", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.info", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.setspawn", PermissionDefault.OP);
        registerPermission(pluginManager, "plaza.command.world.save", PermissionDefault.OP);
    }

    public static boolean execute(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        final String sub = args[0].toLowerCase();
        final PlazaCommandInterface command = COMMANDS.get(sub);
        if (command == null) {
            sendUsage(sender);
            return true;
        }

        final String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        return command.onCommand(sender, null, sub, subArgs);
    }

    public static List<String> tabComplete(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(new ArrayList<>(COMMANDS.keySet()), args[0]);
        }

        final PlazaCommandInterface command = COMMANDS.get(args[0].toLowerCase());
        if (command instanceof PlazaWorldTabCompletable completable) {
            final String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, subArgs.length);
            return completable.onTabComplete(sender, null, args[0], subArgs);
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

    public static void sendUsage(final CommandSender sender) {
        PlazaCommand.send(sender, "&ePlaza world commands:");
        PlazaCommand.send(sender, "&b/plaza world create <name> [source]");
        PlazaCommand.send(sender, "&b/plaza world load <name>");
        PlazaCommand.send(sender, "&b/plaza world unload <name> [save]");
        PlazaCommand.send(sender, "&b/plaza world delete <name>");
        PlazaCommand.send(sender, "&b/plaza world clone <source> <target>");
        PlazaCommand.send(sender, "&b/plaza world migrate <name> <new-source>");
        PlazaCommand.send(sender, "&b/plaza world import <folder> <name> [source]");
        PlazaCommand.send(sender, "&b/plaza world export <name> <folder>");
        PlazaCommand.send(sender, "&b/plaza world list");
        PlazaCommand.send(sender, "&b/plaza world info <name>");
        PlazaCommand.send(sender, "&b/plaza world setspawn <name>");
        PlazaCommand.send(sender, "&b/plaza world save <name>");
    }

    public static List<String> filter(final List<String> options, final String input) {
        final String lower = input.toLowerCase();
        final List<String> result = new ArrayList<>();
        for (final String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    public static List<String> worldNames() {
        final List<String> names = new ArrayList<>();
        for (final org.bukkit.World world : Bukkit.getWorlds()) {
            names.add(world.getName());
        }
        return names;
    }

    public static List<String> sourceNames() {
        final List<String> names = new ArrayList<>();
        final ConfigurationSection section = PlazaConfig.plazaWorldsSources();
        for (final String key : section.getKeys(false)) {
            if (PlazaConfig.plazaWorldsSourceEnabled(key)) {
                names.add(key);
            }
        }
        return names;
    }

    public static boolean checkPermission(final CommandSender sender, final String permission) {
        if (!sender.hasPermission(permission)) {
            PlazaCommand.sendPermissionMessage(sender);
            return false;
        }
        return true;
    }
}
