package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CheckpointAC implements TabCompleter {

    BiamineReloaded main;

    public CheckpointAC(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(main.dataUtility.getGames());
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("add", "remove", "find", "list", "setbound", "setname", "setfinish"));
        } else if (args.length == 3) {
            switch (args[1]) {
                case "find":
                case "setfinish":
                case "setname":
                case "setbound":
                case "remove":
                    completions.addAll(main.dataUtility.getCheckpoints(args[0]));
                    break;
            }
        } else if (args.length == 4) {
            switch (args[1]) {
                case "find":
                    completions.add("-t");
                    break;
                case "setbound":
                    completions.add("bound1");
                    completions.add("bound2");
                    break;
                case "setname":
                    completions.add("<display_name>");
                    break;
            }
        }


        return completions;
    }
}
