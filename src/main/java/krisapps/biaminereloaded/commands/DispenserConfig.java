package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DispenserConfig implements CommandExecutor {

    BiamineReloaded main;

    public DispenserConfig(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /dispenser <gameID> <addItem|removeItem|show> [amount]
        if (args.length >= 2) {
            String gameID = args[0];
            String operation = args[1];

            switch (operation) {
                case "addItem":
                    if (args.length >= 4) {
                        String itemName = args[2].toUpperCase();
                        int amount = Integer.parseInt(args[3]);

                        if (amount < 0) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.additem-negative"));
                            return true;
                        }

                        Material itemMaterial = Material.getMaterial(itemName);

                        if (itemMaterial == null) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.additem-unknown")
                                    .replaceAll("%item%", itemName)
                            );
                            return true;
                        }


                        ItemStack item = new ItemStack(itemMaterial);
                        item.setAmount(amount);

                        main.dataUtility.addItemToDispense(gameID, item);
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.additem-success")
                                .replaceAll("%item%", capitalize(item.getType().name().replace("_", " ")))
                                .replaceAll("%count%", String.valueOf(amount))
                                .replaceAll("%game%", gameID)
                        );
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.additem-error-insuff"));
                    }
                    break;
                case "removeItem":
                    if (args.length >= 3) {
                        String itemName = args[2].toUpperCase();
                        Material itemMaterial = Material.getMaterial(itemName);
                        if (main.dataUtility.isInDispenserList(gameID, itemMaterial)) {
                            main.dataUtility.removeItemToDispense(gameID, itemMaterial);
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.removeitem-success")
                                    .replaceAll("%item%", capitalize(itemMaterial.name().replace("_", " ")))
                                    .replaceAll("%game%", gameID)
                            );
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.removeitem-notfound")
                                    .replaceAll("%item%", capitalize(itemMaterial.name().replace("_", " ")))
                                    .replaceAll("%game%", gameID)
                            );
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.removeitem-error-insuff"));
                    }
                    break;
                case "show":
                    List<ItemStack> dispenserList = main.dataUtility.getItemsToDispense(gameID);
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.show-notice")
                            .replaceAll("%game%", gameID)
                    );
                    if (!dispenserList.isEmpty()) {
                        for (ItemStack item : dispenserList) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.show-item")
                                    .replaceAll("%item%", capitalize(item.getType().name().replace("_", " ")))
                                    .replaceAll("%count%", String.valueOf(item.getAmount()))
                            );
                        }
                    } else {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.show-empty"));
                    }
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.dispenser.show-footer"));
                    break;
            }

        }
        return true;
    }

    private String capitalize(String str) {
        return String.valueOf(str.charAt(0)).toUpperCase() + str.toLowerCase().substring(1, str.length());
    }
}
