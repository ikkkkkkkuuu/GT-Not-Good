package com.xyp.gtnotgood.common.advancedio;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.glodblock.github.util.Util;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.config.SecurityPermissions;
import appeng.api.parts.IPartHost;

/** Position-and-face UI locator with distance, part identity and AE security checks on the server. */
public final class AdvancedIOGuiFactory extends AbstractUIFactory<AdvancedIOGuiFactory.Data> {

    public static final AdvancedIOGuiFactory INSTANCE = new AdvancedIOGuiFactory();

    private AdvancedIOGuiFactory() {
        super(ModList.GTNotGood.getResourcePath("advanced_io"));
    }

    public void open(EntityPlayer player, PartAdvancedIOBus part) {
        open(player, part, -1);
    }

    /** Opens the upstream amount sub-screen only for an enabled, populated ghost slot. */
    void open(EntityPlayer player, PartAdvancedIOBus part, int amountSlot) {
        if (!(player instanceof EntityPlayerMP serverPlayer) || player instanceof FakePlayer) return;
        if (amountSlot >= 0 && (amountSlot >= part.availableSlots() || part.filter(amountSlot) == null)) return;
        var tile = part.getTile();
        var data = new Data(
            player,
            tile.xCoord,
            tile.yCoord,
            tile.zCoord,
            part.getSide()
                .ordinal(),
            amountSlot);
        if (canInteractWith(player, data)) GuiManager.open(this, data, serverPlayer);
    }

    @Override
    public @Nonnull IGuiHolder<Data> getGuiHolder(Data data) {
        if (data.part == null) throw new IllegalStateException("Advanced IO bus no longer exists");
        return (guiData, sync, settings) -> data.amountSlot < 0 ? AdvancedIOGui.build(data.part, sync)
            : AdvancedIOGui.amountPanel(data.part, sync, data.amountSlot);
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public com.cleanroommc.modularui.screen.ModularScreen createScreen(Data data,
        com.cleanroommc.modularui.screen.ModularPanel panel) {
        return new com.cleanroommc.modularui.screen.ModularScreen(ModList.GTNotGood.getID(), panel);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, Data data) {
        return super.canInteractWith(player, data) && data.getSquaredDistance(player) <= 64
            && data.part != null
            && data.resolve() == data.part
            && (player.worldObj.isRemote
                || Util.hasPermission(player, SecurityPermissions.BUILD, (appeng.api.parts.IPart) data.part));
    }

    @Override
    public void writeGuiData(Data data, PacketBuffer buffer) {
        buffer.writeInt(data.getX());
        buffer.writeInt(data.getY());
        buffer.writeInt(data.getZ());
        buffer.writeByte(data.side);
        buffer.writeInt(data.amountSlot);
    }

    @Override
    public @Nonnull Data readGuiData(EntityPlayer player, PacketBuffer buffer) {
        return new Data(
            player,
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readUnsignedByte(),
            buffer.readInt());
    }

    /** Captures the original part so replacing a cable cannot redirect an already open container. */
    public static final class Data extends PosGuiData {

        private final int side;
        private final PartAdvancedIOBus part;
        private final int amountSlot;

        Data(EntityPlayer player, int x, int y, int z, int side, int amountSlot) {
            super(player, x, y, z);
            this.side = side;
            this.amountSlot = amountSlot >= 0 && amountSlot < PartAdvancedIOBus.CONFIG_SLOTS ? amountSlot : -1;
            this.part = resolve();
        }

        private PartAdvancedIOBus resolve() {
            if (side < 0 || side >= 6 || !getWorld().blockExists(getX(), getY(), getZ())) return null;
            if (getTileEntity() instanceof IPartHost host
                && host.getPart(ForgeDirection.getOrientation(side)) instanceof PartAdvancedIOBus bus) return bus;
            return null;
        }
    }
}
