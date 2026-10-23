package com.xyp.gtnotgood.common.blocks.particlecollider;

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

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.client.particlecollider.ParticleColliderConnectedIcons;
import com.xyp.gtnotgood.client.particlecollider.ParticleColliderGlowTexture;
import com.xyp.gtnotgood.client.render.ElectromagneticRotorRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IBlockWithActiveOffset;
import gregtech.api.interfaces.IBlockWithTextures;
import gregtech.api.interfaces.IHasIndexedTexture;
import gregtech.api.interfaces.ITexture;
import gregtech.common.render.GTRendererBlock;

public final class BlockParticleColliderCasing extends Block
    implements IBlockWithTextures, IHasIndexedTexture, IBlockWithActiveOffset {

    @SideOnly(Side.CLIENT)
    private ITexture[][][] textures;

    public BlockParticleColliderCasing() {
        super(Material.iron);
        setBlockName("particle_collider_casing");
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        setHarvestLevel("wrench", 2);
        GregTechAPI.registerMachineBlock(this, (1 << 0) | (1 << 1) | (1 << 2) | (1 << 10));
    }

    public ParticleColliderCasing getDisplayVariant(int metadata) {
        ParticleColliderCasing variant = ParticleColliderCasing.fromMetadata(metadata);
        return variant == null ? ParticleColliderCasing.Lafium : variant;
    }

    @Override
    public int getActiveOffset() {
        return ParticleColliderCasing.ACTIVE_BIT;
    }

    @Override
    public int getTextureIndex(int metadata) {
        ParticleColliderCasing variant = ParticleColliderCasing.fromMetadata(metadata);
        return variant == null ? Textures.BlockIcons.ERROR_TEXTURE_INDEX : variant.getTextureId();
    }

    @Override
    public int damageDropped(int metadata) {
        return getDisplayVariant(metadata).getMetadata();
    }

    @Override
    public int quantityDropped(int metadata, int fortune, Random random) {
        return ParticleColliderCasing.fromMetadata(metadata) == null ? 0 : 1;
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
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return !ElectromagneticRotorRenderer.hidesFace(world, x, y, z, side)
            && super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        for (ParticleColliderCasing variant : ParticleColliderCasing.values()) {
            if (!variant.hasNativeCasing()) stacks.add(variant.getStack(1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        ParticleColliderConnectedIcons.register(register);
        textures = new ITexture[16][][];
        for (ParticleColliderCasing variant : ParticleColliderCasing.values()) {
            ITexture[] base = { ParticleColliderBlocks.getTexture(variant) };
            textures[variant.getMetadata()] = new ITexture[][] { base, base, base, base, base, base };
        }
        ITexture[] active = { ParticleColliderBlocks.getTexture(ParticleColliderCasing.SuperconductingCoil),
            new ParticleColliderGlowTexture() };
        textures[ParticleColliderCasing.SuperconductingCoil.getMetadata()
            | ParticleColliderCasing.ACTIVE_BIT] = new ITexture[][] { active, active, active, active, active, active };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return ParticleColliderConnectedIcons.getBaseIcon(getDisplayVariant(metadata));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ITexture[][] getTextures(int metadata) {
        if (textures == null) return null;
        return metadata >= 0 && metadata < textures.length && textures[metadata] != null ? textures[metadata]
            : textures[ParticleColliderCasing.Lafium.getMetadata()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ITexture[][] getInventoryTextures(int metadata) {
        return getTextures(getDisplayVariant(metadata).getMetadata());
    }
}
