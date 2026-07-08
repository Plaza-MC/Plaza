package org.plazamc.server.command.world.subcommands;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;
import org.plazamc.server.world.loader.PlazaWorldSourceRegistry;

public final class WorldMigrateSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    private static final Logger LOGGER = Logger.getLogger("Plaza");

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.migrate")) {
            return true;
        }
        if (args.length < 2) {
            PlazaCommand.send(sender, "&cUsage: /plaza world migrate <name> <new-source>");
            return true;
        }

        final String name = args[0];
        final String newSourceName = args[1];
        final String currentSourceName = PlazaConfig.plazaWorldsWorldSource(name);
        final PlazaWorldLoader currentLoader = PlazaWorldSourceRegistry.getLoader(currentSourceName);
        final PlazaWorldLoader newLoader = PlazaWorldSourceRegistry.getLoader(newSourceName);

        if (currentLoader == null) {
            PlazaCommand.send(sender, "&cUnknown current source: " + currentSourceName);
            return true;
        }
        if (newLoader == null) {
            PlazaCommand.send(sender, "&cUnknown target source: " + newSourceName);
            return true;
        }

        try {
            PlazaAPI.instance().migrateWorld(name, currentLoader, newLoader);
            PlazaCommand.send(sender, "&aMigrated world '" + name + "' to source '" + newSourceName + "'.");
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Could not migrate world " + name, ex);
            PlazaCommand.send(sender, "&cCould not migrate world. See console.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return PlazaWorldCommandHandler.filter(PlazaWorldCommandHandler.worldNames(), args[0]);
        }
        if (args.length == 2) {
            return PlazaWorldCommandHandler.filter(PlazaWorldCommandHandler.sourceNames(), args[1]);
        }
        return List.of();
    }
}
