package com.xyp.gtnotgood.common.blocks.machine;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Tiered structural matrix used by the Assembly Factory.
 */
public class AssemblyMatrixBlock extends Block {

    private final int levelTier;
    private final String iconName;

    /**
     * Creates one of the two assembly matrix tiers.
     *
     * @param blockName registry and unlocalized name
     * @param iconName  block texture name inside this mod's resource domain
     * @param levelTier structure tier, either 1 or 2
     */
    public AssemblyMatrixBlock(String blockName, String iconName, int levelTier) {
        super(Material.iron);
        if (levelTier < 1 || levelTier > 2) throw new IllegalArgumentException("Assembly matrix tier must be 1 or 2");
        this.levelTier = levelTier;
        this.iconName = iconName;
        setBlockName(blockName);
        setHardness(6.0F);
        setResistance(20.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
    }

    /** @return structure tier represented by this block */
    public int getLevelTier() {
        return levelTier;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(ModList.GTNotGood.getResourcePath(iconName));
    }
}
