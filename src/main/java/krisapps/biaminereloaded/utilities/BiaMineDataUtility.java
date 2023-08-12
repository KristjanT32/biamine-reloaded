package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.events.InstanceStatusChangeEvent;
import krisapps.biaminereloaded.types.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.security.InvalidParameterException;
import java.util.*;

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
    public void saveCoreData(CoreDataField field, Object value) {
        main.pluginData.set(COREDATA_FILE_PATH_PREFIX + field.getField(), value);
        main.saveAllFiles();
    }

    public Object getCoreData(CoreDataField field) {
        return main.pluginData.get(COREDATA_FILE_PATH_PREFIX + field.getField());
    }

    public Location getTestRegionBounds(CoreDataField bound) {
        return bound == CoreDataField.TEST_REGION_B1 ? main.pluginData.getObject(COREDATA_FILE_PATH_PREFIX + bound.getField(), Location.class)
                : bound == CoreDataField.TEST_REGION_B2 ? main.pluginData.getObject(COREDATA_FILE_PATH_PREFIX + bound.getField(), Location.class)
                : null;
    }

    public Set<String> getCheckpoints(String game) {
        if (gameExists(game)) {
            if (main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + game + ".checkpoints") != null) {
                return main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + game + ".checkpoints").getKeys(false);
            } else {
                return new HashSet<>(0);
            }
        } else {
            return new HashSet<>(0);
        }
    }

    public boolean checkpointExists(String gameID, String checkpoint) {
        return main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpoint) != null;
    }

    public void addCheckpoint(String gameID, String displayName, Location bound1, Location bound2) {
        if (gameExists(gameID)) {
            int checkpointNumber = getCheckpoints(gameID).size();

            main.pluginGames.set(
                    GAME_FILE_PATH_PREFIX + gameID + ".checkpoints.checkpoint" + (checkpointNumber + 1) + ".displayName",
                    displayName
            );
            main.pluginGames.set(
                    GAME_FILE_PATH_PREFIX + gameID + ".checkpoints.checkpoint" + (checkpointNumber + 1) + ".bound1",
                    bound1
            );
            main.pluginGames.set(
                    GAME_FILE_PATH_PREFIX + gameID + ".checkpoints.checkpoint" + (checkpointNumber + 1) + ".bound2",
                    bound2
            );
            main.pluginGames.set(
                    GAME_FILE_PATH_PREFIX + gameID + ".checkpoints.checkpoint" + (checkpointNumber + 1) + ".isFinish",
                    false
            );

            main.saveGames();
        }
    }

    public int setCheckpointBoundary(String gameID, String checkpointID, String boundary, Location location) {
        if (gameExists(gameID) && checkpointExists(gameID, checkpointID)) {
            switch (boundary) {
                case "bound1":
                    main.pluginGames.set(
                            GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID + ".bound1",
                            location
                    );
                    break;
                case "bound2":
                    main.pluginGames.set(
                            GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID + ".bound2",
                            location
                    );
                    break;
            }
            main.saveGames();
            return 200;
        } else {
            return 404;
        }
    }

    public void setCheckpointDisplayName(String gameID, String checkpointID, String name) {
        if (gameExists(gameID) && checkpointExists(gameID, checkpointID)) {
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID + ".displayName", name);
            main.saveGames();
        }
    }

    public boolean setFinish(String gameID, String checkpointID) {
        if (gameExists(gameID) && checkpointExists(gameID, checkpointID)) {
            for (String checkpoint : getCheckpoints(gameID)) {
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpoint + ".isFinish", false);
            }
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID + ".isFinish", true);

            return main.saveGames();
        } else {
            return false;
        }
    }

    public boolean isFinishingCheckpoint(String gameID, String checkpointID) {
        if (gameExists(gameID) && checkpointExists(gameID, checkpointID)) {
            return main.pluginGames.getBoolean(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID + ".isFinish");
        } else {
            return false;
        }
    }

    public CollidableRegion getCheckpoint(String gameID, String checkpointID) {
        if (gameExists(gameID)) {
            if (checkpointExists(gameID, checkpointID)) {
                return new CollidableRegion(
                        main.pluginGames.getObject(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID + ".bound1", Location.class, null),
                        main.pluginGames.getObject(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID + ".bound2", Location.class, null),
                        main.pluginGames.getString(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID + ".displayName", null),
                        main.pluginGames.getBoolean(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID + ".isFinish", false)
                );
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean checkpointSetup(String gameID, String checkpoint) {
        if (gameExists(gameID)) {
            if (checkpointExists(gameID, checkpoint)) {
                CollidableRegion region;
                try {
                    region = getCheckpoint(gameID, checkpoint);
                    return region.getUpperBoundLocation() != null && region.getLowerBoundLocation() != null;
                } catch (NullPointerException e) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    public void deleteCheckpoint(String gameID, String checkpointID) {
        if (gameExists(gameID)) {
            if (checkpointExists(gameID, checkpointID)) {
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".checkpoints." + checkpointID, null);
                main.saveGames();
            }
        }
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

    public String getActiveGame() {
        return main.pluginData.getString("current.activeGameID");
    }

    public void setActiveGame(String gameID) {
        main.pluginData.set("current.activeGameID", gameID);
        main.saveCoredata();
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

    public String getScoreboardConfigProperty(String scoreboardConfigurationID, ScoreboardLine line) {
        if (!scoreboardConfigurationID.equals("default")) {
            if (line.equals(ScoreboardLine.LINE0)) {
                return main.pluginScoreboardConfig.getString(SCOREBOARD_FILE_PATH_PREFIX + scoreboardConfigurationID + ".title");
            } else {
                return main.pluginScoreboardConfig.getString(SCOREBOARD_FILE_PATH_PREFIX + scoreboardConfigurationID + ".line" + line.asNumber());
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

    public int getPropertyLineNumber(String scoreboardConfigurationID, String property) {
        if (!scoreboardConfigurationID.equalsIgnoreCase("default")) {
            for (String line : main.pluginScoreboardConfig.getConfigurationSection(SCOREBOARD_FILE_PATH_PREFIX + scoreboardConfigurationID).getKeys(false)) {
                if (main.pluginScoreboardConfig.getString(SCOREBOARD_FILE_PATH_PREFIX + scoreboardConfigurationID + "." + line).contains(property)) {
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
        InstanceStatus oldStatus = InstanceStatus.valueOf(main.pluginGames.getString(GAME_FILE_PATH_PREFIX + gameID + ".runState"));
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".runState", runstate.toString());
        main.getServer().getScheduler().runTask(main, () -> {
            main.getServer().getPluginManager().callEvent(new InstanceStatusChangeEvent(
                    gameID,
                    oldStatus,
                    runstate
            ));
        });
        return main.saveGames();
    }

    public boolean globalPlaceholderExists(String text) {
        return main.pluginConfig.contains("placeholders." + text);
    }

    public String getGlobalPlaceholderReplacement(String placeholder) {
        return main.pluginConfig.getString("placeholders." + placeholder);
    }

    public boolean createExclusionList(String listID) {
        main.pluginExclusionLists.set(EXCLUSIONS_FILE_PATH_PREFIX + listID + ".excludedPlayers", new ArrayList<String>());
        return main.saveExclusions();
    }

    public boolean exclusionListExists(String listID) {
        return main.pluginExclusionLists.getConfigurationSection(EXCLUSIONS_FILE_PATH_PREFIX + listID) != null && main.pluginExclusionLists.getList(EXCLUSIONS_FILE_PATH_PREFIX + listID + ".excludedPlayers") != null;
    }

    public boolean deleteExclusionList(String listID) {
        unassignExclusionListFromAll(listID);
        main.pluginExclusionLists.set(EXCLUSIONS_FILE_PATH_PREFIX + listID, null);
        return main.saveExclusions();
    }

    public boolean assignExclusionList(String listID, String gameID) {
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".exclusionList", listID);
        return main.saveGames();
    }

    public boolean unassignExclusionListFromAll(String listID) {
        for (String game : main.pluginGames.getConfigurationSection("games").getKeys(false)) {
            if (main.pluginGames.getString(GAME_FILE_PATH_PREFIX + game + ".exclusionList").equals(listID)) {
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + game + ".exclusionList", "none");
            }
        }
        return main.saveGames();
    }

    public boolean unassignExclusionListFrom(String listID, String gameID) {
        if (getGameProperty(gameID, GameProperty.EXCLUSION_LIST_ID).equalsIgnoreCase(listID)) {
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".exclusionList", "none");
        }
        return main.saveGames();
    }

    public List<String> getGamesWithExclusionList(String listID) {
        List<String> games = new ArrayList<>();
        for (String gameID : getGames()) {
            if (getGameProperty(gameID, GameProperty.EXCLUSION_LIST_ID).equalsIgnoreCase(listID)) {
                games.add(gameID);
            }
        }
        return games;
    }

    public List<String> getGamesWithScoreboardConfig(String configID) {
        List<String> games = new ArrayList<>();
        for (String gameID : getGames()) {
            if (getGameProperty(gameID, GameProperty.SCOREBOARD_CONFIGURATION_ID).equalsIgnoreCase(configID)) {
                games.add(gameID);
            }
        }
        return games;
    }

    public Set<String> getExclusionLists() {
        if (main.pluginExclusionLists.getConfigurationSection("exclusions") != null) {
            return main.pluginExclusionLists.getConfigurationSection("exclusions").getKeys(false);
        } else {
            return new HashSet<>(0);
        }
    }

    public boolean appendExcludedPlayer(Player p, String listID) {
        if (exclusionListExists(listID)) {
            List<String> exclusionList = getExcludedPlayers(listID);
            exclusionList.add(p.getUniqueId().toString());
            main.pluginExclusionLists.set(EXCLUSIONS_FILE_PATH_PREFIX + listID + ".excludedPlayers", exclusionList);
            return main.saveExclusions();
        } else {
            return false;
        }
    }

    public boolean removeExcludedPlayer(Player p, String listID) {
        if (exclusionListExists(listID)) {
            List<String> exclusionList = getExcludedPlayers(listID);
            exclusionList.remove(p.getUniqueId().toString());
            main.pluginExclusionLists.set(EXCLUSIONS_FILE_PATH_PREFIX + listID + ".excludedPlayers", exclusionList);
            return main.saveExclusions();
        } else {
            return false;
        }
    }

    public List<String> getExcludedPlayers(String listID) {
        if (exclusionListExists(listID)) {
            return main.pluginExclusionLists.getList(EXCLUSIONS_FILE_PATH_PREFIX + listID + ".excludedPlayers") != null
                    ? (List<String>) main.pluginExclusionLists.getList(EXCLUSIONS_FILE_PATH_PREFIX + listID + ".excludedPlayers")
                    : new ArrayList<>(0);
        } else {
            return null;
        }
    }

    public List<UUID> getExcludedPlayersUUIDList(String listID) {
        if (exclusionListExists(listID)) {
            List<UUID> result = new ArrayList<>();
            for (String uuidString : (List<String>) main.pluginExclusionLists.getList(EXCLUSIONS_FILE_PATH_PREFIX + listID + ".excludedPlayers")) {
                result.add(UUID.fromString(uuidString));
            }
            return result;
        } else {
            return null;
        }
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
            case EXCLUDE_TARGET_PLAYER_FROM_CHECKPOINT_MESSAGE:
                return main.pluginConfig.getString("options.exclude-target-player-from-event-notification");
            case NOTIFY_STATUS_CHANGE:
                return main.pluginConfig.getString("options.notify-instance-status-change");
            case PAUSE_IF_PLAYER_DISCONNECT:
                return main.pluginConfig.getString("options.pause-if-player-disconnect.state");
            case EMERGENCY_PAUSE_DELAY:
                return main.pluginConfig.getString("options.pause-if-player-disconnect.delay");
            case AUTOREJOIN:
                return main.pluginConfig.getString("options.autorejoin");
            case HALT_PLAYERS_WITH_POTIONEFFECT:
                return main.pluginConfig.getString("options.halt-players-with-effect");
            default:
                throw new InvalidParameterException("Unknown config property provided.");
        }
    }
}
