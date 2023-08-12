package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ResumeGame implements CommandExecutor {

    BiamineReloaded main;

    public ResumeGame(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /resumegame <gameID>
        if (args.length >= 1) {
            int result = main.gameUtility.resumeGame();
            if (result == 200) {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.resume.done"));
            } else if (result == 404) {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.resume.not-paused"));
            } else if (result == 500) {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.resume.no-resumables"));
            }
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.resume.insuff"));
        }

        return true;
    }
}
