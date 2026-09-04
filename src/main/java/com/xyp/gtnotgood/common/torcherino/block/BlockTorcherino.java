package com.xyp.gtnotgood.common.torcherino.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.TileEntityGuiFactory;

/**
 * Area Torcherino block that opens the acceleration range and speed GUI on right click.
 */
public class BlockTorcherino extends BlockTorcherinoBase {

    private final TorcherinoTileFactory tileFactory;

    /**
     * Creates one area-acceleration Torcherino tier.
     *
     * @param blockName   unlocalized and registry name
     * @param iconName    texture name
     * @param light       emitted light
     * @param tileFactory tile factory for this tier
     */
    public BlockTorcherino(String blockName, String iconName, float light, TorcherinoTileFactory tileFactory) {
        super(blockName, iconName, light);
        this.tileFactory = tileFactory;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return tileFactory.create();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof IGuiHolder) {
            TileEntityGuiFactory.INSTANCE.open(player, x, y, z);
        }
        return true;
    }
}
