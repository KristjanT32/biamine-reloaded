package krisapps.biaminereloaded.scoreboard;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.game_setup.BiamineBiathlon;
import krisapps.biaminereloaded.types.ScoreboardPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Objects;

public class ScoreboardManager {

    private final String timer = "00:00:00";
    Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    Objective gameObjective;
    BiamineReloaded main;
    private String currentScoreboardConfiguration;
    private boolean scoreboardShown;
    private String timerFormat;

    public ScoreboardManager(BiamineReloaded main) {
        this.main = main;
        if (mainScoreboard.getObjective("biathlonGame") == null) {
            mainScoreboard.registerNewObjective("biathlonGame", "dummy", "name");
        } else {
            mainScoreboard.getObjective("biathlonGame").setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    private void setScoreboardLine(String text, String accessKey, int lineNumber) {
        Team propertyToSet = gameObjective.getScoreboard().registerNewTeam(accessKey);
        propertyToSet.addEntry("|");
        propertyToSet.setPrefix(ChatColor.translateAlternateColorCodes('&', text));

        gameObjective.getScore(ChatColor.BLACK + String.valueOf(ChatColor.WHITE)).setScore(lineNumber);
    }

    private void updateScoreboardEntry(String entry, String newContent) {
        Team propertyToUpdate = mainScoreboard.getTeam(entry);
        propertyToUpdate.setPrefix(ChatColor.translateAlternateColorCodes('&', newContent));
    }

    public void setupScoreboard(BiamineBiathlon gameInfoObject) {

        this.currentScoreboardConfiguration = gameInfoObject.scoreboardConfig;
        this.timerFormat = main.pluginScoreboardConfig.getString(gameInfoObject.scoreboardConfig + ".timerFormat");

        refreshScoreboardData(gameInfoObject.scoreboardConfig, gameInfoObject);
    }

    public void refreshScoreboardData(String scoreboardConfigurationID, BiamineBiathlon gameInfo) {
        String gameName;
        String header;
        String footer;
        String shootings;
        String players;
        String customMessage;
        String winningTime;

        gameName = main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".titleBar");
        gameName = gameName.replaceAll(ScoreboardPlaceholder.TIMER.getPlaceholder(), timer);

        gameObjective.setDisplayName(ChatColor.translateAlternateColorCodes('&', gameName));

        header = !Objects.equals(main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".header"), "_disable")
                ? main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".header")
                : null;

        footer = !Objects.equals(main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".footer"), "_disable")
                ? main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".footer")
                : null;

        shootings = !Objects.equals(main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".shootingsText"), "_disable")
                ? main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".shootingsText")
                : null;
        shootings = shootings.replaceAll(ScoreboardPlaceholder.SHOOTINGS.getPlaceholder(), String.valueOf(gameInfo.shootingsCount));

        players = !Objects.equals(main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".playersText"), "_disable")
                ? main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".playersText")
                : null;
        players = players.replaceAll(ScoreboardPlaceholder.PLAYERS_TOTAL.getPlaceholder(), String.valueOf(gameInfo.totalPlayers));

        players = players.replaceAll(ScoreboardPlaceholder.PLAYERS_FINISHED.getPlaceholder(), String.valueOf(gameInfo.finishedPlayers));

        customMessage = !Objects.equals(main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".customMessage"), "_disable")
                ? main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".customMessage")
                : null;

        winningTime = !Objects.equals(main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".winningTime"), "_disable")
                ? main.pluginScoreboardConfig.getString(scoreboardConfigurationID + ".winningTime")
                : null;
        winningTime = winningTime.replaceAll(ScoreboardPlaceholder.FIRST_FINISHED_PLAYER_TIME.getPlaceholder(), gameInfo.latestTime);

        if (header != null) {
            setScoreboardLine(
                    header,
                    "header",
                    main.pluginScoreboardConfig.getInt(scoreboardConfigurationID + ".header.order")
            );
        }

        if (footer != null) {
            setScoreboardLine(
                    footer,
                    "footer",
                    main.pluginScoreboardConfig.getInt(scoreboardConfigurationID + ".footer.order")
            );
        }

        if (shootings != null) {
            setScoreboardLine(
                    shootings,
                    "shootings",
                    main.pluginScoreboardConfig.getInt(scoreboardConfigurationID + ".shootings.order")
            );
        }

        if (players != null) {
            setScoreboardLine(
                    players,
                    "players",
                    main.pluginScoreboardConfig.getInt(scoreboardConfigurationID + ".players.order")
            );
        }

        if (customMessage != null) {
            setScoreboardLine(
                    customMessage,
                    "customMessage",
                    main.pluginScoreboardConfig.getInt(scoreboardConfigurationID + ".customMessage.order")
            );
        }

        if (winningTime != null) {
            setScoreboardLine(
                    winningTime,
                    "winningTime",
                    main.pluginScoreboardConfig.getInt(scoreboardConfigurationID + ".winningTime.order")
            );
        }
    }

    public void refreshScoreboardTitle(String currentScoreboardConfiguration, String gameLabel, String timer) {
        String result = main.pluginScoreboardConfig.getString(currentScoreboardConfiguration + ".gameName");
        result.replaceAll(ScoreboardPlaceholder.TIMER.getPlaceholder(), timer);
        result.replaceAll(ScoreboardPlaceholder.GAME_LABEL.getPlaceholder(), gameLabel);

        gameObjective.setDisplayName(ChatColor.translateAlternateColorCodes('&', result));

    }
    //TODO: Implement a way for the scoreboard configurations to be used


    public void showScoreboard() {

    }

    public void hideScoreboard() {

    }

    public void resetScoreboard() {

    }


}
