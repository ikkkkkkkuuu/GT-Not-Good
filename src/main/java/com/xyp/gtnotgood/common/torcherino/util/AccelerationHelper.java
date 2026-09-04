package com.xyp.gtnotgood.common.torcherino.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.torcherino.api.ITileEntityTickAcceleration;
import com.xyp.gtnotgood.common.torcherino.api.ITorcherinoTile;
import com.xyp.gtnotgood.config.Config;

/**
 * Shared acceleration routine used by area and wireless Torcherino tile entities.
 */
public final class AccelerationHelper {

    private static final Random SHARED_RANDOM = new Random();
    private static final Map<World, TickTracker> WORLD_TRACKERS = new WeakHashMap<>();

    private AccelerationHelper() {}

    /**
     * Applies a configured number of extra ticks to a single block position.
     *
     * @param world    world containing the target
     * @param torchX   source torch X coordinate
     * @param torchY   source torch Y coordinate
     * @param torchZ   source torch Z coordinate
     * @param timeRate number of extra ticks to apply
     * @param x        target X coordinate
     * @param y        target Y coordinate
     * @param z        target Z coordinate
     */
    public static void accelerateAtPosition(World world, int torchX, int torchY, int torchZ, int timeRate, int x, int y,
        int z) {
        if (world == null || timeRate <= 0) return;
        if (x == torchX && y == torchY && z == torchZ) return;
        if (!world.blockExists(x, y, z)) return;

        boolean useOverlap = !Config.torcherinoEnableStackingAcceleration && Config.torcherinoEnableOverlapDetection;
        if (useOverlap && getPositionSpeed(world, x, y, z) >= timeRate) return;
        if (useOverlap) {
            markPositionAccelerated(world, x, y, z, timeRate);
        }

        long budgetEnd = Config.torcherinoEnableTickBudget ? System.nanoTime() + Config.torcherinoTickBudgetNanos : 0L;

        Block block = world.getBlock(x, y, z);
        if (block != null && block.getTickRandomly()) {
            for (int i = 0; i < timeRate; i++) {
                try {
                    block.updateTick(world, x, y, z, SHARED_RANDOM);
                } catch (Exception ignored) {}
                if (Config.torcherinoEnableTickBudget && System.nanoTime() > budgetEnd) return;
            }
        }

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity == null || tileEntity.isInvalid() || tileEntity instanceof ITorcherinoTile) return;

        if (tileEntity instanceof ITileEntityTickAcceleration) {
            ITileEntityTickAcceleration acceleration = (ITileEntityTickAcceleration) tileEntity;
            if (acceleration.tickAcceleration(timeRate)) {
                return;
            }
        }

        if (!tileEntity.canUpdate()) return;

        for (int i = 0; i < timeRate; i++) {
            try {
                tileEntity.updateEntity();
            } catch (Exception ignored) {}
            if (Config.torcherinoEnableTickBudget && System.nanoTime() > budgetEnd) return;
        }
    }

    private static long packPosition(int x, int y, int z) {
        return ((long) x & 0x1FFFFFL) << 42 | ((long) y & 0x1FFFFFL) << 21 | ((long) z & 0x1FFFFFL);
    }

    private static int getPositionSpeed(World world, int x, int y, int z) {
        synchronized (WORLD_TRACKERS) {
            TickTracker tracker = WORLD_TRACKERS.get(world);
            if (tracker == null) return 0;
            long currentTick = world.getTotalWorldTime();
            if (tracker.worldTick != currentTick) {
                tracker.worldTick = currentTick;
                tracker.acceleratedPositions.clear();
                return 0;
            }
            Integer speed = tracker.acceleratedPositions.get(packPosition(x, y, z));
            return speed == null ? 0 : speed;
        }
    }

    private static void markPositionAccelerated(World world, int x, int y, int z, int speed) {
        synchronized (WORLD_TRACKERS) {
            TickTracker tracker = WORLD_TRACKERS.get(world);
            if (tracker == null) {
                tracker = new TickTracker();
                WORLD_TRACKERS.put(world, tracker);
            }
            long currentTick = world.getTotalWorldTime();
            if (tracker.worldTick != currentTick) {
                tracker.worldTick = currentTick;
                tracker.acceleratedPositions.clear();
            }
            tracker.acceleratedPositions.put(packPosition(x, y, z), speed);
        }
    }

    /**
     * Per-world speed ownership for overlap checks during one server tick.
     */
    private static final class TickTracker {

        long worldTick = -1L;
        final Map<Long, Integer> acceleratedPositions = new HashMap<>(256);
    }
}
