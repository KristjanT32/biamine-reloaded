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
        // Syntax: /terminate <game> [reason]
        if (args.length >= 1) {
            String gameID = args[0];
            if (main.dataUtility.getActiveGame() == null) {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.terminate.noactive"));
                return true;
            }

            if (args.length >= 2) {
                if (main.dataUtility.getActiveGame().equalsIgnoreCase(gameID)) {
                    StringBuilder reason = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        reason.append(args[i]).append(" ");
                    }

                    main.gameUtility.terminateGame(gameID);
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.terminate.success-reason")
                            .replaceAll("%game%", gameID)
                            .replaceAll("%reason%", reason.toString().trim())
                    );
                } else {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.terminate.noch"));
                }
            } else {
                if (main.dataUtility.getActiveGame().equalsIgnoreCase(gameID)) {
                    main.gameUtility.terminateGame(gameID);
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.terminate.success")
                            .replaceAll("%game%", gameID)
                    );
                } else {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.terminate.noch"));
                }
            }

        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.terminate.insuff"));
        }


        return true;
    }
}
