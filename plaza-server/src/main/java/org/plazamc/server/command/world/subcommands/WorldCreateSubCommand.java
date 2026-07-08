package org.plazamc.server.command.world.subcommands;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.api.world.PlazaWorldProperties;
import org.plazamc.api.world.PlazaWorldPropertyMapImpl;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;
import org.plazamc.server.world.PlazaWorldShadow;
import org.plazamc.server.world.loader.PlazaWorldSourceRegistry;

public final class WorldCreateSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    private static final Logger LOGGER = Logger.getLogger("Plaza");

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.create")) {
            return true;
        }
        if (args.length < 1) {
            PlazaCommand.send(sender, "&cUsage: /plaza world create <name> [source]");
            return true;
        }

        final String name = args[0];
        final String source = args.length > 1 ? args[1] : PlazaConfig.plazaWorldsDefaultSource();
        final PlazaWorldLoader loader = PlazaWorldSourceRegistry.getLoader(source);
        if (loader == null) {
            PlazaCommand.send(sender, "&cUnknown source: " + source);
            return true;
        }

        try {
            if (loader.worldExists(name)) {
                PlazaCommand.send(sender, "&cWorld '" + name + "' already exists.");
                return true;
            }

            final PlazaWorld world = PlazaAPI.instance().createEmptyWorld(name, false, new PlazaWorldPropertyMapImpl(), loader);
            PlazaAPI.instance().saveWorld(world);
            PlazaWorldShadow.create(name);
            PlazaCommand.send(sender, "&aCreated world '" + name + "' in source '" + source + "'.");
        } catch (final IOException ex) {
            LOGGER.log(Level.SEVERE, "Could not create world " + name, ex);
            PlazaCommand.send(sender, "&cCould not create world. See console.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return PlazaWorldCommandHandler.filter(PlazaWorldCommandHandler.sourceNames(), args[0]);
        }
        return List.of();
    }
}
