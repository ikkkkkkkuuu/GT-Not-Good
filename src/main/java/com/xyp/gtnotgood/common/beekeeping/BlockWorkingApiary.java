package com.xyp.gtnotgood.common.beekeeping;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import forestry.apiculture.blocks.BlockApiculture;
import forestry.apiculture.blocks.BlockApicultureType;
import forestry.core.tiles.MachineDefinition;
import forestry.plugins.PluginApiculture;

/** A separately registered apiary using Forestry's interactions, geometry and live resource-pack textures. */
public final class BlockWorkingApiary extends BlockApiculture {

    public BlockWorkingApiary() {
        setBlockName("working_apiary");
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        addDefinition(
            new MachineDefinition(0, ModList.GTNotGood.getID() + ".WorkingApiary", TileWorkingApiary.class, null));
    }

    /** Forestry registers its own icons; delegating avoids copying assets or changing its machine definitions. */
    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return PluginApiculture.blocks.apiculture.getIcon(side, BlockApicultureType.APIARY.getMeta());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        return PluginApiculture.blocks.apiculture.getIcon(world, x, y, z, side);
    }
}
