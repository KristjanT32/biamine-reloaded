package krisapps.biaminereloaded.gameloop;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.events.*;
import krisapps.biaminereloaded.logging.BiaMineLogger;
import krisapps.biaminereloaded.scoreboard.ScoreboardManager;
import krisapps.biaminereloaded.timers.BiathlonTimer;
import krisapps.biaminereloaded.types.*;
import krisapps.biaminereloaded.utilities.ItemDispenserUtility;
import krisapps.biaminereloaded.utilities.ReportUtility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.time.DateTimeException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Game implements Listener {

    public static Game instance;
    public ArrayList<Player> players;
    private final Map<UUID, List<HitInfo>> shootingStats = new LinkedHashMap<>();
    public Location startLocation_bound1 = null;
    public Location startLocation_bound2 = null;
    public boolean isPaused = false;
    public int PREP_TASK = -1;
    BukkitTask cleanupTask;
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
    ItemDispenserUtility dispenser;
    private final Map<UUID, Integer> playerPositions = new HashMap<UUID, Integer>();
    Random random = new Random();
    public LinkedHashMap<Player, FinishInfo> finishedPlayers;

    private final Map<UUID, Location> disconnectedPlayers = new HashMap<>();
    ReportUtility reporter;
    boolean gameInitiated = false;

    public Game(String id, BiamineBiathlon gameInfo, BiamineReloaded main) {
        this.currentGameID = id;
        this.lastGame = id;
        this.currentGameInfo = gameInfo;
        this.main = main;
        this.players = new ArrayList<>();
        this.finishedPlayers = new LinkedHashMap<>();
        this.timer = new BiathlonTimer(main);
        this.scoreboardManager = new ScoreboardManager(main);
        this.dispenser = new ItemDispenserUtility(main);
        this.reporter = new ReportUtility(main);
        this.gameInitiated = false;

        activeGameLogger = new BiaMineLogger("BiaMine", "Active Game", main);
        gameSetupLogger = new BiaMineLogger("BiaMine", "Setup", main);
        instance = this;
    }


    // Event Handlers

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        if (players.contains(event.getPlayer()) && !finishedPlayers.containsKey(event.getPlayer())) {
            activeGameLogger.logInfo("[" + currentGameID + "/Service]: Lost player " + event.getPlayer().getName());
            main.getServer().getPluginManager().callEvent(new BiathlonPlayerKickEvent(KickType.UNINTENTIONAL, event.getPlayer(), currentGameInfo.gameID));
        }
    }

    @EventHandler
    public void onPlayerConnect(PlayerJoinEvent event) {
        if (!Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.AUTOREJOIN))) {
            return;
        }

        if (disconnectedPlayers.containsKey(event.getPlayer().getUniqueId())) {
            activeGameLogger.logInfo("[" + currentGameID + "/Service]: Attempting to rejoin " + event.getPlayer().getName());

            broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.player-rejoin")
                    .replaceAll("%player%", event.getPlayer().getName())
            );

            scheduler.runTaskLater(main, () -> {
                main.messageUtility.sendActionbarMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.player-rejoin-target"));
            }, 3 * 20L);

            if (!players.stream().map((player -> player.getUniqueId())).collect(Collectors.toSet()).contains(event.getPlayer().getUniqueId())) {
                players.add(event.getPlayer());
            } else {
                for (Player p : players) {
                    if (p.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                        players.remove(p);
                        break;
                    }
                }
                players.add(event.getPlayer());
            }
            event.getPlayer().teleport(disconnectedPlayers.get(event.getPlayer().getUniqueId()));
        }
    }

    @EventHandler
    public void onRegionEnter(CheckpointPassEvent event) {
        String time = timer.getFormattedTime();
        if (!event.getRegion().isFinish()) {
            main.messageUtility.sendActionbarMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.checkpoint-reached-target")
                    .replaceAll("%time%", time)
            );
            for (Player p : players) {
                if (p == event.getPlayer() && Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.EXCLUDE_TARGET_PLAYER_FROM_CHECKPOINT_MESSAGE)))
                    continue;

                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.checkpoint-reached")
                        .replaceAll("%checkpoint%", event.getRegion().getRegionName())
                        .replaceAll("%player%", event.getPlayer().getName())
                        .replaceAll("%time%", time)
                );
            }
        } else {
            if (!finishedPlayers.containsKey(event.getPlayer()) && finishedPlayers.size() < players.size()) {
                finishedPlayers.put(
                        event.getPlayer(),
                        new FinishInfo(timer.getFormattedTime(), !finishedPlayers.isEmpty() ? finishedPlayers.size() + 1 : 1)
                );

                currentGameInfo.finishedPlayers = finishedPlayers.size();
                scoreboardManager.refreshScoreboardLine(
                        currentGameInfo,
                        ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig, "%playersNotFinished%")));
                main.messageUtility.sendMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.player-finish")
                        .replaceAll("%player%", event.getPlayer().getName())
                        .replaceAll("%time%", finishedPlayers.get(event.getPlayer()).getFinishTime())
                );
                main.messageUtility.sendActionbarMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.player-finish-target"));

                if (finishedPlayers.size() == players.size()) {
                    finishGame();
                }
            }
        }
    }

    @EventHandler
    public void onStateChange(InstanceStatusChangeEvent event) {
        activeGameLogger.logInfo("[" + currentGameID + "/Status Update] Now: " + event.getNewStatus());
        if (Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.NOTIFY_STATUS_CHANGE))) {
            main.messageUtility.sendMessage(initiator, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.statechange")
                    .replaceAll("%instance%", lastGame)
                    .replaceAll("%old%", event.getOldStatus().name())
                    .replaceAll("%new%", event.getNewStatus().name())
            );
        }
    }

    @EventHandler
    public void onPlayerLeave(BiathlonPlayerKickEvent event) {
        disconnectedPlayers.put(event.getPlayer().getUniqueId(), event.getPlayer().getLocation());
        if (event.getKickType().equals(KickType.UNINTENTIONAL)) {
            if (Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.PAUSE_IF_PLAYER_DISCONNECT))) {
                for (Player p : players) {
                    if (!finishedPlayers.containsKey(p)) {
                        main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.player-disconnected.notice")
                                .replaceAll("%delay%", main.dataUtility.getConfigProperty(ConfigProperty.EMERGENCY_PAUSE_DELAY))
                        );
                    }
                }
                delayPause(Integer.parseInt(main.dataUtility.getConfigProperty(ConfigProperty.EMERGENCY_PAUSE_DELAY)));
            }
        }
    }

    @EventHandler
    public void onTargetHit(BiathlonArrowHitEvent hitEvent) {
        if (players.contains(hitEvent.getShooter()) && !finishedPlayers.containsKey(hitEvent.getShooter())) {
            if (hitEvent.getHitType().equals(HitType.HIT)) {
                shootingStats.get(hitEvent.getShooter().getUniqueId()).add(
                        new HitInfo(
                                hitEvent.getTarget(),
                                hitEvent.getSpot(),
                                HitType.HIT,
                                getItemCount(hitEvent.getShooter().getInventory(), Material.ARROW)
                        ));
                broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-hit")
                        .replaceAll("%player%", hitEvent.getShooter().getName())
                        .replaceAll("%order%", String.valueOf(hitEvent.getTarget()))
                        .replaceAll("%spot%", String.valueOf(hitEvent.getSpot()))
                );
            } else if (hitEvent.getHitType().equals(HitType.MISS)) {
                if (hitEvent.getSpot() != -1) {
                    shootingStats.get(hitEvent.getShooter().getUniqueId()).add(
                            new HitInfo(
                                    hitEvent.getSpot(),
                                    HitType.MISS,
                                    getItemCount(hitEvent.getShooter().getInventory(), Material.ARROW)
                            ));
                    broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-miss")
                            .replaceAll("%player%", hitEvent.getShooter().getName())
                            .replaceAll("%spot%", String.valueOf(hitEvent.getSpot()))
                    );
                } else if (hitEvent.getSpot() == -1) {
                    broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-misfire")
                            .replaceAll("%player%", hitEvent.getShooter().getName())
                    );
                }
            }
        }
    }

    @EventHandler
    public void onShootingSpotEnter(BiathlonShootingSpotEnterEvent enterEvent) {
        activeGameLogger.logInfo("[" + currentGameID + "] Player " + enterEvent.getPlayer().getName() + " ENTERED SHOOTING SPOT #" + enterEvent.getSpotID());
        main.getLogger().info("[" + currentGameID + "] Player " + enterEvent.getPlayer().getName() + " ENTERED SHOOTING SPOT #" + enterEvent.getSpotID());
        playerPositions.put(enterEvent.getPlayer().getUniqueId(), enterEvent.getSpotID());
    }

    @EventHandler
    public void onShootingSpotExit(BiathlonShootingSpotExitEvent exitEvent) {
        activeGameLogger.logInfo("[" + currentGameID + "] Player " + exitEvent.getPlayer().getName() + " EXITED SHOOTING SPOT #" + exitEvent.getSpotID());
        main.getLogger().info("[" + currentGameID + "] Player " + exitEvent.getPlayer().getName() + " EXITED SHOOTING SPOT #" + exitEvent.getSpotID());
        playerPositions.remove(exitEvent.getPlayer().getUniqueId());
    }

    // Control Methods

    public void startGame(CommandSender initiator) {
        this.initiator = initiator;
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (Boolean.parseBoolean(main.dataUtility.getCoreData(CoreDataField.GAME_IN_PROGRESS).toString())) {
            main.messageUtility.sendMessage(initiator, main.localizationUtility.getLocalizedPhrase("gameloop.error-gamerunning"));
            return;
        } else {
            main.dataUtility.saveCoreData(CoreDataField.GAME_IN_PROGRESS, true);
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

        if (Boolean.parseBoolean(main.dataUtility.getCoreData(CoreDataField.GAME_IN_PROGRESS).toString())) {
            main.messageUtility.sendMessage(initiator, main.localizationUtility.getLocalizedPhrase("gameloop.error-gamerunning"));
            return;
        } else {
            main.dataUtility.saveCoreData(CoreDataField.GAME_IN_PROGRESS, true);
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
        terminate(false, TerminationContext.UNKNOWN, "The currently active game was manually terminated.");
    }

    public void pauseGame() {
        if (RESUME_COUNTDOWN_TASK != -1) {
            scheduler.cancelTask(RESUME_COUNTDOWN_TASK);
            RESUME_COUNTDOWN_TASK = -1;
            for (Player p : players) {
                if (!finishedPlayers.containsKey(p)) {
                    main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.game-resume.cancelled"));
                }
            }
        } else {
            for (Player p : players) {
                if (!finishedPlayers.containsKey(p)) {
                    main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.game-paused"));
                }
            }
        }
        activeGameLogger.logInfo("[" + currentGameID + "] PAUSED");
        isPaused = true;
        timer.pauseTimer();
        main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.PAUSED);
    }

    private void delayPause(int delay) {
        scheduler.scheduleAsyncRepeatingTask(main, new Runnable() {
            int counter = delay;

            @Override
            public void run() {
                if (counter > 0) {
                    for (Player p : players) {
                        if (!finishedPlayers.containsKey(p)) {
                            main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.player-disconnected.countdown")
                                    .replaceAll("%num%", String.valueOf(counter))
                            );
                        }
                    }
                    counter--;
                } else {
                    pauseGame();
                }
            }
        }, 0, delay * 20L);
    }

    public void resumeGame() {
        if (RESUME_COUNTDOWN_TASK == -1) {
            activeGameLogger.logInfo("[" + currentGameID + "] RESUMING");
            RESUME_COUNTDOWN_TASK = scheduler.scheduleAsyncRepeatingTask(main, new Runnable() {
                int countdown = 3;

                @Override
                public void run() {
                    if (countdown != 0) {
                        for (Player p : players) {
                            if (!finishedPlayers.containsKey(p)) {
                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.game-resume.countdown")
                                        .replaceAll("%num%", String.valueOf(countdown))
                                );
                            }
                        }
                        countdown--;
                    } else {
                        for (Player p : players) {
                            if (!finishedPlayers.containsKey(p)) {
                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.game-resume.resumed"));
                            }
                        }
                        scheduler.cancelTask(RESUME_COUNTDOWN_TASK);
                        RESUME_COUNTDOWN_TASK = -1;
                        isPaused = false;
                        timer.resumeTimer();
                        main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.RUNNING);
                    }
                }
            }, 0, 20L);
        }
    }


    private void finishGame() {
        reporter.generateGameReport(shootingStats, finishedPlayers, currentGameInfo);
        for (Player p : players) {
            main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.game-finished")
                    .replaceAll("%totalTime%", timer.getFormattedTime())
                    .replaceAll("%totalPlayers%", String.valueOf(players.size()))
            );
            main.messageUtility.sendMessage(p, "&e=================================");
            main.messageUtility.sendMessage(p, "&e¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯");
            main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.leaderboard.header"));
            for (Map.Entry<Player, FinishInfo> finishEntry : finishedPlayers.entrySet()) {
                switch (finishEntry.getValue().getLeaderboardOrder()) {
                    case 1:
                        main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.leaderboard.gold")
                                .replaceAll("%player%", p.getName())
                                .replaceAll("%time%", finishEntry.getValue().getFinishTime())
                        );
                        break;
                    case 2:
                        main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.leaderboard.silver")
                                .replaceAll("%player%", p.getName())
                                .replaceAll("%time%", finishEntry.getValue().getFinishTime())
                        );
                        break;
                    case 3:
                        main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.leaderboard.bronze")
                                .replaceAll("%player%", p.getName())
                                .replaceAll("%time%", finishEntry.getValue().getFinishTime())
                        );
                        break;
                    default:
                        main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.leaderboard.generic")
                                .replaceAll("%player%", p.getName())
                                .replaceAll("%time%", finishEntry.getValue().getFinishTime())
                                .replaceAll("%orderNum%", String.valueOf(finishEntry.getValue().getLeaderboardOrder()))
                        );
                        break;
                }
            }
            main.messageUtility.sendMessage(p, "&e_________________________________");
            main.messageUtility.sendMessage(p, "&e=================================");
        }
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Finishing game ... ");
        terminate(false, TerminationContext.GAME_FINISHED, "");
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
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Prep period started ");

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
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Final countdown started ");
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
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Halting players ");
        Bukkit.getScheduler().runTask(main, new Runnable() {
            @Override
            public void run() {
                for (Player p : players) {
                    if (Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.HALT_PLAYERS_WITH_POTIONEFFECT))) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 999999, 255));
                        main.messageUtility.sendActionbarMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.actionbar.finalcountdown-start"));
                    } else {
                        main.messageUtility.sendActionbarMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.actionbar.finalcountdown-start-nohalt"));
                    }
                }
                dispenseItems();
            }
        });
    }

    private void releasePlayers() {
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Starting run ");
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

    private void dispenseItems() {
        dispenser.dispenseToAll(
                players.stream().filter((player -> !finishedPlayers.containsKey(player)
                )).collect(Collectors.toList()), main.dataUtility.getItemsToDispense(currentGameID));
    }

    private void initScoreboard() {
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Scoreboard initialization started ");
        scoreboardManager.setupScoreboard(currentGameInfo, currentGameID);
    }

    private void initRefreshTask() {
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Registering scoreboard refresh task ");
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
        gameSetupLogger.logInfo("[" + currentGameID + "]: Gathering players for game");
        if (currentGameInfo != null) {
            ArrayList<UUID> excluded = (ArrayList<UUID>) main.dataUtility.getExcludedPlayersUUIDList(currentGameInfo.exclusionList);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!excluded.contains(p.getUniqueId())) {
                    players.add(p);
                    shootingStats.put(p.getUniqueId(), new ArrayList<HitInfo>());
                }
            }
            this.currentGameInfo.totalPlayers = players.size();
            this.currentGameInfo.finishedPlayers = finishedPlayers.size();
        }
    }

    public void teleportToStart() {
        gameSetupLogger.logInfo("[" + currentGameID + "]: Teleporting players to starting area");

        for (Player p : players) {
            Location l = getRandomLocation(startLocation_bound1, startLocation_bound2);
            p.teleport(l);
        }
        startPreparationPeriod();
    }


    // Service Methods

    public int kickPlayer(Player p, @Nullable String reason) {
        if (players.contains(p) && !finishedPlayers.containsKey(p)) {

            activeGameLogger.logInfo("[" + currentGameID + "/Game]: Kicking player " + p.getName());
            players.remove(p);
            scoreboardManager.refreshScoreboardLine(currentGameInfo,
                    ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig, "%playersParticipating%"))
            );
            main.getServer().getPluginManager().callEvent(new BiathlonPlayerKickEvent(KickType.ADMIN_ACTION, p, currentGameInfo.gameID));
            for (Player player : players) {
                if (finishedPlayers.containsKey(player)) {
                    continue;
                }
                main.messageUtility.sendMessage(player, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-kicked")
                        .replaceAll("%player%", p.getName())
                        .replaceAll("%reason%", reason == null ? "-" : reason)
                );
            }
            main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-kicked-target")
                    .replaceAll("%reason%", reason == null ? "Administrative action" : reason)
                    .replaceAll("%game%", currentGameID)
            );
            return 200;
        } else if (!players.contains(p)) {
            activeGameLogger.logInfo("[" + currentGameID + "/Service]: Kicking failed, player not found");
            return 404;
        } else if (players.contains(p) && finishedPlayers.containsKey(p)) {
            activeGameLogger.logInfo("[" + currentGameID + "/Service]: Kicking failed, player already finished.");
            return 302;
        }
        return 500;
    }

    public boolean rejoinPlayer(Player target) {
        if (disconnectedPlayers.containsKey(target.getUniqueId())) {
            activeGameLogger.logInfo("[" + currentGameID + "/Game]: Manual rejoin request for " + target.getName());
            broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.player-rejoin")
                    .replaceAll("%player%", target.getName())
            );
            if (!players.stream().map((player -> player.getUniqueId())).collect(Collectors.toSet()).contains(target.getUniqueId())) {
                players.add(target);
            } else {
                for (Player p : players) {
                    if (p.getUniqueId().equals(target.getUniqueId())) {
                        players.remove(p);
                        break;
                    }
                }
                players.add(target);
            }
            target.teleport(disconnectedPlayers.get(target.getUniqueId()));
            return true;
        } else {
            return false;
        }
    }

    private void terminate(boolean preventative, TerminationContext context, String details) {
        if (!context.equals(TerminationContext.GAME_FINISHED)) {
            announceTermination(context, details);
        }
        this.gameInitiated = false;
        timer.stopGlobalTimer();
        scheduler.cancelTask(PREP_TASK);
        scheduler.cancelTask(COUNTDOWN_TASK);
        if (preventative) {
            main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.PREVTERM);
        } else {
            main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.FINALIZING);
            scheduler.runTaskLater(main, () -> {
                scheduler.cancelTask(REFRESH_TASK);
                scheduler.cancelTasks(main);
                main.dataUtility.setActiveGame(null);
                cleanupTask = scheduler.runTaskLater(main, this::cleanup, 20L * 3);
            }, 20L);
        }
    }

    public void reloadTerminate() {
        main.pluginGames.set("games." + currentGameID + ".runState", InstanceStatus.TERMINATED);
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("internals.reload-termination")));
        activeGameLogger.logInfo("Terminating current game due to server reload...");
        timer.stopGlobalTimer();
        scheduler.cancelTask(PREP_TASK);
        scheduler.cancelTask(COUNTDOWN_TASK);
        scheduler.cancelTask(REFRESH_TASK);
        scheduler.cancelTasks(main);
        scoreboardManager.hideScoreboard();
        scoreboardManager.resetScoreboard();
        this.gameInitiated = false;
        this.currentGameInfo = null;
        this.currentGameID = null;
        this.players = new ArrayList<>();
        this.REFRESH_TASK = -1;
        this.PREP_TASK = -1;
        this.COUNTDOWN_TASK = -1;
        this.startLocation_bound1 = null;
        this.startLocation_bound2 = null;
        HandlerList.unregisterAll(this);
    }

    private void cleanup() {
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Cleaning up");
        this.gameInitiated = false;
        this.currentGameInfo = null;
        this.currentGameID = null;
        this.players = new ArrayList<>();
        this.REFRESH_TASK = -1;
        this.PREP_TASK = -1;
        this.COUNTDOWN_TASK = -1;
        this.startLocation_bound1 = null;
        this.startLocation_bound2 = null;
        HandlerList.unregisterAll(this);
        scheduler.runTaskLater(main, new Runnable() {
            @Override
            public void run() {
                scoreboardManager.hideScoreboard();
                scoreboardManager.resetScoreboard();
            }
        }, 20L * 5);
        main.dataUtility.saveCoreData(CoreDataField.GAME_IN_PROGRESS, false);
        main.dataUtility.updateGameRunstate(lastGame, InstanceStatus.STANDBY);
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

    public void broadcastToEveryone(String txt) {
        for (Player p : players) {
            if (!finishedPlayers.containsKey(p)) {
                main.messageUtility.sendMessage(p, txt);
            }
        }
    }

    public int getItemCount(Inventory inv, Material item) {
        ItemStack[] items = inv.getContents();
        for (ItemStack stack : items) {
            if (stack.getType() == item) {
                return stack.getAmount();
            }
        }
        return 0;
    }

    public int getPlayerSpotID(UUID playerUUID) {
        return playerPositions.get(playerUUID) == null ? -1 : playerPositions.get(playerUUID);
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public HashMap<Player, FinishInfo> getFinishedPlayers() {
        return finishedPlayers;
    }

    public String getLastGame() {
        return lastGame;
    }

    public BiamineBiathlon getCurrentGameInfo() {
        return currentGameInfo;
    }

    public CommandSender getInitiator() {
        return initiator;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
