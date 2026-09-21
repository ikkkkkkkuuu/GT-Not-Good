// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

/** Upstream wireless blockstate uses the same cube-all art for every facing. */
public final class BlockPackagedProvider extends Block {

    public BlockPackagedProvider() {
        super(Material.iron);
        // #tr tile.wireless_packaged_pattern_provider.name
        // # Wireless Packaged Pattern Provider
        // # zh_CN 无线封包样板供应器
        setBlockName("wireless_packaged_pattern_provider");
        setBlockTextureName(ModList.GTNotGood.getResourcePath("packaged/provider"));
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
        setHardness(3);
        setResistance(10);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TilePackagedProvider();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote && placer instanceof EntityPlayer player
            && world.getTileEntity(x, y, z) instanceof TilePackagedProvider tile) {
            tile.setOwnerName(player.getCommandSenderName());
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (player.getHeldItem() != null && player.getHeldItem()
            .getItem() instanceof ItemWirelessConnector) return false;
        if (world.getTileEntity(x, y, z) instanceof TilePackagedProvider tile && tile.canConfigure(player)) {
            if (!world.isRemote) TileEntityGuiFactory.INSTANCE.open(player, x, y, z);
            return true;
        }
        return world.isRemote;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TilePackagedProvider tile) {
            for (int i = 0; i < tile.getSizeInventory(); i++) {
                ItemStack stack = tile.getStackInSlotOnClosing(i);
                if (stack != null) world.spawnEntityInWorld(new EntityItem(world, x + .5, y + .5, z + .5, stack));
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
