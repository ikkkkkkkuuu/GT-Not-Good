package com.xyp.gtnotgood.ae2thing.network;

import static com.xyp.gtnotgood.ae2thing.api.Constants.DISPLAY_ONLY;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.xyp.gtnotgood.ae2thing.inventory.InventoryHandler;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.inventory.item.WirelessTerminal;
import com.xyp.gtnotgood.ae2thing.util.BlockPos;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.helpers.InventoryAction;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketInventoryAction implements IMessage {

    private InventoryAction action;
    private int slot;
    private long id;
    private IAEItemStack stack;
    private boolean isEmpty;

    public CPacketInventoryAction() {}

    public CPacketInventoryAction(final InventoryAction action, final int slot, final int id) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = null;
        this.isEmpty = true;
    }

    public CPacketInventoryAction(final InventoryAction action, final int slot, final int id, IAEItemStack stack) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = stack;
        this.isEmpty = stack == null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action.ordinal());
        buf.writeInt(slot);
        buf.writeLong(id);
        buf.writeBoolean(isEmpty);
        if (!isEmpty) {
            try {
                stack.writeToPacket(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = InventoryAction.values()[buf.readInt()];
        slot = buf.readInt();
        id = buf.readLong();
        isEmpty = buf.readBoolean();
        if (!isEmpty) {
            try {
                stack = AEItemStack.loadItemStackFromPacket(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Handler implements IMessageHandler<CPacketInventoryAction, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketInventoryAction message, MessageContext ctx) {
            final EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender.openContainer instanceof final AEBaseContainer baseContainer) {
                Object target = baseContainer.getTarget();
                if (message.action == InventoryAction.AUTO_CRAFT) {
                    final ContainerOpenContext context = baseContainer.getOpenContext();
                    if (context != null) {
                        final TileEntity te = context.getTile();
                        // Resolve the item slot for item-hosted terminals. When our own dual interface view is open the
                        // target is a WirelessTerminal; when the AE2 wireless crafting terminal view is open the target
                        // is AE2's WirelessCraftingTerminalGuiObject (NOT a WirelessTerminal) with no tile, so the old
                        // `target instanceof WirelessTerminal` gate dropped the auto-craft entirely. This packet is only
                        // ever sent by our dual interface terminal, so re-locate it in our own slot encoding rather than
                        // reading the target's slot (AE2's object uses a different baubles-slot offset than ours).
                        int itemSlot = -2; // sentinel: not an item-hosted terminal we recognise
                        if (target instanceof WirelessTerminal wt) {
                            itemSlot = wt.getInventorySlot();
                        } else if (te == null) {
                            itemSlot = com.xyp.gtnotgood.ae2thing.util.Util.findDualInterfaceTerminal(sender);
                        }
                        if (te != null || itemSlot != -2) {
                            if (message.stack == null){
                                if (baseContainer.getTargetStack() instanceof IAEItemStack ais) {
                                    message.stack = ais;
                                }
                            }
                            // [液滴分类] 必须留液滴：AUTO_CRAFT 分支,把流体伪装成液滴物设为合成目标(setTargetStack)下单,CraftingJobV2 只认 IAEItemStack
                            if(message.stack != null && FluidDropCompat.isFluidDrop(message.stack.getItem())){
                                ItemStack is = message.stack.getItemStack().copy();
                                NBTTagCompound data = is.getTagCompound();
                                // A fluid_drop built from a craftable fluid may have no NBT tag; guard against NPE.
                                if (data == null) {
                                    data = new NBTTagCompound();
                                }
                                data.removeTag(DISPLAY_ONLY);
                                is.setTagCompound(data);
                                baseContainer.setTargetStack(AEItemStack.create(is));
                            }else{
                                baseContainer.setTargetStack(message.stack);
                            }
                            if(te != null){
                                InventoryHandler.openGui(
                                    sender,
                                    te.getWorldObj(),
                                    new BlockPos(te),
                                    Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                    GuiType.CRAFTING_AMOUNT);
                            }else{
                                InventoryHandler.openGui(
                                    sender,
                                    sender.getEntityWorld(),
                                    new BlockPos(itemSlot,0,0),
                                    Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                    GuiType.CRAFTING_AMOUNT_ITEM);
                            }
                        }
                        if (sender.openContainer instanceof final ContainerCraftAmount cca) {
                            if (baseContainer.getTargetStack() instanceof IAEItemStack ais) {
                                cca.setItemToCraft(ais);
                            } else if (baseContainer.getTargetStack() != null) {
                                cca.setItemToCraft(baseContainer.getTargetStack());
                            }
                            cca.detectAndSendChanges();
                        }
                    }
                } else {
                    baseContainer.doAction(sender, message.action, message.slot, message.id);
                }
            }
            return null;
        }
    }
}
