package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShootingRangeAC implements TabCompleter {

    BiamineReloaded main;

    public ShootingRangeAC(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(main.dataUtility.getGames());
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("addSpot", "editSpot", "removeSpot", "visualizeSpots", "showInfo"));
        } else if (args.length == 3) {
            switch (args[1]) {
                case "editSpot":
                case "removeSpot":
                    completions.addAll(main.dataUtility.getShootingSpots(args[0]));
                    break;
                case "showInfo":
                    completions.addAll(Arrays.asList("shootingRange", "spots", "target"));
                    break;
            }
        } else if (args.length == 4) {
            if (args[1].equals("editSpot")) {
                completions.addAll(Arrays.asList("setbound1", "setbound2", "addTarget", "removeTarget"));
            }
        } else if (args.length == 5) {
            if (args[1].equalsIgnoreCase("editSpot")) {
                switch (args[2]) {
                    case "addTarget":
                    case "removeTarget":
                        completions.add("<select_block>");
                        break;
                }
            }
        }

        return completions;
    }
}
