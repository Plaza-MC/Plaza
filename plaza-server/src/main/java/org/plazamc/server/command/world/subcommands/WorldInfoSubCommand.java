package org.plazamc.server.command.world.subcommands;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldInstance;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;

public final class WorldInfoSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.info")) {
            return true;
        }
        if (args.length < 1) {
            PlazaCommand.send(sender, "&cUsage: /plaza world info <name>");
            return true;
        }

        final String name = args[0];
        final PlazaWorldInstance instance = PlazaAPI.instance().getLoadedWorld(name);
        if (instance == null) {
            PlazaCommand.send(sender, "&cWorld '" + name + "' is not loaded.");
            return true;
        }

        final PlazaWorld world = instance.getWorldData();
        PlazaCommand.send(sender, String.format("&aWorld info for '%s':\n&7Format: &b%s\n&7Loaded: &b%s\n&7Read-only: &b%s",
                name, world.getFormat().name(), world.getName(), world.isReadOnly()));
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
