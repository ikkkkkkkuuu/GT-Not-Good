package com.xyp.gtnotgood.common.blocks.stellar;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.client.stellar.StellarConnectedIcons;
import com.xyp.gtnotgood.common.blocks.stellar.StellarBlockVariant.Group;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;

public final class BlockStellar extends Block {

    private final StellarBlockVariant[] variants = new StellarBlockVariant[16];
    private final StellarBlockVariant fallback;
    private final int machineMetadataMask;

    @SideOnly(Side.CLIENT)
    private IIcon[] sideIcons;
    @SideOnly(Side.CLIENT)
    private IIcon[] endIcons;

    public BlockStellar(Group group) {
        super(group == Group.Glass ? Material.glass : Material.iron);
        StellarBlockVariant firstVariant = null;
        for (StellarBlockVariant variant : StellarBlockVariant.values()) {
            if (variant.getGroup() == group) {
                variants[variant.getMetadata()] = variant;
                if (firstVariant == null) {
                    firstVariant = variant;
                }
                if (group.usesWrenchHarvest()) {
                    setHarvestLevel("wrench", 2, variant.getMetadata());
                }
            }
        }
        if (firstVariant == null) {
            throw new IllegalArgumentException("A stellar block must have at least one variant");
        }
        fallback = firstVariant;
        machineMetadataMask = group == Group.Casing2 ? 1 << StellarBlockVariant.EddingtonWallCasing.getMetadata() : 0;
        if (machineMetadataMask != 0) {
            GregTechAPI.registerMachineBlock(this, machineMetadataMask);
        }
        setBlockName(group.getRegistryName());
        setBlockTextureName(ModList.GTNotGood.getResourcePath("stellar/" + fallback.getSideTexture()));
        setHardness(group == Group.TurbineHousing ? 6.0F : 5.0F);
        setResistance(group == Group.TurbineHousing ? 12.0F : 10.0F);
        setStepSound(group == Group.Glass ? soundTypeGlass : soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        if (group == Group.Glass) {
            setLightOpacity(0);
        }
    }

    public StellarBlockVariant getVariant(int metadata) {
        return metadata >= 0 && metadata < variants.length ? variants[metadata] : null;
    }

    public StellarBlockVariant getDisplayVariant(int metadata) {
        StellarBlockVariant variant = getVariant(metadata);
        return variant == null ? fallback : variant;
    }

    public boolean isGlass() {
        return blockMaterial == Material.glass;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        updateMachine(world, x, y, z, world.getBlockMetadata(x, y, z));
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        updateMachine(world, x, y, z, metadata);
        super.breakBlock(world, x, y, z, block, metadata);
    }

    private void updateMachine(World world, int x, int y, int z, int metadata) {
        if (!world.isRemote && (machineMetadataMask & (1 << metadata)) != 0) {
            GregTechAPI.causeMachineUpdate(world, x, y, z);
        }
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata;
    }

    @Override
    public int quantityDropped(int metadata, int fortune, Random random) {
        return getVariant(metadata) == null ? 0 : 1;
    }

    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return getVariant(metadata) != null && super.canSilkHarvest(world, player, x, y, z, metadata);
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != this) {
            return 0;
        }
        StellarBlockVariant variant = getVariant(world.getBlockMetadata(x, y, z));
        return variant == null ? 0 : variant.getLightValue();
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
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (isGlass() && world.getBlock(x, y, z) == this) {
            ForgeDirection direction = ForgeDirection.getOrientation(side);
            if (
                world.getBlockMetadata(x, y, z)
                    == world.getBlockMetadata(x - direction.offsetX, y - direction.offsetY, z - direction.offsetZ)
            ) return false;
        }
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        for (StellarBlockVariant variant : variants) {
            if (variant != null) {
                stacks.add(new ItemStack(item, 1, variant.getMetadata()));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        sideIcons = new IIcon[variants.length];
        endIcons = new IIcon[variants.length];
        for (StellarBlockVariant variant : variants) {
            if (variant != null) {
                int metadata = variant.getMetadata();
                sideIcons[metadata] = register
                    .registerIcon(ModList.GTNotGood.getResourcePath("stellar/" + variant.getSideTexture()));
                endIcons[metadata] = variant.getEndTexture().equals(variant.getSideTexture()) ? sideIcons[metadata]
                    : register.registerIcon(ModList.GTNotGood.getResourcePath("stellar/" + variant.getEndTexture()));
                StellarConnectedIcons.register(register, variant.getSideTexture());
                if (!variant.getEndTexture().equals(variant.getSideTexture())) {
                    StellarConnectedIcons.register(register, variant.getEndTexture());
                }
            }
        }
        blockIcon = sideIcons[fallback.getMetadata()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        int metadata = world.getBlockMetadata(x, y, z);
        StellarBlockVariant variant = getVariant(metadata);
        if (variant != null) {
            String texture = side == 0 || side == 1 ? variant.getEndTexture() : variant.getSideTexture();
            IIcon icon = StellarConnectedIcons.getIcon(texture, StellarConnections.getMask(world, x, y, z, side));
            if (icon != null) return icon;
        }
        return getIcon(side, metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        if (sideIcons == null) {
            return blockIcon;
        }
        int displayMetadata = getDisplayVariant(metadata).getMetadata();
        return side == 0 || side == 1 ? endIcons[displayMetadata] : sideIcons[displayMetadata];
    }
}
