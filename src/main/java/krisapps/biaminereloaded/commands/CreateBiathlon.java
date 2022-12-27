package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.ErrorType;
import krisapps.biaminereloaded.utilities.MessageUtility;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CreateBiathlon implements CommandExecutor {

    //TODO: Write logic for basic commands
    //TODO: Start writing logic for localization system to use it on the go

    MessageUtility messages;

    BiamineReloaded main;

    public CreateBiathlon(BiamineReloaded main) {
        this.main = main;
        messages = new MessageUtility(main);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /createbiathlon <gameID>
        if (args.length >= 1) {
            Bukkit.dispatchCommand(sender, "say uh-uh-uh");
        } else {
            messages.sendError(sender, ErrorType.INSUFFICIENT_PARAMETERS);
        }

        return true;
    }
}
