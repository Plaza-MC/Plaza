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
import org.plazamc.server.world.PlazaWorldShadow;
import org.plazamc.server.world.loader.PlazaWorldSourceRegistry;

public final class WorldCloneSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    private static final Logger LOGGER = Logger.getLogger("Plaza");

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.clone")) {
            return true;
        }
        if (args.length < 2) {
            PlazaCommand.send(sender, "&cUsage: /plaza world clone <source> <target>");
            return true;
        }

        final String sourceName = args[0];
        final String targetName = args[1];
        final String source = PlazaConfig.plazaWorldsWorldSource(sourceName);
        final PlazaWorldLoader loader = PlazaWorldSourceRegistry.getLoader(source);
        if (loader == null) {
            PlazaCommand.send(sender, "&cUnknown source: " + source);
            return true;
        }

        try {
            final PlazaWorld world = loader.readWorld(sourceName, false, new PlazaWorldPropertyMapImpl());
            final PlazaWorld cloned = world.clone(targetName, loader);
            PlazaAPI.instance().saveWorld(cloned);
            PlazaWorldShadow.create(targetName);
            PlazaCommand.send(sender, "&aCloned world '" + sourceName + "' to '" + targetName + "'.");
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Could not clone world " + sourceName, ex);
            PlazaCommand.send(sender, "&cCould not clone world. See console.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (args.length <= 1) {
            return PlazaWorldCommandHandler.filter(PlazaWorldCommandHandler.worldNames(), args.length == 0 ? "" : args[0]);
        }
        return List.of();
    }
}
