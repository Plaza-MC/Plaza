package org.plazamc.server.command.subcommands;

import io.papermc.paper.ServerBuildInfo;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plazamc.server.command.PlazaCommand;
import org.plazamc.server.command.PlazaCommandInterface;

public final class InfoSubCommand implements PlazaCommandInterface {

    private static final String SEPARATOR = "<dark_gray><strikethrough>----------------------------------------------</strikethrough></dark_gray>";
    private static final String PLAZA_WEBSITE = "plazamc.org";
    private static final String BLUEVA_WEBSITE = "blueva.net";

    @Override
    public boolean onCommand(final CommandSender sender, final @Nullable Command cmd, final String commandLabel, final String[] args) {
        if (!sender.hasPermission("plaza.command")) {
            PlazaCommand.sendPermissionMessage(sender);
            return true;
        }

        final String version = ServerBuildInfo.buildInfo()
                .asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE);
        final List<Component> lines = new ArrayList<>();
        lines.add(PlazaCommand.mm(SEPARATOR));
        lines.add(Component.empty());
        lines.add(PlazaCommand.mm("<bold><gradient:#34a9d5:#6afafd>Plaza</gradient></bold> <gray>-</gray> <yellow>" + version + "</yellow>"));
        lines.add(PlazaCommand.mm("<gray>Created by Blueva (<click:open_url:'https://" + BLUEVA_WEBSITE + "'><hover:show_text:'<gray>Open " + BLUEVA_WEBSITE + "</gray>'><aqua>" + BLUEVA_WEBSITE + "</aqua></hover></click>)</gray>"));
        lines.add(PlazaCommand.mm("<gray>Website: <click:open_url:'https://" + PLAZA_WEBSITE + "'><hover:show_text:'<gray>Open " + PLAZA_WEBSITE + "</gray>'><aqua>" + PLAZA_WEBSITE + "</aqua></hover></click></gray>"));
        lines.add(Component.empty());
        lines.add(PlazaCommand.mm("<gray>Use</gray> <aqua>/plaza reload</aqua> <gray>to reload Plaza configuration.</gray>"));
        lines.add(Component.empty());
        lines.add(PlazaCommand.mm(SEPARATOR));
        for (final Component line : lines) {
            sender.sendMessage(line);
        }
        return true;
    }
}
