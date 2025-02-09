package krisapps.biaminereloaded.events;

import krisapps.biaminereloaded.gameloop.types.InstanceStatus;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class InstanceStatusChangeEvent extends Event {
    static HandlerList handlers = new HandlerList();
    private final String instanceID;

    private final InstanceStatus oldStatus;
    private final InstanceStatus newStatus;

    public InstanceStatusChangeEvent(String instanceID, InstanceStatus oldStatus, InstanceStatus newStatus) {
        this.instanceID = instanceID;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public InstanceStatus getOldStatus() {
        return oldStatus;
    }

    public InstanceStatus getNewStatus() {
        return newStatus;
    }
}
