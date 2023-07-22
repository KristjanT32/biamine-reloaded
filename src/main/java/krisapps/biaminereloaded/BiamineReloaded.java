package krisapps.biaminereloaded;

import krisapps.biaminereloaded.commands.*;
import krisapps.biaminereloaded.utilities.BiaMineDataUtility;
import krisapps.biaminereloaded.utilities.LocalizationUtility;
import krisapps.biaminereloaded.utilities.MessageUtility;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public final class BiamineReloaded extends JavaPlugin {


    public FileConfiguration pluginConfig;
    public File configFile = new File(getDataFolder(), "config.yml");

    public FileConfiguration pluginData;
    public File dataFile = new File(getDataFolder(), "/core-data/data.yml");

    public FileConfiguration pluginGames;
    public File gameFile = new File(getDataFolder(), "/core-data/games.yml");

    public FileConfiguration pluginScoreboardConfig;
    public File scoreboardConfigFile = new File(getDataFolder(), "/customization/scoreboards.yml");

    public FileConfiguration pluginExclusionLists;
    public File exclusionListFile = new File(getDataFolder(), "/customization/exclusions.yml");

    public FileConfiguration pluginLocalization;
    public File localizationFile = new File(getDataFolder(), "/localization/localization.yml");


    // Public Utilities
    public BiaMineDataUtility dataUtility = new BiaMineDataUtility(this);
    public MessageUtility messageUtility = new MessageUtility(this);
    public LocalizationUtility localizationUtility = new LocalizationUtility(this);


    // LOGGING

    File logFile = new File(getDataFolder(), "biamine.log");


    @Override
    public void onEnable() {
        registerCommands();
        registerEvents();
        setupFiles();
    }

    private void setupFiles() {
        if (!configFile.getParentFile().exists() || !configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", true);
        }
        if (!dataFile.getParentFile().exists() || !dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!gameFile.getParentFile().exists() || !gameFile.exists()) {
            gameFile.getParentFile().mkdirs();
            try {
                gameFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!scoreboardConfigFile.getParentFile().exists() || !scoreboardConfigFile.exists()) {
            scoreboardConfigFile.getParentFile().mkdirs();
            try {
                scoreboardConfigFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!exclusionListFile.getParentFile().exists() || !exclusionListFile.exists()) {
            exclusionListFile.getParentFile().mkdirs();
            try {
                exclusionListFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!localizationFile.getParentFile().exists() || !localizationFile.exists()) {
            localizationFile.getParentFile().mkdirs();
            saveResource("localization.yml", true);
            saveResource("en-US.yml", true);
            try {
                Files.move(Path.of(getDataFolder() + "/localization.yml"), localizationFile.toPath());
                Files.move(Path.of(getDataFolder() + "/en-US.yml"), Path.of(getDataFolder().toPath() + "/localization/en-US.yml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        pluginConfig = new YamlConfiguration();
        pluginGames = new YamlConfiguration();
        pluginData = new YamlConfiguration();
        pluginScoreboardConfig = new YamlConfiguration();
        pluginExclusionLists = new YamlConfiguration();
        pluginLocalization = new YamlConfiguration();

        try {
            pluginConfig.load(configFile);
            getLogger().info("Successfully loaded plugin configuration [1/6]");
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("Failed to load the config file: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            pluginGames.load(gameFile);
            getLogger().info("Successfully loaded games [2/6]");
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("Failed to load the game file: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            pluginData.load(dataFile);
            getLogger().info("Successfully loaded data [3/6]");
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("Failed to load the data file: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            pluginScoreboardConfig.load(scoreboardConfigFile);
            getLogger().info("Successfully loaded scoreboard configurations [4/6]");
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("Failed to load the scoreboard configurations file: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            pluginExclusionLists.load(exclusionListFile);
            getLogger().info("Successfully loaded exclusion lists [5/6]");
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("Failed to load the exclusion lists file: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            pluginLocalization.load(localizationFile);
            getLogger().info("Successfully loaded localization information [6/6]");
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("Failed to load the localization information file: " + e.getMessage());
            e.printStackTrace();
        }

        getLogger().info("Files have been loaded, proceeding to load localizations ...");
        loadLocalizations();


    }

    private void loadLocalizations() {
        LocalizationUtility localizationUtility = new LocalizationUtility(this);

        int foundLocalizations = 0;
        ArrayList<String> langList = (ArrayList<String>) pluginLocalization.getList("languages");
        ArrayList<String> missingLocalizations = new ArrayList<>();

        for (String langCode : langList) {
            File langFile = new File(getDataFolder(), "/localization/" + langCode + ".yml");
            if (!langFile.exists()) {
                getLogger().warning("Could not find the localization file for [ " + langCode + " ]");
                missingLocalizations.add(langCode);
            } else {
                getLogger().info("Successfully recognized localization file for [ " + langCode + " ]");
                foundLocalizations++;
            }
        }
        getLogger().info("Localization discovery complete. Found " + foundLocalizations + " localization files out of " + langList.size() + " specified localizations.");
        getLogger().info("Missing localization files: " + Arrays.toString(missingLocalizations.toArray()));
        getLogger().info("Loading language file...");
        localizationUtility.setupCurrentLanguageFile();
    }

    private void registerEvents() {
        getLogger().info("Registering events and event listeners...");


        getLogger().info("Registering events and event listeners complete!");
    }

    private void registerCommands() {
        getLogger().info("Registering commands...");

        getCommand("createbiathlon").setExecutor(new CreateBiathlon(this));
        getCommand("deletebiathlon").setExecutor(new DeleteBiathlon(this));
        getCommand("setstart").setExecutor(new SetStart(this));
        getCommand("listgames").setExecutor(new ListGames(this));
        getCommand("startgame").setExecutor(new StartGame(this));
        getCommand("sconfig").setExecutor(new ScoreboardConfig(this));


        getCommand("testflight").setExecutor(new TestGame(this));

        getLogger().info("Registering commands complete!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void saveAllFiles() {
        try {
            pluginScoreboardConfig.save(scoreboardConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occurred while trying to save the Scoreboard Configurations File.");
        }

        try {
            pluginGames.save(gameFile);
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occurred while trying to save the Game File.");
        }

        try {
            pluginLocalization.save(localizationFile);
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occurred while trying to save the Localization Information File.");
        }

        try {
            pluginData.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occurred while trying to save the Core Data File.");
        }

        try {
            pluginExclusionLists.save(exclusionListFile);
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occured while trying to save the Exclusions File.");
        }

        try {
            pluginConfig.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occurred while trying to save the General Configuration File.");
        }

    }

    public boolean saveGames() {
        try {
            pluginGames.save(gameFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occurred while trying to save the Game File.");
            return false;
        }
    }

    public boolean saveExclusions() {
        try {
            pluginExclusionLists.save(exclusionListFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occured while trying to save the Exclusions File.");
            return false;
        }
    }

    public boolean saveCoredata() {
        try {
            pluginData.save(dataFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occurred while trying to save the Core Data File.");
            return false;
        }
    }

    public boolean saveScoreboards() {
        try {
            pluginScoreboardConfig.save(scoreboardConfigFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("An error occurred while trying to save the Scoreboard Configurations File.");
            return false;
        }
    }

    public void appendToLog(String msg) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
            bw.append(msg);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
