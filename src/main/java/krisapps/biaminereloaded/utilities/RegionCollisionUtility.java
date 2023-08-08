package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.CollidableRegion;
import org.bukkit.Location;

public class RegionCollisionUtility {

    BiamineReloaded main;

    public RegionCollisionUtility(BiamineReloaded main) {
        this.main = main;
    }

    public boolean checkIntersect(Location playerLocation, CollidableRegion region) {

        int pX = playerLocation.getBlockX();
        int pY = playerLocation.getBlockY();
        int pZ = playerLocation.getBlockZ();

        int x1 = Math.min(region.getLowerBound()[0], region.getUpperBound()[0]);
        int y1 = Math.min(region.getLowerBound()[1], region.getUpperBound()[1]);
        int z1 = Math.min(region.getLowerBound()[2], region.getUpperBound()[2]);

        int x2 = Math.max(region.getLowerBound()[0], region.getUpperBound()[0]);
        int y2 = Math.max(region.getLowerBound()[1], region.getUpperBound()[1]);
        int z2 = Math.max(region.getLowerBound()[2], region.getUpperBound()[2]);

        return pX >= x1 && pX <= x2
                && pY >= y1 && pY <= y2
                && pZ >= z1 && pZ <= z2;
    }


}
