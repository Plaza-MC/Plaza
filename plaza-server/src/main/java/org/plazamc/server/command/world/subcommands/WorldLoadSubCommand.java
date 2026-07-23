package org.plazamc.server.command.world.subcommands;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.api.world.PlazaWorldPropertyMapImpl;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;
import org.plazamc.server.world.loader.PlazaWorldSourceRegistry;

public final class WorldLoadSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    private static final Logger LOGGER = Logger.getLogger("Plaza");

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.load")) {
            return true;
        }
        if (args.length < 1) {
            PlazaCommand.send(sender, "&cUsage: /plaza world load <name>");
            return true;
        }

        final String name = args[0];
        final String format = PlazaConfig.plazaWorldsWorldFormat(name);
        if ("ANVIL".equalsIgnoreCase(format) || "LINEAR".equalsIgnoreCase(format)) {
            try {
                org.plazamc.server.world.PlazaWorldManager.loadFolderWorld(name);
                PlazaCommand.send(sender, "&aLoaded " + format.toUpperCase() + " world '" + name + "'.");
            } catch (final Exception ex) {
                LOGGER.log(Level.SEVERE, "Could not load " + format + " world " + name, ex);
                PlazaCommand.send(sender, "&cCould not load world. See console.");
            }
            return true;
        }

        final String source = PlazaConfig.plazaWorldsWorldSource(name);
        final PlazaWorldLoader loader = PlazaWorldSourceRegistry.getLoader(source);
        if (loader == null) {
            PlazaCommand.send(sender, "&cUnknown source: " + source);
            return true;
        }

        try {
            final PlazaWorld world = loader.readWorld(name, PlazaConfig.plazaWorldsWorldReadOnly(name), new PlazaWorldPropertyMapImpl());
            PlazaAPI.instance().loadWorld(world, true);
            PlazaCommand.send(sender, "&aLoaded world '" + name + "'.");
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Could not load world " + name, ex);
            PlazaCommand.send(sender, "&cCould not load world. See console.");
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
