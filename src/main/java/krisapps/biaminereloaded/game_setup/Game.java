package krisapps.biaminereloaded.game_setup;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.logging.BiaMineLogger;
import krisapps.biaminereloaded.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Game {

    public ArrayList<Player> players;
    public Location startLocation_bound1;
    public Location startLocation_bound2;

    BiaMineLogger activeGameLogger;
    BiaMineLogger gameSetupLogger;
    BiamineReloaded main;

    String currentGameID;
    BiamineBiathlon currentGameInfo;

    public Game(String id, BiamineBiathlon gameInfo, BiamineReloaded main) {
        this.currentGameID = id;
        this.currentGameInfo = gameInfo;
        this.main = main;
        this.players = new ArrayList<>();

        activeGameLogger = new BiaMineLogger("BiaMine", "Active Game", main);
        gameSetupLogger = new BiaMineLogger("BiaMine", "Game Setup", main);
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

    private void gatherPlayers() {
        gameSetupLogger.logInfo("Gathering players...");
        if (currentGameInfo != null) {
            ArrayList<String> excluded = main.dataUtility.getExclusionListByID(currentGameInfo.exclusionList);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!excluded.contains(p.getName())) {
                    players.add(p);
                }
            }
        }
    }


    public void teleportToStart() {
        gameSetupLogger.logInfo("Teleporting players to the start...");
        for (Player p : players) {
            p.teleport(getRandomLocation(startLocation_bound1, startLocation_bound2));
        }
    }

    public void startGame() {
        gameSetupLogger.logInfo("Starting BiaMine Biathlon instance '" + currentGameID + "' [...]");

        startLocation_bound1 = main.dataUtility.getStartLocationFirstBound(currentGameID);
        startLocation_bound2 = main.dataUtility.getStartLocationSecondBound(currentGameID);

        gatherPlayers();
        teleportToStart();
        startPreparationPeriod();
    }

    private void startPreparationPeriod() {
        Runnable prep = new Runnable() {
            @Override
            public void run() {

            }
        };
        prep.run();
        initScoreboard();
        startFinalCountdown();
    }

    private void startFinalCountdown() {
        haltPlayers();
        Runnable final_countdown = new Runnable() {
            @Override
            public void run() {

            }
        };

    }

    private void haltPlayers() {

    }

    private void releasePlayers() {

    }

    private void initScoreboard() {
        ScoreboardManager manager = new ScoreboardManager(main);
        manager.setupScoreboard(currentGameInfo);
        manager.showScoreboard();
    }


}
