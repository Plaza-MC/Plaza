package org.plazamc.server.command.world.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.world.PlazaWorldInstance;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;
import org.plazamc.server.command.world.PlazaWorldCommandHandler;

public final class WorldListSubCommand implements PlazaCommandInterface {

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!PlazaWorldCommandHandler.checkPermission(sender, "plaza.command.world.list")) {
            return true;
        }

        final StringBuilder builder = new StringBuilder("&aLoaded Plaza worlds:");
        if (PlazaAPI.instance().getLoadedWorlds().isEmpty()) {
            builder.append(" &7none");
        } else {
            for (final PlazaWorldInstance instance : PlazaAPI.instance().getLoadedWorlds()) {
                builder.append("\n&7- &b").append(instance.getWorldData().getName()).append(" &7(").append(instance.getWorldData().getFormat().name()).append(")");
            }
        }
        PlazaCommand.send(sender, builder.toString());
        return true;
    }
}
