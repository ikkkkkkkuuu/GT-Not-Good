package com.xyp.gtnotgood.common.torcherino.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.common.torcherino.api.ITorcherinoTile;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Common torch-shaped block behavior for every Torcherino variant.
 */
public abstract class BlockTorcherinoBase extends BlockTorch implements ITileEntityProvider {

    private final String iconName;

    /**
     * Creates a torch block that owns a tile entity and can be toggled by redstone.
     *
     * @param blockName unlocalized and registry name
     * @param iconName  block texture name inside this mod's resource domain
     * @param light     light level emitted by the torch
     */
    protected BlockTorcherinoBase(String blockName, String iconName, float light) {
        super();
        this.iconName = iconName;
        setBlockName(blockName);
        setLightLevel(light);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        this.isBlockContainer = true;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        syncRedstoneState(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighborBlock) {
        syncRedstoneState(world, x, y, z);
        super.onNeighborBlockChange(world, x, y, z, neighborBlock);
    }

    private static void syncRedstoneState(World world, int x, int y, int z) {
        if (world == null || world.isRemote) return;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof ITorcherinoTile) {
            ITorcherinoTile torcherinoTile = (ITorcherinoTile) tile;
            torcherinoTile.setActive(!world.isBlockIndirectlyGettingPowered(x, y, z));
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block blockBroken, int meta) {
        super.breakBlock(world, x, y, z, blockBroken, meta);
        world.removeTileEntity(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        Blocks.torch.randomDisplayTick(world, x, y, z, random);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister registry) {
        this.blockIcon = registry.registerIcon(ModList.GTNotGood.getResourcePath(iconName));
    }
}
