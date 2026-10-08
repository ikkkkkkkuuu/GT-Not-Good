package com.xyp.gtnotgood.common.network;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.commandtree.network.CommandTreePacket;
import com.xyp.gtnotgood.common.network.mebridge.MessageMEWirelessNodeAction;
import com.xyp.gtnotgood.common.network.mebridge.MessageMEWirelessVisualization;
import com.xyp.gtnotgood.common.network.packaged.MessagePackagedConnector;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * Registers SimpleNetworkWrapper packets used by custom GT Not Good systems.
 */
public final class NetworkHandler {

    private NetworkHandler() {}

    /**
     * Registers packet discriminators in a deterministic order.
     */
    public static void registerAllMessage() {
        int id = 0;
        registerMessage(MessageMEWirelessNodeAction.class, MessageMEWirelessNodeAction.Handler.class, id++,
            Side.SERVER);
        registerMessage(MessageMEWirelessVisualization.class, MessageMEWirelessVisualization.Handler.class, id++,
            Side.CLIENT);
        registerMessage(SwapItems.class, SwapItems.Handler.class, id++, Side.SERVER);
        registerMessage(SyncToolBeltData.class, SyncToolBeltData.Handler.class, id++, Side.CLIENT);
        // 矿脉挖掘镐网络包
        // Vein Mining Pickaxe packets
        registerMessage(UpdateVeinMiningSetting.class, UpdateVeinMiningSetting.Handler.class, id++, Side.SERVER);
        registerMessage(ServerConfigMessage.class, ServerConfigMessage.ServerHandler.class, id, Side.SERVER);
        registerMessage(ServerConfigMessage.class, ServerConfigMessage.ClientHandler.class, id++, Side.CLIENT);
        registerMessage(MessagePackagedConnector.class, MessagePackagedConnector.Handler.class, id++, Side.SERVER);
        registerMessage(CommandTreePacket.class, CommandTreePacket.Handler.class, id++, Side.CLIENT);
        registerMessage(WirelessMonitorRequest.class, WirelessMonitorRequest.Handler.class, id++, Side.SERVER);
        registerMessage(WirelessMonitorSnapshot.class, WirelessMonitorSnapshot.Handler.class, id, Side.CLIENT);
    }

    private static <REQ extends IMessage, REPLY extends IMessage> void registerMessage(Class<REQ> messageClass,
        Class<? extends IMessageHandler<REQ, REPLY>> handlerClass, int id, Side side) {
        GTNotGood.channel.registerMessage(handlerClass, messageClass, id, side);
    }
}
