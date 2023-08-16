package krisapps.biaminereloaded.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BiathlonShootingSpotExitEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final int spotID;
    private final String gameID;
    private final Player player;

    public BiathlonShootingSpotExitEvent(int spotID, String gameID, Player player) {
        this.spotID = spotID;
        this.gameID = gameID;
        this.player = player;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public int getSpotID() {
        return spotID;
    }

    public String getGameID() {
        return gameID;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
