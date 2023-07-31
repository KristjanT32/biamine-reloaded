package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class BiaMineDataUtility {

    private final String GAME_FILE_PATH_PREFIX = "games.";
    private final String COREDATA_FILE_PATH_PREFIX = "coredata.";
    private final String SCOREBOARD_FILE_PATH_PREFIX = "scoreboardconfigs.";
    private final String EXCLUSIONS_FILE_PATH_PREFIX = "exclusions.";
    BiamineReloaded main;

    public BiaMineDataUtility(BiamineReloaded main) {
        this.main = main;
    }

    // Save game/customization values
    public void saveProperty(SaveablePropertyType propertyType, String field, String value) {
        switch (propertyType) {
            case NEW_GAME:
            case GAME_PROPERTY:
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + field, value);
                break;

            case SCOREBOARD_CUSTOMIZATION_PROPERTY:
                main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + field, value);
                break;
        }
        main.saveAllFiles();
    }

    // Save core data values
    public void saveCoreData(CoreDataField field, String value) {
        main.pluginData.set(COREDATA_FILE_PATH_PREFIX + field.getField(), value);
        main.saveAllFiles();
    }

    public String getCoreData(CoreDataField field) {
        return main.pluginData.getString(COREDATA_FILE_PATH_PREFIX + field.getField());
    }

    public Set<String> getUserDefinedPlaceholders() {
        return main.pluginConfig.getConfigurationSection("placeholders") != null ? main.pluginConfig.getConfigurationSection("placeholders").getKeys(false) : new HashSet<>();
    }

    public boolean gameExists(String gameID) {
        return main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID) != null;
    }

    public Set<String> getGames() {
        return main.pluginGames.getConfigurationSection("games") == null ? new LinkedHashSet<>() : main.pluginGames.getConfigurationSection("games").getKeys(false);
    }

    public boolean createGame(String gameID, int prepTime, int countdown, String displayName) {
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".preparationTime", prepTime);
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".countdownTime", countdown);
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".displayName", displayName);
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".exclusionList", "none");
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".scoreboardConfiguration", "default");
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".runState", "STANDBY");

        return main.saveGames();
    }

    public boolean createScoreboardConfig(String id) {
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".title", "%title%");
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".line1", "%empty%");
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".line2", "%empty%");
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".line3", "%empty%");
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".line4", "%empty%");
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".line5", "%empty%");
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".line6", "%empty%");
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".line7", "%empty%");
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".line8", "%empty%");

        return main.saveScoreboards();
    }

    public boolean scoreboardConfigExists(String id) {
        if (id.equalsIgnoreCase("default")) {
            return true;
        }
        return main.pluginScoreboardConfig.contains(SCOREBOARD_FILE_PATH_PREFIX + id);
    }

    public Set<String> getScoreboardConfigs() {
        if (main.pluginScoreboardConfig.getConfigurationSection("scoreboardconfigs") == null) return new HashSet<>(0);
        return main.pluginScoreboardConfig.getConfigurationSection("scoreboardconfigs").getKeys(false);
    }

    public String getScoreboardConfigProperty(String id, ScoreboardLine line) {
        if (!id.equals("default")) {
            if (line.equals(ScoreboardLine.LINE0)) {
                return main.pluginScoreboardConfig.getString(SCOREBOARD_FILE_PATH_PREFIX + id + ".title");
            } else {
                return main.pluginScoreboardConfig.getString(SCOREBOARD_FILE_PATH_PREFIX + id + ".line" + line.asNumber());
            }
        } else {
            if (line.equals(ScoreboardLine.LINE0)) {
                return main.pluginConfig.getString("defaults.scoreboardconfig.title");
            } else {
                return main.pluginConfig.getString("defaults.scoreboardconfig.line" + line.asNumber());
            }
        }
    }

    public boolean switchScoreboardProperties(String id, int firstProperty, int secondProperty) {

        String first = getScoreboardConfigProperty(id, ScoreboardLine.asEnum(firstProperty));
        String second = getScoreboardConfigProperty(id, ScoreboardLine.asEnum(secondProperty));

        overwriteScoreboardProperty(id, firstProperty, second);
        overwriteScoreboardProperty(id, secondProperty, first);

        return main.saveScoreboards();

    }

    public int getPropertyLineNumber(String id, String property) {
        if (!id.equalsIgnoreCase("default")) {
            for (String line : main.pluginScoreboardConfig.getConfigurationSection(SCOREBOARD_FILE_PATH_PREFIX + id).getKeys(false)) {
                if (main.pluginScoreboardConfig.getString(SCOREBOARD_FILE_PATH_PREFIX + id + "." + line).contains(property)) {
                    if (!line.equalsIgnoreCase("title")) {
                        return Integer.parseInt(line.replace("line", ""));
                    }
                }
            }
            return 404;
        } else {
            for (String line : main.pluginConfig.getConfigurationSection("defaults.scoreboardconfig").getKeys(false)) {
                if (main.pluginConfig.getString("defaults.scoreboardconfig." + line).contains(property)) {
                    if (!line.equalsIgnoreCase("title")) {
                        return Integer.parseInt(line.replace("line", ""));
                    }
                }
            }
            return 404;
        }
    }

    public boolean overwriteScoreboardProperty(String id, int lineToOverwrite, String overwriteWithProperty) {
        if (lineToOverwrite == 0) {
            main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".title", overwriteWithProperty);
        } else {
            main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id + ".line" + lineToOverwrite, overwriteWithProperty);
        }
        return main.saveScoreboards();
    }

    public boolean deleteScoreboardConfig(String id) {
        unassignScoreboardConfigFromAll(id);
        main.pluginScoreboardConfig.set(SCOREBOARD_FILE_PATH_PREFIX + id, null);
        return main.saveScoreboards();
    }


    public boolean deleteGame(String gameID) {
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID, null);

        return main.saveGames();
    }

    public String getGameProperty(String gameID, GameProperty property) {
        return main.pluginGames.getString(GAME_FILE_PATH_PREFIX + gameID + "." + property.getFieldName());
    }

    public boolean updateGameRunstate(String gameID, InstanceStatus runstate) {
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".runState", runstate.toString());

        return main.saveGames();
    }

    public boolean globalPlaceholderExists(String text) {
        return main.pluginConfig.contains("placeholders." + text);
    }

    public String getGlobalPlaceholderReplacement(String placeholder) {
        return main.pluginConfig.getString("placeholders." + placeholder);
    }

    public ArrayList<String> getExclusionListByID(String listID) {
        return main.pluginExclusionLists.getList(EXCLUSIONS_FILE_PATH_PREFIX + listID) == null ? new ArrayList<String>() : (ArrayList<String>) main.pluginExclusionLists.getList(EXCLUSIONS_FILE_PATH_PREFIX + listID);
    }

    public org.bukkit.Location getStartLocationFirstBound(String game) {
        if (main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + game + ".start") != null) {
            org.bukkit.Location loc1 = new org.bukkit.Location(Bukkit.getWorlds().get(0), main.pluginGames.getDouble(GAME_FILE_PATH_PREFIX + game + ".start.bound1.x"), main.pluginGames.getDouble(GAME_FILE_PATH_PREFIX + game + ".start.bound1.y"), main.pluginGames.getDouble(GAME_FILE_PATH_PREFIX + game + ".start.bound1.z"));
            return loc1;
        }
        return null;
    }

    public org.bukkit.Location getStartLocationSecondBound(String game) {
        if (main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + game + ".start") != null) {
            org.bukkit.Location loc2 = new org.bukkit.Location(Bukkit.getWorlds().get(0), main.pluginGames.getDouble(GAME_FILE_PATH_PREFIX + game + ".start.bound2.x"), main.pluginGames.getDouble(GAME_FILE_PATH_PREFIX + game + ".start.bound2.y"), main.pluginGames.getDouble(GAME_FILE_PATH_PREFIX + game + ".start.bound2.z"));
            return loc2;
        }
        return null;
    }

    public boolean hasSetupStartLocation(String gameID) {
        if (main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".start") != null) {
            return (
                    main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".start.bound1") != null
                            && main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".start.bound2") != null
            );
        } else {
            return false;
        }
    }

    public void setStartLocation(String game, int bound, Player p) {
        if (gameExists(game)) {
            switch (bound) {
                case 1:
                case 2:
                    main.pluginGames.set(GAME_FILE_PATH_PREFIX + game + ".start.bound" + bound + ".x", p.getLocation().getX());
                    main.pluginGames.set(GAME_FILE_PATH_PREFIX + game + ".start.bound" + bound + ".y", p.getLocation().getY());
                    main.pluginGames.set(GAME_FILE_PATH_PREFIX + game + ".start.bound" + bound + ".z", p.getLocation().getZ());
                    main.saveGames();
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * Unassigns the provided scoreboard configuration from all games.
     *
     * @param sconfigID the scoreboard config to unassign
     * @return true if operation succeeds, false if an error prevents the data from being saved
     */
    public boolean unassignScoreboardConfigFromAll(String sconfigID) {
        for (String game : main.pluginGames.getConfigurationSection("games").getKeys(false)) {
            if (main.pluginGames.getString(GAME_FILE_PATH_PREFIX + game + ".scoreboardConfiguration").equals(sconfigID)) {
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + game + ".scoreboardConfiguration", "none");
            }
        }
        return main.saveGames();
    }

    public boolean assignScoreboardConfiguration(String sconfigID, String targetGame) {
        if (gameExists(targetGame)) {
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + targetGame + ".scoreboardConfiguration", sconfigID);
        }
        return main.saveGames();
    }

    public boolean unassignScoreboardConfiguration(String sconfigID, String targetGame) {
        if (gameExists(targetGame)) {
            if (main.pluginGames.getString(GAME_FILE_PATH_PREFIX + targetGame + ".scoreboardConfiguration").equals(sconfigID)) {
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + targetGame + ".scoreboardConfiguration", "none");
            }
        }
        return main.saveGames();
    }


    public String getCurrentLanguage() {
        if (main.pluginData.getString("options.currentLanguage") == null) {
            main.pluginData.set("options.currentLanguage", "en-US");
            main.saveCoredata();
        }
        return main.pluginData.getString("options.currentLanguage");
    }

    public void setCurrentLanguage(String langCode) {
        main.pluginData.set("options.currentLanguage", langCode);
        main.saveCoredata();
    }

    public String getConfigProperty(ConfigProperty property) {
        switch (property) {
            case FOOTER_CONTENT:
                return main.pluginConfig.getString("defaults.footer");
            case HEADER_CONTENT:
                return main.pluginConfig.getString("defaults.header");
            case TIMER_FORMAT:
                return main.pluginConfig.getString("options.timer-format");
            case DATE_FORMAT:
                return main.pluginConfig.getString("options.date-format");
            case DATE_FORMAT_EXTENDED:
                return main.pluginConfig.getString("options.date-format-full");
            case TIME_FORMAT:
                return main.pluginConfig.getString("options.time-format");
            default:
                throw new InvalidParameterException("Unknown config property provided.");
        }
    }
}
