package krisapps.biaminereloaded.types;

public enum ScoreboardLine {
    LINE1(1),
    LINE2(2),
    LINE3(3),
    LINE4(4),
    LINE5(5),
    LINE6(6),
    LINE7(7),
    LINE8(8);

    private final int lineNumber;

    ScoreboardLine(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public static ScoreboardLine asEnum(int lineNumber) {
        return ScoreboardLine.valueOf("LINE" + lineNumber);
    }

    public int asNumber() {
        return this.lineNumber;
    }
}
