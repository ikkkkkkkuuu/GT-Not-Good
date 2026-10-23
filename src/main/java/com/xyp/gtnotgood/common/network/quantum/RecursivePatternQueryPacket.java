// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.network.quantum;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.crafting.cycle.RecursivePatternFinder;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.me.cache.CraftingGridCache;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Asks for the crafting cycles of the request the player is confirming: by the player, to be shown, or by the screen
 * once the calculation is done, to mark them in the crafting tree.
 */
public class RecursivePatternQueryPacket implements IMessage {

    private boolean manual;

    public RecursivePatternQueryPacket() {}

    public RecursivePatternQueryPacket(boolean manual) {
        this.manual = manual;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.manual);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.manual = buf.readBoolean();
    }

    public void handleServer(EntityPlayerMP player) {
        if (!(player.openContainer instanceof ContainerCraftConfirm container)) return;
        var request = container.getItemToCraft();
        IGrid grid;
        try {
            grid = container.getGrid();
        } catch (RuntimeException e) {
            grid = null;
        }
        if (
            request == null || grid == null
                || !(grid.getCache(ICraftingGrid.class) instanceof CraftingGridCache crafting)
        ) {
            return;
        }
        var cycles = RecursivePatternFinder.find(crafting, request, player.worldObj);
        GTNotGood.channel.sendTo(new RecursivePatternResultPacket(request, cycles, this.manual), player);
    }

    public static final class Handler implements IMessageHandler<RecursivePatternQueryPacket, IMessage> {

        @Override
        public IMessage onMessage(RecursivePatternQueryPacket message, MessageContext context) {
            var player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> message.handleServer(player));
            return null;
        }
    }
}
