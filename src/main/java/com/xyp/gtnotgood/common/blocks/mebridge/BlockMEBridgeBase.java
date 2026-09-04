package com.xyp.gtnotgood.common.blocks.mebridge;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;

/**
 * 跨维度 ME 网桥方块公共基类（普通方块，非 GT 机器）。
 * <p>
 * 右键打开 GUI（发起端写频道名 / 接收端选频道）。方块本身是不透明整方块，接入相邻的 AE 线缆。
 */
public abstract class BlockMEBridgeBase extends Block {

    private final String iconName;

    protected BlockMEBridgeBase(String unlocalizedName, String iconName) {
        super(Material.iron);
        this.iconName = iconName;
        setBlockName(unlocalizedName);
        setHardness(3.0f);
        setResistance(10.0f);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public abstract TileEntity createTileEntity(World world, int metadata);

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof com.cleanroommc.modularui.api.IGuiHolder)) return false;
        // MUI2 opens server-side and pushes the panel packet to the client.
        if (!world.isRemote) {
            com.cleanroommc.modularui.factory.TileEntityGuiFactory.INSTANCE.open(player, x, y, z);
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, net.minecraft.entity.EntityLivingBase placer,
        net.minecraft.item.ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (!world.isRemote && placer instanceof EntityPlayer player) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof com.xyp.gtnotgood.common.mebridge.TileMEBridgeBase bridge) {
                bridge.setOwnerName(player.getCommandSenderName());
            }
        }
    }

    /** 子类返回该方块的 tooltip 语言键列表(按行显示,依次翻译)。由 {@link ItemBlockMEBridge} 读取。 */
    public abstract String[] getTooltipKeys();

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        this.blockIcon = reg.registerIcon(GTNotGood.RESOURCE_ROOT_ID + ":" + iconName);
    }
}
