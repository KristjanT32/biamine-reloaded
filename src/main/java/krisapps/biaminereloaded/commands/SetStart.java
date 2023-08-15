package krisapps.biaminereloaded.commands;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.GenericErrorType;
import krisapps.biaminereloaded.utilities.BiaMineDataUtility;
import krisapps.biaminereloaded.utilities.MessageUtility;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetStart implements CommandExecutor {

    BiamineReloaded main;
    MessageUtility messages;
    BiaMineDataUtility data;

    public SetStart(BiamineReloaded main) {
        this.main = main;
        this.messages = new MessageUtility(main);
        this.data = new BiaMineDataUtility(main);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /setstart <gameID> <bound1/bound2>
        if (args.length >= 2) {
            if (data.gameExists(args[0])) {
                switch (args[1]) {
                    case "bound1":
                        data.setStartLocation(args[0], 1, (Player) sender);
                        messages.sendMessage(sender, "&b[&lSetup&r&b]&e: &aSuccessfully set the &lfirst&r&a corner of the starting area of &b" + args[0] + " &a.");
                        break;
                    case "bound2":
                        data.setStartLocation(args[0], 2, (Player) sender);
                        messages.sendMessage(sender, "&b[&lSetup&r&b]&e: &aSuccessfully set the &lsecond&r&a corner of the starting area of &b" + args[0] + " &a.");
                        break;
                    default:
                        messages.sendError(sender, GenericErrorType.INVALID_SYNTAX);
                        break;
                }
                if (data.hasSetupStartLocation(args[0])) {
                    Location location1 = main.dataUtility.getStartLocationFirstBound(args[0]);
                    Location location2 = main.dataUtility.getStartLocationSecondBound(args[0]);
                    highlightAreaWithParticles(location1, location2, (Player) sender);
                }
            } else {
                messages.sendError(sender, GenericErrorType.INVALID_GAME);
            }
        } else {
            messages.sendError(sender, GenericErrorType.INSUFFICIENT_PARAMETERS);
        }
        return true;
    }

    private void highlightAreaWithParticles(Location lowerBound, Location upperBound, Player p) {
        World world = lowerBound.getWorld();
        for (double x = lowerBound.getX(); x <= upperBound.getX(); x++) {
            for (double y = lowerBound.getY(); y <= upperBound.getY(); y++) {
                for (double z = lowerBound.getZ(); z <= upperBound.getZ(); z++) {
                    Location particleLocation = new Location(world, x, y, z);
                    p.spawnParticle(Particle.BLOCK_MARKER, particleLocation, 1);
                }
            }
        }
    }
}
