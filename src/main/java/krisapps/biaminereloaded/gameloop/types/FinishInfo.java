package krisapps.biaminereloaded.gameloop.types;

public class FinishInfo {

    private final String finishTime;
    private final int leaderboardOrder;

    public FinishInfo(String finishTime, int leaderboardOrder) {
        this.finishTime = finishTime;
        this.leaderboardOrder = leaderboardOrder;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public int getLeaderboardOrder() {
        return leaderboardOrder;
    }
}
