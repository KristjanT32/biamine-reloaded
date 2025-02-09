package krisapps.biaminereloaded.scoreboard;

public enum ScoreboardType {
    PRIMARY("biathlonGame"), LEADERBOARD("biathlonLeaderboard"), SHOOTING_RANGE("biathlonShootingRange"), PREVIEW(
            "scoreboardPreview"),
    ;

    final String objectiveName;

    ScoreboardType(String objectiveName) {
        this.objectiveName = objectiveName;
    }

    public String getObjectiveName() {
        return objectiveName;
    }
}
