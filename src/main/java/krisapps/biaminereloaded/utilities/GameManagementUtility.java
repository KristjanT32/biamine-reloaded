package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.gameloop.BiamineBiathlon;
import krisapps.biaminereloaded.gameloop.Game;
import krisapps.biaminereloaded.types.GameProperty;
import krisapps.biaminereloaded.types.InstanceStatus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GameManagementUtility {

    // Current Game Information
    BiamineBiathlon curGameInfo;
    Game curGameObject;


    BiamineReloaded main;

    public GameManagementUtility(BiamineReloaded main) {
        this.main = main;
    }

    public InstanceStatus getStatus(String gameID) {
        if (main.dataUtility.gameExists(gameID)) {
            return InstanceStatus.valueOf(main.dataUtility.getGameProperty(gameID, GameProperty.RUN_STATE));
        } else {
            return InstanceStatus.DOES_NOT_EXIST;
        }
    }

    public void initGame(BiamineBiathlon info, CommandSender initiator) {
        curGameObject = new Game(info.gameID, info, main);
        curGameInfo = info;
        curGameObject.startGame(initiator);
    }

    public void initGameWithPlayers(BiamineBiathlon info, CommandSender initiator, List<String> players) {
        curGameObject = new Game(info.gameID, info, main);
        curGameInfo = info;
        curGameObject.startGame(players, initiator);
    }

    public void terminateGame(String gameID) {
        if (curGameObject == null || curGameInfo == null) {
            return;
        }
        if (curGameInfo.gameID.equalsIgnoreCase(gameID)) {
            curGameObject.stopGame();
            main.dataUtility.setActiveGame(null);
            curGameObject = null;
            curGameInfo = null;
        }
    }

    public void pauseGame(String gameID) {
        if (curGameObject != null && curGameInfo != null) {
            if (!curGameObject.isPaused && curGameInfo.gameID.equalsIgnoreCase(gameID)) {
                curGameObject.pauseGame();
            }
        }
    }

    public void resumeGame(String gameID) {
        if (curGameObject != null && curGameInfo != null) {
            if (curGameObject.isPaused && curGameInfo.gameID.equalsIgnoreCase(gameID)) {
                curGameObject.resumeGame();
            }
        }
    }

    public boolean hasPlayerInGame(Player p) {
        if (curGameObject != null && curGameInfo != null) {
            return curGameObject.players.contains(p) && !curGameObject.finishedPlayers.contains(p);
        } else {
            return false;
        }

    }

}
