package org.plazamc.server.command.subcommands;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;
import org.plazamc.server.command.world.PlazaWorldTabCompletable;

public final class WorldSubCommand implements PlazaCommandInterface, PlazaWorldTabCompletable {

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!sender.hasPermission("plaza.command.world")) {
            PlazaCommand.sendPermissionMessage(sender);
            return true;
        }

        return PlazaWorldCommandHandler.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        return PlazaWorldCommandHandler.tabComplete(sender, args);
    }
}
