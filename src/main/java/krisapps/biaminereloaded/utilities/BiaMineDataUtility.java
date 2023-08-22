package krisapps.biaminereloaded.utilities;

import com.jeff_media.customblockdata.CustomBlockData;
import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.events.InstanceStatusChangeEvent;
import krisapps.biaminereloaded.types.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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
        return main.pluginData.get(COREDATA_FILE_PATH_PREFIX + field.getField()) == null ? "" : main.pluginData.get(COREDATA_FILE_PATH_PREFIX + field.getField());
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
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange", new ArrayList<String>());

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
            if (main.pluginScoreboardConfig.getConfigurationSection(SCOREBOARD_FILE_PATH_PREFIX + scoreboardConfigurationID) == null) {
                return 404;
            }
            for (String line : main.pluginScoreboardConfig.getConfigurationSection(SCOREBOARD_FILE_PATH_PREFIX + scoreboardConfigurationID).getKeys(false)) {
                if (main.pluginScoreboardConfig.getString(SCOREBOARD_FILE_PATH_PREFIX + scoreboardConfigurationID + "." + line).contains(property)) {
                    if (!line.equalsIgnoreCase("title")) {
                        return Integer.parseInt(line.replace("line", ""));
                    }
                }
            }
        } else {
            for (String line : main.pluginConfig.getConfigurationSection("defaults.scoreboardconfig").getKeys(false)) {
                if (main.pluginConfig.getString("defaults.scoreboardconfig." + line).contains(property)) {
                    if (!line.equalsIgnoreCase("title")) {
                        return Integer.parseInt(line.replace("line", ""));
                    }
                }
            }
        }
        return 404;
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

    public void setGameProperty(String gameID, GameProperty property, String val) {
        main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + "." + property.getFieldName(), val);
        main.saveGames();
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
            return new ArrayList<>();
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
            return new ArrayList<>();
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

    public List<DispenserEntry> getDispenserEntries(String gameID) {
        if (!gameExists(gameID)) {
            return new ArrayList<>(0);
        }
        ConfigurationSection itemSection = main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems");
        if (itemSection != null) {
            List<DispenserEntry> out = new ArrayList<>();
            for (String entry : itemSection.getKeys(false)) {
                out.add(new DispenserEntry(
                        main.pluginGames.getItemStack(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".item"),
                        Objects.equals(main.pluginGames.getString(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".valueType"), "auto")
                ));
            }
            return out;
        } else {
            return new ArrayList<>(0);
        }

    }

    public void enableAutoForDispenserItem(String gameID, Material item) {
        if (!gameExists(gameID)) {
            return;
        }

        ConfigurationSection itemSection = main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems");
        if (!getDispenserEntries(gameID).isEmpty()) {
            for (String entry : itemSection.getKeys(false)) {
                if (main.pluginGames.getItemStack(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".item").getType() == item) {
                    main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".valueType", "auto");
                    main.saveGames();
                    return;
                }
            }
        }
    }

    public void disableAutoForDispenserItem(String gameID, Material item) {
        if (!gameExists(gameID)) {
            return;
        }

        ConfigurationSection itemSection = main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems");
        if (!getDispenserEntries(gameID).isEmpty()) {
            for (String entry : itemSection.getKeys(false)) {
                if (main.pluginGames.getItemStack(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".item").getType() == item) {
                    main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".valueType", "amount");
                    main.saveGames();
                    return;
                }
            }
        }
    }

    public DispenserEntry getItemEntry(String gameID, Material item) {
        if (!gameExists(gameID)) {
            return null;
        }

        ConfigurationSection itemSection = main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems");
        if (itemSection != null) {
            for (String entry : itemSection.getKeys(false)) {
                return new DispenserEntry(
                        main.pluginGames.getItemStack(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".item"),
                        Objects.equals(main.pluginGames.getString(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".valueType"), "auto")
                );
            }
        }
        return null;
    }

    public void removeItemToDispense(String gameID, Material item) {
        if (!gameExists(gameID)) {
            return;
        }

        ConfigurationSection itemSection = main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems");
        if (itemSection != null) {
            for (String entry : itemSection.getKeys(false)) {
                if (main.pluginGames.getObject(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".item", ItemStack.class).getType() == item) {
                    main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry, null);
                    main.saveGames();
                    return;
                }
            }
        }
    }

    public void addItemToDispense(String gameID, ItemStack item, boolean isAuto) {
        if (!gameExists(gameID)) {
            return;
        }

        ConfigurationSection itemSection = main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems");

        if (itemSection != null) {
            for (String itemEntry : itemSection.getKeys(false)) {
                if (main.pluginGames.getObject(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + itemEntry + ".item", ItemStack.class).getType() == item.getType()) {
                    main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + itemEntry + ".item", item);
                    main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + itemEntry + ".valueType", isAuto ? "auto" : "amount");
                    main.saveGames();
                    return;
                }
            }
            int entryID = (itemSection.getKeys(false).size() + 1);
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems.item" + entryID + ".item", item);
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems.item" + entryID + ".valueType", isAuto ? "auto" : "amount");
            main.saveGames();
        } else {
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems.item1" + ".item", item);
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems.item1" + ".valueType", isAuto ? "auto" : "amount");
            main.saveGames();
        }
    }

    public boolean isInDispenserList(String gameID, Material item) {
        if (!gameExists(gameID)) {
            return false;
        }
        ConfigurationSection itemSection = main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems");
        if (itemSection != null) {
            for (String entry : itemSection.getKeys(false)) {
                if (main.pluginGames.getObject(GAME_FILE_PATH_PREFIX + gameID + ".dispenseItems." + entry + ".item", ItemStack.class).getType() == item) {
                    return true;
                }
            }
        }
        return false;
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
            case SEND_ITEM_DISPENSER_MESSAGES:
                return main.pluginConfig.getString("options.send-dispenser-messages");
            default:
                throw new InvalidParameterException("Unknown config property provided.");
        }
    }

    public String getConfigPropertyRaw(String path) {
        if (main.pluginConfig.get(path) != null) {
            return main.pluginConfig.getString(path);
        } else {
            return null;
        }
    }

    @SuppressWarnings("Checked and unchecked casts")
    public List<Location> getShootingTargetsForSpot(String gameID, int rangeSpotID) {
        if (gameExists(gameID)) {
            return (List<Location>) main.pluginGames.getList(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + rangeSpotID + ".targets", new ArrayList<>());
        } else {
            return new ArrayList<>();
        }
    }

    public Set<String> getShootingSpots(String gameID) {
        if (gameExists(gameID)) {
            return main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange").getKeys(false);
        } else {
            return new HashSet<>();
        }
    }

    public boolean addTarget(String gameID, String spotID, Location location) {
        if (gameExists(gameID)) {
            if (shootingSpotExists(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")))) {

                List<Location> targetList = (List<Location>) main.pluginGames.getList(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange." + spotID + ".targets", new ArrayList<>());
                targetList.add(location);
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange." + spotID + ".targets", targetList);

                return main.saveGames();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean removeTarget(String gameID, String spotID, Location location) {
        if (gameExists(gameID)) {
            if (shootingSpotExists(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")))) {
                List<Location> targetList = (List<Location>) main.pluginGames.getList(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange." + spotID + ".targets");
                targetList.remove(location);
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange." + spotID + ".targets", targetList);

                return main.saveGames();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    /**
     * Creates a new shooting spot and assigns the first boundary a location.
     *
     * @param gameID      The target game
     * @param location    The location to set the boundary to
     * @param boundNumber The bound ID (1 or 2)
     * @return whether the operation was successful.
     */
    public boolean setShootingSpotBound(String gameID, Location location, int boundNumber) {
        if (gameExists(gameID)) {
            if (main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange") == null) {
                main.pluginGames.createSection(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange");
            }
            int spotID = main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange").getKeys(false).size() > 0
                    ? main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange").getKeys(false).size() + 1
                    : 1;
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotID + ".bound" + boundNumber, location);
            return main.saveGames();
        } else {
            return false;
        }
    }

    /**
     * Sets an existing shooting spot's boundary location.
     *
     * @param gameID      The target game
     * @param location    The location to set the boundary to
     * @param boundNumber The bound ID (1 or 2)
     * @return whether the operation was successful.
     */
    public boolean setShootingSpotBound(String gameID, int spotID, Location location, int boundNumber) {
        if (gameExists(gameID)) {
            if (!shootingSpotExists(gameID, spotID)) {
                return false;
            }
            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotID + ".bound" + boundNumber, location);
            if (main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotID + ".targets") == null) {
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotID + ".targets", new ArrayList<Location>());
            }
            return main.saveGames();
        } else {
            return false;
        }
    }

    public Location getShootingSpotBound(String gameID, int spotID, int boundNumber) {
        if (gameExists(gameID)) {
            Set<String> spots = getShootingSpots(gameID);
            if (spots.contains("shootingSpot" + spotID)) {
                Location bound = main.pluginGames.getLocation(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotID + ".bound" + boundNumber);
                return bound;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public int getShootingSpotIDByTarget(String gameID, Location target) {
        for (String shootingSpot : getShootingSpots(gameID)) {
            if (getShootingTargetsForSpot(gameID, Integer.parseInt(shootingSpot.replace("shootingSpot", ""))).contains(target)) {
                return Integer.parseInt(shootingSpot.replaceAll("shootingSpot", ""));
            }
        }
        return -1;
    }

    public String getGameByShootingSpot(int shootingSpotID) {
        for (String gameID : getGames()) {
            if (getShootingSpots(gameID).contains("shootingSpot" + shootingSpotID)) {
                return gameID;
            }
        }
        return null;
    }

    public CuboidRegion getShootingSpotRegion(String gameID, String shootingSpotID) {
        if (shootingSpotExists(gameID, Integer.parseInt(shootingSpotID.replaceAll("shootingSpot", "")))) {
            CuboidRegion out = new CuboidRegion(
                    getShootingSpotBound(gameID, Integer.parseInt(shootingSpotID.replace("shootingSpot", "")), 1),
                    getShootingSpotBound(gameID, Integer.parseInt(shootingSpotID.replace("shootingSpot", "")), 2)
            );
            if (out.getBound1() != null && out.getBound2() != null) {
                return out;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<Location> getTargetsForGame(String gameID) {
        List<Location> result = new ArrayList<>();
        if (gameExists(gameID)) {
            for (String spot : getShootingSpots(gameID)) {
                for (Location location : getShootingTargetsForSpot(gameID, Integer.parseInt(spot.replaceAll("shootingSpot", "")))) {
                    result.add(location);
                }
            }
            return result;
        } else {
            return new ArrayList<>();
        }
    }

    public boolean removeShootingSpot(String gameID, int spotID) {
        if (main.pluginGames.get(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotID) != null) {

            removeTargetsFor(gameID, spotID);

            main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotID, null);
            main.saveGames();
            migrateShootingSpotIDs(gameID);
            return true;
        } else {
            return false;
        }
    }

    private void removeTargetsFor(String gameID, int spotID) {
        for (Location target : getShootingTargetsForSpot(gameID, spotID)) {
            for (Block b : CustomBlockData.getBlocksWithCustomData(main, target.getChunk())) {
                CustomBlockData blockData = new CustomBlockData(b, main);
                if (blockData.isEmpty()) {
                    continue;
                }
                if (blockData.get(new NamespacedKey(main, "ownerGameID"), PersistentDataType.STRING).equalsIgnoreCase(gameID)) {
                    if (blockData.get(new NamespacedKey(main, "range_spot_number"), PersistentDataType.INTEGER) == spotID) {
                        main.appendToLog("Deleting shooting target for spot #" + spotID + " of game {" + gameID + "}");
                        blockData.clear();
                        removeTarget(gameID, "shootingSpot" + spotID, target);
                    }
                }
            }
        }
    }

    private void migrateShootingSpotIDs(String gameID) {
        int spotNumber = 1;
        for (String spotID : getShootingSpots(gameID)) {
            if (!spotID.equalsIgnoreCase("shootingSpot" + spotNumber)) {
                main.appendToLog("Shooting spot ID mismatch found: expected " + spotNumber + " |actual " + spotID.replaceAll("shootingSpot", "") + "]: Fixing");
                Location bound1 = getShootingSpotBound(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")), 1);
                Location bound2 = getShootingSpotBound(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")), 2);
                List<Location> targets = getShootingTargetsForSpot(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")));
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange." + spotID, null);
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotNumber + ".bound1", bound1);
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotNumber + ".bound2", bound2);
                main.pluginGames.set(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotNumber + ".targets", targets);
                main.saveGames();
            }
            spotNumber++;
        }
    }

    public boolean shootingSpotExists(String gameID, int spotID) {
        return main.pluginGames.getConfigurationSection(GAME_FILE_PATH_PREFIX + gameID + ".shootingRange.shootingSpot" + spotID) != null;
    }

    public boolean isTarget(Block b) {
        return CustomBlockData.hasCustomBlockData(b, main);
    }

    public Object getTargetData(Block block, TargetProperty property) {
        if (!CustomBlockData.hasCustomBlockData(block, main)) {
            return null;
        }
        CustomBlockData blockData = new CustomBlockData(block, main);
        switch (property) {
            case GAME:
                return blockData.get(new NamespacedKey(main, "ownerGameID"),
                        PersistentDataType.STRING);
            case SPOT:
                return blockData.get(new NamespacedKey(main, "range_spot_number"),
                        PersistentDataType.INTEGER);
            case ORDER:
                return blockData.get(new NamespacedKey(main, "target_number"),
                        PersistentDataType.INTEGER);
            case LOCATION:
                int[] locArray = blockData.get(new NamespacedKey(main, "location"),
                        PersistentDataType.PrimitivePersistentDataType.INTEGER_ARRAY);
                String world = blockData.get(new NamespacedKey(main, "location_world"),
                        PersistentDataType.STRING);
                return new Location(Bukkit.getWorld(world), locArray[0], locArray[1], locArray[2]);
            case WORLD:
                return blockData.get(new NamespacedKey(main, "location_world"),
                        PersistentDataType.STRING);
        }
        return null;
    }

    public void setDataValue(String path, String value) {
        main.pluginData.set(path, value);
        main.saveCoredata();
    }

    public void setTemporaryValue(String key, Object value) {
        if (value == null) {
            main.pluginData.set("temporary." + key, null);
        } else {
            main.pluginData.set("temporary." + key, value.toString());
        }
        main.saveCoredata();
    }

    public String getTemporaryValue(String key) {
        return main.pluginData.getString("temporary." + key);
    }
}
