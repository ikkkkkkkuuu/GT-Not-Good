package com.xyp.gtnotgood.common.mecontainer;

import java.util.ArrayList;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.blocks.mebridge.BlockMEBridgeBase;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** ME export container; its dropped block preserves both filters and actual buffered resources. */
public final class BlockMEContainer extends BlockMEBridgeBase {

    @SideOnly(Side.CLIENT)
    private IIcon topIcon;
    @SideOnly(Side.CLIENT)
    private IIcon bottomIcon;

    // #tr tile.me_container.name
    // # ME Container
    // # zh_CN ME 容器
    public BlockMEContainer() {
        super("me_container", "me_container_side");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(ModList.GTNotGood.getResourcePath("me_container_side"));
        topIcon = register.registerIcon(ModList.GTNotGood.getResourcePath("me_container_top"));
        bottomIcon = register.registerIcon(ModList.GTNotGood.getResourcePath("me_container_bottom"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 0 ? bottomIcon : side == 1 ? topIcon : blockIcon;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileMEContainer();
    }

    @Override
    public String[] getTooltipKeys() {
        // #tr tooltip.me_container.info
        // # 36 item + 36 fluid ghost slots. Bidirectional ME access; no local fluid capacity limit.
        // # zh_CN 物品、流体各 36 个虚拟槽；双向接入 ME，流体无本地容量限制。
        return new String[] { "tooltip.me_container.info" };
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        ItemStack stack = new ItemStack(this);
        if (world.getTileEntity(x, y, z) instanceof TileMEContainer tile) {
            stack.setTagCompound(new NBTTagCompound());
            tile.writeContents(stack.getTagCompound());
        }
        drops.add(stack);
        return drops;
    }

    /** Keep the tile alive until the survival harvest has captured its contents. */
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
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (!world.isRemote && stack.hasTagCompound() && world.getTileEntity(x, y, z) instanceof TileMEContainer tile) {
            tile.readContents(stack.getTagCompound());
            tile.markDirty();
        }
    }
}
