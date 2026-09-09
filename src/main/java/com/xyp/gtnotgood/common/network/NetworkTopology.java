package com.xyp.gtnotgood.common.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fluids.IFluidHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Bounded loaded-chunk topology discovery, cached by controllers until the world network version changes. */
public final class NetworkTopology {

    private static final Map<World, Long> VERSIONS = new WeakHashMap<>();
    public final List<Endpoint> endpoints = new ArrayList<>();
    public final Map<String, Endpoint> byKey = new java.util.HashMap<>();
    public int nodes;
    public int controllers;
    /** 0: ready, 1: controller conflict, 2: size limit, 3: unloaded boundary. */
    public int status;

    public static void changed(World world) {
        if (world != null && !world.isRemote) VERSIONS.put(world, version(world) + 1);
    }

    public static long version(World world) {
        return VERSIONS.getOrDefault(world, 0L);
    }

    /** Chunk events also cover empty adjacent chunks, which have no node lifecycle callback. */
    public static final class Events {

        @SubscribeEvent
        public void load(ChunkEvent.Load event) {
            changed(event.world);
        }

        @SubscribeEvent
        public void unload(ChunkEvent.Unload event) {
            changed(event.world);
        }
    }

    public static NetworkTopology scan(TileNetworkController controller) {
        NetworkTopology result = new NetworkTopology();
        World world = controller.getWorldObj();
        Set<String> visited = new HashSet<>();
        ArrayDeque<TileNetworkNode> queue = new ArrayDeque<>();
        queue.add(controller);
        visited.add(position(controller));
        while (!queue.isEmpty()) {
            TileNetworkNode node = queue.removeFirst();
            if (++result.nodes > 1024) {
                result.status = 2;
                break;
            }
            if (node instanceof TileNetworkController) result.controllers++;
            boolean connector = world.getBlock(node.xCoord, node.yCoord, node.zCoord) instanceof BlockNetwork block
                && block.kind == BlockNetwork.CONNECTOR;
            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                int x = node.xCoord + direction.offsetX;
                int y = node.yCoord + direction.offsetY;
                int z = node.zCoord + direction.offsetZ;
                if (y < 0 || y >= world.getHeight()) continue;
                if (!world.blockExists(x, y, z)) {
                    result.status = 3;
                    continue;
                }
                TileEntity target = world.getTileEntity(x, y, z);
                if (target instanceof TileNetworkNode next && !next.isInvalid()) {
                    if (visited.add(position(next))) queue.addLast(next);
                } else if (connector && node.faceEnabled(direction.ordinal())
                    && (target instanceof IInventory || target instanceof IFluidHandler
                        || target instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity)) {
                            Endpoint endpoint = new Endpoint(node, direction);
                            result.endpoints.add(endpoint);
                            result.byKey.put(endpoint.key, endpoint);
                            if (result.endpoints.size() > 1536) {
                                result.status = 2;
                                return result;
                            }
                        }
            }
        }
        if (result.controllers != 1 && result.status == 0) result.status = 1;
        result.endpoints.sort(Comparator.comparing(endpoint -> endpoint.key));
        return result;
    }

    private static String position(TileEntity tile) {
        return tile.xCoord + ":" + tile.yCoord + ":" + tile.zCoord;
    }

    /** Stable connector position plus outward face; the device is accessed through the opposite face. */
    public static final class Endpoint {

        public final TileNetworkNode connector;
        public final ForgeDirection direction;
        public final String key;

        private Endpoint(TileNetworkNode connector, ForgeDirection direction) {
            this.connector = connector;
            this.direction = direction;
            this.key = position(connector) + ":" + direction.ordinal();
        }

        public TileEntity target() {
            World world = connector.getWorldObj();
            int x = connector.xCoord + direction.offsetX;
            int y = connector.yCoord + direction.offsetY;
            int z = connector.zCoord + direction.offsetZ;
            if (connector.isInvalid() || !world.blockExists(x, y, z) || !connector.faceEnabled(direction.ordinal()))
                return null;
            TileEntity tile = world.getTileEntity(x, y, z);
            return tile == null || tile.isInvalid() ? null : tile;
        }

        public int side() {
            return direction.getOpposite()
                .ordinal();
        }
    }
}
