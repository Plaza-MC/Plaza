package org.plazamc.server.command.world.subcommands;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.world.PlazaWorldInstance;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;

public final class WorldSaveSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    private static final Logger LOGGER = Logger.getLogger("Plaza");

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.save")) {
            return true;
        }
        if (args.length < 1) {
            PlazaCommand.send(sender, "&cUsage: /plaza world save <name>");
            return true;
        }

        final String name = args[0];
        final PlazaWorldInstance instance = PlazaAPI.instance().getLoadedWorld(name);
        if (instance == null) {
            PlazaCommand.send(sender, "&cWorld '" + name + "' is not loaded.");
            return true;
        }

        try {
            PlazaAPI.instance().saveWorld(instance.getWorldData());
            PlazaCommand.send(sender, "&aSaved world '" + name + "'.");
        } catch (final IOException ex) {
            LOGGER.log(Level.SEVERE, "Could not save world " + name, ex);
            PlazaCommand.send(sender, "&cCould not save world. See console.");
        }
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
