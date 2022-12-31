package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.GenericErrorType;
import krisapps.biaminereloaded.types.Placeholder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtility {

    BiamineReloaded main;
    LocalizationUtility localizationUtility;

    public MessageUtility(BiamineReloaded main) {
        this.main = main;
        localizationUtility = new LocalizationUtility(main);
    }

    public String replacePlaceholder(String inputString, Object replacement, Placeholder placeholderToReplace) {
        inputString = inputString.replaceAll(placeholderToReplace.getPlaceholder(), replacement.toString());
        return inputString;
    }


    /**
     * Send an error message without placeholders.
     *
     * @param target the player who receives the error message
     * @param type   the type of error message to send
     */
    public void sendError(CommandSender target, GenericErrorType type) {
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
        }
    }

    public void sendTargetOfflineError(CommandSender sender, Player p) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', localizationUtility.getLocalizedPhrase("errors.player-offline").replaceAll("%player%", p.getName())));
    }

    public void sendPlayerNotFoundError(CommandSender sender, Player p) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', localizationUtility.getLocalizedPhrase("errors.player-not-found").replaceAll("%player%", p.getName())));
    }

    /**
     * Send a message (with color codes) to a player.
     * Messages with a placeholder will not get
     * the placeholder replaced.
     *
     * @param target  the player who receives the message
     * @param message the message to send
     * @
     */
    public void sendMessage(CommandSender target, String message) {
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

}
