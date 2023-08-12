package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Rejoin implements CommandExecutor {

    BiamineReloaded main;

    public Rejoin(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((sender instanceof Player)) {
            if (main.gameUtility.rejoinPlayer(((Player) sender).getPlayer())) {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.rejoin.success")
                        .replaceAll("%game%", main.gameUtility.getActiveGameID())
                );
            } else {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.rejoin.fail"));
            }
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.rejoin.playeronly"));
        }
        return true;
    }
}
