package krisapps.biaminereloaded.scoreboard;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.gameloop.BiamineBiathlon;
import krisapps.biaminereloaded.gameloop.Game;
import krisapps.biaminereloaded.gameloop.types.InstanceStatus;
import krisapps.biaminereloaded.logging.BiaMineLogger;
import krisapps.biaminereloaded.timers.TimerFormatter;
import krisapps.biaminereloaded.types.config.ConfigProperty;
import krisapps.biaminereloaded.types.config.GameProperty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ScoreboardManager {

    private static final int SCOREBOARD_FIRST_LINE = 9;
    private final String[] lineFillerSymbols = {
            ChatColor.BLACK + String.valueOf(ChatColor.WHITE),
            ChatColor.BLACK + String.valueOf(ChatColor.RED),
            ChatColor.BLACK + String.valueOf(ChatColor.YELLOW),
            ChatColor.BLACK + String.valueOf(ChatColor.GREEN),
            ChatColor.BLACK + String.valueOf(ChatColor.BLACK),
            ChatColor.BLACK + String.valueOf(ChatColor.DARK_PURPLE),
            ChatColor.BLACK + String.valueOf(ChatColor.LIGHT_PURPLE),
            ChatColor.BLACK + String.valueOf(ChatColor.DARK_GREEN)
    };

    private static final String[] PLACEHOLDERS = {"%bestTime%", "%dateTime%", "%localTime%", "%date%", "%footer%", "%header%", "%shootings%", "%playersNotFinished", "%playersFinished", "%playersParticipating", "%timer%", "%empty%", "%bestTime%"};

    Scoreboard mainScoreboard;

    // The task for refreshing the currently active scoreboard during the cycle.
    private final int SCOREBOARD_REFRESH_TASK = -1;

    // The task for advancing the visible scoreboard type to the next one / next page of current one.
    private final int SCOREBOARD_ADVANCE_TASK = -1;

    // One line length: nickname (max: 16ch) + space (1ch)
    private final int SHOOTING_STAT_NICKNAME_SEGMENT_LENGTH = 17;

    // The primary scoreboard
    Objective gameObjective;

    BiamineReloaded main;

    // The scoreboard used for previewing the scoreboard config
    Objective previewObjective;

    // The scoreboard shown when one or more players are shooting
    Objective shootingStatsObjective;

    // The scoreboard periodically shown with the players sorted by their times
    Objective playersScoreboardObjective;

    BiaMineLogger logger;
    BukkitScheduler scheduler = Bukkit.getScheduler();


    private ScoreboardType currentScoreboardCycleBoard = ScoreboardType.PRIMARY;

    // Specifies whether the current scoreboard switch cycle should be skipped.
    private boolean cycleOverrideActive = false;


    private final HashMap<ScoreboardType, PaginationInfo> scoreboardPaginationInfo = new HashMap<>();

    public ScoreboardManager(BiamineReloaded main) {
        this.main = main;
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        this.logger = new BiaMineLogger("BiaMine", "Scoreboard", main);

        this.scoreboardPaginationInfo.put(ScoreboardType.LEADERBOARD, new PaginationInfo(1, 1, 5));
        this.scoreboardPaginationInfo.put(ScoreboardType.SHOOTING_RANGE, new PaginationInfo(1, 1, 5));

        // Register all scoreboards' objectives.
        Arrays.stream(ScoreboardType.values()).forEach(this::registerObjective);
    }

    /**
     * Starts the scoreboard refresh cycle for the supplied game.
     * This will also ensure the scoreboard is shown, if it wasn't already.
     *
     * @param game The game configuration.
     */
    @SuppressWarnings("ConstantConditions")
    public void initScoreboardCycle(BiamineBiathlon game) {
        if (SCOREBOARD_REFRESH_TASK != -1) {
            scheduler.cancelTask(SCOREBOARD_REFRESH_TASK);
            logger.logInfo("Cancelled existing scoreboard refresh task.");
        }

        if (SCOREBOARD_ADVANCE_TASK != -1) {
            scheduler.cancelTask(SCOREBOARD_ADVANCE_TASK);
            logger.logInfo("Cancelled existing scoreboard advance task.");
        }

        if (!main.dataUtility.scoreboardConfigExists(game.scoreboardConfig)) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    main.localizationUtility.getLocalizedPhrase("internals.setupsb-err-invconf")
            ));
            return;
        }

        playersScoreboardObjective.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                main.localizationUtility
                        .getLocalizedPhrase("gameloop.scoreboard.leaderboard.title")
                        .replace("%gameName%", main.dataUtility.getGameProperty(game.gameID, GameProperty.DISPLAY_NAME))
        ));
        shootingStatsObjective.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                main.localizationUtility
                        .getLocalizedPhrase("gameloop.scoreboard.shooting-stats.title")
                        .replace("%gameName%", main.dataUtility.getGameProperty(game.gameID, GameProperty.DISPLAY_NAME))
        ));

        scheduler.runTaskTimerAsynchronously(main, () -> {
                    renderScoreboards(game);
                }, 0L, 10L
        );

        scheduler.runTaskTimerAsynchronously(main,
                this::advanceScoreboardType,
                20L * 10L,
                20L * Long.parseLong(main.dataUtility.getConfigProperty(ConfigProperty.SCOREBOARD_CYCLE_PERIOD))
        );

        logger.logInfo("Started scoreboard refresh cycle for '" + game.gameID + "'");
    }

    private void renderScoreboards(BiamineBiathlon gameInfo) {
        if (currentScoreboardCycleBoard == null) {return;}
        if (mainScoreboard.getObjective(DisplaySlot.SIDEBAR) != null) {
            if (!mainScoreboard
                    .getObjective(DisplaySlot.SIDEBAR)
                    .getName()
                    .equals(currentScoreboardCycleBoard.getObjectiveName())) {
                mainScoreboard
                        .getObjective(currentScoreboardCycleBoard.getObjectiveName())
                        .setDisplaySlot(DisplaySlot.SIDEBAR);
            }
        } else {
            mainScoreboard
                    .getObjective(currentScoreboardCycleBoard.getObjectiveName())
                    .setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        switch (currentScoreboardCycleBoard) {
            case PRIMARY:
                refreshGameScoreboard(gameInfo);
                break;
            case LEADERBOARD:
                refreshLeaderboard(gameInfo);
                break;
            case SHOOTING_RANGE:
                refreshShootingScoreboard(gameInfo);
                break;
            case PREVIEW:
                break;
        }
    }

    private void advanceScoreboardType() {
        if (cycleOverrideActive) {
            cycleOverrideActive = false;
            return;
        }

        PaginationInfo paginationInfo = scoreboardPaginationInfo.get(currentScoreboardCycleBoard);
        switch (currentScoreboardCycleBoard) {
            case PRIMARY:
                if (shouldSkipScoreboard(ScoreboardType.LEADERBOARD)) {
                    if (!shouldSkipScoreboard(ScoreboardType.SHOOTING_RANGE)) {
                        currentScoreboardCycleBoard = ScoreboardType.SHOOTING_RANGE;
                    }
                } else {
                    currentScoreboardCycleBoard = ScoreboardType.LEADERBOARD;
                }
                break;
            case LEADERBOARD:
                // If the current scoreboard has pages to show, instead of changing the scoreboard, paginate forward.
                if (paginationInfo.getTotalPages() > 1) {
                    if (paginationInfo.getCurrentPage() < paginationInfo.getTotalPages()) {
                        paginationInfo.setCurrentPage(paginationInfo.getCurrentPage() + 1);
                        scoreboardPaginationInfo.replace(currentScoreboardCycleBoard, paginationInfo);
                    } else {
                        paginationInfo.setCurrentPage(1);
                        scoreboardPaginationInfo.replace(currentScoreboardCycleBoard, paginationInfo);
                        if (!shouldSkipScoreboard(ScoreboardType.SHOOTING_RANGE)) {
                            currentScoreboardCycleBoard = ScoreboardType.SHOOTING_RANGE;
                        } else {
                            currentScoreboardCycleBoard = ScoreboardType.PRIMARY;
                        }
                    }
                    return;
                } else {
                    if (!shouldSkipScoreboard(ScoreboardType.SHOOTING_RANGE)) {
                        currentScoreboardCycleBoard = ScoreboardType.SHOOTING_RANGE;
                    } else {
                        currentScoreboardCycleBoard = ScoreboardType.PRIMARY;
                    }
                }
                break;
            case SHOOTING_RANGE:
                // If the current scoreboard has pages to show, instead of changing the scoreboard, paginate forward.
                if (paginationInfo.getTotalPages() > 1) {
                    if (paginationInfo.getCurrentPage() < paginationInfo.getTotalPages()) {
                        paginationInfo.setCurrentPage(paginationInfo.getCurrentPage() + 1);
                        scoreboardPaginationInfo.replace(currentScoreboardCycleBoard, paginationInfo);
                    } else {
                        paginationInfo.setCurrentPage(1);
                        scoreboardPaginationInfo.replace(currentScoreboardCycleBoard, paginationInfo);
                        if (!(Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.KEEP_SHOOTING_RANGE_IF_ALL_PRESENT)) && Game.instance
                                .getPlayersOnShootingRange()
                                .size() == Game.instance.players.size())) {
                            currentScoreboardCycleBoard = ScoreboardType.PRIMARY;
                        }
                    }
                } else {
                    if (!(Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.KEEP_SHOOTING_RANGE_IF_ALL_PRESENT)) && Game.instance
                            .getPlayersOnShootingRange()
                            .size() == Game.instance.players.size())) {
                        currentScoreboardCycleBoard = ScoreboardType.PRIMARY;
                    }
                }
                break;
        }
    }

    private void refreshGameScoreboard(BiamineBiathlon gameInfo) {
        String scoreboardConfigurationID = gameInfo.scoreboardConfig;

        String l1 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE1);
        String l2 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE2);
        String l3 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE3);
        String l4 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE4);
        String l5 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE5);
        String l6 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE6);
        String l7 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE7);
        String l8 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE8);

        setScoreboardLine(ScoreboardType.PRIMARY, 1, "line1", findReplacePlaceholders(l1, gameInfo));
        setScoreboardLine(ScoreboardType.PRIMARY, 2, "line2", findReplacePlaceholders(l2, gameInfo));
        setScoreboardLine(ScoreboardType.PRIMARY, 3, "line3", findReplacePlaceholders(l3, gameInfo));
        setScoreboardLine(ScoreboardType.PRIMARY, 4, "line4", findReplacePlaceholders(l4, gameInfo));
        setScoreboardLine(ScoreboardType.PRIMARY, 5, "line5", findReplacePlaceholders(l5, gameInfo));
        setScoreboardLine(ScoreboardType.PRIMARY, 6, "line6", findReplacePlaceholders(l6, gameInfo));
        setScoreboardLine(ScoreboardType.PRIMARY, 7, "line7", findReplacePlaceholders(l7, gameInfo));
        setScoreboardLine(ScoreboardType.PRIMARY, 8, "line8", findReplacePlaceholders(l8, gameInfo));
    }

    private void refreshShootingScoreboard(BiamineBiathlon gameInfo) {
        List<String> playerProgresses = new ArrayList<>();

        for (Player p : Game.instance.players) {
            if (Game.instance.hasFinished(p)) {continue;}
            if (Game.instance.getPlayerSpotID(p.getUniqueId()) != -1) {
                playerProgresses.add("&b" + p.getName() + " ".repeat(SHOOTING_STAT_NICKNAME_SEGMENT_LENGTH - p
                        .getName()
                        .length()) + Game.instance.getShootingProgressIndicatorForCurrentLap(p.getUniqueId()));
            }
        }

        scoreboardPaginationInfo.computeIfPresent(ScoreboardType.SHOOTING_RANGE, (k, v) -> {
                    v.setTotalPages((playerProgresses.size() - 1) / v.getItemsPerPage());
                    return v;
                }
        );

        int page = scoreboardPaginationInfo.get(ScoreboardType.SHOOTING_RANGE).getCurrentPage();
        int maxPages = scoreboardPaginationInfo.get(ScoreboardType.SHOOTING_RANGE).getTotalPages();
        if (maxPages == 0) {maxPages = 1;}
        setScoreboardLine(ScoreboardType.SHOOTING_RANGE,
                1,
                "s_line1", main.localizationUtility.getLocalizedPhrase("gameloop.scoreboard.shooting-stats.header")
        );

        int paginationAdjustment = ((scoreboardPaginationInfo
                .get(ScoreboardType.SHOOTING_RANGE)
                .getCurrentPage() - 1) * scoreboardPaginationInfo.get(ScoreboardType.SHOOTING_RANGE).getItemsPerPage());

        // Fills lines 2-7 with player progresses, if possible.
        for (int i = 2; i < 8; i++) {
            if ((i - 2) > playerProgresses.size() - 1) {
                setScoreboardLine(ScoreboardType.SHOOTING_RANGE, i, "s_line" + i, "&8* ---");
            } else {
                setScoreboardLine(ScoreboardType.SHOOTING_RANGE,
                        i,
                        "s_line" + i,
                        ((i - 1) + (paginationAdjustment)) + ". " + playerProgresses.get((i - 2) + (paginationAdjustment))
                );
            }
        }
        setScoreboardLine(ScoreboardType.SHOOTING_RANGE,
                8,
                "s_line8",
                main.localizationUtility.getLocalizedPhrase("gameloop.scoreboard.shooting-stats.footer")
                                        .replace("%page%", (page > 9 ? page : "0" + page).toString())
                                        .replace("%maxPages%", (maxPages > 9 ? maxPages : "0" + maxPages).toString())
        );
    }

    private void refreshLeaderboard(BiamineBiathlon gameInfo) {
        HashMap<Long, String> entries = new LinkedHashMap<>();
        LinkedList<Long> sortedTimes;

        // Map all players' lag times to their scoreboard entries.
        for (Player p : Game.instance.players) {
            if (Game.instance.hasFinished(p) && !Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.INCLUDE_FINISHED_PLAYERS_IN_LEADERBOARD))) {
                continue;
            }
            long playerLag = Game.instance.getLagTime(p);

            if (playerLag == 0) {
                entries.put(0L, "&b&l" + p.getName());
            } else {
                entries.put((Game.instance.getTimer().getElapsedSeconds() - playerLag),
                        "&b" + p.getName() + " ".repeat(SHOOTING_STAT_NICKNAME_SEGMENT_LENGTH - p
                                .getName()
                                .length()) + "&f+&c" + TimerFormatter.formatTimer(Math.toIntExact(Game.instance
                                .getTimer()
                                .getElapsedSeconds() - playerLag))
                );
            }
        }

        // Sort the entries' keys in descending order
        sortedTimes = entries.keySet().stream().sorted().collect(Collectors.toCollection(LinkedList::new));


        // Map the now sorted keys back to their values into a new map.
        LinkedList<Map.Entry<Long, String>> sortedEntries = new LinkedList<>();
        sortedTimes.forEach(time -> {
            sortedEntries.add(new AbstractMap.SimpleEntry<>(time, entries.get(time)));
        });

        scoreboardPaginationInfo.computeIfPresent(ScoreboardType.LEADERBOARD, (k, v) -> {
                    v.setTotalPages((sortedEntries.size() - 1) / v.getItemsPerPage());
                    return v;
                }
        );

        int page = scoreboardPaginationInfo.get(ScoreboardType.LEADERBOARD).getCurrentPage();
        int maxPages = scoreboardPaginationInfo.get(ScoreboardType.LEADERBOARD).getTotalPages();
        if (maxPages == 0) {maxPages = 1;}

        setScoreboardLine(ScoreboardType.LEADERBOARD,
                1,
                "l_line1", main.localizationUtility.getLocalizedPhrase("gameloop.scoreboard.leaderboard.header")
        );

        int paginationAdjustment = ((scoreboardPaginationInfo
                .get(ScoreboardType.LEADERBOARD)
                .getCurrentPage() - 1) * scoreboardPaginationInfo.get(ScoreboardType.LEADERBOARD).getItemsPerPage());

        for (int i = 2; i < 8; i++) {
            if ((i - 2) > sortedEntries.size() - 1) {
                setScoreboardLine(ScoreboardType.LEADERBOARD, i, "l_line" + i, "&8* ---");
            } else {
                setScoreboardLine(ScoreboardType.LEADERBOARD,
                        i,
                        "l_line" + i,
                        ((i - 1) + (paginationAdjustment)) + ". " + sortedEntries
                                .get((i - 2) + (paginationAdjustment))
                                .getValue()
                );
            }
        }
        setScoreboardLine(ScoreboardType.LEADERBOARD,
                8,
                "l_line8",
                main.localizationUtility.getLocalizedPhrase("gameloop.scoreboard.leaderboard.footer")
                                        .replace("%page%", (page > 9 ? page : "0" + page).toString())
                                        .replace("%maxPages%", (maxPages > 9 ? maxPages : "0" + maxPages).toString())
        );
    }

    /**
     * Requests the supplied scoreboard type to be shown immediately.
     * This method will do nothing, if the supplied scoreboard type
     * has been disabled in the configuration file.
     *
     * @param type The scoreboard type to show - only {@link ScoreboardType#PRIMARY}, {@link ScoreboardType#LEADERBOARD} and {@link ScoreboardType#SHOOTING_RANGE} are supported.
     */
    public void forceShowScoreboard(ScoreboardType type) {
        if (type == ScoreboardType.PREVIEW) {return;}

        logger.logInfo("Force-showing scoreboard of type " + type);
        if (type == ScoreboardType.SHOOTING_RANGE && !Boolean.parseBoolean(main.dataUtility.getConfigProperty(
                ConfigProperty.SHOOTING_RANGE_SCOREBOARD_ENABLED))) {
            logger.logInfo("The Shooting Range scoreboard has been disabled in the config and will not be shown.");
            return;
        }
        if (type == ScoreboardType.LEADERBOARD && !Boolean.parseBoolean(main.dataUtility.getConfigProperty(
                ConfigProperty.LEADERBOARD_ENABLED))) {
            logger.logInfo("The Leaderboard scoreboard has been disabled in the config and will not be shown.");
            return;
        }
        cycleOverrideActive = true;
        currentScoreboardCycleBoard = type;
    }

    private boolean shouldSkipScoreboard(ScoreboardType type) {
        if (type == ScoreboardType.PREVIEW || type == ScoreboardType.PRIMARY) {return false;}
        if (type == ScoreboardType.LEADERBOARD) {
            return Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.SKIP_EMPTY_SCOREBOARDS)) && Game.instance.players.size() <= 1 || !Boolean.parseBoolean(
                    main.dataUtility.getConfigProperty(ConfigProperty.LEADERBOARD_ENABLED));
        }
        if (type == ScoreboardType.SHOOTING_RANGE) {
            return Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.SKIP_EMPTY_SCOREBOARDS)) && Game.instance
                    .getPlayersOnShootingRange()
                    .isEmpty() || !Boolean.parseBoolean(main.dataUtility.getConfigProperty(ConfigProperty.SHOOTING_RANGE_SCOREBOARD_ENABLED));
        }
        return false;
    }

    /**
     * Sets the supplied scoreboard's n-th line's content to be equal to <code>content</code>.
     *
     * @param scoreboard The scoreboard whose line to modify.
     * @param line       The line number (1-8)
     * @param lineId     The access key for the supplied line.
     * @param content    The content for the line.
     */
    private void setScoreboardLine(ScoreboardType scoreboard, int line, String lineId, String content) {
        if (!objectiveExists(scoreboard)) {
            registerObjective(scoreboard);
        }
        Objective o = getObjective(scoreboard);

        // Safeguard: if a line is already occupied, meaning a team already exists, clear it (to re-register later)
        if (mainScoreboard.getTeam(lineId) != null) {
            // Has the actual content changed?
            if (mainScoreboard
                    .getTeam(lineId)
                    .getPrefix()
                    .equals(ChatColor.translateAlternateColorCodes('&', content))) {
                logger.logInfo("Skipping update for " + lineId + "(line: " + line + ") since content has not changed.");
                return;
            }
            try {
                mainScoreboard.getTeam(lineId).unregister();
            } catch (IllegalStateException e) {
                logger.logError("Failed to register new scoreboard team for line '" + lineId + "' - " + e.getMessage());
                return;
            }
        }
        Team propertyToSet = mainScoreboard.registerNewTeam(lineId);

        // Add a team entry with an empty name to allow for empty strings to appear on the scoreboard.
        propertyToSet.addEntry(lineFillerSymbols[line - 1]);

        // Add the actual line content as the prefix of the team.
        propertyToSet.setPrefix(ChatColor.translateAlternateColorCodes('&', content));

        // Set the line team's score to ensure correct line order.
        o.getScore(lineFillerSymbols[line - 1]).setScore(SCOREBOARD_FIRST_LINE - line);
    }


    private boolean objectiveExists(ScoreboardType scoreboard) {
        return mainScoreboard.getObjective(scoreboard.getObjectiveName()) != null;
    }

    private void registerObjective(ScoreboardType scoreboard) {
        switch (scoreboard) {
            case PRIMARY:
                if (!objectiveExists(scoreboard)) {
                    gameObjective = mainScoreboard.registerNewObjective(scoreboard.getObjectiveName(),
                            "dummy",
                            "primary"
                    );
                } else {
                    gameObjective = mainScoreboard.getObjective(scoreboard.getObjectiveName());
                }
                break;
            case LEADERBOARD:
                if (!objectiveExists(scoreboard)) {
                    playersScoreboardObjective = mainScoreboard.registerNewObjective(scoreboard.getObjectiveName(),
                            "dummy", "leaderboard"
                    );
                } else {
                    playersScoreboardObjective = mainScoreboard.getObjective(scoreboard.getObjectiveName());
                }
                break;
            case SHOOTING_RANGE:
                if (!objectiveExists(scoreboard)) {
                    shootingStatsObjective = mainScoreboard.registerNewObjective(scoreboard.getObjectiveName(),
                            "dummy",
                            "shooting_range"
                    );
                } else {
                    shootingStatsObjective = mainScoreboard.getObjective(scoreboard.getObjectiveName());
                }
                break;
            case PREVIEW:
                if (!objectiveExists(scoreboard)) {
                    previewObjective = mainScoreboard.registerNewObjective(scoreboard.getObjectiveName(),
                            "dummy",
                            "preview"
                    );
                } else {
                    previewObjective = mainScoreboard.getObjective(scoreboard.getObjectiveName());
                }
                break;
        }
    }

    @NotNull
    private Objective getObjective(ScoreboardType scoreboard) {
        if (!objectiveExists(scoreboard)) {registerObjective(scoreboard);}
        switch (scoreboard) {
            case PRIMARY:
                return gameObjective;
            case LEADERBOARD:
                return playersScoreboardObjective;
            case SHOOTING_RANGE:
                return shootingStatsObjective;
            case PREVIEW:
                return previewObjective;
        }
        return null;
    }

    private void setPreviewScoreboardLine(String text, String accessKey, int lineNumber) {

        if (mainScoreboard.getObjective("scoreboardPreview") == null) {
            previewObjective = mainScoreboard.registerNewObjective("scoreboardPreview", "dummy", "preview");
        } else {
            previewObjective = mainScoreboard.getObjective("scoreboardPreview");
        }

        // Safeguard: if a line is already occupied, meaning a team already exists, reregister it.
        if (mainScoreboard.getTeam(accessKey) != null) {
            mainScoreboard.getTeam(accessKey).unregister();
        }

        Team propertyToSet = previewObjective.getScoreboard().registerNewTeam(accessKey);
        propertyToSet.addEntry(lineFillerSymbols[lineNumber - 1]);
        propertyToSet.setPrefix(ChatColor.translateAlternateColorCodes('&', text));

        previewObjective.getScore(lineFillerSymbols[lineNumber - 1]).setScore(SCOREBOARD_FIRST_LINE - lineNumber);
    }

    private void updateScoreboardEntry(String entry, String newContent) {
        Team propertyToUpdate = mainScoreboard.getTeam(entry);
        propertyToUpdate.setPrefix(ChatColor.translateAlternateColorCodes('&', newContent));
    }

    public void previewScoreboard(String scoreboardConfigurationID) {

        if (Game.instance != null) {
            if (Game.instance.REFRESH_TASK != -1) {
                return;
            }
        }

        BiamineBiathlon gameInfo = new BiamineBiathlon(4, 10, 0, "12:34", scoreboardConfigurationID, "none", "_internaltest");

        String l1 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE1);
        String l2 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE2);
        String l3 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE3);
        String l4 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE4);
        String l5 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE5);
        String l6 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE6);
        String l7 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE7);
        String l8 = main.dataUtility.getScoreboardConfigProperty(scoreboardConfigurationID, ScoreboardLine.LINE8);

        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l1, gameInfo), "preview_line1", 1);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l2, gameInfo), "preview_line2", 2);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l3, gameInfo), "preview_line3", 3);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l4, gameInfo), "preview_line4", 4);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l5, gameInfo), "preview_line5", 5);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l6, gameInfo), "preview_line6", 6);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l7, gameInfo), "preview_line7", 7);
        setPreviewScoreboardLine(findReplacePreviewPlaceholders(l8, gameInfo), "preview_line8", 8);

        String title = main.dataUtility.getScoreboardConfigProperty(gameInfo.scoreboardConfig, ScoreboardLine.LINE0);
        title = title.replaceAll("%timer%", gameInfo.latestTime);
        title = title.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&a[state]"));
        title = title.replaceAll("%title%", ChatColor.translateAlternateColorCodes('&', "&aDisplay Name"));

        previewObjective.setDisplayName(ChatColor.translateAlternateColorCodes('&', title + " &f(&ePreview mode&f)"));
        previewObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void clearPreview() {
        previewObjective.setDisplaySlot(null);
        previewObjective.unregister();
    }

    public void refreshPrimaryScoreboardLine(BiamineBiathlon gameInfo, ScoreboardLine line) {
        if (currentScoreboardCycleBoard == ScoreboardType.PRIMARY) {
            if (!line.equals(ScoreboardLine.NO_SUCH_LINE)) {
                setScoreboardLine(ScoreboardType.PRIMARY,
                        line.asNumber(),
                        "line" + line.asNumber(),
                        findReplacePlaceholders(main.dataUtility.getScoreboardConfigProperty(gameInfo.scoreboardConfig,
                                        line
                                ), gameInfo
                        )
                );
            }
        }
    }

    public static String[] getSupportedPlaceholders() {
        return PLACEHOLDERS;
    }

    public void refreshScoreboardTitle(BiamineBiathlon info) {
        if (!objectiveExists(ScoreboardType.PRIMARY)) {registerObjective(ScoreboardType.PRIMARY);}

        String result = main.dataUtility.getScoreboardConfigProperty(info.scoreboardConfig, ScoreboardLine.LINE0);
        result = result.replaceAll("%timer%", info.latestTime);
        result = result.replaceAll("%state%",
                getFormattedStateString(InstanceStatus.valueOf(main.dataUtility.getGameProperty(info.gameID,
                        GameProperty.RUN_STATE
                )))
        );
        result = result.replaceAll("%title%", ChatColor.translateAlternateColorCodes('&', main.dataUtility.getGameProperty(info.gameID, GameProperty.DISPLAY_NAME)));

        gameObjective.setDisplayName(ChatColor.translateAlternateColorCodes('&', result));
    }

    private String findReplacePreviewPlaceholders(String input, BiamineBiathlon info) {

        // Replace built-in placeholders
        input = input.replaceAll("%dateTime%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.DATE_FORMAT_EXTENDED)).format(LocalDateTime.now()));
        input = input.replaceAll("%localTime%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.TIME_FORMAT)).format(LocalTime.now()));
        input = input.replaceAll("%date%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.DATE_FORMAT)).format(LocalDate.now()));
        input = input.replaceAll("%footer%", main.dataUtility.getConfigProperty(ConfigProperty.FOOTER_CONTENT));
        input = input.replaceAll("%header%", main.dataUtility.getConfigProperty(ConfigProperty.HEADER_CONTENT));
        input = input.replaceAll("%shootings%", String.valueOf(info.shootingsCount));
        input = input.replaceAll("%playersNotFinished%", (info.totalPlayers - info.finishedPlayers) + "/" + info.totalPlayers);
        input = input.replaceAll("%playersFinished%", String.valueOf(info.finishedPlayers));
        input = input.replaceAll("%playersParticipating%", String.valueOf(info.totalPlayers));
        input = input.replaceAll("%timer%", info.latestTime);
        input = input.replaceAll("%empty%", "");
        input = input.replaceAll("%bestTime%", "N/A");
        input = input.replaceAll("%state%", ChatColor.translateAlternateColorCodes('&', "&ePreview"));

        // Find and replace all user-defined placeholders.
        for (String word : input.split(" ")) {
            if (word.startsWith("_")) {
                if (main.dataUtility.globalPlaceholderExists(word)) {
                    input = input.replaceAll(word, main.dataUtility.getGlobalPlaceholderReplacement(word));
                }
            }
        }

        return input;
    }

    private String findReplacePlaceholders(String input, BiamineBiathlon info) {

        // Replace built-in placeholders
        input = input.replaceAll("%dateTime%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.DATE_FORMAT_EXTENDED)).format(LocalDateTime.now()));
        input = input.replaceAll("%localTime%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.TIME_FORMAT)).format(LocalTime.now()));
        input = input.replaceAll("%date%", DateTimeFormatter.ofPattern(main.dataUtility.getConfigProperty(ConfigProperty.DATE_FORMAT)).format(LocalDate.now()));
        input = input.replaceAll("%footer%", main.dataUtility.getConfigProperty(ConfigProperty.FOOTER_CONTENT));
        input = input.replaceAll("%header%", main.dataUtility.getConfigProperty(ConfigProperty.HEADER_CONTENT));
        input = input.replaceAll("%shootings%", String.valueOf(info.shootingsCount));
        input = input.replaceAll("%playersNotFinished%", (info.totalPlayers - info.finishedPlayers) + "/" + info.totalPlayers);
        input = input.replaceAll("%playersFinished%", String.valueOf(info.finishedPlayers));
        input = input.replaceAll("%playersParticipating%", String.valueOf(info.totalPlayers));
        input = input.replaceAll("%timer%", info.latestTime);
        input = input.replaceAll("%empty%", "");
        input = input.replaceAll("%bestTime%", Game.instance.getBestFinishTime() == null ? "N/A" : Game.instance.getBestFinishTime().getKey().getName() + " - " + Game.instance.getBestFinishTime().getValue().getFinishTime());

        // Find and replace all user-defined placeholders.
        for (String word : input.split(" ")) {
            if (word.startsWith("_")) {
                if (main.dataUtility.globalPlaceholderExists(word)) {
                    input = input.replaceAll(word, main.dataUtility.getGlobalPlaceholderReplacement(word));
                }
            }
        }

        // Handle state styling
        input = input.replaceAll("%state%",
                ChatColor.translateAlternateColorCodes('&',
                        getFormattedStateString(InstanceStatus.valueOf(main.dataUtility
                                .getGameProperty(info.gameID, GameProperty.RUN_STATE)
                                .toUpperCase()))
                )
        );
        return input;
    }

    private String getFormattedStateString(InstanceStatus status) {
        switch (status) {
            case STANDBY:
                return "&f" + main.localizationUtility.getLocalizedPhrase("internals.states.stdby");
            case PREP:
                return "&e" + main.localizationUtility.getLocalizedPhrase("internals.states.prep");
            case COUNTDOWN:
                return "&e" + main.localizationUtility.getLocalizedPhrase("internals.states.ctdwn");
            case RUNNING:
                return "&a" + main.localizationUtility.getLocalizedPhrase("internals.states.run");
            case FINALIZING:
                return "&d" + main.localizationUtility.getLocalizedPhrase("internals.states.final");
            case CLEANUP:
                return "&b" + main.localizationUtility.getLocalizedPhrase("internals.states.cln");
            case PAUSED:
                return "&9" + main.localizationUtility.getLocalizedPhrase("internals.states.pause");
            case TERMINATED:
                return "&c" + main.localizationUtility.getLocalizedPhrase("internals.states.term");
            case PREVTERM:
                return "&4" + main.localizationUtility.getLocalizedPhrase("internals.states.prevterm");
            default:
                return "&4N/A";
        }
    }


    public void showPrimaryScoreboard() {
        mainScoreboard.getObjective(ScoreboardType.PRIMARY.getObjectiveName()).setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void hidePrimaryScoreboard() {
        mainScoreboard.getObjective(ScoreboardType.PRIMARY.getObjectiveName()).setDisplaySlot(null);
    }

    public void clearSidebar() {
        mainScoreboard.clearSlot(DisplaySlot.SIDEBAR);
        gameObjective.unregister();
        playersScoreboardObjective.unregister();
        shootingStatsObjective.unregister();
    }

    public void reset() {
        clearSidebar();
        scheduler.cancelTask(SCOREBOARD_ADVANCE_TASK);
        scheduler.cancelTask(SCOREBOARD_REFRESH_TASK);
    }


}
