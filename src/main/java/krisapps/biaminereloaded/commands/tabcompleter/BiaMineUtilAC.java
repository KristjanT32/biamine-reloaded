package krisapps.biaminereloaded.commands.tabcompleter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BiaMineUtilAC implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> operList = Arrays.asList("resetDefaultLanguageFile",
                "refreshFiles",
                "resetScoreboard",
                "printActiveGameID",
                "reloadLocalizations",
                "rereadConfig",
                "reloadCurrentLanguageFile",
                "reload"
        );


        if (args.length == 1) {
            completions.clear();
            for (String opr : operList) {
                if (opr.startsWith(args[0])) {
                    completions.add(opr);
                }
            }
            if (completions.isEmpty()) {
                completions.addAll(operList);
            }
        }

        Collections.sort(completions);
        return completions;
    }
}
