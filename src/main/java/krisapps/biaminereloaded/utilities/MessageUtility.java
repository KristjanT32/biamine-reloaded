package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.GenericErrorType;
import krisapps.biaminereloaded.types.Placeholder;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

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
            case INVALID_GAME:
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', localizationUtility.getLocalizedPhrase("errors.invalid-game")));
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

    /**
     * Sends a title with the color codes interpreted.
     *
     * @param target   the player to send the title to.
     * @param title    the title content
     * @param subtitle the subtitle content
     * @param fadeIn   time for the title to fade in for (in ticks)
     * @param stay     time for the title to stay on screen for (in ticks)
     * @param fadeOut  time for the title to fade out for (in ticks)
     */
    public void sendTitle(Player target, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        target.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subtitle), fadeIn, stay, fadeOut);
    }

    public void sendActionbarMessage(Player target, String text) {
        target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', text)));
    }

    public BaseComponent createClickableTeleportButton(String textPath, Location target, @Nullable String hoverTextPath) {
        BaseComponent[] component = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase(textPath)));
        TextComponent out = new TextComponent(component);
        out.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @p " + target.getBlockX() + " " + target.getBlockY() + " " + target.getBlockZ()));
        if (hoverTextPath != null) {
            out.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase(hoverTextPath)))));
        }
        return out;
    }

    public BaseComponent createFileButton(String textPath, String filePath, @Nullable String hoverTextPath) {
        BaseComponent[] component = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase(textPath)));
        TextComponent out = new TextComponent(component);
        if (hoverTextPath != null) {
            out.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase(hoverTextPath).replaceAll("%path%", filePath)))));
        }
        return out;
    }

    public TextComponent createClickableButton(String textPath, String command, @Nullable String hoverTextPath) {
        BaseComponent[] component = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase(textPath)));
        TextComponent out = new TextComponent(component);
        out.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        if (hoverTextPath != null) {
            out.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase(hoverTextPath)))));
        }
        return out;
    }

}
