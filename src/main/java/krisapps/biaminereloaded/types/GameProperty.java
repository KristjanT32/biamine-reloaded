package krisapps.biaminereloaded.types;

public enum GameProperty {

    DISPLAY_NAME("displayName"),
    COUNTDOWN_TIME("countdownTime"),
    PREPARATION_TIME("preparationTime"),
    RUN_STATE("runState"),
    SCOREBOARD_CONFIGURATION_ID("scoreboardConfiguration"),
    EXCLUSION_LIST_ID("exclusionList"),
    ITEMS_TO_DISPENSE("items");
    String fieldName;

    GameProperty(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }

}
