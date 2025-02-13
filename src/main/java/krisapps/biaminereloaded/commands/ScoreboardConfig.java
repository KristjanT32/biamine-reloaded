package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.gameloop.Game;
import krisapps.biaminereloaded.scoreboard.ScoreboardLine;
import krisapps.biaminereloaded.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.List;
import java.util.Set;

public class ScoreboardConfig implements CommandExecutor {

    BiamineReloaded main;

    public ScoreboardConfig(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /sconfig <create|edit|delete|list>
        //                  create: <id>
        //                  edit: <id> <property> <operation [moveToLine, raiseBy, lowerBy, changeTo]> <numberOfLines|newProperty>
        //                  delete: <id>
        //                  list: <none>

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "create":
                    // Creating configs.
                    if (args.length >= 2) {
                        String id = args[1];
                        // If there's not already a configuration with the same name
                        if (!main.dataUtility.scoreboardConfigExists(id)) {
                            if (main.dataUtility.createScoreboardConfig(id)) {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.create-success-creation").replaceAll("%id%", id));
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.create-error-save"));
                            }
                        } else {
                            if (args.length >= 3) {
                                if (args[2].equalsIgnoreCase("-f")) {
                                    if (main.dataUtility.createScoreboardConfig(id)) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.create-success-creation").replaceAll("%id%", id));
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.create-error-save"));
                                    }
                                } else {
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.create-error-unknownswitch"));
                                }
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.create-error-exist"));
                            }
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.create-error-insuff"));
                    }
                    break;
                case "edit":
                    // Editing configs.
                    if (args.length >= 5 || (args.length == 4 && args[3].equalsIgnoreCase("clear"))) {
                        String id = args[1];
                        String property = args[2];
                        String operation = args[3];
                        String newValue = "";
                        try {
                            newValue = args[4];
                        } catch (IndexOutOfBoundsException ignored) {
                        }

                        if (main.dataUtility.scoreboardConfigExists(id)) {
                            switch (operation) {
                                case "moveTo":
                                    if (id.equalsIgnoreCase("default")) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.error-editdefault"));
                                        return true;
                                    }

                                    // Move a property to a different line.
                                    int propertyLine = main.dataUtility.getPropertyLineNumber(id, property);
                                    int newLine = Integer.parseInt(newValue);

                                    if (newLine > 8) {
                                        newLine = 8;
                                    }
                                    // If any of the line numbers are 404, that means that one of the properties was not found and therefore, the operation should be terminated.
                                    if (propertyLine == 404 || newLine == 404) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-findprop"));
                                        return true;
                                    }
                                    // If the switch method returns true, all succeeded.
                                    if (main.dataUtility.switchScoreboardProperties(id, propertyLine, newLine)) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-success-moveto")
                                                .replaceAll("%target%", property)
                                                .replaceAll("%newline%", String.valueOf(newLine))
                                        );
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-save"));
                                    }
                                    break;

                                case "raiseBy":
                                    if (id.equalsIgnoreCase("default")) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.error-editdefault"));
                                        return true;
                                    }

                                    // Raise a property by a number of lines.
                                    int currentLine = main.dataUtility.getPropertyLineNumber(id, property);
                                    int finalLine = currentLine - Integer.parseInt(newValue);

                                    if (finalLine < 1) {
                                        finalLine = 1;
                                    }

                                    // If currentLine is over 8, the property was not found. Terminate everything.
                                    if (currentLine >= 404) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-findprop"));
                                        return true;
                                    }
                                    // If the switch method returns true, all succeeded.
                                    if (main.dataUtility.switchScoreboardProperties(id, currentLine, finalLine)) {
                                        if (currentLine != finalLine) {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-success-raiselower")
                                                    .replaceAll("%target%", property)
                                                    .replaceAll("%operation%", operation)
                                                    .replaceAll("%targetLine%", String.valueOf(currentLine))
                                                    .replaceAll("%modifier%", newValue)
                                            );
                                        } else {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-noch-raise"));
                                        }
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-save"));
                                    }
                                    break;

                                case "lowerBy":
                                    if (id.equalsIgnoreCase("default")) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.error-editdefault"));
                                        return true;
                                    }

                                    // Lower a property by a number of lines.
                                    int currentLine_2 = main.dataUtility.getPropertyLineNumber(id, property);
                                    int finalLine_2 = currentLine_2 + Integer.parseInt(newValue);

                                    if (finalLine_2 > 8) {
                                        finalLine_2 = 8;
                                    }

                                    // If currentLine is over 8, the property was not found. Terminate everything.
                                    if (currentLine_2 >= 404) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-findprop"));
                                        return true;
                                    }
                                    // If the switch method returns true, all succeeded.
                                    if (main.dataUtility.switchScoreboardProperties(id, currentLine_2, finalLine_2)) {
                                        if (currentLine_2 != finalLine_2) {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-success-raiselower")
                                                    .replaceAll("%target%", property)
                                                    .replaceAll("%targetLine%", String.valueOf(currentLine_2))
                                                    .replaceAll("%operation%", operation)
                                                    .replaceAll("%modifier%", newValue)
                                            );
                                        } else {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-noch-lower"));
                                        }
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-save"));
                                    }
                                    break;

                                case "changeTo":
                                    if (id.equalsIgnoreCase("default")) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.error-editdefault"));
                                        return true;
                                    }
                                    // Change a property on a provided line.
                                    int propertyLineNumber = main.dataUtility.getPropertyLineNumber(id, property);

                                    // If any of the line numbers are 404, that means that one of the properties was not found and therefore, the operation should be terminated.
                                    if (propertyLineNumber == 404) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-findprop"));
                                        return true;
                                    }
                                    // If the switch method returns true, all succeeded.
                                    if (main.dataUtility.overwriteScoreboardProperty(id, propertyLineNumber, newValue)) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-success-change")
                                                .replaceAll("%old%", String.valueOf(propertyLineNumber))
                                                .replaceAll("%newprop%", newValue)
                                        );
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-save"));
                                    }
                                    break;

                                case "setPropertyTo":
                                    if (id.equalsIgnoreCase("default")) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.error-editdefault"));
                                        return true;
                                    }

                                    ScoreboardLine line;
                                    StringBuilder replacement = new StringBuilder();
                                    for (int i = 4; i < args.length; i++) {
                                        replacement.append(args[i]).append(" ");
                                    }

                                    try {
                                        if (!property.equalsIgnoreCase("title")) {
                                            line = ScoreboardLine.valueOf(property.toUpperCase());
                                        } else {
                                            line = ScoreboardLine.LINE0;
                                        }
                                    } catch (IllegalArgumentException e) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-unknownline"));
                                        return true;
                                    }
                                    String editType;
                                    if (List
                                            .of(ScoreboardManager.getSupportedPlaceholders())
                                            .contains(replacement.toString().trim())) {
                                        editType = main.localizationUtility.getLocalizedPhrase(
                                                "commands.sconfig.edit-types.built-in");
                                    } else {
                                        editType = main.localizationUtility.getLocalizedPhrase(
                                                "commands.sconfig.edit-types.text-or-global");
                                    }
                                    // If the line edited was in fact the title
                                    if (line.equals(ScoreboardLine.LINE0)) {
                                        if (main.dataUtility.overwriteScoreboardProperty(id, line.asNumber(), replacement.toString().trim())) {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-titleedit")
                                                                                                            .replaceAll(
                                                                                                                    "%newValue%",
                                                                                                                    replacement
                                                                                                                            .toString()
                                                                                                                            .trim()
                                                                                                            )
                                            );
                                        } else {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-save"));
                                        }
                                        // If the line edited was not the title (usual)
                                    } else {
                                        if (main.dataUtility.overwriteScoreboardProperty(id, line.asNumber(), replacement.toString().trim())) {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-customedit")
                                                    .replaceAll("%lineNumber%", String.valueOf(line.asNumber() != 0 ? line.asNumber() : "title"))
                                                    .replaceAll("%editType%", editType)
                                                                                                            .replaceAll(
                                                                                                                    "%newValue%",
                                                                                                                    replacement
                                                                                                                            .toString()
                                                                                                                            .trim()
                                                                                                            )
                                            );
                                        } else {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-save"));
                                        }
                                    }
                                    break;
                                case "clear":
                                    if (id.equalsIgnoreCase("default")) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.error-editdefault"));
                                        return true;
                                    }

                                    ScoreboardLine lineToClear;
                                    try {
                                        lineToClear = ScoreboardLine.valueOf(property.toUpperCase());
                                    } catch (IllegalArgumentException e) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-unknownline"));
                                        return true;
                                    }
                                    if (main.dataUtility.overwriteScoreboardProperty(id, lineToClear.asNumber(), "%empty%")) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-linecleared")
                                                .replaceAll("%lineNumber%", String.valueOf(lineToClear.asNumber()))
                                        );
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-save"));
                                    }
                                    break;

                                default:
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.invalid-property-operation")
                                            .replaceAll("%operation%", operation)
                                            .replaceAll("%property%", property));
                                    break;
                            }
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-notfound").replaceAll("%configuration%", id));
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-insuff"));
                    }
                    break;
                case "delete":
                    // Deleting configs.
                    if (args.length >= 2) {
                        String id = args[1];
                        if (id.equalsIgnoreCase("default")) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.error-editdefault"));
                            return true;
                        }
                        if (main.dataUtility.scoreboardConfigExists(id)) {
                            if (main.dataUtility.deleteScoreboardConfig(id)) {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.delete-success-deleted").replaceAll("%id%", id));
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.delete-error-delete"));
                            }
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.delete-error-notfound").replaceAll("%configuration%", id));
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.delete-error-insuff"));
                    }
                    break;

                case "list":
                    // Listing configurations.
                    Set<String> configs = main.dataUtility.getScoreboardConfigs();
                    main.messageUtility.sendMessage(sender, "&b=======================================");
                    if (configs.size() > 0) {
                        for (String config : configs) {
                            List<String> gamesUsing = main.dataUtility.getGamesWithScoreboardConfig(config);
                            if (gamesUsing.isEmpty()) {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.list-item")
                                        .replaceAll("%item%", config)
                                );
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.list-item-uses")
                                        .replaceAll("%item%", config)
                                        .replaceAll("%uses%",
                                                gamesUsing.size() >= 2
                                                        ? gamesUsing.get(0) + ", " + gamesUsing.get(1) + "...and " + ((gamesUsing.size() - 2) > 0 ? (gamesUsing.size() - 2) + " more" : "")
                                                        : gamesUsing.get(0)
                                        ));
                            }
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.list-empty"));
                    }
                    main.messageUtility.sendMessage(sender, "&b=======================================");
                    break;

                case "show":
                    if (args.length >= 2) {
                        String id = args[1];
                        BukkitScheduler scheduler = Bukkit.getScheduler();
                        if (main.dataUtility.scoreboardConfigExists(id)) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.show-showing")
                                    .replaceAll("%configuration%", id)
                            );
                            if (Game.instance != null) {
                                Game.instance.getScoreboardManager().previewScoreboard(id);
                                scheduler.scheduleSyncDelayedTask(main, () -> Game.instance.getScoreboardManager().clearPreview(), 20L * 5);
                            } else {
                                ScoreboardManager scoreboardManager = new ScoreboardManager(main);
                                scoreboardManager.previewScoreboard(id);
                                scheduler.scheduleSyncDelayedTask(main, scoreboardManager::clearPreview, 20L * 5);
                            }
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.show-error-notfound").replaceAll("%configuration%", id));
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.show-error-insuff"));
                    }
                    break;

                case "assign":
                    if (args.length >= 3) {

                        // If the -ra switch is provided, first remove the scoreboard config from all other games, then assign.
                        if (args.length == 4) {
                            String sconfig = args[1];
                            String target = args[2];
                            if (args[3].equalsIgnoreCase("-ra")) {
                                main.dataUtility.unassignScoreboardConfigFromAll(sconfig);
                                main.dataUtility.assignScoreboardConfiguration(sconfig, target);
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.assign-success-reassign")
                                        .replaceAll("%config%", sconfig)
                                        .replaceAll("%target%", target)
                                );
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.assign-error-invalidswitch").replaceAll("%switch%", args[3]));
                            }
                        } else {
                            String sconfig = args[1];
                            String target = args[2];
                            main.dataUtility.assignScoreboardConfiguration(sconfig, target);
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.assign-success")
                                    .replaceAll("%config%", sconfig)
                                    .replaceAll("%target%", target)
                            );
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.assign-error-insuff"));
                    }
                    break;
                default:
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.invalid-operation").replaceAll("%operation%", args[0]));
                    break;
            }

        } else {
            return false;
        }
        return true;
    }
}
