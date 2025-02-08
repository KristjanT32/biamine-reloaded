package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class LocalizationUtility {

    String currentLanguage = "en-US";
    File languageFile;
    FileConfiguration lang;


    BiamineReloaded main;
    BiaMineDataUtility dataUtility;

    public LocalizationUtility(BiamineReloaded main) {
        this.main = main;
        dataUtility = new BiaMineDataUtility(main);
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public FileConfiguration getCurrentLanguageFile() {
        return lang;
    }

    public FileConfiguration getDefaultLanguageFile() {
        File out = new File(Path.of(main.getDataFolder() + "/core-data/temp.yml").toString());
        try {
            FileUtils.copyInputStreamToFile(main.getResource("en-US.yml"), out);
        } catch (IOException e) {
            e.printStackTrace();
            Bukkit.broadcastMessage(ChatColor.RED + "Failed to load internal language file. Error: " + e.getMessage());
        }
        return YamlConfiguration.loadConfiguration(out);
    }

    public void resetDefaultLanguageFile(CommandSender reportTo) {
        try {
            Files.delete(Path.of(main.getDataFolder().toPath() + "/localization/en-US.yml"));
        } catch (IOException e) {
        }

        try {
            Files.copy(main.getResource("en-US.yml"), Path.of(main.getDataFolder().toPath() + "/localization/en-US.yml"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            main.appendToLog("Failed to replace en-US.yml with an internal copy: " + e.getMessage());
            main.messageUtility.sendMessage(reportTo, main.localizationUtility.getLocalizedPhrase("internals.rplanfile-error"));
        }
    }

    public void changeLanguage(String languageCode) {
        this.currentLanguage = languageCode;
        dataUtility.setCurrentLanguage(languageCode);
        setupCurrentLanguageFile();
    }

    public boolean languageFileExists(String languageCode) {
        return new File(main.getDataFolder() + "/localization/" + languageCode + ".yml").exists();
    }

    public List<String> getLanguages() {
        return main.pluginLocalization.getStringList("languages");
    }

    public void setupCurrentLanguageFile() {
        this.lang = null;

        currentLanguage = dataUtility.getCurrentLanguage();
        languageFile = new File(main.getDataFolder(), "/localization/" + currentLanguage + ".yml");

        lang = new YamlConfiguration();

        try {
            lang.load(languageFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            main.getLogger().warning("Failed to load " + languageFile.getName() + " due to: " + e.getMessage());
        }
    }

    /**
     * Gets a phrase from the localization file in the language currently set.
     * This only returns the string, no manipulations are made with it.
     *
     * @param id The phraseID within the localization file.
     * @return The phrase in the current language.
     */

    public String getLocalizedPhrase(String id) {
        if (lang == null) {
            setupCurrentLanguageFile();
        }
        return lang.getString(id) != null ? lang.getString(id) : "Localized string not found: " + id;
    }


}
