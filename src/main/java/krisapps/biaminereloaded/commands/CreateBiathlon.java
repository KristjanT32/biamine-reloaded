package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.GenericErrorType;
import krisapps.biaminereloaded.types.Placeholder;
import krisapps.biaminereloaded.utilities.BiaMineDataUtility;
import krisapps.biaminereloaded.utilities.LocalizationUtility;
import krisapps.biaminereloaded.utilities.MessageUtility;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CreateBiathlon implements CommandExecutor {

    //TODO: Write logic for basic commands
    //TODO: Start writing logic for localization system to use it on the go

    MessageUtility messages;
    BiaMineDataUtility dataUtility;
    LocalizationUtility localizationUtility;

    BiamineReloaded main;

    public CreateBiathlon(BiamineReloaded main) {
        this.main = main;
        messages = new MessageUtility(main);
        dataUtility = new BiaMineDataUtility(main);
        localizationUtility = new LocalizationUtility(main);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /createbiathlon <gameID> <preparationTime> <finalCountdown> <displayName>
        if (args.length >= 4) {
            String gameID = args[0];
            int preparationTime = Integer.parseInt(args[1]) > 0 ? Integer.parseInt(args[1]) : 5;
            int finalCountdown = Integer.parseInt(args[2]) > 0 ? Integer.parseInt(args[2]) : 3;
            String displayName = args[3];

            if (!dataUtility.gameExists(gameID)) {
                if (dataUtility.createGame(gameID, preparationTime, finalCountdown, displayName)) {
                    String msg;
                    msg = messages.replacePlaceholder(localizationUtility.getLocalizedPhrase("commands.createbiathlon.success"), gameID, Placeholder.BIATHLON_INSTANCE);
                    msg = messages.replacePlaceholder(msg, preparationTime, Placeholder.PREPARATION_TIME);
                    msg = messages.replacePlaceholder(msg, finalCountdown, Placeholder.FINAL_COUNTDOWN);

                    messages.sendMessage(sender, msg);
                    messages.sendMessage(sender, localizationUtility.getLocalizedPhrase("commands.createbiathlon.info"));
                } else {
                    messages.sendMessage(sender, localizationUtility.getLocalizedPhrase("commands.createbiathlon.failure"));
                }
            } else {
                messages.sendMessage(sender, localizationUtility.getLocalizedPhrase("commands.createbiathlon.already-exists"));
            }


        } else {
            messages.sendError(sender, GenericErrorType.INSUFFICIENT_PARAMETERS);
        }

        return true;
    }
}
