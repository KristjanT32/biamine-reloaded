package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.gameloop.BiamineBiathlon;
import krisapps.biaminereloaded.gameloop.types.AreaPassInfo;
import krisapps.biaminereloaded.gameloop.types.FinishInfo;
import krisapps.biaminereloaded.gameloop.types.HitInfo;
import krisapps.biaminereloaded.gameloop.types.HitType;
import krisapps.biaminereloaded.timers.TimerFormatter;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ReportUtility {

    BiamineReloaded main;

    public ReportUtility(BiamineReloaded main) {
        this.main = main;
    }

    public void generateGameReport(Map<UUID, List<HitInfo>> shootingStats, HashMap<Player, FinishInfo> finishInfo, BiamineBiathlon gameInfo, Map<UUID, List<AreaPassInfo>> arrivals, CommandSender initiator) {
        File file;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH-mm-ss");
        SimpleDateFormat timeFormat2 = new SimpleDateFormat("HH:mm:ss");

        main.appendToLog("Generating a game report file: " + main.dataUtility.getConfigPropertyRaw("options.game-report.path"));
        if (main.dataUtility.getConfigPropertyRaw("options.game-report.path").equals("%dataFolder%")) {
            file = new File(main.getDataFolder(), "report-biathlon-" + dateFormat.format(Date.from(Instant.now())) + "-" + timeFormat.format(Date.from(Instant.now())) + ".txt");
        } else {
            file = new File(main.dataUtility.getConfigPropertyRaw("options.game-report.path"), "report-biathlon-" + dateFormat.format(Date.from(Instant.now())) + "-" + timeFormat.format(Date.from(Instant.now())) + ".txt");
        }

        try {

            FileWriter fWriter = new FileWriter(file);
            BufferedWriter writer = new BufferedWriter(fWriter);

            writer.append("===============================================================================");
            writer.newLine();
            writer.append(String.format("Biathlon Game Report for %s", gameInfo.gameID));
            writer.newLine();
            writer.append(String.format("Date: %s", dateFormat.format(Date.from(Instant.now()))));
            writer.newLine();
            writer.append(String.format("Report generated at: %s", timeFormat2.format(Date.from(Instant.now()))));
            writer.newLine();
            writer.append("===============================================================================");
            writer.newLine();
            writer.newLine();
            writer.append("===============================================================================");
            writer.newLine();
            writer.append("Game Duration: " + gameInfo.latestTime);
            writer.newLine();
            writer.append("Total Shootings: " + gameInfo.shootingsCount);
            writer.newLine();
            writer.append("===============================================================================");
            writer.newLine();
            writer.newLine();
            writer.append("=[ FINAL SCOREBOARD ]==========================================================");
            writer.newLine();
            writer.newLine();
            for (Map.Entry<Player, FinishInfo> entry : finishInfo.entrySet()) {
                writer.append("* [" + entry.getValue().getLeaderboardOrder() + ".] " + entry.getKey().getName());
                writer.append(" - finished at " + entry.getValue().getFinishTime());
                writer.newLine();
            }
            writer.newLine();
            writer.append("===============================================================================");
            writer.newLine();
            writer.newLine();
            writer.append("=[ SHOOTING STATS ]============================================================");
            writer.newLine();
            writer.newLine();
            for (Map.Entry<UUID, List<HitInfo>> entry : shootingStats.entrySet()) {
                writer.append(" Player " + (Bukkit.getPlayer(entry.getKey()) == null ? entry.getKey().toString() : Bukkit.getPlayer(entry.getKey()).getName()));
                writer.newLine();
                int shotOrder = 1;
                int arrowsOnStart = entry.getValue().get(0).getArrowsRemaining();
                int arrowsOnEnd = entry.getValue().get(entry.getValue().size() - 1).getArrowsRemaining();
                int lastLap = 0;
                boolean lapLabelAppended = false;

                for (HitInfo shot : entry.getValue()) {
                    if (lastLap != shot.getLap()) {
                        lapLabelAppended = false;
                        lastLap++;
                    }
                    if (!lapLabelAppended) {
                        lapLabelAppended = true;
                        writer.newLine();
                        writer.append("     --- Lap " + lastLap + " ---");
                        writer.newLine();
                    }

                    if (shot.getType().equals(HitType.HIT)) {
                        writer.append("     #" + shotOrder + ": HIT - Target no. " + shot.getTarget() + " on spot no. " + shot.getSpot() + " - arrows remaining: " + shot.getArrowsRemaining());
                        writer.newLine();
                    } else {
                        writer.append("     #" + shotOrder + ": MISS on spot no. " + shot.getSpot() + " - arrows remaining: " + shot.getArrowsRemaining());
                        writer.newLine();
                    }
                    shotOrder++;
                }
                // If the arrow count was constant, something's up
                if (arrowsOnStart == arrowsOnEnd) {
                    writer.append("     [SUS!] Arrow count constant throughout shooting");
                    writer.newLine();
                }
                if (arrowsOnEnd > 0) {
                    writer.append("     [SUS!] Player had excess arrows (+" + arrowsOnEnd + " arrows)");
                    writer.newLine();
                }
                writer.newLine();
                writer.newLine();
            }
            writer.append("===============================================================================");
            writer.newLine();
            writer.newLine();
            writer.append("=[ TIMINGS INFO ]==============================================================");
            for (UUID playerUUID : arrivals.keySet()) {
                Player p = Bukkit.getPlayer(playerUUID);
                String playerName;
                if (p == null) {
                    playerName = playerUUID.toString();
                } else {
                    playerName = p.getName();
                }
                writer.newLine();
                writer.newLine();
                writer.append("# Timings for " + playerName);
                writer.newLine();

                int lastLapLabel = 0;
                List<String> appendedAreas = new ArrayList<>();

                for (AreaPassInfo info : arrivals.get(playerUUID)) {
                    if (lastLapLabel != info.getReachedOnLap()) {
                        lastLapLabel = info.getReachedOnLap();
                        writer.newLine();
                        writer.append("--- Lap " + lastLapLabel + " ---");
                        writer.newLine();

                        // For each new lap, clear the appended segments list.
                        appendedAreas.clear();
                    }

                    switch (info.getAreaType()) {
                        case CHECKPOINT:
                            writer.append("[-/-] Reached checkpoint '" + info.getAreaName() + "' at " + info.getTimerTime());
                            writer.newLine();
                            break;
                        case SHOOTING_SPOT:
                            // Skip the shooting spot entries that have already been appended to the report for the current lap
                            if (appendedAreas.contains(info.getAreaName())) {continue;}

                            AreaPassInfo arrivalEntry = null;
                            AreaPassInfo departureEntry = null;
                            int currentLap = lastLapLabel;

                            if (info.leftArea()) {
                                // If the current entry is the departure entry

                                // Find the arrival entry
                                arrivalEntry = arrivals
                                        .get(playerUUID)
                                        .stream()
                                        .filter(el -> el
                                                .getAreaName()
                                                .equals(info.getAreaName()) && !el.leftArea() && el.getReachedOnLap() == currentLap)
                                        .collect(Collectors.toList())
                                        .get(0);

                                departureEntry = info;
                            } else {
                                // If the current entry is the arrival entry

                                arrivalEntry = info;

                                // Find the departure entry
                                departureEntry = arrivals
                                        .get(playerUUID)
                                        .stream()
                                        .filter(el -> el
                                                .getAreaName()
                                                .equals(info.getAreaName()) && el.leftArea() && el.getReachedOnLap() == currentLap)
                                        .collect(Collectors.toList())
                                        .get(0);
                            }

                            writer.newLine();
                            writer.append("[-->] Claimed " + arrivalEntry.getAreaName() + " at " + arrivalEntry.getTimerTime());
                            writer.newLine();
                            writer.append("[<--] Left " + departureEntry.getAreaName() + " at " + departureEntry.getTimerTime());
                            writer.newLine();
                            writer.append("[<->] Total time spent: " + TimerFormatter.formatDifference(departureEntry.getTimerTime(),
                                    arrivalEntry.getTimerTime()
                            ));
                            writer.newLine();
                            writer.newLine();

                            appendedAreas.add(arrivalEntry.getAreaName());
                            appendedAreas.add(departureEntry.getAreaName());
                            break;
                        case FINISH_LINE:
                            writer.append("[!!!] Reached the finish line at: " + info.getTimerTime());
                            break;
                    }



                }
                writer.newLine();
                writer.newLine();
            }
            writer.newLine();
            writer.append("===============================================================================");
            writer.newLine();
            writer.append("## END REPORT ##");
            writer.close();

            initiator.spigot().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("gameloop.report-ready"))), main.messageUtility.createFileButton("clicktext.view-report", file.toPath().toString().replaceAll("\\\\", "/"), "hovertext.report-file"));
        } catch (IOException e) {
            main.getLogger().info("Failed to generate the game report: " + e.getMessage());
            main.appendToLog("Failed to generate the game report: " + e.getMessage());
        }

    }


}
