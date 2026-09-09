package com.xyp.gtnotgood.common.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.network.NetworkTopology.Endpoint;
import com.xyp.gtnotgood.common.network.TileNetworkController.Channel;

import gregtech.api.interfaces.tileentity.IBasicEnergyContainer;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Lossless GT EU conversion. Extraction is buffered before delivery; rejected EU remains persisted.
 * Only GT machines with explicit voltage, amperage and capacity contracts are connected.
 * Destination packets never exceed rated input voltage, including when powered by a higher tier source.
 */
final class NetworkEnergyTransfer {

    private NetworkEnergyTransfer() {}

    static boolean tick(TileNetworkController controller, Channel channel, Map<IGregTechTileEntity, Long> drawn) {
        List<Endpoint> sources = new ArrayList<>();
        List<Endpoint> sinks = new ArrayList<>();
        for (Endpoint endpoint : controller.topology().endpoints) {
            NetworkRule rule = channel.rules.get(endpoint.key);
            if (rule == null || !(endpoint.target() instanceof IGregTechTileEntity)) continue;
            if (rule.mode == 1 && rule.due(
                controller.getWorldObj()
                    .getTotalWorldTime()))
                sources.add(endpoint);
            if (rule.mode == 2) sinks.add(endpoint);
        }
        if (sinks.isEmpty()) return false;
        java.util.Collections.rotate(sinks, -(channel.cursor % sinks.size()));
        sinks.sort(
            Comparator.comparingInt((Endpoint e) -> channel.rules.get(e.key).priority)
                .reversed());
        boolean changed = false;
        int probes = 8192;
        if (!channel.hasCargo()) {
            for (int i = 0; i < sources.size(); i++) {
                if (--probes < 0) break;
                Endpoint source = sources.get((channel.cursor + i) % sources.size());
                if (!(source.target() instanceof IGregTechTileEntity tile)) continue;
                ForgeDirection side = channel.rules.get(source.key)
                    .face(source);
                if (!tile.outputsEnergyTo(side)) continue;
                long amperes = Math.max(0, tile.getOutputAmperage() - drawn.getOrDefault(tile, 0L));
                long limit = Math.min(channel.rules.get(source.key).rate, Math.max(0, tile.getStoredEU()));
                long voltage = Math.min(Math.max(0, tile.getOutputVoltage()), limit);
                if (voltage == 0 || amperes == 0) continue;
                amperes = Math.min(amperes, limit / voltage);
                boolean hasDestination = false;
                for (Endpoint sink : sinks) {
                    if (--probes < 0) break;
                    if (deviceKey(source).equals(deviceKey(sink))) continue;
                    if (sink.target() instanceof IGregTechTileEntity target && capacity(
                        target,
                        channel.rules.get(sink.key)
                            .face(sink),
                        channel.rules.get(sink.key).rate) > 0) {
                        hasDestination = true;
                        break;
                    }
                }
                if (!hasDestination || !tile.drainEnergyUnits(side, voltage, amperes)) continue;
                drawn.put(tile, drawn.getOrDefault(tile, 0L) + amperes);
                channel.energy = voltage * amperes;
                channel.source = deviceKey(source);
                tile.markDirty();
                changed = true;
                break;
            }
        }
        channel.cursor = (channel.cursor + 1) & Integer.MAX_VALUE;
        for (Endpoint sink : sinks) {
            if (!channel.rules.get(sink.key)
                .due(
                    controller.getWorldObj()
                        .getTotalWorldTime()))
                continue;
            if (channel.energy == 0) break;
            if (deviceKey(sink).equals(channel.source)) continue;
            if (!(sink.target() instanceof IGregTechTileEntity target)) continue;
            long moved = deliver(
                target,
                channel.rules.get(sink.key)
                    .face(sink),
                channel.energy,
                channel.rules.get(sink.key).rate);
            if (moved > 0) {
                channel.energy -= moved;
                target.markDirty();
                changed = true;
            }
        }
        return changed;
    }

    /** Returns a safe EU offer bounded by free space and the configured per-tick limit. */
    private static long capacity(IBasicEnergyContainer target, ForgeDirection side, long limit) {
        if (!target.inputEnergyFrom(side) || target.getInputVoltage() <= 0 || target.getInputAmperage() <= 0) return 0;
        long stored = Math.max(0, target.getStoredEU());
        long capacity = Math.max(0, target.getEUCapacity());
        return Math.min(Math.max(0, limit), capacity > stored ? capacity - stored : 0);
    }

    /**
     * Re-packets buffered EU at or below the recipient's voltage and respects GT's accepted-amp counter.
     * Multiplication is bounded by the offer before calling GT, so neither overflow nor overvoltage is possible.
     *
     * @return actual EU accepted; the caller retains the remainder
     */
    static long deliver(IBasicEnergyContainer target, ForgeDirection side, long available, long limit) {
        long offer = Math.min(Math.max(0, available), capacity(target, side, limit));
        long voltage = Math.min(offer, Math.max(0, target.getInputVoltage()));
        if (voltage == 0) return 0;
        long amperes = Math.min(offer / voltage, Math.max(0, target.getInputAmperage()));
        long accepted = target.injectEnergyUnits(side, voltage, amperes);
        return Math.max(0, Math.min(amperes, accepted)) * voltage;
    }

    private static String deviceKey(Endpoint endpoint) {
        return (endpoint.connector.xCoord + endpoint.direction.offsetX) + ":"
            + (endpoint.connector.yCoord + endpoint.direction.offsetY)
            + ":"
            + (endpoint.connector.zCoord + endpoint.direction.offsetZ);
    }
}
