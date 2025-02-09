package krisapps.biaminereloaded.gameloop.types;

import org.bukkit.inventory.ItemStack;

public class DispenserEntry {
    ItemStack item;
    boolean isAuto;

    public DispenserEntry(ItemStack item, boolean isAuto) {
        this.item = item;
        this.isAuto = isAuto;
    }

    public ItemStack getItem() {
        return item;
    }

    public boolean isAuto() {
        return isAuto;
    }


}
