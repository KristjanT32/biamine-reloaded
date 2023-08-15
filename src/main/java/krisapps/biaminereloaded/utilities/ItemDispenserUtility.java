package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;

public class ItemDispenserUtility {

    BiamineReloaded main;

    public ItemDispenserUtility(BiamineReloaded main) {
        this.main = main;
    }

    public void dispense(Player p, List<ItemStack> items) {
        PlayerInventory inventory = p.getInventory();
        int skippedItems = 0;
        for (ItemStack item : items) {
            if (inventory.containsAtLeast(item, item.getAmount())) {
                skippedItems++;
            } else {
                inventory.addItem(item);
                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.gave")
                        .replaceAll("%item%", capitalize(item.getType().name().replace("_", " ")))
                        .replaceAll("%count%", String.valueOf(item.getAmount()))
                );
            }
        }
        if (skippedItems > 0) {
            main.messageUtility.sendActionbarMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.finished-skip")
                    .replaceAll("%skipped%", String.valueOf(skippedItems))
            );
        } else {
            main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.finished"));
        }
    }

    public void dispenseToAll(List<Player> players, List<ItemStack> items) {
        for (Player player : players) {
            PlayerInventory inventory = player.getInventory();
            int skippedItems = 0;
            for (ItemStack item : items) {
                if (inventory.containsAtLeast(item, item.getAmount())) {
                    skippedItems++;
                } else {
                    inventory.addItem(item);
                    main.messageUtility.sendMessage(player, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.gave")
                            .replaceAll("%item%", capitalize(item.getType().name().replace("_", " ")))
                            .replaceAll("%count%", String.valueOf(item.getAmount()))
                    );
                }
            }
            if (skippedItems > 0) {
                main.messageUtility.sendActionbarMessage(player, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.finished-skip")
                        .replaceAll("%skipped%", String.valueOf(skippedItems))
                );
            }
        }
    }

    private String capitalize(String str) {
        return String.valueOf(str.charAt(0)).toUpperCase() + str.toLowerCase().substring(1, str.length());
    }


}
