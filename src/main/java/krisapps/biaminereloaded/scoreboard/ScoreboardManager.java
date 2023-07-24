package krisapps.biaminereloaded.scoreboard;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.game_setup.BiamineBiathlon;
import krisapps.biaminereloaded.types.ConfigProperty;
import krisapps.biaminereloaded.types.ScoreboardLine;
import krisapps.biaminereloaded.types.ScoreboardPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ScoreboardManager {

    private final String timer = "Hh:Mm:Ss";
    Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    Objective gameObjective;
    BiamineReloaded main;
    private String currentScoreboardConfiguration;
    private boolean scoreboardShown;
    private String timerFormat;

    public ScoreboardManager(BiamineReloaded main) {
        this.main = main;
        if (mainScoreboard.getObjective("biathlonGame") == null) {
            gameObjective = mainScoreboard.registerNewObjective("biathlonGame", "dummy", "name");
        } else {
            gameObjective = mainScoreboard.getObjective("biathlonGame");
            mainScoreboard.getObjective("biathlonGame").setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    private void setScoreboardLine(String text, String accessKey, int lineNumber) {

        // Safeguard: if a line is already occupied, meaning a team already exists, reregister it.
        if (mainScoreboard.getTeam(accessKey) != null) {
            mainScoreboard.getTeam(accessKey).unregister();
        }

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

        if (!main.dataUtility.scoreboardConfigExists(gameInfoObject.scoreboardConfig)) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("internals.setupsb-err-invconf")));
            return;
        }


        this.currentScoreboardConfiguration = gameInfoObject.scoreboardConfig;
        this.timerFormat = main.pluginScoreboardConfig.getString(gameInfoObject.scoreboardConfig + ".timerFormat");

        refreshScoreboardData(gameInfoObject.scoreboardConfig, gameInfoObject);
    }

    public void refreshScoreboardData(String scoreboardConfigurationID, BiamineBiathlon gameInfo) {
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

    public void refreshScoreboardTitle(String currentScoreboardConfiguration, String gameLabel, String timer) {
        String result = main.pluginScoreboardConfig.getString(currentScoreboardConfiguration + ".gameName");
        result = result.replaceAll(ScoreboardPlaceholder.TIMER.getPlaceholder(), timer);
        result = result.replaceAll(ScoreboardPlaceholder.GAME_LABEL.getPlaceholder(), gameLabel);

        gameObjective.setDisplayName(ChatColor.translateAlternateColorCodes('&', result));
    }

    // timer, playersParticipating, playersNotFinished, shootings, header, footer

    private String findReplacePlaceholders(String input, BiamineBiathlon info) {
        Bukkit.getLogger().info("Preparing to transform value: " + input);
        if (!main.dataUtility.globalPlaceholderExists(input)) {
            switch (input) {
                case "%timer%":
                    input = input.replaceAll("%timer%", timer);
                    break;
                case "%playersParticipating%":
                    input = input.replaceAll("%playersParticipating%", String.valueOf(info.totalPlayers));
                    break;
                case "%playersNotFinished%":
                    input = input.replaceAll("%playersNotFinished%", String.valueOf(info.totalPlayers - info.finishedPlayers));
                    break;
                case "%shootings%":
                    input = input.replaceAll("%shootings%", String.valueOf(info.shootingsCount));
                    break;
                case "%header%":
                    input = input.replaceAll("%header%", main.dataUtility.getConfigProperty(ConfigProperty.HEADER_CONTENT));
                    break;
                case "%footer%":
                    input = input.replaceAll("%footer%", main.dataUtility.getConfigProperty(ConfigProperty.FOOTER_CONTENT));
                    break;
                case "empty":
                    input = "";
                    break;
                default:
                    break;
            }
        } else {
            input = main.dataUtility.getGlobalPlaceholderReplacement(input);
        }
        return input;
    }


    //TODO: Implement a way for the scoreboard configurations to be used


    public void showScoreboard() {
        mainScoreboard.getObjective("biathlonGame").setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void hideScoreboard() {
        mainScoreboard.getObjective("biathlonGame").setDisplaySlot(null);
    }

    public void resetScoreboard() {

    }


}
