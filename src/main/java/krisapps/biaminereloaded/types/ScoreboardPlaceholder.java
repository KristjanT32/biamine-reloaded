package krisapps.biaminereloaded.types;

public enum ScoreboardPlaceholder {
    PLAYERS_FINISHED("%playersFinished%"),
    PLAYERS_TOTAL("%playersTotal%"),
    GAME_LABEL("%label%"),
    TIMER("%timer%"),
    SHOOTINGS("%shootingsCount%"),
    HEADER("%header%"),
    FOOTER("%footer%"),
    DATE("%currentDate%"),
    FIRST_FINISHED_PLAYER_TIME("%winningTime%"),

    ;

    private final String placeholder;

    ScoreboardPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }
}
