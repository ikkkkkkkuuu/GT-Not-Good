package com.xyp.gtnotgood.client.journeymap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.util.ForgeDirection;

public final class JourneyMapLanding {

    private static final int SEARCH_RADIUS = 16;

    private JourneyMapLanding() {}

    public enum Status {
        Safe,
        Unknown,
        Blocked
    }

    public static final class Result {

        public final Status status;
        public final int y;

        private Result(Status status, int y) {
            this.status = status;
            this.y = y;
        }
    }

    public static Result resolve(World world, int x, int z, int preferredY, boolean cave) {
        int fallback = clamp(preferredY, world == null ? 256 : world.getActualHeight());
        if (x < -30_000_000 || x >= 30_000_000 || z < -30_000_000 || z >= 30_000_000)
            return new Result(Status.Blocked, fallback);
        return resolve(world == null ? null : new WorldColumn(world, x, z), preferredY, cave);
    }

    static Result resolve(Column column, int preferredY, boolean cave) {
        int height = column == null ? 256 : Math.min(256, column.height());
        int fallback = clamp(preferredY, height);
        if (column == null || !column.loaded()) return new Result(Status.Unknown, fallback);
        if (height < 3) return new Result(Status.Blocked, fallback);
        int maxY = height - 2;
        if (cave) {
            for (int distance = 0; distance <= SEARCH_RADIUS; distance++) {
                int above = fallback + distance;
                if (above <= maxY && safe(column, above)) return new Result(Status.Safe, above);
                int below = fallback - distance;
                if (distance > 0 && below >= 1 && safe(column, below)) return new Result(Status.Safe, below);
            }
        } else {
            int surface = clamp(column.surfaceY(), height);
            for (int y = surface; y <= maxY; y++) {
                if (safe(column, y)) return new Result(Status.Safe, y);
            }
            for (int y = surface - 1; y >= 1; y--) {
                if (safe(column, y)) return new Result(Status.Safe, y);
            }
        }
        return new Result(Status.Blocked, fallback);
    }

    private static int clamp(int y, int height) {
        return Math.max(1, Math.min(Math.max(1, Math.min(256, height) - 2), y));
    }

    private static boolean safe(Column column, int y) {
        return column.supports(y - 1) && column.clear(y) && column.clear(y + 1);
    }

    interface Column {

        int height();

        int surfaceY();

        boolean loaded();

        boolean supports(int y);

        boolean clear(int y);
    }

    private static final class WorldColumn implements Column {

        private final World world;
        private final int x;
        private final int z;
        private final Chunk chunk;

        private WorldColumn(World world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
            IChunkProvider provider = world.getChunkProvider();
            Chunk loaded = provider != null && provider.chunkExists(x >> 4, z >> 4)
                ? world.getChunkFromChunkCoords(x >> 4, z >> 4)
                : null;
            chunk = loaded instanceof EmptyChunk ? null : loaded;
        }

        @Override
        public int height() {
            return world.getActualHeight();
        }

        @Override
        public int surfaceY() {
            return chunk.getHeightValue(x & 15, z & 15);
        }

        @Override
        public boolean loaded() {
            return chunk != null;
        }

        @Override
        public boolean supports(int y) {
            Block floor = chunk.getBlock(x & 15, y, z & 15);
            if (hazardous(floor) || !floor.isSideSolid(world, x, y, z, ForgeDirection.UP)) return false;
            AxisAlignedBB collision = floor.getCollisionBoundingBoxFromPool(world, x, y, z);
            return collision != null && Math.abs(collision.maxY - y - 1) < 1.0E-6
                && collision.minX <= x + .2
                && collision.maxX >= x + .8
                && collision.minZ <= z + .2
                && collision.maxZ >= z + .8;
        }

        @Override
        public boolean clear(int y) {
            Block block = chunk.getBlock(x & 15, y, z & 15);
            return !hazardous(block) && block.isAir(world, x, y, z)
                && block.getCollisionBoundingBoxFromPool(world, x, y, z) == null;
        }

        private static boolean hazardous(Block block) {
            Material material = block.getMaterial();
            return material.isLiquid() || material == Material.fire
                || material == Material.cactus
                || material == Material.portal
                || material == Material.web;
        }
    }
}
