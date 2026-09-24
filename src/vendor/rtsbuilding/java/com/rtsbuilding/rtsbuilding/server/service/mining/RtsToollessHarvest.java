package com.rtsbuilding.rtsbuilding.server.service.mining;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;

import com.rtsbuilding.rtsbuilding.platform.math.BlockPos;

/**
 * GTNG's built-in RTS harvester: no physical tool, with native block drops in both game modes.
 * Callers enforce RTS range and claim rules and capture spawned drops for automatic storage.
 * The Forge break event and native removal callbacks remain authoritative; no fallback item is
 * fabricated, so blocks intentionally producing no drops continue to do so.
 */
public final class RtsToollessHarvest {
    private RtsToollessHarvest() {}

    /**
     * Harvests once while preserving the held stack and player mode, including on callback failure.
     * Native harvest callbacks run after removal with willHarvest=true for delayed tile removal.
     *
     * @param player server player requesting the operation
     * @param pos validated target position
     * @return whether native removal accepted the operation
     */
    public static boolean harvest(EntityPlayerMP player, BlockPos pos) {
        WorldServer world = player.getServerForPlayer();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        Block block = world.getBlock(x, y, z);
        int metadata = world.getBlockMetadata(x, y, z);
        if (block.isAir(world, x, y, z) || block.getBlockHardness(world, x, y, z) < 0.0F) return false;
        net.minecraftforge.event.world.BlockEvent.BreakEvent event = ForgeHooks.onBlockBreakEvent(
                world, player.theItemInWorldManager.getGameType(), player, x, y, z);
        if (event.isCanceled()) return false;
        int experience = event.getExpToDrop();
        int slot = player.inventory.currentItem;
        ItemStack held = player.inventory.mainInventory[slot];
        boolean creative = player.capabilities.isCreativeMode;
        try {
            player.inventory.mainInventory[slot] = null;
            player.capabilities.isCreativeMode = false;
            block.onBlockHarvested(world, x, y, z, metadata, player);
            if (!block.removedByPlayer(world, player, x, y, z, true)) return false;
            block.onBlockDestroyedByPlayer(world, x, y, z, metadata);
            block.harvestBlock(world, player, x, y, z, metadata);
            if (experience > 0 && !creative) block.dropXpOnBlockBreak(world, x, y, z, experience);
            return true;
        } finally {
            player.inventory.mainInventory[slot] = held;
            player.capabilities.isCreativeMode = creative;
        }
    }
}
