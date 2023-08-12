package krisapps.biaminereloaded.timers;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

public class BiathlonTimer {

    BukkitScheduler scheduler = Bukkit.getScheduler();
    BiamineReloaded main;
    int seconds = 0;
    int minutes = 0;
    int hours = 0;
    String formattedTime = "";
    String timerFormat = "";
    private int globalTimerTaskID = -20;
    private boolean isPaused = false;

    public BiathlonTimer(BiamineReloaded main) {
        this.main = main;
    }

    public void startGlobalTimer(String timerFormat) {
        this.timerFormat = timerFormat;
        globalTimerTaskID = scheduler.scheduleAsyncRepeatingTask(main, new Runnable() {
            @Override
            public void run() {
                if (isPaused) {
                    return;
                }

                if (seconds < 59) {
                    seconds++;
                } else {
                    seconds = 0;
                    if (minutes < 59) {
                        minutes++;
                    } else {
                        minutes = 0;
                        seconds = 0;
                        hours++;
                    }
                }
                formattedTime = TimerFormatter.formatTimer(timerFormat, seconds, minutes, hours);
            }
        }, 0, 20);
    }

    public void stopGlobalTimer() {
        if (globalTimerTaskID != -20) {
            Bukkit.getScheduler().cancelTask(globalTimerTaskID);
        }
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getHours() {
        return hours;
    }

    public int getGlobalTimerTaskID() {
        return globalTimerTaskID;
    }

    public void pauseTimer() {
        isPaused = true;
    }

    public void resumeTimer() {
        isPaused = false;
    }
}
