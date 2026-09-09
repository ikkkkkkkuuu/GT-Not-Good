package com.xyp.gtnotgood.common.network;

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

/** Three physical network blocks sharing placement, topology invalidation and controller-data preservation. */
public class BlockNetwork extends Block {

    public static final int CONTROLLER = 0;
    public static final int PIPE = 1;
    public static final int CONNECTOR = 2;
    public static int cableRenderId = -1;
    public final int kind;
    @SideOnly(Side.CLIENT)
    private IIcon front;

    public BlockNetwork(int kind, String name) {
        super(Material.iron);
        this.kind = kind;
        setBlockName(name);
        setHardness(2.0F);
        setResistance(10.0F);
        setLightOpacity(kind == CONTROLLER ? 255 : 0);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        if (kind != CONTROLLER) setBlockBounds(0.3125F, 0.3125F, 0.3125F, 0.6875F, 0.6875F, 0.6875F);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return kind == CONTROLLER ? new TileNetworkController() : new TileNetworkNode();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (kind == PIPE || player.isSneaking()) return false;
        if (!world.isRemote) TileEntityGuiFactory.INSTANCE.open(player, x, y, z);
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        NetworkTopology.changed(world);
        world.markBlockForUpdate(x, y, z);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        NetworkTopology.changed(world);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        NetworkTopology.changed(world);
        super.breakBlock(world, x, y, z, block, metadata);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(world, player, x, y, z, false);
    }

    /** Keep the tile available until Forge has collected the controller's NBT-bearing drop. */
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
        ArrayList<ItemStack> drops = new ArrayList<>();
        ItemStack stack = new ItemStack(this);
        if (world.getTileEntity(x, y, z) instanceof TileNetworkNode node && kind != PIPE) {
            NBTTagCompound data = new NBTTagCompound();
            node.writeToNBT(data);
            stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound()
                .setTag("networkData", data);
        }
        drops.add(stack);
        return drops;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
        if (!world.isRemote && kind == CONTROLLER) {
            int quadrant = net.minecraft.util.MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
            int facing = new int[] { 2, 5, 3, 4 }[quadrant];
            world.setBlockMetadataWithNotify(x, y, z, facing, 2);
        }
        if (!world.isRemote && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey("networkData")
            && world.getTileEntity(x, y, z) instanceof TileNetworkNode node) {
            NBTTagCompound data = (NBTTagCompound) stack.getTagCompound()
                .getCompoundTag("networkData")
                .copy();
            data.setInteger("x", x);
            data.setInteger("y", y);
            data.setInteger("z", z);
            node.readFromNBT(data);
            node.markDirty();
            NetworkTopology.changed(world);
        }
    }

    public boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
        int nx = x + direction.offsetX, ny = y + direction.offsetY, nz = z + direction.offsetZ;
        if (ny < 0 || ny >= 256) return false;
        if (world instanceof World actual && !actual.blockExists(nx, ny, nz)) return false;
        if (world.getBlock(nx, ny, nz) instanceof BlockNetwork) return true;
        TileEntity target = world.getTileEntity(nx, ny, nz);
        return kind == CONNECTOR && (target instanceof net.minecraft.inventory.IInventory
            || target instanceof net.minecraftforge.fluids.IFluidHandler);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        if (kind == CONTROLLER) {
            setBlockBounds(0, 0, 0, 1, 1, 1);
            return;
        }
        float low = kind == CONNECTOR ? 0.25F : 0.3125F;
        float high = 1 - low;
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
        return kind == CONTROLLER;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return kind == CONTROLLER;
    }

    @Override
    public int getRenderType() {
        return kind == CONTROLLER ? 0 : cableRenderId;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(
            ModList.GTNotGood.getResourcePath(
                "network/" + (kind == CONTROLLER ? "machine_side" : kind == PIPE ? "netcable" : "connector_side")));
        if (kind == CONTROLLER)
            front = register.registerIcon(ModList.GTNotGood.getResourcePath("network/machine_controller"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int facing = metadata >= 2 && metadata <= 5 ? metadata : 3;
        return kind == CONTROLLER && side == facing ? front : blockIcon;
    }
}
