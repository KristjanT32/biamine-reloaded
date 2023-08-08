package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.CollidableRegion;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckpointConfig implements CommandExecutor {

    BiamineReloaded main;

    public CheckpointConfig(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /checkpoint <gameID> <add|remove|find|list|setbound> <none|id|bound>
        if (!(sender instanceof Player)) {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("errors.player-only"));
        } else {
            if (args.length >= 2) {
                String gameID = args[0];
                String operation = args[1];
                if (main.dataUtility.gameExists(gameID)) {
                    switch (operation) {
                        case "add":
                            main.dataUtility.addCheckpoint(
                                    gameID,
                                    "checkpoint-" + (main.dataUtility.getCheckpoints(gameID).isEmpty() ? "1" : main.dataUtility.getCheckpoints(gameID).size() + 1),
                                    null,
                                    null
                            );
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.success-add")
                                    .replaceAll("%checkpoints%", String.valueOf(main.dataUtility.getCheckpoints(gameID).size()))
                            );
                            break;
                        case "remove":
                            if (args.length >= 3) {
                                main.dataUtility.deleteCheckpoint(gameID, args[2]);
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.success-remove")
                                        .replaceAll("%target%", args[2])
                                );
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.error-insuff"));
                            }
                            break;
                        case "find":
                            if (args.length >= 3) {
                                String checkpointID = args[2];
                                CollidableRegion region = main.dataUtility.getCheckpoint(gameID, checkpointID);
                                if (args.length >= 4) {
                                    if (args[3].equalsIgnoreCase("-t")) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.success-find-tp"));
                                        if (region.getUpperBoundLocation() != null) {
                                            ((Player) sender).teleport(region.getUpperBoundLocation());
                                        } else if (region.getLowerBound() != null) {
                                            ((Player) sender).teleport(region.getUpperBoundLocation());
                                        } else {
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.error-find-tp"));
                                        }
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.success-find")
                                                .replaceAll("%bound1%", region.getUpperBoundLocation() != null
                                                        ? "X: " + region.getUpperBoundLocation().getBlockX() + "\n"
                                                        + "Y: " + region.getUpperBoundLocation().getBlockY() + "\n"
                                                        + "Z: " + region.getUpperBoundLocation().getBlockZ()
                                                        : "not set"
                                                )
                                                .replaceAll("%bound2%", region.getUpperBoundLocation() != null
                                                        ? "X: " + region.getLowerBoundLocation().getBlockX() + "\n"
                                                        + "Y: " + region.getLowerBoundLocation().getBlockY() + "\n"
                                                        + "Z: " + region.getLowerBoundLocation().getBlockZ()
                                                        : "not set"
                                                ));
                                    }
                                } else {
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.success-find")
                                            .replaceAll("%bound1%", region.getUpperBoundLocation() != null
                                                    ? "X: " + region.getUpperBoundLocation().getBlockX() + "\n"
                                                    + "Y: " + region.getUpperBoundLocation().getBlockY() + "\n"
                                                    + "Z: " + region.getUpperBoundLocation().getBlockZ()
                                                    : "not set"
                                            )
                                            .replaceAll("%bound2%", region.getUpperBoundLocation() != null
                                                    ? "X: " + region.getLowerBoundLocation().getBlockX() + "\n"
                                                    + "Y: " + region.getLowerBoundLocation().getBlockY() + "\n"
                                                    + "Z: " + region.getLowerBoundLocation().getBlockZ()
                                                    : "not set"
                                            ));
                                }

                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.error-find-noid"));
                            }
                            break;
                        case "list":
                            main.messageUtility.sendMessage(sender, "&b=======================================");
                            if (!main.dataUtility.getCheckpoints(gameID).isEmpty()) {
                                for (String checkpoint : main.dataUtility.getCheckpoints(gameID)) {
                                    if (main.dataUtility.isFinishingCheckpoint(gameID, checkpoint)) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.list-item-finish")
                                                .replaceAll("%item%", checkpoint)
                                        );
                                    } else {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.list-item")
                                                .replaceAll("%item%", checkpoint)
                                                .replaceAll("%completeStatus%", main.dataUtility.checkpointSetup(gameID, checkpoint) ? "" : "&4[&cincomplete&4]")
                                        );
                                    }
                                }
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.list-empty"));
                            }
                            main.messageUtility.sendMessage(sender, "&b=======================================");
                            break;
                        case "setbound":
                            if (args.length >= 4) {
                                String checkpointID = args[2];
                                String bound = args[3];
                                Location location = ((Player) sender).getLocation();

                                int result = main.dataUtility.setCheckpointBoundary(gameID, checkpointID, bound, location);
                                if (result == 200) {
                                    switch (bound) {
                                        case "bound1":
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.setbound-success-set1")
                                                    .replaceAll("%x%", String.valueOf(location.getBlockX()))
                                                    .replaceAll("%y%", String.valueOf(location.getBlockY()))
                                                    .replaceAll("%z%", String.valueOf(location.getBlockZ()))
                                            );
                                            break;
                                        case "bound2":
                                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.setbound-success-set2")
                                                    .replaceAll("%x%", String.valueOf(location.getBlockX()))
                                                    .replaceAll("%y%", String.valueOf(location.getBlockY()))
                                                    .replaceAll("%z%", String.valueOf(location.getBlockZ()))
                                            );
                                            break;
                                    }
                                } else if (result == 404) {
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.setbound-error-notfound"));
                                }
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.setbound-insuff"));
                            }
                            break;
                        case "setname":
                            if (args.length >= 4) {
                                String checkpoint = args[2];
                                StringBuilder newName = new StringBuilder();
                                for (int i = 3; i < args.length; i++) {
                                    newName.append(args[i]).append(" ");
                                }

                                main.dataUtility.setCheckpointDisplayName(gameID, checkpoint, newName.toString().trim());
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.success-setname")
                                        .replaceAll("%name%", newName.toString().trim())
                                        .replaceAll("%checkpoint%", checkpoint)
                                );
                            } else {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.setname-error-insuff"));
                            }
                            break;
                        case "setfinish":
                            if (args.length >= 3) {
                                String checkpoint = args[2];
                                if (main.dataUtility.setFinish(gameID, checkpoint)) {
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.success-setfinish")
                                            .replaceAll("%checkpoint%", checkpoint)
                                    );
                                } else {
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.setfinish-error-generic"));
                                }
                            }
                            break;
                    }
                } else {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.checkpoint.error-nogame").replaceAll("%game%", args[0]));
                }
            }
        }


        return true;
    }
}
