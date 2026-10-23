package com.xyp.gtnotgood.common.blocks.heart;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
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
import gregtech.api.interfaces.IHasIndexedTexture;

public final class BlockHeartCasing extends Block implements IHasIndexedTexture {

    private final boolean glass;
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;
    @SideOnly(Side.CLIENT)
    private IIcon magnetSide;

    public BlockHeartCasing(boolean glass) {
        super(glass ? Material.glass : Material.iron);
        this.glass = glass;
        setBlockName(glass ? "heart_glass" : "heart_casing");
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(glass ? soundTypeGlass : soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        if (glass) setLightOpacity(0);
        else setHarvestLevel("wrench", 2);
        int metadataMask = 0;
        for (HeartCasing variant : HeartCasing.values()) {
            if (variant.isGlass() == glass) metadataMask |= 1 << variant.getMetadata();
        }
        GregTechAPI.registerMachineBlock(this, metadataMask);
    }

    public HeartCasing getVariant(int metadata) {
        HeartCasing variant = HeartCasing.fromMetadata(metadata);
        return variant != null && variant.isGlass() == glass ? variant : null;
    }

    public HeartCasing getDisplayVariant(int metadata) {
        HeartCasing variant = getVariant(metadata);
        return variant != null ? variant
            : glass ? HeartCasing.RheniumReinforcedEnergyGlass : HeartCasing.GravitonFieldConstraintCasing;
    }

    @Override
    public int getTextureIndex(int metadata) {
        HeartCasing variant = getVariant(metadata);
        return variant == null ? Textures.BlockIcons.ERROR_TEXTURE_INDEX : variant.getTextureId();
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata;
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
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
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return !glass;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return getVariant(world.getBlockMetadata(x, y, z)) == HeartCasing.AnnihilateCore ? 4 : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (glass && world.getBlock(x, y, z) == this) return false;
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        for (HeartCasing variant : HeartCasing.values()) {
            if (variant.hasNativeCasing()) continue;
            if (variant.isGlass() == glass) stacks.add(variant.getStack(1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[HeartCasing.values().length];
        for (HeartCasing variant : HeartCasing.values()) {
            if (variant.isGlass() != glass) continue;
            icons[variant.ordinal()] = register
                .registerIcon(ModList.GTNotGood.getResourcePath("heart/" + variant.getTextureName()));
            if (variant.isConnected()) HeartConnectedIcons.register(register, variant);
        }
        if (!glass)
            magnetSide = register.registerIcon(ModList.GTNotGood.getResourcePath("heart/dyson_deployment_magnet_side"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        HeartCasing variant = getDisplayVariant(metadata);
        if (variant == HeartCasing.DysonDeploymentMagnet && side > 1) return magnetSide;
        return icons[variant.ordinal()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        HeartCasing variant = getDisplayVariant(world.getBlockMetadata(x, y, z));
        IIcon connected = HeartConnectedIcons.getIcon(variant, HeartConnections.getMask(world, x, y, z, side, variant));
        return connected == null ? getIcon(side, variant.getMetadata()) : connected;
    }
}
