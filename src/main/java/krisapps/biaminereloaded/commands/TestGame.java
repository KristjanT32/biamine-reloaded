package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.game_setup.BiamineBiathlon;
import krisapps.biaminereloaded.game_setup.Game;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TestGame implements CommandExecutor {

    BiamineReloaded main;

    public TestGame(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Runnable startTask = new Runnable() {
            @Override
            public void run() {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aStarting the internal test game. &eCommencing in &b5 &eseconds."));
                Game testGame = new Game(
                        "test_game",
                        new BiamineBiathlon(3, 10, 0, "00:00:00", "test_scoreboard", "test_exclusions"),
                        main
                );
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                testGame.startGame();
            }
        };
        startTask.run();

        return true;
    }
}
