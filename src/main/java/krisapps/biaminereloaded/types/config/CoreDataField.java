package krisapps.biaminereloaded.types.config;

public enum CoreDataField {
    LAST_VERSION("lastVersion"),
    TEST_REGION_B1("testregion.b1"),
    TEST_REGION_B2("testregion.b2"),

    LAST_ACTIVE_GAME("current.lastActiveGameObject"),
    GAME_IN_PROGRESS("gameInProgress");

    private final String fieldName;

    CoreDataField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getField() {
        return this.fieldName;
    }
}
