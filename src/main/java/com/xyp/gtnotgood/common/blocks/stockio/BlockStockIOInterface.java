package com.xyp.gtnotgood.common.blocks.stockio;

import java.util.ArrayList;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.mebridge.BlockMEBridgeBase;
import com.xyp.gtnotgood.common.parts.stockio.StockIOGuiFactory;
import com.xyp.gtnotgood.utils.enums.ModList;

/** Full-block ME interface shape with a selectable machine face and portable configuration/refunds. */
public final class BlockStockIOInterface extends BlockMEBridgeBase {

    // #tr tile.stock_io_interface.name
    // # ME Stock IO Interface
    // # zh_CN 库存 IO 接口 (ME)
    public BlockStockIOInterface() {
        super("stock_io_interface", "advancedio/advanced_io_bus_back");
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(ModList.AE2.getResourcePath("interface/BlockInterface_Purple"));
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileStockIOInterface();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!(world.getTileEntity(x, y, z) instanceof TileStockIOInterface tile)) return false;
        if (!world.isRemote) StockIOGuiFactory.INSTANCE.open(player, tile);
        return true;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        return ForgeDirection.getOrientation(side)
            .getOpposite()
            .ordinal();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileStockIOInterface tile) {
            if (stack.hasTagCompound()) tile.getLogic()
                .readContents(stack.getTagCompound());
            tile.getLogic()
                .setTargetSide(ForgeDirection.getOrientation(world.getBlockMetadata(x, y, z)));
            tile.markDirty();
        }
    }

    @Override
    public String[] getTooltipKeys() {
        return new String[] { "tooltip.stock_io_interface.direct", "tooltip.stock_io_interface.slots" };
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        ItemStack portable = new ItemStack(this);
        if (world.getTileEntity(x, y, z) instanceof TileStockIOInterface tile) {
            NBTTagCompound tag = new NBTTagCompound();
            tile.getLogic()
                .writeContents(tag);
            portable.setTagCompound(tag);
        }
        drops.add(portable);
        return drops;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        super.harvestBlock(world, player, x, y, z, metadata);
        world.setBlockToAir(x, y, z);
    }
}
