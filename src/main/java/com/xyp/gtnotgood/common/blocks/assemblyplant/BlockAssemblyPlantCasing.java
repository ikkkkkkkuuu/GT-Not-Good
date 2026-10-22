package com.xyp.gtnotgood.common.blocks.assemblyplant;

import java.util.List;
import java.util.Random;

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
import com.xyp.gtnotgood.client.assemblyplant.AssemblyPlantConnectedIcons;
import com.xyp.gtnotgood.client.assemblyplant.AssemblyPlantConnectedTexture;
import com.xyp.gtnotgood.client.assemblyplant.AssemblyPlantGlowTexture;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IBlockWithActiveOffset;
import gregtech.api.interfaces.IBlockWithTextures;
import gregtech.api.interfaces.IHasIndexedTexture;
import gregtech.api.interfaces.ITexture;
import gregtech.common.render.GTRendererBlock;

public final class BlockAssemblyPlantCasing extends Block
    implements IBlockWithTextures, IHasIndexedTexture, IBlockWithActiveOffset {

    private final boolean glass;
    @SideOnly(Side.CLIENT)
    private ITexture[][][] textures;

    public BlockAssemblyPlantCasing(boolean glass) {
        super(glass ? Material.glass : Material.iron);
        this.glass = glass;
        setBlockName(glass ? "assembly_plant_glass" : "assembly_plant_casing");
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(glass ? soundTypeGlass : soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        if (glass) setLightOpacity(0);
        else setHarvestLevel("wrench", 2);
        int metadataMask = 0;
        for (AssemblyPlantCasing variant : AssemblyPlantCasing.values()) {
            if (variant.isGlass() != glass) continue;
            metadataMask |= 1 << variant.getMetadata();
            if (variant.hasActiveAppearance()) metadataMask |= 1 << (variant.getMetadata() | getActiveOffset());
        }
        GregTechAPI.registerMachineBlock(this, metadataMask);
    }

    public AssemblyPlantCasing getVariant(int metadata) {
        AssemblyPlantCasing variant = AssemblyPlantCasing.fromMetadata(metadata);
        return variant != null && variant.isGlass() == glass ? variant : null;
    }

    public AssemblyPlantCasing getDisplayVariant(int metadata) {
        AssemblyPlantCasing variant = getVariant(metadata);
        return variant == null
            ? glass ? AssemblyPlantCasing.LaminatedGlass : AssemblyPlantCasing.AdvancedAssemblyLineUnit
            : variant;
    }

    @Override
    public int getActiveOffset() {
        return AssemblyPlantCasing.ACTIVE_BIT;
    }

    @Override
    public int getTextureIndex(int metadata) {
        AssemblyPlantCasing variant = getVariant(metadata);
        return variant == null ? Textures.BlockIcons.ERROR_TEXTURE_INDEX : variant.getTextureId();
    }

    @Override
    public int damageDropped(int metadata) {
        return getDisplayVariant(metadata).getMetadata();
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        return damageDropped(world.getBlockMetadata(x, y, z));
    }

    @Override
    public int quantityDropped(int metadata, int fortune, Random random) {
        return getVariant(metadata) == null ? 0 : 1;
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
    public void breakBlock(World world, int x, int y, int z, Block oldBlock, int metadata) {
        super.breakBlock(world, x, y, z, oldBlock, metadata);
        if (!world.isRemote) GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public boolean isOpaqueCube() {
        return !glass;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return GTRendererBlock.RENDER_ID;
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
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (glass && world.getBlock(x, y, z) == this) return false;
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        for (AssemblyPlantCasing variant : AssemblyPlantCasing.values()) {
            if (variant.isGlass() == glass) stacks.add(variant.getStack(1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        AssemblyPlantConnectedIcons.register(register, glass);
        textures = new ITexture[16][][];
        for (AssemblyPlantCasing variant : AssemblyPlantCasing.values()) {
            if (variant.isGlass() != glass) continue;
            ITexture[] base = { variant.getTexture() };
            textures[variant.getMetadata()] = faces(base);
            if (variant.hasActiveAppearance()) {
                ITexture activeBase = new AssemblyPlantConnectedTexture(variant, true);
                ITexture[] active = variant == AssemblyPlantCasing.ElectrolyticCell ? new ITexture[] { activeBase }
                    : new ITexture[] { activeBase, new AssemblyPlantGlowTexture(variant) };
                textures[variant.getMetadata() | getActiveOffset()] = faces(active);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static ITexture[][] faces(ITexture[] texture) {
        return new ITexture[][] { texture, texture, texture, texture, texture, texture };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return AssemblyPlantConnectedIcons.getIcon(getDisplayVariant(metadata), 0, (metadata & getActiveOffset()) != 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ITexture[][] getTextures(int metadata) {
        if (textures == null) return null;
        return metadata >= 0 && metadata < textures.length && textures[metadata] != null ? textures[metadata]
            : textures[getDisplayVariant(metadata).getMetadata()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ITexture[][] getInventoryTextures(int metadata) {
        return getTextures(getDisplayVariant(metadata).getMetadata());
    }
}
