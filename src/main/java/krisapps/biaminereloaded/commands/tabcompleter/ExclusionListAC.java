package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ExclusionListAC implements TabCompleter {

    BiamineReloaded main;

    public ExclusionListAC(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "edit", "assign", "unassign", "list", "view", "delete"));
        } else if (args.length == 2) {
            switch (args[0]) {
                case "create":
                    completions.add("<exclusionListID>");
                    break;
                case "view":
                case "unassign":
                case "assign":
                case "edit":
                case "delete":
                    completions.addAll(main.dataUtility.getExclusionLists());
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0]) {
                case "edit":
                    completions.addAll(Arrays.asList("addPlayer", "removePlayer"));
                    break;
                case "assign":
                case "unassign":
                    completions.addAll(main.dataUtility.getGames());
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0]) {
                case "edit":
                    if (args[2].equalsIgnoreCase("addPlayer")) {
                        if (main.dataUtility.getExcludedPlayers(args[1]) == null) {
                            return null;
                        }
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (!main.dataUtility.getExcludedPlayers(args[1]).contains(p.getUniqueId().toString())) {
                                completions.add(p.getName());
                            }
                        }
                    } else if (args[2].equalsIgnoreCase("removePlayer")) {
                        for (String pUUID : main.dataUtility.getExcludedPlayers(args[1])) {
                            Player p = Bukkit.getPlayer(UUID.fromString(pUUID));
                            if (p != null) {
                                completions.add(p.getName());
                            }
                        }
                    }
                    break;
            }
        }

        return completions;
    }
}
