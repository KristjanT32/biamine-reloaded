package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.data.BiaMineDataUtility;
import krisapps.biaminereloaded.types.ErrorType;
import krisapps.biaminereloaded.utilities.LocalizationUtility;
import krisapps.biaminereloaded.utilities.MessageUtility;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DeleteBiathlon implements CommandExecutor {

    MessageUtility messages;
    BiaMineDataUtility dataUtility;
    LocalizationUtility localizationUtility;

    BiamineReloaded main;

    public DeleteBiathlon(BiamineReloaded main) {
        this.main = main;
        messages = new MessageUtility(main);
        dataUtility = new BiaMineDataUtility(main);
        localizationUtility = new LocalizationUtility(main);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /deletebiathlon <gameID>
        if (args.length > 0) {
            if (dataUtility.gameExists(args[0])) {
                if (dataUtility.deleteGame(args[0])) {
                    messages.sendMessage(
                            sender,
                            localizationUtility.getLocalizedPhrase("commands.deletebiathlon.success")
                    );
                } else {
                    messages.sendMessage(
                            sender,
                            localizationUtility.getLocalizedPhrase("commands.deletebiathlon.failure")
                    );
                }
            } else {
                messages.sendMessage(
                        sender,
                        localizationUtility.getLocalizedPhrase("commands.deletebiathlon.not-found")
                );
            }
        } else {
            messages.sendError(sender, ErrorType.INSUFFICIENT_PARAMETERS, null);
        }

        return true;
    }
}
