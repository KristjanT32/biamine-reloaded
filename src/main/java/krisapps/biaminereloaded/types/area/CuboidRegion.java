package krisapps.biaminereloaded.types.area;

import org.bukkit.Location;

public class CuboidRegion {

    private final Location bound1;
    private final Location bound2;

    public CuboidRegion(Location bound1, Location bound2) {
        this.bound1 = bound1;
        this.bound2 = bound2;
    }

    public Location getBound1() {
        return bound1;
    }

    public Location getBound2() {
        return bound2;
    }
}
