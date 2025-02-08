package krisapps.biaminereloaded.commands;

import com.jeff_media.customblockdata.CustomBlockData;
import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.CuboidRegion;
import krisapps.biaminereloaded.types.VisualisationType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ShootingRangeConfig implements CommandExecutor, Listener {

    BiamineReloaded main;

    public ShootingRangeConfig(BiamineReloaded main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Syntax: /shootingrange <gameID> <addSpot|editSpot|removeSpot|visualiseSpots|showInfo>
        /*
                   addSpot: <none>
                   editSpot: <spotID> <setbound1|setbound2|addTarget|removeTarget>
                   removeSpot <spotID>
                   visualiseSpots: <none>
                   showInfo: <shootingRange|spots|target>
         */

        if (args.length >= 2) {
            String gameID = args[0];
            String operation = args[1];
            Player p = (Player) sender;

            if (!main.dataUtility.gameExists(gameID)) {
                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.generic.nogame"));
                return true;
            }

            switch (operation) {
                case "addSpot":
                    main.dataUtility.setShootingSpotBound(gameID, p.getLocation(), 1);
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.addspot.added")
                            .replaceAll("%game%", gameID)
                    );
                    break;
                case "editSpot":
                    if (args.length >= 4) {
                        String spotID = args[2];
                        String subOperation = args[3];

                        if (!main.dataUtility.shootingSpotExists(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")))) {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.generic.nospot"));
                        }

                        switch (subOperation) {
                            case "setbound1":
                                Location location = p.getLocation();
                                if (main.dataUtility.setShootingSpotBound(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")), location, 1)) {
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.setbound1-set")
                                            .replaceAll("%number%", spotID.replaceAll("shootingSpot", ""))
                                            .replaceAll("%x%", String.valueOf(location.getX()))
                                            .replaceAll("%y%", String.valueOf(location.getY()))
                                            .replaceAll("%z%", String.valueOf(location.getZ()))
                                    );
                                } else {
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.generic.failed"));
                                }
                                break;
                            case "setbound2":
                                Location location2 = p.getLocation();
                                if (main.dataUtility.setShootingSpotBound(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")), location2, 2)) {
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.setbound2-set")
                                            .replaceAll("%number%", spotID.replaceAll("shootingSpot", ""))
                                            .replaceAll("%x%", String.valueOf(location2.getX()))
                                            .replaceAll("%y%", String.valueOf(location2.getY()))
                                            .replaceAll("%z%", String.valueOf(location2.getZ()))
                                    );
                                } else {
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.generic.failed"));
                                }
                                break;
                            case "addTarget":
                                main.dataUtility.setTemporaryValue("pendingOperation.operation", "addTarget");
                                main.dataUtility.setTemporaryValue("pendingOperation.playerUUID", ((Player) sender).getUniqueId().toString());
                                main.dataUtility.setTemporaryValue("pendingOperation.spotNumber", String.valueOf(Integer.parseInt(spotID.replaceAll("shootingSpot", ""))));
                                main.dataUtility.setTemporaryValue("pendingOperation.spotID", spotID);
                                main.dataUtility.setTemporaryValue("pendingOperation.gameID", gameID);
                                initTimeLimit(10, sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.addtarget-click"));
                                break;
                            case "removeTarget":
                                main.dataUtility.setTemporaryValue("pendingOperation.operation", "removeTarget");
                                main.dataUtility.setTemporaryValue("pendingOperation.playerUUID", ((Player) sender).getUniqueId().toString());
                                main.dataUtility.setTemporaryValue("pendingOperation.spotNumber", String.valueOf(Integer.parseInt(spotID.replaceAll("shootingSpot", ""))));
                                main.dataUtility.setTemporaryValue("pendingOperation.spotID", spotID);
                                main.dataUtility.setTemporaryValue("pendingOperation.gameID", gameID);
                                initTimeLimit(10, sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.removetarget-click"));
                                break;
                        }
                    }
                    break;
                case "removeSpot":
                    if (args.length >= 3) {
                        String spotID = args[2];
                        if (main.dataUtility.shootingSpotExists(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")))) {
                            main.dataUtility.removeShootingSpot(gameID, Integer.parseInt(spotID.replaceAll("shootingSpot", "")));
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.removespot.removed")
                                    .replaceAll("%game%", gameID)
                                    .replaceAll("%spotID%", spotID)
                            );
                        } else {
                            main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.removespot.notfound")
                                    .replaceAll("%spot%", spotID)
                            );
                        }
                    }
                    break;
                case "visualiseSpots":
                    if (main.visualisationUtility.isRunning() && main.visualisationUtility.isBusyWith(VisualisationType.AREA)) {
                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.visualise.blocked"));
                        return true;
                    }
                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.visualise.start"));
                    ArrayList<ArmorStand> numberEntities = new ArrayList<>();
                    ArrayList<CuboidRegion> areaList = new ArrayList<>();

                    if (!main.dataUtility.getShootingSpots(gameID).isEmpty()) {
                        for (String shootingSpot : main.dataUtility.getShootingSpots(gameID)) {
                            Location b1 = main.dataUtility.getShootingSpotBound(gameID, Integer.parseInt(shootingSpot.replaceAll("shootingSpot", "")), 1);
                            Location b2 = main.dataUtility.getShootingSpotBound(gameID, Integer.parseInt(shootingSpot.replaceAll("shootingSpot", "")), 2);
                            if (b1 == null || b2 == null) {
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.visualise.err-spot")
                                        .replaceAll("%spot%", shootingSpot)
                                );
                                continue;
                            }
                            areaList.add(new CuboidRegion(b1, b2));

                            // Spawn an invisible armor stand with the spot info
                            Location centerPoint = new Location(b1.getWorld(),
                                    ((double) (b1.getBlockX() + b2.getBlockX()) / 2) + 0.5,
                                    ((double) (b1.getBlockY() + b2.getBlockY()) / 2) + 0.5,
                                    ((double) (b1.getBlockZ() + b2.getBlockZ()) / 2) + 0.5
                            );
                            ArmorStand stand = (ArmorStand) p.getWorld().spawnEntity(centerPoint, EntityType.ARMOR_STAND);
                            stand.setCustomNameVisible(true);
                            stand.setCustomName(ChatColor.translateAlternateColorCodes('&', "&f[&4Shooting Spot &l#%spot%&f]&b - &f(&e%game%&f)&r")
                                    .replaceAll("%game%", gameID)
                                    .replaceAll("%spot%", shootingSpot.replaceAll("shootingSpot", ""))
                            );
                            stand.setInvulnerable(true);
                            stand.setGravity(false);
                            stand.setVisible(false);
                            numberEntities.add(stand);
                            main.getServer().getScheduler().runTaskLater(main, new Runnable() {
                                @Override
                                public void run() {
                                    for (ArmorStand armorStand : numberEntities) {
                                        armorStand.remove();
                                    }
                                }
                            }, 20L * 10);
                        }
                        main.visualisationUtility.massVisualiseAreas(areaList, 10);
                    }
                    break;
                case "showInfo":
                    if (args.length >= 3) {
                        String suboperation = args[2];
                        switch (suboperation) {
                            case "shootingRange":
                                List<Location> targets = main.dataUtility.getTargetsForGame(gameID);
                                Set<String> spots = main.dataUtility.getShootingSpots(gameID);

                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.header"));
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.spots-title"));
                                for (String spot : spots) {

                                    TextComponent component1 = new TextComponent(ChatColor.translateAlternateColorCodes('&', main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.list-item").replaceAll("%spot%", spot + " - ")));
                                    TextComponent component2 = (TextComponent) main.messageUtility.createClickableTeleportButton("clicktext.teleport", main.dataUtility.getShootingSpotBound(gameID, Integer.parseInt(spot.replaceAll("shootingSpot", "")), 1), "hovertext.teleport-spot");
                                    sender.spigot().sendMessage(component1, component2);
                                }
                                main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.footer")
                                        .replaceAll("%spots%", String.valueOf(spots.size()))
                                        .replaceAll("%targets%", String.valueOf(targets.size()))
                                );
                                break;
                            case "spots":
                                Set<String> spotList = main.dataUtility.getShootingSpots(gameID);
                                for (String s : spotList) {
                                    List<Location> targetsForSpot = main.dataUtility.getShootingTargetsForSpot(gameID, Integer.parseInt(s.replaceAll("shootingSpot", "")));
                                    main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.spot-header")
                                            .replaceAll("%spot%", s + " &f(&a" + targetsForSpot.size() + "&f)&r")
                                    );
                                    for (Location t : targetsForSpot) {
                                        main.messageUtility.sendMessage(sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.list-item")
                                                                                                        .replaceAll(
                                                                                                                "%spot%",
                                                                                                                String.format(
                                                                                                                        "&eX: &b%d &eY: &b%d &eZ: &b%d&r",
                                                                                                                        t.getBlockX(),
                                                                                                                        t.getBlockY(),
                                                                                                                        t.getBlockZ()
                                                                                                                )
                                                                                                        )
                                        );
                                    }

                                }
                                break;
                            case "target":
                                main.dataUtility.setTemporaryValue("pendingOperation.operation", "targetInfo");
                                main.dataUtility.setTemporaryValue("pendingOperation.playerUUID", ((Player) sender).getUniqueId());
                                initTimeLimit(10, sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.targetinfo-click"));
                                break;
                        }

                    }
                    break;
            }
        }
        return true;
    }

    public void initTimeLimit(int time, CommandSender sender, String message) {
        main.dataUtility.setTemporaryValue("timers.click-task", main.getServer().getScheduler().scheduleAsyncRepeatingTask(main, new Runnable() {
            int timer = time;

            @Override
            public void run() {
                if (timer > 0) {
                    main.messageUtility.sendActionbarMessage((Player) sender, message.replaceAll("%time%", String.valueOf(timer)));
                    timer--;
                } else {
                    main.dataUtility.setTemporaryValue("pendingOperation", null);
                    main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                    main.messageUtility.sendActionbarMessage((Player) sender, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.generic.timed-out"));
                }
            }
        }, 0, 20L));
    }

    @EventHandler
    public void onBlockRightClicked(PlayerInteractEvent interactEvent) {
        if (main.dataUtility.getTemporaryValue("pendingOperation.operation") == null) {
            return;
        }
        if (!interactEvent.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (interactEvent.getHand() == EquipmentSlot.HAND) {

            if (!main.dataUtility.getTemporaryValue("pendingOperation.operation").equalsIgnoreCase("none")) {
                if (main.dataUtility.getTemporaryValue("pendingOperation.playerUUID") != null) {
                    if (interactEvent.getPlayer().getUniqueId().equals(UUID.fromString(main.dataUtility.getTemporaryValue("pendingOperation.playerUUID")))) {
                        Block target = interactEvent.getClickedBlock();
                        String gameID = main.dataUtility.getTemporaryValue("pendingOperation.gameID");
                        String spotID = main.dataUtility.getTemporaryValue("pendingOperation.spotID");
                        String spotNumber = main.dataUtility.getTemporaryValue("pendingOperation.spotNumber");

                        if (target == null) {
                            main.appendToLog("Failed to register clicked block while processing operation.");
                            main.messageUtility.sendActionbarMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.generic.invalidblock"));
                            return;
                        }

                        if (!target.isEmpty()) {
                            CustomBlockData dataContainer = new CustomBlockData(target, main);
                            switch (main.dataUtility.getTemporaryValue("pendingOperation.operation")) {
                                case "addTarget":

                                    // If there is data for the block, it's definitely in use by some game.
                                    if (!dataContainer.getKeys().isEmpty()) {
                                        main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                                        main.dataUtility.setTemporaryValue("pendingOperation", null);
                                        main.messageUtility.sendMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.addtarget-inuse")
                                                .replaceAll("%spot%", "Shooting Spot #" + dataContainer.get(new NamespacedKey(main, "range_spot_number"), PersistentDataType.PrimitivePersistentDataType.INTEGER))
                                                .replaceAll("%game%", dataContainer.get(new NamespacedKey(main, "ownerGameID"), PersistentDataType.PrimitivePersistentDataType.STRING))
                                                .replaceAll("%number%", spotNumber)

                                        );
                                        return;
                                    }

                                    int[] locationArray = new int[3];
                                    locationArray[0] = target.getLocation().getBlockX();
                                    locationArray[1] = target.getLocation().getBlockY();
                                    locationArray[2] = target.getLocation().getBlockZ();

                                    dataContainer.set(new NamespacedKey(main, "location"),
                                            PersistentDataType.PrimitivePersistentDataType.INTEGER_ARRAY, locationArray);
                                    dataContainer.set(new NamespacedKey(main, "location_world"),
                                            PersistentDataType.STRING, target.getLocation().getWorld().getName());
                                    dataContainer.set(new NamespacedKey(main, "target_number"),
                                            PersistentDataType.INTEGER, !main.dataUtility.getShootingTargetsForSpot(gameID, Integer.parseInt(spotNumber)).isEmpty() ? main.dataUtility.getShootingTargetsForSpot(gameID, Integer.parseInt(spotNumber)).size() + 1 : 1);
                                    dataContainer.set(new NamespacedKey(main, "range_spot_number"),
                                            PersistentDataType.INTEGER, Integer.parseInt(spotNumber));
                                    dataContainer.set(new NamespacedKey(main, "ownerGameID"),
                                            PersistentDataType.STRING, gameID);

                                    main.dataUtility.addTarget(gameID, spotID, target.getLocation());
                                    main.messageUtility.sendActionbarMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.block-set")
                                            .replaceAll("%x%", String.valueOf(target.getLocation().getBlockX()))
                                            .replaceAll("%y%", String.valueOf(target.getLocation().getBlockY()))
                                            .replaceAll("%z%", String.valueOf(target.getLocation().getBlockZ()))
                                    );
                                    main.messageUtility.sendMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.addtarget-added")
                                            .replaceAll("%spot%", spotID)
                                            .replaceAll("%game%", gameID)
                                            .replaceAll("%number%", spotNumber)
                                    );
                                    main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                                    main.dataUtility.setTemporaryValue("pendingOperation", null);
                                    break;
                                case "removeTarget":
                                    dataContainer = new CustomBlockData(target.getLocation().getBlock(), main);

                                    if (dataContainer.getKeys().isEmpty()) {
                                        main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                                        main.dataUtility.setTemporaryValue("pendingOperation", null);
                                        main.messageUtility.sendMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.removetarget-notatarget")
                                                .replaceAll("%number%", spotNumber)
                                        );
                                        return;
                                    } else {
                                        if (!dataContainer.get(new NamespacedKey(main, "ownerGameID"), PersistentDataType.STRING).equals(gameID)) {
                                            main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                                            main.dataUtility.setTemporaryValue("pendingOperation", null);
                                            main.messageUtility.sendMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.removetarget-wrong")
                                                    .replaceAll("%param%", "game")
                                                    .replaceAll("%number%", spotNumber)
                                            );
                                            return;
                                        } else if (!dataContainer.get(new NamespacedKey(main, "range_spot_number"), PersistentDataType.INTEGER).equals(Integer.parseInt(spotNumber))) {
                                            main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                                            main.dataUtility.setTemporaryValue("pendingOperation", null);
                                            main.messageUtility.sendMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.removetarget-wrong")
                                                    .replaceAll("%param%", "shooting spot")
                                                    .replaceAll("%number%", spotNumber)
                                            );
                                            return;
                                        }
                                    }
                                    dataContainer.clear();
                                    main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                                    main.dataUtility.setTemporaryValue("pendingOperation", null);
                                    main.dataUtility.removeTarget(gameID, spotID, target.getLocation());
                                    main.messageUtility.sendMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.removetarget-removed")
                                            .replaceAll("%number%", spotNumber)
                                    );
                                    main.messageUtility.sendActionbarMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.block-set")
                                            .replaceAll("%x%", String.valueOf(target.getLocation().getBlockX()))
                                            .replaceAll("%y%", String.valueOf(target.getLocation().getBlockY()))
                                            .replaceAll("%z%", String.valueOf(target.getLocation().getBlockZ()))
                                    );
                                    break;
                                case "targetInfo":

                                    boolean isCorrupted = false;

                                    int[] info_location;
                                    String info_world;
                                    int info_targetnum;
                                    int info_spotID;
                                    String info_gameID;

                                    if (CustomBlockData.hasCustomBlockData(target, main)) {

                                        try {
                                            info_location = dataContainer.get(new NamespacedKey(main, "location"),
                                                    PersistentDataType.PrimitivePersistentDataType.INTEGER_ARRAY);
                                        } catch (NullPointerException e) {
                                            isCorrupted = true;
                                            info_location = null;
                                        }
                                        try {
                                            info_world = dataContainer.get(new NamespacedKey(main, "location_world"),
                                                    PersistentDataType.STRING);
                                        } catch (NullPointerException e) {
                                            isCorrupted = true;
                                            info_world = "N/A";
                                        }
                                        try {
                                            info_targetnum = dataContainer.get(new NamespacedKey(main, "target_number"),
                                                    PersistentDataType.INTEGER);
                                        } catch (NullPointerException e) {
                                            isCorrupted = true;
                                            info_targetnum = -1;
                                        }
                                        try {
                                            info_spotID = dataContainer.get(new NamespacedKey(main, "range_spot_number"),
                                                    PersistentDataType.INTEGER);
                                        } catch (NullPointerException e) {
                                            isCorrupted = true;
                                            info_spotID = -1;
                                        }
                                        try {
                                            info_gameID = dataContainer.get(new NamespacedKey(main, "ownerGameID"),
                                                    PersistentDataType.STRING);
                                        } catch (NullPointerException e) {
                                            isCorrupted = true;
                                            info_gameID = "N/A";
                                        }

                                        if (isCorrupted) {
                                            main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                                            main.dataUtility.setTemporaryValue("pendingOperation", null);
                                            main.messageUtility.sendMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.targetinfo-corrupted")
                                                    .replaceAll("%order%", info_targetnum == -1 ? "?" : String.valueOf(info_targetnum))
                                                    .replaceAll("%spot%", info_spotID == -1 ? "N/A" : String.valueOf(info_spotID))
                                                    .replaceAll("%game%", info_gameID)
                                                    .replaceAll("%x%", info_location == null ? "?" : String.valueOf(info_location[0]))
                                                    .replaceAll("%y%", info_location == null ? "?" : String.valueOf(info_location[1]))
                                                    .replaceAll("%z%", info_location == null ? "?" : String.valueOf(info_location[2]))
                                                    .replaceAll("%world%", info_world)
                                            );
                                        } else {
                                            main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                                            main.dataUtility.setTemporaryValue("pendingOperation", null);
                                            main.messageUtility.sendMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.targetinfo-info")
                                                    .replaceAll("%order%", info_targetnum == -1 ? "?" : String.valueOf(info_targetnum))
                                                    .replaceAll("%spot%", info_spotID == -1 ? "N/A" : String.valueOf(info_spotID))
                                                    .replaceAll("%game%", info_gameID)
                                                    .replaceAll("%x%", info_location == null ? "?" : String.valueOf(info_location[0]))
                                                    .replaceAll("%y%", info_location == null ? "?" : String.valueOf(info_location[1]))
                                                    .replaceAll("%z%", info_location == null ? "?" : String.valueOf(info_location[2]))
                                                    .replaceAll("%world%", info_world)
                                            );
                                        }
                                    } else {
                                        main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                                        main.dataUtility.setTemporaryValue("pendingOperation", null);
                                        main.messageUtility.sendMessage(interactEvent.getPlayer(), main.localizationUtility.getLocalizedPhrase("commands.shootingrange.showinfo.targetinfo-notarget")
                                                .replaceAll("%x%", String.valueOf(target.getLocation().getBlockX()))
                                                .replaceAll("%y%", String.valueOf(target.getLocation().getBlockX()))
                                                .replaceAll("%z%", String.valueOf(target.getLocation().getBlockX()))
                                        );
                                    }
                                    break;
                            }
                        } else {
                            main.getServer().getScheduler().cancelTask(Integer.parseInt(main.dataUtility.getTemporaryValue("timers.click-task")));
                            main.dataUtility.setTemporaryValue("pendingOperation", null);
                            Player p = Bukkit.getPlayer(main.dataUtility.getTemporaryValue("pendingOperation.playerUUID"));
                            main.messageUtility.sendMessage(p, main.localizationUtility.getLocalizedPhrase("commands.shootingrange.editspot.addtarget-notair")
                                    .replaceAll("%number%", String.valueOf(spotNumber))
                            );
                        }
                    }
                }
            }
        }
    }


}
