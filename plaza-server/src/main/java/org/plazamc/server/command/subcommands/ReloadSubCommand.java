package org.plazamc.server.command.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;

public final class ReloadSubCommand implements PlazaCommandInterface {

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!sender.hasPermission("plaza.command.reload")) {
            PlazaCommand.sendPermissionMessage(sender);
            return true;
        }

        PlazaConfig.reload();
        PlazaCommand.send(sender, "&aConfiguration reloaded.");
        return true;
    }
}
