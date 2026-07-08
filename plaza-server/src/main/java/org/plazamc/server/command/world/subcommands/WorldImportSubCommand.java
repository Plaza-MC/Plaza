package org.plazamc.server.command.world.subcommands;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.exceptions.InvalidWorldException;
import org.plazamc.api.exceptions.WorldLoadedException;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;
import org.plazamc.server.world.PlazaWorldShadow;
import org.plazamc.server.world.loader.PlazaWorldSourceRegistry;

public final class WorldImportSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    private static final Logger LOGGER = Logger.getLogger("Plaza");

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.import")) {
            return true;
        }
        if (args.length < 2) {
            PlazaCommand.send(sender, "&cUsage: /plaza world import <folder> <name> [source]");
            return true;
        }

        final File folder = new File(args[0]);
        final String name = args[1];
        final String source = args.length > 2 ? args[2] : PlazaConfig.plazaWorldsDefaultSource();
        final PlazaWorldLoader loader = PlazaWorldSourceRegistry.getLoader(source);
        if (loader == null) {
            PlazaCommand.send(sender, "&cUnknown source: " + source);
            return true;
        }

        try {
            final PlazaWorld world = PlazaAPI.instance().importVanillaWorld(folder, name, loader);
            PlazaAPI.instance().saveWorld(world);
            PlazaWorldShadow.create(name);
            PlazaCommand.send(sender, "&aImported world '" + name + "' from '" + folder.getName() + "'.");
        } catch (final WorldLoadedException ex) {
            PlazaCommand.send(sender, "&cWorld '" + name + "' is currently loaded.");
        } catch (final InvalidWorldException ex) {
            PlazaCommand.send(sender, "&cFolder '" + folder.getName() + "' is not a valid world.");
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Could not import world " + name, ex);
            PlazaCommand.send(sender, "&cCould not import world. See console.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (args.length == 3) {
            return PlazaWorldCommandHandler.filter(PlazaWorldCommandHandler.sourceNames(), args[2]);
        }
        return List.of();
    }
}
