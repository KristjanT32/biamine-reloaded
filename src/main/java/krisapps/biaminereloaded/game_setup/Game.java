package krisapps.biaminereloaded.game_setup;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.logging.BiaMineLogger;
import krisapps.biaminereloaded.scoreboard.ScoreboardManager;
import krisapps.biaminereloaded.types.GameProperty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

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
    public int PREP_TASK = -1;
    public int COUNTDOWN_TASK = -1;
    BukkitScheduler scheduler = Bukkit.getScheduler();


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

        int prepTime = Integer.parseInt(main.dataUtility.getGameProperty(currentGameID, GameProperty.PREPARATION_TIME));

        int mins = prepTime / 60;
        int secs = prepTime - mins * prepTime;

        for (Player p : players) {
            main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.phase.prep.start")
                    .replaceAll("%MM%", String.valueOf(mins > 9 ? mins : "0" + mins))
                    .replaceAll("%SS%", String.valueOf(secs > 9 ? secs : "0" + secs))
            );
        }

        PREP_TASK = scheduler.scheduleAsyncRepeatingTask(main, new Runnable() {
            int prepTime = Integer.parseInt(main.dataUtility.getGameProperty(currentGameID, GameProperty.PREPARATION_TIME));

            @Override
            public void run() {
                if (prepTime >= 0) {
                    for (Player p : players) {

                        int mins = prepTime / 60;
                        int secs = prepTime - mins * prepTime;
                        main.messageUtility.sendActionbarMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.timertick.prep")
                                .replaceAll("%MM%", String.valueOf(mins > 9 ? mins : "0" + mins))
                                .replaceAll("%SS%", String.valueOf(secs > 9 ? secs : "0" + secs))
                        );
                    }
                    prepTime--;
                } else {
                    scheduler.cancelTask(PREP_TASK);
                    initScoreboard();
                    startFinalCountdown();
                }
            }
        }, 0, 20);
    }

    private void startFinalCountdown() {
        haltPlayers();
        COUNTDOWN_TASK = scheduler.scheduleAsyncRepeatingTask(main, new Runnable() {
            int countdown = Integer.parseInt(main.dataUtility.getGameProperty(currentGameID, GameProperty.COUNTDOWN_TIME));

            @Override
            public void run() {
                if (countdown >= 0) {
                    for (Player p : players) {
                        switch (countdown) {
                            case 3:
                                p.sendTitle(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.ready.title")),
                                        ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.ready.subtitle")),
                                        0, 20, 0
                                );

                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.timertick.countdown")
                                        .replaceAll("%time%", String.valueOf(countdown))
                                );
                                break;
                            case 2:
                                p.sendTitle(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.set.title")),
                                        ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.set.subtitle")),
                                        0, 20, 0
                                );

                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.timertick.countdown")
                                        .replaceAll("%time%", String.valueOf(countdown))
                                );
                                break;
                            case 1:
                                p.sendTitle(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.go.title")),
                                        ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.go.subtitle")),
                                        0, 20, 0
                                );
                                break;
                            case 0:
                                p.sendTitle(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.release.title")),
                                        ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.release.subtitle")),
                                        0, 20, 0
                                );
                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.timertick.countdown")
                                        .replaceAll("%time%", String.valueOf(countdown))
                                );

                                scheduler.cancelTask(COUNTDOWN_TASK);
                                releasePlayers();
                                break;
                            default:
                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.timertick.countdown")
                                        .replaceAll("%time%", String.valueOf(countdown))
                                );
                                break;
                        }
                    }
                    countdown--;
                }
            }
        }, 0, 20);

    }

    private void haltPlayers() {

    }

    private void releasePlayers() {
        Bukkit.broadcastMessage(ChatColor.GREEN + "TODO.");
    }

    private void initScoreboard() {
        ScoreboardManager manager = new ScoreboardManager(main);
        manager.setupScoreboard(currentGameInfo);
        manager.showScoreboard();
    }


}
