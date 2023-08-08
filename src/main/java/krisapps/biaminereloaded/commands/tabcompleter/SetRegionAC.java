package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetRegionAC implements TabCompleter {

    BiamineReloaded main;

    public SetRegionAC(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("setstart")) {
            if (args.length == 1) {
                completions.addAll(main.dataUtility.getGames());
            } else if (args.length == 2) {
                completions.addAll(Arrays.asList("bound1", "bound2"));
            }
        } else if (command.getName().equalsIgnoreCase("addcheckpoint")) {
            if (args.length == 1) {
                completions.addAll(main.dataUtility.getGames());
            } else if (args.length == 2) {
                completions.addAll(main.dataUtility.getCheckpoints(args[0]));
                completions.add("<new>");
            } else if (args.length == 3) {
                completions.addAll(Arrays.asList("bound1", "bound2"));
            }
        }
        return completions;
    }
}
