package com.xyp.gtnotgood.common.parts.stockio;

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
import com.xyp.gtnotgood.common.blocks.stockio.StockIOGui;
import com.xyp.gtnotgood.common.blocks.stockio.StockIOLogic;
import com.xyp.gtnotgood.common.blocks.stockio.TileStockIOInterface;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.PixelFontModularScreen;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.parts.IPartHost;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiThemes;

/** Binds settings to the original loaded block/part and checks distance plus AE BUILD permission. */
public final class StockIOGuiFactory extends AbstractUIFactory<StockIOGuiFactory.Data> {

    public static final StockIOGuiFactory INSTANCE = new StockIOGuiFactory();

    private StockIOGuiFactory() {
        super(ModList.GTNotGood.getResourcePath("stock_io_interface"));
    }

    public void open(EntityPlayer player, PartStockIOInterface part) {
        TileEntity tile = part.getTile();
        open(
            player,
            new Data(
                player,
                tile.xCoord,
                tile.yCoord,
                tile.zCoord,
                part.getSide()
                    .ordinal()));
    }

    public void open(EntityPlayer player, TileStockIOInterface tile) {
        open(player, new Data(player, tile.xCoord, tile.yCoord, tile.zCoord, -1));
    }

    private void open(EntityPlayer player, Data data) {
        if (player instanceof EntityPlayerMP serverPlayer && !(player instanceof FakePlayer)
            && canInteractWith(player, data)) GuiManager.open(this, data, serverPlayer);
    }

    @Override
    public @Nonnull IGuiHolder<Data> getGuiHolder(Data data) {
        return (guiData, sync, settings) -> {
            settings.useTheme(GTGuiThemes.STANDARD.getId());
            return StockIOGui.build(data.logic(), sync, () -> canInteractWith(sync.getPlayer(), data));
        };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(Data data, ModularPanel panel) {
        return new PixelFontModularScreen(ModList.ModIds.GT_NOT_GOOD, panel);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, Data data) {
        if (!super.canInteractWith(player, data) || data.getSquaredDistance(player) > 64
            || data.source == null
            || data.source != data.resolve()) return false;
        if (player.worldObj.isRemote) return true;
        var node = ((IActionHost) data.source).getActionableNode();
        return node == null || node.getGrid()
            .<ISecurityGrid>getCache(ISecurityGrid.class)
            .hasPermission(player, SecurityPermissions.BUILD);
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
        private final Object source;

        Data(EntityPlayer player, int x, int y, int z, int side) {
            super(player, x, y, z);
            this.side = side;
            this.source = resolve();
        }

        private Object resolve() {
            if (!getWorld().blockExists(getX(), getY(), getZ())) return null;
            TileEntity tile = getTileEntity();
            if (side == -1) return tile instanceof TileStockIOInterface ? tile : null;
            return side >= 0 && side < 6
                && tile instanceof IPartHost host
                && host.getPart(ForgeDirection.getOrientation(side)) instanceof PartStockIOInterface part ? part : null;
        }

        private StockIOLogic logic() {
            return source instanceof TileStockIOInterface tile ? tile.getLogic()
                : ((PartStockIOInterface) source).getLogic();
        }
    }
}
