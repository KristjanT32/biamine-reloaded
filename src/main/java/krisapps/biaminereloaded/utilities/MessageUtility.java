package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.ErrorType;
import krisapps.biaminereloaded.types.Placeholder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageUtility {

    BiamineReloaded main;
    LocalizationUtility localizationUtility;

    public MessageUtility(BiamineReloaded main) {
        this.main = main;
        localizationUtility = new LocalizationUtility(main);
    }

    // Placeholders:
    /*

    %instance% = gameID
    %player% = player

     */

    public String replacePlaceholder(String inputString, Object replacement, Placeholder placeholderToReplace) {
        inputString = inputString.replaceAll(placeholderToReplace.getPlaceholder(), replacement.toString());
        return inputString;
    }


    /**
     * Send an error message (no placeholders)
     *
     * @param target the player who receives the error message
     * @param type   the type of error message to send
     */
    public void sendError(CommandSender target, ErrorType type, String placeholderReplacement) {
        switch (type) {
            case INSUFFICIENT_PARAMETERS:
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', localizationUtility.getLocalizedPhrase("errors.insufficient-parameters")));
                break;
            case TOO_MANY_PARAMETERS:
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', localizationUtility.getLocalizedPhrase("errors.too-many-parameters")));
                break;
            case INVALID_SYNTAX:
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', localizationUtility.getLocalizedPhrase("errors.invalid-syntax")));
                break;
            case PLAYER_NOT_FOUND:
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', localizationUtility.getLocalizedPhrase("errors.player-not-found").replaceAll("%player%", placeholderReplacement)));
                break;
            case TARGET_OFFLINE:
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', localizationUtility.getLocalizedPhrase("errors.player-offline").replaceAll("%player%", placeholderReplacement)));
                break;
        }
    }

    /**
     * Send a message (with color codes) to a player.
     *
     * @param target  the player who receives the message
     * @param message the message to send
     * @
     */
    public void sendMessage(CommandSender target, String message) {
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

}
