package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ListGames implements CommandExecutor {


    BiamineReloaded main;

    public ListGames(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /listgames
        main.messageUtility.sendMessage(sender, "&b=======================================");
        if (main.dataUtility.getGames().size() > 0) {
            for (String gameID : main.dataUtility.getGames()) {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.listgames.prefix-game") + gameID);
            }
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.listgames.nogames"));
        }
        main.messageUtility.sendMessage(sender, "&b=======================================");

        return true;
    }
}
