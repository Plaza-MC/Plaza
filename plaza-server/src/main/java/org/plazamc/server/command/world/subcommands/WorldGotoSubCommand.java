package org.plazamc.server.command.world.subcommands;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.world.PlazaWorldInstance;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;

public final class WorldGotoSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.goto")) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            PlazaCommand.send(sender, "&cThis command can only be used by players.");
            return true;
        }
        if (args.length < 1) {
            PlazaCommand.send(sender, "&cUsage: /plaza world goto <world>");
            return true;
        }

        final String name = args[0];
        final PlazaWorldInstance instance = PlazaAPI.instance().getLoadedWorld(name);
        if (instance == null) {
            PlazaCommand.send(sender, "&cWorld '" + name + "' is not loaded.");
            return true;
        }

        final World bukkitWorld = instance.getBukkitWorld();
        final Location spawn = bukkitWorld.getSpawnLocation();
        // Center the player on the spawn block instead of teleporting to the corner.
        spawn.add(0.5D, 0.0D, 0.5D);
        player.teleport(spawn);
        PlazaCommand.send(sender, "&aTeleported to world '" + name + "'.");
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
