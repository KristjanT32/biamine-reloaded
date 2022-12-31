package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.data.BiaMineDataUtility;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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

    public void setupCurrentLanguageFile() {
        main.getLogger().info("Loading: " + currentLanguage);
        currentLanguage = dataUtility.getCurrentLanguage();
        languageFile = new File(main.getDataFolder(), "/localization/" + currentLanguage + ".yml");

        lang = new YamlConfiguration();

        try {
            lang.load(languageFile);
            main.getLogger().info("Language file loaded successfully!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            main.getLogger().warning("Failed to load " + languageFile.getName() + " due to: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            main.getLogger().warning("Failed to load " + languageFile.getName() + " due to: " + e.getMessage());
        } catch (InvalidConfigurationException e) {
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
        return lang.getString(id) != null ? lang.getString(id) : "Localized phrase not found.";
    }


}
