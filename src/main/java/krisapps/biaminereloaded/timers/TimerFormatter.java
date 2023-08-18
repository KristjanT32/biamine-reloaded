package krisapps.biaminereloaded.timers;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimerFormatter {

    public static String formatTimer(String format, int seconds, int minutes, int hours) {
        String formattedTimer = format;
        formattedTimer = formattedTimer.replaceAll("hh", formatTimeUnit(hours));
        formattedTimer = formattedTimer.replaceAll("mm", formatTimeUnit(minutes));
        formattedTimer = formattedTimer.replaceAll("ss", formatTimeUnit(seconds));
        return formattedTimer;
    }

    public static int getMinutesFrom(int seconds) {
        return (int) Math.floor((double) seconds / 60);
    }

    public static int getHoursFrom(int seconds) {
        return (int) Math.floor((double) (int) Math.floor((double) seconds / 60) / 60);
    }

    public static String formatTimeUnit(int unit) {
        return unit <= 9
                ? "0" + unit
                : String.valueOf(unit);
    }

    public static String getDifference(String time1, String time2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime parsedTime1 = LocalTime.parse(time1, formatter);
        LocalTime parsedTime2 = LocalTime.parse(time2, formatter);

        if (parsedTime1.isAfter(parsedTime2)) {
            Duration dur = Duration.between(parsedTime2, parsedTime1);

            return String.format("%s:%s:%s", formatTimeUnit((int) dur.toHours()), formatTimeUnit(dur.toMinutesPart()), formatTimeUnit(dur.toSecondsPart()));
        } else if (parsedTime2.isAfter(parsedTime1)) {
            Duration dur = Duration.between(parsedTime1, parsedTime2);

            return String.format("%s:%s:%s", formatTimeUnit((int) dur.toHours()), formatTimeUnit(dur.toMinutesPart()), formatTimeUnit(dur.toSecondsPart()));
        }
        return "00:00:00";
    }


}
