package krisapps.biaminereloaded.scoreboard;

public enum Placeholder {

    BIATHLON_INSTANCE("%instance%"),
    PLAYER("%player%"),

    PREPARATION_TIME("%preptime%"),
    FINAL_COUNTDOWN("%countdown%"),


    ;
    private final String placeholder;

    Placeholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }
}
