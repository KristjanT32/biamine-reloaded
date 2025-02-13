package krisapps.biaminereloaded.gameloop;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.events.*;
import krisapps.biaminereloaded.gameloop.types.*;
import krisapps.biaminereloaded.logging.BiaMineLogger;
import krisapps.biaminereloaded.scoreboard.ScoreboardLine;
import krisapps.biaminereloaded.scoreboard.ScoreboardManager;
import krisapps.biaminereloaded.scoreboard.ScoreboardType;
import krisapps.biaminereloaded.timers.BiathlonTimer;
import krisapps.biaminereloaded.timers.TimerFormatter;
import krisapps.biaminereloaded.types.area.AreaType;
import krisapps.biaminereloaded.types.config.ConfigProperty;
import krisapps.biaminereloaded.types.config.CoreDataField;
import krisapps.biaminereloaded.types.config.GameProperty;
import krisapps.biaminereloaded.utilities.ItemDispenserUtility;
import krisapps.biaminereloaded.utilities.ReportUtility;
import krisapps.biaminereloaded.utilities.SoundUtility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
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

    // Statistics
    private final Map<String, BestTimeEntry> bestTimes = new HashMap<>();
    private final LinkedHashMap<Player, FinishInfo> finishedPlayers = new LinkedHashMap<>();
    private final Map<UUID, Integer> playerPositions = new HashMap<>();
    private final Map<UUID, List<HitInfo>> shootingStats = new LinkedHashMap<>();
    private final Map<UUID, List<AreaPassInfo>> arrivalStats = new LinkedHashMap<>();
    private final Map<UUID, List<String>> passedCheckpoints = new LinkedHashMap<>();
    private final Map<UUID, Integer> lapTracker = new HashMap<>();
    private final Map<UUID, Location> disconnectedPlayers = new HashMap<>();

    // Scheduler Task IDs
    public int PREP_TASK = -1;
    public int COUNTDOWN_TASK = -1;
    public int REFRESH_TASK = -1;
    public int RESUME_COUNTDOWN_TASK = -1;
    public int PAUSE_COUNTDOWN_TASK = -1;

    BiaMineLogger activeGameLogger;
    BiaMineLogger gameSetupLogger;
    BiamineReloaded main;
    BiathlonTimer timer;

    BiamineBiathlon currentGameInfo;
    BukkitScheduler scheduler = Bukkit.getScheduler();
    CommandSender initiator;
    public ArrayList<Player> players;
    public boolean isPaused = false;
    BukkitTask cleanupTask;

    ScoreboardManager scoreboardManager;
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

    public Game getInstance() {
        if (instance == null) {
            instance = this;
        }
        return instance;
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

            if (!players.stream().map((Entity::getUniqueId)).collect(Collectors.toSet()).contains(event.getPlayer().getUniqueId())) {
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

        // Record a new 'best' for a checkpoint, if none exist
        if (!bestTimes.containsKey(checkpointID)) {
            if (event.getRegion().isFinish()) {
                if (canFinish(playerUUID)) {
                    activeGameLogger.logInfo("New best time for " + checkpointID + ": " + time);
                    bestTimes.put(checkpointID, new BestTimeEntry(event.getPlayer(), time));
                }
            } else {
                activeGameLogger.logInfo("New best time for " + checkpointID + ": " + time);
                bestTimes.put(checkpointID, new BestTimeEntry(event.getPlayer(), time));
            }
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
            // If there are records of them passing a checkpoint, and a record of them passing the current checkpoint
            if (getPassedCheckpoints(playerUUID).contains(checkpointID)) {
                if (getPassedCheckpoints(playerUUID).size() < main.dataUtility.getCheckpoints(currentGameID).size() - 1) {
                    main.appendToLog("Player " + event.getPlayer().getName() + " attempted to pass through " + checkpointID + " again!");
                    return;
                }
                if (event.getRegion().isFinish()) {
                    return;
                }

                // Clear their passed checkpoints, since they started a new lap
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
                if (event.getRegion().isFinish()) {
                    if (canFinish(playerUUID)) {
                        getPassedCheckpoints(playerUUID).add(checkpointID);
                    }
                } else {
                    getPassedCheckpoints(playerUUID).add(checkpointID);
                }
            }
        }
        if (!event.getRegion().isFinish()) {
            main.messageUtility.sendActionbarMessage(event.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.checkpoint-reached-target")
                    .replaceAll("%time%", time)
            );
            arrivalStats
                    .get(playerUUID)
                    .add(new AreaPassInfo(checkpointID, time, AreaType.CHECKPOINT, lapTracker.get(playerUUID)));

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
                                                                                                   .replaceAll("%lag%",
                                                                                                           TimerFormatter.formatDifference(
                                                                                                                   time,
                                                                                                                   bestTimes
                                                                                                                           .get(checkpointID)
                                                                                                                           .getTime()
                                                                                                           )
                                                                                                   )
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
            // Only register the arrival statistic if the player can finish the game (ignore accidental passing of the finish line)
            if (canFinish(playerUUID)) {
                arrivalStats
                        .get(playerUUID)
                        .add(new AreaPassInfo(checkpointID, time, AreaType.FINISH_LINE, lapTracker.get(playerUUID)));
            }
            registerFinish(event.getPlayer());
        }
    }

    @EventHandler
    public void onInstanceStatusChanged(InstanceStatusChangeEvent event) {
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
                broadcastToEveryone(main.localizationUtility
                        .getLocalizedPhrase("gameloop.runtime.player-disconnected.notice")
                        .replaceAll("%delay%", main.dataUtility.getConfigProperty(ConfigProperty.EMERGENCY_PAUSE_DELAY))
                );
                scheduleDisconnectSuspend(Integer.parseInt(main.dataUtility.getConfigProperty(ConfigProperty.EMERGENCY_PAUSE_DELAY)));
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
                                        getItemCount(hitEvent.getShooter().getInventory(), Material.ARROW),
                                        getLap(hitEvent.getShooter().getUniqueId())
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
                                    getItemCount(hitEvent.getShooter().getInventory(), Material.ARROW),
                                    getLap(hitEvent.getShooter().getUniqueId())
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
        arrivalStats
                .get(enterEvent.getPlayer().getUniqueId())
                .add(new AreaPassInfo("Shooting Spot #" + enterEvent.getSpotID(),
                        time,
                        AreaType.SHOOTING_SPOT,
                        lapTracker.get(enterEvent.getPlayer().getUniqueId())
                ));

        main.messageUtility.sendActionbarMessage(enterEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("gameloop.runtime.spot-claim")
                .replaceAll("%num%", String.valueOf(enterEvent.getSpotID())
                ));
        if (Objects.equals(main.dataUtility.getConfigPropertyRaw("notification-settings.shooting-spot-claim.enabled"), "true")) {
            broadcastToEveryone(main.localizationUtility.getLocalizedPhrase("gameloop.runtime.spot-claim-others")
                    .replaceAll("%player%", enterEvent.getPlayer().getName())
                    .replaceAll("%num%", String.valueOf(enterEvent.getSpotID()))
            );
        }

        if (Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.AUTO_SHOW_SHOOTING_RANGE))) {
            scoreboardManager.forceShowScoreboard(ScoreboardType.SHOOTING_RANGE);
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
        arrivalStats
                .get(exitEvent.getPlayer().getUniqueId())
                .add(new AreaPassInfo("Shooting Spot #" + exitEvent.getSpotID(),
                        time,
                        true,
                        AreaType.SHOOTING_SPOT,
                        lapTracker.get(exitEvent.getPlayer().getUniqueId())
                ));
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



    public void startGame(CommandSender initiator) {
        this.initiator = initiator;
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (initializeGame(initiator)) return;

        gameSetupLogger.logInfo("Starting BiaMine Biathlon instance '" + currentGameID + "'");
        if (!checkStartingArea()) {return;}


        gatherPlayers();
        if (players.isEmpty()) {
            terminate(true, TerminationContext.CANNOT_GATHER_PLAYERS, "Could not gather any players for the game.");
        } else {
            teleportToStart();
            startPreparationPeriod();
            initRefreshTask();
        }
    }

    public void startGame(List<String> pList, CommandSender initiator) {
        this.initiator = initiator;
        if (initializeGame(initiator)) return;

        gameSetupLogger.logInfo("Starting BiaMine Biathlon instance '" + currentGameID + "' with selected players");

        if (!checkStartingArea()) {return;}

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
                    continue;
                }
                initializeBiathlonPlayer(p);
            }
        }

        if (players.isEmpty()) {
            terminate(true, TerminationContext.CANNOT_GATHER_PLAYERS, "No players were eligible to be added to the game.");
        } else {
            this.currentGameInfo.totalPlayers = players.size();
            this.currentGameInfo.finishedPlayers = finishedPlayers.size();
            teleportToStart();
            startPreparationPeriod();
            initRefreshTask();
        }
    }

    private void initializeBiathlonPlayer(Player p) {
        players.add(p);
        shootingStats.put(p.getUniqueId(), new ArrayList<>());
        arrivalStats.put(p.getUniqueId(), new ArrayList<>());
        passedCheckpoints.put(p.getUniqueId(), new ArrayList<>());
        lapTracker.put(p.getUniqueId(), 0);
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

    /**
     * Ensures the start area is complete.
     * This will also preventatively terminate the game if any errors are present in the starting area.
     *
     * @return <code>true</code> if the starting area is complete, <code>false</code> otherwise.
     */
    private boolean checkStartingArea() {
        startLocation_bound1 = main.dataUtility.getStartLocationFirstBound(currentGameID);
        startLocation_bound2 = main.dataUtility.getStartLocationSecondBound(currentGameID);

        if (startLocation_bound1 == null) {
            terminate(true, TerminationContext.BOUND1_MISSING, "Starting area incomplete, bound 1 missing.");
            return false;
        } else if (startLocation_bound2 == null) {
            terminate(true, TerminationContext.BOUND2_MISSING, "Starting area incomplete, bound 2 missing.");
            return false;
        }
        return true;
    }

    /**
     * Gracefully terminates the game.
     */
    public void stopGame() {
        terminate(false, TerminationContext.UNKNOWN, "The currently active game was manually terminated.");
    }

    /**
     * Pauses the game.
     */
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

    /**
     * Resumes the game, if paused.
     */
    public void resumeGame() {
        if (RESUME_COUNTDOWN_TASK == -1) {
            activeGameLogger.logInfo("[" + currentGameID + "] RESUMING");
            RESUME_COUNTDOWN_TASK = scheduler.runTaskTimerAsynchronously(main, new Runnable() {
                int countdown = 3;

                @Override
                public void run() {
                    if (countdown != 0) {
                        for (Player p : players) {
                            if (!finishedPlayers.containsKey(p)) {
                                sounds.playTickSound(p);
                                main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.runtime.game-resume.countdown")
                                                                                           .replaceAll("%num%",
                                                                                                   String.valueOf(
                                                                                                           countdown))
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
            }, 0, 20L).getTaskId();
        }
    }

    /**
     * Schedules a countdown to pause the game due to a disconnected participating player.
     *
     * @param delay The delay before pausing, in seconds.
     */
    private void scheduleDisconnectSuspend(int delay) {
        if (PAUSE_COUNTDOWN_TASK != -1) {return;}

        PAUSE_COUNTDOWN_TASK = scheduler.runTaskTimerAsynchronously(main, new Runnable() {
                    int counter = delay;

                    @Override
                    public void run() {
                        if (counter > 0) {
                            for (Player p : players) {
                                if (!finishedPlayers.containsKey(p)) {
                                    sounds.playTickSound(p);
                                    main.messageUtility.sendMessage(p,
                                            main.localizationUtility
                                                    .getLocalizedPhrase("gameloop.runtime.player-disconnected.countdown")
                                                    .replaceAll("%num%", String.valueOf(counter))
                                    );
                                }
                            }
                            counter--;
                        } else {
                            scheduler.cancelTask(PAUSE_COUNTDOWN_TASK);
                            pauseGame();
                        }
                    }
                }, 0, 20L
        ).getTaskId();
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
            scoreboardManager.refreshPrimaryScoreboardLine(currentGameInfo,
                    ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig, "%timer%")));
        } catch (DateTimeException e) {
            terminate(true, TerminationContext.INVALID_FORMAT, "The supplied timer format is invalid.");
            return;
        }
        try {
            scoreboardManager.refreshPrimaryScoreboardLine(currentGameInfo,
                    ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig, "%dateTime%"))
            );
        } catch (DateTimeException e) {
            terminate(true, TerminationContext.INVALID_FORMAT, "The supplied full date format is invalid.");
            return;
        }
        try {
            scoreboardManager.refreshPrimaryScoreboardLine(currentGameInfo,
                    ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig, "%localTime%"))
            );
        } catch (DateTimeException e) {
            terminate(true, TerminationContext.INVALID_FORMAT, "The supplied clock format is invalid.");
            return;
        }
        try {
            scoreboardManager.refreshScoreboardTitle(currentGameInfo);
        } catch (DateTimeException e) {
            terminate(true,
                    TerminationContext.INVALID_FORMAT,
                    "The supplied title is invalid or contains datetime format errors.");
            return;
        }
        scoreboardManager.refreshPrimaryScoreboardLine(currentGameInfo,
                ScoreboardLine.asEnum(main.dataUtility.getPropertyLineNumber(currentGameInfo.scoreboardConfig,
                        "%state%"
                ))
        );

        for (Player p : players) {
            if (finishedPlayers.containsKey(p)) {
                continue;
            }

            // If the player is in a shooting spot, show the progress of hitting the targets
            if (getPlayerSpotID(p.getUniqueId()) != -1) {
                main.messageUtility.sendActionbarMessage(p, getShootingProgressIndicatorForCurrentLap(p.getUniqueId()));
            } else {
                // Lag behind time
                if (passedCheckpoints.get(p.getUniqueId()) != null) {
                    if (passedCheckpoints.get(p.getUniqueId()).size() < bestTimes.size()) {
                        try {
                            List<String> playerPassedCheckpoints = passedCheckpoints.get(p.getUniqueId());
                            String lastPassedCheckpoint = playerPassedCheckpoints.get(playerPassedCheckpoints.size() - 1);

                            // You can't lag behind yourself, so skip if the best time is by the target player.
                            if (bestTimes.get(lastPassedCheckpoint).getPlayer().getUniqueId() == p.getUniqueId()) {
                                continue;
                            }

                            main.messageUtility.sendActionbarMessage(p,
                                    main.localizationUtility
                                            .getLocalizedPhrase("gameloop.player-lag")
                                            .replace("%lag%",
                                                    TimerFormatter.formatDifference(timer.getFormattedTime(),
                                                            bestTimes.get(lastPassedCheckpoint).getTime()
                                                    )
                                            )
                            );
                        } catch (IndexOutOfBoundsException ignored) {}
                    }
                }
            }
        }
    }



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
        PREP_TASK = scheduler.runTaskTimerAsynchronously(main, new Runnable() {
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
        }, 0, 20).getTaskId();
    }
    private void startFinalCountdown() {
        main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.COUNTDOWN);
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Final countdown started ");

        scheduler.runTask(main, this::teleportToStart);
        haltPlayers();
        COUNTDOWN_TASK = scheduler.runTaskTimerAsynchronously(main, new Runnable() {
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
        }, 0, 20).getTaskId();

    }

    private void haltPlayers() {
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Halting players ");
        Bukkit.getScheduler().runTask(main, () -> {
            for (Player p : players) {
                if (Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.HALT_PLAYERS_WITH_POTIONEFFECT))) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 999999, 255));
                    main.messageUtility.sendActionbarMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.actionbar.finalcountdown-start"));
                } else {
                    main.messageUtility.sendActionbarMessage(p, main.localizationUtility.getLocalizedPhrase("gameloop.actionbar.finalcountdown-start-nohalt"));
                }
            }
            dispenseItems();
        });
    }
    private void releasePlayers() {
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Starting run ");
        main.dataUtility.setActiveGame(currentGameID);
        main.getServer().getPluginManager().registerEvents(this, main);
        Bukkit.getScheduler().runTask(main, () -> {
            for (Player p : players) {
                p.removePotionEffect(PotionEffectType.SLOW);
                sounds.playStartSound(p);
            }
            main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.RUNNING);
            timer.startGlobalTimer(main.dataUtility.getConfigProperty(ConfigProperty.TIMER_FORMAT));
            scoreboardManager.showPrimaryScoreboard();
        });
    }

    private void dispenseItems() {
        dispenser.dispenseToAll(
                players.stream().filter((player -> !finishedPlayers.containsKey(player)
                )).collect(Collectors.toList()), currentGameID, currentGameInfo.shootingsCount);
    }

    private void initScoreboard() {
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Scoreboard initialization started ");
        scoreboardManager.initScoreboardCycle(currentGameInfo);
    }

    private void initRefreshTask() {
        activeGameLogger.logInfo("[" + currentGameID + "/Service]: Registering game refresh task ");

        // Register the task responsible for keeping the game data up to date.
        // This also shows lag time and shooting stats ([] [X] []), as well as refreshing all time-related lines on the scoreboard
        REFRESH_TASK = Bukkit.getScheduler().runTaskTimerAsynchronously(main, () -> {
            if (!isPaused) {
                gameLoop();
            }
                }, 0, 10L
        ).getTaskId();
    }

    private void gatherPlayers() {
        gameSetupLogger.logInfo("[" + currentGameID + "]: Gathering players for game");
        if (currentGameInfo != null) {
            ArrayList<UUID> excluded = (ArrayList<UUID>) main.dataUtility.getExcludedPlayersUUIDList(currentGameInfo.exclusionList);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!excluded.contains(p.getUniqueId())) {
                    initializeBiathlonPlayer(p);
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
    }


    /**
     * Kicks the supplied player from the game.
     * If you wish, you may omit the reason by passing <code>null</code> instead of the <code>reason</code> parameter.
     * @param p The {@link Player} to kick.
     * @param reason The reason for the kick to show to the player, or <code>null</code> if none.
     * @return <code>200</code> if the player was kicked successfully<br><code>404</code> if the player was not found<br><code>302</code> if the player has already finished the game<br><code>500</code> in case of any errors.
     */
    public int kickPlayer(Player p, @Nullable String reason) {
        if (players.contains(p) && !finishedPlayers.containsKey(p)) {

            activeGameLogger.logInfo("[" + currentGameID + "/Game]: Kicking player " + p.getName());
            players.remove(p);
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

    /**
     * Rejoins the supplied player to the game if they've been disconnected.
     * This method will only rejoin the player if they've participated in this game before.
     * @param target The {@link Player} to rejoin.
     * @return <code>true</code> if successful, <code>false</code> if the player hasn't participated in this game.
     */
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

    /**
     * Terminates the game due to a server reload and broadcasts a message to everyone.
     */
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
        scoreboardManager.hidePrimaryScoreboard();
        scoreboardManager.reset();
        resetInstanceVariables();
    }


    // Service methods
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
                    }
            );
        } else {
            main.dataUtility.updateGameRunstate(currentGameID, InstanceStatus.FINALIZING);
            scheduler.runTask(main, () -> {
                        scheduler.cancelTask(REFRESH_TASK);
                        scheduler.cancelTasks(main);
                        main.dataUtility.setActiveGame(null);
                        cleanupTask = scheduler.runTaskLater(main, this::cleanup, 5);
                    }
            );
        }
    }
    private void resetInstanceVariables() {
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
        resetInstanceVariables();
        scheduler.runTaskLater(main, () -> {
            scoreboardManager.hidePrimaryScoreboard();
            scoreboardManager.reset();
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


    // Utility methods
    private int getItemCount(Inventory inv, Material item) {
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

    private String getOrderLettersFor(int num) {
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

    private void broadcastToEveryone(String txt) {
        for (Player p : players) {
            if (!finishedPlayers.containsKey(p)) {
                main.messageUtility.sendMessage(p, txt);
            }
        }
    }

    private int randomInteger(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private Location getRandomLocation(Location loc1, Location loc2) {

        int minX = (int) Math.min(loc1.getX(), loc2.getX());
        int minY = (int) Math.min(loc1.getY(), loc2.getY());
        int minZ = (int) Math.min(loc1.getZ(), loc2.getZ());

        int maxX = (int) Math.max(loc1.getX(), loc2.getX());
        int maxY = (int) Math.max(loc1.getY(), loc2.getY());
        int maxZ = (int) Math.max(loc1.getZ(), loc2.getZ());

        return new Location(loc1.getWorld(),
                randomInteger(minX, maxX),
                randomInteger(minY, maxY),
                randomInteger(minZ, maxZ)
        );
    }

    private boolean canFinish(UUID playerUUID) {
        return ((getLap(playerUUID) >= currentGameInfo.shootingsCount) && currentGameInfo.shootingsCount != 0) && (shootingStats
                .getOrDefault(playerUUID, new ArrayList<>(0))
                .size() >= getShotsRequiredForFinishing());
    }

    private int getShotsRequiredForFinishing() {
        return (main.dataUtility.getTargetsForGame(currentGameID).size() / main.dataUtility
                .getShootingSpots(currentGameID)
                .size()) * currentGameInfo.shootingsCount;
    }

    public String getShootingProgressIndicatorForCurrentLap(UUID player) {
        StringBuilder progress = new StringBuilder();
        LinkedList<HitInfo> shotsForLap = shootingStats
                .get(player)
                .stream()
                .filter(hitInfo -> hitInfo.getLap() == getLap(player))
                .collect(Collectors.toCollection(LinkedList::new));
        int targetsPerSpot = main.dataUtility.getShootingTargetsForSpot(currentGameID, 1).size();

        for (int i = 0; i < targetsPerSpot; i++) {
            if (i < shotsForLap.size()) {
                if (shotsForLap.get(i).getType() == HitType.HIT) {
                    progress.append(" &a☒");
                } else {
                    progress.append(" &c☐");
                }
            } else {
                progress.append(" &f☐");
            }
        }
        return progress.toString().trim();
    }


    /**
     * Gets the Shooting Spot ID for the supplied player.
     *
     * @param playerUUID The {@link UUID} of the player.
     * @return The spot ID, or <code>-1</code> if the player is not currently at a shooting spot.
     */
    public int getPlayerSpotID(UUID playerUUID) {
        return playerPositions.get(playerUUID) == null ? -1 : playerPositions.get(playerUUID);
    }

    /**
     * Gets the information object for this game.
     * @return a {@link BiamineBiathlon} object for this Game instance.
     */
    public BiamineBiathlon getCurrentGameInfo() {
        return currentGameInfo;
    }

    /**
     * Gets the scoreboard manager for this game.
     * @return a {@link ScoreboardManager} for this game.
     */
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    /**
     * Returns a list of all checkpoints' IDs the supplied player has passed.
     * @param p The {@link UUID} of the player
     * @return a {@link List<String>} of checkpoint IDs.
     */
    public List<String> getPassedCheckpoints(UUID p) {
        return passedCheckpoints.get(p);
    }

    /**
     * Gets the lap the supplied player is currently on.
     * @param p The {@link UUID} of the player
     * @return The lap number.
     */
    public int getLap(UUID p) {
        return lapTracker.get(p);
    }

    /**
     * Checks whether the supplied player has finished the game.
     * @param player The {@link Player}
     * @return <code>true</code> if the player has finished, <code>false</code> otherwise.
     */
    public boolean hasFinished(Player player) {
        return finishedPlayers.containsKey(player);
    }

    /**
     * Registers the supplied player as having finished the game.
     *
     * @param player The {@link Player} to register
     */
    public void registerFinish(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!hasFinished(player) && finishedPlayers.size() < players.size()) {

            if ((getLap(playerUUID) < currentGameInfo.shootingsCount) && currentGameInfo.shootingsCount != 0) {
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
            if (shootingStats.getOrDefault(playerUUID, new ArrayList<>(0)).isEmpty() || shootingStats
                    .getOrDefault(playerUUID, new ArrayList<>(0))
                    .size() < getShotsRequiredForFinishing()) {
                main.messageUtility.sendMessage(player,
                        main.localizationUtility.getLocalizedPhrase("gameloop.too-early-shootings")
                );
                for (Player p : players) {
                    if (finishedPlayers.containsKey(p)) {
                        continue;
                    }
                    if (p.getUniqueId() == playerUUID) {
                        continue;
                    }
                    main.messageUtility.sendMessage(p,
                            main.localizationUtility
                                    .getLocalizedPhrase("gameloop.too-early-shootings-others")
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

    /**
     * Gets the best finish time in this game.
     * @return A {@link Map.Entry}, or <code>null</code> if none found.
     */
    public Map.Entry<Player, FinishInfo> getBestFinishTime() {
        if (finishedPlayers.isEmpty()) {
            return null;
        }
        return finishedPlayers.entrySet().stream().findFirst().get();
    }

    public Map<String, BestTimeEntry> getBestTimes() {
        return bestTimes;
    }

    public LinkedHashMap<Player, FinishInfo> getFinishedPlayers() {
        return finishedPlayers;
    }

    public Map<UUID, Integer> getPlayerPositions() {
        return playerPositions;
    }

    public Map<UUID, List<HitInfo>> getShootingStats() {
        return shootingStats;
    }

    public Map<UUID, List<AreaPassInfo>> getArrivalStats() {
        return arrivalStats;
    }

    public Map<UUID, List<String>> getPassedCheckpoints() {
        return passedCheckpoints;
    }

    public Map<UUID, Integer> getLapTracker() {
        return lapTracker;
    }

    public Map<UUID, Location> getDisconnectedPlayers() {
        return disconnectedPlayers;
    }

    public BiathlonTimer getTimer() {
        return timer;
    }

    public List<UUID> getPlayersOnShootingRange() {
        return players
                .stream()
                .map(Entity::getUniqueId)
                .filter(uniqueId -> getPlayerSpotID(uniqueId) != -1)
                .collect(Collectors.toList());
    }
}
