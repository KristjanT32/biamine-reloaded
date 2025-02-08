package krisapps.biaminereloaded.types;

public class AreaPassInfo {

    boolean leftArea;
    private final String areaName;
    private final String timerTime;
    private final AreaType areaType;
    private final int reachedOnLap;


    public AreaPassInfo(String areaName, String timerTime, AreaType areaType, int reachedOnLap) {
        this.areaName = areaName;
        this.timerTime = timerTime;
        this.areaType = areaType;
        this.reachedOnLap = reachedOnLap;
        this.leftArea = false;
    }

    public AreaPassInfo(String areaName, String timerTime, boolean leftArea, AreaType areaType, int reachedOnLap) {
        this.areaName = areaName;
        this.timerTime = timerTime;
        this.leftArea = leftArea;
        this.areaType = areaType;
        this.reachedOnLap = reachedOnLap;
    }

    public String getAreaName() {
        return areaName;
    }

    public String getTimerTime() {
        return timerTime;
    }

    public boolean leftArea() {
        return leftArea;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public int getReachedOnLap() {
        return reachedOnLap;
    }
}
