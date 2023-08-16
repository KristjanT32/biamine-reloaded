package krisapps.biaminereloaded.types;

public class HitInfo {
    private final int target;
    private final int spot;
    private final HitType type;
    private final int arrowsRemaining;

    public HitInfo(int spot, HitType hitType, int arrowsRemaining) {
        this.spot = spot;
        this.type = hitType;
        this.arrowsRemaining = arrowsRemaining;
        this.target = -1;
    }

    public HitInfo(int target, int spot, HitType type, int arrowsRemaining) {
        this.target = target;
        this.spot = spot;
        this.type = type;
        this.arrowsRemaining = arrowsRemaining;
    }

    public int getTarget() {
        return target;
    }

    public int getSpot() {
        return spot;
    }

    public HitType getType() {
        return type;
    }

    public int getArrowsRemaining() {
        return arrowsRemaining;
    }
}
