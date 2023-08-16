package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.CuboidRegion;
import krisapps.biaminereloaded.types.VisualisationType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VisualisationUtility {

    BiamineReloaded main;
    Map<Integer, VisualisationType> visualisationTasks;
    private int ACTIVE_TASK = -1;

    public VisualisationUtility(BiamineReloaded main) {
        this.main = main;
        this.visualisationTasks = new HashMap<>();
    }

    public void visualizeArea(Location bound1, Location bound2, int duration) {
        World world = bound1.getWorld();
        if (!isRunning()) {
            ACTIVE_TASK = main.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

                final int minX = Math.min(bound1.getBlockX(), bound2.getBlockX());
                final int minY = Math.min(bound1.getBlockY(), bound2.getBlockY());
                final int minZ = Math.min(bound1.getBlockZ(), bound2.getBlockZ());

                final int maxX = Math.max(bound1.getBlockX(), bound2.getBlockX());
                final int maxY = Math.max(bound1.getBlockY(), bound2.getBlockY());
                final int maxZ = Math.max(bound1.getBlockZ(), bound2.getBlockZ());

                @Override
                public void run() {
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                world.spawnParticle(Particle.BLOCK_MARKER, x + 0.5, y + 0.5, z + 0.5, 5, 0, 0, 0, 0, Material.BARRIER.createBlockData());
                            }
                        }
                    }
                }
            }, 0, 1);
            visualisationTasks.put(ACTIVE_TASK, VisualisationType.AREA);
            main.getServer().getScheduler().runTaskLaterAsynchronously(main, () -> main.getServer().getScheduler().cancelTask(ACTIVE_TASK), duration * 20L);
        }
    }

    public void massVisualiseAreas(ArrayList<CuboidRegion> areaList, int duration) {
        World world = areaList.get(0).getBound1().getWorld();
        ArrayList<Integer> tasks = new ArrayList<>();
        for (CuboidRegion region : areaList) {
            Location bound1 = region.getBound1();
            Location bound2 = region.getBound2();
            tasks.add(main.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

                final int minX = Math.min(bound1.getBlockX(), bound2.getBlockX());
                final int minY = Math.min(bound1.getBlockY(), bound2.getBlockY());
                final int minZ = Math.min(bound1.getBlockZ(), bound2.getBlockZ());

                final int maxX = Math.max(bound1.getBlockX(), bound2.getBlockX());
                final int maxY = Math.max(bound1.getBlockY(), bound2.getBlockY());
                final int maxZ = Math.max(bound1.getBlockZ(), bound2.getBlockZ());

                @Override
                public void run() {
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                world.spawnParticle(Particle.BLOCK_MARKER, x + 0.5, y + 0.5, z + 0.5, 5, 0, 0, 0, 0, Material.BARRIER.createBlockData());
                            }
                        }
                    }
                }
            }, 0, 1));

            visualisationTasks.put(ACTIVE_TASK, VisualisationType.AREA);
        }
        massCancel(tasks, 10);
    }

    private void massCancel(ArrayList<Integer> tasks, int delay) {
        main.getServer().getScheduler().runTaskLaterAsynchronously(main, new Runnable() {
            @Override
            public void run() {
                for (int task : tasks) {
                    main.getServer().getScheduler().cancelTask(task);
                }
            }
        }, delay * 20L);
    }

    // TODO: Add visualisation methods for checkpoints, starting area

    public boolean isRunning() {
        return !visualisationTasks.isEmpty();
    }

    public ArrayList<VisualisationType> getActiveVisualisationTypes() {
        ArrayList<VisualisationType> out = new ArrayList<>();
        for (Map.Entry<Integer, VisualisationType> pair : visualisationTasks.entrySet()) {
            if (main.getServer().getScheduler().isCurrentlyRunning(pair.getKey())) {
                if (!out.contains(pair.getValue())) {
                    out.add(pair.getValue());
                }
            }
        }
        return out;
    }

}
