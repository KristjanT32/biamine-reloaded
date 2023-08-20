package krisapps.biaminereloaded.utilities;

import krisapps.biaminereloaded.BiamineReloaded;
import krisapps.biaminereloaded.types.CollidableRegion;
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
    Map<Integer, VisualisationType> runningTasks;

    public VisualisationUtility(BiamineReloaded main) {
        this.main = main;
        this.runningTasks = new HashMap<>();
    }

    public boolean isRunning() {
        return !runningTasks.isEmpty();
    }

    public boolean isBusyWith(VisualisationType taskType) {
        return runningTasks.containsValue(taskType);
    }

    public void visualiseArea(Location bound1, Location bound2, int duration) {
        World world = bound1.getWorld();
        if (world == null) {
            return;
        }
        int TASK = runVisualise(bound1, bound2, world);
        runningTasks.put(TASK, VisualisationType.AREA);
        cancelVisualisation(TASK, duration);
    }

    private int runVisualise(Location bound1, Location bound2, World world) {
        return main.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

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
    }

    public void massVisualiseAreas(ArrayList<CuboidRegion> areaList, int duration) {
        World world = areaList.get(0).getBound1().getWorld();
        ArrayList<Integer> tasks = new ArrayList<>();
        for (CuboidRegion region : areaList) {
            Location bound1 = region.getBound1();
            Location bound2 = region.getBound2();
            int TASK = runVisualise(bound1, bound2, world);
            tasks.add(TASK);
            runningTasks.put(TASK, VisualisationType.AREA);
        }
        massCancel(tasks, 10);
    }

    public void visualiseCheckpoint(CollidableRegion region, int duration) {
        Location bound1 = region.getLowerBoundLocation();
        Location bound2 = region.getUpperBoundLocation();
        World world = bound1.getWorld();

        Particle particleToUse = region.isFinish() ? Particle.DRAGON_BREATH : Particle.COMPOSTER;

        int TASK = main.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

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
                            world.spawnParticle(particleToUse, x + 0.5, y + 0.5, z + 0.5, 2, 0, 0, 0, 0.01);
                        }
                    }
                }
            }
        }, 0, 1);
        runningTasks.put(TASK, VisualisationType.AREA);
        cancelVisualisation(TASK, duration);
    }

    private void massCancel(ArrayList<Integer> tasks, int delay) {
        main.getServer().getScheduler().runTaskLaterAsynchronously(main, new Runnable() {
            @Override
            public void run() {
                for (int task : tasks) {
                    main.getServer().getScheduler().cancelTask(task);
                    runningTasks.remove(task);
                }
            }
        }, delay * 20L);
    }

    private void cancelVisualisation(int task, int duration) {
        main.getServer().getScheduler().runTaskLaterAsynchronously(main, new Runnable() {
            @Override
            public void run() {
                main.getServer().getScheduler().cancelTask(task);
                runningTasks.remove(task);
            }
        }, duration * 20L);
    }

    public void cancelAllVisualisations() {
        for (Map.Entry<Integer, VisualisationType> entry : runningTasks.entrySet()) {
            main.appendToLog("Cancelling visualisation task: " + entry.getKey() + " (of type " + entry.getValue() + ")");
            main.getServer().getScheduler().cancelTask(entry.getKey());
        }
    }


}
