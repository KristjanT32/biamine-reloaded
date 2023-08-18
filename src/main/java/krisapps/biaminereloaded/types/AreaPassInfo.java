package krisapps.biaminereloaded.types;

public class AreaPassInfo {

    boolean leftArea;
    private final String areaName;
    private final String timerTime;


    public AreaPassInfo(String areaName, String timerTime) {
        this.areaName = areaName;
        this.timerTime = timerTime;
        this.leftArea = false;
    }

    public AreaPassInfo(String areaName, String timerTime, boolean leftArea) {
        this.areaName = areaName;
        this.timerTime = timerTime;
        this.leftArea = leftArea;
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
}
