package krisapps.biaminereloaded.data;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.SaveablePropertyType;

import java.util.ArrayList;

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
    public void saveCoreData(String field, String value) {
        main.pluginData.set(COREDATA_FILE_PATH_PREFIX + field, value);
        main.saveAllFiles();
    }

    public boolean gameExists(String gameID) {
        return main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID) != null
                ;
    }

    public boolean createGame(String gameID, int prepTime, int countdown, String displayName) {
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".preparationTime", prepTime);
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".countdownTime", countdown);
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".displayName", displayName);
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".exclude", new ArrayList<>());
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".scoreboardConfiguration", "default");
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".timerFormat", "default");

        return main.saveGames();
    }

    public boolean deleteGame(String gameID) {
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID, null);

        return main.saveGames();
    }

    public String getCurrentLanguage() {
        return main.pluginData.getString("options.currentLanguage") == null
                ? "en-US"
                : main.pluginData.getString("options.currentLanguage");
    }
}
