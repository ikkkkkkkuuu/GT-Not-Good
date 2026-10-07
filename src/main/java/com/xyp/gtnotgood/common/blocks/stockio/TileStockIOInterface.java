package com.xyp.gtnotgood.common.blocks.stockio;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.common.blocks.mebridge.TileMEBridgeBase;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.PixelFontModularScreen;

import appeng.api.networking.GridFlags;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.me.helpers.AENetworkProxy;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiThemes;

/** Full-block host for the same virtual stock transaction used by the cable-mounted interface. */
public final class TileStockIOInterface extends TileMEBridgeBase implements IGuiHolder<PosGuiData> {

    private ForgeDirection targetSide = ForgeDirection.NORTH;
    private final BaseActionSource actionSource = new MachineSource(this);
    public final StockIOLogic logic = new StockIOLogic(new StockIOLogic.Host() {

        @Override
        public boolean isServerSide() {
            return TileStockIOInterface.this.isServerSide();
        }

        @Override
        public AENetworkProxy getProxy() {
            return TileStockIOInterface.this.getProxy();
        }

        @Override
        public BaseActionSource getActionSource() {
            return actionSource;
        }

        @Override
        public TileEntity getTarget() {
            return TileStockIOInterface.this.getTarget();
        }

        @Override
        public ForgeDirection getTargetSide() {
            return targetSide;
        }

        @Override
        public void setTargetSide(ForgeDirection side) {
            targetSide = side;
        }

        @Override
        public void markDirty() {
            TileStockIOInterface.this.markDirty();
        }

        @Override
        public long getTimer() {
            return worldObj == null ? 0 : worldObj.getTotalWorldTime();
        }
    });

    public StockIOLogic getLogic() {
        return logic;
    }

    public ForgeDirection getTargetSide() {
        return targetSide;
    }

    public void setTargetSide(ForgeDirection side) {
        logic.setTargetSide(side);
    }

    public TileEntity getTarget() {
        if (worldObj == null) return null;
        int x = xCoord + targetSide.offsetX;
        int y = yCoord + targetSide.offsetY;
        int z = zCoord + targetSide.offsetZ;
        return worldObj.blockExists(x, y, z) ? worldObj.getTileEntity(x, y, z) : null;
    }

    @Override
    protected ItemStack getVisualRepresentation() {
        return GTNGItemList.StockIOInterface.get(1);
    }

    @Override
    public AENetworkProxy getProxy() {
        boolean creating = gridProxy == null;
        AENetworkProxy proxy = super.getProxy();
        if (creating) {
            proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            proxy.setIdlePowerUsage(1);
        }
        return proxy;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        logic.tick();
    }

    @Override
    public void securityBreak() {
        if (isServerSide() && !isInvalid()) worldObj.func_147480_a(xCoord, yCoord, zCoord, true);
    }

    public void writeContents(NBTTagCompound tag) {
        logic.writeContents(tag);
    }

    public void readContents(NBTTagCompound tag) {
        logic.readContents(tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeContents(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readContents(tag);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        settings.useTheme(GTGuiThemes.STANDARD.getId());
        return StockIOGui.build(logic, sync);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new PixelFontModularScreen(ModList.ModIds.GT_NOT_GOOD, mainPanel);
    }
}
