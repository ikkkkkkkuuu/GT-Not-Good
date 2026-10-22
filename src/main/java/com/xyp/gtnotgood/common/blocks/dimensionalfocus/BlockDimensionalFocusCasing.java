package com.xyp.gtnotgood.common.blocks.dimensionalfocus;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IBlockWithActiveOffset;
import gregtech.api.interfaces.IHasIndexedTexture;

public final class BlockDimensionalFocusCasing extends Block implements IHasIndexedTexture, IBlockWithActiveOffset {

    private final boolean glass;
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;
    @SideOnly(Side.CLIENT)
    private IIcon molecularTop;
    @SideOnly(Side.CLIENT)
    private IIcon superconductorActive;

    public BlockDimensionalFocusCasing(boolean glass) {
        super(glass ? Material.glass : Material.iron);
        this.glass = glass;
        setBlockName(glass ? "dimensional_focus_glass" : "dimensional_focus_casing");
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(glass ? soundTypeGlass : soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        if (glass) setLightOpacity(0);
        else setHarvestLevel("wrench", 2);
        GregTechAPI.registerMachineBlock(this, glass ? 1 << DimensionalFocusCasing.FusionGlass.getMetadata() : 0x103d);
    }

    public DimensionalFocusCasing getVariant(int metadata) {
        DimensionalFocusCasing variant = DimensionalFocusCasing.fromMetadata(metadata);
        return variant != null && variant.isGlass() == glass ? variant : null;
    }

    @Override
    public int getActiveOffset() {
        return 8;
    }

    public DimensionalFocusCasing getDisplayVariant(int metadata) {
        DimensionalFocusCasing variant = getVariant(metadata);
        return variant == null
            ? glass ? DimensionalFocusCasing.FusionGlass : DimensionalFocusCasing.DimensionallyTranscendent
            : variant;
    }

    @Override
    public int getTextureIndex(int metadata) {
        DimensionalFocusCasing variant = getVariant(metadata);
        return variant == null ? Textures.BlockIcons.ERROR_TEXTURE_INDEX : variant.getTextureId();
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata & 7;
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z) & 7;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        if (!world.isRemote) GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        super.breakBlock(world, x, y, z, block, metadata);
        if (!world.isRemote) GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public boolean isOpaqueCube() {
        return !glass;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return !glass;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return glass ? 1 : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getMixedBrightnessForBlock(IBlockAccess world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        return world.getBlock(x, y, z) == this && getVariant(metadata) == DimensionalFocusCasing.ImprovedSuperconductor
            && (metadata & 8) != 0 ? 0xf000f0 : super.getMixedBrightnessForBlock(world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (glass && world.getBlock(x, y, z) == this) return false;
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return !glass;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        for (DimensionalFocusCasing variant : DimensionalFocusCasing.values()) {
            if (variant.isGlass() == glass) stacks.add(variant.getStack(1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[DimensionalFocusCasing.values().length];
        for (DimensionalFocusCasing variant : DimensionalFocusCasing.values()) {
            if (variant.isGlass() != glass) continue;
            icons[variant.ordinal()] = register
                .registerIcon(ModList.GTNotGood.getResourcePath("dimensionalfocus/" + variant.getTextureName()));
            if (variant.isConnected()) DimensionalFocusConnectedIcons.register(register, variant);
        }
        if (!glass) {
            molecularTop = register
                .registerIcon(ModList.GTNotGood.getResourcePath("dimensionalfocus/molecular_coil_top"));
            superconductorActive = register.registerIcon(
                ModList.GTNotGood.getResourcePath("dimensionalfocus/improved_superconductor_coil_active"));
            DimensionalFocusConnectedIcons.registerActive(register);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        DimensionalFocusCasing variant = getDisplayVariant(metadata);
        if (variant == DimensionalFocusCasing.MolecularCoil && side < 2) return molecularTop;
        if (variant == DimensionalFocusCasing.ImprovedSuperconductor && (metadata & 8) != 0)
            return superconductorActive;
        return icons[variant.ordinal()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        int metadata = world.getBlockMetadata(x, y, z);
        DimensionalFocusCasing variant = getDisplayVariant(metadata);
        IIcon icon = DimensionalFocusConnectedIcons.getIcon(variant,
            DimensionalFocusConnections.getMask(world, x, y, z, side, variant), (metadata & 8) != 0);
        return icon == null ? getIcon(side, metadata) : icon;
    }

    public void setActive(World world, int x, int y, int z, boolean active) {
        if (world.isRemote || world.getBlock(x, y, z) != this) return;
        int metadata = world.getBlockMetadata(x, y, z);
        if (getVariant(metadata) != DimensionalFocusCasing.ImprovedSuperconductor) return;
        int next = active ? metadata | 8 : metadata & 7;
        if (next != metadata) world.setBlockMetadataWithNotify(x, y, z, next, 2);
    }
}
