package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.gameloop.BiamineBiathlon;
import krisapps.biaminereloaded.gameloop.Game;
import krisapps.biaminereloaded.types.CoreDataField;
import krisapps.biaminereloaded.types.GameProperty;
import krisapps.biaminereloaded.types.InstanceStatus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GameManagementUtility {
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
        Game g = new Game(info.gameID, info, main);
        g.startGame(initiator);
    }

    public void initGameWithPlayers(BiamineBiathlon info, CommandSender initiator, List<String> players) {
        Game g = new Game(info.gameID, info, main);
        Game game = Game.instance;



        g.startGame(players, initiator);
    }

    public int terminateGame() {
        if (Game.instance == null) {
            return 500;
        }
        Game.instance.stopGame();
        main.dataUtility.setActiveGame(null);
        return 200;
    }

    public void reloadTerminate() {
        if (Game.instance == null) {
            return;
        }
        Game.instance.reloadTerminate();
    }

    public int pauseGame() {
        if (Game.instance == null) {
            return 500;
        }
        if (!Game.instance.isPaused) {
            Game.instance.pauseGame();
            return 200;
        } else {
            return 404;
        }
    }

    public int resumeGame() {
        if (Game.instance == null) {
            return 500;
        }
        if (Game.instance.isPaused) {
            Game.instance.resumeGame();
            return 200;
        } else {
            return 404;
        }
    }

    public boolean hasPlayerInGame(Player p) {
        if (Game.instance == null) {
            return false;
        }
        return Game.instance.players.contains(p) && !Game.instance.finishedPlayers.containsKey(p);
    }

    public int kickPlayer(Player p, String reason) {
        if (Game.instance == null) {
            return 500;
        }
        return Game.instance.kickPlayer(p, reason);
    }

    public boolean rejoinPlayer(Player p) {
        if (Game.instance == null) {
            return false;
        }
        return Game.instance.rejoinPlayer(p);
    }

    public String getActiveGameID() {
        try {
            if (Game.instance == null) {
                return "unknown";
            } else {
                return Game.instance.getCurrentGameInfo().gameID;
            }
        } catch (NoClassDefFoundError | NullPointerException e) {
            try {
                return main.dataUtility.getCoreData(CoreDataField.LAST_ACTIVE_GAME).toString();
            } catch (NoClassDefFoundError | NullPointerException err) {
                return null;
            }
        }
    }

    public void resetScoreboard() {
        if (Game.instance == null) {
            return;
        }
        Game.instance.getScoreboardManager().resetScoreboard();
    }

    public void refreshScoreboard() {
        if (Game.instance == null) {
            return;
        }
        Game.instance.getScoreboardManager().refreshScoreboardData(Game.instance.getCurrentGameInfo().scoreboardConfig, Game.instance.getCurrentGameInfo());
    }
}
