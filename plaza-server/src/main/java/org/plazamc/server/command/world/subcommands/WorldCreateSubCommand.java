package org.plazamc.server.command.world.subcommands;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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
import org.plazamc.server.generator.PlazaVoidChunkGenerator;
import org.plazamc.server.world.PlazaAnvilWorld;
import org.plazamc.server.world.PlazaWorldManager;
import org.plazamc.server.world.loader.PlazaWorldSourceRegistry;

public final class WorldCreateSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    private static final Logger LOGGER = Logger.getLogger("Plaza");
    private static final List<String> FORMATS = Arrays.asList("slime", "anvil");

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.create")) {
            return true;
        }
        if (args.length < 1) {
            PlazaCommand.send(sender, "&cUsage: /plaza world create <name> [source] [format]");
            return true;
        }

        final String name = args[0];
        final String source;
        final String format;

        if (args.length == 1) {
            source = PlazaConfig.plazaWorldsDefaultSource();
            format = "slime";
        } else if (args.length == 2) {
            source = args[1].toLowerCase();
            format = "slime";
        } else {
            source = args[1].toLowerCase();
            format = args[2].toLowerCase();
        }

        if ("anvil".equals(format)) {
            return createAnvilWorld(sender, name, source);
        }

        if (!"slime".equals(format)) {
            PlazaCommand.send(sender, "&cWorld format '" + format + "' is not supported yet.");
            return true;
        }

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
            PlazaAPI.instance().loadWorld(world, true);
            PlazaConfig.addPlazaWorld(name, format, source);
            PlazaCommand.send(sender, "&aCreated and loaded world '" + name + "' (format: " + format + ", source: " + source + ").");
        } catch (final IOException ex) {
            LOGGER.log(Level.SEVERE, "Could not create world " + name, ex);
            PlazaCommand.send(sender, "&cCould not create world. See console.");
        }
        return true;
    }

    private static boolean createAnvilWorld(final CommandSender sender, final String name, final String source) {
        if (org.bukkit.Bukkit.getWorld(name) != null) {
            PlazaCommand.send(sender, "&cWorld '" + name + "' already exists.");
            return true;
        }

        PlazaConfig.addPlazaWorld(name, "anvil", source);

        final WorldCreator creator = new WorldCreator(name)
            .environment(World.Environment.NORMAL)
            .generator(new PlazaVoidChunkGenerator());
        final World bukkitWorld = creator.createWorld();
        if (bukkitWorld == null) {
            PlazaCommand.send(sender, "&cCould not create Anvil world '" + name + "'.");
            return true;
        }

        final PlazaAnvilWorld worldData = new PlazaAnvilWorld(name, false);
        PlazaWorldManager.registerLoadedWorld(worldData, bukkitWorld);
        PlazaCommand.send(sender, "&aCreated and loaded Anvil world '" + name + "'.");
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (args.length == 2) {
            return PlazaWorldCommandHandler.filter(PlazaWorldCommandHandler.sourceNames(), args[1]);
        }
        if (args.length == 3) {
            return PlazaWorldCommandHandler.filter(FORMATS, args[2]);
        }
        return List.of();
    }
}
