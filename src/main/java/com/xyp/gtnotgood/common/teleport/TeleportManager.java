package com.xyp.gtnotgood.common.teleport;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.ae2thing.util.Util;

/**
 * Server-authoritative teleport helpers used by the wireless dual interface terminal.
 */
public final class TeleportManager {

    private TeleportManager() {}

    /**
     * Teleports beside an interface selected from the server-validated wireless interface terminal list.
     *
     * @param player player requesting the teleport
     * @param target interface location and side selected in the terminal
     * @return true when the player was moved
     */
    public static boolean teleportNearInterface(EntityPlayerMP player, Util.DimensionalCoordSide target) {
        int dimension = target.getDimension();
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            sendInterfaceDimensionUnavailable(player);
            return false;
        }

        WorldServer targetWorld = DimensionManager.getWorld(dimension);
        if (targetWorld == null) {
            DimensionManager.initDimension(dimension);
            targetWorld = DimensionManager.getWorld(dimension);
        }
        if (targetWorld == null) {
            sendInterfaceDimensionUnavailable(player);
            return false;
        }

        targetWorld.getChunkFromBlockCoords(target.x, target.z);
        int[] arrival = findSafeInterfaceArrival(targetWorld, target.x, target.y, target.z, target.getSide());
        if (arrival == null) {
            // #tr gtnotgood.message.interface_teleport.no_safe_location
            // # No safe standing space was found beside this interface.
            // # zh_CN 未在该接口旁找到安全的落脚位置。
            player
                .addChatMessage(new ChatComponentTranslation("gtnotgood.message.interface_teleport.no_safe_location"));
            return false;
        }

        double targetX = arrival[0] + 0.5D;
        double targetY = arrival[1];
        double targetZ = arrival[2] + 0.5D;
        player.closeScreen();
        if (player.dimension == dimension) {
            player.playerNetServerHandler
                .setPlayerLocation(targetX, targetY, targetZ, player.rotationYaw, player.rotationPitch);
        } else {
            MinecraftServer.getServer()
                .getConfigurationManager()
                .transferPlayerToDimension(
                    player,
                    dimension,
                    new DirectTeleportTeleporter(targetWorld, targetX, targetY, targetZ));
        }
        return true;
    }

    private static void sendInterfaceDimensionUnavailable(EntityPlayerMP player) {
        // #tr gtnotgood.message.interface_teleport.dimension_unavailable
        // # The interface dimension is currently unavailable.
        // # zh_CN 接口所在维度当前不可用。
        player
            .addChatMessage(new ChatComponentTranslation("gtnotgood.message.interface_teleport.dimension_unavailable"));
    }

    private static int[] findSafeInterfaceArrival(WorldServer world, int x, int y, int z,
        ForgeDirection preferredSide) {
        ForgeDirection[] horizontal = { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST,
            ForgeDirection.EAST };
        if (preferredSide != null && preferredSide != ForgeDirection.UNKNOWN && preferredSide.offsetY == 0) {
            int[] preferred = findSafeColumn(world, x + preferredSide.offsetX, y, z + preferredSide.offsetZ);
            if (preferred != null) return preferred;
        }
        for (ForgeDirection direction : horizontal) {
            if (direction == preferredSide) continue;
            int[] result = findSafeColumn(world, x + direction.offsetX, y, z + direction.offsetZ);
            if (result != null) return result;
        }
        for (int radius = 2; radius <= 12; radius++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    if (Math.abs(offsetX) != radius && Math.abs(offsetZ) != radius) continue;
                    int[] result = findSafeColumn(world, x + offsetX, y, z + offsetZ);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    private static int[] findSafeColumn(WorldServer world, int x, int targetY, int z) {
        for (int deltaY = 0; deltaY <= 12; deltaY++) {
            int above = targetY + deltaY;
            if (isSafe(world, x, above, z)) return new int[] { x, above, z };
            if (deltaY > 0) {
                int below = targetY - deltaY;
                if (isSafe(world, x, below, z)) return new int[] { x, below, z };
            }
        }
        return null;
    }

    private static boolean isSafe(WorldServer world, int x, int y, int z) {
        if (y < 1 || y >= world.getActualHeight() - 1) return false;
        if (!world.isAirBlock(x, y, z) || !world.isAirBlock(x, y + 1, z)) return false;
        Block floor = world.getBlock(x, y - 1, z);
        return floor != null && floor.getMaterial()
            .blocksMovement();
    }

    private static final class DirectTeleportTeleporter extends Teleporter {

        private final double x;
        private final double y;
        private final double z;

        private DirectTeleportTeleporter(WorldServer world, double x, double y, double z) {
            super(world);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void placeInPortal(Entity entity, double ignoredX, double ignoredY, double ignoredZ, float yaw) {
            entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
            entity.motionX = 0.0D;
            entity.motionY = 0.0D;
            entity.motionZ = 0.0D;
        }
    }
}
