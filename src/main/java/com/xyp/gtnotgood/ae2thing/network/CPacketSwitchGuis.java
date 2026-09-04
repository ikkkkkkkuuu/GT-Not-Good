package com.xyp.gtnotgood.ae2thing.network;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.ae2thing.inventory.InventoryHandler;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.util.BlockPos;
import com.xyp.gtnotgood.ae2thing.util.Util;

import appeng.client.gui.AEBaseGui;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;
import appeng.container.interfaces.IInventorySlotAware;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketSwitchGuis implements IMessage {

    private GuiType guiType;
    /**
     * When set, the server ignores {@link #guiType} and instead resolves the view to open from the terminal's own
     * authoritative NBT ({@link Util#getLastGuiMode}). Used by the Baubles keybind path, whose worn-slot NBT is not
     * synced back to the client, so the client cannot reliably read the last-used mode itself.
     */
    private boolean restoreSaved;

    public CPacketSwitchGuis() {}

    public CPacketSwitchGuis(GuiType guiType) {
        this.guiType = guiType;
        AEBaseGui.setSwitchingGuis(true);
    }

    /**
     * Ask the server to reopen the dual interface terminal in whichever view was last saved on the stack, without the
     * client reading (and potentially clobbering with a stale value) the terminal's NBT.
     */
    public static CPacketSwitchGuis restoreLast() {
        CPacketSwitchGuis p = new CPacketSwitchGuis();
        p.restoreSaved = true;
        AEBaseGui.setSwitchingGuis(true);
        return p;
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
        guiType = GuiType.getByOrdinal(byteBuf.readByte());
        restoreSaved = byteBuf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(guiType != null ? guiType.ordinal() : 0);
        buf.writeBoolean(restoreSaved);
    }

    public static class Handler implements IMessageHandler<CPacketSwitchGuis, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketSwitchGuis message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            World w = player.worldObj;
            // Baubles keybind path: the client can't read the worn terminal's last-used mode (server NBT changes on
            // worn slots aren't synced back), so it just asks us to restore. Resolve from authoritative server NBT and
            // do NOT write it back (writing the client-supplied value would clobber the real saved mode).
            if (message.restoreSaved) {
                int s = Util.findDualInterfaceTerminal(player);
                if (s != -1) {
                    GuiType mode = Util
                        .getLastGuiMode(Util.getTerminalInSlot(player, s), GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL);
                    InventoryHandler.openGui(player, w, new BlockPos(s, 0, 0), ForgeDirection.UNKNOWN, mode);
                }
                return null;
            }
            if (message.guiType == null) {
                return null;
            }
            Container cont = player.openContainer;
            if (cont instanceof ContainerQuickEncodingTerminal quickTerminal) {
                // openGui constructs the next wireless host from the same ItemStack.
                // Restore AE2's native 9-wide pin-row values before that host reads NBT;
                // onContainerClosed alone can run too late in the GUI-switch sequence.
                quickTerminal.restoreNativePinRows();
            }
            if (message.guiType == GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL
                || message.guiType == GuiType.WIRELESS_CRAFTING_TERMINAL) {
                int s = Util.findDualInterfaceTerminal(player);
                if (s != -1) {
                    // Remember which of the two views the player switched to so reopening the terminal restores it.
                    Util.setLastGuiMode(player, s, message.guiType);
                    InventoryHandler.openGui(player, w, new BlockPos(s, 0, 0), ForgeDirection.UNKNOWN, message.guiType);
                }
                return null;
            }
            if (cont instanceof AEBaseContainer c) {
                ContainerOpenContext context = c.getOpenContext();
                if (context == null) {
                    return null;
                }
                // Save the old container's PrimaryGui so the new container can use it
                PrimaryGui pGui = c.getPrimaryGui();
                TileEntity te = context.getTile();
                if (te != null) {
                    InventoryHandler.openGui(
                        player,
                        player.worldObj,
                        new BlockPos(te),
                        Objects.requireNonNull(context.getSide()),
                        message.guiType);
                } else {
                    InventoryHandler.openGui(
                        player,
                        player.getEntityWorld(),
                        new BlockPos(((IInventorySlotAware) (c.getTarget())).getInventorySlot(), 0, 0),
                        Objects.requireNonNull(context.getSide()),
                        message.guiType);
                }
                // Set PrimaryGui on the new container for AE2's new GUI system
                if (pGui != null && player.openContainer instanceof AEBaseContainer nc) {
                    nc.setOpenContext(context);
                    nc.setPrimaryGui(pGui);
                }
            }
            return null;
        }

    }

}
