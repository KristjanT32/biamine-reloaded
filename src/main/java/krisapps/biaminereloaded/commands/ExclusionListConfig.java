package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class ExclusionListConfig implements CommandExecutor {

    BiamineReloaded main;

    public ExclusionListConfig(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /exclusionlist <<create|edit|assign|delete>
        //                          create: <id>
        //                          edit <excList> <addPlayer|removePlayer> <player>
        //                          assign: <excList> <gameID>
        //                          list: <none>
        //                          view: <id>
        //                          delete <excList>

        if (args.length >= 1) {
            String operation = args[0];
            switch (operation) {
                case "create":
                    if (args.length >= 2) {
                        String listID = args[1];
                        if (main.dataUtility.exclusionListExists(listID)) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-occupied")
                                    .replaceAll("%id%", listID)
                            );
                            return true;
                        }
                        if (main.dataUtility.createExclusionList(listID)) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.success-create")
                                    .replaceAll("%id%", listID)
                            );
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-create")
                                    .replaceAll("%id%", listID)
                            );
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-create-insuff"));
                    }
                    break;
                case "edit":
                    if (args.length >= 4) {
                        String listID = args[1];
                        String listOperation = args[2];
                        Player targetPlayer = Bukkit.getPlayer(args[3]);
                        OfflinePlayer offlinePlayer;
                        if (targetPlayer == null) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-edit-playeroffline"));
                            return true;
                        }

                        if (main.dataUtility.exclusionListExists(listID)) {
                            switch (listOperation) {
                                case "addPlayer":
                                    if (!main.dataUtility.getExcludedPlayers(listID).contains(targetPlayer.getUniqueId().toString())) {
                                        if (main.dataUtility.appendExcludedPlayer(targetPlayer, listID)) {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.success-addplayer")
                                                    .replaceAll("%player%", targetPlayer.getName())
                                            );
                                        } else {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-edit"));
                                        }
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-edit-playerthere")
                                                .replaceAll("%player%", targetPlayer.getName())
                                        );
                                    }
                                    break;
                                case "removePlayer":
                                    if (main.dataUtility.getExcludedPlayers(listID).contains(targetPlayer.getUniqueId().toString())) {
                                        if (main.dataUtility.removeExcludedPlayer(targetPlayer, listID)) {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.success-removeplayer")
                                                    .replaceAll("%player%", targetPlayer.getName())
                                            );
                                        } else {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-edit"));
                                        }
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-edit-playernotfound")
                                                .replaceAll("%player%", targetPlayer.getName())
                                        );
                                    }
                                    break;
                            }
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-edit-listnotfound")
                                    .replaceAll("%id%", listID)
                            );
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-edit-insuff"));
                    }
                    break;
                case "assign":
                    if (args.length >= 2) {
                        String listID = args[1];
                        String targetGame = args[2];

                        if (!main.dataUtility.exclusionListExists(listID)) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-assign-nonexlist")
                                    .replaceAll("%list%", listID)
                            );
                            return true;
                        }
                        if (!main.dataUtility.gameExists(targetGame)) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-assign-nonextarget")
                                    .replaceAll("%game%", targetGame)
                            );
                            return true;
                        }

                        if (main.dataUtility.assignExclusionList(listID, targetGame)) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.success-assign")
                                    .replaceAll("%list%", listID)
                                    .replaceAll("%game%", targetGame)
                            );
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-assign"));
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-assign-insuff"));
                    }
                    break;

                case "unassign":
                    if (args.length >= 2) {
                        String listID = args[1];
                        String targetGame = args[2];

                        if (!main.dataUtility.exclusionListExists(listID)) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-assign-nonexlist")
                                    .replaceAll("%list%", listID)
                            );
                            return true;
                        }
                        if (!main.dataUtility.gameExists(targetGame)) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-assign-nonextarget")
                                    .replaceAll("%game%", targetGame)
                            );
                            return true;
                        }
                        if (!main.dataUtility.getGamesWithExclusionList(listID).isEmpty()) {
                            if (main.dataUtility.unassignExclusionListFrom(listID, targetGame)) {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.success-unassign")
                                        .replaceAll("%list%", listID)
                                        .replaceAll("%game%", targetGame)
                                );
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-unassign"));
                            }
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-unassign-nogameswith"));
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-unassign-insuff"));
                    }
                    break;

                case "list":
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.list.header"));
                    if (main.dataUtility.getExclusionLists().isEmpty()) {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.list.empty"));
                    }
                    for (String listID : main.dataUtility.getExclusionLists()) {
                        int uses = main.dataUtility.getGamesWithExclusionList(listID).size();
                        if (uses > 0) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.list.item-used")
                                    .replaceAll("%list%", listID)
                                    .replaceAll("%uses%", String.valueOf(uses))
                            );
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.list.item")
                                    .replaceAll("%list%", listID)
                            );
                        }
                    }
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.list.header"));
                    break;
                case "view":
                    if (args.length >= 2) {
                        String targetList = args[1];
                        if (main.dataUtility.exclusionListExists(targetList)) {
                            if (main.dataUtility.getExcludedPlayers(targetList).isEmpty()) {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.view.empty"));
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.view.header"));
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.view.subtitle")
                                        .replaceAll("%list%", targetList)
                                );
                                ArrayList<Player> online = new ArrayList<>();
                                for (String playerUUID : main.dataUtility.getExcludedPlayers(targetList)) {

                                    Player bukkitPlayer = (Bukkit.getPlayer(UUID.fromString(playerUUID)));
                                    String playerName = bukkitPlayer.getName();
                                    if (bukkitPlayer.isOnline()) {
                                        online.add(bukkitPlayer);
                                    }

                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.view.item")
                                            .replaceAll("%player%", playerName == null ? "*unknown*" : (bukkitPlayer.isOnline() ? playerName : playerName + "&c*&r"))
                                            .replaceAll("%uuid%", playerUUID)
                                    );
                                }
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.view.footer")
                                        .replaceAll("%excluded%", String.valueOf(main.dataUtility.getExcludedPlayers(targetList).size()))
                                        .replaceAll("%online%", String.valueOf(online.size()))
                                );
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.view.header"));
                            }
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.view.not-found")
                                    .replaceAll("%list%", targetList)
                            );
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-view-insuff"));
                    }
                    break;
                case "delete":
                    if (args.length >= 2) {
                        String list = args[1];
                        if (main.dataUtility.exclusionListExists(list)) {
                            if (main.dataUtility.deleteExclusionList(list)) {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.success-delete")
                                        .replaceAll("%list%", list)
                                );
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-delete"));
                            }
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-delete-notfound")
                                    .replaceAll("%list%", list)
                            );
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.error-delete-insuff"));
                    }
                    break;
            }
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.exclusionlist.insuff"));
        }

        return true;
    }
}
