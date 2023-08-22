package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtility {

    BiamineReloaded main;
    private boolean enabled = true;

    public SoundUtility(BiamineReloaded main) {
        this.main = main;
    }

    public void playSound(Player p, Sound sound) {
        if (!enabled) {
            return;
        }
        p.playSound(p, sound, 100, 1);
    }

    public void playTickSound(Player p) {
        if (!enabled) {
            return;
        }
        p.playSound(p, Sound.BLOCK_DISPENSER_FAIL, 100, 10);
    }

    public void playHitSound(Player p) {
        if (!enabled) {
            return;
        }
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 10);
    }

    public void playMissSound(Player p) {
        if (!enabled) {
            return;
        }
        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 100, -5);
    }

    public void playStartSound(Player p) {
        if (!enabled) {
            return;
        }
        p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_0, 100, 5);
    }

    public void playFinishSound(Player p) {
        if (!enabled) {
            return;
        }
        p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_1, 100, 1);
    }


    public void setEnabled(boolean state) {
        this.enabled = state;
    }

}
