package krisapps.biaminereloaded.scoreboard;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.gameloop.BiamineBiathlon;
import krisapps.biaminereloaded.gameloop.Game;
import krisapps.biaminereloaded.types.ConfigProperty;
import krisapps.biaminereloaded.types.GameProperty;
import krisapps.biaminereloaded.types.InstanceStatus;
import krisapps.biaminereloaded.types.ScoreboardLine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ScoreboardManager {

    private static final int SCOREBOARD_FIRST_LINE = 9;
    private final String[] lineFillerSymbols = {
            ChatColor.BLACK + String.valueOf(ChatColor.WHITE),
            ChatColor.BLACK + String.valueOf(ChatColor.RED),
            ChatColor.BLACK + String.valueOf(ChatColor.YELLOW),
            ChatColor.BLACK + String.valueOf(ChatColor.GREEN),
            ChatColor.BLACK + String.valueOf(ChatColor.BLACK),
            ChatColor.BLACK + String.valueOf(ChatColor.DARK_PURPLE),
            ChatColor.BLACK + String.valueOf(ChatColor.LIGHT_PURPLE),
            ChatColor.BLACK + String.valueOf(ChatColor.DARK_GREEN)
    };
    Scoreboard mainScoreboard;
    Objective gameObjective;
    Objective previewObjective;
    BiamineReloaded main;


    public ScoreboardManager(BiamineReloaded main) {
        this.main = main;
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (mainScoreboard.getObjective("biathlonGame") == null) {
            gameObjective = mainScoreboard.registerNewObjective("biathlonGame", "dummy", "name");
        } else {
            gameObjective = mainScoreboard.getObjective("biathlonGame");
            mainScoreboard.getObjective("biathlonGame").setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    private void setScoreboardLine(String text, String accessKey, int lineNumber) {

        if (mainScoreboard.getObjective("biathlonGame") == null) {
            gameObjective = mainScoreboard.registerNewObjective("biathlonGame", "dummy", "name");
        }

        // Safeguard: if a line is already occupied, meaning a team already exists, reregister it.
        if (mainScoreboard.getTeam(accessKey) != null) {
            mainScoreboard.getTeam(accessKey).unregister();
        }

        Team propertyToSet = gameObjective.getScoreboard().registerNewTeam(accessKey);
        propertyToSet.addEntry(lineFillerSymbols[lineNumber - 1]);
        propertyToSet.setPrefix(ChatColor.translateAlternateColorCodes('&', text));

        gameObjective.getScore(lineFillerSymbols[lineNumber - 1]).setScore(SCOREBOARD_FIRST_LINE - lineNumber);
    }

    private void setPreviewScoreboardLine(String text, String accessKey, int lineNumber) {

        if (mainScoreboard.getObjective("scoreboardPreview") == null) {
            previewObjective = mainScoreboard.registerNewObjective("scoreboardPreview", "dummy", "preview");
        } else {
            previewObjective = mainScoreboard.getObjective("scoreboardPreview");
        }

        // Safeguard: if a line is already occupied, meaning a team already exists, reregister it.
        if (mainScoreboard.getTeam(accessKey) != null) {
            mainScoreboard.getTeam(accessKey).unregister();
        }

        Team propertyToSet = previewObjective.getScoreboard().registerNewTeam(accessKey);
        propertyToSet.addEntry(lineFillerSymbols[lineNumber - 1]);
        propertyToSet.setPrefix(ChatColor.translateAlternateColorCodes('&', text));

        previewObjective.getScore(lineFillerSymbols[lineNumber - 1]).setScore(SCOREBOARD_FIRST_LINE - lineNumber);
    }

    private void updateScoreboardEntry(String entry, String newContent) {
        Team propertyToUpdate = mainScoreboard.getTeam(entry);
        propertyToUpdate.setPrefix(ChatColor.translateAlternateColorCodes('&', newContent));
    }

    public void setupScoreboard(BiamineBiathlon gameInfoObject, String gameID) {

        if (mainScoreboard.getObjective("biathlonGame") == null) {
            gameObjective = mainScoreboard.registerNewObjective("biathlonGame", "dummy", "name");
        }

        if (!main.dataUtility.scoreboardConfigExists(gameInfoObject.scoreboardConfig)) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("internals.setupsb-err-invconf")));
            return;
        }

        refreshScoreboardData(gameInfoObject.scoreboardConfig, gameInfoObject);
    }

    public void previewScoreboard(String scoreboardConfigurationID) {

        if (Game.instance != null) {
            if (Game.instance.REFRESH_TASK != -1) {
                return;
            }
        }

        BiamineBiathlon gameInfo = new BiamineBiathlon(4, 10, 0, "12:34", scoreboardConfigurationID, "none", "_internaltest");

        String l1 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE1);
        String l2 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE2);
        String l3 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE3);
        String l4 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE4);
        String l5 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE5);
        String l6 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE6);
        String l7 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE7);
        String l8 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE8);

        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l1, gameInfo), "preview_line1", 1);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l2, gameInfo), "preview_line2", 2);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l3, gameInfo), "preview_line3", 3);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l4, gameInfo), "preview_line4", 4);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l5, gameInfo), "preview_line5", 5);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l6, gameInfo), "preview_line6", 6);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l7, gameInfo), "preview_line7", 7);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l8, gameInfo), "preview_line8", 8);

        String title = main.dataUtility.getScoreboardConfigProperty(gameInfo.scoreboardConfig, ScoreboardLine.LINE0);
        title = title.replaceAll("%timer%", gameInfo.latestTime);
        title = title.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&a[state]"));
        title = title.replaceAll("%title%", ChatColor.translateAlternateColorCodes('&', "&aDisplay Name"));

        previewObjective.setDisplayName(ChatColor.translateAlternateColorCodes('&', title + " &f(&ePreview mode&f)"));
        previewObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void clearPreview() {
        previewObjective.setDisplaySlot(null);
        previewObjective.unregister();
    }

    public void refreshScoreboardData(String scoreboardConfigurationID, BiamineBiathlon gameInfo) {

        if (mainScoreboard.getObjective("biathlonGame") == null) {
            gameObjective = mainScoreboard.registerNewObjective("biathlonGame", "dummy", "name");
        }

        String l1 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE1);
        String l2 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE2);
        String l3 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE3);
        String l4 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE4);
        String l5 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE5);
        String l6 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE6);
        String l7 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE7);
        String l8 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE8);

        setScoreboardLine(findReplacePlaceholders(l1, gameInfo), "line1", 1);
        setScoreboardLine(findReplacePlaceholders(l2, gameInfo), "line2", 2);
        setScoreboardLine(findReplacePlaceholders(l3, gameInfo), "line3", 3);
        setScoreboardLine(findReplacePlaceholders(l4, gameInfo), "line4", 4);
        setScoreboardLine(findReplacePlaceholders(l5, gameInfo), "line5", 5);
        setScoreboardLine(findReplacePlaceholders(l6, gameInfo), "line6", 6);
        setScoreboardLine(findReplacePlaceholders(l7, gameInfo), "line7", 7);
        setScoreboardLine(findReplacePlaceholders(l8, gameInfo), "line8", 8);
    }

    public void refreshScoreboardLine(BiamineBiathlon gameInfo, ScoreboardLine line) {
        if (!line.equals(ScoreboardLine.NO_SUCH_LINE)) {
            setScoreboardLine(findReplacePlaceholders(
                            main.dataUtility.getScoreboardConfigProperty(gameInfo.scoreboardConfig, line), gameInfo),
                    "line" + line.asNumber(),
                    line.asNumber()
            );
        }
    }

    public void refreshScoreboardTitle(BiamineBiathlon info) {
        String result = main.dataUtility.getScoreboardConfigProperty(info.scoreboardConfig, ScoreboardLine.LINE0);
        result = result.replaceAll("%timer%", info.latestTime);
        result = result.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', main.dataUtility.getGameProperty(info.gameID, GameProperty.RUN_STATE)));
        result = result.replaceAll("%title%", ChatColor.translateAlternateColorCodes('&', main.dataUtility.getGameProperty(info.gameID, GameProperty.DISPLAY_NAME)));

        if (mainScoreboard.getObjective("biathlonGame") == null) {
            gameObjective = mainScoreboard.registerNewObjective("biathlonGame", "dummy", "name");
        }

        gameObjective.setDisplayName(ChatColor.translateAlternateColorCodes('&', result));
    }

    private String findReplacePlaceholders(String input, BiamineBiathlon info) {

        // Replace built-in placeholders
        input = input.replaceAll("%dateTime%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.DATE_FORMAT_EXTENDED)).format(LocalDateTime.now()));
        input = input.replaceAll("%localTime%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.TIME_FORMAT)).format(LocalTime.now()));
        input = input.replaceAll("%date%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.DATE_FORMAT)).format(LocalDate.now()));
        input = input.replaceAll("%footer%", main.dataUtility.getConfigProperty(ConfigProperty.FOOTER_CONTENT));
        input = input.replaceAll("%header%", main.dataUtility.getConfigProperty(ConfigProperty.HEADER_CONTENT));
        input = input.replaceAll("%shootings%", String.valueOf(info.shootingsCount));
        input = input.replaceAll("%playersNotFinished%", (info.totalPlayers - info.finishedPlayers) + "/" + info.totalPlayers);
        input = input.replaceAll("%playersFinished%", String.valueOf(info.finishedPlayers));
        input = input.replaceAll("%playersParticipating%", String.valueOf(info.totalPlayers));
        input = input.replaceAll("%timer%", info.latestTime);
        input = input.replaceAll("%empty%", "");
        input = input.replaceAll("%bestTime%", Game.instance.getBestFinishTime() == null ? "N/A" : Game.instance.getBestFinishTime().getKey().getName() + " - " + Game.instance.getBestFinishTime().getValue().getFinishTime());

        // Find and replace all user-defined placeholders.
        for (String word : input.split(" ")) {
            if (word.startsWith("_")) {
                if (main.dataUtility.globalPlaceholderExists(word)) {
                    input = input.replaceAll(word, main.dataUtility.getGlobalPlaceholderReplacement(word));
                }
            }
        }

        // Handle state styling
        switch (InstanceStatus.valueOf(main.dataUtility.getGameProperty(info.gameID, GameProperty.RUN_STATE).toUpperCase())) {
            case STANDBY:
                input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&fStandby"));
                break;
            case PREP:
                input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&ePreparing"));
                break;
            case COUNTDOWN:
                input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&eCountdown"));
                break;
            case RUNNING:
                input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&aRunning"));
                break;
            case FINALIZING:
                input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&dFinalizing"));
                break;
            case CLEANUP:
                input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&bCleanup"));
                break;
            case PAUSED:
                input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&9Paused"));
                break;
            case TERMINATED:
                input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&cTerminated"));
                break;
            case PREVTERM:
                input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&4Preventatively Terminated"));
                break;
        }

        return input;
    }

    private String findReplacePreviewPlaceholders(String input, BiamineBiathlon info) {

        // Replace built-in placeholders
        input = input.replaceAll("%dateTime%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.DATE_FORMAT_EXTENDED)).format(LocalDateTime.now()));
        input = input.replaceAll("%localTime%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.TIME_FORMAT)).format(LocalTime.now()));
        input = input.replaceAll("%date%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.DATE_FORMAT)).format(LocalDate.now()));
        input = input.replaceAll("%footer%", main.dataUtility.getConfigProperty(ConfigProperty.FOOTER_CONTENT));
        input = input.replaceAll("%header%", main.dataUtility.getConfigProperty(ConfigProperty.HEADER_CONTENT));
        input = input.replaceAll("%shootings%", String.valueOf(info.shootingsCount));
        input = input.replaceAll("%playersNotFinished%", (info.totalPlayers - info.finishedPlayers) + "/" + info.totalPlayers);
        input = input.replaceAll("%playersFinished%", String.valueOf(info.finishedPlayers));
        input = input.replaceAll("%playersParticipating%", String.valueOf(info.totalPlayers));
        input = input.replaceAll("%timer%", info.latestTime);
        input = input.replaceAll("%empty%", "");
        input = input.replaceAll("%bestTime%", "N/A");
        input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&ePreview"));

        // Find and replace all user-defined placeholders.
        for (String word : input.split(" ")) {
            if (word.startsWith("_")) {
                if (main.dataUtility.globalPlaceholderExists(word)) {
                    input = input.replaceAll(word, main.dataUtility.getGlobalPlaceholderReplacement(word));
                }
            }
        }

        return input;
    }


    public void showScoreboard() {
        mainScoreboard.getObjective("biathlonGame").setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void hideScoreboard() {
        mainScoreboard.getObjective("biathlonGame").setDisplaySlot(null);
    }

    public void resetScoreboard() {
        hideScoreboard();
        gameObjective.unregister();
    }


}
