package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.game_setup.BiamineBiathlon;
import krisapps.biaminereloaded.game_setup.Game;
import krisapps.biaminereloaded.types.GameProperty;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;

public class StartGame implements CommandExecutor {

    BiamineReloaded main;

    public StartGame(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /startgame <gameID> <shootings> <players[]>

        if (args.length >= 3) {
            String gameID = args[0];
            int shootings = Integer.parseInt(args[1]);

            ArrayList<String> players = new ArrayList<>();
            players.addAll(Arrays.asList(args).subList(2, args.length));

            BiamineBiathlon instance = new BiamineBiathlon(shootings, players.size(), 0, "00:00",
                    main.dataUtility.getGameProperty(gameID, GameProperty.SCOREBOARD_CONFIGURATION_ID),
                    main.dataUtility.getGameProperty(gameID, GameProperty.EXCLUSION_LIST_ID));

            Game game = new Game(gameID, instance, main);

            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.startgame.starting-notice"));
            game.startGame();

        } else {

        }


        return true;
    }
}
