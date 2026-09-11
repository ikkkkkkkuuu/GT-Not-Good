package com.xyp.gtnotgood.common.packet;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.mebridge.MessageMEWirelessNodeAction;
import com.xyp.gtnotgood.common.mebridge.MessageMEWirelessVisualization;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * Registers SimpleNetworkWrapper packets used by custom GT Not Good systems.
 */
public final class NetWorkHandler {

    private NetWorkHandler() {}

    /**
     * Registers packet discriminators in a deterministic order.
     */
    public static void registerAllMessage() {
        int id = 0;
        registerMessage(
            MessageMEWirelessNodeAction.class,
            MessageMEWirelessNodeAction.Handler.class,
            id++,
            Side.SERVER);
        registerMessage(
            MessageMEWirelessVisualization.class,
            MessageMEWirelessVisualization.Handler.class,
            id++,
            Side.CLIENT);
        registerMessage(SwapItems.class, SwapItems.Handler.class, id++, Side.SERVER);
        registerMessage(SyncToolBeltData.class, SyncToolBeltData.Handler.class, id++, Side.CLIENT);
        // 矿脉挖掘镐网络包
        // Vein Mining Pickaxe packets
        registerMessage(SyncVeinPickaxeNBT.class, SyncVeinPickaxeNBT.Handler.class, id++, Side.SERVER);
        registerMessage(ServerConfigMessage.class, ServerConfigMessage.ServerHandler.class, id, Side.SERVER);
        registerMessage(ServerConfigMessage.class, ServerConfigMessage.ClientHandler.class, id++, Side.CLIENT);
    }

    private static <REQ extends IMessage, REPLY extends IMessage> void registerMessage(Class<REQ> messageClass,
        Class<? extends IMessageHandler<REQ, REPLY>> handlerClass, int id, Side side) {
        GTNotGood.channel.registerMessage(handlerClass, messageClass, id, side);
    }
}
