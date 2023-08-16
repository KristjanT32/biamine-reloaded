package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.gameloop.BiamineBiathlon;
import krisapps.biaminereloaded.types.ConfigProperty;
import krisapps.biaminereloaded.types.FinishInfo;
import krisapps.biaminereloaded.types.HitInfo;
import krisapps.biaminereloaded.types.HitType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class ReportUtility {

    BiamineReloaded main;

    public ReportUtility(BiamineReloaded main) {
        this.main = main;
    }

    public void generateGameReport(Map<UUID, List<HitInfo>> shootingStats, HashMap<Player, FinishInfo> finishInfo, BiamineBiathlon gameInfo) {
        File file;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH-mm-ss");
        SimpleDateFormat timeFormat2 = new SimpleDateFormat("HH:mm:ss");
        if (main.dataUtility.getConfigProperty(ConfigProperty.GAME_REPORT_PATH).equals("%dataFolder%")) {
            try {
                file = new File(main.getDataFolder(), "report-biathlon-" + dateFormat.format(Date.from(Instant.now())) + "-" + timeFormat.format(Date.from(Instant.now())) + ".txt");
                FileWriter fWriter = new FileWriter(file);
                BufferedWriter writer = new BufferedWriter(fWriter);

                writer.append("===============================================================================");
                writer.newLine();
                writer.append(String.format("GAME REPORT FILE for %s", gameInfo.gameID));
                writer.newLine();
                writer.append(String.format("DATE: %s", dateFormat.format(Date.from(Instant.now()))));
                writer.newLine();
                writer.append(String.format("REPORT GENERATED AT: %s", timeFormat2.format(Date.from(Instant.now()))));
                writer.newLine();
                writer.append("===============================================================================");
                writer.newLine();
                writer.append("===============================================================================");
                writer.newLine();
                writer.append("Game Duration: " + gameInfo.latestTime);
                writer.newLine();
                writer.append("Total Shootings: " + gameInfo.shootingsCount);
                writer.newLine();
                writer.append("===============================================================================");
                writer.newLine();
                writer.append("=[ FINAL SCOREBOARD ]==========================================================");
                writer.newLine();
                for (Map.Entry<Player, FinishInfo> entry : finishInfo.entrySet()) {
                    writer.append("* [" + entry.getValue().getLeaderboardOrder() + ".] " + entry.getKey().getName());
                    writer.append(" - finished at " + entry.getValue().getFinishTime());
                    writer.newLine();
                }
                writer.append("===============================================================================");
                writer.newLine();
                writer.append("=[ SHOOTING STATS ]============================================================");
                writer.newLine();
                for (Map.Entry<UUID, List<HitInfo>> entry : shootingStats.entrySet()) {
                    writer.append(" Player " + (Bukkit.getPlayer(entry.getKey()) == null ? entry.getKey().toString() : Bukkit.getPlayer(entry.getKey()).getName()));
                    writer.newLine();
                    int shotOrder = 1;
                    for (HitInfo shot : entry.getValue()) {
                        if (shot.getType().equals(HitType.HIT)) {
                            writer.append("     #" + shotOrder + ": HIT - Target no." + shot.getTarget() + " on spot no." + shot.getSpot() + " - arrows remaining: " + shot.getArrowsRemaining());
                            writer.newLine();
                        } else {
                            writer.append("     #" + shotOrder + ": MISS on spot no." + shot.getSpot() + " - arrows remaining: " + shot.getArrowsRemaining());
                            writer.newLine();
                        }
                        shotOrder++;
                    }
                }
                writer.append("===============================================================================");
                writer.newLine();
                writer.append("## END REPORT ##");
                writer.close();
            } catch (IOException e) {
                main.getLogger().info("Failed to generate the game report: " + e.getMessage());
                main.appendToLog("Failed to generate the game report: " + e.getMessage());
            }
        }

    }


}
