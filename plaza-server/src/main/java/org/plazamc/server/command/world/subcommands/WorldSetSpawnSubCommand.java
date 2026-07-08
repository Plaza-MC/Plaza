package org.plazamc.server.command.world.subcommands;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;

public final class WorldSetSpawnSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.setspawn")) {
            return true;
        }
        if (args.length < 1) {
            PlazaCommand.send(sender, "&cUsage: /plaza world setspawn <name>");
            return true;
        }

        final String name = args[0];
        final org.bukkit.World bukkitWorld = Bukkit.getWorld(name);
        if (bukkitWorld == null) {
            PlazaCommand.send(sender, "&cWorld '" + name + "' is not loaded.");
            return true;
        }

        final Location spawn = sender instanceof Player player ? player.getLocation() : bukkitWorld.getSpawnLocation();
        bukkitWorld.setSpawnLocation(spawn);
        PlazaCommand.send(sender, String.format("&aSet spawn of '%s' to %.1f, %.1f, %.1f.", name, spawn.getX(), spawn.getY(), spawn.getZ()));
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return PlazaWorldCommandHandler.filter(PlazaWorldCommandHandler.worldNames(), args[0]);
        }
        return List.of();
    }
}
