package krisapps.biaminereloaded.scoreboard;

public enum ScoreboardLine {

    LINE0(0),
    LINE1(1),
    LINE2(2),
    LINE3(3),
    LINE4(4),
    LINE5(5),
    LINE6(6),
    LINE7(7),
    LINE8(8),
    NO_SUCH_LINE(404);

    private final int lineNumber;

    ScoreboardLine(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public static ScoreboardLine asEnum(int lineNumber) {
        try {
            return ScoreboardLine.valueOf("LINE" + lineNumber);
        } catch (IllegalArgumentException e) {
            return ScoreboardLine.NO_SUCH_LINE;
        }
    }

    public int asNumber() {
        return this.lineNumber;
    }
}
