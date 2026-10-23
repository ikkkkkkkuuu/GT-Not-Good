package com.xyp.gtnotgood.common.blocks.lighthunter;

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
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing.Group;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IBlockWithActiveOffset;
import gregtech.api.interfaces.IBlockWithTextures;
import gregtech.api.interfaces.IHasIndexedTexture;
import gregtech.api.interfaces.ITexture;
import gregtech.common.render.GTRendererBlock;

public final class BlockLightHunterCasing extends Block
    implements IHasIndexedTexture, IBlockWithTextures, IBlockWithActiveOffset {

    private final Group group;
    @SideOnly(Side.CLIENT)
    private Map<String, IIcon> icons;
    @SideOnly(Side.CLIENT)
    private ITexture[][][] renderTextures;

    public BlockLightHunterCasing(Group group) {
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
        for (LightHunterCasing variant : LightHunterCasing.values()) {
            if (variant.getGroup() == group) {
                metadataMask |= 1 << variant.getMetadata();
                if (variant.hasActiveAppearance()) metadataMask |= 1 << variant.getActiveMetadata();
            }
        }
        GregTechAPI.registerMachineBlock(this, metadataMask);
    }

    public String getRegistryName() {
        return switch (group) {
            case Casing -> "light_hunter_casing";
            case Casing2 -> "light_hunter_casing_2";
            case Glass -> "light_hunter_glass";
        };
    }

    public boolean isGlass() {
        return group == Group.Glass;
    }

    public LightHunterCasing getVariant(int metadata) {
        return LightHunterCasing.fromMetadata(group, metadata);
    }

    public LightHunterCasing getDisplayVariant(int metadata) {
        LightHunterCasing variant = getVariant(metadata);
        return variant != null ? variant : switch (group) {
            case Casing -> LightHunterCasing.UhvMachineCasing;
            case Casing2 -> LightHunterCasing.SuperComputationComponent;
            case Glass -> LightHunterCasing.ForceFieldGlass;
        };
    }

    @Override
    public int getTextureIndex(int metadata) {
        LightHunterCasing variant = getVariant(metadata);
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
        return group == Group.Casing2 ? LightHunterCasing.ACTIVE_BIT : 0;
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
        for (LightHunterCasing variant : LightHunterCasing.values()) {
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
        for (LightHunterCasing variant : LightHunterCasing.values()) {
            if (variant.getGroup() != group) continue;
            for (int side = 0; side < 6; side++) {
                String name = variant.getTextureName(side);
                if (registered.add(name)) {
                    icons.put(name, register.registerIcon(ModList.GTNotGood.getResourcePath("lighthunter/" + name)));
                    if (variant.isConnected(side)) LightHunterConnectedIcons.register(register, name);
                }
            }
            if (variant.hasActiveAppearance()) {
                String active = variant.getTextureName(2, true);
                if (
                    registered.add(active)
                ) icons.put(active, register.registerIcon(ModList.GTNotGood.getResourcePath("lighthunter/" + active)));
                String glow = variant.getGlowTextureName();
                LightHunterConnectedIcons.registerGlow(register, glow, LightHunterCasing.isConnectedTexture(glow));
                ITexture[] textures = { new LightHunterConnectedTexture(variant, true),
                    new LightHunterGlowTexture(variant) };
                renderTextures[variant.getActiveMetadata()] = faces(textures);
            }
            renderTextures[variant.getMetadata()] = faces(new ITexture[] { LightHunterBlocks.getTexture(variant) });
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        LightHunterCasing variant = getDisplayVariant(metadata);
        return icons.get(variant.getTextureName(side,
            variant.hasActiveAppearance() && (metadata & LightHunterCasing.ACTIVE_BIT) != 0));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        LightHunterCasing variant = getDisplayVariant(world.getBlockMetadata(x, y, z));
        boolean active = variant.hasActiveAppearance()
            && (world.getBlockMetadata(x, y, z) & LightHunterCasing.ACTIVE_BIT) != 0;
        IIcon connected = LightHunterConnectedIcons.getIcon(variant.getTextureName(side, active),
            LightHunterConnections.getMask(world, x, y, z, side, variant));
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
        LightHunterCasing variant = getVariant(metadata);
        if (variant == null || !variant.hasActiveAppearance()) return;
        int next = active ? variant.getActiveMetadata() : variant.getMetadata();
        if (next != metadata) world.setBlockMetadataWithNotify(x, y, z, next, 2);
    }
}
