package krisapps.biaminereloaded.commands.tabcompleter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KickAC implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Bukkit.getOnlinePlayers().stream().map((player -> player.getName())).collect(Collectors.toSet()));
        } else if (args.length == 2) {
            completions.add("[reason]");
        }
        return completions;
    }
}
