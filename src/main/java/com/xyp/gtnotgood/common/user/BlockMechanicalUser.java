package com.xyp.gtnotgood.common.user;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Six-direction, powerless player-interaction block. The marked face points toward its target. */
public final class BlockMechanicalUser extends Block {

    @SideOnly(Side.CLIENT)
    private IIcon front;
    @SideOnly(Side.CLIENT)
    private IIcon back;

    public BlockMechanicalUser() {
        super(Material.iron);
        // #tr tile.mechanical_user.name
        // # Mechanical User
        // # zh_CN 使用者
        setBlockName("mechanical_user");
        setHardness(3.5F);
        setResistance(10);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileMechanicalUser();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        world.setBlockMetadataWithNotify(x, y, z, BlockPistonBase.determineOrientation(world, x, y, z, placer), 2);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer) return false;
        if (!world.isRemote) TileEntityGuiFactory.INSTANCE.open(player, x, y, z);
        return true;
    }

    @Override
    public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
        if (!world.isRemote && axis != ForgeDirection.UNKNOWN) {
            world.setBlockMetadataWithNotify(x, y, z, axis.ordinal(), 3);
            return true;
        }
        return false;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileMechanicalUser tile) {
            for (int i = 0; i < tile.getSizeInventory(); i++) {
                ItemStack stack = tile.getStackInSlotOnClosing(i);
                if (stack != null) world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack));
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(ModList.GTNotGood.getResourcePath("mechanical_user/interact_side"));
        front = register.registerIcon(ModList.GTNotGood.getResourcePath("mechanical_user/interact_use"));
        back = register.registerIcon(ModList.GTNotGood.getResourcePath("mechanical_user/interact_back"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        int facing = meta >= 0 && meta < 6 ? meta : 1;
        // The inventory metadata is zero; display the marked face on top like the reference item.
        if (meta == 0) facing = 0;
        return side == facing ? front : side == (facing ^ 1) ? back : blockIcon;
    }
}
