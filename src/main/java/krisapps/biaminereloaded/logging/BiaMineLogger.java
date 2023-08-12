package krisapps.biaminereloaded.logging;

import krisapps.biaminereloaded.BiamineReloaded;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;

public class BiaMineLogger {

    BiamineReloaded main;
    private final String loggerPrefix;
    private final String modulePrefix;

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

    private String getDate() {
        return SimpleDateFormat.getDateTimeInstance().format(Date.from(Instant.now()));
    }

    public void logCritError(String msg) {
        main.appendToLog(String.format("[%s::%s at %s CRIT]: " + msg, loggerPrefix, modulePrefix, getDate()));

    }

    public void logError(String msg) {
        main.appendToLog(String.format("[%s::%s at %s ERR]: " + msg, loggerPrefix, modulePrefix, getDate()));
    }

    public void logInfo(String msg) {
        main.appendToLog(String.format("[%s::%s at %s INF]: " + msg, loggerPrefix, modulePrefix, getDate()));
    }

}
