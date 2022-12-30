package krisapps.biaminereloaded.game_setup;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Game {

    public Player[] players;
    public Location startLocation_bound1;
    public Location startLocation_bound2;

    Random random = new Random();

    public Location getRandomLocation(Location loc1, Location loc2) {
        double minX = Math.min(loc1.getX(), loc2.getX());
        double minY = Math.min(loc1.getY(), loc2.getY());
        double minZ = Math.min(loc1.getZ(), loc2.getZ());

        double maxX = Math.max(loc1.getX(), loc2.getX());
        double maxY = Math.max(loc1.getY(), loc2.getY());
        double maxZ = Math.max(loc1.getZ(), loc2.getZ());

        return new Location(loc1.getWorld(), randomDouble(minX, maxX), randomDouble(minY, maxY), randomDouble(minZ, maxZ));
    }

    public double randomDouble(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble(Math.abs(max - min + 1));
    }


    public void teleportToStart() {
        for (Player p : players) {
            p.teleport(getRandomLocation(startLocation_bound1, startLocation_bound2));
        }
    }


    public void gameLoop() {

    }


}
