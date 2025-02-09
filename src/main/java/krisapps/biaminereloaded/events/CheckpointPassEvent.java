package krisapps.biaminereloaded.events;

import krisapps.biaminereloaded.types.area.CollidableRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CheckpointPassEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final CollidableRegion region;

    public CheckpointPassEvent(Player p, CollidableRegion r) {
        this.player = p;
        this.region = r;
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

    public CollidableRegion getRegion() {
        return region;
    }
}
