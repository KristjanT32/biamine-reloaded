package krisapps.biaminereloaded.gameloop;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.events.CheckpointPassEvent;
import krisapps.biaminereloaded.events.InstanceStatusChangeEvent;
import krisapps.biaminereloaded.logging.BiaMineLogger;
import krisapps.biaminereloaded.scoreboard.ScoreboardManager;
import krisapps.biaminereloaded.timers.BiathlonTimer;
import krisapps.biaminereloaded.types.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Game implements Listener {

    public ArrayList<Player> players;
    public ArrayList<Player> finishedPlayers;
    public Location startLocation_bound1 = null;
    public Location startLocation_bound2 = null;
    public boolean isPaused = false;
    public int PREP_TASK = -1;
    public int COUNTDOWN_TASK = -1;
    public int REFRESH_TASK = -1;
    public int RESUME_COUNTDOWN_TASK = -1;
    BiaMineLogger activeGameLogger;
    BiaMineLogger gameSetupLogger;
    BiamineReloaded main;
    BiathlonTimer timer;
    ScoreboardManager scoreboardManager;
    String currentGameID;
    String lastGame;
    BiamineBiathlon currentGameInfo;
    BukkitScheduler scheduler = Bukkit.getScheduler();
    CommandSender initiator;
    Random random = new Random();

    public Game(String id, BiamineBiathlon gameInfo, BiamineReloaded main) {
        this.currentGameID = id;
        this.lastGame = id;
        this.currentGameInfo = gameInfo;
        this.main = main;
        this.players = new ArrayList<>();
        this.finishedPlayers = new ArrayList<>();
        this.timer = new BiathlonTimer(main);
        this.scoreboardManager = new ScoreboardManager(main);

        activeGameLogger = new BiaMineLogger("BiaMine", "Active Game", main);
        gameSetupLogger = new BiaMineLogger("BiaMine", "Game Setup", main);
    }

    // Event Handlers
    @EventHandler
    public void onRegionEnter(CheckpointPassEvent event) {
        if (!event.getRegion().isFinish()) {
            main.messageUtility.sendActionbarMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.checkpoint-reached-target"));
            for (Player p : players) {
                if (p.getUniqueId().equals(event.getPlayer().getUniqueId()) && Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.EXCLUDE_TARGET_PLAYER_FROM_CHECKPOINT_MESSAGE)))
                    continue;
                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.checkpoint-reached")
                        .replaceAll("%checkpoint%", event.getRegion().getRegionName())
                        .replaceAll("%player%", event.getPlayer().getName())
                        .replaceAll("%time%", timer.getFormattedTime())
                );
            }
        } else {
            finishedPlayers.add(event.getPlayer());
            players.remove(event.getPlayer());

            this.currentGameInfo.finishedPlayers = finishedPlayers.size();
            scoreboardManager.refreshScoreboardLine(
                    currentGameInfo,
                    ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(this.currentGameInfo.scoreboardConfig, "%playersNotFinished%")));

        }
    }

    @EventHandler
    public void onStateChange(InstanceStatusChangeEvent event) {
        if (Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.NOTIFY_STATUS_CHANGE))) {
            main.messageUtility.sendMessage(initiator, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.statechange")
                    .replaceAll("%instance%", lastGame)
                    .replaceAll("%old%", event.getOldStatus().name())
                    .replaceAll("%new%", event.getNewStatus().name())
            );
        }
    }

    // Control Methods

    public void startGame(CommandSender initiator) {
        this.initiator = initiator;
        if (REFRESH_TASK != -1) {
            main.messageUtility.sendMessage(initiator, main.localizationUtility.getLocalizedPhrase("gameloop.error-gamerunning"));
            return;
        }

        gameSetupLogger.logInfo("Starting BiaMine Biathlon instance '" + currentGameID + "' [...]");
        startLocation_bound1 = main.dataUtility.getStartLocationFirstBound(currentGameID);
        startLocation_bound2 = main.dataUtility.getStartLocationSecondBound(currentGameID);

        if (startLocation_bound1 == null) {
            terminate(true, TerminationContext.BOUND1_MISSING, "Starting area incomplete, bound 1 missing.");
            return;
        } else if (startLocation_bound2 == null) {
            terminate(true, TerminationContext.BOUND2_MISSING, "Starting area incomplete, bound 2 missing.");
            return;
        }



        gatherPlayers();
        if (players.isEmpty()) {
            terminate(true, TerminationContext.CANNOT_GATHER_PLAYERS, "Could not gather any players for the game.");
        } else {
            teleportToStart();
            initRefreshTask();
        }
    }

    public void startGame(List<String> pList, CommandSender initiator) {
        this.initiator = initiator;
        if (REFRESH_TASK != -1) {
            main.messageUtility.sendMessage(initiator, main.localizationUtility.getLocalizedPhrase("gameloop.error-gamerunning"));
            return;
        }

        gameSetupLogger.logInfo("Starting BiaMine Biathlon instance '" + currentGameID + "' with selected players [...]");

        startLocation_bound1 = main.dataUtility.getStartLocationFirstBound(currentGameID);
        startLocation_bound2 = main.dataUtility.getStartLocationSecondBound(currentGameID);

        if (startLocation_bound1 == null) {
            terminate(true, TerminationContext.BOUND1_MISSING, "Starting area incomplete, bound 1 missing.");
            return;
        } else if (startLocation_bound2 == null) {
            terminate(true, TerminationContext.BOUND2_MISSING, "Starting area incomplete, bound 2 missing.");
            return;
        }

        if (!main.dataUtility.scoreboardConfigExists(currentGameInfo.scoreboardConfig)) {
            terminate(true, TerminationContext.SCOREBOARD_CONFIG_MISSING, "There does not appear to be a valid scoreboard configuration assigned to this game.");
            return;
        }


        for (String playerName : pList) {
            if (Bukkit.getPlayer(playerName) != null) {
                players.add(Bukkit.getPlayer(playerName));
            }
        }

        if (players.isEmpty()) {
            terminate(true, TerminationContext.CANNOT_GATHER_PLAYERS, "No players were eligible to be added to the game.");
        } else {
            teleportToStart();
            initRefreshTask();
        }
    }

    public void stopGame() {
        terminate(false, TerminationContext.UNKNOWN, "The currently active game was stopped manually via '/terminate'.");
    }

    public void pauseGame() {

        if (RESUME_COUNTDOWN_TASK != -1) {
            scheduler.cancelTask(RESUME_COUNTDOWN_TASK);
            RESUME_COUNTDOWN_TASK = -1;
            for (Player p : players) {
                if (!finishedPlayers.contains(p)) {
                    main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.game-resume.cancelled"));
                }
            }
        } else {
            for (Player p : players) {
                if (!finishedPlayers.contains(p)) {
                    main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.game-paused"));
                }
            }
        }
        isPaused = true;
        main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.PAUSED);
    }

    public void resumeGame() {
        if (RESUME_COUNTDOWN_TASK == -1) {
            RESUME_COUNTDOWN_TASK = scheduler.scheduleAsyncRepeatingTask(main, new Runnable() {
                int countdown = 3;

                @Override
                public void run() {
                    if (countdown != 0) {
                        for (Player p : players) {
                            if (!finishedPlayers.contains(p)) {
                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.game-resume.countdown")
                                        .replaceAll("%num%", String.valueOf(countdown))
                                );
                            }
                        }
                        countdown--;
                    } else {
                        for (Player p : players) {
                            if (!finishedPlayers.contains(p)) {
                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.game-resume.resumed"));
                            }
                        }
                        scheduler.cancelTask(RESUME_COUNTDOWN_TASK);
                        RESUME_COUNTDOWN_TASK = -1;
                        isPaused = false;
                        main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.RUNNING);
                    }
                }
            }, 0, 20L);
        }
    }

    private void gameLoop() {
        // Refresh timer
        currentGameInfo.latestTime = timer.getFormattedTime();

        try {
            scoreboardManager.refreshScoreboardLine(currentGameInfo,
                    ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig, "%timer%")));
        } catch (DateTimeException e) {
            terminate(true, TerminationContext.INVALID_FORMAT, "Problem with timer format.");
            return;
        }
        try {
            scoreboardManager.refreshScoreboardLine(currentGameInfo,
                    ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig, "%dateTime%"))
            );
        } catch (DateTimeException e) {
            terminate(true, TerminationContext.INVALID_FORMAT, "Problem with full date format.");
            return;
        }
        try {
            scoreboardManager.refreshScoreboardLine(currentGameInfo,
                    ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig, "%localTime%"))
            );
        } catch (DateTimeException e) {
            terminate(true, TerminationContext.INVALID_FORMAT, "Problem with clock format.");
            return;
        }
        try {
            scoreboardManager.refreshScoreboardTitle(currentGameInfo);
        } catch (DateTimeException e) {
            terminate(true, TerminationContext.INVALID_FORMAT, "Problem with title");
            return;
        }
        scoreboardManager.refreshScoreboardLine(currentGameInfo, ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig, "%state%")));
    }

    // Phase Methods

    private void startPreparationPeriod() {

        main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.PREP);

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
        main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.COUNTDOWN);
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
                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.timertick.countdown")
                                        .replaceAll("%time%", String.valueOf(countdown))
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

                                // Start the game
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
        Bukkit.getScheduler().runTask(main, new Runnable() {
            @Override
            public void run() {
                for (Player p : players) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 999999, 255));
                    main.messageUtility.sendActionbarMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.actionbar.finalcountdown-start"));
                }
            }
        });
    }

    private void releasePlayers() {
        main.dataUtility.setActiveGame(currentGameID);
        main.getServer().getPluginManager().registerEvents(this, main);
        Bukkit.getScheduler().runTask(main, new Runnable() {
            @Override
            public void run() {
                for (Player p : players) {
                    p.removePotionEffect(PotionEffectType.SLOW);
                }
                main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.RUNNING);
                timer.startGlobalTimer(main.dataUtility.getConfigProperty(ConfigProperty.TIMER_FORMAT));
                scoreboardManager.showScoreboard();
            }
        });
    }

    private void initScoreboard() {
        scoreboardManager.setupScoreboard(currentGameInfo, currentGameID);
    }

    private void initRefreshTask() {
        REFRESH_TASK = Bukkit.getScheduler().scheduleAsyncRepeatingTask(main, new Runnable() {
            @Override
            public void run() {
                if (!isPaused) {
                    gameLoop();
                }
            }
        }, 0, 20L);
    }


    private void gatherPlayers() {
        gameSetupLogger.logInfo("Gathering players...");
        if (currentGameInfo != null) {
            ArrayList<UUID> excluded = main.dataUtility.getExclusionListByID(currentGameInfo.exclusionList);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!excluded.contains(p.getUniqueId())) {
                    players.add(p);
                }
            }
            this.currentGameInfo.totalPlayers = players.size();
            this.currentGameInfo.finishedPlayers = finishedPlayers.size();
        }
    }

    public void teleportToStart() {
        gameSetupLogger.logInfo("Teleporting players to the start...");

        for (Player p : players) {
            Location l = getRandomLocation(startLocation_bound1, startLocation_bound2);
            p.teleport(l);
        }
        startPreparationPeriod();
    }


    // Service Methods

    private void terminate(boolean preventative, TerminationContext context, String details) {
        announceTermination(context, details);
        timer.stopGlobalTimer();
        scheduler.cancelTask(REFRESH_TASK);
        scheduler.cancelTask(PREP_TASK);
        scheduler.cancelTask(COUNTDOWN_TASK);
        scheduler.cancelTasks(main);
        if (preventative) {
            main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.PREVTERM);
        } else {
            main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.CLEANUP);
            cleanup();
        }
    }

    private void cleanup() {
        this.currentGameInfo = null;
        this.currentGameID = null;
        this.players = new ArrayList<>();
        this.REFRESH_TASK = -1;
        this.PREP_TASK = -1;
        this.COUNTDOWN_TASK = -1;
        this.startLocation_bound1 = null;
        this.startLocation_bound2 = null;
        scheduler.runTaskLater(main, new Runnable() {
            @Override
            public void run() {
                scoreboardManager.hideScoreboard();
                scoreboardManager.resetScoreboard();
            }
        }, 20L * 5);
    }

    private void announceTermination(TerminationContext context, String msg) {
        switch (context) {
            case BOUND1_MISSING:
            case BOUND2_MISSING:
                main.messageUtility.sendMessage(initiator, "&e================================================");
                main.messageUtility.sendMessage(initiator,
                        main.localizationUtility.getLocalizedPhrase("gameloop.runtime.prevterm-nostart")
                                .replaceAll("%instance%", currentGameID)
                                .replaceAll("%details%", msg)
                );
                main.messageUtility.sendMessage(initiator, "&e================================================");
                break;
            case CANNOT_GATHER_PLAYERS:
                main.messageUtility.sendMessage(initiator, "&e================================================");
                main.messageUtility.sendMessage(initiator,
                        main.localizationUtility.getLocalizedPhrase("gameloop.runtime.prevterm-nopl")
                                .replaceAll("%instance%", currentGameID)
                );
                main.messageUtility.sendMessage(initiator, "&e================================================");
                break;
            case NO_START:
                main.messageUtility.sendMessage(initiator, "&e================================================");
                main.messageUtility.sendMessage(initiator,
                        main.localizationUtility.getLocalizedPhrase("gameloop.runtime.prevterm-nostartarea")
                                .replaceAll("%instance%", currentGameID)
                );
                main.messageUtility.sendMessage(initiator, "&e================================================");
                break;
            case NO_FINISH:
                main.messageUtility.sendMessage(initiator, "&e================================================");
                main.messageUtility.sendMessage(initiator,
                        main.localizationUtility.getLocalizedPhrase("gameloop.runtime.prevterm-nofinish")
                                .replaceAll("%instance%", currentGameID)
                                .replaceAll("%details%", msg)
                );
                main.messageUtility.sendMessage(initiator, "&e================================================");
                break;
            case UNKNOWN:
                main.messageUtility.sendMessage(initiator, "&e================================================");
                if (msg.isEmpty()) {
                    main.messageUtility.sendMessage(initiator,
                            main.localizationUtility.getLocalizedPhrase("gameloop.runtime.terminated-generic")
                                    .replaceAll("%instance%", currentGameID)
                    );
                } else {
                    main.messageUtility.sendMessage(initiator,
                            main.localizationUtility.getLocalizedPhrase("gameloop.runtime.terminated-generic-reason")
                                    .replaceAll("%instance%", currentGameID)
                                    .replaceAll("%reason%", msg)
                    );
                }
                main.messageUtility.sendMessage(initiator, "&e================================================");
                break;
            case INVALID_FORMAT:
                main.messageUtility.sendMessage(initiator, "&e================================================");
                main.messageUtility.sendMessage(initiator,
                        main.localizationUtility.getLocalizedPhrase("gameloop.runtime.prevterm-sb-format")
                                .replaceAll("%instance%", currentGameID)
                                .replaceAll("%details%", msg)
                );
                main.messageUtility.sendMessage(initiator, "&e================================================");
                break;
            case SCOREBOARD_CONFIG_MISSING:
                main.messageUtility.sendMessage(initiator, "&e================================================");
                main.messageUtility.sendMessage(initiator,
                        main.localizationUtility.getLocalizedPhrase("gameloop.runtime.prevterm-sb-noconf")
                                .replaceAll("%instance%", currentGameID)
                                .replaceAll("%details%", msg)
                );
                main.messageUtility.sendMessage(initiator, "&e================================================");
                break;
        }
    }

    private double randomDouble(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble(Math.abs(max - min + 1));
    }

    public Location getRandomLocation(Location loc1, Location loc2) {

        double minX = Math.min(loc1.getX(), loc2.getX());
        double minY = Math.min(loc1.getY(), loc2.getY());
        double minZ = Math.min(loc1.getZ(), loc2.getZ());

        double maxX = Math.max(loc1.getX(), loc2.getX());
        double maxY = Math.max(loc1.getY(), loc2.getY());
        double maxZ = Math.max(loc1.getZ(), loc2.getZ());

        return new Location(loc1.getWorld(), randomDouble(minX, maxX), randomDouble(minY, maxY), randomDouble(minZ, maxZ));
    }


}
