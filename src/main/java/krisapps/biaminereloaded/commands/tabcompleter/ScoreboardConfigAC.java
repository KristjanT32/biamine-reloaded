package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.scoreboard.ScoreboardManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScoreboardConfigAC implements TabCompleter {

    BiamineReloaded main;

    public ScoreboardConfigAC(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "edit", "show", "assign", "list"));
        } else if (args.length == 2) {
            if (Arrays.asList("delete", "edit", "show", "assign").contains(args[0])) {
                completions.addAll(main.dataUtility.getScoreboardConfigs());
            } else if (args[0].equals("create")) {
                completions.add("<configID>");
            }
        } else if (args.length == 3) {
            switch (args[0]) {
                case "edit":
                    completions.addAll(Arrays.asList("<placeholderOnScoreboard>", "title", "line1", "line2", "line3", "line4", "line5", "line6", "line7", "line8"));
                    break;
                case "assign":
                    completions.addAll(main.dataUtility.getGames());
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0]) {
                case "edit":
                    if (args[2].startsWith("line")) {
                        completions.add("setPropertyTo");
                        completions.add("clear");
                    } else if (args[2].equalsIgnoreCase("title")) {
                        completions.add("setPropertyTo");
                    } else {
                        completions.addAll(Arrays.asList("moveTo", "raiseBy", "lowerBy", "changeTo"));
                    }
                    break;
                case "assign":
                    completions.add("-ra");
                    break;
            }
        } else if (args.length == 5) {
            switch (args[3]) {
                case "setPropertyTo":
                    if (args[2].equalsIgnoreCase("title")) {
                        if (args[4].startsWith("%")) {
                            completions.addAll(Arrays.asList("%timer%", "%state%", "%title%"));
                        } else {
                            completions.add("<title content>");
                        }
                    } else {
                        if (args[4].startsWith("%")) {
                            completions.addAll(List.of(ScoreboardManager.getSupportedPlaceholders()));
                        } else if (args[4].startsWith("_")) {
                            completions.addAll(main.dataUtility.getUserDefinedPlaceholders());
                        } else {
                            completions.add("<content>");
                        }
                    }
                    break;
                case "raiseBy":
                case "lowerBy":
                    completions.add("<numberOfLines>");
                    break;
                case "moveTo":
                    completions.add("<newLine>");
                    break;
                case "changeTo":
                    completions.add("<newProperty>");
            }
        }
        return completions;
    }
}
