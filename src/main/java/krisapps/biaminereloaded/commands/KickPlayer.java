package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickPlayer implements CommandExecutor {

    BiamineReloaded main;

    public KickPlayer(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /kickplayer <player> [reason]

        if (args.length >= 1) {
            String playerName = args[0];
            Player p = Bukkit.getPlayer(playerName);
            StringBuilder reason = new StringBuilder();
            if (p != null) {
                for (int i = 1; i < args.length; i++) {
                    reason.append(args[i]).append(" ");
                }
                int result = main.gameUtility.kickPlayer(p, reason.toString().trim());
                if (result == 200) {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.kickplayer.success")
                            .replaceAll("%player%", playerName)
                    );
                } else if (result == 404) {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.kickplayer.notingame"));
                } else if (result == 302) {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.kickplayer.finished"));
                } else if (result == 500) {
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.kickplayer.nogames"));
                }
            } else {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.kickplayer.notfound"));
            }
        } else {
            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.kickplayer.insuff"));
        }


        return true;
    }
}
