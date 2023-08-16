package krisapps.biaminereloaded.events;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.CollidableRegion;
import krisapps.biaminereloaded.types.CuboidRegion;
import krisapps.biaminereloaded.types.InstanceStatus;
import krisapps.biaminereloaded.utilities.RegionCollisionUtility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {

    BiamineReloaded main;
    RegionCollisionUtility collisionUtility;
    Map<UUID, Long> waitMap = new HashMap<>();

    Map<UUID, String> playerTracker = new HashMap<>();


    public PlayerMoveListener(BiamineReloaded main) {
        this.main = main;
        this.collisionUtility = new RegionCollisionUtility(main);
    }

    private void addPlayerToSleepMap(UUID player) {
        waitMap.put(player, System.currentTimeMillis());
    }

    private void removePlayerFromSleepMap(UUID player) {
        waitMap.remove(player);
    }


    // Checkpoint collision
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent moveEvent) {
        Player p = moveEvent.getPlayer();
        String activeGame = main.dataUtility.getActiveGame();
        if (activeGame == null) {
            return;
        }

        // Freeze player if in the active game, and the game is paused.
        if (main.gameUtility.getStatus(activeGame).equals(InstanceStatus.PAUSED)) {
            if (main.gameUtility.hasPlayerInGame(moveEvent.getPlayer())) {
                moveEvent.setCancelled(true);
                return;
            }
        }

        for (String checkpoint : main.dataUtility.getCheckpoints(activeGame)) {
            CollidableRegion checkpointRegion;
            try {
                checkpointRegion = main.dataUtility.getCheckpoint(activeGame, checkpoint);
            } catch (NullPointerException e) {
                continue;
            }
            if (collisionUtility.checkIntersect(moveEvent.getTo(), checkpointRegion)) {
                if (waitMap.containsKey(p.getUniqueId())) {
                    if ((System.currentTimeMillis() - waitMap.get(p.getUniqueId())) >= 3000L) {
                        removePlayerFromSleepMap(p.getUniqueId());
                    }
                } else {
                    addPlayerToSleepMap(p.getUniqueId());
                    Bukkit.getPluginManager().callEvent(new CheckpointPassEvent(moveEvent.getPlayer(), checkpointRegion));
                }
            }
        }
    }

    // Shooting spot collision
    @EventHandler
    public void onPlayerMove2(PlayerMoveEvent moveEvent) {
        Player p = moveEvent.getPlayer();
        String activeGame = main.dataUtility.getActiveGame();
        if (activeGame == null) {
            return;
        }

        for (String shootingSpot : main.dataUtility.getShootingSpots(activeGame)) {
            CuboidRegion shootingSpotRegion;
            try {
                shootingSpotRegion = main.dataUtility.getShootingSpotRegion(activeGame, shootingSpot);
            } catch (NullPointerException e) {
                continue;
            }
            if (collisionUtility.checkIntersect(moveEvent.getTo(), shootingSpotRegion)) {
                if (!collisionUtility.checkIntersect(moveEvent.getFrom(), shootingSpotRegion)) {
                    playerTracker.put(p.getUniqueId(), shootingSpot);
                    Bukkit.getPluginManager().callEvent(new BiathlonShootingSpotEnterEvent(Integer.parseInt(shootingSpot.replaceAll("shootingSpot", "")), activeGame, p));
                    return;
                }
            } else {

                if (collisionUtility.checkIntersect(moveEvent.getFrom(), shootingSpotRegion)) {
                    // Remove the player, since they're not in a spot anymore.
                    Bukkit.getPluginManager().callEvent(new BiathlonShootingSpotExitEvent(Integer.parseInt(playerTracker.get(p.getUniqueId()).replaceAll("shootingSpot", "")),
                            activeGame,
                            p
                    ));
                    playerTracker.remove(p.getUniqueId());
                    return;
                }
            }
        }
    }

}
