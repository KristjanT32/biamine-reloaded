package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Terminate implements CommandExecutor {

    BiamineReloaded main;

    public Terminate(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /terminate [reason]
        StringBuilder reason = new StringBuilder();
        if (args.length >= 1) {
            for (int i = 0; i < args.length; i++) {
                reason.append(args[i]).append(" ");
            }
        }
            if (main.dataUtility.getActiveGame() == null) {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.terminate.noactive"));
                return true;
            } else {
                main.gameUtility.terminateGame();
                if (!reason.toString().isEmpty()) {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.terminate.success-reason")
                            .replaceAll("%game%", main.gameUtility.getActiveGameID())
                            .replaceAll("%reason%", reason.toString().trim())
                    );
                } else {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.terminate.success")
                            .replaceAll("%game%", main.gameUtility.getActiveGameID())
                    );
                }
            }
        return true;
    }
}
