package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BiaMineUtility implements CommandExecutor {

    BiamineReloaded main;

    public BiaMineUtility(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /biamineutil <operation>
        // Operations: | resetDefaultLanguageFile
        //             | refreshFiles
        //             | refreshActiveScoreboard
        //             | resetScoreboard
        //             | printActiveGameID
        //             | reloadLocalizations
        //             | rereadConfig
        //             | reloadCurrentLanguageFile

        if (args.length >= 1) {
            switch (args[0]) {
                case "resetDefaultLanguageFile":
                    int response = main.resetDefaultLanguageFile();
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.reset-default-lang.completed")
                            .replaceAll("%response%", response == 200
                                    ? "Successfully reset the default language file (en-US)."
                                    : "Something prevented the plugin from completing the operation. Check the logs for more info."
                            )
                    );
                    break;
                case "refreshFiles":
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.refresh-files.start"));
                    main.reloadFiles();
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.refresh-files.completed"));
                    break;
                case "refreshActiveScoreboard":
                    main.gameUtility.refreshScoreboard();
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("generic.operation.succeeded"));
                    break;
                case "resetScoreboard":
                    main.gameUtility.resetScoreboard();
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("generic.operation.succeeded"));
                    break;
                case "printActiveGameID":
                    if (main.gameUtility.getActiveGameID() != null) {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.printactivegame.response")
                                .replaceAll("%game%", main.gameUtility.getActiveGameID())
                        );
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.printactivegame.none"));
                    }
                    break;
                case "reloadLocalizations":
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.reloadlocalizations.start"));
                    main.reloadLocalizations();
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.reloadlocalizations.completed"));
                    break;
                case "rereadConfig":
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.reread-config.start"));
                    main.reloadConfig();
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.reread-config.completed"));
                    break;
                case "reloadCurrentLanguageFile":
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.reload-current-lang.start"));
                    main.reloadCurrentLanguageFile();
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.reload-current-lang.completed"));
                    break;
                case "reload":
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("internals.reload-start"));
                    main.reloadConfig();
                    main.reloadLocalizations();
                    main.reloadFiles();
                    main.reloadCurrentLanguageFile();
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("internals.reload-complete"));
                    break;
                default:
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biamineutil.invalid-operation"));
            }
        }


        return true;
    }
}
