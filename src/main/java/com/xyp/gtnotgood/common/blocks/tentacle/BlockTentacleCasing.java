package com.xyp.gtnotgood.common.blocks.tentacle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.xyp.gtnotgood.common.blocks.tentacle.TentacleCasing.Group;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IBlockWithActiveOffset;
import gregtech.api.interfaces.IBlockWithTextures;
import gregtech.api.interfaces.IHasIndexedTexture;
import gregtech.api.interfaces.ITexture;
import gregtech.common.render.GTRendererBlock;

public final class BlockTentacleCasing extends Block
    implements IHasIndexedTexture, IBlockWithTextures, IBlockWithActiveOffset {

    private final Group group;
    @SideOnly(Side.CLIENT)
    private Map<String, IIcon> icons;
    @SideOnly(Side.CLIENT)
    private ITexture[][][] renderTextures;

    public BlockTentacleCasing(Group group) {
        super(group == Group.Glass ? Material.glass : Material.iron);
        this.group = group;
        setBlockName(getRegistryName());
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(isGlass() ? soundTypeGlass : soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        if (isGlass()) setLightOpacity(0);
        else setHarvestLevel("wrench", 2);
        int metadataMask = 0;
        for (TentacleCasing variant : TentacleCasing.values()) {
            if (variant.getGroup() == group) {
                metadataMask |= 1 << variant.getMetadata();
                if (variant.hasActiveAppearance()) metadataMask |= 1 << variant.getActiveMetadata();
            }
        }
        GregTechAPI.registerMachineBlock(this, metadataMask);
    }

    public String getRegistryName() {
        return switch (group) {
            case Casing -> "tentacle_casing";
            case Casing2 -> "tentacle_casing_2";
            case Glass -> "tentacle_glass";
        };
    }

    public boolean isGlass() {
        return group == Group.Glass;
    }

    public TentacleCasing getVariant(int metadata) {
        return TentacleCasing.fromMetadata(group, metadata);
    }

    public TentacleCasing getDisplayVariant(int metadata) {
        TentacleCasing variant = getVariant(metadata);
        return variant != null ? variant : switch (group) {
            case Casing -> TentacleCasing.SpacetimeAssemblyLineCasing;
            case Casing2 -> TentacleCasing.MolecularCasing;
            case Glass -> TentacleCasing.LaserCasing;
        };
    }

    @Override
    public int getTextureIndex(int metadata) {
        TentacleCasing variant = getVariant(metadata);
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
    public int getActiveOffset() {
        return group == Group.Casing2 ? TentacleCasing.ACTIVE_BIT : 0;
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
        return !isGlass();
    }

    @Override
    public boolean renderAsNormalBlock() {
        return !isGlass();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return isGlass() ? 1 : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return GTRendererBlock.RENDER_ID;
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return !isGlass();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (isGlass() && world.getBlock(x, y, z) == this) return false;
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        for (TentacleCasing variant : TentacleCasing.values()) {
            if (variant.hasNativeCasing()) continue;
            if (variant.getGroup() == group) stacks.add(variant.getStack(1));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new HashMap<>();
        renderTextures = new ITexture[16][][];
        Set<String> registered = new HashSet<>();
        for (TentacleCasing variant : TentacleCasing.values()) {
            if (variant.getGroup() != group) continue;
            for (int side = 0; side < 6; side++) {
                String name = variant.getTextureName(side);
                if (registered.add(name)) {
                    icons.put(name, TentacleConnectedIcons.registerIcon(register, name));
                    if (variant.isConnected(side)) TentacleConnectedIcons.register(register, name);
                }
            }
            if (variant.hasGlow()) {
                String active = variant.getTextureName(2, true);
                if (registered.add(active)) icons.put(active, TentacleConnectedIcons.registerIcon(register, active));
                String glow = variant.getGlowTextureName();
                TentacleConnectedIcons.registerGlow(register, glow, TentacleCasing.isConnectedTexture(glow));
                ITexture[] textures = { new TentacleConnectedTexture(variant, true), new TentacleGlowTexture(variant) };
                renderTextures[variant.getActiveMetadata()] = faces(textures);
            }
            renderTextures[variant.getMetadata()] = faces(variant.hasGlow() && !variant.hasActiveAppearance()
                ? new ITexture[] { TentacleBlocks.getTexture(variant), new TentacleGlowTexture(variant) }
                : new ITexture[] { TentacleBlocks.getTexture(variant) });
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        TentacleCasing variant = getDisplayVariant(metadata);
        return icons.get(
            variant.getTextureName(side, variant.hasActiveAppearance() && (metadata & TentacleCasing.ACTIVE_BIT) != 0));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        TentacleCasing variant = getDisplayVariant(world.getBlockMetadata(x, y, z));
        boolean active = variant.hasActiveAppearance()
            && (world.getBlockMetadata(x, y, z) & TentacleCasing.ACTIVE_BIT) != 0;
        IIcon connected = TentacleConnectedIcons.getIcon(variant.getTextureName(side, active),
            TentacleConnections.getMask(world, x, y, z, side, variant));
        return connected == null ? getIcon(side, world.getBlockMetadata(x, y, z)) : connected;
    }

    @SideOnly(Side.CLIENT)
    private static ITexture[][] faces(ITexture[] texture) {
        return new ITexture[][] { texture, texture, texture, texture, texture, texture };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ITexture[][] getTextures(int metadata) {
        if (renderTextures == null) return null;
        return metadata >= 0 && metadata < 16 && renderTextures[metadata] != null ? renderTextures[metadata]
            : renderTextures[getDisplayVariant(metadata).getMetadata()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ITexture[][] getInventoryTextures(int metadata) {
        return getTextures(getDisplayVariant(metadata).getMetadata());
    }

    public void setActive(World world, int x, int y, int z, boolean active) {
        if (world.isRemote || world.getBlock(x, y, z) != this) return;
        int metadata = world.getBlockMetadata(x, y, z);
        TentacleCasing variant = getVariant(metadata);
        if (variant == null || !variant.hasActiveAppearance()) return;
        int next = active ? variant.getActiveMetadata() : variant.getMetadata();
        if (next != metadata) world.setBlockMetadataWithNotify(x, y, z, next, 2);
    }
}
