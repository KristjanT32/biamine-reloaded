package krisapps.biaminereloaded.commands.tabcompleter;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class SetLanguageAC implements TabCompleter {

    BiamineReloaded main;

    public SetLanguageAC(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(main.localizationUtility.getLanguages());
        }

        return completions;
    }
}
