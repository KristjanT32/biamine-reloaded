package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SetLanguage implements CommandExecutor {

    BiamineReloaded main;

    public SetLanguage(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /setlanguage <langCode>
        if (args.length > 0) {
            String langCode = args[0];
            if (main.localizationUtility.getLanguages().contains(langCode)) {
                if (main.localizationUtility.languageFileExists(langCode)) {
                    main.localizationUtility.changeLanguage(langCode);
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.setlanguage.success").replaceAll("%langName%", main.localizationUtility.getLocalizedPhrase("languageName")));
                } else {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.setlanguage.no-file").replaceAll("%langCode%", args[0]));
                }
            } else {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.setlanguage.unknown").replaceAll("%langCode%", args[0]));
            }
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.setlanguage.insuff"));
        }
        return true;
    }
}
