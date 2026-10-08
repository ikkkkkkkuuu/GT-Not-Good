package com.xyp.gtnotgood.common.blocks.largeinterface;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.glodblock.github.common.block.FCBaseBlock;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.client.largeinterface.LargeInterfaceRenderer;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.util.AEColor;
import appeng.api.util.IOrientable;
import appeng.core.features.AEFeature;
import appeng.tile.misc.TileInterface;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Native AE block interactions with a dedicated large-pattern host and orange default appearance. */
public final class BlockLargeInterface extends FCBaseBlock {

    @SideOnly(Side.CLIENT)
    private IIcon[][] interfaceIcons;

    // #tr tile.large_interface.name
    // # ME Large Dual Interface
    // # zh_CN ME 大容量二合一接口
    public BlockLargeInterface() {
        super(Material.iron, "large_interface");
        setBlockTextureName(ModList.AE2FluidCraft.getResourcePath("interface/fluid_interface_Orange"));
        setFullBlock(true);
        setOpaque(true);
        setTileEntity(TileLargeInterface.class);
        setFeature(EnumSet.of(AEFeature.Core));
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected LargeInterfaceRenderer getRenderer() {
        return new LargeInterfaceRenderer();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        interfaceIcons = new IIcon[3][AEColor.VALUES.length];
        String[] paths = { "fluid_interface", "fluid_interface_a", "fluid_interface_arrow" };
        for (AEColor color : AEColor.VALID_COLORS) {
            for (int i = 0; i < paths.length; i++) {
                interfaceIcons[i][color.ordinal()] = register
                    .registerIcon(ModList.AE2FluidCraft.getResourcePath("interface/" + paths[i] + "_" + color.name()));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public IIcon getInterfaceIcon(int texture, AEColor color) {
        return interfaceIcons[texture][(color == AEColor.Transparent ? AEColor.Orange : color).ordinal()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        TileLargeInterface tile = getTileEntity(world, x, y, z);
        return tile != null && tile.getForward() == ForgeDirection.UNKNOWN ? getInterfaceIcon(0, tile.getColor())
            : super.getIcon(world, x, y, z, side);
    }

    @Override
    public boolean onActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY,
        float hitZ) {
        if (player.isSneaking()) return false;
        TileLargeInterface tile = getTileEntity(world, x, y, z);
        if (tile == null) return false;
        if (!world.isRemote) LargeInterfaceGuiFactory.INSTANCE.open(player, tile);
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        TileLargeInterface tile = getTileEntity(world, x, y, z);
        if (tile != null) tile.getInterfaceDuality().updateRedstoneState();
    }

    @Override
    protected boolean hasCustomRotation() {
        return true;
    }

    @Override
    protected void customRotateBlock(IOrientable tile, ForgeDirection axis) {
        if (tile instanceof TileInterface iface) iface.setSide(axis);
    }

    @Override
    public BlockLargeInterface register() {
        GameRegistry.registerBlock(this, ItemBlockLargeInterface.class, "large_interface");
        GameRegistry.registerTileEntity(TileLargeInterface.class, ModList.GTNotGood.getResourcePath("large_interface"));
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        addInterfaceInformation(lines);
    }

    public static void addInterfaceInformation(List<String> lines) {
        // #tr tooltip.large_interface.patterns
        // # 900 pattern slots; scroll through 9 columns
        // # zh_CN 900 个样板槽，9 列滚动显示
        lines.add(StatCollector.translateToLocal("tooltip.large_interface.patterns"));
        // #tr tooltip.large_interface.return
        // # Returns items and fluids to ME; no stocking configuration
        // # zh_CN 物品与流体产物直接回 ME，不支持库存标记拉取
        lines.add(StatCollector.translateToLocal("tooltip.large_interface.return"));
    }
}
