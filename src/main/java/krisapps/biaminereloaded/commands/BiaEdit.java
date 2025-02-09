package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.config.GameProperty;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BiaEdit implements CommandExecutor {

    BiamineReloaded main;

    public BiaEdit(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /biaedit <game> <property> <newValue>
        if (args.length >= 3) {
            String gameID = args[0];
            String property = args[1];

            if (!main.dataUtility.gameExists(gameID)) {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biaedit.error-nogame")
                        .replaceAll("%game%", gameID)
                );
                return true;
            }
            switch (property) {
                case "display_name":
                    StringBuilder displayName = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        displayName.append(args[i]).append(" ");
                    }

                    if (!displayName.toString().isEmpty()) {
                        String oldName = main.dataUtility.getGameProperty(gameID, GameProperty.DISPLAY_NAME);
                        main.dataUtility.setGameProperty(gameID,
                                GameProperty.DISPLAY_NAME,
                                displayName.toString().trim()
                        );
                        String newName = main.dataUtility.getGameProperty(gameID, GameProperty.DISPLAY_NAME);

                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biaedit.set-success")
                                .replaceAll("%property%", property)
                                .replaceAll("%game%", gameID)
                                .replaceAll("%old%", oldName)
                                .replaceAll("%new%", newName)
                        );
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biaedit.set-fail-empty")
                                .replaceAll("%property%", property)
                        );
                    }
                    break;
                case "final_countdown":
                    int oldCountdown = Integer.parseInt(main.dataUtility.getGameProperty(gameID, GameProperty.COUNTDOWN_TIME));
                    int newVal = Integer.parseInt(args[2]);

                    if (newVal > 0) {
                        main.dataUtility.setGameProperty(gameID, GameProperty.COUNTDOWN_TIME, String.valueOf(newVal));
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biaedit.set-success")
                                .replaceAll("%property%", property)
                                .replaceAll("%game%", gameID)
                                .replaceAll("%old%", String.valueOf(oldCountdown))
                                .replaceAll("%new%", String.valueOf(newVal))
                        );
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biaedit.set-fail-notzero")
                                .replaceAll("%property%", property)
                        );
                    }
                    break;
                case "prep_duration":
                    int oldDuration = Integer.parseInt(main.dataUtility.getGameProperty(gameID, GameProperty.COUNTDOWN_TIME));
                    int newDuration = Integer.parseInt(args[2]);

                    if (newDuration > 0) {
                        main.dataUtility.setGameProperty(gameID, GameProperty.PREPARATION_TIME, String.valueOf(newDuration));
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biaedit.set-success")
                                .replaceAll("%property%", property)
                                .replaceAll("%game%", gameID)
                                .replaceAll("%old%", String.valueOf(oldDuration))
                                .replaceAll("%new%", String.valueOf(newDuration))
                        );
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biaedit.set-fail-notzero")
                                .replaceAll("%property%", property)
                        );
                    }
                    break;
                default:
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biaedit.error-invalid-property")
                            .replaceAll("%property%", property)
                    );
                    return true;
            }
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.biaedit.insuff"));
        }


        return true;
    }
}
