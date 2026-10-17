package com.xyp.gtnotgood.common.blocks.webscreen;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.client.webscreen.ClientWebScreens;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class BlockWebScreen extends Block {

    private static final int[] PLACEMENT_FACING = { 2, 5, 3, 4 };
    @SideOnly(Side.CLIENT)
    private IIcon front;
    @SideOnly(Side.CLIENT)
    private IIcon back;

    public BlockWebScreen() {
        super(Material.iron);
        // #tr tile.web_screen.name
        // # Web Display
        // # zh_CN 网页显示屏
        setBlockName("web_screen");
        setHardness(3.5F);
        setResistance(10);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileWebScreen();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int direction = MathHelper.floor_double(placer.rotationYaw * 4 / 360 + 0.5) & 3;
        world.setBlockMetadataWithNotify(x, y, z, PLACEMENT_FACING[direction], 2);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (player instanceof FakePlayer) return false;
        if (world.isRemote && world.getTileEntity(x, y, z) instanceof TileWebScreen tile) {
            interact(tile, player.isSneaking());
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    private void interact(TileWebScreen tile, boolean stop) {
        if (stop) ClientWebScreens.stop(tile);
        else ClientWebScreens.open(tile);
    }

    @Override
    public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
        if (world.isRemote || axis.ordinal() < 2 || axis.ordinal() > 5) return false;
        world.setBlockMetadataWithNotify(x, y, z, axis.ordinal(), 3);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(ModList.GTNotGood.getResourcePath("web_screen_side"));
        front = register.registerIcon(ModList.GTNotGood.getResourcePath("web_screen_front"));
        back = register.registerIcon(ModList.GTNotGood.getResourcePath("web_screen_back"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int facing = metadata >= 2 && metadata <= 5 ? metadata : 3;
        return side == facing ? front : side == (facing ^ 1) ? back : blockIcon;
    }
}
