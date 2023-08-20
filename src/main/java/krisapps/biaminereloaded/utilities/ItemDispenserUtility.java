package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
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
            for (ItemStack existingItem : inventory.getContents()) {
                if (existingItem.getType() == item.getType()) {
                    if (existingItem.getAmount() > item.getAmount() || existingItem.getAmount() < item.getAmount()) {
                        inventory.remove(existingItem);
                        inventory.addItem(item);
                        main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.gave")
                                .replaceAll("%item%", capitalize(item.getType().name().replace("_", " ")))
                                .replaceAll("%count%", String.valueOf(item.getAmount()))
                        );
                    } else if (existingItem.getAmount() == item.getAmount()) {
                        skippedItems++;
                    }
                }
            }
        }
        if (skippedItems == items.size()) {
            main.messageUtility.sendActionbarMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.finished-skip")
                    .replaceAll("%skipped%", String.valueOf(skippedItems))
            );
        } else if (skippedItems == 0) {
            main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.finished"));
        } else if (skippedItems > 0 && skippedItems < items.size()) {
            main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.finished-skipped-some"));
        }
    }

    public void dispenseToAll(List<Player> players, List<ItemStack> items) {
        for (Player player : players) {
            PlayerInventory inventory = player.getInventory();
            for (ItemStack item : items) {
                if (!hasItem(inventory, item)) {
                    inventory.addItem(item);
                    main.messageUtility.sendMessage(player, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.gave")
                            .replaceAll("%item%", capitalize(item.getType().name().replace("_", " ")))
                            .replaceAll("%count%", String.valueOf(item.getAmount()))
                    );
                } else {
                    for (ItemStack foundItem : findExisting(inventory, item)) {
                        inventory.remove(foundItem);
                    }
                    inventory.addItem(item);
                    main.messageUtility.sendMessage(player, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.itemdispense.gave")
                            .replaceAll("%item%", capitalize(item.getType().name().replace("_", " ")))
                            .replaceAll("%count%", String.valueOf(item.getAmount()))
                    );
                }
            }
        }
    }

    private String capitalize(String str) {
        return String.valueOf(str.charAt(0)).toUpperCase() + str.toLowerCase().substring(1, str.length());
    }

    private boolean hasItem(Inventory inventory, ItemStack itemStack) {
        for (ItemStack existing : inventory.getContents()) {
            if (existing == null) {
                continue;
            }
            if (existing.isSimilar(itemStack)) {
                return true;
            }
        }
        return false;
    }

    private List<ItemStack> findExisting(Inventory inventory, ItemStack target) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack existing : inventory.getContents()) {
            if (existing == null) {
                continue;
            }
            if (existing.isSimilar(target)) {
                items.add(existing);
            }
        }
        return items;
    }


}
