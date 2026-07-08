package org.plazamc.server.command.world.subcommands;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;
import org.plazamc.server.world.PlazaWorldShadow;
import org.plazamc.server.world.loader.PlazaWorldSourceRegistry;

public final class WorldDeleteSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    private static final Logger LOGGER = Logger.getLogger("Plaza");

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.delete")) {
            return true;
        }
        if (args.length < 1) {
            PlazaCommand.send(sender, "&cUsage: /plaza world delete <name>");
            return true;
        }

        final String name = args[0];
        final String source = PlazaConfig.plazaWorldsWorldSource(name);
        final PlazaWorldLoader loader = PlazaWorldSourceRegistry.getLoader(source);
        if (loader == null) {
            PlazaCommand.send(sender, "&cUnknown source: " + source);
            return true;
        }

        try {
            if (Bukkit.getWorld(name) != null) {
                Bukkit.unloadWorld(name, false);
            }
            loader.deleteWorld(name);
            PlazaWorldShadow.delete(name);
            PlazaCommand.send(sender, "&aDeleted world '" + name + "'.");
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Could not delete world " + name, ex);
            PlazaCommand.send(sender, "&cCould not delete world. See console.");
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
