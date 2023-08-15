package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DispenserAC implements TabCompleter {

    BiamineReloaded main;

    public DispenserAC(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(main.dataUtility.getGames());
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("addItem", "removeItem", "show"));
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("addItem")) {
                for (Material mat : Material.values()) {
                    if (mat.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(mat.name().toLowerCase());
                    }
                }
            } else if (args[1].equalsIgnoreCase("removeItem")) {
                for (Material mat : Material.values()) {
                    if (mat.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(mat.name().toLowerCase());
                    }
                }
            }
        } else if (args.length == 4) {
            if (args[1].equalsIgnoreCase("addItem")) {
                completions.add("<amount>");
            }
        }

        return completions;
    }
}
