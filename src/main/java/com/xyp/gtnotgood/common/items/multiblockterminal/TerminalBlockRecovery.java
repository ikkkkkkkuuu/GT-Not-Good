package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicTank;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;

public final class TerminalBlockRecovery {

    private static final ThreadLocal<Harvest> activeHarvest = new ThreadLocal<>();

    static {
        MinecraftForge.EVENT_BUS.register(new TerminalBlockRecovery());
    }

    private TerminalBlockRecovery() {}

    public static boolean isHarvesting() {
        return activeHarvest.get() != null;
    }

    public static boolean harvest(EntityPlayerMP player, World world, int x, int y, int z, TerminalItemSource source) {
        TileEntity originalTile = world.blockExists(x, y, z) ? world.getTileEntity(x, y, z) : null;
        IMetaTileEntity machine = originalTile instanceof IGregTechTileEntity gtTile ? gtTile.getMetaTileEntity()
            : null;
        boolean harvested = harvest(player, world, x, y, z, source::give);
        if (
            harvested && !player.theItemInWorldManager.isCreative()
                && world.getTileEntity(x, y, z) != originalTile
                && machine != null
        ) recoverNativeFluids(machine, source);
        return harvested;
    }

    public static boolean harvest(EntityPlayerMP player, World world, int x, int y, int z, Consumer<ItemStack> sink) {
        if (
            world.isRemote || player.worldObj != world
                || player.theItemInWorldManager.theWorld != world
                || activeHarvest.get() != null
                || !world.blockExists(x, y, z)
                || !world.canMineBlock(player, x, y, z)
                || !player.canPlayerEdit(x, y, z, 1, player.getHeldItem())
        ) return false;
        Block block = world.getBlock(x, y, z);
        if (block.isAir(world, x, y, z) || block.getBlockHardness(world, x, y, z) < 0) return false;

        Harvest harvest = new Harvest(world, x, y, z, new ArrayList<>());
        activeHarvest.set(harvest);
        try {
            return player.theItemInWorldManager.tryHarvestBlock(x, y, z);
        } finally {
            activeHarvest.remove();
            for (ItemStack drop : harvest.drops()) sink.accept(drop);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void recoverDrops(EntityJoinWorldEvent event) {
        Harvest harvest = activeHarvest.get();
        if (harvest == null || event.world != harvest.world() || !(event.entity instanceof EntityItem item)) return;
        if (
            item.forceSpawn || item.posX < harvest.x() - 0.5
                || item.posX > harvest.x() + 1.5
                || item.posY < harvest.y() - 0.5
                || item.posY > harvest.y() + 1.5
                || item.posZ < harvest.z() - 0.5
                || item.posZ > harvest.z() + 1.5
        ) return;
        ItemStack drop = item.getEntityItem();
        if (drop == null || drop.stackSize <= 0) return;
        harvest.drops().add(drop.copy());
        event.setCanceled(true);
    }

    private static void recoverNativeFluids(IMetaTileEntity machine, TerminalItemSource source) {
        // These exact GT classes omit their physical fluids from setItemNBT; subclasses may preserve them themselves.
        if (machine.getClass() == MTEHatchMultiInput.class) {
            FluidStack[] fluids = ((MTEHatchMultiInput) machine).getStoredFluid();
            for (int slot = 0; slot < fluids.length; slot++) {
                FluidStack fluid = fluids[slot];
                fluids[slot] = null;
                if (fluid != null && fluid.amount > 0) source.giveFluid(fluid.copy(), false);
            }
        } else if (machine.getClass() == MTEHatchInput.class || machine.getClass() == MTEHatchOutput.class) {
            MTEBasicTank tank = (MTEBasicTank) machine;
            FluidStack fluid = tank.mFluid;
            tank.mFluid = null;
            if (fluid != null && fluid.amount > 0) source.giveFluid(fluid.copy(), false);
        }
    }

    private record Harvest(World world, int x, int y, int z, List<ItemStack> drops) {}
}
