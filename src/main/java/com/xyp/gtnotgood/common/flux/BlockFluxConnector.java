/* Flux Networks block/model port. Copyright (c) 2018 Ollie Lansdell, MIT. */
package com.xyp.gtnotgood.common.flux;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IEnergyConnected;

/**
 * Forge 1.7.10 adapter for the original Flux plug/point blocks and multipart models.
 * Drops contain settings only; buffered EU is returned once in breakBlock, avoiding creative/drop duplication.
 */
public class BlockFluxConnector extends Block {

    public static int renderId = -1;
    public final boolean plug;
    @SideOnly(Side.CLIENT)
    private IIcon on, off, colour;

    public BlockFluxConnector(boolean plug, String name) {
        super(Material.iron);
        this.plug = plug;
        setBlockName(name);
        setHardness(2.0F);
        setResistance(10.0F);
        setLightOpacity(0);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        float low = plug ? 0.25F : 0.34375F;
        setBlockBounds(low, low, low, 1 - low, 1 - low, 1 - low);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return plug ? new TileFluxPlug() : new TileFluxPoint();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        if (!world.isRemote) TileEntityGuiFactory.INSTANCE.open(player, x, y, z);
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack) {
        if (world.isRemote || !(world.getTileEntity(x, y, z) instanceof TileFluxConnector tile)) return;
        if (stack.hasTagCompound()) tile.readSettings(stack.getTagCompound());
        if (entity instanceof EntityPlayer player) tile.placedBy(player);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        if (world.getTileEntity(x, y, z) instanceof TileFluxConnector tile) tile.returnBufferToNetwork();
        super.breakBlock(world, x, y, z, block, metadata);
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

    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return false;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> result = new ArrayList<>();
        ItemStack item = new ItemStack(this);
        if (world.getTileEntity(x, y, z) instanceof TileFluxConnector tile) {
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeSettings(tag);
            item.setTagCompound(tag);
        }
        FluxDropData.normalize(item, false);
        result.add(item);
        return result;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
        world.markBlockForUpdate(x, y, z);
    }

    public boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return world.getTileEntity(x, y, z) instanceof TileFluxConnector connector
            && connector.hasVisualConnection(side);
    }

    /** Checks physical GT ports without waiting for the server's active energy tick cache. */
    public boolean canConnectTo(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        int nx = x + side.offsetX, ny = y + side.offsetY, nz = z + side.offsetZ;
        if (world instanceof World actual && !actual.blockExists(nx, ny, nz)) return false;
        TileEntity tile = world.getTileEntity(nx, ny, nz);
        if (tile instanceof TileFluxConnector || !(tile instanceof IEnergyConnected target)) return false;
        return plug ? target.outputsEnergyTo(side.getOpposite(), false)
            : target.inputEnergyFrom(side.getOpposite(), false);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        float low = plug ? 0.25F : 0.34375F, high = 1 - low;
        setBlockBounds(
            connects(world, x, y, z, ForgeDirection.WEST) ? 0 : low,
            connects(world, x, y, z, ForgeDirection.DOWN) ? 0 : low,
            connects(world, x, y, z, ForgeDirection.NORTH) ? 0 : low,
            connects(world, x, y, z, ForgeDirection.EAST) ? 1 : high,
            connects(world, x, y, z, ForgeDirection.UP) ? 1 : high,
            connects(world, x, y, z, ForgeDirection.SOUTH) ? 1 : high);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return renderId;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        String prefix = "flux/flux_" + (plug ? "plug" : "point");
        on = register.registerIcon(ModList.GTNotGood.getResourcePath(prefix + "_on"));
        off = register.registerIcon(ModList.GTNotGood.getResourcePath(prefix + "_off"));
        colour = register.registerIcon(ModList.GTNotGood.getResourcePath(prefix + "_colour"));
        blockIcon = on;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return on;
    }

    @SideOnly(Side.CLIENT)
    public IIcon texture(boolean active, boolean tint) {
        return tint ? colour : active ? on : off;
    }
}
