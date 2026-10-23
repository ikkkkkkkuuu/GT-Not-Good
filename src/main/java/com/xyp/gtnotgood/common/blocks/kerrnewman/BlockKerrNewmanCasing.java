package com.xyp.gtnotgood.common.blocks.kerrnewman;

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
import com.xyp.gtnotgood.client.render.KerrNewmanConnectedIcons;
import com.xyp.gtnotgood.client.render.KerrNewmanConnections;
import com.xyp.gtnotgood.client.render.KerrNewmanGlowTexture;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IBlockWithActiveOffset;
import gregtech.api.interfaces.IBlockWithTextures;
import gregtech.api.interfaces.IHasIndexedTexture;
import gregtech.api.interfaces.ITexture;
import gregtech.common.render.GTRendererBlock;

public final class BlockKerrNewmanCasing extends Block
    implements IBlockWithTextures, IHasIndexedTexture, IBlockWithActiveOffset {

    private final KerrNewmanCasing.Group group;
    private final boolean glass;
    @SideOnly(Side.CLIENT)
    private ITexture[][][] textures;

    public BlockKerrNewmanCasing(KerrNewmanCasing.Group group, String name) {
        super(group == KerrNewmanCasing.Group.Glass ? Material.glass : Material.iron);
        this.group = group;
        glass = group == KerrNewmanCasing.Group.Glass;
        setBlockName(name);
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(glass ? soundTypeGlass : soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        if (glass) setLightOpacity(0);
        else setHarvestLevel("wrench", 2);
        int metadataMask = 0;
        for (KerrNewmanCasing variant : KerrNewmanCasing.values()) {
            if (variant.getGroup() != group) continue;
            metadataMask |= 1 << variant.getMetadata();
            if (variant.hasActiveAppearance()) metadataMask |= 1 << (variant.getMetadata() | getActiveOffset());
        }
        GregTechAPI.registerMachineBlock(this, metadataMask);
    }

    public KerrNewmanCasing getVariant(int metadata) {
        return KerrNewmanCasing.fromMetadata(group, metadata);
    }

    public KerrNewmanCasing getDisplayVariant(int metadata) {
        KerrNewmanCasing variant = getVariant(metadata);
        if (variant != null) return variant;
        return switch (group) {
            case Casing -> KerrNewmanCasing.EnhancedHyperCasing;
            case Casing2 -> KerrNewmanCasing.AtomicCasing;
            case Glass -> KerrNewmanCasing.IndustrialFramelessGlass;
        };
    }

    @Override
    public int getActiveOffset() {
        return KerrNewmanCasing.ACTIVE_BIT;
    }

    @Override
    public int getTextureIndex(int metadata) {
        KerrNewmanCasing variant = getVariant(metadata);
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
        if (glass && world.getBlock(x, y, z) == this) {
            ForgeDirection face = ForgeDirection.getOrientation(side);
            int ownX = x - face.offsetX;
            int ownY = y - face.offsetY;
            int ownZ = z - face.offsetZ;
            if (
                world.getBlock(ownX, ownY, ownZ) == this
                    && world.getBlockMetadata(x, y, z) == world.getBlockMetadata(ownX, ownY, ownZ)
            ) return false;
        }
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        for (KerrNewmanCasing variant : KerrNewmanCasing.values()) {
            if (variant.hasNativeCasing()) continue;
            if (variant.getGroup() == group) stacks.add(variant.getStack(1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        KerrNewmanConnectedIcons.register(register, group);
        textures = new ITexture[16][][];
        for (KerrNewmanCasing variant : KerrNewmanCasing.values()) {
            if (variant.getGroup() != group) continue;
            ITexture[] base = { KerrNewmanBlocks.getTexture(variant) };
            textures[variant.getMetadata()] = faces(base);
            if (variant.hasActiveAppearance()) {
                ITexture[] active = { KerrNewmanBlocks.getTexture(variant), new KerrNewmanGlowTexture(variant) };
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
        return KerrNewmanConnectedIcons.getIcon(getDisplayVariant(metadata), 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        KerrNewmanCasing variant = getDisplayVariant(world.getBlockMetadata(x, y, z));
        return KerrNewmanConnectedIcons.getIcon(variant, KerrNewmanConnections.getMask(world, x, y, z, side, variant));
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

    public void setActive(World world, int x, int y, int z, boolean active) {
        if (world.isRemote || world.getBlock(x, y, z) != this) return;
        int metadata = world.getBlockMetadata(x, y, z);
        KerrNewmanCasing variant = getVariant(metadata);
        if (variant == null || !variant.hasActiveAppearance()) return;
        int next = variant.getMetadata() | (active ? getActiveOffset() : 0);
        if (next != metadata) world.setBlockMetadataWithNotify(x, y, z, next, 2);
    }
}
