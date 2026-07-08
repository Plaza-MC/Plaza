package org.plazamc.server.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Interface for Plaza subcommands.
 */
public interface PlazaCommandInterface {

    boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args);
}
