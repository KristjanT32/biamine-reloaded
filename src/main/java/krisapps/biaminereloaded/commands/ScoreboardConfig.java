package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.ScoreboardLine;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
                    // operations: moveTo, raiseBy, lowerBy, changeTo
                    // properties: timer, playersParticipating, playersNotFinished, shootings, header, footer

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
                                    ScoreboardLine line;
                                    String replacement = args[4];
                                    try {
                                        line = ScoreboardLine.valueOf(property.toUpperCase());
                                    } catch (IllegalArgumentException e) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-unknownline"));
                                        return true;
                                    }
                                    String editType;
                                    if (List.of("timer", "playersParticipating", "playersNotFinished", "shootings", "header", "footer").contains(replacement)) {
                                        editType = "built-in placeholder";
                                    } else {
                                        editType = "custom text or global placeholder";
                                    }
                                    if (main.dataUtility.overwriteScoreboardProperty(id, line.asNumber(), replacement)) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-customedit")
                                                .replaceAll("%lineNumber%", String.valueOf(line.asNumber()))
                                                .replaceAll("%editType%", editType)
                                                .replaceAll("%newValue%", replacement)
                                        );
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-error-save"));
                                    }
                                    break;
                                case "clear":
                                    ScoreboardLine lineToClear;
                                    try {
                                        lineToClear = ScoreboardLine.valueOf(property.toUpperCase());
                                    } catch (IllegalArgumentException e) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.edit-unknownline"));
                                        return true;
                                    }
                                    if (main.dataUtility.overwriteScoreboardProperty(id, lineToClear.asNumber(), "empty")) {
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
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.list-item").replaceAll("%item%", config));
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.list-empty"));
                    }
                    main.messageUtility.sendMessage(sender, "&b=======================================");
                    break;

                case "show":
                    if (args.length >= 2) {
                        String id = args[1];
                        if (main.dataUtility.scoreboardConfigExists(id)) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.show-result")
                                    .replaceAll("%id%", id)
                                    .replaceAll("%line1%", main.dataUtility.getScoreboardConfigProperty(id, ScoreboardLine.LINE1))
                                    .replaceAll("%line2%", main.dataUtility.getScoreboardConfigProperty(id, ScoreboardLine.LINE2))
                                    .replaceAll("%line3%", main.dataUtility.getScoreboardConfigProperty(id, ScoreboardLine.LINE3))
                                    .replaceAll("%line4%", main.dataUtility.getScoreboardConfigProperty(id, ScoreboardLine.LINE4))
                                    .replaceAll("%line5%", main.dataUtility.getScoreboardConfigProperty(id, ScoreboardLine.LINE5))
                                    .replaceAll("%line6%", main.dataUtility.getScoreboardConfigProperty(id, ScoreboardLine.LINE6))
                                    .replaceAll("%line7%", main.dataUtility.getScoreboardConfigProperty(id, ScoreboardLine.LINE7))
                                    .replaceAll("%line8%", main.dataUtility.getScoreboardConfigProperty(id, ScoreboardLine.LINE8))
                            );
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.show-error-notfound").replaceAll("%configuration%", id));
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.sconfig.show-error-insuff"));
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
