package krisapps.biaminereloaded.events;

import com.jeff_media.customblockdata.CustomBlockData;
import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.gameloop.Game;
import krisapps.biaminereloaded.types.HitType;
import krisapps.biaminereloaded.types.TargetProperty;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class ArrowHitListener implements Listener {

    BiamineReloaded main;

    public ArrowHitListener(BiamineReloaded main) {
        this.main = main;
    }


    @EventHandler
    public void onArrowHit(ProjectileHitEvent hitEvent) {
        if (main.gameUtility.getActiveGameID() == null) {
            return;
        }
        if (Game.instance == null) {
            return;
        }
        if (hitEvent.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) hitEvent.getEntity();
            if (arrow.getShooter() instanceof Player) {
                Player p = (Player) arrow.getShooter();
                if (hitEvent.getHitBlock() != null) {
                    Block b = hitEvent.getHitBlock();
                    if (CustomBlockData.hasCustomBlockData(b, main)) {
                        Bukkit.getPluginManager().callEvent(
                                new BiathlonArrowHitEvent(
                                        HitType.HIT,
                                        p,
                                        (Integer) main.dataUtility.getTargetData(b, TargetProperty.ORDER),
                                        (Integer) main.dataUtility.getTargetData(b, TargetProperty.SPOT),
                                        main.dataUtility.getTargetData(b, TargetProperty.GAME).toString()
                                ));
                    } else {
                        Bukkit.getPluginManager().callEvent(
                                new BiathlonArrowHitEvent(
                                        HitType.MISS,
                                        p,
                                        -1,
                                        Game.instance.getPlayerSpotID(p.getUniqueId()),
                                        main.gameUtility.getActiveGameID()
                                ));
                    }
                } else {
                    Bukkit.getPluginManager().callEvent(
                            new BiathlonArrowHitEvent(
                                    HitType.MISS,
                                    p,
                                    -1,
                                    Game.instance.getPlayerSpotID(p.getUniqueId()),
                                    main.gameUtility.getActiveGameID()
                            ));
                }
            }
        }
    }
}
