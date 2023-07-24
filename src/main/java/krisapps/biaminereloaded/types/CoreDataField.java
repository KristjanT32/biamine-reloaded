package krisapps.biaminereloaded.types;

public enum CoreDataField {
    LAST_VERSION("lastVersion");

    private final String fieldName;

    CoreDataField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getField() {
        return this.fieldName;
    }
}
