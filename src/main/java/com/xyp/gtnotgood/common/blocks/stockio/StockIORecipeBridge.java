package com.xyp.gtnotgood.common.blocks.stockio;

import java.util.Arrays;
import java.util.function.IntSupplier;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.parts.stockio.PartStockIOInterface;

import appeng.api.parts.IPartHost;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/**
 * Gives the native singleblock recipe map a temporary view of one adjacent ME interface. Local inputs
 * remain first and are consumed only after ME accepts the entire transaction. Network snapshots never
 * enter the machine's physical inventory or tank, including during lookup and rejected overclocks.
 */
public final class StockIORecipeBridge {

    private static final ThreadLocal<Attempt> active = new ThreadLocal<>();

    private StockIORecipeBridge() {}

    /** ME inventory changes do not touch a machine slot, so idle machines retry once per second. */
    public static void retryIdle(MTEBasicMachine machine, IGregTechTileEntity base, long tick) {
        if (base.isServerSide() && tick % 20 == 0
            && machine.mMaxProgresstime <= 0
            && base.isAllowedToWork()
            && find(base) != null) base.markInventoryBeenModified();
    }

    /**
     * Runs the original lookup, output checks, cleanroom checks and overclock calculation together.
     *
     * @param machine  native singleblock performing the check
     * @param original unmodified recipe-check body
     * @return native result, or the native requirements-failed result when ME could not commit
     */
    public static int checkRecipe(MTEBasicMachine machine, IntSupplier original) {
        if (active.get() != null) return original.getAsInt();
        StockIOLogic logic = find(machine.getBaseMetaTileEntity());
        if (logic == null) return original.getAsInt();
        StockIOSnapshot snapshot = logic.startRecipe();
        if (snapshot == null) return original.getAsInt();
        Attempt attempt = new Attempt(machine, snapshot);
        active.set(attempt);
        boolean committed = false;
        try {
            int result = original.getAsInt();
            if (result != 2) return result;
            if (!attempt.inputs.valid() || !logic.endRecipe(snapshot)) return 1;
            attempt.inputs.commit();
            committed = true;
            return result;
        } finally {
            if (!committed) attempt.restoreOutputs();
            logic.cancelRecipe(snapshot);
            active.remove();
        }
    }

    /** Native lookup and consumption must share these same mutable item copies. */
    public static ItemStack[] itemInputs(MTEBasicMachine machine, ItemStack[] local) {
        Attempt attempt = active.get();
        return attempt != null && attempt.machine == machine ? attempt.inputs.items(local) : local;
    }

    /** Adds all configured fluid types to both lookup and consumption, without staging a physical tank. */
    public static FluidStack[] fluidInputs(MTEBasicMachine machine, FluidStack[] local) {
        Attempt attempt = active.get();
        return attempt != null && attempt.machine == machine ? attempt.inputs.fluids(local) : local;
    }

    /** Finds one already-loaded interface whose machine-facing side touches this controller. */
    static StockIOLogic find(IGregTechTileEntity base) {
        if (base == null || !base.isServerSide()) return null;
        World world = base.getWorld();
        if (world == null) return null;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            int x = base.getXCoord() + side.offsetX;
            int y = base.getYCoord() + side.offsetY;
            int z = base.getZCoord() + side.offsetZ;
            if (!world.blockExists(x, y, z)) continue;
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile == null || tile.isInvalid()) continue;
            StockIOLogic logic = null;
            if (tile instanceof TileStockIOInterface block) logic = block.getLogic();
            else if (tile instanceof IPartHost host
                && host.getPart(side.getOpposite()) instanceof PartStockIOInterface part) logic = part.getLogic();
            if (logic != null && logic.getTargetSide() == side.getOpposite() && logic.isEnabled() && logic.isOnline())
                return logic;
        }
        return null;
    }

    private static final class Attempt {

        private final MTEBasicMachine machine;
        private final LocalInputs inputs;
        private final ItemStack[] outputs;
        private final FluidStack outputFluid;
        private final int duration;
        private final int eut;

        private Attempt(MTEBasicMachine machine, StockIOSnapshot snapshot) {
            this.machine = machine;
            inputs = new LocalInputs(snapshot);
            outputs = Arrays.stream(machine.mOutputItems)
                .map(stack -> stack == null ? null : stack.copy())
                .toArray(ItemStack[]::new);
            outputFluid = machine.mOutputFluid == null ? null : machine.mOutputFluid.copy();
            duration = machine.mMaxProgresstime;
            eut = machine.mEUt;
        }

        private void restoreOutputs() {
            System.arraycopy(outputs, 0, machine.mOutputItems, 0, outputs.length);
            machine.mOutputFluid = outputFluid;
            machine.mMaxProgresstime = duration;
            machine.mEUt = eut;
        }
    }

    /** Tracks local ownership separately from the network's independently validated consumption ledger. */
    static final class LocalInputs {

        private final StockIOSnapshot snapshot;
        private ItemStack[] itemOwners;
        private ItemStack[] itemCopies;
        private ItemStack[] items;
        private FluidStack[] fluidOwners;
        private FluidStack[] fluidCopies;
        private FluidStack[] fluids;

        LocalInputs(StockIOSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        ItemStack[] items(ItemStack[] local) {
            if (items != null) return items;
            itemOwners = local == null ? new ItemStack[0] : local.clone();
            itemCopies = Arrays.stream(itemOwners)
                .map(stack -> stack == null ? null : stack.copy())
                .toArray(ItemStack[]::new);
            int count = itemCopies.length;
            for (ItemStack stack : snapshot.items) if (stack != null) count++;
            items = Arrays.copyOf(itemCopies, count);
            int next = itemCopies.length;
            for (ItemStack stack : snapshot.items) if (stack != null) items[next++] = stack;
            return items;
        }

        FluidStack[] fluids(FluidStack[] local) {
            if (fluids != null) return fluids;
            fluidOwners = local == null ? new FluidStack[0] : local.clone();
            fluidCopies = Arrays.stream(fluidOwners)
                .map(stack -> stack == null ? null : stack.copy())
                .toArray(FluidStack[]::new);
            int count = fluidCopies.length;
            for (FluidStack stack : snapshot.fluids) if (stack != null) count++;
            fluids = Arrays.copyOf(fluidCopies, count);
            int next = fluidCopies.length;
            for (FluidStack stack : snapshot.fluids) if (stack != null) fluids[next++] = stack;
            return fluids;
        }

        boolean valid() {
            if (itemOwners != null) {
                for (int i = 0; i < itemOwners.length; i++) {
                    ItemStack owner = itemOwners[i];
                    ItemStack copy = itemCopies[i];
                    if (owner != null && (copy == null || !owner.isItemEqual(copy)
                        || !ItemStack.areItemStackTagsEqual(owner, copy)
                        || copy.stackSize < 0
                        || copy.stackSize > owner.stackSize)) return false;
                }
            }
            if (fluidOwners != null) {
                for (int i = 0; i < fluidOwners.length; i++) {
                    FluidStack owner = fluidOwners[i];
                    FluidStack copy = fluidCopies[i];
                    if (owner != null
                        && (copy == null || !owner.isFluidEqual(copy) || copy.amount < 0 || copy.amount > owner.amount))
                        return false;
                }
            }
            return true;
        }

        void commit() {
            if (itemOwners != null) {
                for (int i = 0; i < itemOwners.length; i++)
                    if (itemOwners[i] != null) itemOwners[i].stackSize = itemCopies[i].stackSize;
            }
            if (fluidOwners != null) {
                for (int i = 0; i < fluidOwners.length; i++)
                    if (fluidOwners[i] != null) fluidOwners[i].amount = fluidCopies[i].amount;
            }
        }
    }
}
