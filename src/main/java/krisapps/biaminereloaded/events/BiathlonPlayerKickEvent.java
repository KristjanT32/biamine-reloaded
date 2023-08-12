package krisapps.biaminereloaded.events;

import krisapps.biaminereloaded.types.KickType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BiathlonPlayerKickEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final KickType type;
    private final String gameID;

    public BiathlonPlayerKickEvent(KickType type, Player p, String gameID) {
        this.gameID = gameID;
        this.type = type;
        this.player = p;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public KickType getKickType() {
        return type;
    }

    public String getGameID() {
        return gameID;
    }
}
