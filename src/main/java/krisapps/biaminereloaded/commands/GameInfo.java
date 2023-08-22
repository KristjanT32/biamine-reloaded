package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.GameProperty;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GameInfo implements CommandExecutor {

    BiamineReloaded main;

    public GameInfo(BiamineReloaded main) {
        this.main = main;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /biainfo <gameID>

        if (main.dataUtility.gameExists(args[0])) {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.gameinfo.info")
                    .replaceAll("%game%", args[0])
                    .replaceAll("%disname%", main.dataUtility.getGameProperty(args[0], GameProperty.DISPLAY_NAME))
                    .replaceAll("%prep%", main.dataUtility.getGameProperty(args[0], GameProperty.PREPARATION_TIME))
                    .replaceAll("%final%", main.dataUtility.getGameProperty(args[0], GameProperty.COUNTDOWN_TIME))
                    .replaceAll("%sconf%", main.dataUtility.getGameProperty(args[0], GameProperty.SCOREBOARD_CONFIGURATION_ID))
                    .replaceAll("%exlist%", main.dataUtility.getGameProperty(args[0], GameProperty.EXCLUSION_LIST_ID))
                    .replaceAll("%dispenser%", main.dataUtility.getDispenserEntries(args[0]).isEmpty() ? "&c&lNOT IN USE" : "&a&lIN USE")
                    .replaceAll("%runstate%", main.dataUtility.getGameProperty(args[0], GameProperty.RUN_STATE))
                    .replaceAll("%chkpnt%", String.valueOf(main.dataUtility.getCheckpoints(args[0]).size()))
                    .replaceAll("%shtrng%", main.dataUtility.getShootingSpots(args[0]).isEmpty() ? "&c&lNOT IN USE" : "&a&lIN USE")
            );
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.gameinfo.notfound"));
        }


        return true;
    }
}
