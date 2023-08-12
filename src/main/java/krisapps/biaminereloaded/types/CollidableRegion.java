package krisapps.biaminereloaded.types;

import org.bukkit.Location;
import org.bukkit.World;

public class CollidableRegion {

    private final String regionName;
    private final boolean isFinish;
    private final int[] lowerBound = new int[3];
    private final int[] upperBound = new int[3];

    private final World world;

    public CollidableRegion(Location lowerBound, Location upperBound, String regionName) {
        this.lowerBound[0] = lowerBound.getBlockX();
        this.lowerBound[1] = lowerBound.getBlockY();
        this.lowerBound[2] = lowerBound.getBlockZ();

        this.upperBound[0] = upperBound.getBlockX();
        this.upperBound[1] = upperBound.getBlockY();
        this.upperBound[2] = upperBound.getBlockZ();

        this.regionName = regionName;
        this.world = upperBound.getWorld();
        this.isFinish = false;
    }

    public CollidableRegion(Location lowerBound, Location upperBound, String regionName, boolean isFinish) {
        this.lowerBound[0] = lowerBound.getBlockX();
        this.lowerBound[1] = lowerBound.getBlockY();
        this.lowerBound[2] = lowerBound.getBlockZ();

        this.upperBound[0] = upperBound.getBlockX();
        this.upperBound[1] = upperBound.getBlockY();
        this.upperBound[2] = upperBound.getBlockZ();

        this.regionName = regionName;
        this.world = upperBound.getWorld();
        this.isFinish = isFinish;
    }

    public static CollidableRegion of(Location loc, Location loc2) {
        return new CollidableRegion(
                loc,
                loc2,
                "Unnamed region"
        );
    }

    public static CollidableRegion of(Location loc, Location loc2, String regionName, boolean isFinish) {
        return new CollidableRegion(
                loc,
                loc2,
                regionName,
                isFinish
        );
    }

    public int[] getLowerBound() {
        return this.lowerBound;
    }

    public int[] getUpperBound() {
        return this.upperBound;
    }

    public Location getUpperBoundLocation() {
        return new Location(
                world,
                this.upperBound[0],
                this.upperBound[1],
                this.upperBound[2]
        );
    }

    public Location getLowerBoundLocation() {
        return new Location(
                world,
                this.lowerBound[0],
                this.lowerBound[1],
                this.lowerBound[2]
        );
    }

    public String getRegionName() {
        return this.regionName;
    }

    public boolean isFinish() {
        return this.isFinish;
    }

    public World getWorld() {
        return world;
    }
}
