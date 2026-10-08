package com.xyp.gtnotgood.common.blocks.largeinterface;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.PixelFontModularScreen;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.parts.IPartHost;
import appeng.parts.AEBasePart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiThemes;

/** Shares the block/part panel while validating the original loaded host, distance and live AE BUILD permission. */
public final class LargeInterfaceGuiFactory extends AbstractUIFactory<LargeInterfaceGuiFactory.Data> {

    public static final LargeInterfaceGuiFactory INSTANCE = new LargeInterfaceGuiFactory();

    private LargeInterfaceGuiFactory() {
        super(ModList.GTNotGood.getResourcePath("large_me_interface"));
    }

    public void open(EntityPlayer player, LargeInterfaceHost host) {
        TileEntity tile = host.getTileEntity();
        if (tile == null || !(player instanceof EntityPlayerMP serverPlayer) || player instanceof FakePlayer) return;
        int side = host instanceof AEBasePart part ? part.getSide().ordinal() : -1;
        Data data = new Data(player, tile.xCoord, tile.yCoord, tile.zCoord, side);
        if (data.source == host && canInteractWith(player, data)) GuiManager.open(this, data, serverPlayer);
    }

    @Override
    public @Nonnull IGuiHolder<Data> getGuiHolder(Data data) {
        if (data.source == null) {
            throw new IllegalStateException("Large ME interface is not loaded at " + data
                .getX() + "," + data.getY() + "," + data.getZ() + " (side " + data.side + ")");
        }
        return (guiData, sync, settings) -> {
            settings.useTheme(GTGuiThemes.STANDARD.getId());
            return LargeInterfaceGui.build(data.source, sync, () -> canInteractWith(sync.getPlayer(), data));
        };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(Data data, ModularPanel panel) {
        return new PixelFontModularScreen(ModList.ModIds.GT_NOT_GOOD, panel);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, Data data) {
        if (
            !super.canInteractWith(player, data) || data.getSquaredDistance(player) > 64
                || data.source == null
                || data.source != data.resolve()
        ) return false;
        if (player.worldObj.isRemote) return true;
        var node = data.source.getActionableNode();
        return node == null || node.getGrid().<ISecurityGrid>getCache(ISecurityGrid.class).hasPermission(player,
            SecurityPermissions.BUILD);
    }

    @Override
    public void writeGuiData(Data data, PacketBuffer buffer) {
        buffer.writeInt(data.getX());
        buffer.writeInt(data.getY());
        buffer.writeInt(data.getZ());
        buffer.writeByte(data.side);
    }

    @Override
    public @Nonnull Data readGuiData(EntityPlayer player, PacketBuffer buffer) {
        return new Data(player, buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readByte());
    }

    public static final class Data extends PosGuiData {

        private final int side;
        private final LargeInterfaceHost source;

        Data(EntityPlayer player, int x, int y, int z, int side) {
            super(player, x, y, z);
            this.side = side;
            source = resolve();
        }

        private LargeInterfaceHost resolve() {
            if (!getWorld().blockExists(getX(), getY(), getZ())) return null;
            TileEntity tile = getTileEntity();
            if (side == -1) return tile instanceof LargeInterfaceHost host ? host : null;
            return side >= 0 && side < 6
                && tile instanceof IPartHost partHost
                && partHost.getPart(ForgeDirection.getOrientation(side)) instanceof LargeInterfaceHost host ? host
                    : null;
        }
    }
}
