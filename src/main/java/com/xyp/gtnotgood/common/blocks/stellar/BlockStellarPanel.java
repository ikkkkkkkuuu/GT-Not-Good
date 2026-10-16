package com.xyp.gtnotgood.common.blocks.stellar;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.client.stellar.StellarPanelTextures;
import com.xyp.gtnotgood.common.blocks.stellar.StellarPanelVariant.Base;
import com.xyp.gtnotgood.common.blocks.stellar.StellarPanelVariant.Group;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.IBlockWithTextures;
import gregtech.api.interfaces.ITexture;
import gregtech.common.render.GTRendererBlock;

public final class BlockStellarPanel extends Block implements IBlockWithTextures {

    private final StellarPanelVariant[] variants = new StellarPanelVariant[16];
    private final StellarPanelVariant fallback;

    @SideOnly(Side.CLIENT)
    private ITexture[][][] textures;

    @SideOnly(Side.CLIENT)
    private ITexture[][][] inventoryTextures;

    public BlockStellarPanel(Group group) {
        super(Material.iron);
        StellarPanelVariant firstVariant = null;
        for (StellarPanelVariant variant : StellarPanelVariant.values()) {
            if (variant.getGroup() == group) {
                variants[variant.getMetadata()] = variant;
                setHarvestLevel("wrench", 2, variant.getMetadata());
                if (firstVariant == null) {
                    firstVariant = variant;
                }
            }
        }
        if (firstVariant == null) {
            throw new IllegalArgumentException("A stellar panel must have at least one variant");
        }
        fallback = firstVariant;
        setBlockName(group.getRegistryName());
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
    }

    public StellarPanelVariant getVariant(int metadata) {
        return metadata >= 0 && metadata < variants.length ? variants[metadata] : null;
    }

    public StellarPanelVariant getDisplayVariant(int metadata) {
        StellarPanelVariant variant = getVariant(metadata);
        return variant == null ? fallback : variant;
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
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        for (StellarPanelVariant variant : variants) {
            if (variant != null) {
                stacks.add(new ItemStack(item, 1, variant.getMetadata()));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        textures = new ITexture[variants.length][][];
        inventoryTextures = new ITexture[variants.length][][];
        for (StellarPanelVariant variant : variants) {
            if (variant != null) {
                int metadata = variant.getMetadata();
                ITexture[][] worldTextures = StellarPanelTextures.create(variant, register);
                textures[metadata] = worldTextures;
                ITexture[][] itemTextures = worldTextures.clone();
                // GT rotates inventory models by 90 degrees, exposing the west face.
                itemTextures[ForgeDirection.WEST.ordinal()] = worldTextures[ForgeDirection.NORTH.ordinal()];
                itemTextures[ForgeDirection.NORTH.ordinal()] = worldTextures[ForgeDirection.WEST.ordinal()];
                inventoryTextures[metadata] = itemTextures;
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        Base base = getDisplayVariant(metadata).getBase();
        return base.getBlock().getIcon(side, base.getMetadata());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ITexture[][] getTextures(int metadata) {
        return textures == null ? null : textures[getDisplayVariant(metadata).getMetadata()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ITexture[][] getInventoryTextures(int metadata) {
        return inventoryTextures == null ? null : inventoryTextures[getDisplayVariant(metadata).getMetadata()];
    }
}
