package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.data.BiaMineDataUtility;
import krisapps.biaminereloaded.utilities.LocalizationUtility;
import krisapps.biaminereloaded.utilities.MessageUtility;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ListGames implements CommandExecutor {


    BiamineReloaded main;
    BiaMineDataUtility data;
    MessageUtility messages;
    LocalizationUtility locutil;

    public ListGames(BiamineReloaded main) {
        this.main = main;
        this.messages = new MessageUtility(main);
        this.data = new BiaMineDataUtility(main);
        this.locutil = new LocalizationUtility(main);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /listgames
        messages.sendMessage(sender, "&b=======================================");
        if (data.getGames().size() > 0) {
            for (String gameID : data.getGames()) {
                messages.sendMessage(sender, locutil.getLocalizedPhrase("commands.listgames.prefix-game") + gameID);
            }
        } else {
            messages.sendMessage(sender, locutil.getLocalizedPhrase("commands.listgames.nogames"));
        }
        messages.sendMessage(sender, "&b=======================================");


        return true;
    }
}
