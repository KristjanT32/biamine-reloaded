package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class CreateGameAC implements TabCompleter {

    BiamineReloaded main;

    public CreateGameAC(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("<gameID>");
        } else if (args.length == 2) {
            completions.add("<prepDuration>");
        } else if (args.length == 3) {
            completions.add("<finalCountdownDuration>");
        } else if (args.length == 4) {
            completions.add("<displayName>");
        }
        return completions;
    }


}
