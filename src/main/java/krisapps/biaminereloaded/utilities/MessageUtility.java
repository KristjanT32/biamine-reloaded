package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.ErrorType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageUtility {

    BiamineReloaded main;
    LocalizationUtility localizationUtility;

    public MessageUtility(BiamineReloaded main) {
        this.main = main;
        localizationUtility = new LocalizationUtility(main);
    }

    public void sendError(CommandSender target, ErrorType type) {
        switch (type) {
            case INSUFFICIENT_PARAMETERS:
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', localizationUtility.getLocalizedPhrase("errors.insufficient-parameters")));
                break;
            case TOO_MANY_PARAMETERS:
                break;
            case INVALID_SYNTAX:
                break;
            case PLAYER_NOT_FOUND:
                break;
            case TARGET_OFFLINE:
                break;
        }
    }

}
