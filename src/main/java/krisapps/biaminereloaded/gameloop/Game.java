package krisapps.biaminereloaded.gameloop;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.events.*;
import krisapps.biaminereloaded.logging.BiaMineLogger;
import krisapps.biaminereloaded.scoreboard.ScoreboardManager;
import krisapps.biaminereloaded.timers.BiathlonTimer;
import krisapps.biaminereloaded.timers.TimerFormatter;
import krisapps.biaminereloaded.types.*;
import krisapps.biaminereloaded.utilities.ItemDispenserUtility;
import krisapps.biaminereloaded.utilities.ReportUtility;
import krisapps.biaminereloaded.utilities.SoundUtility;
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
    public Location startLocation_bound1 = null;
    public Location startLocation_bound2 = null;
    public final LinkedHashMap<Player, FinishInfo> finishedPlayers = new LinkedHashMap<>();
    // Statistics
    private final Map<String, BestTimeEntry> bestTimes = new HashMap<>();
    public int COUNTDOWN_TASK = -1;
    public int REFRESH_TASK = -1;
    public int RESUME_COUNTDOWN_TASK = -1;

    BiaMineLogger activeGameLogger;
    BiaMineLogger gameSetupLogger;
    BiamineReloaded main;
    BiathlonTimer timer;
    ScoreboardManager scoreboardManager;
    private final Map<UUID, Integer> playerPositions = new HashMap<>();
    private final Map<UUID, List<HitInfo>> shootingStats = new LinkedHashMap<>();
    // Scheduler Task IDs
    public int PREP_TASK = -1;
    BiamineBiathlon currentGameInfo;
    BukkitScheduler scheduler = Bukkit.getScheduler();
    CommandSender initiator;
    public ArrayList<Player> players;
    public boolean isPaused = false;
    private final Map<UUID, List<AreaPassInfo>> arrivalStats = new LinkedHashMap<>();
    private final Map<UUID, List<String>> passedCheckpoints = new LinkedHashMap<>();
    private final Map<UUID, Integer> lapTracker = new HashMap<>();
    BukkitTask cleanupTask;
    private final Map<UUID, Location> disconnectedPlayers = new HashMap<>();
    ItemDispenserUtility dispenser;
    SoundUtility sounds;

    ReportUtility reporter;
    private String currentGameID;
    private final String lastGame;

    public Game(String id, BiamineBiathlon gameInfo, BiamineReloaded main) {
        this.currentGameID = id;
        this.lastGame = id;
        this.currentGameInfo = gameInfo;
        this.main = main;
        this.players = new ArrayList<>();
        this.timer = new BiathlonTimer(main);
        this.scoreboardManager = new ScoreboardManager(main);
        this.dispenser = new ItemDispenserUtility(main);
        this.reporter = new ReportUtility(main);
        this.sounds = new SoundUtility(main);

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
    public void onCheckpointPass(CheckpointPassEvent event) {
        String time = timer.getFormattedTime();
        String checkpointID = event.getRegion().getRegionName();
        UUID playerUUID = event.getPlayer().getUniqueId();

        // Record a new 'best' for a checkpoint, if none exists
        if (!bestTimes.containsKey(checkpointID)) {
            activeGameLogger.logInfo("New best time for " + checkpointID + ": " + time);
            bestTimes.put(checkpointID, new BestTimeEntry(event.getPlayer(), time));
        }

        // If the player has not passed any checkpoints
        if (getPassedCheckpoints(playerUUID).isEmpty()) {
            if (event.getRegion().isFinish()) {
                return;
            }
            getPassedCheckpoints(playerUUID).add(checkpointID);
            if (getLap(playerUUID) == 0) {
                lapTracker.replace(playerUUID, 1);
            } else {
                lapTracker.replace(playerUUID, getLap(playerUUID) + 1);
            }
            main.appendToLog(event.getPlayer().getName() + " has started a new lap: " + getLap(playerUUID));
            if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.new-lap.enabled"), "true")) {
                broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.player-new-lap")
                        .replaceAll("%lap%", String.valueOf(getLap(playerUUID)))
                        .replaceAll("%player%", event.getPlayer().getName())
                        .replaceAll("%orderphrase%", getOrderLettersFor(getLap(playerUUID)))
                );
            }
        } else {
            // If there are records of him passing a checkpoint, and a record of him passing the current checkpoint
            if (getPassedCheckpoints(playerUUID).contains(checkpointID)) {
                if (getPassedCheckpoints(playerUUID).size() < main.dataUtility.getCheckpoints(currentGameID).size() - 1) {
                    main.appendToLog("Player " + event.getPlayer().getName() + " attempted to pass through " + checkpointID + " again!");
                    return;
                }
                if (event.getRegion().isFinish()) {
                    return;
                }

                // Clear his passed checkpoints, since he's started a new lap
                getPassedCheckpoints(playerUUID).clear();
                int curLap = getLap(playerUUID);
                lapTracker.replace(playerUUID, curLap + 1);
                int lap = getLap(playerUUID);

                    main.appendToLog(event.getPlayer().getName() + " has started a new lap: " + lap);
                if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.new-lap.enabled"), "true")) {
                    broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.player-new-lap")
                            .replaceAll("%lap%", String.valueOf(lap))
                            .replaceAll("%player%", event.getPlayer().getName())
                            .replaceAll("%orderphrase%", getOrderLettersFor(lap))
                    );
                }
                bestTimes.replace(checkpointID, new BestTimeEntry(event.getPlayer(), time));
                getPassedCheckpoints(playerUUID).add(checkpointID);
            } else {
                getPassedCheckpoints(playerUUID).add(checkpointID);
            }
        }
        if (!event.getRegion().isFinish()) {
            main.messageUtility.sendActionbarMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.checkpoint-reached-target")
                    .replaceAll("%time%", time)
            );
            arrivalStats.get(playerUUID).add(new AreaPassInfo(checkpointID, time));

            if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.checkpoint-reached.enabled"), "true")) {
                if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.checkpoint-reached.target"), "all")) {
                    if (bestTimes.get(checkpointID).getPlayer().getUniqueId() == playerUUID) {
                        main.messageUtility.sendMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.checkpoint-reached")
                                .replaceAll("%checkpoint%", checkpointID)
                                .replaceAll("%player%", event.getPlayer().getName())
                                .replaceAll("%time%", time)
                        );
                    } else {
                        main.messageUtility.sendMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.checkpoint-reached-lagbehind")
                                .replaceAll("%checkpoint%", checkpointID)
                                .replaceAll("%player%", event.getPlayer().getName())
                                .replaceAll("%time%", time)
                                .replaceAll("%lag%", TimerFormatter.getDifference(time, bestTimes.get(checkpointID).getTime()))
                        );
                    }
                } else {
                    main.messageUtility.sendMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.checkpoint-reached")
                            .replaceAll("%checkpoint%", checkpointID)
                            .replaceAll("%player%", event.getPlayer().getName())
                            .replaceAll("%time%", time)
                    );
                }
            }
        } else {
            finishPlayer(event.getPlayer());
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
                broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.player-disconnected.notice")
                        .replaceAll("%delay%", main.dataUtility.getConfigProperty(ConfigProperty.EMERGENCY_PAUSE_DELAY))
                );
                delayPause(Integer.parseInt(main.dataUtility.getConfigProperty(ConfigProperty.EMERGENCY_PAUSE_DELAY)));
            }
        }
    }

    @EventHandler
    public void onTargetHit(BiathlonArrowHitEvent hitEvent) {
        if (players.contains(hitEvent.getShooter()) && !finishedPlayers.containsKey(hitEvent.getShooter())) {
            if (hitEvent.getHitType().equals(HitType.HIT)) {
                // If the player hit a target, but was outside the shooting spot
                if (hitEvent.getSpot() != -1 && playerPositions.get(hitEvent.getShooter().getUniqueId()) != null) {
                    if (playerPositions.get(hitEvent.getShooter().getUniqueId()).equals(hitEvent.getSpot())) {
                        shootingStats.get(hitEvent.getShooter().getUniqueId()).add(
                                new HitInfo(
                                        hitEvent.getTarget(),
                                        hitEvent.getSpot(),
                                        HitType.HIT,
                                        getItemCount(hitEvent.getShooter().getInventory(), Material.ARROW)
                                ));
                        sounds.playHitSound(hitEvent.getShooter());
                        if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.target-hit.enabled"), "true")) {
                            if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.target-hit.target"), "all")) {
                                broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-hit")
                                        .replaceAll("%player%", hitEvent.getShooter().getName())
                                        .replaceAll("%order%", String.valueOf(hitEvent.getTarget()))
                                        .replaceAll("%spot%", String.valueOf(hitEvent.getSpot()))
                                );
                            } else {
                                main.messageUtility.sendMessage(hitEvent.getShooter(), main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-hit")
                                        .replaceAll("%player%", hitEvent.getShooter().getName())
                                        .replaceAll("%order%", String.valueOf(hitEvent.getTarget()))
                                        .replaceAll("%spot%", String.valueOf(hitEvent.getSpot()))
                                );
                            }
                        }
                    } else {
                        main.messageUtility.sendMessage(hitEvent.getShooter(), main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-misfire")
                                .replaceAll("%player%", hitEvent.getShooter().getName()));
                    }
                } else if (hitEvent.getSpot() == -1) {
                    main.messageUtility.sendMessage(hitEvent.getShooter(), main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-misfire")
                            .replaceAll("%player%", hitEvent.getShooter().getName()));
                }
            } else if (hitEvent.getHitType().equals(HitType.MISS)) {
                // If the player missed a target, but was outside the shooting spot
                if (hitEvent.getSpot() != -1) {
                    shootingStats.get(hitEvent.getShooter().getUniqueId()).add(
                            new HitInfo(
                                    hitEvent.getSpot(),
                                    HitType.MISS,
                                    getItemCount(hitEvent.getShooter().getInventory(), Material.ARROW)
                            ));
                    sounds.playMissSound(hitEvent.getShooter());
                    if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.target-hit.enabled"), "true")) {
                        if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.target-hit.target"), "all")) {
                            broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-miss")
                                    .replaceAll("%player%", hitEvent.getShooter().getName())
                                    .replaceAll("%spot%", String.valueOf(hitEvent.getSpot()))
                            );
                        } else {
                            main.messageUtility.sendMessage(hitEvent.getShooter(), main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-miss")
                                    .replaceAll("%player%", hitEvent.getShooter().getName())
                                    .replaceAll("%spot%", String.valueOf(hitEvent.getSpot()))
                            );
                        }
                    }
                } else if (hitEvent.getSpot() == -1) {
                    main.messageUtility.sendMessage(hitEvent.getShooter(), main.localizationUtility.getLocalizedPhrase("gameloop.runtime.player-target-misfire")
                            .replaceAll("%player%", hitEvent.getShooter().getName()));
                }
            }
        }
    }

    @EventHandler
    public void onShootingSpotEnter(BiathlonShootingSpotEnterEvent enterEvent) {
        activeGameLogger.logInfo("[" + currentGameID + "] Player " + enterEvent.getPlayer().getName() + " ENTERED SHOOTING SPOT #" + enterEvent.getSpotID());
        String time = timer.getFormattedTime();


        playerPositions.put(enterEvent.getPlayer().getUniqueId(), enterEvent.getSpotID());
        arrivalStats.get(enterEvent.getPlayer().getUniqueId()).add(new AreaPassInfo("Shooting Spot #" + enterEvent.getSpotID(), time));

        main.messageUtility.sendActionbarMessage(enterEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.runtime.spot-claim")
                .replaceAll("%num%", String.valueOf(enterEvent.getSpotID())
                ));
        if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.shooting-spot-claim.enabled"), "true")) {
            broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.runtime.spot-claim-others")
                    .replaceAll("%player%", enterEvent.getPlayer().getName())
                    .replaceAll("%num%", String.valueOf(enterEvent.getSpotID()))
            );
        }
    }

    @EventHandler
    public void onOccupiedShootingSpotEnter(BiathlonOccupiedShootingSpotEnterEvent occupiedEvent) {
        activeGameLogger.logInfo("[" + currentGameID + "] Player " + occupiedEvent.getPlayer().getName() + " ATTEMPTED TO CLAIM SHOOTING SPOT #" + occupiedEvent.getSpotID() + " (FAIL, OCCUPIED)");
        main.messageUtility.sendActionbarMessage(occupiedEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.runtime.spot-occupied")
                .replaceAll("%num%", String.valueOf(occupiedEvent.getSpotID())
                ));
    }

    @EventHandler
    public void onShootingSpotExit(BiathlonShootingSpotExitEvent exitEvent) {
        String time = timer.getFormattedTime();

        activeGameLogger.logInfo("[" + currentGameID + "] Player " + exitEvent.getPlayer().getName() + " EXITED SHOOTING SPOT #" + exitEvent.getSpotID());

        playerPositions.remove(exitEvent.getPlayer().getUniqueId());
        arrivalStats.get(exitEvent.getPlayer().getUniqueId()).add(new AreaPassInfo("Shooting Spot #" + exitEvent.getSpotID(), time, true));
        if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.shooting-spot-exit.enabled"), "true")) {
            if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.shooting-spot-exit.target"), "all")) {
                broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.runtime.spot-leave")
                        .replaceAll("%player%", exitEvent.getPlayer().getName())
                        .replaceAll("%num%", String.valueOf(exitEvent.getSpotID()))
                );
            } else {
                main.messageUtility.sendMessage(exitEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.runtime.spot-leave")
                        .replaceAll("%player%", exitEvent.getPlayer().getName())
                        .replaceAll("%num%", String.valueOf(exitEvent.getSpotID())));
            }
        }
    }

    // Control Methods

    public void startGame(CommandSender initiator) {
        this.initiator = initiator;
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (initializeGame(initiator)) return;

        gameSetupLogger.logInfo("Starting BiaMine Biathlon instance '" + currentGameID + "'");
        if (verifyStart()) return;


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
        if (initializeGame(initiator)) return;

        gameSetupLogger.logInfo("Starting BiaMine Biathlon instance '" + currentGameID + "' with selected players");

        if (verifyStart()) return;

        if (!main.dataUtility.scoreboardConfigExists(currentGameInfo.scoreboardConfig)) {
            terminate(true, TerminationContext.SCOREBOARD_CONFIG_MISSING, "There does not appear to be a valid scoreboard configuration assigned to this game.");
            return;
        }

        // Collect players, set their statistics defaults
        for (String playerName : pList) {
            if (Bukkit.getPlayer(playerName) != null) {
                Player p = Bukkit.getPlayer(playerName);
                if (p == null) {
                    main.messageUtility.sendMessage(initiator, main.localizationUtility.getLocalizedPhrase("gameloop.error-invalid-player")
                            .replaceAll("%player%", playerName)
                    );
                }
                players.add(p);
                shootingStats.put(p.getUniqueId(), new ArrayList<>());
                arrivalStats.put(p.getUniqueId(), new ArrayList<>());
                passedCheckpoints.put(p.getUniqueId(), new ArrayList<>());
                lapTracker.put(p.getUniqueId(), 0);
            }
        }

        if (players.isEmpty()) {
            terminate(true, TerminationContext.CANNOT_GATHER_PLAYERS, "No players were eligible to be added to the game.");
        } else {
            this.currentGameInfo.totalPlayers = players.size();
            this.currentGameInfo.finishedPlayers = finishedPlayers.size();
            teleportToStart();
            initRefreshTask();
        }
    }

    private boolean initializeGame(CommandSender initiator) {
        sounds.setEnabled(Boolean.parseBoolean(main.dataUtility.getConfigPropertyRaw("options.sound-effects")));
        if (Boolean.parseBoolean(main.dataUtility.getCoreData(CoreDataField.GAME_IN_PROGRESS).toString())) {
            main.messageUtility.sendMessage(initiator, main.localizationUtility.getLocalizedPhrase("gameloop.error-gamerunning"));
            return true;
        } else {
            main.dataUtility.saveCoreData(CoreDataField.GAME_IN_PROGRESS, true);
        }
        return false;
    }

    private boolean verifyStart() {
        startLocation_bound1 = main.dataUtility.getStartLocationFirstBound(currentGameID);
        startLocation_bound2 = main.dataUtility.getStartLocationSecondBound(currentGameID);

        if (startLocation_bound1 == null) {
            terminate(true, TerminationContext.BOUND1_MISSING, "Starting area incomplete, bound 1 missing.");
            return true;
        } else if (startLocation_bound2 == null) {
            terminate(true, TerminationContext.BOUND2_MISSING, "Starting area incomplete, bound 2 missing.");
            return true;
        }
        return false;
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
                            sounds.playTickSound(p);
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
                                sounds.playTickSound(p);
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
        if (Objects.equals(main.dataUtility.getConfigPropertyRaw("options.game-report.enabled"), "true")) {
            main.messageUtility.sendMessage(initiator, main.localizationUtility.getLocalizedPhrase("gameloop.report-generate"));
            reporter.generateGameReport(shootingStats, finishedPlayers, currentGameInfo, arrivalStats, initiator);
        }
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
                                .replaceAll("%player%", finishEntry.getKey().getName())
                                .replaceAll("%time%", finishEntry.getValue().getFinishTime())
                        );
                        break;
                    case 2:
                        main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.leaderboard.silver")
                                .replaceAll("%player%", finishEntry.getKey().getName())
                                .replaceAll("%time%", finishEntry.getValue().getFinishTime())
                        );
                        break;
                    case 3:
                        main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.leaderboard.bronze")
                                .replaceAll("%player%", finishEntry.getKey().getName())
                                .replaceAll("%time%", finishEntry.getValue().getFinishTime())
                        );
                        break;
                    default:
                        main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.leaderboard.generic")
                                .replaceAll("%player%", finishEntry.getKey().getName())
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

        for (Player p : players) {
            if (finishedPlayers.containsKey(p)) {
                continue;
            }


            // Lag behind time
            if (passedCheckpoints.get(p.getUniqueId()) != null) {
                if (passedCheckpoints.get(p.getUniqueId()).size() < bestTimes.values().size()) {
                    try {
                        List<String> playerPassedCheckpoints = passedCheckpoints.get(p.getUniqueId());
                        String lastPassedCheckpoint = playerPassedCheckpoints.get(playerPassedCheckpoints.size() - 1);

                        // You can't lag behind yourself, so skip if the best time is by the target player.
                        if (bestTimes.get(lastPassedCheckpoint).getPlayer().getUniqueId() == p.getUniqueId()) {
                            continue;
                        }

                        main.messageUtility.sendActionbarMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.player-lag")
                                .replace("%lag%", TimerFormatter.getDifference(timer.getFormattedTime(), bestTimes.get(lastPassedCheckpoint).getTime()))
                        );
                    } catch (IndexOutOfBoundsException ignored) {
                    }
                }
            }
        }
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
                if (countdown >= 1) {

                    for (Player p : players) {
                        if (finishedPlayers.containsKey(p)) {
                            continue;
                        }
                        sounds.playTickSound(p);
                    }

                    switch (countdown) {
                        case 3:
                            for (Player p : players) {
                                if (finishedPlayers.containsKey(p)) {
                                    continue;
                                }
                                p.sendTitle(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.ready.title")),
                                        ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.ready.subtitle")),
                                        0, 20, 0
                                );
                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.timertick.countdown")
                                        .replaceAll("%time%", String.valueOf(countdown))
                                );
                            }
                            break;
                        case 2:
                            for (Player p : players) {
                                if (finishedPlayers.containsKey(p)) {
                                    continue;
                                }
                                p.sendTitle(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.set.title")),
                                        ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.set.subtitle")),
                                        0, 20, 0
                                );
                            }
                            break;
                        case 1:
                            for (Player p : players) {
                                if (finishedPlayers.containsKey(p)) {
                                    continue;
                                }
                                p.sendTitle(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.go.title")),
                                        ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.go.subtitle")),
                                        0, 20, 0
                                );
                            }
                            break;
                    }
                    broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.timertick.countdown")
                            .replaceAll("%time%", String.valueOf(countdown)));
                    // Start the game
                    countdown--;
                } else {
                    for (Player p : players) {
                        if (finishedPlayers.containsKey(p)) {
                            continue;
                        }
                        p.sendTitle(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.release.title")),
                                ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.timertick.release.subtitle")),
                                0, 20, 0
                        );
                    }
                    scheduler.cancelTask(COUNTDOWN_TASK);
                    releasePlayers();
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
                    sounds.playStartSound(p);
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
                )).collect(Collectors.toList()), currentGameID, currentGameInfo.shootingsCount);
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
                    arrivalStats.put(p.getUniqueId(), new ArrayList<>());
                    passedCheckpoints.put(p.getUniqueId(), new ArrayList<>());
                    lapTracker.put(p.getUniqueId(), 0);
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
            scoreboardManager.refreshScoreboardData(currentGameInfo.scoreboardConfig, currentGameInfo);
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
            if (!players.stream().map((Player::getUniqueId)).collect(Collectors.toSet()).contains(target.getUniqueId())) {
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
        timer.stopGlobalTimer();
        scheduler.cancelTask(PREP_TASK);
        scheduler.cancelTask(COUNTDOWN_TASK);
        if (preventative) {
            main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.PREVTERM);
            main.dataUtility.saveCoreData(CoreDataField.GAME_IN_PROGRESS, false);
            scheduler.runTask(main, () -> {
                scheduler.cancelTask(REFRESH_TASK);
                scheduler.cancelTasks(main);
                main.dataUtility.setActiveGame(null);
                cleanupTask = scheduler.runTaskLater(main, this::cleanup, 5);
            });
        } else {
            main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.FINALIZING);
            scheduler.runTask(main, () -> {
                scheduler.cancelTask(REFRESH_TASK);
                scheduler.cancelTasks(main);
                main.dataUtility.setActiveGame(null);
                cleanupTask = scheduler.runTaskLater(main, this::cleanup, 5);
            });
        }
    }

    public void reloadTerminate() {
        main.pluginGames.set("games." + currentGameID + ".runState", InstanceStatus.TERMINATED);
        main.dataUtility.setDataValue("coredata.gameInProgress", String.valueOf(false));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("internals.reload-termination")));
        activeGameLogger.logInfo("Terminating current game due to server reload...");
        timer.stopGlobalTimer();
        scheduler.cancelTask(PREP_TASK);
        scheduler.cancelTask(COUNTDOWN_TASK);
        scheduler.cancelTask(REFRESH_TASK);
        scheduler.cancelTasks(main);
        scoreboardManager.hideScoreboard();
        scoreboardManager.resetScoreboard();
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

    private int randomInteger(int min, int max) {
        return (int) (min + ThreadLocalRandom.current().nextDouble(Math.abs(max - min + 1)));
    }

    public Location getRandomLocation(Location loc1, Location loc2) {

        int minX = (int) Math.min(loc1.getX(), loc2.getX());
        int minY = (int) Math.min(loc1.getY(), loc2.getY());
        int minZ = (int) Math.min(loc1.getZ(), loc2.getZ());

        int maxX = (int) Math.max(loc1.getX(), loc2.getX());
        int maxY = (int) Math.max(loc1.getY(), loc2.getY());
        int maxZ = (int) Math.max(loc1.getZ(), loc2.getZ());

        return new Location(loc1.getWorld(), randomInteger(minX, maxX), randomInteger(minY, maxY), randomInteger(minZ, maxZ));
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
            if (stack == null) {
                continue;
            }
            if (stack.getType() == item) {
                return stack.getAmount();
            }
        }
        return 0;
    }

    public int getPlayerSpotID(UUID playerUUID) {
        return playerPositions.get(playerUUID) == null ? -1 : playerPositions.get(playerUUID);
    }

    public String getOrderLettersFor(int num) {
        String strNum = String.valueOf(num);
        char lastChar = strNum.charAt(strNum.length() - 1);
        switch (Integer.parseInt(String.valueOf(lastChar))) {
            case 1:
                return main.localizationUtility.getLocalizedPhrase("orderphrases.1");
            case 2:
                return main.localizationUtility.getLocalizedPhrase("orderphrases.2");
            case 3:
                return main.localizationUtility.getLocalizedPhrase("orderphrases.3");
            default:
                return main.localizationUtility.getLocalizedPhrase("orderphrases.generic");
        }
    }

    public BiamineBiathlon getCurrentGameInfo() {
        return currentGameInfo;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public List<String> getPassedCheckpoints(UUID p) {
        return passedCheckpoints.get(p);
    }

    public int getLap(UUID p) {
        return lapTracker.get(p);
    }

    public boolean hasFinished(Player player) {
        return finishedPlayers.containsKey(player);
    }

    public void finishPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!hasFinished(player) && finishedPlayers.size() < players.size()) {

            if (getLap(playerUUID) < currentGameInfo.shootingsCount) {
                main.messageUtility.sendMessage(player, main.localizationUtility.getLocalizedPhrase("gameloop.too-early"));
                for (Player p : players) {
                    if (finishedPlayers.containsKey(p)) {
                        continue;
                    }
                    if (p.getUniqueId() == playerUUID) {
                        continue;
                    }
                    main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.too-early-others")
                            .replaceAll("%player%", player.getName())
                    );
                }
                return;
            }

            // Save a FinishInfo object for the finished player.
            finishedPlayers.put(
                    player,
                    new FinishInfo(timer.getFormattedTime(), !finishedPlayers.isEmpty() ? finishedPlayers.size() + 1 : 1)
            );

            currentGameInfo.finishedPlayers = finishedPlayers.size();
            scoreboardManager.refreshScoreboardData(currentGameInfo.scoreboardConfig, currentGameInfo);
            sounds.playFinishSound(player);
            if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.player-finish.enabled"), "true")) {
                if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.player-finish.target"), "all")) {
                    main.messageUtility.sendMessage(player, main.localizationUtility.getLocalizedPhrase("gameloop.player-finish")
                            .replaceAll("%player%", player.getName())
                            .replaceAll("%time%", finishedPlayers.get(player).getFinishTime())
                    );
                    main.messageUtility.sendActionbarMessage(player, main.localizationUtility.getLocalizedPhrase("gameloop.player-finish-target"));
                } else {
                    main.messageUtility.sendActionbarMessage(player, main.localizationUtility.getLocalizedPhrase("gameloop.player-finish-target"));
                }
            }

            if (finishedPlayers.size() == players.size()) {
                finishGame();
            }
        }
    }

    public Map.Entry<Player, FinishInfo> getBestFinishTime() {
        if (finishedPlayers.isEmpty()) {
            return null;
        }
        return finishedPlayers.entrySet().stream().findFirst().get();
    }
}
