package com.xyp.gtnotgood.common.flux;

import java.util.ArrayList;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** Flux model with a single attachment arm facing the container clicked during placement. */
public final class BlockFluxLogistics extends BlockFluxConnector {

    public BlockFluxLogistics() {
        super(true, "flux_logistics_plug");
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileFluxLogistics();
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        return ForgeDirection.getOrientation(side)
            .getOpposite()
            .ordinal();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileFluxLogistics tile) {
            if (stack.hasTagCompound()) tile.readContents(stack.getTagCompound());
            if (entity instanceof EntityPlayer player) tile.placedBy(player);
        }
    }

    @Override
    public boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return world.getBlockMetadata(x, y, z) % 6 == side.ordinal();
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        ItemStack stack = new ItemStack(this);
        if (world.getTileEntity(x, y, z) instanceof TileFluxLogistics tile) {
            NBTTagCompound contents = new NBTTagCompound();
            tile.writeContents(contents);
            stack.setTagCompound(contents);
        }
        FluxDropData.normalize(stack, true);
        drops.add(stack);
        return drops;
    }
}
