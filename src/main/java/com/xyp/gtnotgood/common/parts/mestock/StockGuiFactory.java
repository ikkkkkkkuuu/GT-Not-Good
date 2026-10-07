package com.xyp.gtnotgood.common.parts.mestock;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.xyp.gtnotgood.common.blocks.mestock.TileMERequester;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.parts.AEBasePart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Position/face locator with source identity, distance and AE BUILD checks for every interaction.
 * Remote editing additionally requires the original loaded requester to remain on the terminal's live network.
 * Client proxies carry only synced GUI state; they never load remote chunks or create server network nodes.
 */
public final class StockGuiFactory extends AbstractUIFactory<StockGuiFactory.Data> {

    public static final StockGuiFactory instance = new StockGuiFactory();

    private StockGuiFactory() {
        super(ModList.GTNotGood.getResourcePath("me_stock"));
    }

    public void open(EntityPlayer player, AEBasePart part) {
        TileEntity tile = part.getHost()
            .getTile();
        open(
            player,
            new Data(
                player,
                tile.xCoord,
                tile.yCoord,
                tile.zCoord,
                part.getSide()
                    .ordinal(),
                null));
    }

    public void open(EntityPlayer player, TileMERequester tile) {
        open(player, new Data(player, tile.xCoord, tile.yCoord, tile.zCoord, -1, null));
    }

    void openRemote(EntityPlayer player, Data terminal, TileMERequester tile) {
        open(
            player,
            new Data(
                player,
                terminal.getX(),
                terminal.getY(),
                terminal.getZ(),
                terminal.side,
                new int[] { tile.getWorldObj().provider.dimensionId, tile.xCoord, tile.yCoord, tile.zCoord }));
    }

    void openAmount(EntityPlayer player, PartThresholdExportBus bus, int row) {
        if (row < 0 || row >= bus.stockSlots()
            || bus.stockConfig()
                .key(row) == null)
            return;
        TileEntity tile = bus.getHost()
            .getTile();
        open(
            player,
            new Data(
                player,
                tile.xCoord,
                tile.yCoord,
                tile.zCoord,
                bus.getSide()
                    .ordinal(),
                null,
                row));
    }

    private void open(EntityPlayer player, Data data) {
        if (player instanceof EntityPlayerMP serverPlayer && !(player instanceof FakePlayer)
            && canInteractWith(player, data)) GuiManager.open(this, data, serverPlayer);
    }

    @Override
    public @Nonnull IGuiHolder<Data> getGuiHolder(Data data) {
        return (guiData, sync, settings) -> data.amountSlot >= 0 && data.target instanceof PartThresholdExportBus bus
            ? StockGui.amount(bus, data.amountSlot, data, sync)
            : data.target instanceof StockHost host ? StockGui.build(host, data, sync) : StockGui.terminal(data, sync);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(Data data, ModularPanel panel) {
        return new ModularScreen(ModList.GTNotGood.getID(), panel) {

            @Override
            public void onResize(int width, int height) {
                if (panel instanceof StockGui.TerminalPanel terminal) terminal.resizeRows(height);
                super.onResize(width, height);
            }
        };
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, Data data) {
        if (!super.canInteractWith(player, data) || data.getSquaredDistance(player) > 64
            || data.source == null
            || data.source != data.resolveSource()) return false;
        if (player.worldObj.isRemote) return true;
        if (data.amountSlot >= 0
            && (!(data.source instanceof PartThresholdExportBus bus) || data.amountSlot >= bus.stockSlots()
                || data.amountKey == null
                || data.amountKey != bus.stockConfig()
                    .key(data.amountSlot)))
            return false;
        if (!(data.source instanceof IActionHost action) || !permitted(player, action)) return false;
        if (data.remote == null) return data.target == data.source;
        if (!(data.source instanceof PartRequesterTerminal terminal)
            || !(data.target instanceof TileMERequester requester)
            || data.target != data.resolveRemote()) return false;
        try {
            return terminal.getProxy()
                .isActive()
                && requester.getProxy()
                    .isActive()
                && terminal.getProxy()
                    .getGrid()
                    == requester.getProxy()
                        .getGrid()
                && permitted(player, requester);
        } catch (GridAccessException ignored) {
            return false;
        }
    }

    private static boolean permitted(EntityPlayer player, IActionHost host) {
        var node = host.getActionableNode();
        if (node == null) return true; // Allow local setup before a cable/node is ready.
        ISecurityGrid security = node.getGrid()
            .getCache(ISecurityGrid.class);
        return security.hasPermission(player, SecurityPermissions.BUILD);
    }

    @Override
    public void writeGuiData(Data data, PacketBuffer buffer) {
        buffer.writeInt(data.getX());
        buffer.writeInt(data.getY());
        buffer.writeInt(data.getZ());
        buffer.writeByte(data.side);
        buffer.writeByte(data.amountSlot);
        buffer.writeBoolean(data.remote != null);
        if (data.remote != null) for (int value : data.remote) buffer.writeInt(value);
    }

    @Override
    public @Nonnull Data readGuiData(EntityPlayer player, PacketBuffer buffer) {
        int x = buffer.readInt(), y = buffer.readInt(), z = buffer.readInt(), side = buffer.readByte();
        int amountSlot = buffer.readByte();
        int[] remote = null;
        if (buffer.readBoolean())
            remote = new int[] { buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt() };
        return new Data(player, x, y, z, side, remote, amountSlot);
    }

    public static final class Data extends PosGuiData {

        final int side;
        final int amountSlot;
        final int[] remote;
        final Object source;
        final Object target;
        final IAEStack<?> amountKey;

        Data(EntityPlayer player, int x, int y, int z, int side, int[] remote) {
            this(player, x, y, z, side, remote, -1);
        }

        Data(EntityPlayer player, int x, int y, int z, int side, int[] remote, int amountSlot) {
            super(player, x, y, z);
            this.side = side;
            this.amountSlot = amountSlot;
            this.remote = remote;
            source = resolveSource();
            amountKey = amountSlot >= 0 && source instanceof PartThresholdExportBus bus && amountSlot < bus.stockSlots()
                ? bus.stockConfig()
                    .key(amountSlot)
                : null;
            if (remote != null && player.worldObj.isRemote) {
                TileMERequester proxy = new TileMERequester();
                proxy.setWorldObj(player.worldObj);
                target = proxy;
            } else target = remote == null ? source : resolveRemote();
        }

        Object resolveSource() {
            if (!getWorld().blockExists(getX(), getY(), getZ())) return null;
            TileEntity tile = getTileEntity();
            if (side == -1) return tile instanceof TileMERequester ? tile : null;
            if (side < 0 || side >= 6 || !(tile instanceof IPartHost host)) return null;
            IPart part = host.getPart(ForgeDirection.getOrientation(side));
            return part instanceof PartThresholdExportBus || part instanceof PartThresholdLevelEmitter
                || part instanceof PartRequesterTerminal ? part : null;
        }

        Object resolveRemote() {
            if (remote == null) return null;
            World world = DimensionManager.getWorld(remote[0]);
            if (world == null || !world.blockExists(remote[1], remote[2], remote[3])) return null;
            return world.getTileEntity(remote[1], remote[2], remote[3]);
        }

        StockGridCache cache() {
            if (!(source instanceof PartRequesterTerminal terminal) || getWorld().isRemote) return null;
            try {
                return terminal.getProxy()
                    .getGrid()
                    .getCache(StockGridCache.class);
            } catch (GridAccessException ignored) {
                return null;
            }
        }
    }

    boolean canEdit(EntityPlayer player, Data data, TileMERequester tile) {
        if (!canInteractWith(player, data) || tile == null || tile.isInvalid()) return false;
        if (data.source == tile) return true;
        Data remote = new Data(
            player,
            data.getX(),
            data.getY(),
            data.getZ(),
            data.side,
            new int[] { tile.getWorldObj().provider.dimensionId, tile.xCoord, tile.yCoord, tile.zCoord });
        return remote.target == tile && canInteractWith(player, remote);
    }
}
