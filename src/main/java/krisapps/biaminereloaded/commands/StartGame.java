package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.gameloop.BiamineBiathlon;
import krisapps.biaminereloaded.gameloop.Game;
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

        if (args.length >= 2) {
            String gameID = args[0];
            int shootings = Integer.parseInt(args[1]);

            ArrayList<String> players = new ArrayList<>();
            if (args.length >= 3) {
                players.addAll(Arrays.asList(args).subList(2, args.length));
            }

            if (!players.isEmpty()) {
                BiamineBiathlon instance = new BiamineBiathlon(shootings, players.size(), 0, "00:00:00",
                        main.dataUtility.getGameProperty(gameID, GameProperty.SCOREBOARD_CONFIGURATION_ID),
                        main.dataUtility.getGameProperty(gameID, GameProperty.EXCLUSION_LIST_ID), gameID);

                Game game = new Game(gameID, instance, main);

                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.startgame.starting-notice"));
                game.startGame(players, sender);
            } else {
                BiamineBiathlon instance = new BiamineBiathlon(shootings, players.size(), 0, "00:00:00",
                        main.dataUtility.getGameProperty(gameID, GameProperty.SCOREBOARD_CONFIGURATION_ID),
                        main.dataUtility.getGameProperty(gameID, GameProperty.EXCLUSION_LIST_ID), gameID);

                Game game = new Game(gameID, instance, main);

                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.startgame.starting-notice"));
                game.startGame(sender);
            }
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.startgame.insuff"));
        }
        return true;
    }
}
