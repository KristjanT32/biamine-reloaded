package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class TerminateAC implements TabCompleter {

    BiamineReloaded main;

    public TerminateAC(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(main.dataUtility.getGames());
        } else if (args.length >= 2) {
            completions.add("[reason]");
        }

        return completions;
    }
}
