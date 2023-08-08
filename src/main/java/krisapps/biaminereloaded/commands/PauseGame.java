package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PauseGame implements CommandExecutor {

    BiamineReloaded main;

    public PauseGame(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /pausegame <gameID>
        if (args.length >= 1) {
            main.gameUtility.pauseGame(args[0]);
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.pause.done"));
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.pause.insuff"));
        }

        return true;
    }
}
