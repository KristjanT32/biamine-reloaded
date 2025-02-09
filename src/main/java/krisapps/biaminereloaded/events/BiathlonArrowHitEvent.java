package krisapps.biaminereloaded.events;

import krisapps.biaminereloaded.gameloop.types.HitType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BiathlonArrowHitEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final HitType hitType;
    private final Player shooter;
    private final int target;
    private final int spot;
    private final String game;

    public BiathlonArrowHitEvent(HitType hitType, Player shooter, int target, int spot, String game) {
        this.hitType = hitType;
        this.shooter = shooter;
        this.target = target;
        this.spot = spot;
        this.game = game;
    }

    public BiathlonArrowHitEvent(HitType hitType, Player shooter, int spot, String game) {
        this.hitType = hitType;
        this.shooter = shooter;
        this.spot = spot;
        this.game = game;
        this.target = -1;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HitType getHitType() {
        return hitType;
    }

    public Player getShooter() {
        return shooter;
    }

    public int getTarget() {
        return target;
    }

    public int getSpot() {
        return spot;
    }

    public String getGame() {
        return game;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
