package krisapps.biaminereloaded.timers;

public class TimerFormatter {

    public static String formatTimer(String format, int seconds, int minutes, int hours) {
        String formattedTimer = format;
        formattedTimer = formattedTimer.replaceAll("hh", formatTimeUnit(hours));
        formattedTimer = formattedTimer.replaceAll("mm", formatTimeUnit(minutes));
        formattedTimer = formattedTimer.replaceAll("ss", formatTimeUnit(seconds));
        return formattedTimer;
    }

    private static String formatTimeUnit(int unit) {
        return unit <= 9
                ? "0" + unit
                : String.valueOf(unit);
    }


}
