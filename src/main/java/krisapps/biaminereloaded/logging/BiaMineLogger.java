package krisapps.biaminereloaded.logging;

import krisapps.biaminereloaded.BiamineReloaded;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BiaMineLogger {

    BiamineReloaded main;
    private final String loggerPrefix;
    private final String modulePrefix;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy-HH:mm:ss");

    public BiaMineLogger(String loggerPrefix, String modulePrefix, BiamineReloaded main) {
        this.loggerPrefix = loggerPrefix;
        this.modulePrefix = modulePrefix;
        this.main = main;
    }

    public BiaMineLogger(String loggerPrefix, BiamineReloaded main) {
        this.loggerPrefix = loggerPrefix;
        this.modulePrefix = "General";
        this.main = main;
    }

    public BiaMineLogger(BiamineReloaded main) {
        this.loggerPrefix = "BiaMine";
        this.modulePrefix = "General";
        this.main = main;
    }

    private String getTimeStamp() {
        return formatter.format(LocalDateTime.now());
    }

    public void logCritError(String msg) {
        main.appendToLog(String.format("[%s] [%s/%s CRIT]: ", getTimeStamp(), loggerPrefix, modulePrefix) + msg);
    }

    public void logError(String msg) {
        main.appendToLog(String.format("[%s] [%s/%s ERR]: ", getTimeStamp(), loggerPrefix, modulePrefix) + msg);
    }

    public void logInfo(String msg) {
        main.appendToLog(String.format("[%s] [%s/%s INFO]: ", getTimeStamp(), loggerPrefix, modulePrefix) + msg);
    }

}
