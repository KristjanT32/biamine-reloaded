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
        System.err.println(String.format("[%s | %s CRIT]: ", loggerPrefix, modulePrefix) + msg);
        main.appendToLog(String.format("[%s | %s - %s CRIT]: ", loggerPrefix, modulePrefix, getDate()));

    }

    public void logError(String msg) {
        System.err.println(String.format("[%s | %s ERR]: ", loggerPrefix, modulePrefix) + msg);
        main.appendToLog(String.format("[%s | %s - %s ERR]: ", loggerPrefix, modulePrefix, getDate()));
    }

    public void logInfo(String msg) {
        System.out.println(String.format("[%s | %s INF]: ", loggerPrefix, modulePrefix) + msg);
        main.appendToLog(String.format("[%s | %s - %s INF]: ", loggerPrefix, modulePrefix, getDate()));
    }

}
