package krisapps.biaminereloaded.game_setup;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.logging.BiaMineLogger;
import krisapps.biaminereloaded.utilities.LocalizationUtility;
import krisapps.biaminereloaded.utilities.MessageUtility;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Game {

    public Player[] players;
    public Location startLocation_bound1;
    public Location startLocation_bound2;

    BiaMineLogger activeGameLogger;
    BiaMineLogger gameSetupLogger;
    BiamineReloaded main;

    String currentGameID;
    BiamineBiathlon currentGameInfo;
    MessageUtility messages;
    LocalizationUtility localizationUtility;

    public Game(String id, BiamineBiathlon gameInfo, BiamineReloaded main) {
        this.currentGameID = id;
        this.currentGameInfo = gameInfo;
        this.main = main;

        activeGameLogger = new BiaMineLogger("BiaMine", "Active Game", main);
        gameSetupLogger = new BiaMineLogger("BiaMine", "Game Setup", main);
        messages = new MessageUtility(main);
        localizationUtility = new LocalizationUtility(main);
    }

    Random random = new Random();

    public Location getRandomLocation(Location loc1, Location loc2) {
        double minX = Math.min(loc1.getX(), loc2.getX());
        double minY = Math.min(loc1.getY(), loc2.getY());
        double minZ = Math.min(loc1.getZ(), loc2.getZ());

        double maxX = Math.max(loc1.getX(), loc2.getX());
        double maxY = Math.max(loc1.getY(), loc2.getY());
        double maxZ = Math.max(loc1.getZ(), loc2.getZ());

        return new Location(loc1.getWorld(), randomDouble(minX, maxX), randomDouble(minY, maxY), randomDouble(minZ, maxZ));
    }

    public double randomDouble(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble(Math.abs(max - min + 1));
    }


    public void teleportToStart() {
        gameSetupLogger.logInfo("Teleporting players to the start...");
        for (Player p : players) {
            p.teleport(getRandomLocation(startLocation_bound1, startLocation_bound2));
        }
    }

    public void startGame() {
        gameSetupLogger.logInfo("Starting BiaMine Biathlon instance '" + currentGameID + "' [...]");
        teleportToStart();
        startPreparationPeriod();
    }

    private void startPreparationPeriod() {

    }

    private void startFinalCountdown() {

    }

    private void haltPlayers() {

    }

    private void releasePlayers() {

    }


}
