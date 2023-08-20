package krisapps.biaminereloaded.types;

import org.bukkit.entity.Player;

public class BestTimeEntry {

    private Player player;
    private String time;

    public BestTimeEntry(Player player, String time) {
        this.player = player;
        this.time = time;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
