package org.plazamc.server.command.world;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface for {@code /plaza world} subcommands that provide tab-completion.
 */
public interface PlazaWorldTabCompletable {

    List<String> onTabComplete(CommandSender sender, @Nullable Command cmd, String commandLabel, String[] args);
}
