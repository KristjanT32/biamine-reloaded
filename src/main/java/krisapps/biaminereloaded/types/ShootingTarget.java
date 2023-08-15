package krisapps.biaminereloaded.types;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class ShootingTarget {
    PersistentDataContainer dataContainer;

    public ShootingTarget(int rangeSpotNumber, int targetNumber, String gameID, Location location, Plugin main) {
        dataContainer = new CustomBlockData(location.getBlock(), main);

        int[] locationArray = new int[3];
        locationArray[0] = location.getBlockX();
        locationArray[1] = location.getBlockY();
        locationArray[2] = location.getBlockZ();

        dataContainer.set(new NamespacedKey(main, "location"),
                PersistentDataType.PrimitivePersistentDataType.INTEGER_ARRAY, locationArray);
        dataContainer.set(new NamespacedKey(main, "location_world"),
                PersistentDataType.STRING, location.getWorld().getName());
        dataContainer.set(new NamespacedKey(main, "target_number"),
                PersistentDataType.INTEGER, targetNumber);
        dataContainer.set(new NamespacedKey(main, "range_spot_number"),
                PersistentDataType.INTEGER, rangeSpotNumber);
        dataContainer.set(new NamespacedKey(main, "ownerGameID"),
                PersistentDataType.STRING, gameID);
    }

    public Location getLocation(Plugin main) {
        int[] locationArray = this.dataContainer.get(new NamespacedKey(main, "location"), PersistentDataType.INTEGER_ARRAY);
        World world = main.getServer().getWorld(this.dataContainer.get(new NamespacedKey(main, "location_world"), PersistentDataType.STRING));

        return new Location(world, locationArray[0], locationArray[1], locationArray[2]);
    }

    public int getShootingRangeSpotID(Plugin main) {
        Object containerResponse = this.dataContainer.get(new NamespacedKey(main, "range_spot_number"), PersistentDataType.INTEGER);
        if (containerResponse != null) {
            return Integer.parseInt(containerResponse.toString());
        } else {
            return -1;
        }
    }

    public int getTargetID(Plugin main) {
        Object containerResponse = this.dataContainer.get(new NamespacedKey(main, "target_number"), PersistentDataType.INTEGER);
        if (containerResponse != null) {
            return Integer.parseInt(containerResponse.toString());
        } else {
            return -1;
        }
    }

    public String getGame(Plugin main) {
        Object containerResponse = this.dataContainer.get(new NamespacedKey(main, "ownerGameID"), PersistentDataType.STRING);
        if (containerResponse != null) {
            return containerResponse.toString();
        } else {
            return null;
        }
    }
}
