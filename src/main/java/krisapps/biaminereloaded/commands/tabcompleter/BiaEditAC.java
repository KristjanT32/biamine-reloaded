package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BiaEditAC implements TabCompleter {

    BiamineReloaded main;

    public BiaEditAC(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(main.dataUtility.getGames());
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("display_name", "prep_duration", "final_countdown"));
        } else if (args.length == 3) {
            switch (args[1]) {
                case "display_name":
                    completions.add("<new_display_name>");
                    break;
                case "prep_duration":
                case "final_countdown":
                    completions.add("<new_duration>");
                    break;
            }
        }

        return completions;
    }
}
