package krisapps.biaminereloaded.game_setup;

public class BiamineBiathlon {
    public int shootingsCount;
    public int totalPlayers;
    public int finishedPlayers;
    public String latestTime;
    public String scoreboardConfig;
    public String exclusionList;

    public BiamineBiathlon(int shootingsCount, int totalPlayers, int finishedPlayers, String latestTime, String scoreboardConfig, String exclusionList) {
        this.shootingsCount = shootingsCount;
        this.totalPlayers = totalPlayers;
        this.finishedPlayers = finishedPlayers;
        this.latestTime = latestTime;
        this.scoreboardConfig = scoreboardConfig;
        this.exclusionList = exclusionList;
    }


}
