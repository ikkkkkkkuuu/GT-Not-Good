// Interface cuboids adapted from GTNH AE2 rv3-beta-1073-GTNH, AlgorithmX2, LGPL-3.0-or-later.
package com.xyp.gtnotgood.common.parts.stockio;

import java.util.Collections;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.stockio.StockIOLogic;
import com.xyp.gtnotgood.common.items.stockio.ItemStockIOInterface;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartRenderHelper;
import appeng.api.parts.PartItemStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.parts.PartBasicState;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Cable-mounted recipe source. Its facing selects one adjacent machine; no inputs are prefilled. */
public final class PartStockIOInterface extends PartBasicState implements IGridTickable {

    private final BaseActionSource source = new MachineSource(this);
    private final StockIOLogic logic = new StockIOLogic(new StockIOLogic.Host() {

        public boolean isServerSide() {
            return getTile() != null && getTile().getWorldObj() != null && !getTile().getWorldObj().isRemote;
        }

        public AENetworkProxy getProxy() {
            return PartStockIOInterface.this.getProxy();
        }

        public BaseActionSource getActionSource() {
            return source;
        }

        public TileEntity getTarget() {
            TileEntity tile = getTile();
            ForgeDirection side = getSide();
            if (tile == null || tile.getWorldObj() == null || side == null) return null;
            int x = tile.xCoord + side.offsetX, y = tile.yCoord + side.offsetY, z = tile.zCoord + side.offsetZ;
            return tile.getWorldObj().blockExists(x, y, z) ? tile.getWorldObj().getTileEntity(x, y, z) : null;
        }

        public ForgeDirection getTargetSide() {
            return getSide() == null ? ForgeDirection.NORTH : getSide();
        }

        public void setTargetSide(ForgeDirection side) {}

        public boolean canSelectTargetSide() {
            return false;
        }

        public void markDirty() {
            if (getHost() == null) return;
            getHost().markForSave();
            try {
                getProxy().getTick().alertDevice(getProxy().getNode());
            } catch (GridAccessException ignored) {}
        }

        public long getTimer() {
            return getTile() == null || getTile().getWorldObj() == null ? 0
                : getTile().getWorldObj().getTotalWorldTime();
        }
    });

    public PartStockIOInterface(ItemStack stack) {
        super(stack);
        getProxy().setIdlePowerUsage(1);
        if (stack.hasTagCompound()) logic.readContents(stack.getTagCompound());
    }

    public StockIOLogic getLogic() {
        return logic;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        logic.tick();
        return TickRateModulation.SAME;
    }

    @Override
    public void onNeighborChanged() {
        try {
            getProxy().getTick().alertDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {}
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, Vec3 pos) {
        if (player.isSneaking()) return false;
        if (!player.worldObj.isRemote) StockIOGuiFactory.INSTANCE.open(player, this);
        return true;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        logic.writeContents(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        logic.readContents(tag);
    }

    @Override
    public ItemStack getItemStack(PartItemStack type) {
        ItemStack stack = super.getItemStack(type);
        if (type != PartItemStack.Break && type != PartItemStack.Wrench && type != PartItemStack.Pick) return stack;
        ItemStack portable = stack.copy();
        portable.setTagCompound(type == PartItemStack.Pick ? null : logic.getRemovalContents());
        return portable;
    }

    @Override
    public void securityBreak() {
        if (getHost() == null || getItemStack().stackSize <= 0) return;
        ItemStack portable = getItemStack(PartItemStack.Break);
        getHost().removePart(getSide(), false);
        Platform.spawnDrops(getTile().getWorldObj(), getTile().xCoord, getTile().yCoord, getTile().zCoord,
            Collections.singletonList(portable));
        getItemStack().stackSize = 0;
    }

    @Override
    public int cableConnectionRenderTo() {
        return 4;
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        helper.addBox(2, 2, 14, 14, 14, 16);
        helper.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getBreakingTexture() {
        return getItemStack().getIconIndex();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventory(IPartRenderHelper helper, RenderBlocks renderer) {
        renderBody(helper, renderer, 0, 0, 0, true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(int x, int y, int z, IPartRenderHelper helper, RenderBlocks renderer) {
        setRenderCache(helper.useSimplifiedRendering(x, y, z, this, getRenderCache()));
        renderBody(helper, renderer, x, y, z, false);
    }

    @SideOnly(Side.CLIENT)
    private void renderBody(IPartRenderHelper helper, RenderBlocks renderer, int x, int y, int z, boolean inventory) {
        ItemStockIOInterface item = (ItemStockIOInterface) getItemStack().getItem();
        helper.setTexture(item.sides, item.sides, item.back, getItemStack().getIconIndex(), item.sides, item.sides);
        int[][] boxes = { { 2, 2, 14, 14, 14, 16 }, { 5, 5, 12, 11, 11, 13 }, { 5, 5, 13, 11, 11, 14 } };
        for (int[] box : boxes) {
            helper.setBounds(box[0], box[1], box[2], box[3], box[4], box[5]);
            if (inventory) helper.renderInventoryBox(renderer);
            else helper.renderBlock(x, y, z, renderer);
        }
        if (!inventory) renderLights(x, y, z, helper, renderer);
    }
}
