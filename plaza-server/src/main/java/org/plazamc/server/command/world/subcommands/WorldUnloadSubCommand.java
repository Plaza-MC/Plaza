package org.plazamc.server.command.world.subcommands;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;
import org.plazamc.server.world.PlazaWorldManager;

public final class WorldUnloadSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.unload")) {
            return true;
        }
        if (args.length < 1) {
            PlazaCommand.send(sender, "&cUsage: /plaza world unload <name> [save]");
            return true;
        }

        final String name = args[0];
        final boolean save = args.length <= 1 || Boolean.parseBoolean(args[1]);

        if (PlazaWorldManager.unloadWorld(name, save)) {
            PlazaCommand.send(sender, "&aUnloaded world '" + name + "'.");
        } else {
            PlazaCommand.send(sender, "&cCould not unload world '" + name + "'.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return PlazaWorldCommandHandler.filter(PlazaWorldCommandHandler.worldNames(), args[0]);
        }
        if (args.length == 2) {
            return PlazaWorldCommandHandler.filter(List.of("true", "false"), args[1]);
        }
        return List.of();
    }
}
